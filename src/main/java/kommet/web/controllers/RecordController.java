/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Action;
import kommet.basic.StandardAction;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.ViewUtil;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.ValidationError;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.SystemContextFactory;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.web.RequestAttributes;
import kommet.web.actions.ActionUtil;

@Controller
public class RecordController extends CommonKommetController
{
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	SystemContextFactory sysContextFactory;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/searchrecords", method = RequestMethod.POST)
	@ResponseBody
	public void searchRecord (@RequestParam("typeId") String sTypeId,
								@RequestParam(value = "keyword", required = false) String keyword,
								@RequestParam(value = "searchField", required = false) String searchField,
								HttpServletResponse resp, HttpSession session) throws KommetException, IOException
	{
		EnvData env = envService.getCurrentEnv(session);
		Type type = env.getType(KID.get(sTypeId));
		
		if (!StringUtils.hasText(searchField))
		{
			searchField = type.getDefaultFieldApiName();
		}
		
		String query = "select id, " + searchField + " from " + type.getQualifiedName();
		if (StringUtils.hasText(keyword))
		{
			query += " where " + searchField + " like '" + keyword + "%'";
		}
		
		List<Record> records = env.getSelectCriteriaFromDAL(query, AuthUtil.getAuthData(session)).list();
		PrintWriter out = resp.getWriter();
		out.write("[");
		
		List<String> values = new ArrayList<String>();
		
		for (Record record : records)
		{
			values.add(record.getFieldStringValue(searchField, AuthUtil.getAuthData(session).getLocale()));
		}
		
		out.write(MiscUtils.implode(values, ", ", "\"", null));
		out.write("]");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/unassociate", method = RequestMethod.POST)
	@ResponseBody
	public void unassociate (@RequestParam(value = "assocField", required = false) String sAssocFieldId,
									@RequestParam(value = "recordId", required = false) String sRecordId,
									@RequestParam(value = "assocRecordId", required = false) String sAssociatedRecordId,
									HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sAssocFieldId))
		{
			out.write(getErrorJSON("Association field ID is empty"));
			return;
		}
		
		if (!StringUtils.hasText(sRecordId))
		{
			out.write(getErrorJSON("Record ID is empty"));
			return;
		}
		
		if (!StringUtils.hasText(sAssociatedRecordId))
		{
			out.write(getErrorJSON("Associated record ID is empty"));
			return;
		}
		
		KID associationFieldId = null;
		KID recordId = null;
		KID associatedRecordId = null;
		
