/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.basic.keetle.PageData;
import kommet.config.UserSettingKeys;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.services.SystemSettingService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.MiscUtils;
import kommet.web.RequestAttributes;
import kommet.web.kmparams.KmParamException;
import kommet.web.kmparams.KmParamUtils;
import kommet.web.kmparams.actions.OverrideLayout;

/**
 * Common abstract controller class for all controllers.
 * @author Radek Krawiec
 */
public abstract class CommonKommetController
{
	private List<String> errorMsgs = new ArrayList<String>();
	private List<String> actionMsgs = new ArrayList<String>();
	
	protected ModelAndView getMessagePage(String msg)
	{
		ModelAndView mv = new ModelAndView("common/msg");
		List<String> msgs = new ArrayList<String>();
		msgs.add(msg);
		mv.addObject("actionMsgs", msgs);
		return mv;
	}
	
	protected static void addRmParams(ModelAndView mv, HttpServletRequest req, EnvData env) throws KmParamException
	{
		mv.addObject(RequestAttributes.PAGE_DATA_ATTR_NAME, KmParamUtils.initRmParams(req, new PageData(env)));
	}
	
	protected Locale getDefaultLocale (EnvData env, SystemSettingService settingService) throws KommetException
	{
		Locale locale = env != null ? settingService.getDefaultLocale(env) : null;
		return locale != null ? locale : Locale.EN_US;
	}
	
	protected static void addLayoutPath(UserCascadeHierarchyService uchService, ModelAndView mv, AuthData authData, EnvData env) throws KommetException
	{
		KID layoutId = uchService.getUserSettingAsKID(UserSettingKeys.KM_SYS_DEFAULT_LAYOUT_ID, authData, AuthData.getRootAuthData(env), env);
		
		if (authData.getUserSettings() != null && authData.getUserSettings().getLayout() != null)
		{
			// get layout from user-specific settings
			layoutId = authData.getUserSettings().getLayout().getId();
		}
		
		PageData pageData = (PageData)mv.getModel().get(RequestAttributes.PAGE_DATA_ATTR_NAME);
		
		if (pageData != null && pageData.getRmParams() != null)
		{
			OverrideLayout overrideLayout = (OverrideLayout)pageData.getRmParams().getSingleActionNode("layout");
			layoutId = overrideLayout.getLayoutId();
		}
		
		mv.addObject("layoutPath", layoutId != null ? (env.getId() + "/" + layoutId) : null);
	}
	
	/*@ExceptionHandler(Exception.class)
	public ModelAndView handleError(HttpServletRequest req, HttpServletResponse resp, Exception exception)
	{
	    ModelAndView mv = new ModelAndView("common/exception");
	    mv.addObject("exception", exception);
	    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    return mv;
	}*/
	
	protected static void addLayoutPath (ModelAndView mv, KID layoutId, EnvData env)
	{
		mv.addObject("layoutPath", layoutId != null ? (env.getId() + "/" + layoutId) : null);
	}
	
	protected ModelAndView getErrorPage(String errorMsg)
	{
		return getErrorPage(errorMsg, false);
	}
	
	protected ModelAndView getErrorPage(String errorMsg, boolean isBlankLayout)
	{
		ModelAndView mv = new ModelAndView(isBlankLayout ? "common/blankLayoutMsg" : "common/msg");
		List<String> msgs = new ArrayList<String>();
		msgs.add(errorMsg);
		mv.addObject("errorMsgs", msgs);
		return mv;
	}
	
	protected ModelAndView getErrorPage(List<String> errorMsgs)
	{
		ModelAndView mv = new ModelAndView("common/msg");
		mv.addObject("errorMsgs", errorMsgs);
		return mv;
	}
	
	protected List<String> getMessage(String msg)
	{
		List<String> msgs = new ArrayList<String>();
		msgs.add(msg);
		return msgs;
	}
	
	protected boolean hasErrorMessages()
	{
		return !this.errorMsgs.isEmpty();
	}
	
	public void addError (String msg)
	{
		this.errorMsgs.add(msg);
	}
	
	protected void clearMessages()
	{
		this.errorMsgs = new ArrayList<String>();
		this.actionMsgs = new ArrayList<String>();
	}
	
	protected boolean validateNotEmpty(String fieldValue, String fieldName)
	{
		if (!StringUtils.hasText(fieldValue))
		{
			addError("Field " + fieldName + " is empty");
			return false;
		}
		else
		{
			return true;
		}
	}
	
	public void addActionMessage (String msg)
	{
		this.actionMsgs.add(msg);
	}
	
	public void setErrorMsgs(List<String> errorMsgs)
	{
		this.errorMsgs = errorMsgs;
	}
	public List<String> getErrorMsgs()
	{
		return errorMsgs;
	}
	public void setActionMsgs(List<String> actionMsgs)
	{
		this.actionMsgs = actionMsgs;
	}
	public List<String> getActionMsgs()
	{
		return actionMsgs;
	}
	
	protected String getErrorJSON(List<String> msgs)
	{
		List<String> escapedMsgs = new ArrayList<String>();
		for (String msg : msgs)
		{
			escapedMsgs.add(msg.replaceAll("\\r?\\n", " ").replaceAll("\"", "'"));
		}
		
		return "{ \"status\": \"error\", \"messages\": [ " + MiscUtils.implode(escapedMsgs, ", ", "\"", null) + "] }";
	}
	
	protected String getErrorJSON(String msg)
	{
		return "{ \"status\": \"error\", \"messages\": [\"" + msg.replaceAll("\\r?\\n", " ").replaceAll("\"", "'") + "\"] }";
	}
	
	protected String getSuccessJSON(String msg)
	{
		return "{ \"status\": \"success\", \"message\": \"" + msg + "\" }";
	}
	
	protected String getSuccessDataJSON(String data)
	{
		return "{ \"status\": \"success\", \"data\": " + data + " }";
	}
}