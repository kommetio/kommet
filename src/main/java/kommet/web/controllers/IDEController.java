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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthUtil;
import kommet.basic.Class;
import kommet.basic.CustomTypeRecordProxy;
import kommet.basic.Layout;
import kommet.basic.Profile;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.HttpResponse;
import kommet.basic.keetle.LayoutFilter;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.StandardObjectController;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.ViewSyntaxException;
import kommet.basic.keetle.ViewUtil;
import kommet.basic.keetle.tagdata.Attribute;
import kommet.basic.keetle.tagdata.Namespace;
import kommet.basic.keetle.tagdata.Tag;
import kommet.basic.keetle.tagdata.TagData;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.emailing.Attachment;
import kommet.env.EnvAlreadyExistsException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.ide.IDEFile;
import kommet.ide.IDEService;
import kommet.json.JSON;
import kommet.koll.ClassCompilationException;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.koll.Intellisense;
import kommet.koll.KollUtil;
import kommet.koll.annotations.Action;
import kommet.koll.annotations.ActionConfig;
import kommet.koll.annotations.CrossOrigin;
import kommet.koll.annotations.Disabled;
import kommet.koll.annotations.Param;
import kommet.koll.annotations.Params;
import kommet.koll.annotations.Public;
import kommet.koll.annotations.Rest;
import kommet.koll.annotations.ReturnsFile;
import kommet.koll.annotations.triggers.AfterDelete;
import kommet.koll.annotations.triggers.AfterInsert;
import kommet.koll.annotations.triggers.AfterUpdate;
import kommet.koll.annotations.triggers.BeforeDelete;
import kommet.koll.annotations.triggers.BeforeInsert;
import kommet.koll.annotations.triggers.BeforeUpdate;
import kommet.koll.annotations.triggers.Trigger;
import kommet.koll.compiler.CompilationError;
import kommet.koll.compiler.KommetCompiler;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.ViewResourceService;
import kommet.testing.TestError;
import kommet.testing.TestException;
import kommet.testing.TestResults;
import kommet.triggers.InvalidClassForTriggerException;
import kommet.utils.AppConfig;
import kommet.utils.CodeUtils;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.vendorapis.jexl.JexlScript;

@Controller
public class IDEController extends CommonKommetController
{
	@Inject
	EnvService envService;

	@Inject
	DataService dataService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	ErrorLogService errorLogService;
	
	@Inject
	ClassService clsService;
	
	@Inject
	ErrorLogService logService;
	
