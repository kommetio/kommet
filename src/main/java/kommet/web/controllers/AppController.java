/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

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
import kommet.basic.App;
import kommet.basic.AppUrl;
import kommet.basic.Profile;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.ValidationMessage;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.AppFilter;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.AppService;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class AppController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	AppService appService;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/apps/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session) throws KommetException
	{
		return new ModelAndView("apps/list");
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/appurls/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveAppURL(@RequestParam(required = false, value = "appId") String sAppId,
							@RequestParam(required = false, value = "url") String url,
							HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sAppId))
		{
			out.write(RestUtil.getRestErrorResponse("App id not specified"));
			return;
		}
		
		if (!StringUtils.hasText(url))
		{
			out.write(RestUtil.getRestErrorResponse("URL not specified"));
			return;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		AppUrl appURL = new AppUrl();
		appURL.setApp(appService.get(KID.get(sAppId), authData, env));
		appURL.setUrl(url);
		appService.save(appURL, authData, env, envService.getMasterEnv());
				
		out.write(RestUtil.getRestSuccessResponse("App URL created"));
		return;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/apps/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteApp(@RequestParam(required = false, value = "id") String sAppId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sAppId))
		{
			out.write(RestUtil.getRestErrorResponse("App id not specified"));
			return;
		}
		
		KID appId = null;
		
		try
		{
			appId = KID.get(sAppId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid app ID '" + sAppId + "'"));
			return;
		}
		
		// delete app URL by id
		appService.delete(appId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session), envService.getMasterEnv());
		
		out.write(RestUtil.getRestSuccessResponse("App deleted"));
		return;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/appurls/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteAppURL(@RequestParam(required = false, value = "id") String sAppUrlId,
									HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sAppUrlId))
		{
			out.write(RestUtil.getRestErrorResponse("App URL id not specified"));
			return;
		}
		
		KID appUrlId = null;
		
		try
		{
			appUrlId = KID.get(sAppUrlId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid app URL id '" + sAppUrlId + "'"));
			return;
		}
		
		// delete app URL by id
		appService.deleteAppUrl(appUrlId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session), envService.getMasterEnv());
		
		out.write(RestUtil.getRestSuccessResponse("App URL deleted"));
		return;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/apps/new", method = RequestMethod.GET)
	public ModelAndView create(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("apps/edit");
		mv.addObject("pageTitle", "New app");
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/apps/save", method = RequestMethod.POST)
	public ModelAndView save(@RequestParam(required = false, value = "appId") String sAppId,
							@RequestParam(required = false, value = "name") String name,
							@RequestParam(required = false, value = "label") String label,
							@RequestParam(required = false, value = "type") String type,
							@RequestParam(required = false, value = "landingUrl") String landingUrl,
							HttpSession session) throws KommetException
	{
		clearMessages();
		
		App app = null;
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (StringUtils.hasText(sAppId))
		{
			KID appId = null;
			
			try
			{
				appId = KID.get(sAppId);
			}
			catch (KIDException e)
			{
				return getErrorPage("Invalid app ID " + sAppId);
			}
			
			app = appService.get(appId, authData, env);
		}
		else
		{
			app = new App();
		}
		
		app.setName(name);
		app.setLandingUrl(landingUrl);
		app.setType(type);
		app.setLabel(label);
		
		// validate
		if (!StringUtils.hasText(name))
		{
			addError("App name must be specified");
		}
		else if (!ValidationUtil.isValidOptionallyQualifiedResourceName(name))
		{
			addError("Invalid app name");
		}
		
		if (!StringUtils.hasText(label))
		{
			addError("App label must be specified");
		}
		
		if (!StringUtils.hasText(type))
		{
			addError("App type must be specified");
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("apps/edit");
			mv.addObject("app", app);
			mv.addObject("pageTitle", app.getId() != null ? app.getLabel() + " - edit" : "New app");
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		try
		{
			// save app
			app = appService.save(app, authData, env);
		}
		catch (FieldValidationException e)
		{
			for (ValidationMessage msg : e.getMessages())
      		{
      			addError(msg.getText());
      		}
			
			ModelAndView mv = new ModelAndView("apps/edit");
			mv.addObject("app", app);
			mv.addObject("pageTitle", app.getName() + " - edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		catch (Exception e)
		{
			ModelAndView mv = new ModelAndView("apps/edit");
			mv.addObject("app", app);
			mv.addObject("pageTitle", app.getName() + " - edit");
			mv.addObject("errorMsgs", getMessage(e.getMessage()));
			return mv;
		}
		
		// redirect to app details
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/apps/" + app.getId());
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/apps/edit/{appId}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("appId") String sAppId, HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID appId = null;
		try
		{
			appId = KID.get(sAppId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid app ID " + sAppId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		// find app
		App app = appService.get(appId, authData, env);
		if (app == null)
		{
			return getErrorPage("App with ID " + appId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("apps/edit");
		mv.addObject("app", app);
		mv.addObject("pageTitle", app.getLabel() + " - edit");
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/apps/{appId}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("appId") String sAppId, HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID appId = null;
		try
		{
			appId = KID.get(sAppId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid app ID " + sAppId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		AppFilter filter = new AppFilter();
		filter.addAppId(appId);
		
		// find app
		List<App> apps = appService.find(filter, authData, env);
		if (apps.isEmpty())
		{
			return getErrorPage("App with ID " + appId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("apps/details");
		mv.addObject("app", apps.get(0));
		return mv;
	}
}