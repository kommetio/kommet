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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.data.KommetException;
import kommet.dataimport.csv.CSVParser;
import kommet.dataimport.csv.CsvLineMapProcessor;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.ClassService;
import kommet.koll.SystemContextException;
import kommet.koll.SystemContextFactory;
import kommet.koll.compiler.KommetCompiler;
import kommet.rest.RestUtil;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
@Controller
public class DataImportController extends CommonKommetController
{	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	EnvService envService;
	
	@Inject
	SystemContextFactory sysContextFactory;
	
	@Inject
	ClassService classService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dataimport", method = RequestMethod.GET)
	public ModelAndView showDataImport(HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("dataimport/import");
		mv.addObject("uploadItem", new UploadItem());
		
		List<Class<?>> rowHandlers = compiler.findSubclasses(classService.getClasses(null, env), CsvLineMapProcessor.class, env);
		List<String> handlerNames = new ArrayList<String>();
		for (Class<?> handler : rowHandlers)
		{
			// only use user-defined classes as handlers
			if (!MiscUtils.isEnvSpecific(handler.getName()))
			{
				continue;
			}
			String handlerName = MiscUtils.envToUserPackage(handler.getName(), env);
			handlerNames.add(handlerName);
		}
		
		mv.addObject("rowHandlers", handlerNames);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dataimport/upload", method = RequestMethod.POST)
	@ResponseBody
	public void upload (UploadItem uploadItem, BindingResult result,
			@RequestParam(value = "rowHandler", required = false) String rowHandler,
            HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		
		MultipartFile file = uploadItem.getFileData();
		CSVParser parser = new CSVParser();
		
		if (!StringUtils.hasText(rowHandler))
		{
			addError("Row handler not specified");
		}
		
		if (hasErrorMessages())
		{
			out.write(RestUtil.getRestErrorResponse(getErrorMsgs()));
			return;
		}
		
		Class<?> handlerClass = null;
		EnvData env = envService.getCurrentEnv(session);
		try
		{
			handlerClass = compiler.getClass(MiscUtils.userToEnvPackage(rowHandler, env), false, env);
		}
		catch (ClassNotFoundException e)
		{
			out.write(RestUtil.getRestErrorResponse("Row handler class not found"));
			return;
		}
		
		Object handlerClassInstance = null;
		try
		{
			handlerClassInstance = handlerClass.newInstance();
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse("Could not instantiate handler class"));
			return;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		// need to insert auth data for the current thread manually
		env.addAuthData(authData);
		
		// inject system context if requested
		try
		{
			sysContextFactory.injectSystemContext(handlerClassInstance, authData, env);
		}
		catch (SystemContextException e)
		{
			out.write(RestUtil.getRestErrorResponse("Failed to inject system context into the handler class"));
			return;
		}
		
		try
		{
			parser.parse(file.getInputStream(), (CsvLineMapProcessor)handlerClassInstance);
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse("Error calling handler: " + e.getMessage()));
			return;
		}
		
		out.write(RestUtil.getRestSuccessResponse("Data import successful"));
	}
}