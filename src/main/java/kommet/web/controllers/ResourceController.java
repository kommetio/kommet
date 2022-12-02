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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.config.Constants;
import kommet.config.UIConfig;
import kommet.config.UserSettingKeys;
import kommet.data.ComponentType;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.i18n.I18nDictionary;
import kommet.i18n.Locale;
import kommet.json.JSON;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.MiscUtils;
import kommet.utils.NumberFormatUtil;
import kommet.utils.PropertyUtilException;
import kommet.utils.UrlUtil;

@Controller
public class ResourceController extends BasicRestController
{
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	EnvService envService;
	
	private Map<Locale, String> i18nValuesByLocale = new HashMap<Locale, String>();
	
	/**
	 * Returns the km.js.config file.
	 * 
	 * Note that the file is served publicly and available to unauthenticated users. While serving content within this file, make sure to apply permissions
	 * so that no protected content is leaked.
	 * @param response
	 * @param req
	 * @param session
	 * @throws PropertyUtilException
	 * @throws KommetException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlUtil.CONFIG_JS_URL, method = RequestMethod.GET)
	@ResponseBody
	public void getEnvConfig(HttpServletResponse response, HttpServletRequest req, HttpSession session) throws PropertyUtilException, KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		
		StringBuilder code = new StringBuilder();
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		String servletHost = authData.getUserCascadeSettings().get(UserSettingKeys.KM_SYS_HOST);
		if (StringUtils.isEmpty(servletHost))
		{
			servletHost = req.getContextPath();
		}
		
		// if auth data is null, then the getCurrentEnv method will throw an error
		EnvData env = authData != null ? envService.getCurrentEnv(session) : null;
		
		code.append("km.js.config = {").append("\n");
		
		if (authData != null)
		{
			// add user info
			code.append("\tauthData: { ");
			
			code.append("user: { userName: \"").append(authData.getUser().getUserName()).append("\", id: \"").append(authData.getUser().getId()).append("\" }, ");
			
			code.append("timeZoneOffset: ").append(authData.getTimeZoneOffset());
			
			// end auth data
			code.append("},\n");
			
			// add environment package prefix
			code.append("\tenvPackagePrefix: \"kommet.envs." + authData.getEnvName() + "\",\n");
			
			// add number format for the users locale
			code.append("\tlocaleNumberFormat: \"" + NumberFormatUtil.getLocaleSpecificNumberFormat(authData.getLocale()).replaceAll("\\\\", "\\\\\\\\") + "\",\n");
			
			// add i18n labels
			code.append("\"i18n\": {");
			
			code.append(getI18nValues(authData.getI18n()));
			
			// close i18n
			code.append("},\n");
			
			// start user settings
			code.append("\"userSettings\": {");
			
			code.append(getUserSettings(authData.getUserCascadeSettings()));
			
			// close user settings
			code.append("},\n");
		}
		
		code.append("sessionId: ").append(authData != null ? "\"" + authData.getSessionId() + "\"" : "null").append(",\n");
		
		code.append("envId: ").append(env != null ? "\"" + env.getId() + "\"" : "null").append(",\n");
		
		// add context path
		code.append("\tcontextPath: \"" + servletHost + "\",\n");
		
		// add system context path
		code.append("\tsysContextPath: \"" + servletHost + "/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "\",\n");
		
		// add image path
		code.append("\timagePath: \"" + servletHost + "/resources/images\",\n");
		
		// add component types
		List<String> componentTypes = new ArrayList<String>();
		for (ComponentType type : ComponentType.values())
		{
			componentTypes.add("\"" + type.getId() + "\": { name: \"" + type.name() + "\" }");
		}
		
		code.append("\tcomponentTypes: { " + MiscUtils.implode(componentTypes, ", ") + " },\n");
		
		if (env != null)
		{
			// start types by ID
			code.append("\"types\": {");
			
			code.append(getTypesById(authData, env));
			
			// close types by ID
			code.append("},\n");
		}
		
		code.append("}");
		
		response.setContentType("application/javascript");
		
		out.write(code.toString());
	}
	
	private String getTypesById(AuthData authData, EnvData env) throws KommetException
	{
		List<String> serializedTypes = new ArrayList<String>();
		
		for (Type type : env.getUserAccessibleTypes())
		{
			serializedTypes.add("\"" + type.getKID() + "\": " + JSON.serialize(type, authData));
		}
		
		return MiscUtils.implode(serializedTypes, ", ");
	}

	private String getUserSettings(Map<String, String> userCascadeSettings)
	{
		List<String> vals = new ArrayList<String>();
		
		for (String key : userCascadeSettings.keySet())
		{
			// skip layout settings because they contains very long values, and those values are JSON strings with quotes so they would have to be escaped
			if (key.startsWith(UserSettingKeys.KM_SYS_FIELD_LAYOUT + "."))
			{
				continue;
			}
			
			vals.add("\"" + key + "\": \"" + userCascadeSettings.get(key) + "\"");
		}
		
		return MiscUtils.implode(vals, ", ");
	}

	@RequestMapping(value = UrlUtil.USER_CSS_STYLES_URL, method = RequestMethod.GET)
	@ResponseBody
	public void getUsetCssStyles(HttpServletResponse response, HttpServletRequest req, HttpSession session) throws PropertyUtilException, KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		
		EnvData env = envService.getCurrentEnv(session);
		if (env == null)
		{
			return;
		}
		
		Type systemValueType = env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX));
		AuthData rootAuthData = AuthData.getRootAuthData(env);
		AuthData authData = AuthUtil.getAuthData(session);
		
		String customStylesOn = (String)uchService.getSettingValue(systemValueType, "value", "key", Constants.SYSTEM_SETTING_UI_CUSTOM_STYLES_ON, authData, rootAuthData, env);
		if (!"true".equals(customStylesOn))
		{
			return;
		}
		
		StringBuilder code = new StringBuilder();
		
		if (authData == null)
		{
			return;
		}
		
		UIConfig config = authData.getUIConfig();
		
		if (config == null)
		{
			return;
		}
		
		String important = " !important";
		
		if (StringUtils.hasText(config.getUserMenuFontSize()))
		{
			code.append("#user-menu > ul > li > a, #user-menu > ul > li > ul > li > a { font-size: " + config.getUserMenuFontSize()).append(important).append(" }\n");
		}
		
		if (StringUtils.hasText(config.getSystemMenuFontSize()))
		{
			code.append("#left-menu > ul > li > a, #left-menu > ul > li > ul > li > a { font-size: " + config.getSystemMenuFontSize()).append(important).append(" }\n");
		}
		
		if (StringUtils.hasText(config.getButtonFontSize()))
		{
			code.append(".ui-widget a.sbtn, .ui-widget input.sbtn, a.sbtn, input.sbtn { font-size: " + config.getButtonFontSize()).append(important).append(" }\n");
		}
		
		response.setContentType("text/css");
		out.write(code.toString());
		
	}

	private String getI18nValues(I18nDictionary i18n)
	{
		if (this.i18nValuesByLocale.containsKey(i18n.getLocale()))
		{
			return this.i18nValuesByLocale.get(i18n.getLocale());
		}
		
		List<String> i18nValues = new ArrayList<String>();
		
		for (String key : i18n.getKeys())
		{
			i18nValues.add("\"" + key + "\": \"" + i18n.get(key) + "\"");
		}
		
		this.i18nValuesByLocale.put(i18n.getLocale(), MiscUtils.implode(i18nValues, ",\n"));
		return this.i18nValuesByLocale.get(i18n.getLocale());
	}
}