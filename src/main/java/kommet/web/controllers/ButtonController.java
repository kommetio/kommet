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

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Action;
import kommet.basic.BasicSetupService;
import kommet.basic.Button;
import kommet.basic.Profile;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.ButtonService;
import kommet.utils.UrlUtil;

@Controller
public class ButtonController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	ButtonService buttonService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/buttons/new", method = RequestMethod.GET)
	public ModelAndView create(@RequestParam(value = "typeId", required = false) String sTypeId, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("buttons/edit");
		mv.addObject("pageTitle", "New button");
		
		if (StringUtils.hasText(sTypeId))
		{
			mv.addObject("type", env.getType(KID.get(sTypeId)));
		}
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/buttons/{buttonId}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("buttonId") String sButtonId, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		Button button = buttonService.get(KID.get(sButtonId), AuthUtil.getAuthData(session), env);
		if (button == null)
		{
			throw new KommetException("Button with ID" + sButtonId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("buttons/details");
		mv.addObject("pageTitle", "New button");
		mv.addObject("button", button);
		mv.addObject("type", env.getType(button.getTypeId()));
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/buttons/edit/{buttonId}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("buttonId") String sButtonId, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		Button button = buttonService.get(KID.get(sButtonId), AuthUtil.getAuthData(session), env);
		if (button == null)
		{
			throw new KommetException("Button with ID" + sButtonId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("buttons/edit");
		mv.addObject("pageTitle", button.getLabel());
		mv.addObject("button", button);
		mv.addObject("type", env.getType(button.getTypeId()));
		
		String actionType = null;
		if (button.getUrl() != null)
		{
			actionType = "url";
		}
		else if (button.getOnClick() != null)
		{
			actionType = "onClick";
		}
		else if (button.getAction() != null)
		{
			actionType = "action";
		}
		
		mv.addObject("actionType", actionType);
		
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/buttons/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteButton(@RequestParam(required = false, value = "id") String sButtonId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sButtonId))
		{
			out.write(RestUtil.getRestErrorResponse("App id not specified"));
			return;
		}
		
		KID buttonId = null;
		
		try
		{
			buttonId = KID.get(sButtonId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid button ID '" + sButtonId + "'"));
			return;
		}
		
		// delete app URL by id
		buttonService.delete(buttonId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		
		out.write(RestUtil.getRestSuccessResponse("Button deleted"));
		return;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/buttons/save", method = RequestMethod.POST)
	@ResponseBody
	public void save(@RequestParam(value = "typeId", required = false) String sTypeId,
					@RequestParam(value = "buttonId", required = false) String sButtonId,
					@RequestParam(value = "label", required = false) String label,
					@RequestParam(value = "labelKey", required = false) String labelKey,
					@RequestParam(value = "name", required = false) String name,
					@RequestParam(value = "url", required = false) String url,
					@RequestParam(value = "onClick", required = false) String onClick,
					@RequestParam(value = "actionId", required = false) String sActionId,
					@RequestParam(value = "actionType", required = false) String actionType,
					@RequestParam(value = "displayCondition", required = false) String displayCondition,
					HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		clearMessages();
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		Button button = new Button();
		
		if (StringUtils.hasText(sButtonId))
		{
			button = buttonService.get(KID.get(sButtonId), authData, env);
		}
		
		if (!StringUtils.hasText(name))
		{
			addError("Please select button name");
		}
		
		if (!StringUtils.hasText(label))
		{
			addError("Please select button label");
		}
		
		if (!StringUtils.hasText(sTypeId))
		{
			addError("Please select type");
			button.setTypeId(null);
		}
		else
		{
			button.setTypeId(KID.get(sTypeId));
		}
		
		button.setName(name);
		button.setLabelKey(StringUtils.hasText(labelKey) ? labelKey : null);
		button.setLabel(StringUtils.hasText(label) ? label : null);
		button.setDisplayCondition(StringUtils.hasText(displayCondition) ? displayCondition : null);
		
		if (StringUtils.hasText(actionType))
		{
			if ("action".equals(actionType))
			{
				if (StringUtils.hasText(sActionId))
				{
					Action action = new Action();
					action.setId(KID.get(sActionId));
					button.setAction(action);
				}
				else
				{
					addError("Please select action to be called by the button");
				}
				
				button.setUrl(null);
				button.setOnClick(null);
			}
			else if ("url".equals(actionType))
			{
				button.setUrl(url);
				button.setAction(null);
				button.setOnClick(null);
			}
			else if ("onClick".equals(actionType))
			{
				button.setUrl(null);
				button.setAction(null);
				button.setOnClick(onClick);
			}
		}
		else
		{
			addError("Please select type of action to be called by this button");
		}
		
		PrintWriter out = resp.getWriter();
		
		if (hasErrorMessages())
		{
			out.write(RestUtil.getRestErrorResponse(getErrorMsgs()));
			return;
		}
		
		try
		{
			button = buttonService.save(button, authData, env);
			out.write(RestUtil.getRestSuccessDataResponse("{ \"buttonId\": \"" + button.getId() + "\" }"));
			return;
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse(e.getMessage()));
			return;
		}
	}
}