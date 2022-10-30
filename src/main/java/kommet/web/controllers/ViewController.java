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
import java.util.List;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthUtil;
import kommet.basic.View;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.StandardObjectController;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.ViewUtil;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.UntypedRecord;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.utils.AppConfig;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;
import kommet.web.RequestAttributes;

@Controller
public class ViewController extends CommonKommetController
{
	@Inject
	ViewService viewService;
	
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig appConfig;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/views/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteView (@RequestParam(value = "viewId", required = false) String sViewId, HttpSession session, HttpServletResponse response) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		KID viewId = null;
		try
		{
			viewId = KID.get(sViewId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid view ID " + sViewId));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		View view = viewService.getView(viewId, env);
		
		if (view == null)
		{
			out.write(getErrorJSON("View with ID " + sViewId + " not found"));
			return;
		}
		
		if (view.getIsSystem() == true || view.getTypeId() != null)
		{
			out.write(getErrorJSON("This is a system field and cannot be deleted"));
			return;
		}
		
		try
		{
			viewService.deleteView(view, env);
			out.write(getSuccessJSON("View has been deleted"));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error deleting view: " + e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/views/list", method = RequestMethod.GET)
	public ModelAndView list (HttpSession session, HttpServletRequest req) throws KommetException
	{
		ModelAndView mv = new ModelAndView("views/list");
		
		EnvData env = envService.getCurrentEnv(session);
		
		List<View> views = viewService.getViews(null, env); 
		mv.addObject("views", views);
		
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "Views", appConfig.getBreadcrumbMax(), session);
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/views/new", method = RequestMethod.GET)
	public ModelAndView newView (HttpSession session) throws KommetException
	{
		return new ModelAndView("views/edit");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/views/{viewId}", method = RequestMethod.GET)
	public ModelAndView viewDetails (@PathVariable("viewId") String sViewId, HttpSession session) throws KommetException
	{
		KID viewId = null;
		try
		{
			viewId = KID.get(sViewId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid view ID " + sViewId);
		}
		
		ModelAndView mv = new ModelAndView("views/details");
		View view = viewService.getView(viewId, envService.getCurrentEnv(session));
		
		if (view == null)
		{
			return getErrorPage("View with ID " + sViewId + " not found");
		}
		
		mv.addObject("view", view);
		
		// tells if this view represents type details/edit
		mv.addObject("isTypeDetails", view.getKeetleCode().contains("<km:objectDetails "));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/views/edit/{viewId}", method = RequestMethod.GET)
	public ModelAndView viewEdit (@PathVariable("viewId") String viewId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("views/edit");
		mv.addObject("view", viewService.getView(KID.get(viewId), envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/views/editor/{viewId}", method = RequestMethod.GET)
	public ModelAndView openViewEditor (@PathVariable("viewId") String viewId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("views/editor");
		mv.addObject("view", viewService.getView(KID.get(viewId), envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/views/preview", method = RequestMethod.GET)
	public void preview (@RequestParam(value = "viewId", required = false) String viewId, HttpSession session, HttpServletRequest request, HttpServletResponse response) throws KommetException, ServletException, IOException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		//View view = viewService.getView(KID.get(viewId), env);
		
		PageData pageData = new PageData(env);
		Record mockRecord = new UntypedRecord();
		pageData.setValue(StandardObjectController.RECORD_VAR_PARAM, mockRecord);
		pageData.setValue(PageData.ACTION_MSGS_KEY, new ArrayList<String>());
		pageData.setValue(PageData.ERROR_MSGS_KEY, new ArrayList<String>());
		
		request.setAttribute(RequestAttributes.PAGE_DATA_ATTR_NAME, pageData);
		
		RequestDispatcher dispatcher = request.getRequestDispatcher("/" + env.getKeetleDir(appConfig.getRelativeKeetleDir()) + "/" + viewId + ".jsp");
		dispatcher.forward(request, response);
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/views/create", method = RequestMethod.POST)
	public ModelAndView createView (@RequestParam(value = "name", required = false) String name,
								@RequestParam(value = "package", required = false) String packageName,
								HttpSession session) throws KommetException
	{
		clearMessages();
		View view = new View();
		
		boolean isViewEmpty = !validateNotEmpty(name, "View name");
		
		if (!isViewEmpty && !ValidationUtil.isValidResourceName(name))
		{
			addError("Invalid view name. " + ValidationUtil.INVALID_RESOURCE_ERROR_EXPLANATION);
		}
		
		validateNotEmpty(packageName, "Package name");
		
		view.setName(name);
		EnvData env = envService.getCurrentEnv(session);
		view.setPackageName(packageName);
		
		// check if view does not exist
		ViewFilter filter = new ViewFilter();
		filter.setName(name);
		filter.setPackage(view.getPackageName());
		
		if (!viewService.getViews(filter, env).isEmpty())
		{
			addError("A view with the given name and package already exists.");
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("views/edit");
			view.setPackageName(packageName);
			mv.addObject("view", view);
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		view.setPath(name);
		view.setIsSystem(false);
		// set default content for view
		view.initKeetleCode(ViewUtil.getEmptyViewCode(view.getName(), packageName), appConfig, env);
		view = viewService.save(view, appConfig, AuthUtil.getAuthData(session), env);
		
		// store updated view on disk
		viewService.storeView(view, env);
		
		// opem the new view in IDE
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/ide/" + view.getId());
	}
	
	/*@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/views/save", method = RequestMethod.POST)
	public ModelAndView saveView (@RequestParam(value = "viewId", required = false) String viewId,
								@RequestParam(value = "code", required = false) String code,
								@RequestParam(value = "name", required = false) String name,
								@RequestParam(value = "package", required = false) String packageName,
								HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID id = null;
		EnvData env = envService.getCurrentEnv(session);
		KeetleView view = null;
		
		if (StringUtils.hasText(viewId))
		{
			id = KID.get(viewId);
			view = keetleService.getView(id, env);
		}
		else
		{
			view = new KeetleView();
			view.setIsSystem(false);
		}
		
		validateNotEmpty(name, "View name");
		
		if (!ValidationUtil.isValidResourceName(name))
		{
			addError("View name can contain only letters, digits and an underscore. It must not start with a digit, or start or end with an underscore.");
		}
		
		view.setName(name);
		// the view is placed in the base directory of the env, so its path is equal to its name
		view.setPath(name);
		
		// set properties from request
		if (!StringUtils.hasText(code))
		{
			addError("Code is required - a view cannot be empty");
		}
		view.setKeetleCode(code);
		
		if (!StringUtils.hasText(packageName))
		{
			addError("Package name is required");
		}
		view.setPackageName(packageName);
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("views/edit");
			mv.addObject("view", view);
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		// save view
		view = keetleService.save(view, AuthUtil.getAuthData(session).getUserId(), env);
		
		// store updated view on disk
		keetleService.storeView(view, env);
		
		ModelAndView mv = new ModelAndView("views/details");
		mv.addObject("view", view);
		mv.addObject("actionMsgs", getMessage("View has been saved"));
		return mv;
	}*/
}