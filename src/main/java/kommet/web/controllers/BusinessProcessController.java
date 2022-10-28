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
import java.util.HashMap;
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
import kommet.basic.BasicSetupService;
import kommet.basic.BusinessAction;
import kommet.basic.BusinessProcess;
import kommet.basic.BusinessProcessInput;
import kommet.basic.BusinessProcessOutput;
import kommet.basic.Profile;
import kommet.basic.RecordAccessType;
import kommet.businessprocess.BusinessActionFilter;
import kommet.businessprocess.BusinessProcessFilter;
import kommet.businessprocess.BusinessProcessSaveResult;
import kommet.businessprocess.BusinessProcessService;
import kommet.businessprocess.ProcessBlock;
import kommet.businessprocess.ProcessDeserializer;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.json.AdditionalPropertySerializer;
import kommet.json.JSON;
import kommet.koll.ClassService;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class BusinessProcessController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	BusinessProcessService bpService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ClassService classService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ErrorLogService logService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/bp/builder", method = RequestMethod.GET)
	public ModelAndView showProcessBuilder(HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		ModelAndView mv = new ModelAndView("bp/builder");
		mv.addObject("serializedProcess", "{}");
		mv.addObject("availableTypes", JSON.serialize(env.getUserAccessibleTypes(), authData, getAdditionalFieldSerializer()));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/bp/actions/list", method = RequestMethod.GET)
	public ModelAndView actionList(HttpSession session) throws KommetException
	{
		return new ModelAndView("bp/actionList");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/bp/processes/list", method = RequestMethod.GET)
	public ModelAndView processList(HttpSession session) throws KommetException
	{
		return new ModelAndView("bp/processList");
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/bp/deleteprocess", method = RequestMethod.POST)
	@ResponseBody
	public void deleteApp(@RequestParam(required = false, value = "id") String sProcessId,
									HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sProcessId))
		{
			out.write(RestUtil.getRestErrorResponse("App id not specified"));
			return;
		}
		
		KID processId = null;
		
		try
		{
			processId = KID.get(sProcessId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid process ID '" + sProcessId + "'"));
			return;
		}
		
		// delete process by id
		bpService.deleteProcess(processId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		
		out.write(RestUtil.getRestSuccessResponse("Process deleted"));
		return;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/bp/processes/{id}", method = RequestMethod.GET)
	public ModelAndView processDetails(@PathVariable("id") String sProcessId, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		ModelAndView mv = new ModelAndView("bp/processDetails");
		
		BusinessProcess process = bpService.getBusinessProcess(KID.get(sProcessId), authData, env);
		mv.addObject("process", process);
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/bp/builder/{id}", method = RequestMethod.GET)
	public ModelAndView openProcessBuilder(@PathVariable("id") String sProcessId, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		ModelAndView mv = new ModelAndView("bp/builder");
		
		BusinessProcess process = bpService.getBusinessProcess(KID.get(sProcessId), authData, env);
		
		mv.addObject("serializedProcess", JSON.serializeObjectProxy(process, authData));
		mv.addObject("processId", sProcessId);
		mv.addObject("availableTypes", JSON.serialize(env.getUserAccessibleTypes(), authData, getAdditionalFieldSerializer()));
		
		return mv;
	}
	
	private AdditionalPropertySerializer getAdditionalFieldSerializer()
	{
		return new AdditionalPropertySerializer()
		{
			@Override
			public List<String> getProperties(Object o)
			{
				if (o instanceof Field)
				{
					Field field = (Field)o;
					List<String> serializedFieldProps = new ArrayList<String>();
					
					serializedFieldProps.add("\"javaType\": \"" + field.getDataType().getJavaType() + "\"");
					
					return serializedFieldProps;
				}
				
				return null;
			}
		};
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/bp/processes/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveProcess(@RequestParam(value = "serializedProcess", required = false) String serializedProcessJSON,
							HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		HashMap<String, Object> processMap = JSON.parseToMap(serializedProcessJSON);
		
		ProcessDeserializer processDeserializer = new ProcessDeserializer(bpService, authData, env);
		
		BusinessProcess process = null;
		
		try
		{
			process = processDeserializer.getProcessFromMap(processMap);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, authData.getUserId(), env);
			out.write(RestUtil.getRestErrorResponse(e.getMessage()));
			return;
		}
		
		try
		{
			BusinessProcessSaveResult saveResult = bpService.save(process, classService, dataService, authData, env);
			
			if (saveResult.isSuccess())
			{
				out.write(RestUtil.getRestSuccessDataResponse("{ \"processId\": \"" + process.getId() + "\" }"));
			}
			else
			{
				out.write(RestUtil.getRestErrorResponse(saveResult.getErrors()));
			}
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, authData.getUserId(), env);
			out.write(RestUtil.getRestErrorResponse("Error occurred while saving process"));
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, authData.getUserId(), env);
			out.write(RestUtil.getRestErrorResponse(e.getMessage()));
			return;
		}		
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/bp/builder/actions", method = RequestMethod.GET)
	@ResponseBody
	public void builderActions(HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{	
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		//basicSetupService.createFieldValueAction(authData, env);
		
		// query actions
		List<BusinessAction> allActions = bpService.get(new BusinessActionFilter(), authData, env);
		
		List<BusinessAction> actions = new ArrayList<BusinessAction>();
		
		// temporarily hide the for-each action
		for (BusinessAction a : allActions)
		{
			if (!"For Each".equals(a.getName()))
			{
				actions.add(a);
			}
		}
		
		
		BusinessProcessFilter processFilter = new BusinessProcessFilter();
		processFilter.setIsCallable(true);
		List<BusinessProcess> processes = bpService.get(processFilter, authData, env);
		
		List<String> serializedActions = new ArrayList<String>();
		List<String> serializedProcesses = new ArrayList<String>();
		
		List<ProcessBlock> callables = new ArrayList<ProcessBlock>();
		callables.addAll(actions);
		callables.addAll(processes);
		
		for (ProcessBlock callable : callables)
		{
			List<String> actionProps = new ArrayList<String>();
			actionProps.add("\"name\": \"" + callable.getName() + "\"");
			actionProps.add("\"id\": \"" + callable.getId().getId() + "\"");
			
			if (callable instanceof BusinessAction)
			{
				actionProps.add("\"callableType\": \"action\"");
				actionProps.add("\"label\": \"" + callable.getName() + "\"");
				actionProps.add("\"type\": \"" + ((BusinessAction)callable).getType() + "\"");
				actionProps.add("\"isCustom\": " + ((BusinessAction)callable).getAccessType().equals(RecordAccessType.PUBLIC.getId()) + "");
				
				// initial actions are those that trigger triggerable processes (e.g. Record Create/Update)
				actionProps.add("\"isInitial\": " + BusinessAction.isInitial(((BusinessAction)callable)) + "");
			}
			else if (callable instanceof BusinessProcess)
			{
				actionProps.add("\"callableType\": \"process\"");
				actionProps.add("\"label\": \"" + ((BusinessProcess)callable).getLabel() + "\"");
			}
			
			// add inputs
			List<String> serializedInputs = new ArrayList<String>();
			for (BusinessProcessInput input : callable.getInputs())
			{
				List<String> inputProps = new ArrayList<String>();
				inputProps.add("\"name\": \"" + input.getName() + "\"");
				inputProps.add("\"id\": \"" + input.getId() + "\"");
				inputProps.add("\"type\": \"input\"");
				inputProps.add("\"dataTypeId\": " + (input.getDataTypeId() != null ? "\"" + input.getDataTypeId() + "\"" : "null"));
				inputProps.add("\"dataTypeName\": \"" + input.getDataTypeName() + "\"");
				inputProps.add("\"dataTypeLabel\": \"" + (input.getDataTypeId() != null ? env.getType(input.getDataTypeId()).getQualifiedName() : input.getDataTypeName()) + "\"");
				
				serializedInputs.add("{ " + MiscUtils.implode(inputProps, ", ") + " }");
			}
			actionProps.add("\"inputs\": [ " + MiscUtils.implode(serializedInputs, ", ") + " ]");
			
			// add inputs
			List<String> serializedOutputs = new ArrayList<String>();
			for (BusinessProcessOutput output : callable.getOutputs())
			{
				List<String> outputProps = new ArrayList<String>();
				outputProps.add("\"name\": \"" + output.getName() + "\"");
				outputProps.add("\"id\": \"" + output.getId() + "\"");
				outputProps.add("\"type\": \"output\"");
				outputProps.add("\"dataTypeId\": " + (output.getDataTypeId() != null ? "\"" + output.getDataTypeId() + "\"" : "null"));
				outputProps.add("\"dataTypeName\": \"" + output.getDataTypeName() + "\"");
				outputProps.add("\"dataTypeLabel\": \"" + (output.getDataTypeId() != null ? env.getType(output.getDataTypeId()).getQualifiedName() : output.getDataTypeName()) + "\"");
				
				serializedOutputs.add("{ " + MiscUtils.implode(outputProps, ", ") + " }");
			}
			actionProps.add("\"outputs\": [ " + MiscUtils.implode(serializedOutputs, ", ") + " ]");
			
			if (callable instanceof BusinessAction)
			{
				serializedActions.add("{ " + MiscUtils.implode(actionProps, ", ") + " }");
			}
			else if (callable instanceof BusinessProcess)
			{
				serializedProcesses.add("{ " + MiscUtils.implode(actionProps, ", ") + " }");
			}
		}
		
		out.write(RestUtil.getRestSuccessDataResponse("{ \"types\": " + JSON.serialize(dataService.getTypes(null, false, false, env), authData) + ", \"actions\": [ " + MiscUtils.implode(serializedActions, ", ") + "], \"processes\": [ " + MiscUtils.implode(serializedProcesses, ", ") + " ] }"));
	}
}