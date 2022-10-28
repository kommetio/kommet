/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;

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
import kommet.basic.TypeTrigger;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.exceptions.NotImplementedException;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.triggers.TriggerService;
import kommet.triggers.TriggerUtil;
import kommet.utils.UrlUtil;

@Controller
public class TriggerController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ClassService classService;
	
	@Inject
	TriggerService triggerService;
	
	/*@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/triggers/save", method = RequestMethod.POST)
	public ModelAndView saveTypeTrigger(@RequestParam(value = "typeId", required = false) String sTypeId,
										@RequestParam(value = "kollFileId", required = false) String sKollFileId,
										@RequestParam(value = "isActive", required = false) String isActive,
										@RequestParam(value = "typeTriggerId", required = false) String typeTriggerId,
										HttpSession session) throws KommetException
	{
		clearMessages();
		KID typeId = null;
		try
		{
			typeId = KID.get(sTypeId);
		}
		catch (KommetException e)
		{
			return getErrorPage("Invalid type ID " + sTypeId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		Type type = env.getType(typeId);
		if (type == null)
		{
			addError("Type with ID " + typeId + " does not exist");
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("triggers/edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("type", type);
			mv.addObject("kollFiles", TriggerUtil.getTriggerCandidates(type, true, classService, compiler, env));
			return mv;
		}
		
		TypeTrigger typeTrigger = null;
		if (StringUtils.hasText(typeTriggerId))
		{
			typeTrigger = triggerService.getById(KID.get(typeTriggerId), env);
			typeTrigger.setIsActive("true".equals(isActive));
			throw new NotImplementedException("Editing type trigger not implemented");
		}
		else
		{
			typeTrigger = triggerService.registerTriggerWithType(classService.getClass(KID.get(sKollFileId), env), type, false, "true".equals(isActive), AuthUtil.getAuthData(session), env);
			return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/trigger/" + typeTrigger.getId());
		}
	}*/
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/triggers/new/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView newTrigger(@PathVariable("keyPrefix") String sKeyPrefix, HttpSession session) throws KommetException
	{
		KeyPrefix keyPrefix = null;
		try
		{
			keyPrefix = KeyPrefix.get(sKeyPrefix);
		}
		catch (KeyPrefixException e)
		{
			return getErrorPage("Invalid object prefix " + sKeyPrefix);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		Type type = env.getType(keyPrefix);
		if (type == null)
		{
			return getErrorPage("Type with prefix " + keyPrefix.getPrefix() + " not found");
		}
		
		ModelAndView mv = new ModelAndView("triggers/edit");
		mv.addObject("type", type);
		mv.addObject("kollFiles", TriggerUtil.getTriggerCandidates(type, true, classService, compiler, env));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/trigger/unregister/{typeTriggerId}", method = RequestMethod.POST)
	@ResponseBody
	public void unregister (@PathVariable("typeTriggerId") String sTypeTriggerId, HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		
		KID typeTriggerId = null;
		
		try
		{
			typeTriggerId = KID.get(sTypeTriggerId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid type trigger assignment ID " + sTypeTriggerId));
			return;
		}
		
		triggerService.unregisterTriggerWithType(typeTriggerId, envService.getCurrentEnv(session));
		out.write(getSuccessJSON("Trigger unregistered"));
		return;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/trigger/{typeTriggerId}", method = RequestMethod.GET)
	public ModelAndView typeTriggerDetails(@PathVariable("typeTriggerId") String sTypeTriggerId, HttpSession session) throws KommetException
	{
		KID typeTriggerId = null;
		try
		{
			typeTriggerId = KID.get(sTypeTriggerId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid trigger ID " + sTypeTriggerId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		TypeTrigger typeTrigger = triggerService.getById(typeTriggerId, env);
		ModelAndView mv = new ModelAndView("triggers/details");
		mv.addObject("typeTrigger", typeTrigger);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/triggers/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView showTriggersForType(@PathVariable("keyPrefix") String sKeyPrefix, HttpSession session) throws KommetException
	{
		KeyPrefix keyPrefix = null;
		try
		{
			keyPrefix = KeyPrefix.get(sKeyPrefix);
		}
		catch (KeyPrefixException e)
		{
			return getErrorPage("Invalid object prefix " + sKeyPrefix);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		Type type = env.getType(keyPrefix);
		if (type == null)
		{
			return getErrorPage("Type with prefix " + keyPrefix.getPrefix() + " not found");
		}
		
		ModelAndView mv = new ModelAndView("types/triggers");
		mv.addObject("triggers", env.getTriggers(type.getKID()).values());
		mv.addObject("typeId", type.getKID());
		return mv;
	}
}