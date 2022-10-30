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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
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

import kommet.auth.AuthUtil;
import kommet.auth.UserService;
import kommet.basic.Profile;
import kommet.basic.UniqueCheck;
import kommet.config.Constants;
import kommet.dao.UniqueCheckFilter;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.UniqueCheckService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class UniqueCheckController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	UniqueCheckService ucService;
	
	@Inject
	DataService dataService;
	
	@Inject
	UserService userService;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/uniquechecks/save", method = RequestMethod.POST)
	public ModelAndView save(@RequestParam(value = "typeId", required = false) String sTypeId,
							@RequestParam(value = "uniqueCheckId", required = false) String sUniqueCheckId,
							@RequestParam(value = "name", required = false) String name,
							@RequestParam(value = "fieldIds", required = false) String fieldIdList, HttpSession session) throws KommetException
	{
		clearMessages();
		
		UniqueCheck check = new UniqueCheck();
		EnvData env = envService.getCurrentEnv(session);
		Type type = env.getType(KID.get(sTypeId));
		
		if (StringUtils.hasText(sUniqueCheckId))
		{
			check = ucService.get(KID.get(sUniqueCheckId), env);
			
			if (isSystemUniqueCheck(check))
			{
				addError("Cannot edit system unique check");
			}
			
			// clear fields. they will be added anew based on user input
			check.clearFields();
		}
		
		if (!StringUtils.hasText(name))
		{
			addError("Please enter unique check name");
		}
		else if (name.startsWith("kommet."))
		{
			addError("Unique check names must not start with reserved prefix kommet");
		}
		
		check.setName(name);
		
		if (!StringUtils.hasText(sTypeId))
		{
			addError("Type not defined");
		}
		check.setTypeId(KID.get(sTypeId));
		
		Set<KID> fieldIds = new HashSet<KID>();
		
		if (!StringUtils.hasText(fieldIdList))
		{
			addError("No fields selected");
		}
		else
		{
			List<String> sFieldIds = MiscUtils.splitAndTrim(fieldIdList, ",");
			for (String sFieldId : sFieldIds)
			{
				fieldIds.add(KID.get(sFieldId));
				check.addField(type.getField(KID.get(sFieldId)));
			}
			
			// check if a unique check with the given field set does not already exist
			UniqueCheckFilter filter = new UniqueCheckFilter();
			filter.addTypeId(check.getTypeId());
			
			List<UniqueCheck> typeChecks = ucService.find(filter, env, dataService);
			for (UniqueCheck existingCheck : typeChecks)
			{	
				if (existingCheck.getParsedFieldIds().size() == fieldIds.size())
				{
					boolean allFieldsPresent = true;
					
					for (KID existingFieldId : existingCheck.getParsedFieldIds())
					{
						allFieldsPresent &= fieldIds.contains(existingFieldId);
					}
					
					if (allFieldsPresent)
					{
						addError("Unique check " + existingCheck.getName() + " has the same field set as the one defined for the current field");
						break;
					}
				}
			}
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("uniquechecks/edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("uc", check);
			mv.addObject("fields", type.getFields());
			mv.addObject("keyPrefix", type.getKeyPrefix());
			mv.addObject("typeId", type.getKID());
			
			Set<KID> selectedFieldIds = new HashSet<KID>();
			selectedFieldIds.addAll(check.getParsedFieldIds());
			mv.addObject("selectedFieldIds", selectedFieldIds);
			return mv;
		}
		
		check = ucService.save(check, AuthUtil.getAuthData(session), env);
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/uniquechecks/" + check.getId());
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/uniquechecks/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete(@RequestParam(value = "id", required = false) String sUniqueCheckId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{	
		EnvData env = envService.getCurrentEnv(session);
		UniqueCheck check = ucService.get(KID.get(sUniqueCheckId), env);
		ucService.delete(KID.get(sUniqueCheckId), AuthUtil.getAuthData(session), env);
		
		resp.setContentType("application/json; charset=UTF-8");
		PrintWriter out = resp.getWriter();
		out.write(RestUtil.getRestSuccessDataResponse("{ \"keyPrefix\": \"" + env.getType(check.getTypeId()).getKeyPrefix() + "\" }"));
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/uniquechecks/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable(value = "id") String sUniqueCheckId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("uniquechecks/details");
		EnvData env = envService.getCurrentEnv(session);
		UniqueCheck check = ucService.get(KID.get(sUniqueCheckId), env);
		mv.addObject("isSystem", isSystemUniqueCheck(check));
		
		if (isSystemUniqueCheck(check))
		{
			Type type = env.getType(check.getTypeId());
			check.setName("Unique field: " + type.getField(check.getParsedFieldIds().get(0)).getApiName());
		}
		
		mv.addObject("uc", check);
		
		Map<KID, Field> fieldsById = new HashMap<KID, Field>();
		for (KID fieldId : check.getParsedFieldIds())
		{
			fieldsById.put(fieldId, dataService.getField(fieldId, env));
		}
		
		mv.addObject("fieldsById", fieldsById);
		
		mv.addObject("createdBy", userService.getUser(check.getCreatedBy().getId(), env));
		return mv;
	}
	
	private static boolean isSystemUniqueCheck (UniqueCheck uc)
	{
		return uc.getName().startsWith(Constants.UNIQUE_CHECK_FIELD_PREFIX);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/uniquechecks/new", method = RequestMethod.GET)
	public ModelAndView create(@RequestParam(value = "typeId", required = false) String sTypeId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("uniquechecks/edit");
		
		if (!StringUtils.hasText(sTypeId))
		{
			mv.addObject("errorMsgs", getMessage("Type not defined"));
			return mv;
		}
		
		KID typeId = null;
		
		try
		{
			typeId = KID.get(sTypeId);
		}
		catch (KIDException e)
		{
			mv.addObject("errorMsgs", getMessage("Invalid type ID " + sTypeId));
			return mv;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		Type type = env.getType(typeId);
		
		mv.addObject("typeId", typeId);
		mv.addObject("keyPrefix", type.getKeyPrefix());
		mv.addObject("fields", getFieldsForUniqueCheck(type));
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/uniquechecks/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable(value = "id") String sUniqueCheckId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("uniquechecks/edit");
		
		
		EnvData env = envService.getCurrentEnv(session);
		UniqueCheck check = ucService.get(KID.get(sUniqueCheckId), env);
		
		Type type = env.getType(check.getTypeId());
		
		mv.addObject("uc", check);
		mv.addObject("typeId", check.getTypeId());
		mv.addObject("keyPrefix", type.getKeyPrefix());
		mv.addObject("fields", getFieldsForUniqueCheck(type));
		
		Set<KID> selectedFieldIds = new HashSet<KID>();
		selectedFieldIds.addAll(check.getParsedFieldIds());
		mv.addObject("selectedFieldIds", selectedFieldIds);
		return mv;
	}
	
	/**
	 * Get field which can be included in a unique check.
	 * @param type
	 * @return
	 */
	private static List<Field> getFieldsForUniqueCheck (Type type)
	{
		List<Field> fields = new ArrayList<Field>();
		
		for (Field field : type.getFields())
		{
			// exclude some data types
			// also exclude ID fields because they are always unique
			if (UniqueCheckService.isValidUniqueCheckFieldDataType(field.getDataTypeId()) && !field.getApiName().equals(Field.ID_FIELD_NAME))
			{
				fields.add(field);
			}
		}
		
		return fields;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/typeuniquechecks", method = RequestMethod.GET)
	public ModelAndView listForType(@RequestParam(value = "typeId", required = false) String sTypeId, HttpSession session) throws KommetException
	{
		UniqueCheckFilter filter = new UniqueCheckFilter();
		if (StringUtils.hasText(sTypeId))
		{
			filter.addTypeId(KID.get(sTypeId));
		}
		
		ModelAndView mv = new ModelAndView("uniquechecks/listfortype");
		
		EnvData env = envService.getCurrentEnv(session);
		
		List<UniqueCheck> checks = ucService.find(filter, env, dataService);
		
		// convert names of system unique checks
		for (UniqueCheck check : checks)
		{
			if (isSystemUniqueCheck(check))
			{
				Type type = env.getType(check.getTypeId());
				check.setName("Unique field: " + type.getField(check.getParsedFieldIds().get(0)).getApiName());
			}
		}
		
		mv.addObject("ucs", checks);
		
		Map<KID, Field> fieldsById = new HashMap<KID, Field>();
		
		for (UniqueCheck uc : checks)
		{
			for (KID fieldId : uc.getParsedFieldIds())
			{
				fieldsById.put(fieldId, dataService.getField(fieldId, env));
			}
		}
		
		mv.addObject("fieldsById", fieldsById);
		return mv;
	}
}