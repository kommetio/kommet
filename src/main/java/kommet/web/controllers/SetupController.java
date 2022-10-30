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
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Action;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.Profile;
import kommet.basic.View;
import kommet.basic.actions.ActionFilter;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.keetle.ViewService;
import kommet.basic.types.SystemTypes;
import kommet.basic.types.UserCascadeHierarchyKType;
import kommet.basic.types.UserKType;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.TypeFilter;
import kommet.emailing.EmailAccount;
import kommet.emailing.EmailMessage;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.QueryResultOrder;
import kommet.json.JSON;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.labels.TextLabelService;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.GlobalSettingsService;
import kommet.services.SystemSettingService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class SetupController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	BasicSetupService setupService;
	
	@Inject
	GlobalSettingsService globalSettingService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ClassService classService;
	
	@Inject
	SystemSettingService settingService;
	
	@Inject
	EmailService emailService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ActionService actionService;
	
	@Inject
	TextLabelService labelService;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/setup", method = RequestMethod.GET)
	public ModelAndView setup() throws KommetException
	{
		return new ModelAndView("setup/home");
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/adminpanel", method = RequestMethod.GET)
	public ModelAndView adminPanel() throws KommetException
	{
		return new ModelAndView("setup/adminpanel");
	}
	
	@RestrictedAccess(profiles = Profile.ROOT_NAME)
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/generatefieldid", method = RequestMethod.GET)
	public ModelAndView generateFieldId() throws KommetException
	{
		return new ModelAndView("tools/generatefieldid");
	}
	
	@RestrictedAccess(profiles = Profile.ROOT_NAME)
	@ResponseBody
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/mailtest", method = RequestMethod.GET)
	public void testMail(HttpServletResponse resp, HttpSession session) throws KommetException, IOException
	{
		EmailAccount acc = new EmailAccount();
		acc.setSmtpHost("smtp.gmail.com");
		acc.setImapPort(995);
		acc.setPassword("");
		acc.setSmtpPort(587);
		acc.setImapHost("imap.gmail.com");
		acc.setUserName("radek.krawiec@gmail.com");
		acc.setSenderAddress(acc.getUserName());
		acc.setSenderName("Radek from Raimme");
		
		//emailService.sendEmail("Test", MiscUtils.toList(new Recipient("radek.krawiec@gmail.com")), "AA", "BB", null, null, acc);
		
		List<EmailMessage> newMessages = emailService.readEmails(acc, new Date(117, 1, 12));
		newMessages.get(0);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@ResponseBody
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/setup/stats/items", method = RequestMethod.GET)
	public void getRecentClasses(HttpServletResponse resp, HttpSession session) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		// get classes
		ClassFilter classFilter = new ClassFilter();
		classFilter.setOrderBy("createdDate");
		classFilter.setOrder(QueryResultOrder.DESC);
		classFilter.setLimit(10);
		classFilter.setSystemFile(false);
		
		// serialize classes into JSON
		List<String> serializedClasses = new ArrayList<String>();
		for (Class cls : classService.getClasses(classFilter, env))
		{
			serializedClasses.add(JSON.serialize(cls, authData));
		}
		
		// get types
		TypeFilter typeFilter = new TypeFilter();
		typeFilter.setIsBasic(false);
		typeFilter.setOrderBy("created");
		typeFilter.setOrder(QueryResultOrder.DESC);
		typeFilter.setLimit(10);
		
		// serialize types into JSON
		List<String> serializedTypes = new ArrayList<String>();
		for (Type type : dataService.getTypes(typeFilter, false, false, env))
		{
			serializedTypes.add(JSON.serialize(type, authData));
		}
		
		// get views
		ViewFilter viewFilter = new ViewFilter();
		viewFilter.setOrderBy("createdDate");
		viewFilter.setOrder(QueryResultOrder.DESC);
		viewFilter.setLimit(10);
		viewFilter.setSystemView(false);
		
		// serialize views into JSON
		List<String> serializedViews = new ArrayList<String>();
		for (View view : viewService.getViews(viewFilter, env))
		{
			serializedViews.add(JSON.serialize(view, authData));
		}
		
		// get actions
		ActionFilter actionFilter = new ActionFilter();
		actionFilter.setOrderBy("createdDate");
		actionFilter.setOrder(QueryResultOrder.DESC);
		actionFilter.setLimit(10);
		actionFilter.setIsSystem(false);
		
		// serialize views into JSON
		List<String> serializedActions = new ArrayList<String>();
		for (Action action : actionService.getActions(actionFilter, env))
		{
			serializedActions.add(JSON.serialize(action, authData));
		}
		
		StringBuilder data = new StringBuilder("{ ");
		data.append("\"classes\": [").append(MiscUtils.implode(serializedClasses, ", ")).append(" ], ");
		data.append("\"types\": [").append(MiscUtils.implode(serializedTypes, ", ")).append(" ], ");
		data.append("\"views\": [").append(MiscUtils.implode(serializedViews, ", ")).append(" ], ");
		data.append("\"actions\": [").append(MiscUtils.implode(serializedActions, ", ")).append(" ]");
		data.append(" }");
		
		try
		{
			out.write(RestUtil.getRestSuccessDataResponse("{ \"data\": " + data + " }"));
			return;
		}
		catch (Exception e)
		{
			out.write(getErrorJSON("Error: " + e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/createsettingvaluetype", method = RequestMethod.GET)
	public ModelAndView createSettingValueType(HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		Type t = env.getType(KeyPrefix.get(KID.USER_CASCADE_HIERARCHY_PREFIX));
		UserCascadeHierarchyKType uchType = new UserCascadeHierarchyKType();
		uchType.setApiName(t.getApiName());
		uchType.setKID(t.getKID());
		uchType.setDbTable(t.getDbTable());
		uchType.setLabel(t.getLabel());
		uchType.setBasic(t.isBasic());
		
		setupService.createSettingValueType(uchType, env);
		return getErrorPage("Setting value created");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/createtypeinfo", method = RequestMethod.GET)
	public ModelAndView createTypeInfo(@RequestParam(value = "typeId", required = false) String sTypeId, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		Type type = env.getType(KID.get(sTypeId));
		
		Class controller = classService.getClass(type.getQualifiedName() + "Controller", env);
		
		dataService.createStandardActionsForType(type, controller, env);
		
		return new ModelAndView("setup/home");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/createtype", method = RequestMethod.GET)
	public ModelAndView createType(@RequestParam(value = "type", required = false) String type, 
									@RequestParam(value = "env", required = false) String envId,
									HttpSession session) throws KommetException
	{
		if (!StringUtils.hasText(type))
		{
			addError("Type not defined");
		}
		
		if (!StringUtils.hasText(envId))
		{
			addError("Environment not defined");
		}
		
		if (hasErrorMessages())
		{
			return getErrorPage(getErrorMsgs());
		}
		
		EnvData env = envService.get(KID.get(envId));
		
		if (type.equals(SystemTypes.USER_RECORD_SHARING_API_NAME))
		{
			setupService.createUserRecordSharings((UserKType)env.getType(KeyPrefix.get(KID.USER_PREFIX)), env);
		}
		
		return getMessagePage("Type " + type + " successfully created.");
	}
	
	@RestrictedAccess(profiles = Profile.ROOT_NAME)
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reloadconfig", method = RequestMethod.GET)
	@ResponseBody
	public void reloadConfig(HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		try
		{
			appConfig.clearCache();
			out.write(getSuccessJSON("Done"));
			return;
		}
		catch (Exception e)
		{
			out.write(getErrorJSON("Error: " + e.getMessage()));
			return;
		}
	}
	
	@RestrictedAccess(profiles = Profile.ROOT_NAME)
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/textlabels/clearcache", method = RequestMethod.GET)
	@ResponseBody
	public void clearTextLabelCache(HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		try
		{
			labelService.initTextLabels(envService.getCurrentEnv(session));
			out.write(getSuccessJSON("Done"));
			return;
		}
		catch (Exception e)
		{
			out.write(getErrorJSON("Error: " + e.getMessage()));
			return;
		}
	}
	
	@RestrictedAccess(profiles = Profile.ROOT_NAME)
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/envs/clearcache", method = RequestMethod.GET)
	@ResponseBody
	public void clearEnvCache(HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		
		try
		{
			envService.clear(env.getId());
			out.write(getSuccessJSON("Done"));
			return;
		}
		catch (Exception e)
		{
			out.write(getErrorJSON("Error: " + e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/initenv", method = RequestMethod.GET)
	public ModelAndView initEnv(@RequestParam(value = "env", required = false) String envId) throws KIDException, KommetException
	{	
		// get env, but do not read in types because this operation requires existence
		// of the UniqueCheck type on the env
		EnvData env = envService.get(KID.get(envId), false, false, false, false, false, false, false, false, false, false, false, false, false, false);
		
		// run basic configuration
		setupService.runBasicSetup(env);
		
		settingService.reloadSystemSettings(env);
		
		return getMessagePage("Environment initialized.");
	}
}