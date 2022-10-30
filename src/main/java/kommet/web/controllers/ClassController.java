/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Class;
import kommet.config.Constants;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.ClassCompilationException;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.koll.KollUtil;
import kommet.koll.compiler.CompilationError;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.UrlUtil;

@Controller
public class ClassController extends CommonKommetController
{
	@Inject
	ClassService classService;
	
	@Inject
	EnvService envService;
	
	@Inject
	KommetCompiler compiler;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/classes/list", method = RequestMethod.GET)
	public ModelAndView list (@RequestParam(value = "name", required = false) String name,
							@RequestParam(value = "searchPhrase", required = false) String searchPhrase, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("classes/list");
		
		ClassFilter filter = new ClassFilter();
		if (StringUtils.hasText(name))
		{
			filter.setNameLike(name);
		}
		if (StringUtils.hasText(searchPhrase))
		{
			filter.setContentLike(searchPhrase);
		}
		
		mv.addObject("files", classService.getClasses(filter, envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/classes/compile/{fileId}", method = RequestMethod.GET)
	public ModelAndView compile (@PathVariable("fileId") String fileId, HttpSession session) throws KommetException
	{
		clearMessages();
		
		Class file = classService.getClass(KID.get(fileId), envService.getCurrentEnv(session));
		
		ModelAndView mv = new ModelAndView("classes/details");
		mv.addObject("file", file);
		
		List<String> compilationErrors = compile(file, session);
		if (!compilationErrors.isEmpty())
		{
			mv.addObject("errorMsgs", compilationErrors);
		}
		else
		{
			mv.addObject("actionMsgs", getMessage("Compilation successful"));
		}
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/classes/{fileId}", method = RequestMethod.GET)
	public ModelAndView fileDetails (@PathVariable("fileId") String fileId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("classes/details");
		Class file = classService.getClass(KID.get(fileId), envService.getCurrentEnv(session));
		mv.addObject("file", file);
		mv.addObject("packageName", file.getPackageName());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/classes/new", method = RequestMethod.GET)
	public ModelAndView newFile (HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("classes/edit");
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/classes/edit/{fileId}", method = RequestMethod.GET)
	public ModelAndView fileEdit (@PathVariable("fileId") String fileId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("classes/edit");
		Class file = classService.getClass(KID.get(fileId), envService.getCurrentEnv(session));
		mv.addObject("file", file);
		mv.addObject("packageName", file.getPackageName());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/classes/save", method = RequestMethod.POST)
	public ModelAndView save (@RequestParam(value = "fileId", required = false) String fileId,
								@RequestParam(value = "name", required = false) String name,
								@RequestParam(value = "package", required = false) String userPackageName,
								HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID id = null;
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		Class cls = null;
		
		if (StringUtils.hasText(fileId))
		{
			// remove the line below to allow for editing classes
			// but generally we don't want user to modify classes in a different way than through IDE, API or deployment
			addError("Classes can only be edited through IDE or API");
			
			id = KID.get(fileId);
			cls = classService.getClass(id, env);
		}
		else
		{
			cls = new Class();
			cls.setIsSystem(false);
		}
		
		if (!StringUtils.hasText(userPackageName))
		{
			addError("Package name is empty");
		}
		else if (!KollUtil.isValidClassPackageName(userPackageName) && !AuthUtil.isRoot(authData))
		{
			// only root users can save classes whose package starts with "kommet"
			addError("Package name must not start with the reserved prefix " + Constants.KOMMET_BASE_PACKAGE);
		}
		
		cls.setPackageName(userPackageName);
		
		validateNotEmpty(name, "Class name");
		cls.setName(name);
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("classes/edit");
			mv.addObject("file", cls);
			mv.addObject("packageName", userPackageName);
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		// TODO in the future use real KOLL code
		cls.setKollCode(KollUtil.getTemplateCode(name, userPackageName, env));
		cls.setJavaCode(classService.getKollTranslator(env).kollToJava(cls.getKollCode(), true, authData, env));
		
		// compile file before saving
		List<String> errorMsgs = compile(cls, session);
		
		if (errorMsgs.isEmpty())
		{
			try
			{
				// save view
				cls = classService.save(cls, authData, env);
			}
			catch (ClassCompilationException e)
			{
				ModelAndView mv = new ModelAndView("classes/edit");
				mv.addObject("file", cls);
				mv.addObject("packageName", userPackageName);
				mv.addObject("errorMsgs", getMessage("Compilation error: " + e.getMessage()));
				return mv;
			}
			catch (KommetException e)
			{
				ModelAndView mv = new ModelAndView("classes/edit");
				mv.addObject("file", cls);
				mv.addObject("packageName", userPackageName);
				mv.addObject("errorMsgs", getMessage("Error while saving file: " + e.getMessage()));
				return mv;
			}
		}
		else
		{
			// if compilation failed, do not save the file
			ModelAndView mv = new ModelAndView("classes/edit");
			mv.addObject("errorMsgs", errorMsgs);
			mv.addObject("file", cls);
			mv.addObject("packageName", userPackageName);
			return mv;
		}
		
		ModelAndView mv = new ModelAndView("classes/details");
		cls = classService.getClass(cls.getId(), env);
		mv.addObject("file", cls);
		mv.addObject("packageName", cls.getPackageName());
		mv.addObject("actionMsgs", getMessage("Class has been saved"));
		return mv;
	}

	private List<String> compile(Class file, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		CompilationResult result = compiler.compile(file, env);
		
		List<String> msgs = new ArrayList<String>();
		
		if (!result.isSuccess())
		{
			for (CompilationError err : result.getErrors())
			{
				msgs.add(err.getMessage());
			}
		}
		else
		{
			// after a class has been successfully compiled, we need to reset the class loader
			// for the env so that it reloads the class definition (there seems to be no other
			// simple way to update a class that has already been loaded to a class loader)
			compiler.resetClassLoader(env);
		}
		
		return msgs;
	}
}
