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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthUtil;
import kommet.auth.LoginHistoryFilter;
import kommet.auth.LoginHistoryService;
import kommet.basic.LoginHistory;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.QueryResultOrder;
import kommet.json.JSON;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class LoginHistoryController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	LoginHistoryService lhService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/loginhistory", method = RequestMethod.GET)
	@ResponseBody
	public void userLoginHistory (@RequestParam(value = "userId", required = false) String sUserId,
						@RequestParam(value = "maxResults", required = false) String sMaxResults,
						HttpServletResponse response, HttpSession session) throws IOException, KommetException
	{
		PrintWriter out = response.getWriter();
		
		KID userId = null;
		
		try
		{
			userId = KID.get(sUserId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid user ID '" + sUserId + "'"));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		LoginHistoryFilter filter = new LoginHistoryFilter();
		filter.addUserId(userId);
		filter.setOrderBy("createdDate");
		filter.setOrder(QueryResultOrder.DESC);
		filter.setLimit(StringUtils.hasText(sMaxResults) ? Integer.valueOf(sMaxResults) : 10);
		
		List<LoginHistory> logs = lhService.get(filter, env);
		out.write(getSuccessDataJSON(JSON.serializeObjectProxies(logs, MiscUtils.toSet("id", "loginUser.id", "loginUser.userName", "createdDate", "method", "result", "ip4Address", "ip6Address"), AuthUtil.getAuthData(session))));
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/loginhistory", method = RequestMethod.GET)
	public ModelAndView loginHistory (HttpSession session) throws KommetException
	{
		return new ModelAndView("loginhistory/list");
	}
}