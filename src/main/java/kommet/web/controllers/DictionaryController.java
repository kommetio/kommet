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

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Dictionary;
import kommet.basic.DictionaryItem;
import kommet.basic.Profile;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.json.JSON;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.DictionaryService;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class DictionaryController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	DictionaryService dictionaryService;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session) throws KommetException
	{
		return new ModelAndView("dictionaries/list");
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/{dictId}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("dictId") String sDictionaryId, HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID dictId = null;
		try
		{
			dictId = KID.get(sDictionaryId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid dictionary ID " + sDictionaryId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		ModelAndView mv = new ModelAndView("dictionaries/details");
		mv.addObject("dictionary", dictionaryService.get(dictId, authData, env));
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/edit/{dictId}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("dictId") String sDictionaryId, HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID dictId = null;
		try
		{
			dictId = KID.get(sDictionaryId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid dictionary ID " + sDictionaryId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		ModelAndView mv = new ModelAndView("dictionaries/edit");
		mv.addObject("dictionary", dictionaryService.get(dictId, authData, env));
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/new", method = RequestMethod.GET)
	public ModelAndView create(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("dictionaries/edit");
		mv.addObject("pageTitle", "New dictionary");
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/save", method = RequestMethod.POST)
	@ResponseBody
	public void save(@RequestParam(required = false, value = "id") String sDictionaryId,
					@RequestParam(required = false, value = "name") String name,
					HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		Dictionary dict = new Dictionary();
		
		if (StringUtils.hasText(sDictionaryId))
		{
			KID dictId = null;
			
			try
			{
				dictId = KID.get(sDictionaryId);
			}
			catch (KIDException e)
			{
				out.write(RestUtil.getRestErrorResponse("Invalid app ID '" + sDictionaryId + "'"));
				return;
			}
			
			dict = dictionaryService.get(dictId, authData, env);
		}
		
		if (!StringUtils.hasText(name))
		{
			out.write(RestUtil.getRestErrorResponse("Empty dictionary name"));
			return;
		}
		else if (!ValidationUtil.isValidOptionallyQualifiedResourceName(name))
		{
			out.write(RestUtil.getRestErrorResponse("Invalid dictionary name '" + name + "'"));
			return;
		}
		
		dict.setName(name);
		
		dict = dictionaryService.save(dict, authData, env);
		
		out.write(RestUtil.getRestSuccessDataResponse("{ \"dictionaryId\": \"" + dict.getId() + "\" }"));
		return;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/saveitem", method = RequestMethod.POST)
	@ResponseBody
	public void saveItem(@RequestParam(required = false, value = "dictionaryId") String sDictionaryId,
					@RequestParam(required = false, value = "name") String name,
					@RequestParam(required = false, value = "key") String key,
					@RequestParam(required = false, value = "index") String sIndex,
					HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		Dictionary dict = new Dictionary();
		
		if (StringUtils.hasText(sDictionaryId))
		{
			KID dictId = null;
			
			try
			{
				dictId = KID.get(sDictionaryId);
			}
			catch (KIDException e)
			{
				out.write(RestUtil.getRestErrorResponse("Invalid app ID '" + sDictionaryId + "'"));
				return;
			}
			
			dict = dictionaryService.get(dictId, authData, env);
		}
		
		if (!StringUtils.hasText(name))
		{
			out.write(RestUtil.getRestErrorResponse("Empty item name"));
			return;
		}
		
		DictionaryItem item = new DictionaryItem();
		item.setDictionary(dict);
		item.setName(name);
		item.setKey(StringUtils.hasText(key) ? key : null);
		item.setIndex(Integer.valueOf(sIndex));
		
		item = dictionaryService.save(item, authData, env);
		
		out.write(RestUtil.getRestSuccessDataResponse("{ \"itemId\": \"" + item.getId() + "\" }"));
		return;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/initonenv", method = RequestMethod.POST)
	@ResponseBody
	public void initDictionaries(HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		envService.getCurrentEnv(session).initDictionaries(dictionaryService);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/items/{dictId}", method = RequestMethod.GET)
	@ResponseBody
	public void getItems(@PathVariable("dictId") String sDictionaryId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		KID dictId = null;
		try
		{
			dictId = KID.get(sDictionaryId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid dictionary ID " + dictId));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		Dictionary dict = dictionaryService.get(dictId, authData, env);
		
		List<String> serializedItems = new ArrayList<String>();
		
		for (DictionaryItem item : dict.getItems())
		{
			serializedItems.add(JSON.serialize(item, authData))	;
		}
		
		out.write(RestUtil.getRestSuccessDataResponse("{ \"items\": [" + MiscUtils.implode(serializedItems, ", ") + "] }"));
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteDictionary(@RequestParam(required = false, value = "id") String sDictionaryId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sDictionaryId))
		{
			out.write(RestUtil.getRestErrorResponse("Dictionary ID not specified"));
			return;
		}
		
		KID dictId = null;
		
		try
		{
			dictId = KID.get(sDictionaryId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid app ID '" + sDictionaryId + "'"));
			return;
		}
		
		// delete app URL by id
		dictionaryService.delete(dictId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		
		out.write(RestUtil.getRestSuccessResponse("Dictionary deleted"));
		return;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dictionaries/deleteitem", method = RequestMethod.POST)
	@ResponseBody
	public void deleteItem(@RequestParam(required = false, value = "id") String sItemId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sItemId))
		{
			out.write(RestUtil.getRestErrorResponse("Item ID not specified"));
			return;
		}
		
		// delete item by id
		dictionaryService.deleteItem(KID.get(sItemId), AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		
		out.write(RestUtil.getRestSuccessResponse("Dictionary item deleted"));
		return;
	}
}