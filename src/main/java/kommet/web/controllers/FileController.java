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
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
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
import kommet.basic.FileRecordAssignment;
import kommet.basic.FileRevision;
import kommet.basic.Profile;
import kommet.config.UserSettingKeys;
import kommet.data.DataService;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.ValidationMessage;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.files.FileFilter;
import kommet.files.FileRecordAssignmentFilter;
import kommet.files.FileService;
import kommet.i18n.InternationalizationService;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;
import kommet.utils.UrlUtil;

@Controller
public class FileController extends CommonKommetController
{
	@Inject
	FileService fileService;
	
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig config;
	
	@Inject
	DataService dataService;
	
	@Inject
	InternationalizationService i18n;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	/**
	 * Maximum file size to upload, in bytes.
	 */
	private Integer DEFAULT_MAX_FILE_SIZE = 5000000;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/download/{fileId}", method = RequestMethod.GET)
	public void download(@PathVariable("fileId") String sFileId, HttpServletResponse response, HttpSession session) throws PropertyUtilException, KommetException
	{
		// TODO add a check for valid file ID
		KID fileId = KID.get(sFileId);
		AuthData authData = AuthUtil.getAuthData(session);
		
		EnvData env = envService.getCurrentEnv(session);
		
		try
	    {
			// get file from database
			FileFilter filter = new FileFilter();
			filter.addId(fileId);
			List<File> files = fileService.find(filter, true, true, authData, env);
			
			if (files.isEmpty())
			{
				// TODO add a check for valid file ID
				throw new KommetException("File not found");
			}
			
			File file = files.get(0);
			java.io.File systemFile = new java.io.File(config.getFileDir() + "/" + file.getLatestRevision().getPath());
			if (!systemFile.exists())
			{
				throw new KommetException("File not found on server");
			}
			
			// get your file as InputStream
			InputStream is = new FileInputStream(systemFile);
			
			response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\""); 
			
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
	
	/**
	 * Native upload method uploads a file and saves it.
	 * @param uploadedFileName
	 * @param sRevisionId
	 * @param filePart
	 * @param response
	 * @param req
	 * @param session
	 * @throws KommetException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/files/nativeupload", method = RequestMethod.POST)
	@ResponseBody
	public void nativeUpload (@RequestParam(value = "fileName", required = false) String uploadedFileName,
			@RequestParam(value = "revisionId", required = false) String sRevisionId,
			@RequestParam("uploadedFile") MultipartFile filePart,
            HttpServletResponse response, HttpServletRequest req, HttpSession session) throws KommetException, IOException
	{
		clearMessages();
		
		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();
		
		KID revisionId = null;
		EnvData env = envService.getCurrentEnv(session);
		FileRevision revision = null;
		File file = null;
		
		InputStream inputStream = null;
		Long fileSize = null;
		String originalFileName = null;
		
	    inputStream = filePart.getInputStream();
	    fileSize = filePart.getSize();
	    originalFileName = filePart.getOriginalFilename();
	    
	    if (!StringUtils.hasText(uploadedFileName))
		{
	    	uploadedFileName = originalFileName;
	    	
	    	if (!StringUtils.hasText(uploadedFileName))
	    	{
	    		out.write(RestUtil.getRestErrorResponse("File name not set"));
	    		return;
	    	}
		}
		
		OutputStream outputStream = null;
		AuthData authData = AuthUtil.getAuthData(session);
		
		Integer maxFileSize = uchService.getUserSettingAsInt(UserSettingKeys.KM_SYS_MAX_FILE_SIZE, authData, AuthData.getRootAuthData(env), env);
		if (maxFileSize == null)
		{
			maxFileSize = DEFAULT_MAX_FILE_SIZE;
		}
		
		try
		{	
			if (fileSize > 0)
			{	
				if (fileSize > maxFileSize)
				{
					out.write(RestUtil.getRestErrorResponse(authData.getI18n().get("files.maxsizeexceeded") + " " + maxFileSize + " " + authData.getI18n().get("files.maxsizeexceeded.bytes")));
					return;
				}
				
				String diskPath = null;
				
				// if it's an existing file, the file name and revision ID will be set
				if (StringUtils.hasText(sRevisionId))
				{
					try
					{
						revisionId = KID.get(sRevisionId);
					}
					catch (KIDException e)
					{
						out.write(RestUtil.getRestErrorResponse("Invalid revision ID " + sRevisionId));
						return;
					}
					
					// make sure the passed file name matches the revision ID to prevent malicious changing of paths
					revision = fileService.findRevision(revisionId, env);
					
					if (revision == null)
					{
						out.write(RestUtil.getRestErrorResponse("Revision with ID " + revisionId + " not found"));
						return;
					}
					
					diskPath = revision.getPath();
				}
				else
				{
					diskPath = MiscUtils.getHash(30) + "." + FilenameUtils.getExtension(originalFileName);
				}
				
				outputStream = new FileOutputStream(config.getFileDir() + "/" + diskPath);
				
				int readBytes = 0;
				byte[] buffer = new byte[maxFileSize];
				int totalBytes = 0;
				while ((readBytes = inputStream.read(buffer, 0, 10000)) != -1)
				{
					totalBytes += readBytes;
					outputStream.write(buffer, 0, readBytes);
				}
				outputStream.close();
				inputStream.close();
				
				if (revision != null)
				{
					file = revision.getFile();
					revision.setSize(totalBytes);
					revision = fileService.saveRevision(revision, authData, env);
				}
				else
				{
					file = new File();
					file.setAccess(File.PUBLIC_ACCESS);
					// save new file
					file.setName(uploadedFileName);
					file = fileService.saveFile(file, authData, env);
					
					// create file revision
					if (revision == null)
					{
						revision = new FileRevision();
						revision.setFile(file);
						
						// if it's a new file, it is uploaded before a revision and file object are created for it,
						// so we need to generate a file name here
						revision.setPath(diskPath);
					}
					
					revision.setName(file.getName());
					revision.setRevisionNumber(1);
					
					// get file size
					java.io.File uploadedFile = new java.io.File(config.getFileDir() + "/" + revision.getPath());
					revision.setSize(Long.valueOf(uploadedFile.length()).intValue());
					revision = fileService.saveRevision(revision, authData, env);
				}
				
				out.write("{ \"success\": true, \"fileName\": \"" + uploadedFileName + "\", \"originalFileName\": \"" + originalFileName + "\", \"fileRevisionId\": \"" + revision.getId() + "\", \"fileId\": \"" + file.getId() + "\" }");
			}
			else
			{
				out.write(RestUtil.getRestErrorResponse(authData.getI18n().get("files.emptyfile")));
				return;
			}
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse("Upload failed with message: " + e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/files/upload", method = RequestMethod.POST)
	@ResponseBody
	public void upload (UploadItem uploadItem, BindingResult result, @RequestParam(value = "fileName", required = false) String fileName,
			@RequestParam(value = "revisionId", required = false) String sRevisionId, @RequestParam(value = "fileParam", required = false) String fileParam,
            HttpServletResponse response, HttpServletRequest req, HttpSession session) throws KommetException, IOException
	{
		clearMessages();
		
		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();
		
		KID revisionId = null;
		EnvData env = envService.getCurrentEnv(session);
		String uploadedFileName = null;
		FileRevision revision = null;
		
		InputStream inputStream = null;
		Long fileSize = null;
		String originalFileName = null;
		
		MultipartFile file = uploadItem.getFileData();
		
		if (file == null)
		{
			out.write(getErrorJSON("No file selected for upload"));
			return;
		}
		
		inputStream = file.getInputStream();
		originalFileName = uploadItem.getFileData().getOriginalFilename();
		fileSize = file.getSize();

		// if it's an existing file, the file name and revision ID will be set
		if (StringUtils.hasText(fileName) && StringUtils.hasText(sRevisionId))
		{
			try
			{
				revisionId = KID.get(sRevisionId);
			}
			catch (KIDException e)
			{
				out.write(getErrorJSON("Invalid revision ID " + sRevisionId));
				return;
			}
			
			if (!StringUtils.hasText(fileName))
			{
				out.write(getErrorJSON("File name not set"));
				return;
			}
			
			// make sure the passed file name matches the revision ID to prevent malicious changing of paths
			revision = fileService.findRevision(revisionId, env);
			
			if (revision == null)
			{
				out.write(getErrorJSON("Revision with ID " + revisionId + " not found"));
				return;
			}
			
			if (!fileName.equals(revision.getPath()))
			{
				out.write(getErrorJSON("Revision ID does not match the path"));
				return;
			}
			
			uploadedFileName = fileName;
		}
		else if (!StringUtils.hasText(fileName) && !StringUtils.hasText(sRevisionId))
		{
			// if it's a new file, it is uploaded before a revision and file object are created for it,
			// so we need to generate a file name here
			uploadedFileName = MiscUtils.getHash(30) + "." + FilenameUtils.getExtension(originalFileName);
		}
		else
		{
			out.write(getErrorJSON("Either both revision ID and file name or none have to be set"));
			return;
		}
		
		OutputStream outputStream = null;
		AuthData authData = AuthUtil.getAuthData(session);
		
		Integer maxFileSize = uchService.getUserSettingAsInt(UserSettingKeys.KM_SYS_MAX_FILE_SIZE, authData, AuthData.getRootAuthData(env), env);
		if (maxFileSize == null)
		{
			maxFileSize = DEFAULT_MAX_FILE_SIZE;
		}
		
		try
		{	
			if (fileSize > 0)
			{	
				if (fileSize > maxFileSize)
				{
					out.write(getErrorJSON(authData.getI18n().get("files.maxsizeexceeded") + " 5Mb."));
					return;
				}
				
				outputStream = new FileOutputStream(config.getFileDir() + "/" + uploadedFileName);
				
				int readBytes = 0;
				byte[] buffer = new byte[maxFileSize];
				int totalBytes = 0;
				while ((readBytes = inputStream.read(buffer, 0, 10000)) != -1)
				{
					totalBytes += readBytes;
					outputStream.write(buffer, 0, readBytes);
				}
				outputStream.close();
				inputStream.close();
				
				// if it's not a new file and revision exists, save the size of the file
				// NOTE: for new files the size will be set when the file is saved
				if (revision != null)
				{
					revision.setSize(totalBytes);
					fileService.saveRevision(revision, authData, env);
				}
				
				out.write("{ \"status\": \"success\", \"fileName\": \"" + uploadedFileName + "\", \"originalFileName\": \"" + originalFileName + "\" }");
			}
			else
			{
				out.write(getErrorJSON(authData.getI18n().get("files.emptyfile")));
				return;
			}
		}
		catch (Exception e)
		{
			out.write(getErrorJSON("Upload failed with message: " + e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/unassignfile", method = RequestMethod.POST)
	@ResponseBody
	public void unassignFile (@RequestParam(value = "assignmentId", required = false) String assignmentIdStr, HttpServletResponse response, HttpSession session) throws IOException
	{
		PrintWriter out = response.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		
		KID assignmentId;
		try
		{
			assignmentId = KID.get(assignmentIdStr);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid assignment ID " + assignmentIdStr));
			return;
		}
		
		EnvData env;
		try
		{
			env = envService.getCurrentEnv(session);
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error reading environment: " + e.getMessage()));
			return;
		}
		
		// find assignment
		FileRecordAssignmentFilter filter = new FileRecordAssignmentFilter();
		filter.addId(assignmentId);
		filter.setInitFiles(true);
		List<FileRecordAssignment> assignments = null;
		try
		{
			assignments = fileService.findAssignments(filter, authData, env);
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error finding assignments with ID " + assignmentIdStr + ": " + e.getMessage()));
			return;
		}
		
		FileRecordAssignment assignment = assignments.get(0);
		
		// TODO it is not optimal to use "unassign" below, we could just remove the assignment object
		try
		{
			// unassign file
			fileService.unassignFileToRecord(assignment.getFile().getId(), assignment.getRecordId(), true, null, env);
			
			// check if the file is assigned to other records, if not, delete it
			filter = new FileRecordAssignmentFilter();
			filter.addFileId(assignment.getFile().getId());
			if (fileService.findAssignments(filter, authData, env).isEmpty())
			{
				fileService.deleteFile(assignment.getFile(), true, null, env);
			}
			
			out.write("{ \"result\": \"success\" }");
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Could not unassign file: " + e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/files/save", method = RequestMethod.POST)
	@ResponseBody
	public void save (@RequestParam(value = "fileId", required = false) String fileIdStr,
			@RequestParam(value = "systemFileName", required = false) String systemFileName,
			@RequestParam(value = "fileName", required = false) String fileName,
			@RequestParam(value = "recordId", required = false) String sRecordId,
			@RequestParam(value = "comment", required = false) String comment,
			@RequestParam(value = "assocFieldIds", required = false) String assocFieldIds,
			@RequestParam(value = "widgetId", required = false) String widgetId,
			@RequestParam(value = "parentDialog", required = false) String parentDialog,
			HttpServletResponse response, HttpSession session) throws IOException
	{
		clearMessages();
		response.setContentType("text/json; charset=UTF-8");
		PrintWriter out = response.getWriter();
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		KID fileId = null;
		KID recordId = null;
		
		if (StringUtils.hasText(sRecordId))
		{
			if (StringUtils.hasText(fileIdStr))
			{
				// if file ID and record ID are not null, it means that we are trying to assign an existing
				// file to a record - it would be possible, but we don't support it in this flow
				out.write(getErrorJSON("It is not allowed to associated existing files with records"));
				return;
			}
			
			try
			{
				recordId = KID.get(sRecordId);
			}
			catch (KIDException e)
			{
				out.write(getErrorJSON("Invalid record ID " + sRecordId));
				return;
			}
		}
		
		try
		{
			File file = null;
			EnvData env = envService.getCurrentEnv(session);
			
			if (!StringUtils.hasText(fileName))
			{
				// TODO return edit page with error message instead
				out.write(getErrorJSON("File name not set"));
				return;
			}
			
			if (StringUtils.hasText(fileIdStr))
			{
				try
				{
					fileId = KID.get(fileIdStr);
				}
				catch (KIDException e1)
				{
					out.write(getErrorJSON("Invalid file ID " + fileIdStr));
					return;
				}
				
				if (StringUtils.hasText(systemFileName))
				{
					out.write(getErrorJSON("When updating an existing file, it is not necessary to give a file name of the uploaded item"));
					return;
				}
				
				// get file by ID
				FileFilter filter = new FileFilter();
				filter.addId(fileId);
				List<File> files = fileService.find(filter, true, true, null, env);
				file = files.get(0);
			}
			else
			{
				file = new File();
				file.setAccess(File.PUBLIC_ACCESS);
			}
			
			file.setName(fileName);
			// save the file
			file = fileService.saveFile(file, authData, env);
			
			boolean existingFile = fileId != null;
			fileId = file.getId();
			
			if (!existingFile)
			{
				// create file revision
				FileRevision revision = new FileRevision();
				revision.setFile(file);
				revision.setName(file.getName());
				revision.setPath(systemFileName);
				revision.setRevisionNumber(1);
				
				// get file size
				java.io.File uploadedFile = new java.io.File(config.getFileDir() + "/" + revision.getPath());
				revision.setSize(Long.valueOf(uploadedFile.length()).intValue());
				fileService.saveRevision(revision, authData, env);
			}
			// if systemFileName is set, then it means a new version of the file has be uploaded
			// and we want to store it as the latest revision, without creating a new one
			else if (StringUtils.hasText(systemFileName))
			{
				// get system file name from latest revision and rename the one recently uploaded
				java.io.File uploadedFile = new java.io.File(config.getFileDir() + "/" + systemFileName);
				if (!uploadedFile.exists())
				{
					throw new KommetException("Uploaded file " + systemFileName + " not found in disk storage");
				}
				
				// TODO update revision size here
				
				// rename
				uploadedFile.renameTo(new java.io.File(config.getFileDir() + "/" + file.getLatestRevision().getPath()));
			}
			
			if (recordId != null)
			{
				// make sure the user has edit rights on the record
				// TODO when per-record rights are implemented, change the canEditType method below
				// to canEditRecord
				if (!authData.canEditType(env.getTypeByRecordId(recordId).getKID(), false, env))
				{
					out.write(getErrorJSON("File cannot be assigned to record " + recordId + " due to insufficient rights on the records."));
					return;
				}
				
				fileService.assignFileToRecord(fileId, recordId, comment, authData, env);
				
				// if it has been requested to associate the file with the record through other
				// association fields, do this
				if (StringUtils.hasText(assocFieldIds))
				{
					for (String sAssocFieldId : assocFieldIds.split(";"))
					{
						dataService.associate(KID.get(sAssocFieldId), recordId, fileId, authData, env);
					}
				}
			}
			
			out.write("{ \"result\": \"success\", \"widgetId\": \"" + (widgetId != null ? widgetId : "") + "\", \"parentDialog\": \"" + (parentDialog != null ? parentDialog : "") + "\", \"fileId\": \"" + fileId.getId() + "\", \"recordId\": \"" + sRecordId + "\" }");
		}
		catch (FieldValidationException e)
		{	
			List<String> msgs = new ArrayList<String>();
			for (ValidationMessage msg : e.getMessages())
			{
				msgs.add(msg.getText());
			}
			out.write(getErrorJSON("Error uploading file: " + MiscUtils.implode(msgs, ", ")));
		}
		catch (Exception e)
		{
			out.write(getErrorJSON("Error uploading file: " + e.getMessage()));
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/files/list", method = RequestMethod.GET)
	public ModelAndView list (HttpSession session) throws KommetException
	{
		return new ModelAndView("files/list");
		//List<File> filesWithRevisions = fileService.find(null, true, true, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		//mv.addObject("files", filesWithRevisions);
		//return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/files/edit/{fileId}", method = RequestMethod.GET)
	public ModelAndView edit (@PathVariable(value = "fileId") String fileId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("files/edit");
		addLayoutPath(uchService, mv, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		return getFileDetails(mv, fileId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
	}
	
	private ModelAndView getFileDetails(ModelAndView mv, String fileId, AuthData authData, EnvData env) throws KommetException
	{
		KID fileKID = null;
		try
		{
			fileKID = KID.get(fileId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Incorrect file ID " + fileId);
		}
		
		FileFilter filter = new FileFilter();
		filter.addId(fileKID);
		List<File> filesWithRevisions = fileService.find(filter, true, true, authData, env);
		
		if (filesWithRevisions.isEmpty())
		{
			mv.addObject("errorMsgs", getMessage("File not found"));
		}
		else
		{
			File file = filesWithRevisions.get(0);
			UploadItem uploadItem = new UploadItem();
			uploadItem.setFilename(file.getName());
			mv.addObject("uploadItem", uploadItem);
			mv.addObject("file", file);
			mv.addObject("downloadLabel", i18n.get(authData.getUser().getLocaleSetting(), "files.download"));
			mv.addObject("uploadLabel", i18n.get(authData.getUser().getLocaleSetting(), "files.upload"));
		}
		
		addLayoutPath(uchService, mv, authData, env);
		return mv;
	}

	/**
	 * Displays file details.
	 * @param fileId
	 * @param sRecordId
	 * @param session
	 * @return
	 * @throws KommetException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/files/{fileId}", method = RequestMethod.GET)
	public ModelAndView details (@PathVariable(value = "fileId") String fileId,
								@RequestParam(value = "recordId", required = false) String sRecordId,
								HttpSession session) throws KommetException
	{	
		ModelAndView mv = new ModelAndView("files/details");
		AuthData authData = AuthUtil.getAuthData(session);
		
		KID recordId;
		if (StringUtils.hasText(sRecordId))
		{
			EnvData env = envService.getCurrentEnv(session);
			
			try
			{
				recordId = KID.get(sRecordId);
			}
			catch (KIDException e)
			{
				return getErrorPage("Invalid record ID " + sRecordId);
			}
			
			mv.addObject("recordId", recordId);
			mv.addObject("recordTypeLabel", env.getTypeByRecordId(recordId).getLabel());
			
			// find the file object assignment
			FileRecordAssignmentFilter filter = new FileRecordAssignmentFilter();
			filter.addFileId(KID.get(fileId));
			filter.addRecordId(recordId);
			List<FileRecordAssignment> assignments = fileService.findAssignments(filter, authData, env);
			if (!assignments.isEmpty())
			{
				mv.addObject("fileObjAssignment", assignments.get(0));
			}
			else
			{
				return getErrorPage("File " + fileId + " is not assigned to record " + sRecordId);
			}
		}
		
		return getFileDetails(mv, fileId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
	}
	
	/**
	 * Displays file details.
	 * @param fileId
	 * @param sRecordId
	 * @param session
	 * @return
	 * @throws KommetException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/filerevisions/{revId}", method = RequestMethod.GET)
	public ModelAndView revisionDetails (@PathVariable(value = "revId") String revisionId,	HttpSession session) throws KommetException
	{	
		ModelAndView mv = new ModelAndView("files/revisiondetails");
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		FileRevision rev = fileService.getFileRevision(KID.get(revisionId), authData, env);
		rev.setFile(fileService.getFileById(rev.getFile().getId(), env));
		mv.addObject("rev", rev);
		return mv;
	}
	
	/**
	 * Opens an upload screen where new file can be uploaded, edited and saved.
	 * @param sRecordId
	 * @param assocFieldIds
	 * @param widgetId
	 * @param parentDialogVar The name of the Javascript variable representing a dialog that displays this page. The Javascript variable must represent an km.js.ui.dialog object.
	 * @param session
	 * @param req
	 * @return
	 * @throws KommetException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/files/new", method = RequestMethod.GET)
	public ModelAndView newFile (@RequestParam(value = "recordId", required = false) String sRecordId,
								@RequestParam(value = "assocFieldIds", required = false) String assocFieldIds,
								@RequestParam(value = "widgetId", required = false) String widgetId,
								@RequestParam(value = "parentDialog", required = false) String parentDialogVar,
								HttpSession session, HttpServletRequest req) throws KommetException
	{
		ModelAndView mv = new ModelAndView("files/edit");
		mv.addObject("uploadItem", new UploadItem());
		mv.addObject("parentDialog", parentDialogVar);
		EnvData env = envService.getCurrentEnv(session);
		
		if (!StringUtils.hasText(widgetId) && !StringUtils.hasText(parentDialogVar))
		{
			mv.addObject("errorMsgs", getMessage("File upload page cannot be called with both widget ID and parentDialog parameters"));
			return mv;
		}
		
		if (StringUtils.hasText(sRecordId))
		{
			KID recordId;
			try
			{
				recordId = KID.get(sRecordId);
			}
			catch (KIDException e)
			{
				return getErrorPage("Invalid record ID " + sRecordId);
			}
			
			mv.addObject("recordId", recordId);
			
			// if this file is to be linked with the record by anything more than the usual
			// record-file assignment, this property contains a list of comma-separated IDs
			// of association fields on the record's type
			mv.addObject("assocFieldIds", assocFieldIds);
			mv.addObject("widgetId", widgetId);
			
			mv.addObject("recordTypeLabel", env.getTypeByRecordId(recordId).getLabel());
			mv.addObject("i18n", i18n.getDictionary(AuthUtil.getAuthData(session).getUser().getLocaleSetting()));
			
			addRmParams(mv, req, env);
			
			if (StringUtils.hasText(widgetId) || StringUtils.hasText(parentDialogVar))
			{
				// if page is run in widget mode or as a dialog, it will always be rendered with blank layout
				addLayoutPath(mv, env.getBlankLayoutId(), env);
			}
			else
			{
				// render page with standard layout
				addLayoutPath(uchService, mv, AuthUtil.getAuthData(session), env);
			}
		}
		return mv;
	}
}