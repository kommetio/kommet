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
import kommet.basic.Layout;
import kommet.basic.Profile;
import kommet.basic.RecordAccessType;
import kommet.basic.keetle.LayoutService;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.SystemSettingService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class LayoutController extends CommonKommetController
{
	@Inject
	LayoutService layoutService;
	
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig config;
	
	@Inject
	SystemSettingService systemSettingService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layouts/settings", method = RequestMethod.GET)
	public ModelAndView settingsView (HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("layouts/settings");
		mv.addObject("defaultLayout", layoutService.getDefaultLayoutName(AuthUtil.getAuthData(session), envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layouts/editsettings", method = RequestMethod.GET)
	public ModelAndView settingsEdit (HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("layouts/editsettings");
		mv.addObject("defaultLayout", layoutService.getDefaultLayoutName(AuthUtil.getAuthData(session), envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layouts/savesettings", method = RequestMethod.POST)
	public ModelAndView saveSettings (@RequestParam(value = "defaultLayout", required = false) String defaultLayoutName,
										HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		Layout defaultLayout = null;
		
		if (StringUtils.hasText(defaultLayoutName))
		{
			// find default layout
			defaultLayout = layoutService.getByName(defaultLayoutName, env);
		}
		
		ModelAndView mv = new ModelAndView("layouts/settings");
		layoutService.setDefaultLayout(defaultLayout, AuthUtil.getAuthData(session), env);
		
		mv.addObject("defaultLayout", defaultLayoutName);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layouts/list", method = RequestMethod.GET)
	public ModelAndView list (HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("layouts/list");
		
		EnvData env = envService.getCurrentEnv(session);
		 
		mv.addObject("layouts", layoutService.find(null, env));
		return mv;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layouts/new", method = RequestMethod.GET)
	public ModelAndView newLayout (HttpSession session) throws KommetException
	{
		return new ModelAndView("layouts/edit");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layouts/{layoutId}", method = RequestMethod.GET)
	public ModelAndView details (@PathVariable("layoutId") String layoutId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("layouts/details");
		
		Layout layout = layoutService.getById(KID.get(layoutId), envService.getCurrentEnv(session));
		
		mv.addObject("layout", layout);
		mv.addObject("canEdit", RecordAccessType.PUBLIC.getId() == layout.getAccessType());
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layouts/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete(@RequestParam(required = false, value = "id") String sLayoutId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sLayoutId))
		{
			out.write(RestUtil.getRestErrorResponse("Layout id not specified"));
			return;
		}
		
		KID layoutId = null;
		
		try
		{
			layoutId = KID.get(sLayoutId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid app ID '" + sLayoutId + "'"));
			return;
		}
		
		// delete app URL by id
		layoutService.delete(layoutId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		
		out.write(RestUtil.getRestSuccessResponse("Layout deleted"));
		return;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layouts/edit/{layoutId}", method = RequestMethod.GET)
	public ModelAndView edit (@PathVariable("layoutId") String layoutId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("layouts/edit");
		mv.addObject("layout", layoutService.getById(KID.get(layoutId), envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layout/save", method = RequestMethod.POST)
	public ModelAndView save (@RequestParam(value = "layoutId", required = false) String layoutId,
								@RequestParam(value = "name", required = false) String name,
								@RequestParam(value = "package", required = false) String packageName,
								HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID id = null;
		EnvData env = envService.getCurrentEnv(session);
		Layout layout = null;
		
		if (StringUtils.hasText(layoutId))
		{
			id = KID.get(layoutId);
			layout = layoutService.getById(id, env);
		}
		else
		{
			layout = new Layout();
			layout.setCode(LayoutService.getEmptyLayoutCode(name));
		}
		
		layout.setName(name);
		boolean isNameEmpty = !validateNotEmpty(name, "Layout name");
		
		if (!isNameEmpty && !ValidationUtil.isValidOptionallyQualifiedResourceName(name))
		{
			addError("Layout name can contain only letters, digits and an underscore. It must not start with a digit, or start or end with an underscore.");
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("layouts/edit");
			mv.addObject("layout", layout);
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		// save view
		layout = layoutService.save(layout, AuthUtil.getAuthData(session), env);
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/layouts/" + layout.getId());
	}
}