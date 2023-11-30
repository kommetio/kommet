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

import kommet.auth.AuthUtil;
import kommet.basic.ViewResource;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.rest.RestUtil;
import kommet.services.ViewResourceService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class ViewResourceController extends CommonKommetController
{
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	EnvService envService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/viewresources/save", method = RequestMethod.POST)
	public ModelAndView save (@RequestParam(value = "mimeType", required = false) String mimeType,
			@RequestParam(value = "resourceName", required = false) String resourceName,
			@RequestParam(value = "resourceId", required = false) String sResourceId,
            HttpSession session) throws KommetException, IOException
	{
		clearMessages();
		
		ViewResource resource = new ViewResource();
		EnvData env = envService.getCurrentEnv(session);
		
		if (StringUtils.hasText(sResourceId))
		{
			// get existing resource
			resource = viewResourceService.get(KID.get(sResourceId), env);
		}
		else
		{
			// generate random disk file name
			resource.setPath(MiscUtils.getHash(30));
		}
		
		resource.setMimeType(mimeType);
		resource.setName(resourceName);
		
		if (!StringUtils.hasText(mimeType))
		{
			addError("MIME type not specified");
		}
		
		if (!StringUtils.hasText(resourceName))
		{
			addError("Name not specified");
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("viewresources/edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("resource", resource);
			return mv;
		}
		
		// save resource
		resource = viewResourceService.save(resource, AuthUtil.getAuthData(session), env);
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/viewresources/" + resource.getId());
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/viewresources/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete(@RequestParam("resourceId") String id, HttpSession session, HttpServletResponse response) throws IOException, KommetException
	{
		KID resourceId = KID.get(id);
		PrintWriter out = response.getWriter();
		
		EnvData env = envService.getCurrentEnv(session);
		
		try
		{
			viewResourceService.delete(resourceId, AuthUtil.getAuthData(session), env);
			out.write(RestUtil.getRestSuccessResponse("View resource deleted"));
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse("Deleting view resource failed. Nested: " + e.getMessage()));
		}	
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/viewresources/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session, HttpServletRequest req) throws KommetException
	{ 	
		ModelAndView mv = new ModelAndView("viewresources/list");
		Breadcrumbs.add(req.getRequestURL().toString(), "View Resources", appConfig.getBreadcrumbMax(), session, getContextPath(session));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/viewresources/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("id") String resourceId, HttpSession session, HttpServletRequest req) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		ViewResource resource = viewResourceService.get(KID.get(resourceId), env);
		
		if (resource == null)
		{
			return getErrorPage("View resource with ID " + resourceId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("viewresources/details");
		Breadcrumbs.add(req.getRequestURL().toString(), "View Resource: " + resource.getName(), appConfig.getBreadcrumbMax(), session, getContextPath(session));
		mv.addObject("resource", resource);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/viewresources/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("id") String resourceId, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		ViewResource resource = viewResourceService.get(KID.get(resourceId), env);
		
		if (resource == null)
		{
			return getErrorPage("View resource with ID " + resourceId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("viewresources/edit");
		mv.addObject("resource", resource);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/viewresources/new", method = RequestMethod.GET)
	public ModelAndView create() throws KommetException
	{
		return new ModelAndView("viewresources/edit");
	}
}