	private static Set<String> allJavaClasses = null;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/ide/setcurrentfile", method = RequestMethod.POST)
	@ResponseBody
	public void setCurrentFile(@RequestParam("fileId") String sFileId, HttpSession session, HttpServletResponse response) throws KIDException, KommetException, IOException
	{
		KID itemId = null;
		try
		{
			itemId = KID.get(sFileId);
		}
		catch (KIDException e)
		{
			response.getWriter().write(getErrorJSON("Incorrect file ID " + sFileId));
			return;
		}
		
		IDEService.setCurrentFile(itemId, session);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/codetemplate", method = RequestMethod.GET)
	@ResponseBody
	public void getCodeTemplate(@RequestParam("type") String templateType,
								@RequestParam(value = "templateFileName", required = false) String templateFileName,
								@RequestParam(value = "typeName", required = false) String typeName,
								HttpSession session, HttpServletResponse response) throws IOException, KommetException
	{
		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		
		String data = "{}";
		String fileDataJSON = "{}";
		
		if (templateType.equals("controller"))
		{
			String sourceCode;
			try
			{
				sourceCode = CodeUtils.getControllerTemplate(templateFileName);
				
				List<String> fileData = new ArrayList<String>();
				fileData.add("\"code\": \"" + JSON.escape(sourceCode) + "\"");
				List<String> collapsibleSections = CodeUtils.getCollapsibleSections(sourceCode);
				
				if (collapsibleSections != null && !collapsibleSections.isEmpty())
				{	
					fileData.add(CodeUtils.getCollapsibleSectionsJSON(collapsibleSections));
				}
				
				fileDataJSON = "{ " + MiscUtils.implode(fileData, ", ") + " }";
				
				data = "{ \"source\": \"" + JSON.escape(sourceCode) + "\", \"fileData\": " + fileDataJSON + " }";
			}
			catch (KommetException e)
			{
				logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, AuthUtil.getAuthData(session).getUserId(), env);
				out.write(RestUtil.getRestErrorResponse("Could not get source code template"));
				return;
			}
		}
		else if (templateType.equals("class"))
		{
			String sourceCode;
			try
			{
				sourceCode = CodeUtils.getClassTemplate(templateFileName);
				
				List<String> fileData = new ArrayList<String>();
				fileData.add("\"code\": \"" + JSON.escape(sourceCode) + "\"");
				List<String> collapsibleSections = CodeUtils.getCollapsibleSections(sourceCode);
				
				if (collapsibleSections != null && !collapsibleSections.isEmpty())
				{	
					fileData.add(CodeUtils.getCollapsibleSectionsJSON(collapsibleSections));
				}
				
				fileDataJSON = "{ " + MiscUtils.implode(fileData, ", ") + " }";
				
				data = "{ \"source\": \"" + JSON.escape(sourceCode) + "\", \"fileData\": " + fileDataJSON + " }";
			}
			catch (KommetException e)
			{
				logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, AuthUtil.getAuthData(session).getUserId(), env);
				out.write(RestUtil.getRestErrorResponse("Could not get source code template"));
				return;
			}
		}
		else if (templateType.equals("trigger"))
		{
			String sourceCode;
			try
			{
				Type type = StringUtils.hasText(typeName) ? env.getType(typeName) : env.getType(KeyPrefix.get(KID.USER_PREFIX)); 
				
				sourceCode = CodeUtils.getTriggerTemplate(templateFileName, type);
				
				List<String> fileData = new ArrayList<String>();
				fileData.add("\"code\": \"" + JSON.escape(sourceCode) + "\"");
				List<String> collapsibleSections = CodeUtils.getCollapsibleSections(sourceCode);
				
				if (collapsibleSections != null && !collapsibleSections.isEmpty())
				{	
					fileData.add(CodeUtils.getCollapsibleSectionsJSON(collapsibleSections));
				}
				
				fileDataJSON = "{ " + MiscUtils.implode(fileData, ", ") + " }";
				
				data = "{ \"source\": \"" + JSON.escape(sourceCode) + "\", \"fileData\": " + fileDataJSON + " }";
			}
			catch (KommetException e)
			{
				logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, AuthUtil.getAuthData(session).getUserId(), env);
				out.write(RestUtil.getRestErrorResponse("Could not get source code template"));
				return;
			}
		}
		else if (templateType.equals("businessAction"))
		{
			String sourceCode;
			try
			{
				sourceCode = CodeUtils.getBusinessActionTemplate(templateFileName);
				
				List<String> fileData = new ArrayList<String>();
				fileData.add("\"code\": \"" + JSON.escape(sourceCode) + "\"");
				List<String> collapsibleSections = CodeUtils.getCollapsibleSections(sourceCode);
				
				if (collapsibleSections != null && !collapsibleSections.isEmpty())
				{	
					fileData.add(CodeUtils.getCollapsibleSectionsJSON(collapsibleSections));
				}
				
				fileDataJSON = "{ " + MiscUtils.implode(fileData, ", ") + " }";
				
				data = "{ \"source\": \"" + JSON.escape(sourceCode) + "\", \"fileData\": " + fileDataJSON + " }";
			}
			catch (KommetException e)
			{
				logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, AuthUtil.getAuthData(session).getUserId(), env);
				out.write(RestUtil.getRestErrorResponse("Could not get source code template"));
				return;
			}
		}
		else if (templateType.equals("view"))
		{
			String sourceCode;
			try
			{
				sourceCode = CodeUtils.getViewTemplate(templateFileName);
				
				List<String> fileData = new ArrayList<String>();
				fileData.add("\"code\": \"" + JSON.escape(sourceCode) + "\"");
				
				fileDataJSON = "{ " + MiscUtils.implode(fileData, ", ") + " }";
				
				data = "{ \"source\": \"" + JSON.escape(sourceCode) + "\", \"fileData\": " + fileDataJSON + " }";
			}
			catch (KommetException e)
			{
				logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, AuthUtil.getAuthData(session).getUserId(), env);
				out.write(RestUtil.getRestErrorResponse("Could not get source code template"));
				return;
			}
		}
		else if (templateType.equals("layout"))
		{
			String sourceCode;
			try
			{
				sourceCode = CodeUtils.getLayoutTemplate(templateFileName);
				
				List<String> fileData = new ArrayList<String>();
				fileData.add("\"code\": \"" + JSON.escape(sourceCode) + "\"");
				
				fileDataJSON = "{ " + MiscUtils.implode(fileData, ", ") + " }";
				
				data = "{ \"source\": \"" + JSON.escape(sourceCode) + "\", \"fileData\": " + fileDataJSON + " }";
			}
			catch (KommetException e)
			{
				logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, AuthUtil.getAuthData(session).getUserId(), env);
				out.write(RestUtil.getRestErrorResponse("Could not get source code template"));
				return;
			}
		}
		else
		{
			out.write(RestUtil.getRestErrorResponse("Unsupported template type '" + templateType + "'"));
			return;
		}
		
		out.write(RestUtil.getRestSuccessDataResponse(data));
		return;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/ide/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveItem(@RequestParam(value = "fileId", required = false) String sFileId,
						@RequestParam(value = "code", required = false) String code,
						@RequestParam(value = "isNew", required = false) String isNew,
						@RequestParam(value = "type", required = false) String fileType,
						HttpSession session, HttpServletResponse response) throws KIDException, KommetException, IOException
	{
		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();
		
		KID itemId = null;
		EnvData env = envService.getCurrentEnv(session);
		Type type = null;
		boolean isNewFile = "true".equals(isNew); 
		
		if (!isNewFile)
		{
			try
			{
				itemId = KID.get(sFileId);
			}
			catch (KIDException e)
			{
				out.write(RestUtil.getRestErrorResponse("Incorrect file ID " + sFileId));
				return;
			}
			
			type = env.getTypeByRecordId(itemId);
		}
		else
		{
			if (!StringUtils.hasText(fileType))
			{
				out.write(getSaveErrorJSON("<new file>", "File type not defined"));
				return;
			}
			
			// it is a new file, so we cannot deduce its type from the ID
			// instead, we use the "fileType" parameter
			if ("class".equals(fileType))
			{
				type = env.getType(KeyPrefix.get(KID.CLASS_PREFIX));
			}
			else if ("view".equals(fileType))
			{
				type = env.getType(KeyPrefix.get(KID.VIEW_PREFIX));
			}
			else if ("layout".equals(fileType))
			{
				type = env.getType(KeyPrefix.get(KID.LAYOUT_PREFIX));
			}
			else if ("viewResource".equals(fileType))
			{
				type = env.getType(KeyPrefix.get(KID.VIEW_RESOURCE_PREFIX));
			}
			else
			{
				out.write(getSaveErrorJSON("<new file>", "File type " + fileType + " not supported"));
				return;
			}
		}
		
		if (type.getKeyPrefix().getPrefix().equals(KID.CLASS_PREFIX))
		{
			Class file = !isNewFile ? clsService.getClass(itemId, env) : KollUtil.getClassFromCode(code, env);
			
			if (isNewFile)
			{
				file.setIsSystem(false);
			}
			
			if (file == null)
			{
				out.write(RestUtil.getRestErrorResponse("File does not exist " + sFileId));
				return;
			}
			
			if (!validateCode(code))
			{
				out.write(getSaveErrorJSON(file.getQualifiedName(), "Invalid class code"));
				return;
			}
				
			file.setKollCode(code);
			
			try
			{
				clsService.fullSave(file, dataService, true, AuthUtil.getAuthData(session), env);
				IDEService.refreshFile(new IDEFile(file), session);
				out.write(RestUtil.getRestSuccessDataResponse("{ \"fileId\": \"" + file.getId() + "\", \"fileName\": \"" + file.getName() + "\" }"));
				return;
			}
			catch (ClassCompilationException e)
			{
				if (e.getCompilationResult() != null)
				{
					out.write(getCompilationErrorsJSON(file.getQualifiedName(), e.getCompilationResult().getErrors()));
				}
				else
				{
					out.write(getSaveErrorJSON(file.getQualifiedName(), e.getMessage()));
				}
				return;
			}
			catch (InvalidClassForTriggerException e)
			{
				out.write(getSaveErrorJSON(file.getQualifiedName(), e.getMessage()));
				return;
			}
			catch (KommetException e)
			{
				String msg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : "Unknown error while saving file";
				
				out.write(getSaveErrorJSON(file.getQualifiedName(), msg));
				return;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				out.write(getSaveErrorJSON(file.getQualifiedName(), "Unknown error while saving file" + MiscUtils.coalesce(e.getMessage(), "")));
				return;
			}
		}
		else if (type.getKeyPrefix().getPrefix().equals(KID.VIEW_PREFIX))
		{
			View view = null;
			
			if (isNewFile)
			{
				view = new View();
				view.setIsSystem(false);
			}
			else
			{
				view = viewService.getView(itemId, env);
				
				if (view == null)
				{
					out.write(RestUtil.getRestErrorResponse("File does not exist " + sFileId));
					return;
				}
			}
			
			if (!validateCode(code))
			{
				out.write(getSaveErrorJSON(view.getQualifiedName(), "Invalid view code"));
				return;
			}
	
			try
			{
				try
				{
					String[] viewProps = ViewUtil.getViewPropertiesFromCode(ViewUtil.wrapKeetle(code));
					view.setName(viewProps[0]);
					view.setPackageName(viewProps[1]);
					
					viewService.fullSave(view, code, true, AuthUtil.getAuthData(session), env);
				}
				catch (KommetException e)
				{
					out.write(getSaveErrorJSON(view.getQualifiedName(), e.getMessage()));
					return;
				}
				catch (Exception e)
				{
					out.write(getSaveErrorJSON(view.getQualifiedName(), e.getMessage()));
					return;
				}
				
				IDEService.refreshFile(new IDEFile(view), session);
				out.write(RestUtil.getRestSuccessDataResponse("{ \"fileId\": \"" + view.getId() + "\", \"fileName\": \"" + view.getName() + "\" }"));
				return;
			}
			catch (Exception e)
			{
				out.write(getSaveErrorJSON(view.getQualifiedName(), "Error saving file " + e.getMessage()));
				return;
			}
		}
		else if (type.getKeyPrefix().getPrefix().equals(KID.LAYOUT_PREFIX))
		{
			Layout layout = null;
			
			if (isNewFile)
			{
				layout = new Layout();
			}
			else
			{
				layout = layoutService.getById(itemId, env);
				if (layout == null)
				{
					out.write(RestUtil.getRestErrorResponse("File does not exist " + sFileId));
					return;
				}
			}
			
			if (!validateCode(code))
			{
				out.write(getSaveErrorJSON(layout.getName(), "Invalid layout code"));
				return;
			}
			
			String[] viewProps = LayoutService.getLayoutPropertiesFromCode(ViewUtil.wrapKeetle(code));
			String newLayoutName = viewProps[0];
			
			// add package to full layout name
			if (StringUtils.hasText(viewProps[1]))
			{
				newLayoutName = viewProps[1] + "." + newLayoutName;
			}
			
			layout.setName(newLayoutName);
			
			try
			{
				layout.setCode(code);
			}
			catch (ViewSyntaxException e)
			{
				out.write(getSaveErrorJSON(layout.getName(), "Layout syntax error: " + e.getMessage()));
				return;
			}
			catch (KommetException e)
			{
				out.write(getSaveErrorJSON(layout.getName(), "Error parsing layout: " + e.getMessage()));
				return;
			}
			
			try
			{
				layoutService.save(layout, AuthUtil.getAuthData(session), env);
				IDEService.refreshFile(new IDEFile(layout), session);
				out.write(RestUtil.getRestSuccessDataResponse("{ \"fileId\": \"" + layout.getId() + "\", \"fileName\": \"" + layout.getName() + "\" }"));
				return;
			}
			catch (KommetException e)
			{
				out.write(getSaveErrorJSON(layout.getName(), "Error saving file " + e.getMessage()));
				return;
			}
		}
		else if (type.getKeyPrefix().getPrefix().equals(KID.VIEW_RESOURCE_PREFIX))
		{
			ViewResource resource = viewResourceService.get(itemId, env);
			if (resource == null)
			{
				out.write(RestUtil.getRestErrorResponse("File does not exist " + sFileId));
				return;
			}
			
			resource.setContent(code);
			
			try
			{
				viewResourceService.save(resource, AuthUtil.getAuthData(session), env);
				IDEService.refreshFile(new IDEFile(resource), session);
				out.write(RestUtil.getRestSuccessDataResponse("{ \"fileId\": \"" + resource.getId() + "\", \"fileName\": \"" + resource.getName() + "\" }"));
				return;
			}
			catch (KommetException e)
			{
				out.write(getSaveErrorJSON(resource.getName(), "Error saving file " + e.getMessage()));
				return;
			}
		}
		else
		{
			out.write(RestUtil.getRestErrorResponse("Unsupported type for IDE " + type.getQualifiedName()));
			return;
		}
	}
	
	protected String getSaveErrorJSON(String itemName, String msg)
	{
		return "{ \"success\": false, \"messages\": [ { \"item\": \"" + itemName + "\", \"message\": \"" + msg.replaceAll("\\r?\\n", " ").replaceAll("\"", "'") + "\" } ] }";
	}
	
	private String getCompilationErrorsJSON(String itemName, List<CompilationError> errors)
	{
		StringBuilder res = new StringBuilder("{ \"status\": \"error\", \"messages\": [");
		
		List<String> strErrors = new ArrayList<String>();
		for (CompilationError err : errors)
		{
			strErrors.add("{ \"item\": \"" + itemName + "\", \"line\": " + err.getLine() + ", \"message\": \"" + err.getMessage().replaceAll("\\r?\\n", " ") + "\" }");
		}
		
		res.append(MiscUtils.implode(strErrors, ", "));
		res.append("] }");
		return res.toString();
	}

	private boolean validateCode(String code)
	{
		return StringUtils.hasText(code);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/livejavahints", method = RequestMethod.POST)
	@ResponseBody
	public void getLiveJavaHints(@RequestParam(value = "code", required = false) String code,
								@RequestParam(value = "line", required = false) Integer line,
								@RequestParam(value = "position", required = false) Integer position,
								@RequestParam(value = "varName", required = false) String varName,
								@RequestParam(value = "methodName", required = false) String methodName,
								HttpSession session, HttpServletResponse resp) throws KIDException, KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		
		try
		{
			Set<String> hints = (new Intellisense(compiler.getClassLoader(env))).getHints(code, varName, methodName, line, position, env);
			out.write(RestUtil.getRestSuccessDataResponse("{ \"methods\": [ " + MiscUtils.implode(hints, ", ") + " ] }"));
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			out.write(RestUtil.getRestErrorResponse("Error compiling class"));
			return;
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/ide", method = RequestMethod.GET)
	public ModelAndView openIDE(@CookieValue(value = "openFileIds", defaultValue = "") String openFileIds,
								@RequestParam(value = "template", required = false) String template,
								@RequestParam(value = "typeId", required = false) String templateTypeId, HttpSession session) throws KIDException, KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		// get list of open files from cookie
		if (StringUtils.hasText(openFileIds))
		{
			String[] fileIds = openFileIds.split(";");
			
			for (String sFileId : fileIds)
			{
				KID fileId = KID.get(sFileId);
				
				if (!IDEService.isOpenFile(fileId, session))
				{
					openFile(fileId, session, env);
				}
			}
		}
		
		ModelAndView mv = new ModelAndView("/ide/ide");
		
		if (StringUtils.hasText(template))
		{
			mv.addObject("template", template);
			
			if (StringUtils.hasText(templateTypeId))
			{
				mv.addObject("templateTypeName", env.getType(KID.get(templateTypeId)).getQualifiedName());
			}
		}
		
		initDirectoryMap(mv, env);
		
		// operate on an ordered list to know under which index is the currently open tab
		return addOpenFileParams(mv, session, null);
	}

	@SuppressWarnings("unchecked")
	private static String getDirectoryTreeJSON(LinkedHashMap<String, Object> fileMap)
	{
		if (fileMap == null)
		{
			return "null";
		}
		
		List<String> nodes = new ArrayList<String>();
		
		// sort by keys
		List<Map.Entry<String, Object>> fileNames = new ArrayList<Map.Entry<String, Object>>(fileMap.entrySet());
		Collections.sort(fileNames, new Comparator<Map.Entry<String, Object>>() {
			public int compare(Map.Entry<String, Object> a, Map.Entry<String, Object> b) {
				// put folders on top
				if (a.getValue() instanceof LinkedHashMap<?, ?> && !(b.getValue() instanceof LinkedHashMap<?, ?>))
				{
					return -1;
				}
				else if (b.getValue() instanceof LinkedHashMap<?, ?> && !(a.getValue() instanceof LinkedHashMap<?, ?>))
				{
					return 1;
				}
				else
				{
					// compare name is both entries are folders or regular files
					return a.getKey().compareTo(b.getKey());
				}
			}
		});
		
		for (Map.Entry<String, Object> entry : fileNames)
		{
			if (entry.getValue() instanceof LinkedHashMap<?, ?>)
			{
				nodes.add("{ \"name\": \"" + entry.getKey() + "\", \"children\": " + getDirectoryTreeJSON((LinkedHashMap<String, Object>)entry.getValue()) + "}");
			}
			else if (entry.getValue() instanceof Class)
			{
				nodes.add("{ \"name\": \"" + entry.getKey() + "\", \"id\": \"" + ((Class)entry.getValue()).getId() + "\" }");
			}
			else if (entry.getValue() instanceof View)
			{
				nodes.add("{ \"name\": \"" + entry.getKey() + "\", \"id\": \"" + ((View)entry.getValue()).getId() + "\" }");
			}
			else if (entry.getValue() instanceof ViewResource)
			{
				nodes.add("{ \"name\": \"" + entry.getKey() + "\", \"id\": \"" + ((ViewResource)entry.getValue()).getId() + "\" }");
			}
			else if (entry.getValue() instanceof Layout)
			{
				nodes.add("{ \"name\": \"" + entry.getKey() + "\", \"id\": \"" + ((Layout)entry.getValue()).getId() + "\" }");
			}
		}
		
		if (nodes.isEmpty())
		{
			return "null";
		}
		
		return "[" + MiscUtils.implode(nodes, ", ") + "]";
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/ide/close", method = RequestMethod.POST)
	@ResponseBody
	public void closeFile (@RequestParam("fileId") String sFileId, HttpServletResponse resp, HttpSession session) throws KIDException, KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		KID itemId = null;
		try
		{
			itemId = KID.get(sFileId);
		}
		catch (KIDException e)
		{
			// ignore
			out.write(getErrorJSON("Incorrect file ID " + sFileId));
			return;
		}
		
		IDEService.closeFile(itemId, session);
		
		IDEFile currentFile = null;
		
		// if no file is currently open, open the first one on the list
		if (IDEService.getCurrentFile(session) == null && IDEService.hasOpenFiles(session))
		{
			currentFile = IDEService.getOpenFiles(session).iterator().next(); 
			IDEService.openFile(currentFile, session);
		}
		
		out.write(getSuccessDataJSON("{ \"currentFileId\": " + (currentFile != null ? "\"" + currentFile.getId() + "\"" : "null") + " }"));
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/ide/{fileId}", method = RequestMethod.GET)
	public ModelAndView openFileInIDE(@PathVariable("fileId") String sFileId, HttpSession session) throws KIDException, KommetException
	{
		ModelAndView mv = new ModelAndView("/ide/ide");
		
		KID fileId = null;
		try
		{
			fileId = KID.get(sFileId);
		}
		catch (KIDException e)
		{
			mv.addObject("errorMsgs", getMessage("Invalid file ID " + sFileId));
			mv.addObject("files", IDEService.getOpenFiles(session));
			return mv;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		// open file in IDE
		OpenFileResult result = openFile(fileId, session, env);
		
		// if there was an error opening the file, return error message and exit
		if (!result.getErrors().isEmpty())
		{
			mv.addObject("errorMsgs", result.getErrors());
			mv.addObject("files", IDEService.getOpenFiles(session));
			return mv;
		}
		
		initDirectoryMap(mv, env);
		
		return addOpenFileParams(mv, session, fileId);
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/ide/openfile", method = RequestMethod.GET)
	@ResponseBody
	public void openFile(@RequestParam("fileId") String sFileId, HttpServletResponse resp, HttpSession session) throws KIDException, KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		KID fileId = null;
		try
		{
			fileId = KID.get(sFileId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid file ID " + sFileId));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		// open file in IDE
		OpenFileResult result = openFile(fileId, session, env);
		
		// if there was an error opening the file, return error message and exit
		if (!result.getErrors().isEmpty())
		{
			out.write(getErrorJSON(result.getErrors()));
			return;
		}
		
		List<String> fileData = new ArrayList<String>();
		fileData.add("\"id\": \"" + fileId + "\"");
		fileData.add("\"code\": \"" + JSON.escapeHTML(StringEscapeUtils.escapeHtml4(result.getSourceCode())) + "\"");
		
		if (result.getCollapsedSections() != null && !result.getCollapsedSections().isEmpty())
		{	
			fileData.add(CodeUtils.getCollapsibleSectionsJSON(result.getCollapsedSections()));
		}
		
		out.write(getSuccessDataJSON("{ " + MiscUtils.implode(fileData, ", ") + " }"));
	}
	
	private OpenFileResult openFile (KID fileId, HttpSession session, EnvData env) throws KommetException
	{
		Type type = env.getTypeByRecordId(fileId);
		OpenFileResult result = new OpenFileResult();
		result.setErrors(new ArrayList<String>());
		
		if (type.getKeyPrefix().getPrefix().equals(KID.CLASS_PREFIX))
		{
			Class file = clsService.getClass(fileId, env);
			if (file == null)
			{
				result.getErrors().add("Class file does not exist " + fileId.getId());
				return result;
			}
			
			if (!Boolean.FALSE.equals(file.getIsSystem()))
			{
				// do not allow for editing system classes
				result.getErrors().add(file.getQualifiedName() + " is a system class and cannot be viewed");
				return result;
			}
			
			IDEService.openFile(new IDEFile(file), session);
			result.setSourceCode(file.getKollCode());
			result.setCollapsedSections(CodeUtils.getCollapsibleSections(result.getSourceCode()));
		}
		else if (type.getKeyPrefix().getPrefix().equals(KID.VIEW_PREFIX))
		{
			View file = viewService.getView(fileId, env);
			if (file == null)
			{
				result.getErrors().add("View does not exist " + fileId.getId());
				return result;
			}
			
			if (!Boolean.FALSE.equals(file.getIsSystem()))
			{
				// do not allow for editing system classes
				result.getErrors().add(file.getQualifiedName() + " is a system view and cannot be viewed");
				return result;
			}
			
			IDEService.openFile(new IDEFile(file), session);
			result.setSourceCode(file.getKeetleCode());
		}
		else if (type.getKeyPrefix().getPrefix().equals(KID.LAYOUT_PREFIX))
		{
			Layout layout = layoutService.getById(fileId, env);
			if (layout == null)
			{
				result.getErrors().add("Layout does not exist " + fileId.getId());
				return result;
			}
			IDEService.openFile(new IDEFile(layout), session);
			result.setSourceCode(layout.getCode());
		}
		else if (type.getKeyPrefix().getPrefix().equals(KID.VIEW_RESOURCE_PREFIX))
		{
			ViewResource resource = viewResourceService.get(fileId, env);
			if (resource == null)
			{
				result.getErrors().add("View resource does not exist " + fileId.getId());
				return result;
			}
			IDEService.openFile(new IDEFile(resource), session);
			result.setSourceCode(resource.getContent());
		}
		else
		{
			result.getErrors().add("Unsupported type for IDE " + type.getQualifiedName());
			return result;
		}
		
		return result;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/ide/dirtree", method = RequestMethod.GET)
	@ResponseBody
	public void getDirectoryTree(HttpServletResponse resp, HttpSession session) throws KIDException, KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		out.write(RestUtil.getRestSuccessDataResponse(getDirMapJSON(envService.getCurrentEnv(session))));
		return;
	}
	
	private String getDirMapJSON(EnvData env) throws KommetException
	{
		ClassFilter kollFilter = new ClassFilter();
		kollFilter.setSystemFile(false);
		
		ViewFilter viewFilter = new ViewFilter();
		viewFilter.setSystemView(false);
		
		LayoutFilter layoutFilter = new LayoutFilter();
		layoutFilter.setIsSystem(false);
		
		// add map of all files
		LinkedHashMap<String, Object> allFiles = MiscUtils.getFileDirectoryMap(clsService.getClasses(kollFilter, env), viewService.getViews(viewFilter, env), viewResourceService.find(null, env), layoutService.find(layoutFilter, env), env);
		
		LinkedHashMap<String, Object> envFiles = null;
		
		if (!allFiles.isEmpty())
		{
			envFiles = allFiles;
		}
		else
		{
			// in rare cases when env is empty, no files will be returned
			envFiles = new LinkedHashMap<String, Object>();
		}
		
		return getDirectoryTreeJSON(envFiles);
	}

	private void initDirectoryMap (ModelAndView mv, EnvData env) throws KommetException
	{	
		mv.addObject("dirMap", getDirMapJSON(env));
	}
	
	private static ModelAndView addOpenFileParams (ModelAndView mv, HttpSession session, KID currentFileId)
	{
		// operate on an ordered list to know under which index is the currently open tab
		// since JQuery Tabs operates on tab indices
		List<IDEFile> openFilesList = new ArrayList<IDEFile>();
		openFilesList.addAll(IDEService.getOpenFiles(session));
		mv.addObject("files", openFilesList);
		
		KID usedFileId = currentFileId;
		if (usedFileId == null)
		{
			IDEFile currentFile = IDEService.getCurrentFile(session);
			if (currentFile != null)
			{
				usedFileId = currentFile.getId();
			}
		}
		
		Integer currentFileIndex = 0;
		int i = 0;
		
		for (IDEFile file : openFilesList)
		{
			if (file.getId().equals(usedFileId))
			{
				currentFileIndex = i;
				break;
			}
			i++;
		}
		
		mv.addObject("currentFileIndex", currentFileIndex);
		mv.addObject("currentFileId", usedFileId);
		mv.addObject("files", openFilesList);
		return mv;
	}
	
	class OpenFileResult
	{
		private List<String> errors;
		private String sourceCode;
		private List<String> collapsedSections;
		
		public List<String> getErrors()
		{
			return errors;
		}
		public void setErrors(List<String> errors)
		{
			this.errors = errors;
		}
		public String getSourceCode()
		{
			return sourceCode;
		}
		public void setSourceCode(String sourceCode)
		{
			this.sourceCode = sourceCode;
		}
		public List<String> getCollapsedSections()
		{
			return collapsedSections;
		}
		public void setCollapsedSections(List<String> collapsedSections)
		{
			this.collapsedSections = collapsedSections;
		}
	}
	
	private String getJavaHints(EnvData env) throws KommetException
	{
		List<String> hints = new ArrayList<String>();
		
		for (Type type : env.getAccessibleTypes())
		{
			hints.add("{ \"displayText\": \"" + type.getApiName() + "\", \"text\": \"" + type.getQualifiedName() + "\", \"javaImport\": \"" + type.getQualifiedName() + "\" }");
		}
		
		for (Class cls : clsService.getClasses(null, env))
		{
			hints.add("{ \"displayText\": \"" + cls.getName() + "\", \"text\": \"" + cls.getQualifiedName() + "\", \"javaImport\": \"" + cls.getQualifiedName() + "\" }");
		}
		
		// add java keywords
		for (int i = 0; i < CodeUtils.javaKeywords.length; i++)
		{
			hints.add("{ \"text\": \"" + CodeUtils.javaKeywords[i] + "\", \"javaImport\": null }");
		}
		
		// add annotations
		for (java.lang.Class<?> cls : KollUtil.getAnnotations())
		{
			hints.add("{ \"displayText\": \"@" + cls.getSimpleName() + "\", \"text\": \"@" + cls.getSimpleName() + "\", \"javaImport\": \"" + cls.getName() + "\" }");
		}
		
		// imports
		for (java.lang.Class<?> cls : getAllKommetClasses(env))
		{
			hints.add("{ \"displayText\": \"" + cls.getSimpleName() + "\", \"text\": \"" + cls.getName() + "\", \"javaImport\": \"" + cls.getName() + "\" }");
		}
		
		for (String className : getAllJavaClasses())
		{
			hints.add("{ \"displayText\": \"" + className.substring(className.lastIndexOf(".") + 1) + "\", \"text\": \"" + className + "\", \"javaImport\": \"" + className + "\" }");
		}
		
		return "[ " + MiscUtils.implode(hints, ", ") + " ]";
	}
	
	private static Set<String> getAllJavaClasses() throws KommetException
	{
		allJavaClasses = null;
		if (allJavaClasses == null)
		{
			/*List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
			classLoadersList.add(ClasspathHelper.contextClassLoader());
			classLoadersList.add(ClasspathHelper.staticClassLoader());
			
			Reflections reflections = new Reflections(new ConfigurationBuilder()
		    	.setScanners(new SubTypesScanner(false), new ResourcesScanner())
		    	.setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
		    	.filterInputsBy(new FilterBuilder().includePackage("com")));*/
			
			allJavaClasses = new HashSet<String>();
			
			JarFile jarFile = null;
			try
			{
				jarFile = new JarFile(System.getProperty("java.home") + "/lib/jrt-fs.jar");
				
				Enumeration<?> allEntries = jarFile.entries();
	            while (allEntries.hasMoreElements())
	            {
	                JarEntry entry = (JarEntry) allEntries.nextElement();
	                if (!entry.isDirectory() && entry.getName().endsWith(".class") && !entry.getName().contains("$"))
	                {
	                	String name = entry.getName().replaceAll("[\\\\/]", ".").substring(0, entry.getName().lastIndexOf("."));
	                	allJavaClasses.add(name);
	                }
	            }
			}
			catch (IOException e)
			{
				e.printStackTrace();
				throw new KommetException("Could not read JRE classes");
			}
			finally
			{
				if (jarFile != null)
				{
					try
					{
						jarFile.close();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			// add some classes manually
			allJavaClasses.add(List.class.getName());
			allJavaClasses.add(ArrayList.class.getName());
			allJavaClasses.add(Set.class.getName());
			allJavaClasses.add(HashSet.class.getName());
		    	
			/*Reflections reflections = new Reflections(new ConfigurationBuilder().setScanners(new SubTypesScanner(false), new ResourcesScanner(), new TypeElementsScanner()));
			Set<String> typeSet = reflections.getStore().get("TypeElementsScanner").keySet();
			HashSet<java.lang.Class<? extends Object>> classes = Sets.newHashSet(ReflectionUtils.forNames(typeSet, reflections
					            .getConfiguration().getClassLoaders()));*/
			
			/*for (java.lang.Class<?> cls : reflections.getSubTypesOf(Object.class))
			{
				try
				{
					// filter classes that cannot be referenced and throw IllegalAccessError
					allJavaClasses.add(cls.getName());
				}
				catch (IllegalAccessError e)
				{
					// ignore
				}
			}*/
		}
		
		return allJavaClasses;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/ide/hints", method = RequestMethod.GET)
	@ResponseBody
	public void javaCodeCompletionHints(HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{	
		EnvData env = envService.getCurrentEnv(session);
		
		resp.setContentType("application/json; charset=UTF-8");
		PrintWriter out = resp.getWriter();
		out.write(RestUtil.getRestSuccessDataResponse("{ \"javaHints\": " + getJavaHints(env) + ", \"tags\": " + getRmTags(env) + ", \"vrel\": " + getVRELHints() + " }"));
	}

	private String getVRELHints()
	{
		List<String> hints = new ArrayList<String>();
		hints.add("{ \"text\": \"{{$resource.path.<name>}}\" }");
		hints.add("{ \"text\": \"{{$viewResource.path.<name>}}\" }");
		hints.add("{ \"text\": \"{{$userSetting.<name>}}\" }");
		//hints.add("resource.path.<resource-name>");
		//hints.add("viewResource.path.<resource-name>");
		//hints.add("userSetting.<setting-name>");
		
		return "[ " + MiscUtils.implode(hints, ", ") + " ]";
	}

	private String getRmTags(EnvData env) throws KommetException
	{
		List<String> serializedNamespaces = new ArrayList<String>();
		
		TagData tagData = TagData.get(env, appConfig);
		
		for (Namespace ns : tagData.getNamespaces())
		{
			List<String> serializedTags = new ArrayList<String>();
			
			for (Tag tag : ns.getTags())
			{
				List<String> serializedAttrs = new ArrayList<String>();
				for (Attribute attr : tag.getAttributes())
				{
					StringBuilder sb = new StringBuilder();
					sb.append("{ \"name\": \"").append(attr.getName()).append("\", ");
					sb.append("\"required\": ").append(attr.isRequired()).append(" }");
					
					serializedAttrs.add(sb.toString());
				}
				
				StringBuilder stag = new StringBuilder();
				stag.append("{ \"name\": \"").append(tag.getName()).append("\", ");
				stag.append("\"description\": \"").append(tag.getDescription()).append("\", ");
				stag.append("\"attributes\": [ ").append(MiscUtils.implode(serializedAttrs, ", ")).append(" ]");
				
				List<String> serializedChildren = new ArrayList<String>();
				for (Tag child : tag.getChildren())
				{
					serializedChildren.add("{ \"name\": \"" + child.getName() + "\" }");
				}
				
				stag.append(", \"children\": [ ").append(MiscUtils.implode(serializedChildren, ", ")).append(" ]");
				
				stag.append(" }");
				
				serializedTags.add(stag.toString());
			}
			
			serializedNamespaces.add("{ \"name\": \"" + ns.getName() + "\", \"tags\": [ " + MiscUtils.implode(serializedTags, ", ") + "] }");
		}
		
		return "[ " + MiscUtils.implode(serializedNamespaces, ", ") + " ]";
	}

	private List<java.lang.Class<?>> getAllKommetClasses(EnvData env)
	{
		List<java.lang.Class<?>> classes = new ArrayList<java.lang.Class<?>>();
		
		classes.add(Controller.class);
		classes.add(Action.class);
		classes.add(ActionConfig.class);
		classes.add(Public.class);
		classes.add(Param.class);
		classes.add(Params.class);
		classes.add(Rest.class);
		classes.add(ResponseBody.class);
		classes.add(Disabled.class);
		classes.add(ReturnsFile.class);
		classes.add(CrossOrigin.class);
		classes.add(ResponseBody.class);
		
		// add trigger annotations
		classes.add(Trigger.class);
		classes.add(BeforeInsert.class);
		classes.add(BeforeUpdate.class);
		classes.add(BeforeDelete.class);
		classes.add(AfterInsert.class);
		classes.add(AfterUpdate.class);
		classes.add(AfterDelete.class);
		
		classes.add(KeyPrefix.class);
		classes.add(KIDException.class);
		classes.add(KommetException.class);
		classes.add(EnvAlreadyExistsException.class);
		classes.add(PageData.class);
		classes.add(KeyPrefixException.class);
		classes.add(StandardObjectController.class);
		classes.add(BaseController.class);
		classes.add(CustomTypeRecordProxy.class);
		classes.add(Type.class);
		
		classes.add(MiscUtils.class);
		classes.add(kommet.utils.StringUtils.class);
		classes.add(JSON.class);
		classes.add(JexlScript.class);
		classes.add(TestResults.class);
		classes.add(TestError.class);
		classes.add(TestException.class);
		
		classes.add(HttpResponse.class);
		
		//classes.add(KerasModel.class);
		//classes.add(MLException.class);
		
		classes.add(Attachment.class);
		
		// add kommet.nn.* package
		/*classes.add(NetworkRunner.class);
		classes.add(NetworkRunnerException.class);
		classes.add(MultiLayerNetwork.class);
		classes.add(MultiLayerConfiguration.class);*/
			
		return classes;
	}
}