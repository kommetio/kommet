/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;

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
import kommet.basic.TextLabel;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.labels.ManipulatingReferencedLabelException;
import kommet.labels.TextLabelFilter;
import kommet.labels.TextLabelService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.UrlUtil;

@Controller
public class TextLabelController extends CommonKommetController
{
	@Inject
	TextLabelService labelService;
	
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/textlabels/list", method = RequestMethod.GET)
	public ModelAndView list (HttpServletRequest req, HttpSession session) throws KommetException
	{
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "Text labels", appConfig.getBreadcrumbMax(), session, getContextPath(session));
		
		ModelAndView mv = new ModelAndView("textlabels/list");
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/textlabels/edit/{labelId}", method = RequestMethod.GET)
	public ModelAndView edit (@PathVariable(value = "labelId") String labelId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("textlabels/edit");
		addLayoutPath(uchService, mv, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		return getLabelDetails(mv, labelId, envService.getCurrentEnv(session));
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/textlabels/{labelId}", method = RequestMethod.GET)
	public ModelAndView details (@PathVariable(value = "labelId") String labelId, HttpServletRequest req, HttpSession session) throws KommetException
	{	
		ModelAndView mv = new ModelAndView("textlabels/details");
		addLayoutPath(uchService, mv, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		mv = getLabelDetails(mv, labelId, envService.getCurrentEnv(session));
		
		// add breadcrumbs
		TextLabel label = ((TextLabel)mv.getModel().get("label"));
		if (label != null)
		{
			Breadcrumbs.add(req.getRequestURL().toString(), "Text label: " + label.getKey(), appConfig.getBreadcrumbMax(), session, getContextPath(session));
		}
				
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/textlabels/new", method = RequestMethod.GET)
	public ModelAndView newLabel (HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("textlabels/edit");
		addLayoutPath(uchService, mv, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/textlabels/save", method = RequestMethod.POST)
	public ModelAndView save (@RequestParam(value = "labelId", required = false) String sLabelId,
					@RequestParam(value = "key", required = false) String key, 
					@RequestParam(value = "value", required = false) String value, 
					@RequestParam(value = "locale", required = false) String locale,
					HttpSession session) throws KommetException
	{
		clearMessages();
		ModelAndView mv = new ModelAndView("textlabels/edit");
		
		EnvData env = envService.getCurrentEnv(session);
		
		TextLabel label = null;
		
		if (StringUtils.hasText(sLabelId))
		{
			label = labelService.get(KID.get(sLabelId), env);
		}
		else
		{
			label = new TextLabel();
		}
		
		label.setKey(key);
		label.setValue(value);
		
		if (StringUtils.hasLength(locale))
		{
			label.setLocale(locale);
		}
		else
		{
			label.setLocale((String)null);
			label.nullify("locale");
		}
		
		if (!StringUtils.hasText(key))
		{
			addError("Label key is empty");
		}
		
		if (!StringUtils.hasText(value))
		{
			addError("Label value is empty");
		}
		
		if (hasErrorMessages())
		{
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("label", label);
			return mv;
		}
		
		// make sure a label with this key-locale combination does not exist
		if (label.getId() == null)
		{
			TextLabelFilter filter = new TextLabelFilter();
			filter.addKey(label.getKey());
			if (label.getLocaleAsEnum() != null)
			{
				filter.setLocale(label.getLocaleAsEnum());
			}
			
			if (!labelService.get(filter, env).isEmpty())
			{
				mv.addObject("errorMsgs", getMessage("A label with the given key " + (label.getLocaleAsEnum() != null ? "and locale " : "and with universal locale ") + "already exists"));
				mv.addObject("label", label);
				return mv;
			}
		}
		
		try
		{
			// save label
			label = labelService.save(label, AuthUtil.getAuthData(session), env);
		}
		catch (ManipulatingReferencedLabelException e)
		{
			// label key cannot be edited because it is used somewhere in the system
			mv.addObject("errorMsgs", getMessage(e.getMessage()));
			mv.addObject("label", label);
			return mv;
		}
		
		// redirect to label details
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/textlabels/" + label.getId());
	}

	private ModelAndView getLabelDetails(ModelAndView mv, String sLabelId, EnvData env)
	{
		KID labelId = null;
		try
		{
			labelId = KID.get(sLabelId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid ID " + sLabelId);
		}
		
		try
		{
			mv.addObject("label", labelService.get(labelId, env));
			return mv;
		}
		catch (KommetException e)
		{
			mv.addObject("errorMsgs", getMessage("Error getting label: " + e.getMessage()));
			return mv;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/textlabels/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete (@RequestParam(value = "id", required = false) String sLabelId,
			HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{
		KID labelId = null;
		PrintWriter out = response.getWriter();
		
		try
		{
			labelId = KID.get(sLabelId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid label ID " + sLabelId));
			return;
		}
		
		try
		{
			labelService.delete(labelId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
			out.write(getSuccessJSON("Text label has been successfully deleted"));
			return;
		}
		catch (ManipulatingReferencedLabelException e)
		{
			out.write(getErrorJSON(e.getMessage()));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error deleting text label: " + e.getMessage()));
			return;
		}
	}
	
	public class TextLabelComparator implements Comparator<TextLabel>
	{
	    @Override
	    public int compare(TextLabel o1, TextLabel o2)
	    {
	        return o1.getKey().compareTo(o2.getKey());
	    }
	}
}