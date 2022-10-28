/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthUtil;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.json.JSON;
import kommet.utils.UrlUtil;

@Controller
public class DALConsoleController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/querydb", method = RequestMethod.GET)
	public ModelAndView showConsole(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("dalconsole/console");
		mv.addObject("envId", envService.getCurrentEnv(session).getId());
		
		EnvData env = envService.getCurrentEnv(session);
		
		mv.addObject("availableTypes", JSON.serialize(env.getUserAccessibleTypes(), AuthUtil.getAuthData(session)));
		return mv;
	}
}