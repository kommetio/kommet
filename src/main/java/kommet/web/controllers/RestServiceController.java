/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Class;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.View;
import kommet.basic.keetle.ViewService;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.ValidationMessage;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.json.JSON;
import kommet.koll.ClassCompilationException;
import kommet.koll.ClassService;
import kommet.rest.RestUtil;
import kommet.testing.TestService;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class RestServiceController extends BasicRestController
{
	@Inject
	EnvService envService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	ClassService classService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ErrorLogService errorLogService;
	
	@Inject
	TestService testService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DAL_URL, method = RequestMethod.GET)
	@ResponseBody
	public void dalQuery(@RequestParam(value = "q", required = true) String dalQuery,
						@RequestParam(value = "env", required = true) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpServletResponse resp, HttpSession session) throws KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		try
		{
			List<Record> records = restInfo.getEnv().getSelectCriteriaFromDAL(dalQuery).list();
			resp.setContentType("text/json; charset=UTF-8");
			
			returnRestRecords(records, restInfo.getAuthData(), restInfo.getOut());
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			// insert error log, but this is only possible if user data is available
			if (restInfo.getAuthData() != null)
			{
				errorLogService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, restInfo.getAuthData().getUserId(), restInfo.getAuthData(), restInfo.getEnv());
			}
			returnRestError("Error executing DAL query" + (e.getMessage() != null ? ": " + JSON.escape(e.getMessage()) : ""), restInfo.getOut());
			//returnRestError("Error executing DAL query''", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
	
	// TODO write unit tests for this method
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_ASSOCIATE_URL, method = RequestMethod.POST)
	@ResponseBody
	public void assignRecordToRelationship(@RequestParam(value = "relationFieldId", required = false) String sRelationFieldId,
						@RequestParam(value = "parentId", required = false) String sParentId,
						@RequestParam(value = "childId", required = false) String sChildId,
						@RequestParam(value = "code", required = false) String code,
						@RequestParam(value = "env", required = false) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpServletResponse resp, HttpSession session) throws KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		KID relationFieldId = null;
		
		try
		{
			relationFieldId = KID.get(sRelationFieldId);
		}
		catch (KIDException e)
		{
			returnRestError("Invalid relation field ID " + sRelationFieldId, restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		Field relationField = dataService.getField(relationFieldId, restInfo.getEnv());
		
		KID childId = null;
		
		try
		{
			childId = KID.get(sChildId);
		}
		catch (KIDException e)
		{
			returnRestError("Invalid child ID " + sChildId, restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if (relationField.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			Record linkingRecord = dataService.associate(relationFieldId, KID.get(sParentId), childId, restInfo.getAuthData(), restInfo.getEnv());
			restInfo.getOut().write("{ \"success\": true, \"id\": \"" + linkingRecord.getKID() + "\" }");
			return;
		}
		else if (relationField.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
		{
			InverseCollectionDataType dt = (InverseCollectionDataType)relationField.getDataType();
			List<Record> children = dataService.getRecords(Arrays.asList(childId), dt.getInverseType(), Arrays.asList(dt.getInverseProperty()), restInfo.getAuthData(), restInfo.getEnv());
			
			if (children.isEmpty())
			{
				returnRestError("Record with ID " + sChildId + " not found or inaccessible", restInfo.getOut());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			String inversePropertyToSet = dt.getInverseProperty();
			
			// check if the inverse property is a type reference
			// TODO we don't allow any other kinds of inverse properties than type references, so why use this check?
			// or will we want to have simple KIDDataType as inverse property in the future?
			if (dt.getInverseType().getField(inversePropertyToSet).getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				inversePropertyToSet += "." + Field.ID_FIELD_NAME;
			}
			
			children.get(0).setField(inversePropertyToSet, KID.get(sParentId), restInfo.getEnv());
			
			try
			{
				dataService.save(children.get(0), restInfo.getEnv());
				
				// return success response
				restInfo.getOut().write("{ \"success\": true, \"id\": \"" + sChildId + "\" }");
				return;
			}
			catch (Exception e)
			{
				restInfo.getOut().write(RestUtil.getRestErrorResponse("Could not associate records. The reason is: " + e.getMessage()));
				return;
			}
		}
		else
		{
			returnRestError("Field " + relationField.getApiName() + " does not represent a relationship", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
	
	// TODO write unit tests for this method
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_UNASSOCIATE_URL, method = RequestMethod.POST)
	@ResponseBody
	public void unassignRecordFromRelationship(@RequestParam(value = "relationFieldId", required = false) String sRelationFieldId,
						@RequestParam(value = "parentId", required = false) String sParentId,
						@RequestParam(value = "childId", required = false) String sChildId,
						@RequestParam(value = "code", required = false) String code,
						@RequestParam(value = "env", required = false) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpServletResponse resp, HttpSession session) throws KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			//System.out.println("Err " + restInfo.getError() + ", code " + restInfo.getRespCode());
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		KID relationFieldId = null;
		
		try
		{
			relationFieldId = KID.get(sRelationFieldId);
		}
		catch (KIDException e)
		{
			returnRestError("Invalid relation field ID " + sRelationFieldId, restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		Field relationField = dataService.getField(relationFieldId, restInfo.getEnv());
		
		KID childId = null;
		
		try
		{
			childId = KID.get(sChildId);
		}
		catch (KIDException e)
		{
			returnRestError("Invalid child ID " + sChildId, restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if (relationField.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			dataService.unassociate(relationFieldId, KID.get(sParentId), childId, false, restInfo.getAuthData(), restInfo.getEnv());
			restInfo.getOut().write("{ \"success\": true }");
			return;
		}
		else if (relationField.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
		{
			InverseCollectionDataType dt = (InverseCollectionDataType)relationField.getDataType();
			List<Record> children = dataService.getRecords(Arrays.asList(childId), dt.getInverseType(), Arrays.asList(dt.getInverseProperty()), restInfo.getAuthData(), restInfo.getEnv());
			
			if (children.isEmpty())
			{
				returnRestError("Record with ID " + sChildId + " not found or inaccessible", restInfo.getOut());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			children.get(0).setField(dt.getInverseProperty(), SpecialValue.NULL);
			dataService.save(children.get(0), restInfo.getEnv());
			
			// return success response
			restInfo.getOut().write("{ \"success\": true }");
			return;
		}
		else
		{
			returnRestError("Field " + relationField.getApiName() + " does not represent a relationship", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_VIEW_URL, method = RequestMethod.POST)
	@ResponseBody
	public void saveView(@RequestParam(value = "id", required = false) String sViewId,
						@RequestParam(value = "code", required = false) String code,
						@RequestParam(value = "env", required = true) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpServletResponse resp, HttpSession session) throws KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			//System.out.println("Err " + restInfo.getError() + ", code " + restInfo.getRespCode());
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		// only admin users can save classes
		if (!AuthUtil.canModifyCode(restInfo.getAuthData()))
		{
			returnRestError("Insufficient privileges to modify views", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		if (!StringUtils.hasText(code))
		{
			returnRestError("View code is empty", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		View view = null;
		if (StringUtils.hasText(sViewId))
		{	
			try
			{
				// find view by id
				view = viewService.getView(KID.get(sViewId), restInfo.getEnv());
			}
			catch (KIDException e)
			{
				returnRestError("Invalid view ID '" + sViewId + "'", restInfo.getOut());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		}
		else
		{
			view = new View();
			view.setIsSystem(false);
		}
		
		try
		{
			// save view
			view = viewService.fullSave(view, code, true, restInfo.getAuthData(), restInfo.getEnv());
			restInfo.getOut().write("{ \"success\": true, \"id\": \"" + view.getId() + "\" }");
			return;
		}
		catch (UniqueCheckViolationException e)
		{
			returnRestError("Unique check violated. Probably a view with the given name/package already exists", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			returnRestError(e.getMessage() != null ? e.getMessage() : "Unknown error while saving view", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DELETE_CLASS_URL, method = RequestMethod.POST)
	@ResponseBody
	public void deleteClass(@RequestParam(value = "id", required = false) String sClassId,
						@RequestParam(value = "env", required = false) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpServletResponse resp, HttpSession session) throws KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		if (!StringUtils.hasText(sClassId))
		{
			returnRestError("Class ID not specified", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if (!sClassId.startsWith(KID.CLASS_PREFIX))
		{
			returnRestError("Provided ID is not a correct ID of a class", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		KID classId = null;
		
		try
		{
			classId = KID.get(sClassId);
		}
		catch (KIDException e)
		{
			returnRestError("Invalid class ID " + sClassId, restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		// TODO before actually deleting the class, check if it is not used anywhere
		// e.g. by changing its name to some random one and trying to compile all the other classes
		
		try
		{
			classService.delete(classId, dataService, restInfo.getAuthData(), restInfo.getEnv());
			restInfo.getOut().write("{ \"success\": true, \"id\": \"" + classId + "\" }");
			return;
		}
		catch (Exception e)
		{
			returnRestError("Error deleting class: " + e.getMessage(), restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DELETE_RECORD_URL, method = RequestMethod.POST)
	@ResponseBody
	public void deleteRecord(@RequestParam(value = "id", required = false) String sRecordId,
						@RequestParam(value = "env", required = false) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpServletResponse resp, HttpSession session) throws KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			//System.out.println("Err " + restInfo.getError() + ", code " + restInfo.getRespCode());
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		if (!StringUtils.hasText(sRecordId))
		{
			returnRestError("Record ID not specified", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		KID recordId = null;
		
		try
		{
			recordId = KID.get(sRecordId);
		}
		catch (KIDException e)
		{
			returnRestError("Invalid object ID " + sRecordId, restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		Type type = restInfo.getEnv().getTypeByRecordId(recordId);
		if (type == null)
		{
			returnRestError("Record ID " + sRecordId + " cannot be matched against any type", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if (!restInfo.getAuthData().canDeleteType(type.getKID(), false, restInfo.getEnv()))
		{
			returnRestError("Insufficient privileges to delete record of type " + type.getQualifiedName(), restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		try
		{
			dataService.deleteRecord(recordId, restInfo.getAuthData(), restInfo.getEnv());
			restInfo.getOut().write("{ \"success\": true, \"id\": \"" + recordId + "\" }");
			return;
		}
		catch (Exception e)
		{
			returnRestError("Error deleting record: " + e.getMessage(), restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_RUN_TEST_URL, method = RequestMethod.GET)
	@ResponseBody
	public void runTest (@RequestParam(value = "class", required = true) String className, @RequestParam(value = "methods", required = true) String methodList, HttpSession session) throws KommetException
	{
		List<String> methods = MiscUtils.splitAndTrim(methodList, ",");
		EnvData env = envService.getCurrentEnv(session);
		testService.run(className, methods, env);
	}
	
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_CLASS_URL, method = RequestMethod.POST)
	@ResponseBody
	public void saveClass(@RequestParam(value = "id", required = false) String sFileId,
						@RequestParam(value = "name", required = false) String qualifiedName,
						@RequestParam(value = "code", required = false) String code,
						@RequestParam(value = "env", required = true) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpServletResponse resp, HttpSession session) throws KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		// only admin users can save classes
		if (!AuthUtil.canModifyCode(restInfo.getAuthData()))
		{
			returnRestError("Insufficient privileges to modify classes", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		Class file = null;
		
		try
		{
			if (StringUtils.hasText(sFileId))
			{	
				try
				{
					// find file by id
					file = classService.getClass(KID.get(sFileId), restInfo.getEnv());
				}
				catch (KIDException e)
				{
					returnRestError("Invalid file ID '" + sFileId + "'", restInfo.getOut());
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
			}
			else
			{
				// if new file is created, its name has to be passed as a parameter
				// TODO instead of passing the file name, extract it from the passed class code
				if (!StringUtils.hasText(qualifiedName))
				{
					returnRestError("File name is empty. When creating a new file, 'name' parameter containing the file's full qualified name has to be passed", restInfo.getOut());
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
				
				file = new Class();
				file.setIsSystem(false);
				
				if (qualifiedName.contains("."))
				{
					// extract package from qualified name
					file.setPackageName(qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));
					file.setName(qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1));
				}
				else
				{
					file.setName(qualifiedName);
				}
			}
			
			if (!StringUtils.hasText(code))
			{
				returnRestError("Code is empty.", restInfo.getOut());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			file.setKollCode(code);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			errorLogService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, restInfo.getAuthData().getUserId(), restInfo.getAuthData(), restInfo.getEnv());
			returnRestError("Error saving file: " + e.getMessage(), restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		try
		{
			// save view
			file = classService.fullSave(file, dataService, restInfo.getAuthData(), restInfo.getEnv());
			restInfo.getOut().write("{ \"success\": true, \"id\": \"" + file.getId() + "\" }");
			return;
		}
		catch (UniqueCheckViolationException e)
		{
			returnRestError("Unique check violated. Probably a class with the given name/package already exists", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		catch (ClassCompilationException e)
		{
			returnRestError(e.getMessage() != null ? "Compilation error: " + e.getMessage() : "Compilation error", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			returnRestError(e.getMessage() != null ? e.getMessage() : "Unknown error while saving class", restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_RECORD_URL, method = RequestMethod.POST)
	@ResponseBody
	public void saveRecord(@RequestParam(value = "id", required = false) String sRecordId,
						@RequestParam(value = "typeId", required = false) String sTypeId,
						@RequestParam(value = "typePrefix", required = false) String sTypePrefix,
						@RequestParam(value = "typeName", required = false) String qualifiedTypeName,
						@RequestParam(value = "record", required = false) String recordJSON,
						@RequestParam(value = "env", required = false) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpServletResponse resp, HttpSession session) throws KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		Record record = null;
		
		// inject authData to the thread
		restInfo.getEnv().addAuthData(restInfo.getAuthData());
		
		try
		{
			if (StringUtils.hasText(sRecordId))
			{
				KID recordId = null;
				
				try
				{
					recordId = KID.get(sRecordId);
				}
				catch (KIDException e)
				{
					returnRestError("Invalid record ID " + sRecordId, restInfo.getOut());
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					restInfo.getEnv().clearAuthData();
					return;
				}
				
				Type type = restInfo.getEnv().getTypeByRecordId(recordId);
				if (type == null)
				{
					returnRestError("Record ID " + sRecordId + " cannot be matched against any type", restInfo.getOut());
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					restInfo.getEnv().clearAuthData();
					return;
				}
				
				// find object by id
				List<Record> records = dataService.getRecords(Arrays.asList(KID.get(sRecordId)), type, Arrays.asList(Field.ID_FIELD_NAME), restInfo.getAuthData(), restInfo.getEnv());
				if (records.isEmpty())
				{
					returnRestError("Record with ID " + sRecordId + " does not exist or is inaccessible due to insufficient permissions", restInfo.getOut());
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					restInfo.getEnv().clearAuthData();
					return;
				}
				
				record = records.get(0);
			}
			else
			{
				Type type = null;
				
				if (StringUtils.hasText(sTypeId))
				{
					if (StringUtils.hasText(qualifiedTypeName) || StringUtils.hasText(sTypePrefix))
					{
						returnRestError("Both typeId and typeName/typePrefix cannot be specified. Only one of them should be passed", restInfo.getOut());
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						restInfo.getEnv().clearAuthData();
						return;
					}
					
					try
					{
						type = restInfo.getEnv().getType(KID.get(sTypeId));
					}
					catch (KIDException e)
					{
						returnRestError("Type ID '" + sTypeId + "' has invalid format", restInfo.getOut());
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						restInfo.getEnv().clearAuthData();
						return;
					}
					
					if (type == null)
					{
						returnRestError("Type with ID " + sTypeId + " not found", restInfo.getOut());
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						restInfo.getEnv().clearAuthData();
						return;
					}
					
					record = new Record(type);
				}
				else if (StringUtils.hasText(qualifiedTypeName))
				{
					if (StringUtils.hasText(sTypePrefix))
					{
						returnRestError("Both typeName and typePrefix cannot be specified. Only one of them should be passed", restInfo.getOut());
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						restInfo.getEnv().clearAuthData();
						return;
					}
					
					type = restInfo.getEnv().getType(qualifiedTypeName);
					
					if (type == null)
					{
						returnRestError("Type with name " + qualifiedTypeName + " not found", restInfo.getOut());
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						restInfo.getEnv().clearAuthData();
						return;
					}
					
					record = new Record(type);
				}
				else if (StringUtils.hasText(sTypePrefix))
				{
					type = restInfo.getEnv().getType(KeyPrefix.get(sTypePrefix));
					
					if (type == null)
					{
						returnRestError("Type with prefix " + sTypePrefix + " not found", restInfo.getOut());
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						restInfo.getEnv().clearAuthData();
						return;
					}
					
					record = new Record(type);
				}
				else
				{
					returnRestError("At least one of the parameters ID, typeName and typeId needs to be specified", restInfo.getOut());
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					restInfo.getEnv().clearAuthData();
					return;
				}
			}
			
			// parse record from JSON
			Record updatedRecord = null;
			
			try
			{
				updatedRecord = JSON.toRecord(recordJSON, false, record.getType(), restInfo.getEnv());
			}
			catch (KommetException e)
			{
				returnRestError("Error saving record: " + e.getMessage(), restInfo.getOut());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				restInfo.getEnv().clearAuthData();
				return;
			}
			
			// rewrite updated properties from the new record to the original one
			Map<String, Object> updatedValues = updatedRecord.getFieldValues();
			for (String field : updatedValues.keySet())
			{
				Object val = updatedValues.get(field);
				
				if (val == null)
				{
					val = SpecialValue.NULL;
				}
				
				record.setField(field, val, restInfo.getEnv());
			}
			
			try
			{
				// save record
				dataService.save(record, restInfo.getAuthData(), restInfo.getEnv());
			}
			catch (FieldValidationException e)
			{
				List<String> msgs = new ArrayList<String>();
				for (ValidationMessage msg : e.getMessages())
				{
					msgs.add(msg.getText() + "(field " + msg.getFieldLabel() + ")");
				}
				
				returnRestError("Field validation exception while saving record: " + MiscUtils.implode(msgs, ", "), restInfo.getOut());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				restInfo.getEnv().clearAuthData();
				return;
			}
			catch (KommetException e)
			{
				returnRestError("Error saving record: " + e.getMessage(), restInfo.getOut());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				restInfo.getEnv().clearAuthData();
				return;
			}
			
			// return JSON response
			restInfo.getOut().write("{ \"success\": true, \"id\": \"" + record.getKID() + "\" }");
			restInfo.getEnv().clearAuthData();
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			returnRestError(e.getMessage(), restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		finally
		{
			restInfo.getEnv().clearAuthData();
		}
		
	}

	private void returnRestRecords(List<Record> records, AuthData authData, PrintWriter out) throws KommetException
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		
		List<String> recordList = new ArrayList<String>();
		for (Record r : records)
		{
			recordList.add(JSON.serializeRecord(r, authData));
		}
		
		sb.append(MiscUtils.implode(recordList, ", "));
		sb.append(" ]");
		out.write(sb.toString());
	}
}