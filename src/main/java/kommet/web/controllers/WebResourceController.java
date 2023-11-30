/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.File;
import kommet.basic.MimeTypes;
import kommet.basic.WebResource;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.files.FileFilter;
import kommet.files.FileService;
import kommet.rest.RestUtil;
import kommet.services.WebResourceService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class WebResourceController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	WebResourceService webResourceService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	FileService fileService;
	
	@Inject
	SharingService sharingService;
	
	// Maximum file size to upload, in bytes
	private Integer MAX_FILE_SIZE = 5000000;
	
	@SuppressWarnings("deprecation")
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/downloadresource", method = RequestMethod.GET)
	public void downloadWebResource(@RequestParam("name") String name, HttpServletResponse response, HttpSession session) throws PropertyUtilException, KommetException
	{
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		WebResource res = webResourceService.getByName(name, authData, env);
		
		if (res == null)
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Invalid web resource name " + name);
			return; 
		}
		
		try
	    {
			// get file from database
			FileFilter filter = new FileFilter();
			filter.addId(res.getFile().getId());
			
			// query files - if this is a public web resource, we make all files accessible, otherwise we only make the file accessible if the user has access to it
			List<File> files = fileService.find(filter, true, true, Boolean.TRUE.equals(res.getIsPublic()) ? null : authData, env);
			
			if (files.isEmpty())
			{
				throw new KommetException("File not found");
			}
			
			File file = files.get(0);
			java.io.File systemFile = new java.io.File(appConfig.getFileDir() + "/" + file.getLatestRevision().getPath());
			if (!systemFile.exists())
			{
				throw new KommetException("File not found on server");
			}
			
			// get your file as InputStream
			InputStream is = new FileInputStream(systemFile);
			
			String ext = MimeTypes.getExtension(res.getMimeType());
			response.setHeader("Content-Disposition", "attachment; filename=\"" + res.getName() + "." + ext + "\""); 
			
			// copy it to response's OutputStream
			IOUtils.copy(is, response.getOutputStream());
			
			//response.setContentType("application/rtf");
			response.flushBuffer();
	    }
		catch (IOException ex)
		{
			throw new KommetException("IOError writing file to output stream");
	    }
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/webresources/list", method = RequestMethod.GET)
	public ModelAndView list(HttpServletRequest req, HttpSession session) throws KommetException
	{ 	
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "Web resources", appConfig.getBreadcrumbMax(), session, getContextPath(session));
		
		ModelAndView mv = new ModelAndView("webresources/list");
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/webresources/new", method = RequestMethod.GET)
	public ModelAndView newResource() throws KommetException
	{
		ModelAndView mv = new ModelAndView("webresources/edit");
		mv.addObject("uploadItem", new UploadItem());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/webresources/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("id") String resourceId, HttpServletRequest req, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		WebResource resource = webResourceService.get(KID.get(resourceId), env);
		AuthData authData = AuthUtil.getAuthData(session);
		
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), resource.getName(), appConfig.getBreadcrumbMax(), session, getContextPath(session));
		
		ModelAndView mv = new ModelAndView("webresources/details");
		mv.addObject("resource", resource);
		mv.addObject("resourceName", resource.getFile().getName());
		mv.addObject("downloadLabel", authData.getI18n().get("files.download"));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/webresources/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("id") String resourceId, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		WebResource resource = webResourceService.get(KID.get(resourceId), env);
		
		ModelAndView mv = new ModelAndView("webresources/edit");
		mv.addObject("resource", resource);
		mv.addObject("resourceName", resource.getFile().getName());
		mv.addObject("uploadItem", new UploadItem());
		
		// web resource is public if it is shared with the guest user
		mv.addObject("isPublic", sharingService.canViewRecord(resource.getId(), env.getGuestUser().getId(), env));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/webresources/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete(@RequestParam("resourceId") String id, HttpSession session, HttpServletResponse response) throws IOException, KommetException
	{
		KID pageId = KID.get(id);
		EnvData env = envService.getCurrentEnv(session);
		PrintWriter out = response.getWriter();
		
		try
		{
			webResourceService.delete(pageId, true, AuthUtil.getAuthData(session), env);
			out.write(RestUtil.getRestSuccessResponse("Web resource deleted"));
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse("Deleting web resource failed. Nested: " + e.getMessage()));
		}	
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/webresources/save", method = RequestMethod.POST)
	public ModelAndView save (UploadItem uploadItem,
			@RequestParam(value = "resourceName", required = false) String resourceName,
			@RequestParam(value = "resourceId", required = false) String sResourceId,
			@RequestParam(value = "isPublic", required = false) String isPublic,
            HttpSession session) throws KommetException, IOException
	{
		clearMessages();
		
		WebResource resource = null;
		EnvData env = envService.getCurrentEnv(session);
		KID resourceFileId = null;
		
		if (StringUtils.hasText(sResourceId))
		{
			// get existing resource
			resource = webResourceService.get(KID.get(sResourceId), env);
			resourceFileId = resource.getFile().getId();
		}
		else
		{
			resource = new WebResource();
		}
		
		String diskFileName = null;
		
		resource.setName(resourceName);
		resource.setPublic("true".equals(isPublic));
		
		if (!StringUtils.hasText(resourceName))
		{
			addError("Name not specified");
		}
		else if (!ValidationUtil.isValidOptionallyQualifiedResourceName(resourceName))
		{
			addError("Invalid web resource name. It must be a qualified name, e.g. \"images.MyLogo\"");
		}
		
		MultipartFile file = uploadItem.getFileData();
		
		if (file == null)
		{
			addError("No file selected for upload");
		}
		else
		{
			diskFileName = MiscUtils.getHash(30) + "." + FilenameUtils.getExtension(uploadItem.getFileData().getOriginalFilename());
			
			try
			{
				String mimeType = MimeTypes.getFromExtension(FilenameUtils.getExtension(file.getOriginalFilename()));
				resource.setMimeType(mimeType);
			}
			catch (KommetException e)
			{
				addError("Unsupported file extension");
			}
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("webresources/edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("resource", resource);
			mv.addObject("resourceName", resourceName);
			return mv;
		}
		
		InputStream inputStream = null;
		OutputStream outputStream = null;
		AuthData authData = AuthUtil.getAuthData(session);
		
		try
		{	
			File resourceFile = null;
			
			// if file.name is not empty, then it means that a new file has been selected in the form and has to be uploaded
			if (StringUtils.hasText(file.getName()))
			{
				if (file.getSize() > 0)
				{
					inputStream = file.getInputStream();
					
					if (file.getSize() > MAX_FILE_SIZE)
					{
						ModelAndView mv = new ModelAndView("webresources/edit");
						mv.addObject("errorMsgs", getMessage(authData.getI18n().get("files.maxsizeexceeded") + " 5Mb."));
						mv.addObject("resource", resource);
						mv.addObject("resourceName", resourceName);
						return mv;
					}
					
					outputStream = new FileOutputStream(appConfig.getFileDir() + "/" + diskFileName);
					
					int readBytes = 0;
					byte[] buffer = new byte[MAX_FILE_SIZE];
					
					while ((readBytes = inputStream.read(buffer, 0, 10000)) != -1)
					{
						outputStream.write(buffer, 0, readBytes);
					}
					outputStream.close();
					inputStream.close();
					
					// save the file
					resourceFile = fileService.saveFile(resourceFileId, resourceName, diskFileName, File.PUBLIC_ACCESS, true, authData, env);
					resource.setFile(resourceFile);
				}
				else if (resource.getId() == null)
				{
					// file has not been uploaded, and the resource ID is null, so it is a new resource and a file is required
					
					ModelAndView mv = new ModelAndView("webresources/edit");
					mv.addObject("errorMsgs", getMessage(authData.getI18n().get("files.emptyfile")));
					mv.addObject("resource", resource);
					mv.addObject("resourceName", resourceName);
					return mv;
				}
			}
			
			resource = webResourceService.save(resource, true, authData, env);
			
			return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/webresources/" + resource.getId());
		}
		catch (Exception e)
		{
			ModelAndView mv = new ModelAndView("webresources/edit");
			mv.addObject("errorMsgs", getMessage("Upload failed with message: " + e.getMessage()));
			mv.addObject("resource", resource);
			mv.addObject("resourceName", resourceName);
			return mv;
		}
	}
}