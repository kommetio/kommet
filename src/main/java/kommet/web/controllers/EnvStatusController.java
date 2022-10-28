/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class EnvStatusController extends CommonKommetController
{
	@Inject
	AppConfig config;
	
	@Inject
	ErrorLogService logService;
	
	@Inject
	EnvService envService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/maintenancebreak", method = RequestMethod.GET)
	public ModelAndView maintenanceBreak() throws KIDException, KommetException
	{
		ModelAndView mv = new ModelAndView("common/msg");
		mv.addObject("actionMsgs", MiscUtils.toList(config.getServerMaintenanceMessage()));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/pagenotfound", method = RequestMethod.GET)
	public ModelAndView pageNotFound(HttpSession session, HttpServletResponse resp) throws KIDException, KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		if (env != null)
		{
			env.clearAuthData();
		}
		
		resp.setStatus(HttpServletResponse.SC_OK);
		
		ModelAndView mv = new ModelAndView("common/msg");
		mv.addObject("actionMsgs", "The requested URL was not found");
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/handleerror", method = RequestMethod.GET)
	public ModelAndView handleError(@RequestParam(value = "msg", required = false) String msg, HttpSession session) throws KIDException, KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		if (env != null)
		{
			env.clearAuthData();
		}
		
		ModelAndView mv = new ModelAndView("common/msg");
		mv.addObject("actionMsgs", StringUtils.hasText(msg)? msg : "An error has occurred");
		return mv;
	}
}