		try
		{
			associationFieldId = KID.get(sAssocFieldId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid associated field ID " + sAssocFieldId));
			return;
		}
		
		try
		{
			recordId = KID.get(sRecordId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid record ID " + sRecordId));
			return;
		}
		
		try
		{
			associatedRecordId = KID.get(sAssociatedRecordId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid associated record ID " + sAssociatedRecordId));
			return;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		try
		{
			dataService.unassociate(associationFieldId, recordId, associatedRecordId, false, authData, envService.getCurrentEnv(session));
			out.write(getSuccessJSON("Record unassociated successfully"));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error unassociating records: " + MiscUtils.getExceptionDesc(e)));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/item/{rid}", method = RequestMethod.GET)
	public ModelAndView recordDetails (@PathVariable("rid") String rid, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		Type obj = env.getType(KID.get(rid).getKeyPrefix());
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		// update permissions if necessary, and do this only once here so that later
		// in this method we can use canRead* methods that do not perform the update
		authData.updateTypePermissions(true, env);
		authData.updateFieldPermissions(true, env);
		
		if (!authData.canReadType(obj.getKID(), true, env))
		{
			return getErrorPage("Insufficient privileges to view objects of type " + obj.getLabel());
		}
		
		List<Field> displayedFields = new ArrayList<Field>();
		
		// build criteria to retrieve the object
		Criteria c = env.getSelectCriteria(obj.getKID());
		
		// get all fields of the given object
		for (Field field : obj.getFields())
		{
			// make sure the user has access to the given field
			if (authData.canReadField(field, false, env))
			{
				displayedFields.add(field);
				c.addProperty(field.getApiName());
			}
		}
		
		c.add(Restriction.eq("id", KID.get(rid)));
		
		List<Record> records = c.list();
		if (records.isEmpty())
		{
			return getErrorPage("No object found with ID " + rid);
		}
		
		// standard details view name consists of the object's prefix plus "_details" suffix
		ModelAndView mv = new ModelAndView(viewService.getEnvKeetleDir(env) + "/" + ViewUtil.getStandardDetailsView(obj));
		mv.addObject("record", records.get(0));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/list/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView list(@PathVariable("keyPrefix") String keyPrefix, HttpSession session, HttpServletRequest req, HttpServletResponse resp) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		Type type = env.getType(KeyPrefix.get(keyPrefix));
		if (type == null)
		{
			return getErrorPage("Object with prefix " + keyPrefix + " not found");
		}
		
		// get the standard list action for this type and profile
		StandardAction stdListAction = actionService.getStandardListAction(type, AuthUtil.getAuthData(session).getProfile(), env);
		
		if (stdListAction == null)
		{
			// TODO
			// Perhaps use default page here?
			
			return getErrorPage("Default list view not set for type " + type.getQualifiedName() + " and profile " + AuthUtil.getAuthData(session).getProfile().getName());
		}
		
		// get page from env, because there they are stored with their full controller data
		Action listAction = env.getActionForUrl(stdListAction.getAction().getUrl());
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (listAction == null)
		{
			return getErrorPage("Default list view for type " + type.getQualifiedName() + " and profile " + authData.getProfile().getName() + " is " + stdListAction.getAction().getName() + " but it has not been found on the environment. It is a serious environment error.");
		}
		
		PageData pageData = (PageData)ActionUtil.callAction(listAction.getController().getQualifiedName(), listAction.getControllerMethod(), null, sysContextFactory.get(authData, env), req, resp, new PageData(env), compiler, dataService, sharingService, authData, env, appConfig);
		req.setAttribute(RequestAttributes.PAGE_DATA_ATTR_NAME, pageData);
		
		// forward the request to the KTL-JSP file
		RequestDispatcher dispatcher = req.getRequestDispatcher("/" + env.getKeetleDir(appConfig.getRelativeKeetleDir()) + "/" + listAction.getView().getId() + ".jsp");
		try
		{
			dispatcher.forward(req, resp);
		}
		catch (Exception e)
		{
			return getErrorPage("Error forwarding to standard list page: " + e.getMessage());
		}
		
		return null;
		
		/*List<Field> listedFields = new ArrayList<Field>();
		listedFields.add(obj.getField("id"));
		listedFields.add(obj.getField("createdDate"));
		
		List<Record> records = env.getSelectCriteriaFromDAL("select id, createdDate from " + obj.getApiName()).list();
		ModelAndView mv = new ModelAndView(keetleService.getEnvKeetleDir(env) + "/" + KeetleUtil.getStandardListView(obj));
		//ModelAndView mv = new ModelAndView("dynamic/test");
		mv.addObject("records", records);
		mv.addObject("listedFields", listedFields);
		mv.addObject("obj", obj);
		return mv;*/
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/edit/{id}", method = RequestMethod.GET)
	public ModelAndView editRecord(@PathVariable("id") String id, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		Type obj = env.getType(KID.get(id));
		if (obj == null)
		{
			return getErrorPage("Object with ID " + id + " not found");
		}
		
		ModelAndView mv = getEditView(obj, null);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/new/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView newRecord(@PathVariable("keyPrefix") String keyPrefix, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		Type obj = env.getType(KeyPrefix.get(keyPrefix));
		if (obj == null)
		{
			return getErrorPage("Object with prefix " + keyPrefix + " not found");
		}
		
		ModelAndView mv = getEditView(obj, null);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete (@RequestParam(value = "id", required = false) String sId,
									HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasLength(sId))
		{
			out.write(getErrorJSON("Empty record ID"));
			return;
		}
		
		KID id = null;
		try
		{
			id = KID.get(sId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid record ID " + sId));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		dataService.deleteRecord(id, AuthUtil.getAuthData(session), env);
		
		// redirect to record list
		out.write(getSuccessJSON("Record deleted"));
		return;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/record/save", method = RequestMethod.POST)
	public ModelAndView saveRecord (@RequestParam(value = "recordId", required = false) String recordId,
									@RequestParam("keyPrefix") String keyPrefix,
									HttpSession session,
									HttpServletRequest req) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		Type type = env.getType(KeyPrefix.get(keyPrefix));
		if (type == null)
		{
			return getErrorPage("Type with prefix " + keyPrefix + " not found");
		}
		
		Record rec = new Record(type);
		
		Enumeration<String> paramNames = req.getParameterNames();
		while (paramNames.hasMoreElements())
		{
			String param = paramNames.nextElement();
			if (param.startsWith("field_"))
			{
				String[] val = req.getParameterValues(param);
				
				if (val != null && val.length > 0)
				{
					String fieldName = param.replaceFirst("field_", ""); 
					if (val.length > 1)
					{
						throw new KommetException("Field " + fieldName + " appears more than one on the page");
					}
					
					if (StringUtils.hasText(val[0]))
					{
						rec.setField(fieldName, val[0]);
					}
				}
			}
		}
		
		try
		{
			// run field validation
			List<ValidationError> validationErrors = DataService.checkRequiredFields(rec, false, rec.attemptGetKID() != null);
			if (!validationErrors.isEmpty())
			{
				ModelAndView mv = getEditView(type, rec);
				for (ValidationError err : validationErrors)
				{
					addError(err.getMessage());
				}
				mv.addObject("errorMsgs", getErrorMsgs());
				return mv;
			}
			dataService.save(rec, AuthUtil.getAuthData(session), env);
		}
		catch (KommetException e)
		{
			// return error msgs
		}
		return new ModelAndView("redirect:/" + type.getKeyPrefix());
	}

	/**
	 * Gets the edit view for the given type and Record, depending on the user's profile and privileges.
	 * @param obj
	 * @param rec
	 * @return
	 */
	private ModelAndView getEditView(Type obj, Record rec)
	{
		ModelAndView mv = new ModelAndView("records/edit");
		
		List<Field> editableFields = new ArrayList<Field>();
		for (Field field : obj.getFields())
		{
			if (!field.isCreatedOnTypeCreation())
			{
				editableFields.add(field);
			}
		}
		
		mv.addObject("obj", obj);
		mv.addObject("editableFields", editableFields);
		
		if (rec != null)
		{
			mv.addObject("record", rec);
		}
		
		return mv;
	}
}