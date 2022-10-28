/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import kommet.auth.ProfileService;
import kommet.basic.Action;
import kommet.basic.Class;
import kommet.basic.Profile;
import kommet.basic.RecordAccessType;
import kommet.basic.StandardAction;
import kommet.basic.TypeInfo;
import kommet.basic.View;
import kommet.basic.actions.ActionFilter;
import kommet.basic.actions.ActionService;
import kommet.basic.actions.StandardActionType;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.ViewUtil;
import kommet.basic.types.SystemTypes;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeInfoService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.js.jsrc.JSRC;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.koll.KollUtil;
import kommet.koll.compiler.KommetCompiler;
import kommet.rest.RestUtil;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class StandardActionController extends BasicRestController
{
	@Inject
	ActionService actionService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	TypeInfoService typeInfoService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	ClassService classService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ErrorLogService errorLog;
	
	@Inject
	AppConfig appConfig;
	
	private static final Logger log = LoggerFactory.getLogger(StandardActionController.class);
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/savepages/{keyPrefix}", method = RequestMethod.POST)
	public ModelAndView saveStandardActions (@PathVariable("keyPrefix") String keyPrefix, HttpSession session, HttpServletRequest req) throws KommetException
	{
		clearMessages();
		Map<?, ?> params = req.getParameterMap();
		Iterator<?> i = params.keySet().iterator();
		
		EnvData env = envService.getCurrentEnv(session);
		Type type = env.getType(KeyPrefix.get(keyPrefix));

		while ( i.hasNext() )
		{
			String key = (String)i.next();
			if (!key.startsWith("stdpage_"))
			{
				continue;
			}
			
			String value = ((String[])params.get(key))[0];
			
			if (!StringUtils.hasText(value))
			{
				// no page selected
				continue;
			}
			
			String[] bits = key.split("_");
			
			// the first bit is the profile ID
			KID profileId = KID.get(bits[1]);
			
			// the next bit is the view type
			StandardActionType pageType = StandardActionType.fromString(bits[2]);
			
			// set the new standard page for the profile
			actionService.setStandardAction(type.getKID(), KID.get(value), profileId, pageType, RecordAccessType.PUBLIC, AuthUtil.getAuthData(session), env);
		}
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/type/" + keyPrefix);
	}
	
	private ModelAndView prepareStandardActionView (ModelAndView mv, EnvData env, String keyPrefix) throws KommetException
	{
		Type type = env.getType(KeyPrefix.get(keyPrefix));
		
		if (type == null)
		{
			addError("Type with key prefix " + keyPrefix + " not found");
		}
		
		if (hasErrorMessages())
		{
			return getErrorPage(getErrorMsgs());
		}
		
		// get standard pages
		List<StandardAction> stdActions = actionService.getStandardActionsForType(type.getKID(), env);
		
		TypeInfo typeInfo = typeInfoService.getForType(type.getKID(), env);
		
		// group actions by profile
		mv.addObject("stdPagesByProfile", groupActionsByProfile(stdActions, typeInfo, profileService.getProfiles(env)));
		
		// get all profiles
		mv.addObject("profiles", profileService.getProfiles(env));
		mv.addObject("typePrefix", type.getKeyPrefix());
		mv.addObject("typeId", type.getKID());
		mv.addObject("typeInfo", typeInfo);
		
		return mv;
	}
	
	/**
	 * Action called when new default action setting is saved.
	 * @param sTypeId String form of the type ID for which pages are set
	 * @param sProfileId String form of the profile ID for which default pages are set
	 * @param action
	 * @param actionName
	 * @param controllerOption
	 * @param viewOption
	 * @param sControllerId
	 * @param newControllerName
	 * @param controllerMethod
	 * @param newViewName
	 * @param sViewId
	 * @param url
	 * @param usedAction
	 * @param sActionId
	 * @param session
	 * @param req
	 * @return
	 * @throws KommetException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/standardactions/savedefaultaction", method = RequestMethod.POST)
	public ModelAndView saveDefaultAction (@RequestParam("typeId") String sTypeId,
											@RequestParam("profileId") String sProfileId,
											@RequestParam("actionType") String actionType,
											@RequestParam(value = "actionName", required = false) String actionName,
											@RequestParam(value = "controllerOption", required = false) String controllerOption,
											@RequestParam(value = "viewOption", required = false) String viewOption,
											@RequestParam(value = "controllerId", required = false) String sControllerId,
											@RequestParam(value = "newControllerName", required = false) String newControllerName,
											@RequestParam(value = "controllerMethod", required = false) String controllerMethod,
											@RequestParam(value = "newViewName", required = false) String newViewName,
											@RequestParam(value = "viewId", required = false) String sViewId,
											@RequestParam(value = "url", required = false) String url,
											@RequestParam(value = "usedAction", required = false) String usedAction,
											@RequestParam(value = "pageId", required = false) String sActionId,
											@RequestParam(value = "profileScope", required = false) String profileScope,
											HttpSession session) throws KommetException
	{
		clearMessages();
		
		validateSaveDefaultActionInput(usedAction, sActionId, actionName, url, controllerOption, viewOption, controllerMethod, newControllerName, sControllerId, sViewId, newViewName);
		
		if (hasErrorMessages())
		{
			log.debug("Errors in input");
			return getSaveDefaultActionErrorPage(sTypeId, sProfileId, profileScope, actionType, sControllerId, newControllerName, controllerMethod, controllerOption, sViewId, actionName, url, newViewName, viewOption, usedAction, session);
		}
		
		KID controllerId = null;
		KID viewId = null;
		KID typeId = KID.get(sTypeId);
		KID profileId = KID.get(sProfileId);
		
		// the validateSaveDefaultPageInput has already checked that controller and view IDs are correct, so we can
		// just convert them to KID without any checks here - though they can be empty
		if (StringUtils.hasText(sControllerId))
		{
			controllerId = KID.get(sControllerId);
		}
		if (StringUtils.hasText(sViewId))
		{
			viewId = KID.get(sViewId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		// the ID of the page (existing or newly generated) that will be assigned to this standard action
		KID actionId = null;
		Type type = env.getType(typeId);
		
		// Generating a new action for this standard action
		if ("new".equals(usedAction))
		{	
			TypeInfo typeInfo = typeInfoService.getForType(typeId, env);
			if (typeInfo == null)
			{
				return getErrorPage("Type info not found for type " + typeId);
			}
			
			StandardActionType pageType = StandardActionType.fromString(actionType);
			Action existingDefaultAction = actionService.getAction(typeInfo.getDefaultAction(pageType).getId(), env);
			
			// create new page object
			Action action = new Action();
			action.setIsSystem(false);
			// the validation method has already checked that the name is filled
			action.setName(actionName);
			action.setUrl(url);
			action.setIsPublic(false);
			
			if ("new".equals(controllerOption))
			{
				// check if controller exists
				ClassFilter filter = new ClassFilter();
				filter.setQualifiedName(newControllerName);
				if (!classService.getClasses(filter, env).isEmpty())
				{
					addError("A controller with name " + newControllerName + " already exists");
					return getSaveDefaultActionErrorPage(sTypeId, sProfileId, profileScope, actionType, sControllerId, newControllerName, controllerMethod, controllerOption, sViewId, actionName, url, newViewName, viewOption, usedAction, session);
				}
				
				// generate new controller for the action
				Class newController = createController(newControllerName, controllerMethod, authData, env);
				action.setController(newController);
				action.setControllerMethod(controllerMethod);
			}
			else if ("existing".equals(controllerOption))
			{
				action.setController(classService.getClass(controllerId, env));
				action.setControllerMethod(controllerMethod);
			}
			else if ("default".equals(controllerOption))
			{
				action.setController(existingDefaultAction.getController());
				action.setControllerMethod(existingDefaultAction.getControllerMethod());
			}
			
			if ("new".equals(viewOption))
			{
				// check if view exists
				ViewFilter filter = new ViewFilter();
				filter.setName(newViewName);
				if (!viewService.getViews(filter, env).isEmpty())
				{
					addError("A view with name " + newViewName + " already exists");
					return getSaveDefaultActionErrorPage(sTypeId, sProfileId, profileScope, actionType, sControllerId, newControllerName, controllerMethod, controllerOption, sViewId, actionName, url, newViewName, viewOption, usedAction, session);
				}
				
				// generate new view for the action
				View newView = null;
				
				// the default action has only the view ID initialized, so we need to fetch the whole KOLL file
				existingDefaultAction.setView(viewService.getView(existingDefaultAction.getView().getId(), env));
				
				try
				{
					newView = copyView(existingDefaultAction.getView(), env);
				}
				catch (Exception e)
				{
					return getErrorPage("System internal error: could not copy standard view " + existingDefaultAction.getView().getId());
				}
				
				// change package to the package of the type for which the view is generated
				
				if (newViewName.contains("."))
				{
					List<String> viewNameParts = MiscUtils.splitByLastDot(newViewName);
					newView.setPackageName(viewNameParts.get(0));
					newView.setName(viewNameParts.get(1));
				}
				else
				{
					newView.setPackageName(type.getPackage());
					newView.setName(newViewName);
				}
				
				
				// generate the view anew with the new name and package
				newView.initKeetleCode(ViewUtil.getStandardView(type, newView.getName(), newView.getPackageName(), pageType, env), appConfig, env);
				
				// save the generated view and assign it to the action
				action.setView(viewService.save(newView, appConfig, authData, env));
				
				// If it was the edit view that was modified, it also needs to be assigned to the "Save" action.
				// This is because the Save action also uses the default edit view.
				if (pageType.equals(StandardActionType.EDIT))
				{
					ActionFilter actionFilter = new ActionFilter();
					String saveActionUrl = "save/" + type.getKeyPrefix();
					actionFilter.setUrl(saveActionUrl);
					List<Action> saveActions = actionService.getActions(actionFilter, env);
					if (saveActions.size() == 1)
					{
						saveActions.get(0).setView(newView);
						actionService.saveOnEnv(saveActions.get(0), saveActionUrl, authData, env);
					}
					else
					{
						throw new KommetException("Expected exactly one save action for type " + type.getQualifiedName() + ", but found " + saveActions.size());
					}
				}
				
				// store updated view on disk
				//keetleService.storeView(newView, env);
			}
			else if ("existing".equals(viewOption))
			{
				action.setView(viewService.getView(viewId, env));
			}
			else if ("default".equals(viewOption))
			{
				// keep the default view
				action.setView(existingDefaultAction.getView());
			}
			
			// save the new page
			actionId = actionService.saveOnEnv(action, null, true, authData, env).getId();
		}
		// using an existing action for this standard action
		else
		{
			if ("restoredefault".equals(usedAction))
			{
				// restore the default action for this profile, type and action type
				actionId = typeInfoService.getForType(type.getKID(), env).getDefaultAction(StandardActionType.fromString(actionType)).getId();
			}
			else
			{
				// the validate method has already checked that the ID is correct
				actionId = KID.get(sActionId);
			}
			
			// TODO validate if the action.controller.method takes parameter ID for details and edit actions
			// and parameter key prefix(?) for list action
		}
		
		if ("allProfiles".equals(profileScope))
		{
			// apply to all profiles
			for (Profile profile : profileService.getProfiles(env))
			{
				actionService.setStandardAction(typeId, actionId, profile.getId(), StandardActionType.fromString(actionType), RecordAccessType.PUBLIC, authData, env);
			}
		}
		else
		{
			// set the new action as standard for the given profile and action type
			actionService.setStandardAction(typeId, actionId, profileId, StandardActionType.fromString(actionType), RecordAccessType.PUBLIC, authData, env);
		}
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/type/" + type.getKeyPrefix() + "#rm.tab.2");
	}
	
	private Class createController(String controllerName, String controllerMethod, AuthData authData, EnvData env) throws KommetException
	{
		// split controller name
		List<String> nameParts = MiscUtils.splitByLastDot(controllerName);
		
		Class file = new Class();
		file.setName(nameParts.get(1));
		file.setPackageName(nameParts.get(0));
		file.setIsSystem(false);
		
		StringBuilder innerControllerCode = new StringBuilder();
		innerControllerCode.append("\t@" + kommet.koll.annotations.Action.class.getSimpleName()).append("\n");
		innerControllerCode.append("\tpublic ").append(PageData.class.getName()).append(" ").append(controllerMethod).append("()\n");
		innerControllerCode.append("\t{\n\t\treturn null;\n");
		innerControllerCode.append("\t}\n");

		// generate controller code with the given method
		String code = KollUtil.getTemplateKollCode(file.getName(), file.getPackageName(), KollUtil.getImports(), "@Controller", innerControllerCode.toString(), env);
		code = code.replace("public class " + file.getName(), "public class " + file.getName() + " extends " + BaseController.class.getSimpleName());
		file.setKollCode(code);
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(code, false, authData, env));
		
		return file;
	}

	/**
	 * Returns an error page for the "save default action" action.
	 * @param sTypeId
	 * @param sProfileId
	 * @param actionType
	 * @param sControllerId
	 * @param newControllerName
	 * @param controllerMethod
	 * @param controllerOption
	 * @param sViewId
	 * @param actionName
	 * @param url
	 * @param newViewName
	 * @param viewOption
	 * @param usedAction
	 * @param session
	 * @param req
	 * @return
	 * @throws KommetException
	 */
	private ModelAndView getSaveDefaultActionErrorPage(String sTypeId, String sProfileId, String profileScope, String actionType,
													String sControllerId, String newControllerName, String controllerMethod, String controllerOption, String sViewId, String actionName,
													String url, String newViewName, String viewOption, String usedAction,
													HttpSession session) throws KommetException
	{
		// remember error msgs because they will be erased by the modifyStandardActions method below
		List<String> errorMsgs = new ArrayList<String>();
		errorMsgs.addAll(getErrorMsgs());
		
		ModelAndView mv = modifyStandardActions(sTypeId, sProfileId, actionType, session);
		
		Action action = (Action)mv.getModel().get("page");
		
		// apply new values to the action record
		Class controller = new Class();
		if (StringUtils.hasText(sControllerId))
		{
			controller.setId(KID.get(sControllerId));
		}
		action.setController(controller);
		
		View view = new View();
		if (StringUtils.hasText(sViewId))
		{
			view.setId(KID.get(sViewId));
		}
		action.setView(view);
		
		mv.addObject("actionName", actionName);
		mv.addObject("url", url);
		mv.addObject("errorMsgs", errorMsgs);
		mv.addObject("newViewName", newViewName);
		mv.addObject("viewId", sViewId);
		mv.addObject("controllerMethod", controllerMethod);
		mv.addObject("newControllerName", newControllerName);
		mv.addObject("controllerId", sControllerId);
		mv.addObject("controllerOption", controllerOption);
		mv.addObject("viewOption", viewOption);
		mv.addObject("usedAction", usedAction);
		mv.addObject("profileScope", profileScope);
		return mv;
	}
	
	private View copyView(View source, EnvData env) throws KommetException
	{
		View dest = new View();
		dest.initKeetleCode(source.getKeetleCode(), appConfig, env);
		dest.setIsSystem(false);
		dest.setPackageName(source.getPackageName());
		dest.setPath(source.getName());
		
		// do not set package, users, modification dates, typeId or Id
		return dest;
	}

	/**
	 * Makes sure complete and correct data has been specified as input to the action saving
	 * the default action for a given profile.
	 * @param usedAction
	 * @param sActionId
	 * @param actionName
	 * @param url
	 * @param controllerOption
	 * @param viewOption
	 * @param controllerMethod
	 * @param newControllerName
	 * @param sControllerId
	 * @param sViewId
	 * @param newViewName
	 */
	private void validateSaveDefaultActionInput (String usedAction, String sActionId, String actionName, String url, String controllerOption, String viewOption, String controllerMethod, String newControllerName, 
												String sControllerId, String sViewId, String newViewName)
	{
		if (!StringUtils.hasText(usedAction))
		{
			addError("You have to select whether you want to use and existing or a new action");
		}
		else
		{
			if (usedAction.equals("existing"))
			{
				if (StringUtils.hasText(sActionId))
				{
					try
					{
						KID.get(sActionId);
					}
					catch (KIDException e)
					{
						addError("Incorrect action ID " + sActionId);
					}
				}
				else
				{
					addError("Please select the action to be used for this standard action");
				}
			}
			else if (!usedAction.equals("new") && !usedAction.equals("restoredefault"))
			{
				addError("Illegal used action option: only 'existing', 'new' and 'restoredefault' are allowed");
				// skip further validation
				return;
			}
		}
		
		// if we are using an existing action, no further validation needs to be done
		if ("existing".equals(usedAction) || "restoredefault".equals(usedAction))
		{
			return;
		}
		
		if (!StringUtils.hasText(actionName))
		{
			addError("The name of the new action is empty");
		}
		else if (!ValidationUtil.isValidOptionallyQualifiedResourceName(actionName))
		{
			addError("Invalid action name " + actionName);
		}
		
		if (!StringUtils.hasText(url))
		{
			addError("The URL of the new action is empty");
		}
		else
		{
			// TODO validate unique URL
		}
		
		if (!StringUtils.hasText(controllerOption))
		{
			addError("Please specify option for controller");
		}
		
		if (!StringUtils.hasText(viewOption))
		{
			addError("Please specify option for view");
		}
		
		if ("new".equals(controllerOption))
		{
			if (!StringUtils.hasText(newControllerName))
			{
				addError("Please specify the name of the new controller");
			}
			if (!StringUtils.hasText(controllerMethod))
			{
				addError("Please specify the name of the controller method to be generated");
			}
		}
		else if ("existing".equals(controllerOption))
		{
			if (!StringUtils.hasText(controllerMethod))
			{
				addError("Please specify the name of the controller method to be generated");
			}
			
			if (!StringUtils.hasText(sControllerId))
			{
				addError("Please specify the controller to be used for the page");
			}
			else
			{
				try
				{
					KID.get(sControllerId);
				}
				catch (KIDException e)
				{
					addError("Incorrect controller ID passed");
				}
			}
		}
		
		if ("new".equals(viewOption))
		{
			if (!StringUtils.hasText(newViewName))
			{
				addError("Please specify the name of the new view");
			}
			else
			{
				if (!ValidationUtil.isValidOptionallyQualifiedResourceName(newViewName))
				{
					addError("Invalid view name. " + ValidationUtil.INVALID_RESOURCE_ERROR_EXPLANATION);
				}
			}
		}
		else if ("existing".equals(viewOption))
		{
			if (!StringUtils.hasText(sViewId))
			{
				addError("Please specify the view to be used for the action");
			}
			else
			{
				try
				{
					KID.get(sViewId);
				}
				catch (KIDException e)
				{
					addError("Incorrect view ID passed");
				}
			}
		}
	}
	
	/**
	 * Returns JSRC containing all controller classes on the environment. The returned JSON has the following format:
	 * <code>{ success: true, data: jsrc }</code>, where <code>jsrc</code> is the actual JSRC collection.
	 * @param session
	 * @param resp
	 * @throws IOException
	 * @throws KommetException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_GET_VIEWS, method = RequestMethod.GET)
	@ResponseBody
	public void getViews (@RequestParam(value = "env", required = false) String envId,
						@RequestParam(value = "customOnly", required = false) String isCustomOnly,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpSession session, HttpServletResponse resp) throws IOException, KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		
		List<Record> views = restInfo.getEnv().getSelectCriteriaFromDAL("SELECT id, name, packageName, isSystem FROM " + SystemTypes.VIEW_API_NAME + " " + ("true".equals(isCustomOnly) ? " WHERE isSystem = false" : "") + " ORDER BY name ASC").list();
		
		try
		{
			restInfo.getOut().write(RestUtil.getRestSuccessDataResponse(JSRC.serialize(JSRC.build(views, restInfo.getEnv().getType(KeyPrefix.get(KID.VIEW_PREFIX)), 2, restInfo.getEnv(), restInfo.getAuthData()), restInfo.getAuthData())));
			return;
		}
		catch (KommetException e)
		{
			errorLog.logException(e, ErrorLogSeverity.ERROR, StandardActionController.class.getName(), 536, restInfo.getAuthData().getUserId(), restInfo.getAuthData(), restInfo.getEnv());
			restInfo.getOut().write(RestUtil.getRestErrorResponse("Cannot retrieve views"));
			return;
		}
	}
	
	/**
	 * Returns JSRC containing all controller classes on the environment. The returned JSON has the following format:
	 * <code>{ success: true, data: jsrc }</code>, where <code>jsrc</code> is the actual JSRC collection.
	 * @param session
	 * @param resp
	 * @throws IOException
	 * @throws KommetException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_GET_CONTROLLER_CLASSES, method = RequestMethod.GET)
	@ResponseBody
	public void getControllerClasses (@RequestParam(value = "env", required = false) String envId,
									@RequestParam(value = "access_token", required = false) String accessToken,
									HttpSession session, HttpServletResponse resp) throws IOException, KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		
		List<Record> allClasses = restInfo.getEnv().getSelectCriteriaFromDAL("SELECT id, name, packageName FROM Class ORDER BY name ASC").list();
		List<Record> controllers = new ArrayList<Record>();
		for (Record cls : allClasses)
		{
			java.lang.Class<?> controller = null;
			
			try
			{
				controller = compiler.getClass(cls.getField("packageName") + "." + cls.getField("name"), true, restInfo.getEnv());
			}
			catch (ClassNotFoundException e)
			{
				restInfo.getOut().write(RestUtil.getRestErrorResponse("Cannot find class in classpath for file " + cls.getField("packageName") + "." + cls.getField("name")));
				return;
			}
			
			// check if this class is a controller
			if (controller.isAnnotationPresent(kommet.koll.annotations.Controller.class))
			{
				controllers.add(cls);
			}
		}
		
		try
		{
			restInfo.getOut().write(RestUtil.getRestSuccessDataResponse(JSRC.serialize(JSRC.build(controllers, restInfo.getEnv().getType(KeyPrefix.get(KID.CLASS_PREFIX)), 2, restInfo.getEnv(), restInfo.getAuthData()), restInfo.getAuthData())));
			return;
		}
		catch (KommetException e)
		{
			errorLog.logException(e, ErrorLogSeverity.ERROR, StandardActionController.class.getName(), 536, restInfo.getAuthData().getUserId(), restInfo.getAuthData(), restInfo.getEnv());
			restInfo.getOut().write(RestUtil.getRestErrorResponse("Cannot retrieve controller classes"));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/standardpages/modify", method = RequestMethod.GET)
	public ModelAndView modifyStandardActions (@RequestParam("typeId") String sTypeId, @RequestParam("profileId") String sProfileId,
											@RequestParam("action") String sActionType, HttpSession session) throws KommetException
	{
		clearMessages();
		KID typeId = KID.get(sTypeId);
		KID profileId = KID.get(sProfileId);
		
		EnvData env = envService.getCurrentEnv(session);
		
		StandardActionType actionType = StandardActionType.fromString(sActionType);
		
		// find standard action for this profile, type and action
		StandardAction stdAction = actionService.getStandardActionForTypeAndProfile(typeId, profileId, actionType, env);
		
		ModelAndView mv = new ModelAndView("standardpages/editdefaultaction");
		mv.addObject("type", env.getType(typeId));
		mv.addObject("profileId", profileId);
		mv.addObject("profileName", profileService.getProfile(profileId, env).getName());
		
		if (stdAction == null)
		{	
			// if standard action does not exist, use the default one
			TypeInfo typeInfo = typeInfoService.getForType(typeId, env);
			
			if (typeInfo == null)
			{
				return getErrorPage("Type info not found for type " + typeId);
			}
			
			mv.addObject("page", typeInfo.getDefaultAction(actionType));
			mv.addObject("actionType", sActionType);
			mv.addObject("usedAction", "new");
			return mv;
		}
		else
		{
			mv.addObject("page", stdAction.getAction());
			mv.addObject("actionType", actionType.getStringValue().toLowerCase());
			mv.addObject("usedAction", "existing");
			return mv;
		}
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/standardpages/edit/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView editStandardActions (@PathVariable("keyPrefix") String keyPrefix, HttpSession session) throws KommetException
	{
		clearMessages();
		EnvData env = envService.getCurrentEnv(session);
		return prepareStandardActionView(new ModelAndView("standardpages/edit"), env, keyPrefix);
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/standardactions/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView listStandardActions (@PathVariable("keyPrefix") String keyPrefix, HttpSession session) throws KommetException
	{
		clearMessages();
		EnvData env = envService.getCurrentEnv(session);
		return prepareStandardActionView(new ModelAndView("standardpages/list"), env, keyPrefix);
	}
	
	/**
	 * 
	 * @param stdActions
	 * @param profilesForImplicitActions - if set and a profile does not have a standard page assigned to it, the standard page for the type will be used and added to its group returned by this method
	 * @return
	 * @throws KommetException
	 */
	private Map<KID, StdActionsForProfile> groupActionsByProfile (List<StandardAction> stdActions, TypeInfo typeInfo, List<Profile> profilesForImplicitActions) throws KommetException
	{
		Map<KID, StdActionsForProfile> actionsByProfileId = new HashMap<KID, StdActionsForProfile>();
		
		for (StandardAction action : stdActions)
		{
			StdActionsForProfile actionsForProfile = actionsByProfileId.get(action.getProfile().getId());
			if (actionsForProfile == null)
			{
				actionsForProfile = new StdActionsForProfile();
				actionsForProfile.setProfile(action.getProfile());
			}
			
			if (action.getStandardPageType().equals(StandardActionType.LIST))
			{
				actionsForProfile.setListAction(action.getAction());
			}
			else if (action.getStandardPageType().equals(StandardActionType.VIEW))
			{
				actionsForProfile.setDetailsAction(action.getAction());
			}
			else if (action.getStandardPageType().equals(StandardActionType.EDIT))
			{
				actionsForProfile.setEditAction(action.getAction());
			}
			else if (action.getStandardPageType().equals(StandardActionType.CREATE))
			{
				actionsForProfile.setCreateAction(action.getAction());
			}
			else
			{
				throw new KommetException("Unsupported standard page type " + action.getStandardPageType());
			}
			
			actionsByProfileId.put(actionsForProfile.getProfile().getId(), actionsForProfile);
		}
		
		if (profilesForImplicitActions != null)
		{
			for (Profile profile : profilesForImplicitActions)
			{
				if (!actionsByProfileId.containsKey(profile.getId()))
				{
					actionsByProfileId.put(profile.getId(), new StdActionsForProfile());
				}
				
				StdActionsForProfile stdActionsForProfile = actionsByProfileId.get(profile.getId());
				if (stdActionsForProfile.getCreateAction() == null)
				{
					stdActionsForProfile.setCreateAction(typeInfo.getDefaultCreateAction());
				}
				if (stdActionsForProfile.getEditAction() == null)
				{
					stdActionsForProfile.setEditAction(typeInfo.getDefaultEditAction());
				}
				if (stdActionsForProfile.getDetailsAction() == null)
				{
					stdActionsForProfile.setDetailsAction(typeInfo.getDefaultDetailsAction());
				}
				if (stdActionsForProfile.getListAction() == null)
				{
					stdActionsForProfile.setListAction(typeInfo.getDefaultListAction());
				}
				actionsByProfileId.put(profile.getId(), stdActionsForProfile);
			}
		}
		
		return actionsByProfileId;
	}

	public class StdActionsForProfile
	{
		private Profile profile;
		private Action listAction;
		private Action detailsAction;
		private Action editAction;
		private Action createAction;
		
		public void setProfile(Profile profile)
		{
			this.profile = profile;
		}
		public Profile getProfile()
		{
			return profile;
		}
		public void setListAction(Action listAction)
		{
			this.listAction = listAction;
		}
		public Action getListAction()
		{
			return listAction;
		}
		public void setDetailsAction(Action detailsAction)
		{
			this.detailsAction = detailsAction;
		}
		public Action getDetailsAction()
		{
			return detailsAction;
		}
		public void setEditAction(Action editAction)
		{
			this.editAction = editAction;
		}
		public Action getEditAction()
		{
			return editAction;
		}
		public void setCreateAction(Action createAction)
		{
			this.createAction = createAction;
		}
		public Action getCreateAction()
		{
			return createAction;
		}
	}
}