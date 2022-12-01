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
import kommet.basic.DocTemplate;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.docs.DocTemplateService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class DocTemplateController extends CommonKommetController
{
	@Inject
	DocTemplateService templateService;
	
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig appConfig;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/doctemplates", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("doctemplates/list");
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/doctemplates/edit/{templateId}", method = RequestMethod.GET)
	public ModelAndView edit (@PathVariable("templateId") String templateId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("doctemplates/edit");
		mv.addObject("template", templateService.get(KID.get(templateId), envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/doctemplates/{templateId}", method = RequestMethod.GET)
	public ModelAndView details (@PathVariable("templateId") String templateId, HttpSession session, HttpServletRequest req) throws KommetException
	{
		ModelAndView mv = new ModelAndView("doctemplates/details");
		
		DocTemplate template = templateService.get(KID.get(templateId), envService.getCurrentEnv(session));
		
		mv.addObject("template", template);
		mv.addObject("content", MiscUtils.newLinesToBr(template.getContent()));
		
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "Data types", appConfig.getBreadcrumbMax(), session, getContextPath(session));
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/doctemplates/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete (@RequestParam(value = "id", required = false) String sTemplateId,
			HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{
		KID templateId = null;
		PrintWriter out = response.getWriter();
		
		try
		{
			templateId = KID.get(sTemplateId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid template ID " + sTemplateId));
			return;
		}
		
		try
		{
			templateService.delete(templateId, envService.getCurrentEnv(session));
			out.write(getSuccessJSON("Document template has been successfully deleted"));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error deleting template: " + e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/doctemplates/save", method = RequestMethod.POST)
	public ModelAndView save (@RequestParam(value = "templateId", required = false) String layoutId,
								@RequestParam(value = "name", required = false) String name,
								@RequestParam(value = "content", required = false) String content,
								HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID id = null;
		EnvData env = envService.getCurrentEnv(session);
		DocTemplate template = null;
		
		if (StringUtils.hasText(layoutId))
		{
			id = KID.get(layoutId);
			template = templateService.get(id, env);
		}
		else
		{
			template = new DocTemplate();
		}
		
		validateNotEmpty(name, "Template name");
		validateNotEmpty(content, "Content");
		
		template.setName(name);
		template.setContent(content);
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("doctemplates/edit");
			mv.addObject("template", template);
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		// save view
		template = templateService.save(template, AuthUtil.getAuthData(session), env);
		
		ModelAndView mv = new ModelAndView("doctemplates/details");
		mv.addObject("template", template);
		mv.addObject("content", MiscUtils.newLinesToBr(template.getContent()));
		mv.addObject("actionMsgs", getMessage("Template has been saved"));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/doctemplates/new", method = RequestMethod.GET)
	public ModelAndView newTemplate(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("doctemplates/edit");
		return mv;
	}
}