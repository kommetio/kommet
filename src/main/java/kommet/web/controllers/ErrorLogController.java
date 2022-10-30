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
import kommet.basic.ErrorLog;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogFilter;
import kommet.errorlog.ErrorLogService;
import kommet.filters.QueryResultOrder;
import kommet.json.JSON;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class ErrorLogController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	ErrorLogService errorLogService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/errorlogs", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session) throws KIDException, KommetException
	{
		return new ModelAndView("errorlogs/list");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/errorlogs/{logId}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("logId") String sLogId, HttpSession session) throws KIDException, KommetException
	{
		KID logId = null;
		try
		{
			logId = KID.get(sLogId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid log ID " + sLogId);
		}
		
		ModelAndView mv = new ModelAndView("errorlogs/details"); 
		mv.addObject("log", errorLogService.get(logId, envService.getCurrentEnv(session)));
		return mv; 
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/errorlogs/data", method = RequestMethod.GET)
	@ResponseBody
	public void userLoginHistory (@RequestParam(value = "maxResults", required = false) String sMaxResults,
						HttpServletResponse response, HttpSession session) throws IOException, KommetException
	{	
		PrintWriter out = response.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		ErrorLogFilter filter = new ErrorLogFilter();
		filter.setOrder(QueryResultOrder.DESC);
		filter.setOrderBy("createdDate");
		filter.setLimit(StringUtils.hasText(sMaxResults) ? Integer.valueOf(sMaxResults) : 500);
		
		List<ErrorLog> logs = errorLogService.get(filter, env);
		out.write(getSuccessDataJSON(JSON.serializeObjectProxies(logs, MiscUtils.toSet("id", "affectedUser.id", "affectedUser.userName", "createdDate", "message", "codeClass", "codeLine", "severity"), AuthUtil.getAuthData(session))));
	}
}