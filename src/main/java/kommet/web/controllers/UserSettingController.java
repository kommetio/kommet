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
import kommet.basic.Profile;
import kommet.basic.SettingValue;
import kommet.basic.UserCascadeHierarchy;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.config.Constants;
import kommet.config.UserSettingKeys;
import kommet.dao.SettingValueFilter;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.i18n.Locale;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.SettingValueService;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.UrlUtil;

@Controller
public class UserSettingController extends CommonKommetController
{
	@Inject
	SettingValueService settingValueService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig appConfig;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usersettings/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session, HttpServletRequest req) throws KommetException
	{ 	
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "User settings", appConfig.getBreadcrumbMax(), session);
				
		ModelAndView mv = new ModelAndView("usersettings/list");
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usersettings/new", method = RequestMethod.GET)
	public ModelAndView newSetting(HttpSession session) throws KommetException
	{ 	
		ModelAndView mv = new ModelAndView("usersettings/edit");
		return prepareDetails(null, mv, session);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usersettings/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("id") String sSettingId, HttpSession session, HttpServletRequest req) throws KommetException
	{ 	
		// add breadcrumbs
		//Breadcrumbs.add(req.getRequestURL().toString(), "User setting: ", appConfig.getBreadcrumbMax(), session);
		
		ModelAndView mv = new ModelAndView("usersettings/details");
		return prepareDetails(sSettingId, mv, session);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usersettings/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("id") String sSettingId, HttpSession session) throws KommetException
	{ 	
		ModelAndView mv = new ModelAndView("usersettings/edit");
		return prepareDetails(sSettingId, mv, session);
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/objectdetails/customize", method = RequestMethod.POST)
	@ResponseBody
	public void saveObjectDetailsCustomization(@RequestParam(value = "context", required = false) String context,
												@RequestParam(value = "contextValue", required = false) String contextValue,
												@RequestParam(value = "layout", required = false) String layout,
												@RequestParam(value = "typeName", required = false) String typeName,
												HttpSession session, HttpServletResponse resp) throws KommetException
	{ 	
		PrintWriter out;
		try
		{
			out = resp.getWriter();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing JSON response. Response could not be written to output.");
		}
		
		if (!"environment".equals(context))
		{
			out.write(RestUtil.getRestErrorResponse("Unsupported customization context " + context));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		Type type = env.getType(typeName);
		
		SettingValueFilter filter = new SettingValueFilter();
		filter.addKey(UserSettingKeys.KM_SYS_FIELD_LAYOUT + "." + type.getKID());
		
		List<SettingValue> settings = uchService.getSettings(filter, authData, AuthData.getRootAuthData(env), env);
		if (!settings.isEmpty())
		{
			SettingValue setting = settings.get(0);
			setting.setValue(layout);
			uchService.saveSetting(setting, setting.getHierarchy().getActiveContext(), setting.getHierarchy().getActiveContextValue(), authData, env);
		}
		else
		{
			uchService.saveUserSetting(UserSettingKeys.KM_SYS_FIELD_LAYOUT + "." + type.getKID(), layout, UserCascadeHierarchyContext.ENVIRONMENT, true, AuthUtil.getAuthData(session), env);
		}
			
		out.write(RestUtil.getRestSuccessResponse("Layout customization saved"));
		return;
		
		//List<String> serializedFields = new ArrayList<String>();
		
		// get all public static variable from the UserSettingKeys class
		/*for (Field field : UserSettingKeys.class.getDeclaredFields())
		{
			if (field.getName().startsWith("RM_") && Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
			{
				//serializedFields.add("\"" + field.getName() + "\": \"" + field.get(null) + "\"");
			}
		}*/
	}
	
	/*@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usersettings/keys", method = RequestMethod.POST)
	@ResponseBody
	public void getSettingKeys(HttpSession session, HttpServletResponse resp) throws KommetException
	{ 	
		PrintWriter out;
		try
		{
			out = resp.getWriter();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing JSON response. Response could not be written to output.");
		}
		
		List<String> serializedFields = new ArrayList<String>();
		
		// get all public static variable from the UserSettingKeys class
		for (Field field : UserSettingKeys.class.getDeclaredFields())
		{
			if (field.getName().startsWith("RM_") && Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
			{
				serializedFields.add("\"" + field.getName() + "\": \"" + field.get(null) + "\"");
			}
		}
	}*/
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usersettings/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete(@RequestParam(value = "id", required = false) String sSettingId, HttpSession session, HttpServletResponse resp) throws KommetException
	{ 	
		PrintWriter out;
		try
		{
			out = resp.getWriter();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing JSON response. Response could not be written to output.");
		}
		
		try
		{
			settingValueService.delete(KID.get(sSettingId), AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse("Error deleting setting: " + e.getMessage()));
		}
		
		out.write(RestUtil.getRestSuccessResponse("Setting deleted successfully"));
	}
	
	private ModelAndView prepareDetails(String sId, ModelAndView mv, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		KID settingId = null;
		SettingValue setting = null;
		
		if (StringUtils.hasText(sId))
		{
			try
			{
				settingId = KID.get(sId);
			}
			catch (KIDException e)
			{
				return getErrorPage("Invalid setting ID " + sId);
			}
			
			setting = settingValueService.get(settingId, true, authData, env);
			
			if (setting == null)
			{
				return getErrorPage("Setting with ID " + sId + " not found");
			}
			
			String settingName = UserSettingKeys.getName(setting.getKey());
			mv.addObject("pageTitle", settingName != null ? settingName : setting.getKey());
			mv.addObject("settingName", settingName);
			mv.addObject("settingValue", uchService.getInterpretedValue(setting, env));
		}
		else
		{
			mv.addObject("pageTitle", "New user setting");
		}
		
		mv.addObject("locales", Locale.values());
		mv.addObject("setting", setting);
		return mv;
	}

	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usersettings/save", method = RequestMethod.POST)
	public ModelAndView save(@RequestParam(value = "settingId", required = false) String sSettingId,
							@RequestParam(value = "key", required = false) String key,
							@RequestParam(value = "predefinedKey", required = false) String predefinedKey,
							@RequestParam(value = "value", required = false) String value,
							@RequestParam(value = "profileId", required = false) String sProfileId,
							@RequestParam(value = "userGroupId", required = false) String sUserGroupId,
							@RequestParam(value = "userId", required = false) String sUserId,
							@RequestParam(value = "locale", required = false) String sLocale,
							@RequestParam(value = "activeContext", required = false) String activeContextName,
							HttpSession session) throws KommetException
	{ 	
		clearMessages();
		EnvData env = envService.getCurrentEnv(session);
		
		SettingValue setting = new SettingValue();
		UserCascadeHierarchy uch = new UserCascadeHierarchy();
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (StringUtils.hasText(sSettingId))
		{
			KID settingId = null;
			
			try
			{
				settingId = KID.get(sSettingId);
				
				setting = settingValueService.get(settingId, false, authData, env);
				uch = setting.getHierarchy();
			}
			catch (KIDException e)
			{
				addError("Invalid setting ID " + sSettingId);
			}
		}
		
		if (!StringUtils.hasText(key) && !StringUtils.hasText(predefinedKey))
		{
			addError(authData.getI18n().get("usersettings.key.empty.err"));
		}
		else
		{
			if (StringUtils.hasText(key) && StringUtils.hasText(predefinedKey) && !key.equals(predefinedKey))
			{
				addError(authData.getI18n().get("usersettings.key.both.set"));
			}
			else
			{
				if (StringUtils.hasText(predefinedKey))
				{
					key = predefinedKey;
				}
				
				// only root and system administrator can set this setting
				if (key.startsWith(Constants.SYSTEM_SETTING_KEY_PREFIX + ".") && !authData.hasConfigPermission())
				{
					addError(authData.getI18n().get("usersetting.key.systempermission.access"));
				}
			}
		}
		
		UserCascadeHierarchyContext context = null;
		Object ctxValue = null;
		
		if (!StringUtils.hasText(activeContextName))
		{
			addError(authData.getI18n().get("usersettings.ctx.not.selected.err"));
		}
		else
		{
			context = UserCascadeHierarchyContext.fromString(activeContextName);
			
			if (UserCascadeHierarchyContext.ENVIRONMENT.equals(context))
			{
				ctxValue = true;
			}
			else if (UserCascadeHierarchyContext.APPLICATION.equals(context))
			{
				throw new KommetException("Application UCH context not yet supported");
			}
			else if (UserCascadeHierarchyContext.PROFILE.equals(context))
			{
				if (StringUtils.hasText(sProfileId))
				{
					ctxValue = KID.get(sProfileId);
				}
				else
				{
					addError(authData.getI18n().get("usersettings.ctx.profile.err"));
				}
			}
			else if (UserCascadeHierarchyContext.USER_GROUP.equals(context))
			{
				if (StringUtils.hasText(sUserGroupId))
				{
					ctxValue = KID.get(sUserGroupId);
				}
				else
				{
					addError(authData.getI18n().get("usersettings.ctx.usergroup.err"));
				}
			}
			else if (UserCascadeHierarchyContext.USER.equals(context))
			{
				if (StringUtils.hasText(sUserId))
				{
					ctxValue = KID.get(sUserId);
				}
				else
				{
					addError(authData.getI18n().get("usersettings.ctx.user.err"));
				}
			}
			else if (UserCascadeHierarchyContext.LOCALE.equals(context))
			{
				if (StringUtils.hasText(sLocale))
				{
					ctxValue = Locale.valueOf(sLocale);
				}
				else
				{
					addError(authData.getI18n().get("usersettings.ctx.locale.err"));
				}
			}
			
			uch.setActiveContext(context, ctxValue);
			setting.setHierarchy(uch);
		}
		
		setting.setKey(key);
		setting.setValue(value);
		
		if (!hasErrorMessages())
		{
			// check if setting with this key, context and context value already exists
			SettingValueFilter filter = new SettingValueFilter();
			filter.addKey(key);
			filter.setContext(context);
			filter.setContextValue(ctxValue);
			List<SettingValue> existingValues = uchService.getSettings(filter, authData, authData, env);
			
			if (!existingValues.isEmpty())
			{
				if (existingValues.size() > 1)
				{
					// this should never happen
					addError("More than one setting exists with key " + key + ", context " + context + " and context value " + ctxValue);
				}
				else
				{
					// if such setting already exists and it is not the same setting, this is an error
					if (!StringUtils.hasText(sSettingId) || !existingValues.get(0).getId().equals(KID.get(sSettingId)))
					{
						addError("Setting with the given key and context already exists");
					}
				}
			}
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("usersettings/edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("setting", setting);
			return mv;
		}
		
		// save UCH
		setting = uchService.saveSetting(setting, uch.getActiveContext(), uch.getActiveContextValue(), authData, env);
		
		// redirect to setting details
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usersettings/" + setting.getId());	
	}
}