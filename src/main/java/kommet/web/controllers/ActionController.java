/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Action;
import kommet.basic.Class;
import kommet.basic.Profile;
import kommet.basic.View;
import kommet.basic.actions.ActionCreationException;
import kommet.basic.actions.ActionFilter;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.OperationResult;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.env.GenericAction;
import kommet.json.JSON;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.SystemSettingService;
import kommet.systemsettings.SystemSettingKey;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class ActionController extends CommonKommetController
{
	@Inject
	ActionService actionService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ClassService classService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	SystemSettingService settingService;
	
	@Inject
	AppConfig appConfig;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/actions/list", method = RequestMethod.GET)
	public ModelAndView list (HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("actions/list");
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/actions/new", method = RequestMethod.GET)
	public ModelAndView newAction (HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("actions/edit");
		mv.addObject("controllerActionMethodsJSON", getControllerActionMethodsJSON(envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/actions/edit/{pageId}", method = RequestMethod.GET)
	public ModelAndView actionEdit (@PathVariable("pageId") String actionId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("actions/edit");
		
		EnvData env = envService.getCurrentEnv(session);
		
		mv.addObject("page", actionService.getAction(KID.get(actionId), env));
		mv.addObject("controllerActionMethodsJSON", getControllerActionMethodsJSON(env));
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/actions/{pageId}", method = RequestMethod.GET)
	public ModelAndView actionDetails (@PathVariable("pageId") String pageId, HttpSession session, HttpServletRequest req) throws KommetException
	{
		ModelAndView mv = new ModelAndView("actions/details");
		Action action = actionService.getAction(KID.get(pageId), envService.getCurrentEnv(session));
		
		Breadcrumbs.add(req.getRequestURL().toString(), action.getName(), appConfig.getBreadcrumbMax(), session);
		
		return initActionPropertiesForDisplay(mv, action, envService.getCurrentEnv(session));
	}
	
	private ModelAndView initActionPropertiesForDisplay (ModelAndView mv, Action action, EnvData env) throws KommetException
	{
		mv.addObject("page", action);
		
		// convert controller package name to user-defined
		String packageName = action.getController().getPackageName();
		String controllerName =  (StringUtils.hasText(packageName) ? packageName + "." : "") + action.getController().getName();
		mv.addObject("controllerName", controllerName);
		mv.addObject("controllerId", action.getController().getId());
		
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/actions/search", method = RequestMethod.GET)
	@ResponseBody
	public void search (@RequestParam(value = "keyword", required = false) String keyword,
						@RequestParam(value = "showSystemActions", required = false) String sShowSystemActions, HttpServletResponse resp, HttpSession session) throws IOException, KommetException
	{
		PrintWriter out = resp.getWriter();
		
		ActionFilter filter = new ActionFilter();
		filter.setIsSystem("true".equals(sShowSystemActions));

		if (StringUtils.hasText(keyword))
		{
			filter.setNameOrUrl(keyword);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		List<ActionWrapper> returnedActions = new ArrayList<ActionController.ActionWrapper>();
		
		List<Action> actions = actionService.getActions(filter, env);
		
		for (Action action : actions)
		{
			ActionWrapper wrapper = new ActionWrapper();
			wrapper.setId(action.getId());
			wrapper.setCreatedDate(action.getCreatedDate());
			wrapper.setInterpretedName(action.getInterpretedName());
			wrapper.setIsSystem(action.getIsSystem());
			wrapper.setIsGeneric(false);
			wrapper.setIsRest(false);
			wrapper.setUrl(action.getUrl());
			wrapper.setControllerId(action.getController().getId());
			wrapper.setActionMethod(action.getControllerMethod());
			wrapper.setControllerName(action.getController().getName());
			returnedActions.add(wrapper);
		}
		
		// now search generic actions
		for (GenericAction action : env.getGenericActions().values())
		{
			if (!StringUtils.hasText(keyword) || action.getUrl().toLowerCase().contains(keyword.toLowerCase()))
			{
				ActionWrapper wrapper = new ActionWrapper();
				wrapper.setIsGeneric(true);
				wrapper.setUrl(action.getUrl());
				wrapper.setControllerId(action.getControllerClassId());
				wrapper.setActionMethod(action.getActionMethod());
				wrapper.setIsRest(action.isRest());
				wrapper.setControllerName(action.getControllerName());
				returnedActions.add(wrapper);
			}
		}
		
		out.write(RestUtil.getRestSuccessDataResponse(JSON.serialize(returnedActions, AuthUtil.getAuthData(session))));
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/actions/save", method = RequestMethod.POST)
	public ModelAndView saveAction (@RequestParam(value = "pageId", required = false) String actionId,
								@RequestParam(value = "url", required = false) String url,
								@RequestParam(value = "name", required = false) String name,
								@RequestParam(value = "controllerId", required = false) String controllerId,
								@RequestParam(value = "controllerMethod", required = false) String controllerMethod,
								@RequestParam(value = "viewId", required = false) String viewId,
								@RequestParam(value = "isPublic", required = false) String isPublic,
								HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID id = null;
		EnvData env = envService.getCurrentEnv(session);
		
		Action action = null;
		
		if (StringUtils.hasText(actionId))
		{
			id = KID.get(actionId);
			action = actionService.getAction(id, env);
		}
		else
		{
			action = new Action();
			action.setIsSystem(false);
		}
		
		action.setIsPublic("true".equals(isPublic));
		
		if (!StringUtils.hasText(url))
		{
			addError("Action URL is required");
		}
		
		// remember the old URL because a page previously existing under this URL in the env store needs
		// to be removed and the newly saved page has to be stored at the new URL
		String oldUrl = action.getUrl();
		action.setUrl(url);
		
		validateNotEmpty(controllerMethod, "Controller method");
		action.setControllerMethod(controllerMethod);
		
		validateNotEmpty(name, "Name");
		
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(name))
		{
			addError("Invalid action name " + action.getName());
		}
		
		action.setName(name);
		
		if (!StringUtils.hasText(controllerId))
		{
			addError("Class file for the action is required");
		}
		else
		{
			Class controllerFile = classService.getClass(KID.get(controllerId), env);
			action.setController(controllerFile);
			
			if (controllerFile == null)
			{
				addError("The class file specified for this action does not exist");
			}
			else if (StringUtils.hasText(controllerMethod))
			{	
				Method actionMethod = null;
				try
				{
					actionMethod = classService.getActionMethod(controllerFile, controllerMethod, env);
				}
				catch (KommetException e)
				{
					addError(e.getMessage());
				}
				
				if (actionMethod == null)
				{
					addError("There is no method '" + controllerMethod + "' annotated with @Action in controller class " + controllerFile.getName());
				}
				
				action.setController(controllerFile);
			}
		}
		
		validateNotEmpty(viewId, "View");
		
		try
		{
			// get view to have all its data, not just ID
			View view = viewService.getView(KID.get(viewId), env);
			action.setView(view);
		}
		catch (KIDException e)
		{
			addError("Incorrect value of View ID");
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("actions/edit");
			
			mv.addObject("views", viewService.getAllViews(env));
			mv.addObject("kollFiles", classService.getClasses(null, env));
			mv.addObject("page", action);
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("controllerActionMethodsJSON", getControllerActionMethodsJSON(env));
			return mv;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		try
		{
			// save the action
			action = actionService.saveOnEnv(action, oldUrl, authData, env);
		}
		catch (ActionCreationException e)
		{
			// display an error message depending on what caused the exception
			switch (e.getErrCode())
			{
				case ActionCreationException.ERR_CODE_DUPLICATE_GENERIC_ACTION_URL:
					addError(authData.getI18n().get("action.err.duplicateurl.generic"));
					break;
				case ActionCreationException.ERR_CODE_DUPLICATE_REGISTERED_ACTION_URL:
					addError(authData.getI18n().get("action.err.duplicateurl.registered"));
					break;
				case ActionCreationException.ERR_CODE_RESERVED_URL:
					addError(authData.getI18n().get("action.err.reservedurl"));
					break;
				default: addError(authData.getI18n().get("action.err.unknownsaveerror"));
			}
		}
		catch (Exception e)
		{
			addError(authData.getI18n().get("action.err.unknownsaveerror"));
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("actions/edit");
			
			mv.addObject("views", viewService.getAllViews(env));
			mv.addObject("kollFiles", classService.getClasses(null, env));
			mv.addObject("page", action);
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("controllerActionMethodsJSON", getControllerActionMethodsJSON(env));
			return mv;
		}
		
		ModelAndView mv = new ModelAndView("actions/details");
		mv.addObject("actionMsgs", getMessage("Action has been saved"));
		return initActionPropertiesForDisplay(mv, action, envService.getCurrentEnv(session));
	}
	
	private String getControllerActionMethodsJSON(EnvData env) throws KommetException
	{
		List<Class> classes = classService.getClasses(null, env);
		List<String> controllers = new ArrayList<String>();
		
		for (Class cls : classes)
		{
			java.lang.Class<?> javaClass = null;
			try
			{
				javaClass = compiler.getClass(cls, false, env);
			}
			catch (Exception e)
			{
				throw new KommetException("Could not get class " + cls.getQualifiedName() + " from environment");
			}
			
			if (javaClass.isAnnotationPresent(kommet.koll.annotations.Controller.class))
			{
				StringBuilder sb = new StringBuilder();
				sb.append("{ \"name\": \"").append(cls.getName()).append("\", \"package\": \"").append(cls.getPackageName()).append("\", \"id\": \"").append(cls.getId()).append("\"");
				
				List<String> serializedMethods = new ArrayList<String>();
				
				for (Method m : javaClass.getMethods())
				{
					if (m.isAnnotationPresent(kommet.koll.annotations.Action.class))
					{
						serializedMethods.add("\"" + m.getName() + "\"");
					}
				}
				
				sb.append(", \"methods\": [").append(MiscUtils.implode(serializedMethods, ", ")).append("] }");
				controllers.add(sb.toString());
			}
		}
		
		return "[ " + MiscUtils.implode(controllers, ", ") + " ]";
		
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/actions/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteAction(@RequestParam("pageId") String id, HttpSession session, HttpServletResponse response) throws KIDException, IOException
	{
		KID pageId = KID.get(id);
		EnvData env;
		PrintWriter out = response.getWriter();
		
		try
		{
			env = envService.getCurrentEnv(session);
			List<Action> pages = new ArrayList<Action>();
			pages.add(actionService.getAction(pageId, env));
			OperationResult result = actionService.delete(pages, "true".equals(settingService.getSettingValue(SystemSettingKey.REASSIGN_DEFAULT_ACTION_ON_ACTION_DELETE.toString(), env)), AuthUtil.getAuthData(session), env);
			
			if (result.isResult())
			{
				out.write(getSuccessJSON("Action deleted"));
			}
			else
			{
				out.write(getErrorJSON(result.getMessage()));
			}
			
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error deleting page: " + e.getMessage()));
		}
	}
	
	class ActionWrapper
	{
		private KID id;
		private String interpretedName;
		private Boolean isSystem;
		private Boolean isGeneric;
		private Boolean isRest;
		private String url;
		private Date createdDate;
		private KID controllerId;
		private String controllerName;
		private String actionMethod;
		
		public KID getId()
		{
			return id;
		}
		
		public void setId(KID id)
		{
			this.id = id;
		}
		
		public String getInterpretedName()
		{
			return interpretedName;
		}
		
		public void setInterpretedName(String interpretedName)
		{
			this.interpretedName = interpretedName;
		}
		
		public Boolean getIsSystem()
		{
			return isSystem;
		}
		
		public void setIsSystem(Boolean isSystem)
		{
			this.isSystem = isSystem;
		}
		
		public Date getCreatedDate()
		{
			return createdDate;
		}
		
		public void setCreatedDate(Date createdDate)
		{
			this.createdDate = createdDate;
		}

		public Boolean getIsGeneric()
		{
			return isGeneric;
		}

		public void setIsGeneric(Boolean isGeneric)
		{
			this.isGeneric = isGeneric;
		}

		public String getUrl()
		{
			return url;
		}

		public void setUrl(String url)
		{
			this.url = url;
		}

		public KID getControllerId()
		{
			return controllerId;
		}

		public void setControllerId(KID controllerId)
		{
			this.controllerId = controllerId;
		}

		public String getControllerName()
		{
			return controllerName;
		}

		public void setControllerName(String controllerName)
		{
			this.controllerName = controllerName;
		}

		public String getActionMethod()
		{
			return actionMethod;
		}

		public void setActionMethod(String actionMethod)
		{
			this.actionMethod = actionMethod;
		}

		public Boolean getIsRest()
		{
			return isRest;
		}

		public void setIsRest(Boolean isRest)
		{
			this.isRest = isRest;
		}
	}
}