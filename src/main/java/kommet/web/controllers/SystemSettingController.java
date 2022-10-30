/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Profile;
import kommet.basic.SystemSetting;
import kommet.dao.SystemSettingFilter;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.security.RestrictedAccess;
import kommet.services.SystemSettingService;
import kommet.systemsettings.SystemSettingKey;
import kommet.utils.UrlUtil;

@Controller
public class SystemSettingController extends CommonKommetController
{
	@Inject
	SystemSettingService settingService;
	
	@Inject
	EnvService envService;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/systemsettings/details", method = RequestMethod.GET)
	public ModelAndView systemSettingDetails (HttpSession session) throws KommetException
	{
		return getSystemSettingDetails(new ModelAndView("systemsettings/details"), session);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/systemsettings/edit", method = RequestMethod.GET)
	public ModelAndView systemSettingEdit (HttpSession session) throws KommetException
	{
		return getSystemSettingDetails(new ModelAndView("systemsettings/edit"), session);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/systemsettings/save", method = RequestMethod.POST)
	public ModelAndView systemSettingSave (@RequestParam(value = "defaultErrorViewId", required = false) String defaultErrorViewId, HttpSession session, HttpServletRequest req) throws KommetException
	{
		clearMessages();
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		// read system settings from parameters
		List<SystemSettingKey> keys = getProcessedSettings();
		for (SystemSettingKey key : keys)
		{
			Object value = req.getParameter("setting_" + key.toString());
			if (value == null)
			{
				value = "";
			}
			
			settingService.setSetting(key, value.toString(), authData, env);
		}
		
		// default error view is handled differently
		if (!StringUtils.hasText(defaultErrorViewId))
		{
			addError("Default error view not specified");
		}
		else
		{
			settingService.setSetting(SystemSettingKey.DEFAULT_ERROR_VIEW_ID, defaultErrorViewId, authData, env);
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = getSystemSettingDetails(new ModelAndView("systemsettings/edit"), session);
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		return getSystemSettingDetails(new ModelAndView("systemsettings/details"), session);
	}
	
	private List<SystemSettingKey> getProcessedSettings()
	{
		List<SystemSettingKey> keys = new ArrayList<SystemSettingKey>();
		keys.add(SystemSettingKey.DEFAULT_ENV_LOCALE);
		keys.add(SystemSettingKey.IGNORE_NON_EXISTING_FIELD_LABELS);
		keys.add(SystemSettingKey.MIN_PASSWORD_LENGTH);
		keys.add(SystemSettingKey.DEFAULT_ERROR_VIEW_ID);
		
		return keys;
	}

	private ModelAndView getSystemSettingDetails(ModelAndView mv, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		SystemSettingFilter filter = new SystemSettingFilter();
		
		List<SystemSettingKey> keys = getProcessedSettings();
		for (SystemSettingKey key : keys)
		{
			filter.addKey(key);
		}
		
		// query settings
		List<SystemSetting> settings = settingService.find(filter, env);
		
		for (SystemSetting setting : settings)
		{
			mv.addObject("setting_" + setting.getKey(), setting.getValue());
		}
		
		return mv;
	}
}