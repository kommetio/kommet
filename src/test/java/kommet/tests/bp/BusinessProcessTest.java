/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.bp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.BusinessAction;
import kommet.basic.BusinessActionInvocation;
import kommet.basic.BusinessActionTransition;
import kommet.basic.BusinessProcess;
import kommet.basic.BusinessProcessInput;
import kommet.basic.BusinessProcessOutput;
import kommet.basic.BusinessProcessParamAssignment;
import kommet.basic.Class;
import kommet.basic.Email;
import kommet.basic.Profile;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyUtil;
import kommet.basic.Task;
import kommet.basic.User;
import kommet.businessprocess.BusinessActionFilter;
import kommet.businessprocess.BusinessProcessDeclarationException;
import kommet.businessprocess.BusinessProcessException;
import kommet.businessprocess.BusinessProcessExecutor;
import kommet.businessprocess.BusinessProcessSaveResult;
import kommet.businessprocess.BusinessProcessService;
import kommet.businessprocess.ProcessResult;
import kommet.businessprocess.annotations.Execute;
import kommet.businessprocess.annotations.Input;
import kommet.businessprocess.annotations.Output;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class BusinessProcessTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	ClassService classService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	DataService dataService;
	
	@Inject
	BusinessProcessService bpService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ErrorLogService logService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UserService userService;
	
	@Inject
	EnvService envService;
	
	/*private void testRELConditions(EnvData env) throws KommetException
	{
		List<BusinessActionInvocation> missingInvocations = new ArrayList<BusinessActionInvocation>();
		
		Record user = new Record(env.getType(KeyPrefix.get(KID.USER_PREFIX)));
		user.setField("userName", "radek");
		
		Map<String, Map<String, Object>> recordsByInvocationName = new HashMap<String, Map<String, Object>>();
		
		Map<String, Object> someActionResults = new HashMap<String, Object>();
		someActionResults.put("user", user);
		
		recordsByInvocationName.put("some action", someActionResults);
		
		boolean ifCond = BusinessProcessExecutor.evaluateREL("{some action}.user.userName <> 'radek'", missingInvocations, recordsByInvocationName);
		assertTrue(ifCond);
		
		user.setField("userName", "john");
		ifCond = BusinessProcessExecutor.evaluateREL("{some action}.user.userName <> 'radek'", missingInvocations, recordsByInvocationName);
		assertFalse(ifCond);
	}*/
	
	private void testFieldValueAction(AuthData authData, EnvData env) throws KommetException
	{
		BusinessProcess process = new BusinessProcess();
		
		// set the fields necessary for saving the process as draft
		process.setName("com.bp.FieldValueProcess");
		process.setLabel("Test Field Value");
		process.setIsTriggerable(true);
		process.setIsCallable(false);
		process.setIsActive(true);
		process.setIsDraft(false);
		
		// create an entry point action that will trigger the process
		BusinessAction recordCreate = bpService.getRecordCreateAction(env);
				
		BusinessActionInvocation recordCreateCall = process.addAction(recordCreate, "On pigeon created");
		
		// add input to process
		process.addInput("newRecord", "Updated Record", RecordProxy.class.getName(), "record", recordCreateCall);
		
		Type pigeonType = env.getType(TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME);
		assertNotNull(pigeonType);
		
		BusinessAction fieldValueAction = bpService.getFieldValueAction(env);
		
		BusinessActionInvocation getAgeCall = process.addAction(fieldValueAction, "Get pigeon age");
		getAgeCall.setAttribute(BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT, "age");
		
		process.assignParam(recordCreateCall, recordCreate.getOutput("record"), getAgeCall, fieldValueAction.getInput("record"));
		process.addTransition(recordCreateCall, getAgeCall);
		
		// test process
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Grzes");
		pigeon1.setField("age", 1);
		pigeon1 = dataService.save(pigeon1, env);
		
		// query pigeon
		BusinessAction queryUniqueAction = bpService.getAction("QueryUnique", authData, env);
		BusinessActionInvocation queryPigeonCall = process.addAction(queryUniqueAction, "Query Pigeon");
		queryPigeonCall.setAttribute("query", "select id, name, age from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + pigeon1.getKID() + "'");
		
		// read name of the queried pigeon
		BusinessActionInvocation fieldNameCall = process.addAction(fieldValueAction, "Get pigeon name");
		fieldNameCall.setAttribute(BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT, "name");
		
		process.assignParam(queryPigeonCall, queryUniqueAction.getOutput("record"), fieldNameCall, fieldValueAction.getInput("record"));
		process.addTransition(queryPigeonCall, fieldNameCall);
		
		createClonePigeonAction(pigeonType, authData, env);
		
		BusinessAction clonePigeonAction = bpService.getAction("Clone Pigeon", authData, env);
		assertNotNull(clonePigeonAction);
		
		BusinessActionInvocation clonePigeonCall = process.addAction(clonePigeonAction, "Create another pigeon");
		
		// assign pigeon age to create pigeon action
		process.assignParam(getAgeCall, fieldValueAction.getOutput(BusinessAction.FIELD_VALUE_ACTION_OUTPUT), clonePigeonCall, clonePigeonAction.getInput("age"));
		
		// assign pigeon name to create pigeon action
		process.assignParam(fieldNameCall, fieldValueAction.getOutput(BusinessAction.FIELD_VALUE_ACTION_OUTPUT), clonePigeonCall, clonePigeonAction.getInput("name"));
		
		process.addTransition(getAgeCall, clonePigeonCall);
		process.addTransition(fieldNameCall, clonePigeonCall);
		
		BusinessProcessSaveResult result = bpService.save(process, classService, dataService, authData, env);
		assertFalse(result.isSuccess());
		//assertProcessError("Invalid parameter assignment from {Query Pigeon}.record (kommet.basic.RecordProxy) to {Get pigeon name}.record (kommet.basic.RecordProxy)", result, false);
		assertProcessError("Invalid parameter assignment from {Get pigeon age}.value (java.lang.Object) to {Create another pigeon}.age (java.lang.Integer)", result, false);
		assertProcessError("Output of invocation On pigeon created is a record proxy, so field age cannot be read from it", result, false);
		assertProcessError("Invalid parameter assignment from {Get pigeon age}.value (java.lang.Object) to {Create another pigeon}.age (java.lang.Integer)", result, false);
		assertEquals("Expected 3 error, but got " + result.getErrors().size() + ":\n" + MiscUtils.implode(result.getErrors(), "\n"), 3, result.getErrors().size());
		
		// add accepted types to input invocation
		recordCreateCall = process.getInvocation(recordCreateCall.getName());
		recordCreateCall.setAttribute("acceptedTypes", pigeonType.getKID().getId());
		
		BusinessActionInvocation ownerCall = process.addAction(queryUniqueAction, "Get pigeon createdBy");
		ownerCall.setAttribute("query", "select id from User limit 1");
		process.assignParam(ownerCall, queryUniqueAction.getOutput("record"), clonePigeonCall, clonePigeonAction.getInput("owner"));
		
		result = bpService.save(process, classService, dataService, authData, env);
		assertTrue(MiscUtils.implode(result.getErrors(), "\n"), result.isSuccess());
		
		Integer initialCount = env.getSelectCriteriaFromDAL("select id, name, age from " + pigeonType.getQualifiedName()).list().size();
		
		// now create another pigeon and make sure it's cloned
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Zen");
		pigeon2.setField("age", 132);
		pigeon2 = dataService.save(pigeon2, env);
		
		// find the cloned pigeon
		assertEquals(initialCount + 2, env.getSelectCriteriaFromDAL("select id, name, age from " + pigeonType.getQualifiedName()).list().size());
		List<Record> clonedPigeons = env.getSelectCriteriaFromDAL("select id, name, age from " + pigeonType.getQualifiedName() + " where name = 'Grzes - clone'").list();
		assertEquals(1, clonedPigeons.size());
		assertEquals(133, clonedPigeons.get(0).getField("age"));
		
		// deactivate process
		process.setIsActive(false);
		bpService.save(process, classService, dataService, authData, env);
	}
	
	private void testFieldValueActionWithUserAssignment(AuthData authData, EnvData env) throws KommetException
	{
		BusinessProcess process = new BusinessProcess();
		
		// set the fields necessary for saving the process as draft
		process.setName("com.bp.FieldValueProcessWithOwner");
		process.setLabel("Test Field Value");
		process.setIsTriggerable(true);
		process.setIsCallable(false);
		process.setIsActive(true);
		process.setIsDraft(false);
		
		// create an entry point action that will trigger the process
		BusinessAction recordCreate = bpService.getRecordCreateAction(env);
				
		BusinessActionInvocation recordCreateCall = process.addAction(recordCreate, "On pigeon created");
		
		// add input to process
		process.addInput("newRecord", "Updated Record", RecordProxy.class.getName(), "record", recordCreateCall);
		
		Type pigeonType = env.getType(TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME);
		assertNotNull(pigeonType);
		
		BusinessAction fieldValueAction = bpService.getFieldValueAction(env);
		
		BusinessActionInvocation getAgeCall = process.addAction(fieldValueAction, "Get pigeon age");
		getAgeCall.setAttribute(BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT, "age");
		
		process.assignParam(recordCreateCall, recordCreate.getOutput("record"), getAgeCall, fieldValueAction.getInput("record"));
		process.addTransition(recordCreateCall, getAgeCall);
		
		// test process
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Saul");
		pigeon1.setField("age", 1);
		pigeon1 = dataService.save(pigeon1, env);
		
		// query pigeon
		BusinessAction queryUniqueAction = bpService.getAction("QueryUnique", authData, env);
		BusinessActionInvocation queryPigeonCall = process.addAction(queryUniqueAction, "Query Pigeon");
		queryPigeonCall.setAttribute("query", "select id, name, age from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + pigeon1.getKID() + "'");
		
		// read name of the queried pigeon
		BusinessActionInvocation fieldNameCall = process.addAction(fieldValueAction, "Get pigeon name");
		fieldNameCall.setAttribute(BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT, "name");
		
		process.assignParam(queryPigeonCall, queryUniqueAction.getOutput("record"), fieldNameCall, fieldValueAction.getInput("record"));
		process.addTransition(queryPigeonCall, fieldNameCall);
		
		// read name of the queried pigeon
		BusinessActionInvocation ownerCall = process.addAction(fieldValueAction, "Get pigeon createdBy");
		ownerCall.setAttribute(BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT, "createdBy");
		
		process.assignParam(queryPigeonCall, queryUniqueAction.getOutput("record"), fieldNameCall, fieldValueAction.getInput(BusinessAction.FIELD_VALUE_ACTION_RECORD_INPUT));
		
		BusinessAction clonePigeonAction = bpService.getAction("Clone Pigeon", authData, env);
		assertNotNull(clonePigeonAction);
		
		BusinessActionInvocation clonePigeonCall = process.addAction(clonePigeonAction, "Create another pigeon");
		
		// assign pigeon age to create pigeon action
		process.assignParam(getAgeCall, fieldValueAction.getOutput(BusinessAction.FIELD_VALUE_ACTION_OUTPUT), clonePigeonCall, clonePigeonAction.getInput("age"));
		
		// assign pigeon name to create pigeon action
		process.assignParam(fieldNameCall, fieldValueAction.getOutput(BusinessAction.FIELD_VALUE_ACTION_OUTPUT), clonePigeonCall, clonePigeonAction.getInput("name"));
		
		process.assignParam(ownerCall, fieldValueAction.getOutput(BusinessAction.FIELD_VALUE_ACTION_OUTPUT), clonePigeonCall, clonePigeonAction.getInput("owner"));
		
		process.addTransition(getAgeCall, clonePigeonCall);
		process.addTransition(fieldNameCall, clonePigeonCall);
		
		BusinessProcessSaveResult result = bpService.save(process, classService, dataService, authData, env);
		assertFalse(result.isSuccess());
		//assertProcessError("Invalid parameter assignment from {Query Pigeon}.record (kommet.basic.RecordProxy) to {Get pigeon name}.record (kommet.basic.RecordProxy)", result, false);
		assertProcessError("Output of invocation On pigeon created is a record proxy, so field age cannot be read from it", result, false);
		assertProcessError("Cannot obtain returned value from FieldValue invocation Get pigeon age. Probably no record is assigned as input to the invocation", result, false);
		assertProcessError("Cannot obtain returned value from FieldValue invocation Get pigeon createdBy. Probably no record is assigned as input to the invocation", result, false);
		assertProcessError("Invalid parameter assignment from {Get pigeon age}.value (java.lang.Object) to {Create another pigeon}.age (java.lang.Integer)", result, false);
		assertProcessError("Output of invocation On pigeon created is a record proxy, so field age cannot be read from it", result, false);
		assertProcessError("Invalid parameter assignment from {Get pigeon age}.value (java.lang.Object) to {Create another pigeon}.age (java.lang.Integer)", result, false);
		assertEquals("Expected 6 errors, but got " + result.getErrors().size() + ":\n" + MiscUtils.implode(result.getErrors(), "\n"), 6, result.getErrors().size());
		
		// add accepted types to input invocation
		recordCreateCall = process.getInvocation(recordCreateCall.getName());
		recordCreateCall.setAttribute("acceptedTypes", pigeonType.getKID().getId());
		
		// assign process input to the owner FieldValue call
		ownerCall = process.getInvocation(ownerCall.getName());
		process.assignParam(recordCreateCall, recordCreate.getOutput("record"), ownerCall, fieldValueAction.getInput(BusinessAction.FIELD_VALUE_ACTION_RECORD_INPUT));
		
		result = bpService.save(process, classService, dataService, authData, env);
		
		// the error below is expected, because {get pigeon createdby} is a starting action, so it should not take values from any other action
		// to fix this, we will add a transition from the entry point to it, thus making it not a starting action
		assertProcessError("Input parameter record of the starting action Get pigeon createdBy has no assigned input value from the process input, nor is the input defined", result, true);
		
		// add transition so that {Get pigeon createdBy} is no longer a starting action
		process.addTransition(process.getInvocation(recordCreateCall.getName()), process.getInvocation(ownerCall.getName()));
		result = bpService.save(process, classService, dataService, authData, env);
		
		assertTrue(MiscUtils.implode(result.getErrors(), "\n"), result.isSuccess());
		
		Integer initialCount = env.getSelectCriteriaFromDAL("select id, name, age from " + pigeonType.getQualifiedName()).list().size();
		
		// now create another pigeon and make sure it's cloned
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Zen");
		pigeon2.setField("age", 132);
		pigeon2 = dataService.save(pigeon2, env);
		
		// check that a pigeon has been created
		assertEquals(initialCount + 2, env.getSelectCriteriaFromDAL("select id, name, age from " + pigeonType.getQualifiedName()).list().size());
		
		// find the cloned pigeon
		List<Record> clonedPigeons = env.getSelectCriteriaFromDAL("select id, name, age from " + pigeonType.getQualifiedName() + " where name = 'Saul - clone'").list();
		assertEquals(1, clonedPigeons.size());
		assertEquals(133, clonedPigeons.get(0).getField("age"));
	}
	
	@Test
	public void testCreateBusinessProcess() throws KommetException, ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		AuthData authData = dataHelper.getRootAuthData(env);
		
		//testRELConditions(env);
		
		declareTestBusinessActions(authData, env);
		
		// create a new business process
		BusinessProcess taskCreationProcess = new BusinessProcess();
		
		// set the fields necessary for saving the process as draft
		taskCreationProcess.setName("com.bp.TaskCreationProcess");
		taskCreationProcess.setLabel("Create Task");
		taskCreationProcess.setIsTriggerable(true);
		taskCreationProcess.setIsCallable(false);
		
		BusinessProcessSaveResult saveResult = bpService.save(taskCreationProcess, classService, dataService, authData, env);
		assertFalse("Saving process should fail because it is not in draft mode and it does not contain any actions", saveResult.isSuccess());
		assertProcessError("Business process is draft field is empty", saveResult, false);
		
		taskCreationProcess.setIsDraft(true);
		
		saveResult = bpService.save(taskCreationProcess, classService, dataService, authData, env);
		assertFalse("Saving process should fail because it is in draft mode and active at the same time", saveResult.isSuccess());
		assertProcessError("Business process is active field is empty", saveResult, false);
		assertNotProcessError("Business process is draft field is empty", saveResult, false);
		
		taskCreationProcess.setIsActive(false);
		
		// now saving the draft should succeed
		saveResult = bpService.save(taskCreationProcess, classService, dataService, authData, env);
		taskCreationProcess = saveResult.getProcess();
		assertTrue(saveResult.isSuccess());
		assertNotNull(taskCreationProcess.getId());
		taskCreationProcess = bpService.getBusinessProcess(taskCreationProcess.getId(), authData, env);
		assertNotNull(taskCreationProcess);
		assertNull(taskCreationProcess.getCompiledClass());
		
		taskCreationProcess.setIsDraft(true);
		taskCreationProcess.setIsActive(true);
		
		saveResult = bpService.save(taskCreationProcess, classService, dataService, authData, env);
		assertFalse("It should not be possible to set business processes as active that are in draft mode", saveResult.isSuccess());
		assertProcessError("Business processes in draft state cannot be set as active", saveResult, true);
		
		testDefineFullProcess(taskCreationProcess, authData, env);
		testEmbedProcessInProcess(authData, env);
		testFieldValueAction(authData, env);
		testFieldValueActionWithUserAssignment(authData, env);
		testTwoProcessesOnSameRecord(authData, env);
	}
	
	/**
	 * This method tests execution of two business processes on the same record, both of which modify in some way.
	 * The purpose of this test is to make sure that the output of the first process is passed as input to the second one, so that changes
	 * made by both take effect.
	 * @param authData
	 * @param env
	 * @throws KommetException 
	 */
	private void testTwoProcessesOnSameRecord(AuthData authData, EnvData env) throws KommetException
	{
		BusinessProcess setAgeProcess = new BusinessProcess();
		
		// set the fields necessary for saving the process as draft
		setAgeProcess.setName("com.bp.SetAgeProcess");
		setAgeProcess.setLabel("Process that sets age");
		setAgeProcess.setIsTriggerable(true);
		setAgeProcess.setIsCallable(false);
		setAgeProcess.setIsActive(true);
		setAgeProcess.setIsDraft(false);
		
		// create an entry point action that will trigger the process
		BusinessAction recordCreate = bpService.getRecordCreateAction(env);
				
		BusinessActionInvocation recordCreateCall = setAgeProcess.addAction(recordCreate, "On pigeon created");
		
		// add input to process
		setAgeProcess.addInput("newRecord", "Updated Record", RecordProxy.class.getName(), "record", recordCreateCall);
		
		Type pigeonType = env.getType(TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME);
		assertNotNull(pigeonType);
		
		BusinessAction fieldUpdateAction = bpService.getFieldUpdateAction(env);
		
		BusinessActionInvocation getAgeCall = setAgeProcess.addAction(fieldUpdateAction, "Set pigeon age");
		getAgeCall.setAttribute("age", "100");
		
		setAgeProcess.assignParam(recordCreateCall, recordCreate.getOutput("record"), getAgeCall, fieldUpdateAction.getInput("record"));
		setAgeProcess.addTransition(recordCreateCall, getAgeCall);
		
		BusinessProcessSaveResult result = bpService.save(setAgeProcess, classService, dataService, authData, env);
		assertTrue(result.isSuccess());
		
		// create a new pigeon
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Grzegorz");
		pigeon1.setField("age", 1);
		pigeon1 = dataService.save(pigeon1, env);
		
		pigeon1 = env.getSelectCriteriaFromDAL("select id, age from " + pigeonType.getQualifiedName() + " where id = '" + pigeon1.getKID() + "'").singleRecord();
		assertEquals(100, pigeon1.getField("age"));
		
		// now create another process that sets the pigeon name
		BusinessProcess setNameProcess = new BusinessProcess();
		
		// set the fields necessary for saving the process as draft
		setNameProcess.setName("com.bp.SetNameProcess");
		setNameProcess.setLabel("Process that sets name");
		setNameProcess.setIsTriggerable(true);
		setNameProcess.setIsCallable(false);
		setNameProcess.setIsActive(true);
		setNameProcess.setIsDraft(false);
		
		// create an entry point action that will trigger the process
		BusinessAction recordCreate1 = bpService.getRecordCreateAction(env);
				
		BusinessActionInvocation recordCreateCall1 = setNameProcess.addAction(recordCreate1, "On pigeon created");
		
		// add input to process
		setNameProcess.addInput("newRecord", "Updated Record", RecordProxy.class.getName(), "record", recordCreateCall1);
		
		BusinessActionInvocation getNameCall = setNameProcess.addAction(fieldUpdateAction, "Set pigeon name");
		getNameCall.setAttribute("name", "Pete");
		
		setNameProcess.assignParam(recordCreateCall1, recordCreate1.getOutput("record"), getNameCall, fieldUpdateAction.getInput("record"));
		setNameProcess.addTransition(recordCreateCall1, getNameCall);
		
		result = bpService.save(setNameProcess, classService, dataService, authData, env);
		assertTrue(result.isSuccess());
		
		// create a new pigeon
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Grzegorz");
		pigeon2.setField("age", 1);
		pigeon2 = dataService.save(pigeon2, env);
		
		pigeon2 = env.getSelectCriteriaFromDAL("select id, age, name from " + pigeonType.getQualifiedName() + " where id = '" + pigeon2.getKID() + "'").singleRecord();
		assertEquals(100, pigeon2.getField("age"));
		assertEquals("Pete", pigeon2.getField("name"));
	}

	/**
	 * Create a process and embed it in another process
	 * @param authData
	 * @param env
	 * @throws KommetException 
	 */
	private void testEmbedProcessInProcess(AuthData authData, EnvData env) throws KommetException
	{
		Type projectType = dataHelper.createProjectType(env);
		BusinessProcess projectActivateProcess = createProjectActivateProcess(projectType, authData, env);
		
		BusinessProcess process = new BusinessProcess();
		process.setName("com.bp.CreateTask");
		process.setDescription("Create Task");
		process.setLabel("Create Task");
		process.setIsCallable(true);
		process.setIsTriggerable(true);
		process.setIsActive(true);
		process.setIsDraft(false);
		
		BusinessActionInvocation projectCreatedInv = process.addAction(bpService.getRecordCreateAction(env), "Project created");
		projectCreatedInv.setAttribute("acceptedTypes", projectType.getKID().getId());
		
		// add process input
		process.addInput("newProject", "New project", RecordProxy.class.getName(), "record", projectCreatedInv);
		
		// call field update process
		BusinessActionInvocation activateProjectInv = process.addAction(projectActivateProcess, "Activate project");
		
		// add parameter assignment to project activation
		process.assignParam(projectCreatedInv, projectCreatedInv.getInvokedAction().getOutput("record"), activateProjectInv, activateProjectInv.getInvokedProcess().getInput("newProject"));
		
		// add transition
		process.addTransition(projectCreatedInv, activateProjectInv);
		
		BusinessProcessSaveResult saveResult = bpService.save(process, classService, dataService, authData, env);
		assertTrue(saveResult.isSuccess());
		
		// now create a project and make sure the process is run
		Record project1 = new Record(projectType);
		project1.setField("name", "First project");
		dataService.save(project1, env);
		
		project1 = env.getSelectCriteriaFromDAL("select id, isActive from " + projectType.getQualifiedName() + " where id = '" + project1.getKID() + "'").singleRecord();
		assertEquals(Boolean.TRUE, project1.getField("isActive"));
		
		// now update the embedded process, but do not change its inputs or outputs
		projectActivateProcess.setName("com.test.NewName");
		projectActivateProcess.setIsTriggerable(false);
		BusinessProcessSaveResult subProcessSaveResult = bpService.save(projectActivateProcess, classService, dataService, authData, env);
		assertTrue("Unexpected error while saving process: " + MiscUtils.implode(subProcessSaveResult.getErrors(), "\n"), subProcessSaveResult.isSuccess());
		
		// now create a project and make sure the process is run
		Record project2 = new Record(projectType);
		project2.setField("name", "Second project");
		dataService.save(project2, env);
		
		project2 = env.getSelectCriteriaFromDAL("select id, isActive from " + projectType.getQualifiedName() + " where id = '" + project2.getKID() + "'").singleRecord();
		assertEquals(Boolean.TRUE, project2.getField("isActive"));
		
		// now try to remove an input from the embedded process and make sure it is not allowed
		projectActivateProcess.removeInput("newProject");
		subProcessSaveResult = bpService.save(projectActivateProcess, classService, dataService, authData, env);
		assertFalse(subProcessSaveResult.isSuccess());
		assertEquals(1, subProcessSaveResult.getErrors().size());
		assertEquals("Starting action has input values, but the process does not", subProcessSaveResult.getErrors().get(0));
		
		// add a different input to the process
		projectActivateProcess.addInput("newInputParam", "New param", projectType.getKID(), "record", projectActivateProcess.getInvocation("Project created"));
		subProcessSaveResult = bpService.save(projectActivateProcess, classService, dataService, authData, env);
		assertFalse(subProcessSaveResult.isSuccess());
		assertEquals(1, subProcessSaveResult.getErrors().size());
		assertEquals("New input newInputParam added to used process", subProcessSaveResult.getErrors().get(0));
		
		projectActivateProcess = bpService.getBusinessProcess(projectActivateProcess.getId(), true, authData, env);
		
		// update param data type
		projectActivateProcess.getInput("newProject").setDataTypeId(env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID());
		projectActivateProcess.getInput("newProject").setDataTypeName(null);
		subProcessSaveResult = bpService.save(projectActivateProcess, classService, dataService, authData, env);
		
		assertFalse(subProcessSaveResult.isSuccess());
		assertEquals(1, subProcessSaveResult.getErrors().size());
		assertTrue(subProcessSaveResult.getErrors().get(0).startsWith("Data type changed on input newProject"));
		assertTrue(subProcessSaveResult.getErrors().get(0).endsWith("in used process"));
		
		// now create a project and make sure the process is run
		Record project3 = new Record(projectType);
		project3.setField("name", "Third project");
		dataService.save(project3, env);
		
		project3 = env.getSelectCriteriaFromDAL("select id, isActive from " + projectType.getQualifiedName() + " where id = '" + project3.getKID() + "'").singleRecord();
		assertEquals(Boolean.TRUE, project3.getField("isActive"));
		
		// refetch the process because the Activate Project subprocess has been modified, and we don't want these changes to be applied to the instance that we used later
		process = bpService.getBusinessProcess(process.getId(), true, authData, env);
		
		testIfConditionWithOneBranch(process, projectType, authData, env);
	}

	/**
	 * Test a specific situation when there is no transition attached to the "false" branch of an if-condition, and the condition is not met so action should be executed.
	 * @param process
	 * @param dataService2 
	 * @param classService2 
	 * @param authData
	 * @param env
	 * @throws KommetException 
	 */
	private void testIfConditionWithOneBranch(BusinessProcess process, Type projectType, AuthData authData, EnvData env) throws KommetException
	{
		BusinessActionInvocation ifCondition = process.addAction(bpService.getIfAction(env), "Check condition");
		ifCondition.setAttribute("condition", "{Project created}.record.name == 'incorrect name'");
		
		process.addTransition(process.getInvocation("Activate project"), ifCondition);
		
		BusinessActionInvocation fieldUpdateInv1 = process.addAction(bpService.getFieldUpdateAction(env), "Update field status to Open");
		fieldUpdateInv1.setAttribute("status", "Open");
		process.assignParam(process.getInvocation("Project created"), process.getInvocation("Project created").getInvokedAction().getOutput("record"), fieldUpdateInv1, fieldUpdateInv1.getInvokedAction().getInput("record"));
		
		BusinessActionInvocation fieldUpdateInv2 = process.addAction(bpService.getFieldUpdateAction(env), "Update field status to Closed");
		fieldUpdateInv2.setAttribute("status", "Closed");
		process.assignParam(process.getInvocation("Project created"), process.getInvocation("Project created").getInvokedAction().getOutput("record"), fieldUpdateInv2, fieldUpdateInv2.getInvokedAction().getInput("record"));
		
		process.addInvocationAttribute(ifCondition.getName(), "ifTrueInvocationName", fieldUpdateInv1.getName());
		process.addInvocationAttribute(ifCondition.getName(), "ifFalseInvocationName", fieldUpdateInv2.getName());
		process.addTransition(ifCondition, fieldUpdateInv1);
		process.addTransition(ifCondition, fieldUpdateInv2);
		
		BusinessProcessSaveResult saveResult = bpService.save(process, classService, dataService, authData, env);
		assertTrue("Save errors: " + MiscUtils.implode(saveResult.getErrors(), ", "), saveResult.isSuccess());
		
		// create a project with a name that does not fulfill the criteria
		Record project1 = new Record(projectType);
		project1.setField("name", "Some name");
		dataService.save(project1, env);
		
		project1 = env.getSelectCriteriaFromDAL("select id, status from " + projectType.getQualifiedName() + " where id = '" + project1.getKID() + "'").singleRecord();
		assertEquals("Closed", project1.getField("status"));
		
		Record project2 = new Record(projectType);
		project2.setField("name", "incorrect name");
		dataService.save(project2, env);
		
		project2 = env.getSelectCriteriaFromDAL("select id, status from " + projectType.getQualifiedName() + " where id = '" + project2.getKID() + "'").singleRecord();
		assertEquals("Open", project2.getField("status"));
		
		// remove the false branch from the process
		process = bpService.getBusinessProcess(process.getId(), true, authData, env);
		process.getInvocation(ifCondition.getName()).removeAttribute("ifFalseInvocationName"); 
		process.removeTransition(ifCondition, fieldUpdateInv2);
		
		saveResult = bpService.save(process, classService, dataService, authData, env);
		assertFalse(saveResult.isSuccess());
		assertEquals("Input parameter record of the starting action Update field status to Closed has no assigned input value from the process input, nor is the input defined in invocation attributes", saveResult.getErrors().get(0));
		
		process.removeInvocation(fieldUpdateInv2.getName());
		saveResult = bpService.save(process, classService, dataService, authData, env);
		assertTrue(saveResult.isSuccess());
		
		// create a project with a name that does not fulfill the criteria
		Record project3 = new Record(projectType);
		project3.setField("name", "Some name 2");
		dataService.save(project3, env);
		
		project3 = env.getSelectCriteriaFromDAL("select id, status from " + projectType.getQualifiedName() + " where id = '" + project3.getKID() + "'").singleRecord();
		assertNull(project3.attemptGetField("status"));
	}

	private BusinessProcess createProjectActivateProcess(Type projectType, AuthData authData, EnvData env) throws KommetException
	{
		BusinessProcess process = new BusinessProcess();
		process.setName("com.bp.ActivateProject");
		process.setDescription("Some process");
		process.setLabel("Activate Project");
		process.setIsCallable(true);
		process.setIsTriggerable(true);
		process.setIsActive(true);
		process.setIsDraft(false);
		
		BusinessActionInvocation projectCreatedInv = process.addAction(bpService.getRecordCreateAction(env), "Project created");
		projectCreatedInv.setAttribute("acceptedTypes", projectType.getKID().getId());
		
		// add process input
		process.addInput("newProject", "New project", RecordProxy.class.getName(), "record", projectCreatedInv);
		
		// call field update
		BusinessActionInvocation fieldUpdateInv = process.addAction(bpService.getFieldUpdateAction(env), "Update field isActive");
		fieldUpdateInv.setAttribute("isActive", "true");
		
		// add parameter assignment to field update
		process.assignParam(projectCreatedInv, projectCreatedInv.getInvokedAction().getOutput("record"), fieldUpdateInv, fieldUpdateInv.getInvokedAction().getInput("record"));
		
		// add transition
		process.addTransition(projectCreatedInv, fieldUpdateInv);
		
		BusinessProcessSaveResult saveResult = bpService.save(process, classService, dataService, authData, env);
		assertTrue("Unexpected save errors: " + MiscUtils.implode(saveResult.getErrors(), "\n"), saveResult.isSuccess());
		
		return bpService.getBusinessProcess(saveResult.getProcess().getId(), true, authData, env);
	}

	private void assertProcessError(String err, BusinessProcessSaveResult saveResult, boolean startsWith)
	{
		if (saveResult.isSuccess())
		{
			fail("Process does not contain error: " + err);
		}
		
		boolean errFound = false;
		
		for (String error : saveResult.getErrors())
		{
			if ((startsWith && error.startsWith(err)) || error.equals(err))
			{
				errFound = true;
				break;
			}
		}
		
		if (!errFound)
		{
			fail("Process does not contain error " + (startsWith ? "starting with" : "") + ": " + err);
		}
	}
	
	private void assertNotProcessError(String err, BusinessProcessSaveResult saveResult, boolean startsWith)
	{
		if (saveResult.isSuccess())
		{
			fail("Process does not contain error: " + err);
		}
		
		boolean errFound = false;
		
		for (String error : saveResult.getErrors())
		{
			if ((startsWith && error.startsWith(err)) || error.equals(err))
			{
				errFound = true;
			}
		}
		
		if (errFound)
		{
			fail("Process contains unexpected error " + (startsWith ? "starting with" : "") + ": " + err);
		}
	}

	@SuppressWarnings({ "unchecked" })
	private void testDefineFullProcess(BusinessProcess taskCreationProcess, AuthData authData, EnvData env) throws KommetException, ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		// add owner field
		Field ownerField = new Field();
		ownerField.setDataType(new TypeReference(env.getType(KeyPrefix.get(KID.USER_PREFIX))));
		ownerField.setApiName("owner");
		ownerField.setLabel("Owner");
		ownerField.setRequired(false);
		pigeonType.addField(ownerField);
		ownerField = dataService.createField(ownerField, env);
		
		// create an entry point action that will trigger the process
		BusinessAction recordCreate = bpService.getRecordCreateAction(env);
		
		BusinessActionInvocation recordCreateCall = taskCreationProcess.addAction(recordCreate, "On pigeon created");
		assertEquals("On pigeon created", recordCreateCall.getName());
		
		// get another action
		BusinessAction createTaskAction = bpService.getAction("Create Task For Pigeon", authData, env);
		bpService.get(new BusinessActionFilter(), authData, env);
		assertNotNull(createTaskAction);
		
		// add action to process
		BusinessActionInvocation createTaskCall = taskCreationProcess.addAction(createTaskAction, "Create task");
		assertEquals("Create task", createTaskCall.getName());
		
		// assign action's output to another action's input
		taskCreationProcess.assignParam(recordCreateCall, recordCreate.getOutput("record"), createTaskCall, createTaskAction.getInput("record"));
		
		// get query unique result action
		BusinessAction queryUniqueAction = bpService.getAction("QueryUnique", authData, env);
		assertNotNull(queryUniqueAction);
		
		// add action to process
		BusinessActionInvocation queryUniqueCall = taskCreationProcess.addAction(queryUniqueAction, "Query root user");
		assertEquals("Query root user", queryUniqueCall.getName());
		
		taskCreationProcess.assignParam(queryUniqueCall, recordCreate.getOutput("record"), createTaskCall, createTaskAction.getInput("user"));
		
		// now save the process
		taskCreationProcess.setIsActive(true);
		taskCreationProcess.setIsDraft(false); 
		
		BusinessProcessSaveResult saveResult = bpService.save(taskCreationProcess, classService, dataService, authData, env);
		assertFalse("Saving process should fail because it has starting nodes without param assignments", saveResult.isSuccess());
		assertProcessError("Starting action has input values, but the process does not", saveResult, false);
		
		// add input to the process
		taskCreationProcess.addInput("newRecord", "Updated Record", RecordProxy.class.getName(), "record", recordCreateCall);
		taskCreationProcess.addInput("query", "User query", String.class.getName(), "query", queryUniqueCall);
		taskCreationProcess.addOutput("createdTask", "The inserted task", env.getType(KeyPrefix.get(KID.TASK_PREFIX)).getKID(), "task", createTaskCall);
		
		assertNotNull(taskCreationProcess.getInputs());
		assertFalse(taskCreationProcess.getInputs().isEmpty());
		assertNotNull(taskCreationProcess.getOutputs());
		assertFalse(taskCreationProcess.getOutputs().isEmpty());
		
		// add transition between the invocations
		taskCreationProcess.addTransition(recordCreateCall, createTaskCall);
		taskCreationProcess.addTransition(queryUniqueCall, createTaskCall);
		
		saveResult = bpService.save(taskCreationProcess, classService, dataService, authData, env);
		assertFalse("Saving triggerable process with more than one input value should fail", saveResult.isSuccess());
		assertProcessError("Invalid parameter assignment from {Query root user}.record (kommet.basic.RecordProxy) to {Create task}.user (kommet.basic.User)", saveResult, false);
		assertNotProcessError("Starting action has input values, but the process does not", saveResult, false);
		assertNotProcessError("Starting action has input values, but the process does not", saveResult, false);
		
		recordCreateCall = taskCreationProcess.getInvocation(recordCreateCall.getName());
		recordCreateCall.setAttribute("acceptedTypes", pigeonType.getKID().getId());
		
		saveResult = bpService.save(taskCreationProcess, classService, dataService, authData, env);
		assertFalse("Saving triggerable process with more than one input value should fail", saveResult.isSuccess());
		assertProcessError("Invalid parameter assignment from {" + queryUniqueCall.getName(), saveResult, true);
			
		// even though the return type of the query is not known, assume it is correct and verify at runtime
		queryUniqueCall.setAttribute("assumeValidOutputType", "true");
		
		saveResult = bpService.save(taskCreationProcess, classService, dataService, authData, env);
		assertFalse("Saving triggerable process with more than one input value should fail", saveResult.isSuccess());
		assertNotProcessError("Triggerable process has no valid entry point", saveResult, false);
		assertProcessError("Triggerable process " + taskCreationProcess.getName() + " should have exactly one input value, but has 2", saveResult, false);
		assertNotProcessError("Invalid parameter assignment from {" + queryUniqueCall.getName(), saveResult, true);
		assertNotProcessError("Invalid parameter assignment from ", saveResult, true);
		
		taskCreationProcess.setIsTriggerable(false);
		
		// make sure there are not process inputs assigned to any processes at this point
		assertEquals(0, env.getSelectCriteriaFromDAL("select id from BusinessProcessInput where NOT(businessProcess.id ISNULL)").list().size());
		assertEquals(0, env.getSelectCriteriaFromDAL("select id from BusinessProcessOutput where NOT(businessProcess.id ISNULL)").list().size());
		
		// save the process, this time it should be saved successfully
		saveResult = bpService.save(taskCreationProcess, classService, dataService, authData, env);
		assertTrue(saveResult.isSuccess());
		taskCreationProcess = saveResult.getProcess();
		
		testUpdateBusinessAction(createTaskAction, authData, env);
		
		assertEquals(2, env.getSelectCriteriaFromDAL("select id from BusinessProcessInput where NOT(businessProcess.id ISNULL)").list().size());
		assertEquals(1, env.getSelectCriteriaFromDAL("select id from BusinessProcessOutput where NOT(businessProcess.id ISNULL)").list().size());
		
		Record queriedProcess = env.getSelectCriteriaFromDAL("select id, inputs.id, inputs.name from BusinessProcess where id = '" + taskCreationProcess.getId() + "'").singleRecord();
		assertNotNull(queriedProcess);
		List<Record> inputs = (List<Record>)queriedProcess.getField("inputs");
		assertNotNull(inputs);
		assertEquals(2, inputs.size());
		
		queriedProcess = env.getSelectCriteriaFromDAL("select id, paramAssignments.id, inputs.id, inputs.name, outputs.id, outputs.name from BusinessProcess where id = '" + taskCreationProcess.getId() + "'").singleRecord();
		assertNotNull(queriedProcess);
		inputs = (List<Record>)queriedProcess.getField("inputs");
		assertNotNull(inputs);
		assertEquals(2, inputs.size());
		List<Record> outputs = (List<Record>)queriedProcess.getField("outputs");
		assertNotNull(outputs);
		assertEquals(1, outputs.size());
		
		queriedProcess = env.getSelectCriteriaFromDAL("select id, transitions.id, inputs.id, inputs.name, outputs.id, outputs.name from BusinessProcess where id = '" + taskCreationProcess.getId() + "'").singleRecord();
		assertNotNull(queriedProcess);
		inputs = (List<Record>)queriedProcess.getField("inputs");
		assertNotNull(inputs);
		assertEquals(2, inputs.size());
		outputs = (List<Record>)queriedProcess.getField("outputs");
		assertNotNull(outputs);
		assertEquals(1, outputs.size());
		
		// make sure exactly two inputs have been created
		assertEquals(2, env.getSelectCriteriaFromDAL("select id from BusinessProcessInput where NOT(businessProcess.id ISNULL)").list().size());
		assertEquals(1, env.getSelectCriteriaFromDAL("select id from BusinessProcessOutput where NOT(businessProcess.id ISNULL)").list().size());
		
		assertTrue(env.getTriggerableBusinessProcesses().isEmpty());
		
		// query the saved process
		taskCreationProcess = bpService.getBusinessProcess(taskCreationProcess.getId(), authData, env);
		
		assertEquals(2, env.getSelectCriteriaFromDAL("select id from BusinessProcessInput where NOT(businessProcess.id ISNULL)").list().size());
		assertEquals(1, env.getSelectCriteriaFromDAL("select id from BusinessProcessOutput where NOT(businessProcess.id ISNULL)").list().size());
		
		// make sure all collections have been initialized on the process object
		assertNotNull(taskCreationProcess.getInputs());
		assertFalse(taskCreationProcess.getInputs().isEmpty());
		assertEquals(2, taskCreationProcess.getInputs().size());
		
		for (BusinessProcessInput input : taskCreationProcess.getInputs())
		{
			assertTrue(input.getDataTypeName() != null || input.getDataTypeId() != null);
		}
		
		assertNotNull(taskCreationProcess.getOutputs());
		assertFalse(taskCreationProcess.getOutputs().isEmpty());
		assertEquals(1, taskCreationProcess.getOutputs().size());
		
		for (BusinessProcessOutput output : taskCreationProcess.getOutputs())
		{
			assertTrue(output.getDataTypeName() != null || output.getDataTypeId() != null);
		}
		
		assertNotNull(taskCreationProcess.getTransitions());
		assertFalse(taskCreationProcess.getTransitions().isEmpty());
		assertNotNull(taskCreationProcess.getInvocations());
		assertFalse(taskCreationProcess.getInvocations().isEmpty());
		
		for (BusinessActionInvocation inv : taskCreationProcess.getInvocations())
		{
			assertNotNull(inv.getInvokedAction());
			assertNotNull(inv.getInvokedAction().getName());
		}
		
		assertNotNull(taskCreationProcess.getParamAssignments());
		assertFalse(taskCreationProcess.getParamAssignments().isEmpty());
		
		for (BusinessProcessParamAssignment a : taskCreationProcess.getParamAssignments())
		{
			if (a.getSourceParam() != null)
			{
				assertTrue(a.getSourceParam().getDataTypeId() != null || a.getSourceParam().getDataTypeName() != null);
			}
			if (a.getTargetParam() != null)
			{
				assertTrue(a.getTargetParam().getDataTypeId() != null || a.getTargetParam().getDataTypeName() != null);
			}
			if (a.getSourceInvocation() != null)
			{
				assertNotNull(a.getSourceInvocation().getName());
				
				if (a.getSourceInvocation().getName().equals(recordCreateCall.getName()))
				{
					assertNotNull(a.getSourceInvocation().getSingleAttribute("acceptedTypes"));
				}
			}
			if (a.getTargetInvocation() != null)
			{
				assertNotNull(a.getTargetInvocation().getName());
			}
			if (a.getProcessInput() != null)
			{
				assertTrue(a.getProcessInput().getDataTypeId() != null || a.getProcessInput().getDataTypeName() != null);
			}
			if (a.getProcessOutput() != null)
			{
				assertTrue(a.getProcessOutput().getDataTypeId() != null || a.getProcessOutput().getDataTypeName() != null);
			}
		}
		
		// make sure a Java file for this process has not been compiled - this feature is not used
		assertNull(taskCreationProcess.getCompiledClass());
		
		// execute the process
		Map<String, Object> inputValues = new HashMap<String, Object>();
		inputValues.put("query", "select id, userName from User limit 1");
		
		// create a new pigeon
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Grzes");
		pigeon1.setField("age", 1);
		pigeon1 = dataService.save(pigeon1, env);
		RecordProxy proxy = RecordProxyUtil.generateCustomTypeProxy(pigeon1, env, compiler);
		inputValues.put("record", proxy);
		
		BusinessProcessExecutor processExecutor = new BusinessProcessExecutor(compiler, logService, classService, dataService, env);
		
		List<Record> tasks = env.getSelectCriteriaFromDAL("select id from Task where title = '" + pigeon1.getKID() + "'").list();
		assertEquals(0, tasks.size());
		
		ProcessResult processResult = null;
		
		try
		{
			processResult = processExecutor.execute(taskCreationProcess, inputValues, authData); 
			fail("Executing process should fail because input parameter with the correct name has not been passed to it");
		}
		catch (BusinessProcessException e)
		{
			assertEquals("Input parameter 'newRecord' not passed to process", e.getMessage());
		}
		
		inputValues.remove("record");
		inputValues.put("newRecord", proxy);
		processResult = processExecutor.execute(inputValues, authData);
		
		tasks = env.getSelectCriteriaFromDAL("select id from Task where title = '" + pigeon1.getKID() + "'").list();
		assertEquals(1, tasks.size());
		assertTrue(processResult.isSuccess());
		assertTrue(processResult.isPassedEntryPoint());
		assertTrue(processResult.getOutputValues().containsKey("createdTask"));

		// execute again, this time on a prepared executor
		processResult = processExecutor.execute(inputValues, authData);
		
		// make sure another task has been created
		tasks = env.getSelectCriteriaFromDAL("select id from Task where title = '" + pigeon1.getKID() + "'").list();
		assertEquals(2, tasks.size());
		
		assertNotNull(processResult.getOutputValues());
		assertEquals(1, processResult.getOutputValues().size());
		assertTrue(processResult.getOutputValues().containsKey("createdTask"));
		
		recordCreateCall = taskCreationProcess.getInvocation(recordCreateCall.getName());
		
		// now restrict the process entry point to only some types and make sure the process does not execute
		recordCreateCall.setAttribute("acceptedTypes", env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID().getId() + "," + env.getType(KeyPrefix.get(KID.EMAIL_PREFIX)).getKID().getId());
		processExecutor = new BusinessProcessExecutor(compiler, logService, classService, dataService, env);
		processResult = processExecutor.execute(taskCreationProcess, inputValues, authData);
		assertTrue(processResult.getOutputValues().isEmpty());
		assertFalse(processResult.isPassedEntryPoint());
		assertTrue(processResult.isSuccess());
		
		// make sure no new tasks have been created
		tasks = env.getSelectCriteriaFromDAL("select id from Task where title = '" + pigeon1.getKID() + "'").list();
		assertEquals(2, tasks.size());
		
		// now add the pigeon type to the list of types accepted by the entry point
		recordCreateCall.setAttribute("acceptedTypes", env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID().getId() + "," + pigeonType.getKID().getId());
		processExecutor = new BusinessProcessExecutor(compiler, logService, classService, dataService, env);
		processResult = processExecutor.execute(taskCreationProcess, inputValues, authData);
		assertTrue(processResult.getOutputValues().containsKey("createdTask"));
		assertTrue(processResult.isPassedEntryPoint());
		assertTrue(processResult.isSuccess());
		
		// make sure new task has been created
		tasks = env.getSelectCriteriaFromDAL("select id from Task where title = '" + pigeon1.getKID() + "'").list();
		assertEquals(3, tasks.size());
		
		testProcessWithIfCondition(taskCreationProcess, recordCreateCall, queryUniqueCall, createTaskCall, pigeonType, authData, env);
		
		assertNotNull(env.getProcessExecutor(taskCreationProcess, compiler, logService, classService, dataService));
		assertTrue(env.isCachedProcessExecutor(taskCreationProcess.getId()));
		
		// delete process
		bpService.deleteProcess(taskCreationProcess.getId(), authData, env);
		
		assertFalse(env.isCachedProcessExecutor(taskCreationProcess.getId()));
	}

	/**
	 * Tests which updates are allowed on a business action defined in file and used in a process, and which are not.
	 * @param createTaskAction
	 * @param authData
	 * @param env
	 * @throws KommetException 
	 */
	private void testUpdateBusinessAction(BusinessAction createTaskAction, AuthData authData, EnvData env) throws KommetException
	{
		assertNotNull(createTaskAction.getFile());
		assertNotNull(createTaskAction.getFile().getId());
		
		Class actionClass = classService.getClass(createTaskAction.getFile().getId(), env);
		
		String originalCode = actionClass.getKollCode();
		
		// make sure irrelevant changes to source code
		actionClass.setKollCode(originalCode.replace("Task t = new Task();", "// some comment\nTask t = new Task();"));
		
		// saving the class should be successful
		classService.fullSave(actionClass, dataService, authData, env);
		
		// change the input name
		actionClass.setKollCode(originalCode.replace("(name = \"task\")", "(name = \"otherName\")"));
	
		try
		{
			classService.fullSave(actionClass, dataService, authData, env);
			fail("Saving action with changed input name should fail");
		}
		catch (BusinessProcessDeclarationException e)
		{
			assertEquals("New output otherName added to used action", e.getErrors().get(0).getMessage());
		}
		
		// change the input description
		actionClass.setKollCode(originalCode.replace("(name = \"task\")", "(name = \"task\", description = \"test\")"));
		// saving the class should be successful
		classService.fullSave(actionClass, dataService, authData, env);
		
		// now try to add an input to the class
		String newInputCode = "@" + Input.class.getSimpleName() + "(name = \"newInput\")\npublic " + Task.class.getName() + " getNewInput() { return null; }";
		actionClass.setKollCode(originalCode.replace("{ this.user = user; }", "{ this.user = user; }\n\n" + newInputCode));
		
		try
		{
			classService.fullSave(actionClass, dataService, authData, env);
			fail("Saving action declaration with new input should fail because the action is used in a process");
		}
		catch (BusinessProcessDeclarationException e)
		{
			assertEquals("@Input method getNewInput must take exactly one parameter", e.getMessage());
		}
		
		// now try to add an input to the class
		String newOutputCode = "@" + Output.class.getSimpleName() + "(name = \"newInput\")\npublic " + Task.class.getName() + " getNewInput() { return null; }";
		actionClass.setKollCode(originalCode.replace("{ this.user = user; }", "{ this.user = user; }\n\n" + newOutputCode));
		
		try
		{
			classService.fullSave(actionClass, dataService, authData, env);
			fail("Saving action declaration with new input should fail because the action is used in a process");
		}
		catch (BusinessProcessDeclarationException e)
		{
			assertEquals("Number of outputs changed on used action from 1 to 2", e.getMessage());
		}
		
		// update the type of the returned output
		actionClass.setKollCode(originalCode.replace("public " + Task.class.getName() + " getTask()", "public " + RecordProxy.class.getName() + " getTask()"));
		try
		{
			classService.fullSave(actionClass, dataService, authData, env);
			fail("Saving action declaration with new input should fail because the action is used in a process");
		}
		catch (BusinessProcessDeclarationException e)
		{
			assertTrue(e.getMessage().startsWith("Data type changed on output task"));
			assertTrue(e.getMessage().endsWith("in used action"));
		}
	}

	/**
	 * Test a process that includes an if-condition.
	 * @param process
	 * @param recordCreateCall
	 * @param queryUniqueCall
	 * @param createTaskCall
	 * @param pigeonType
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	private void testProcessWithIfCondition(BusinessProcess process, BusinessActionInvocation recordCreateCall, BusinessActionInvocation queryUniqueCall, BusinessActionInvocation createTaskCall, Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		// first remove the transition from the record create input to the record query call
		process.removeTransition(recordCreateCall, queryUniqueCall);
		process.removeTransition(queryUniqueCall, createTaskCall);
		
		assertNotNull(process.getInvocation(createTaskCall.getName()));
		
		// now add an if-condition
		BusinessAction ifAction = bpService.getIfAction(env);
		assertNotNull(ifAction);
		
		// add action to process
		BusinessActionInvocation ifAgeCall = process.addAction(ifAction, "Check pigeon age");
		assertEquals("Check pigeon age", ifAgeCall.getName());
		
		// try to save the process
		BusinessProcessSaveResult saveResult = bpService.save(process, classService, dataService, authData, env);
		assertFalse("Saving process should fail because the if action has no condition assigned", saveResult.isSuccess());
		assertProcessError("If action " + ifAgeCall.getName() + " has no condition assigned", saveResult, false);
		
		ifAgeCall.setAttribute("condition", "{" + recordCreateCall.getName() + "}.record.stubField > 10");
		
		saveResult = bpService.save(process, classService, dataService, authData, env);
		assertFalse("Saving process with an if-action that has no true/false transitions set should fail", saveResult.isSuccess());
		assertProcessError("If action " + ifAgeCall.getName() + " has to have at least one outgoing transition", saveResult, false);
		assertNotProcessError("If action " + ifAgeCall.getName() + " has no condition assigned", saveResult, false);
		
		process.setIsActive(false);
		process.setIsDraft(true);
		process.setIsTriggerable(true);
		
		assertNotNull(process.getInvocation(createTaskCall.getName()));
		
		saveResult = bpService.save(process, classService, dataService, authData, env);
		assertTrue(saveResult.isSuccess());
		process = bpService.getBusinessProcess(process.getId(), authData, env);
		
		assertEquals(2, process.getInputs().size());
		
		// make sure the process has not been registered with the environment (because it is not active)
		assertNotNull(env.getTriggerableBusinessProcesses());
		assertTrue(env.getTriggerableBusinessProcesses().isEmpty());
		
		assertNotNull("Added invocation not set when process was queried", process.getInvocation(ifAgeCall.getName()));
		assertNotNull("Invocation not set when process was queried", process.getInvocation(createTaskCall.getName()));
		
		BusinessActionInvocation ifInvocation = process.getInvocation(ifAgeCall.getName());
		assertNotNull(ifInvocation);
		
		assertTrue("When process is created in draft mode, if-condition attributes are not set", ifInvocation.getAttribute("requiredInvocationIds").isEmpty());
		assertTrue("When process is created in draft mode, if-condition attributes are not set", ifInvocation.getAttribute("evaluatorClassId").isEmpty());
		
		verifyQueriedProcessComplete(process);
		
		// add transition from the entry action to the if-condition
		process.addTransition(recordCreateCall, ifAgeCall);
		
		BusinessAction queryUniqueAction = bpService.getAction("QueryUnique", authData, env);
		
		BusinessActionInvocation queryRootCall = process.addAction(queryUniqueAction, "Query root");
		assertEquals("Query root", queryRootCall.getName());
		queryRootCall.setAttribute("query", "select id, userName from User where userName = 'root'");
		
		BusinessActionInvocation queryAdminCall = process.addAction(queryUniqueAction, "Query admin");
		assertEquals("Query admin", queryAdminCall.getName());
		queryAdminCall.setAttribute("query", "select id, userName from User where profile.name = '" + Profile.SYSTEM_ADMINISTRATOR_NAME + "' limit 1");
		
		process.getInvocation("Query root user").setAttribute("assumeValidOutputType", "true");
		
		// since we want to use an if-condition that checks the age of a pigeon, we have to accept only the pigeon type
		// as process input, otherwise the if-condition evaluation will fail if provided with a record of a different type
		process.getInvocation(recordCreateCall.getName()).setAttribute("acceptedTypes", pigeonType.getKID().getId());
		
		process.addTransition(ifAgeCall, queryRootCall);
		process.addTransition(ifAgeCall, queryAdminCall);
		
		process.addInvocationAttribute(ifAgeCall.getName(), "ifTrueInvocationName", queryRootCall.getName());
		process.addInvocationAttribute(ifAgeCall.getName(), "ifFalseInvocationName", queryAdminCall.getName());
		
		// after the user has been queried, merge back to the task created invocation
		process.addTransition(queryAdminCall, createTaskCall);
		process.addTransition(queryRootCall, createTaskCall);
		
		process.setIsActive(true);
		process.setIsDraft(false);
		process.setIsTriggerable(false);
		
		verifyQueriedProcessComplete(process);
		
		saveResult = bpService.save(process, classService, dataService, authData, env);
			
		// this save should fail because the if condition reference property "age", which is not defined on abstract type "record"
		// (the system does not know whether the record will be an instance of pigeon of some other type, it just knows it's a record)
		assertFalse("Saving process with invalid if condition should fail", saveResult.isSuccess());
		assertProcessError("Field stubField not found on type " + pigeonType.getQualifiedName(), saveResult, false);
		
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Helena");
		pigeon1.setField("age", 9);
		pigeon1 = dataService.save(pigeon1, env);
		
		// correct the if-condition
		process.getInvocation(ifAgeCall.getName()).setAttribute("condition", "{" + recordCreateCall.getName() + "}.record.id == '" + pigeon1.getKID() + "'");
		
		assertEquals(1, env.getSelectCriteriaFromDAL("select id, name, value from BusinessActionInvocationAttribute where name = 'acceptedTypes'").list().size());
		
		// save the process again - this time it should succeed
		saveResult = bpService.save(process, classService, dataService, authData, env);env.getSelectCriteriaFromDAL("select id, name, value from BusinessActionInvocationAttribute").list();
		assertTrue(saveResult.isSuccess());
		
		// make sure the process have not been registered on the environment
		assertNotNull(env.getTriggerableBusinessProcesses());
		assertFalse(env.getTriggerableBusinessProcesses().containsKey(pigeonType.getKID().getId()));
		List<BusinessProcess> pigeonProcesses = env.getTriggerableBusinessProcesses().get(RecordProxy.class.getName());
		assertNull(pigeonProcesses);
		
		assertEquals(1, env.getSelectCriteriaFromDAL("select id, name, value from BusinessActionInvocationAttribute where name = 'acceptedTypes'").list().size());
		
		process = bpService.getBusinessProcess(process.getId(), authData, env);
		
		// make sure attributes are set on the entry point - at one point there was a problem with them being erased when the process was saved
		assertFalse(process.getInvocation(recordCreateCall.getName()).getAttribute("acceptedTypes").isEmpty());
		
		ifInvocation = process.getInvocation(ifAgeCall.getName());
		assertNotNull(ifInvocation);
		assertNotNull(ifInvocation.getAttribute("condition"));
		assertNotNull(ifInvocation.getAttribute("requiredInvocationIds"));
		assertNotNull(ifInvocation.getAttribute("evaluatorClassId"));
		
		KID evaluatorClassId = KID.get(ifInvocation.getSingleAttributeValue("evaluatorClassId"));
		
		// make sure evaluator class for the if-condition has been created
		Class evaluatorClass = classService.getClass(evaluatorClassId, env); 
		assertNotNull(evaluatorClass);
		assertTrue(evaluatorClass.getName().endsWith(ifInvocation.getId().getId()));
		
		long evaluatorClassLastModifiedDate = evaluatorClass.getLastModifiedDate().getTime();
		
		// find the if-condition invocation
		assertNotNull(ifInvocation);
		assertNotNull(ifInvocation.getAttribute("ifTrueInvocationName"));
		assertNotNull(ifInvocation.getAttribute("ifFalseInvocationName"));
		assertEquals(queryRootCall.getName(), ifInvocation.getAttribute("ifTrueInvocationName").get(0).getValue());
		assertEquals(queryAdminCall.getName(), ifInvocation.getAttribute("ifFalseInvocationName").get(0).getValue());
		
		// the query is no longer passed to the process, it will be configured as an attribute of the query
		// the query for the QueryUnique action will not be taken from a process input param, but from an attribute of the invocation which was set earlier
		process.removeInput("query");
		
		KID newRecordInputId = process.getInput("newRecord").getId();
		KID createdTaskOutputId = process.getOutput("createdTask").getId();
		
		saveResult = bpService.save(process, classService, dataService, authData, env);
		assertFalse("Saving process should fail because one of its starting actions has no parameter assignments", saveResult.isSuccess());
		assertProcessError("Input parameter query of the starting action Query root user has no assigned input value from the process input, nor is the input defined in invocation attributes", saveResult, false);
		
		// remove the unused invocation that had no parameter assignments
		process.removeInvocation("Query root user");
		
		saveResult = bpService.save(process, classService, dataService, authData, env);
		assertFalse("Saving process should fail because one of the invocations has not parameter assignment", saveResult.isSuccess());
		assertProcessError("No parameter assignment for input parameter user of action Create task", saveResult, false);
		assertNotProcessError("Input parameter query of the starting action Query root user has no assigned input value from the process input, nor is the input defined in invocation attributes", saveResult, false);
		
		// add parameter assignment for the {Create task}.user parameter
		process.assignParam(queryRootCall, queryRootCall.getInvokedAction().getOutput("record"), process.getInvocation(createTaskCall.getName()), createTaskCall.getInvokedAction().getInput("user"));
		process.assignParam(queryAdminCall, queryAdminCall.getInvokedAction().getOutput("record"), process.getInvocation(createTaskCall.getName()), createTaskCall.getInvokedAction().getInput("user"));
		
		process.setIsTriggerable(true);
		
		KID originalIfInvocationId = ifInvocation.getId();
		
		// this time saving the process should succeed
		saveResult = bpService.save(process, classService, dataService, authData, env);
		assertTrue("Unexpected process errors:\n" + MiscUtils.implode(saveResult.getErrors(), "\n"),saveResult.isSuccess());
		
		// make sure that if the if-condition has not changed, the evaluator class also has not changed
		BusinessProcess refetchedProcess = bpService.getBusinessProcess(process.getId(), authData, env);
		BusinessActionInvocation newIfInvocation = refetchedProcess.getInvocation(ifInvocation.getName());
		assertEquals(originalIfInvocationId, newIfInvocation.getId());
		KID newEvaluatorClassId = KID.get(newIfInvocation.getSingleAttributeValue("evaluatorClassId"));
		assertEquals(evaluatorClassId, newEvaluatorClassId);
		assertEquals(evaluatorClassLastModifiedDate, classService.getClass(newEvaluatorClassId, env).getLastModifiedDate().getTime());
		assertEquals(newRecordInputId, refetchedProcess.getInput("newRecord").getId());
		assertEquals(createdTaskOutputId, refetchedProcess.getOutput("createdTask").getId());
				
		// make sure the process has been registered on the environment
		assertNotNull(env.getTriggerableBusinessProcesses());
		assertFalse(env.getTriggerableBusinessProcesses().containsKey(pigeonType.getKID().getId()));
		pigeonProcesses = env.getTriggerableBusinessProcesses().get(RecordProxy.class.getName());
		assertNotNull(pigeonProcesses);
		assertEquals(1, pigeonProcesses.size());
		assertEquals(process.getId(), pigeonProcesses.get(0).getId());
		
		Map<String, Object> inputValues = new HashMap<String, Object>();
		
		inputValues.put("newRecord", RecordProxyUtil.generateCustomTypeProxy(pigeon1, env, compiler));
		
		// create some system administrator so that the queries used in the process have data
		User sysAdmin = dataHelper.getTestUser("sys@kommet.io", "sys@kommet.io", profileService.getProfileByName(Profile.SYSTEM_ADMINISTRATOR_NAME, env), env);
		
		BusinessProcessExecutor processExecutor = new BusinessProcessExecutor(compiler, logService, classService, dataService, env);
		
		ProcessResult processResult = processExecutor.execute(process, inputValues, authData);
		
		List<Record> tasks = env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task where title = '" + pigeon1.getKID() + "'").list();
		assertEquals(1, tasks.size());
		assertTrue(processResult.getOutputValues().containsKey("createdTask"));
		Record taskAssignee = (Record)tasks.get(0).getField("assignedUser");
		assertNotNull(taskAssignee);
		assertEquals(BasicSetupService.ROOT_USERNAME, taskAssignee.getField("userName"));
		
		int initialTaskCount = env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task").list().size();
		
		// now execute the process providing an older pigeon as input
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Maria");
		pigeon2.setField("age", 11);
		pigeon2 = dataService.save(pigeon2, env);
		
		// no new tasks should be created because there is an if-condition for one specific pigeon ID
		assertEquals(initialTaskCount + 1, env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task").list().size());
		tasks = env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task where title = '" + pigeon2.getKID() + "'").list();
		assertEquals(1, tasks.size());
		assertTrue(processResult.getOutputValues().containsKey("createdTask"));
		taskAssignee = (Record)tasks.get(0).getField("assignedUser");
		assertNotNull(taskAssignee);
		assertEquals(sysAdmin.getId(), taskAssignee.getKID());
		
		// change the if condition to depend on age
		process.getInvocation(ifAgeCall.getName()).setAttribute("condition", "{" + recordCreateCall.getName() + "}.record.age > 10");
		bpService.save(process, classService, dataService, authData, env);
		
		// reinitialize the process executor
		processExecutor = new BusinessProcessExecutor(compiler, logService, classService, dataService, env);
		processExecutor.prepare(process);
		
		// do some updates to the create task action class to make sure they don't break the process
		Class actionFile = classService.getClass("com.bp.CreateTaskBusinessAction", env);
		assertNotNull(actionFile);
		// restore the original action code
		actionFile.setKollCode(actionFile.getKollCode().replace("public " + RecordProxy.class.getName() + " getTask()", "public " + Task.class.getName() + " getTask()"));
		classService.fullSave(actionFile, dataService, authData, env);
		
		Record pigeon3 = new Record(pigeonType);
		pigeon3.setField("name", "Maria");
		pigeon3.setField("age", 11);
		pigeon3 = dataService.save(pigeon3, env);
		
		assertEquals(initialTaskCount + 2, env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task").list().size());
		
		envService.resetEnv(env.getId());
		
		actionFile = classService.getClass("com.bp.CreateTaskBusinessAction", env);
		assertNotNull(actionFile);
		// do some change to code - add a comment
		actionFile.setKollCode(actionFile.getKollCode().replace("t.setPriority(1);", "t.setPriority(1);//comment"));
		classService.fullSave(actionFile, dataService, authData, env);
		
		Record pigeon4 = new Record(pigeonType);
		pigeon4.setField("name", "Maria");
		pigeon4.setField("age", 11);
		pigeon4 = dataService.save(pigeon4, env);
		
		assertEquals(initialTaskCount + 3, env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task").list().size());
		
		// task should be created manually because the process is registered with the env
		tasks = env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task where title = '" + pigeon3.getKID() + "'").list();
		assertEquals(1, tasks.size());
		assertTrue(processResult.getOutputValues().containsKey("createdTask"));
		taskAssignee = (Record)tasks.get(0).getField("assignedUser");
		assertNotNull(taskAssignee);
		assertEquals(BasicSetupService.ROOT_USERNAME, taskAssignee.getField("userName"));
		
		// update the pigeon and make sure this does not trigger the process (because it should only be triggered when a record is created)
		pigeon3 = dataService.save(pigeon3, env);
		assertEquals(initialTaskCount + 3, env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task").list().size());
		
		// call the process manually again
		inputValues.put("newRecord", RecordProxyUtil.generateCustomTypeProxy(pigeon3, env, compiler));
		processResult = processExecutor.execute(inputValues, authData);
		assertEquals(initialTaskCount + 4, env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task").list().size());
		tasks = env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task where title = '" + pigeon3.getKID() + "' order by createdDate desc").list();
		assertEquals(2, tasks.size());
		assertTrue(processResult.getOutputValues().containsKey("createdTask"));
		taskAssignee = (Record)tasks.get(0).getField("assignedUser");
		assertNotNull(taskAssignee);
		assertEquals(BasicSetupService.ROOT_USERNAME, taskAssignee.getField("userName"));
		
		// create a pigeon that does not meet the age criteria in the process
		Record pigeon5 = new Record(pigeonType);
		pigeon5.setField("name", "Maria");
		pigeon5.setField("age", 9);
		dataService.save(pigeon5, env);
		assertEquals(initialTaskCount + 5, env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task").list().size());
		tasks = env.getSelectCriteriaFromDAL("select id, assignedUser.id, assignedUser.userName from Task where title = '" + pigeon5.getKID() + "' order by createdDate desc").list();
		assertEquals(1, tasks.size());
		assertTrue(processResult.getOutputValues().containsKey("createdTask"));
		taskAssignee = (Record)tasks.get(0).getField("assignedUser");
		assertNotNull(taskAssignee);
		assertEquals(sysAdmin.getId(), taskAssignee.getKID());
	}

	/**
	 * Makes sure the business process has complete data
	 * @param process
	 */
	private void verifyQueriedProcessComplete(BusinessProcess process)
	{
		if (process.getTransitions() != null)
		{
			for (BusinessActionTransition t : process.getTransitions())
			{
				assertNotNull(t.getNextAction());
				assertNotNull(t.getNextAction().getName());
				assertNotNull(t.getPreviousAction());
				assertNotNull(t.getPreviousAction().getName());
			}
		}
	}

	private void declareTestBusinessActions(AuthData authData, EnvData env) throws KommetException
	{
		// create action that creates a task
		createCreateTaskAction(authData, env);
	}

	/**
	 * Create a sample action that creates a task.
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	private void createCreateTaskAction(AuthData authData, EnvData env) throws KommetException
	{
		List<String> inputs = new ArrayList<String>();
		inputs.add("@" + Input.class.getSimpleName() + "(name = \"record\")\npublic void setRecord(RecordProxy record) { this.record = record; }");
		
		inputs.add("@" + Input.class.getSimpleName() + "(name = \"user\")\npublic void setUser(" + User.class.getName() + " user) { this.user = user; }");
		
		List<String> outputs = new ArrayList<String>();
		outputs.add("@" + Output.class.getSimpleName() + "(name = \"task\")\npublic " + Task.class.getName() + " getTask() { return this.task; }");
		
		List<String> fields = new ArrayList<String>();
		fields.add("private " + Task.class.getName() + " task;");
		fields.add("private " + RecordProxy.class.getName() + " record;");
		fields.add("private " + User.class.getName() + " user;");
		
		String execute = "@" + Execute.class.getSimpleName() + "\npublic void execute() throws KommetException {";
		execute += "Task t = new Task();\n";
		execute += "t.setTitle(this.record.getId().getId());\n";
		execute += "t.setContent(\"Any content\");\n";
		execute += "t.setPriority(1);\n";
		execute += "t.setStatus(\"Open\");\n";
		execute += "if (this.user == null) { throw new KommetException(\"Assignee not provided\"); }\n";
		execute += "t.setAssignedUser(this.user);\n";
		execute += "this.task = sys.save(t);\n";
		execute += "}";
		
		Class file = getBusinessActionFile("CreateTaskBusinessAction", "com.bp", "Create Task For Pigeon", true, inputs, outputs, execute, fields, authData, env);
		file = classService.fullSave(file, dataService, authData, env);
	}
	
	private void createClonePigeonAction(Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		List<String> inputs = new ArrayList<String>();
		List<String> outputs = new ArrayList<String>();
		inputs.add("@" + Input.class.getSimpleName() + "(name = \"name\")\npublic void setName(" + String.class.getName() + " name) { this.name = name; }");
		
		inputs.add("@" + Input.class.getSimpleName() + "(name = \"age\")\npublic void setAge(" + Integer.class.getName() + " age) { this.age = age; }");
		
		inputs.add("@" + Input.class.getSimpleName() + "(name = \"owner\")\npublic void setOwner(" + env.getType(KeyPrefix.get(KID.USER_PREFIX)).getQualifiedName() + " user) { this.owner = user; }");
		
		List<String> fields = new ArrayList<String>();
		fields.add("private " + String.class.getName() + " name;");
		fields.add("private " + Integer.class.getName() + " age;");
		fields.add("private " + env.getType(KeyPrefix.get(KID.USER_PREFIX)).getQualifiedName() + " owner;");
		
		String execute = "@" + Execute.class.getSimpleName() + "\npublic void execute() throws KommetException {";
		execute += "if (this.age == 133) { return; }\n";
		execute += pigeonType.getQualifiedName() + " pigeon = new " + pigeonType.getQualifiedName() + "();\n";
		execute += "pigeon.setName(this.name + \" - clone\");\n";
		execute += "pigeon.setAge(this.age + 1);\n";
		execute += "if (this.owner != null) { pigeon.setOwner(this.owner); }\n";
		execute += "sys.save(pigeon);\n";
		execute += "}";
		
		Class file = getBusinessActionFile("ClonePigeonAction", "com.bp", "Clone Pigeon", true, inputs, outputs, execute, fields, authData, env);
		file = classService.fullSave(file, dataService, authData, env);
	}

	@Test
	public void testDeclareBusinessAction() throws KommetException, ClassNotFoundException, MalformedURLException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		List<String> inputs = new ArrayList<String>();
		inputs.add("@" + Input.class.getSimpleName() + "(name = \"record\")\npublic void setRecord(Record r) {}");
		
		List<String> outputs = new ArrayList<String>();
		outputs.add("@" + Output.class.getSimpleName() + "(name = \"email\")\npublic String getEmail() { return null; }");
		List<String> fields = new ArrayList<String>();
		
		String execute = "@" + Execute.class.getSimpleName() + "\npublic void execute() { }"; 
		
		Class file = getBusinessActionFile("SendEmailBusinessAction", "com.bp", "Send Email For Pigeon", true, inputs, outputs, execute, fields, authData, env);
		file = classService.fullSave(file, dataService, authData, env);
		
		// make sure a business action has been defined
		BusinessAction sendEmailAction = bpService.getAction("Send Email For Pigeon", authData, env);
		assertNotNull(sendEmailAction);
		assertEquals(file.getId(), sendEmailAction.getFile().getId());
		assertNotNull(sendEmailAction.getInputs());
		assertNotNull(sendEmailAction.getOutputs());
		assertEquals(1, sendEmailAction.getInputs().size());
		assertEquals(1, sendEmailAction.getOutputs().size());
		
		testActionWithIncorrectParameter(authData, env);
		
		// now remove the @BusinessAction annotation
		Class newFile = getBusinessActionFile("SendEmailBusinessAction", "com.bp", "Send Email For Pigeon", false, inputs, outputs, execute, fields, authData, env);
		file.setKollCode(newFile.getKollCode());
		file = classService.fullSave(file, dataService, authData, env);
		assertNull(bpService.getAction("Send Email For Pigeon", authData, env));
		
		// declare the action anew
		newFile = getBusinessActionFile("SendEmailBusinessAction", "com.bp", "Send Email For Pigeon", true, inputs, outputs, execute, fields, authData, env);
		file.setKollCode(newFile.getKollCode());
		file = classService.fullSave(file, dataService, authData, env);
		assertNotNull(bpService.getAction("Send Email For Pigeon", authData, env));
		assertEquals(file.getId(), sendEmailAction.getFile().getId());
		assertNotNull(sendEmailAction.getInputs());
		assertNotNull(sendEmailAction.getOutputs());
		assertEquals(1, sendEmailAction.getInputs().size());
		assertEquals(1, sendEmailAction.getOutputs().size());
		
		// update the action changing its name
		newFile = getBusinessActionFile("SendEmailBusinessAction", "com.bp", "New Action Name", true, inputs, outputs, execute, fields, authData, env);
		file.setKollCode(newFile.getKollCode());
		file = classService.fullSave(file, dataService, authData, env);
		assertNull(bpService.getAction("Send Email For Pigeon", authData, env));
		assertNotNull(bpService.getAction("New Action Name", authData, env));
		BusinessAction newAction = bpService.getAction("New Action Name", authData, env);
		assertEquals(file.getId(), newAction.getFile().getId());
		assertEquals(sendEmailAction.getFile().getId(), newAction.getFile().getId());
		assertNotNull(newAction.getInputs());
		assertNotNull(newAction.getOutputs());
		assertEquals(1, newAction.getInputs().size());
		assertEquals(1, newAction.getOutputs().size());
		
		// now delete the declaring class altogether
		classService.delete(file, dataService, authData, env);
		assertNull(bpService.getAction("Send Email For Pigeon", authData, env));
	}

	private void testActionWithIncorrectParameter(AuthData authData, EnvData env) throws KommetException
	{
		List<String> inputs = new ArrayList<String>();
		inputs.add("@" + Input.class.getSimpleName() + "(name = \"record\")\npublic void setRecord(Record r) {}");
		
		List<String> outputs = new ArrayList<String>();
		outputs.add("@" + Output.class.getSimpleName() + "(name = \"email\")\npublic java.lang.Class<?> getEmail() { return null; }");
		List<String> fields = new ArrayList<String>();
		
		String execute = "@" + Execute.class.getSimpleName() + "\npublic void execute() {}"; 
		
		Class file = getBusinessActionFile("SendAnyEmailAction", "com.bp", "Send Email For Pigeon", true, inputs, outputs, execute, fields, authData, env);
		
		try
		{
			file = classService.fullSave(file, dataService, authData, env);
			fail("Saving action with incorrect parameter type should fail");
		}
		catch (BusinessProcessDeclarationException e)
		{
			assertTrue(e.getMessage().startsWith("Output parameter email has incorrect data type"));
		}
		
		inputs = new ArrayList<String>();
		inputs.add("@" + Input.class.getSimpleName() + "(name = \"record\")\npublic void setRecord(java.lang.Class<?> r) {}");
		
		outputs = new ArrayList<String>();
		outputs.add("@" + Output.class.getSimpleName() + "(name = \"email\")\npublic java.lang.String getEmail() { return null; }");
		fields = new ArrayList<String>(); 
		
		file = getBusinessActionFile("TestBusinessAction", "com.bp", "Send Email For Pigeon", true, inputs, outputs, execute, fields, authData, env);
		
		try
		{
			file = classService.fullSave(file, dataService, authData, env);
			fail("Saving action with incorrect parameter type should fail");
		}
		catch (BusinessProcessDeclarationException e)
		{
			assertTrue(e.getMessage().startsWith("Input parameter record has incorrect data type"));
		}
	}

	private Class getBusinessActionFile(String className, String packageName, String name, boolean addAnnotation, List<String> inputs, List<String> outputs, String execute, List<String> fields, AuthData authData, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		code.append("package " + packageName).append(";\n");
		
		// add imports
		code.append("import ").append(kommet.businessprocess.annotations.BusinessAction.class.getName()).append(";\n");
		code.append("import ").append(Input.class.getName()).append(";\n");
		code.append("import ").append(Output.class.getName()).append(";\n");
		code.append("import ").append(Execute.class.getName()).append(";\n");
		code.append("import ").append(Record.class.getName()).append(";\n");
		code.append("import ").append(RecordProxy.class.getName()).append(";\n");
		code.append("import ").append(User.class.getName()).append(";\n");
		code.append("import ").append(Task.class.getName()).append(";\n");
		code.append("import ").append(KommetException.class.getName()).append(";\n");
		
		if (addAnnotation)
		{
			code.append("\n@" + BusinessAction.class.getSimpleName()).append("(name = \"").append(name).append("\")\n");
		}
		
		code.append("public class ").append(className).append("\n{\n");
		
		code.append(MiscUtils.implode(fields, "\n\n"));
		code.append(MiscUtils.implode(inputs, "\n\n"));
		code.append(MiscUtils.implode(outputs, "\n\n"));
		code.append(execute);
		
		// close class
		code.append("\n}");
		
		Class cls = new Class();
		cls.setName(className);
		cls.setPackageName(packageName);
		cls.setKollCode(code.toString());
		cls.setIsSystem(false);
		
		return cls;
	}
}
