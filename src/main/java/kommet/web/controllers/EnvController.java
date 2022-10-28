/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.util.Date;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import kommet.data.Env;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.services.EnvDataService;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class EnvController extends CommonKommetController
{
	@Inject
	EnvDataService envService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/env/create", method = RequestMethod.GET)
	public ModelAndView createEnv() throws KommetException
	{
		return new ModelAndView("env/create");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/env/docreate", method = RequestMethod.POST)
	public ModelAndView doCreateEnv(@RequestParam("name") String name) throws KommetException
	{
		Env env = new Env();
		env.setName(name);
		env.setCreated(new Date());
		//env.setOwner(getCurrentUser());
		env.setKID(new KID(KID.ENV_PREFIX + MiscUtils.getHash(15)));
		
		//envService.create(env);
		//envService.createEnvDatabase(env);
		
		ModelAndView mv = new ModelAndView("env/docreate");
		mv.addObject("actionMsgs", getMessage("Your environment has been successfully created"));
		return mv;
	}
}