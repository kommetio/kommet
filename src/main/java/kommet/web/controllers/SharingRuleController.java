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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthUtil;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.SharingRuleFilter;
import kommet.services.SharingRuleService;
import kommet.utils.UrlUtil;

@Controller
public class SharingRuleController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	SharingRuleService srService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/typesharingrules/{typeId}", method = RequestMethod.GET)
	public ModelAndView sharingRulesForType(@PathVariable("typeId") String sTypeId, HttpSession session) throws KommetException
	{
		KID typeId = null;
		try
		{
			typeId = KID.get(sTypeId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid type ID " + sTypeId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		Type type = env.getType(typeId);
		if (type == null)
		{
			return getErrorPage("Type with ID " + sTypeId + " not found");
		}
		
		SharingRuleFilter filter = new SharingRuleFilter();
		filter.addReferencedType(typeId);
		
		ModelAndView mv = new ModelAndView("sharingrules/listfortype");
		mv.addObject("sharingRules", srService.get(filter, AuthUtil.getAuthData(session), env));
		return mv;
	}
}