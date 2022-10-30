/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */
	
package kommet.tests.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import kommet.auth.AuthData;
import kommet.basic.Action;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.Profile;
import kommet.basic.RecordProxyUtil;
import kommet.basic.StandardAction;
import kommet.basic.TypeInfo;
import kommet.basic.actions.ActionCreationException;
import kommet.basic.actions.ActionDao;
import kommet.basic.actions.ActionFilter;
import kommet.basic.actions.ActionService;
import kommet.basic.actions.StandardActionType;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.StandardTypeControllerUtil;
import kommet.basic.keetle.ViewService;
import kommet.basic.types.ActionKType;
import kommet.basic.types.SystemTypes;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KeyPrefix;
import kommet.data.KommetException;
import kommet.data.NotNullConstraintViolationException;
import kommet.data.OperationResult;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeInfoService;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.koll.SystemContextFactory;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.tests.types.TypeManipulationTest;
import kommet.utils.AppConfig;
import kommet.web.actions.ActionUtil;

public class ActionTest extends BaseUnitTest
{
	@Inject
	ActionDao actionDao;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ClassService classService;
	
	@Inject
	TypeInfoService typeInfoService;
	
	@Inject
	SystemContextFactory sysContextFactory;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	AppConfig appConfig;
	
	@Test
	public void testSaveAction() throws KommetException, ClassNotFoundException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		String pageUrl = "test/page/name";
		
		// create test action
		ActionKType actionType = (ActionKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME));
		Record testAction = new Record(actionType);
		testAction.setField("url", pageUrl);
		testAction.setField("isSystem", false);
		testAction.setField("isPublic", false);
		testAction.setField("name", "Test Name");
		testAction.setField("createdDate", new Date());
		testAction.setField("controllerMethod", "pigeonList");
		
		assertNull(testAction.attemptGetKID());
		assertFalse((new Action(testAction, env)).isSet("id"));
		
		try
		{
			actionDao.save(new Action(testAction, env), dataHelper.getRootAuthData(env), env);
			fail("Saving action with empty view and controller file should fail");
		}
		catch (KommetException e)
		{
			// expected exception
		}
		
		assertNull(testAction.attemptGetKID());
		
		Record pigeonListView = dataHelper.getPigeonListView(env);
		testAction.setField("view", pigeonListView);
		
		Record pigeonListKollFile = dataHelper.getPigeonListControllerClass(env);
		testAction.setField("controller", pigeonListKollFile);
		
		Action testPageProxy = new Action(testAction, env);
		assertFalse(testPageProxy.isSet("id"));
		assertNull(testPageProxy.getId());
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		try
		{
			actionDao.save(testPageProxy, authData, env);
			fail("Saving action with unsaved controller should fail");
		}
		catch (KommetException e)
		{
			assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("Field reference to controller on type " + SystemTypes.ACTION_API_NAME + " is unsaved") || e.getMessage().contains("Field reference to view on type " + SystemTypes.ACTION_API_NAME + " is unsaved"));
		}
		
		// save view and koll file
		pigeonListView = dataService.save(pigeonListView, env);
		testAction.setField("view", pigeonListView);
		pigeonListKollFile = dataService.save(pigeonListKollFile, env);
		testAction.setField("controller", pigeonListKollFile);
		
		Class controllerFile = new Class(pigeonListKollFile, env);
		
		// compile controller class so that it can be called
		CompilationResult compilationResult = compiler.compile(controllerFile, env);
		
		if (!compilationResult.isSuccess())
		{
			fail("Compilation failed: " + compilationResult.getDescription());
		}
		
		// find class by name
		assertNotNull("File not found: " + controllerFile.getQualifiedName(), compiler.getClass(controllerFile.getQualifiedName(), true, env));
		
		// save the page
		Action savedAction = actionDao.save(new Action(testAction, env), dataHelper.getRootAuthData(env), env);
		assertNotNull(savedAction.getId());
		assertEquals(savedAction.getUrl(), testAction.getField("url"));
		
		// find the inserted action using DAL
		List<Record> actionRecords = env.getSelectCriteriaFromDAL("select id isSystem, createdDate, url, name, controllerMethod, controller.id, view.id from " + SystemTypes.ACTION_API_NAME + " where id = '" + savedAction.getId() + "'").list();
		assertEquals(1, actionRecords.size());
		assertNotNull(actionRecords.get(0).getField("controller"));
		
		// make sure page object can be constructed from this record
		Action stub = new Action(actionRecords.get(0), env);
		//assertNotNull(stub.getView());
		assertNotNull(stub.getController());
		
		// now find the inserted file using DAO
		ActionFilter filter = new ActionFilter();
		filter.setUrl(savedAction.getUrl());
		List<Action> actions = actionDao.find(filter, env);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		assertNotNull(actionService.getActionByName(savedAction.getName(), authData, env));
		
		// find actions by name
		filter = new ActionFilter();
		filter.setNameLike("Test");
		actions = actionDao.find(filter, env);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		
		// find actions by name or URL
		filter = new ActionFilter();
		filter.setNameOrUrl("Test");
		actions = actionDao.find(filter, env);
		assertNotNull(actions);
		assertEquals(1, actions.size());
		
		// testing finding by ID using DAO
		savedAction = actionDao.get(savedAction.getId(), env);
		assertNotNull(savedAction);
		assertNotNull("Controller ID was not retrieved when querying page", savedAction.getController().getId());
		assertEquals(pigeonListKollFile.getKID(), savedAction.getController().getId());
		//assertNotNull(savedPage.getController().getName());
		assertNotNull(savedAction.getControllerMethod());
		
		// get controller for this page
		controllerFile = classService.getClass(savedAction.getController().getId(), env);
		assertNotNull(controllerFile);
		savedAction.setController(controllerFile);
		
		testAddingActionToEnv(savedAction, env);
		testCallingAction(savedAction, env);
		testSavingDuplicateAction(savedAction, authData, env);
		
		// try to delete view associated with the action
		try
		{
			viewService.deleteView(savedAction.getView(), env);
			fail("Deleting view used by an action should fail");
		}
		catch (NotNullConstraintViolationException e)
		{
			assertTrue(e.getMessage().startsWith("Not null constraint violation "));
		}
		
		try
		{
			viewService.delete(savedAction.getView().getId(), authData, env);
			fail("Deleting view used by an action should fail");
		}
		catch (NotNullConstraintViolationException e)
		{
			assertTrue(e.getMessage().startsWith("Not null constraint violation "));
		}
	}
	
	/**
	 * Makes sure saving two actions with the same URL will fail.
	 * @param templateAction
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	private void testSavingDuplicateAction(Action templateAction, AuthData authData, EnvData env) throws KommetException
	{
		String duplicateURL = "some/test/url/for/action";
		
		templateAction.setName("Action One");
		templateAction.setUrl(duplicateURL);
		templateAction.uninitializeId();
		
		templateAction = actionService.saveOnEnv(templateAction, null, authData, env);
		assertNotNull(templateAction.getId());
		
		// now try to save another action with the same URL
		templateAction.uninitializeId();
		
		try
		{
			actionService.saveOnEnv(templateAction, null, authData, env);
			fail("Saving two actions with the same URL should fail");
		}
		catch (ActionCreationException e)
		{
			// expected
			assertEquals(e.getErrCode(), ActionCreationException.ERR_CODE_DUPLICATE_REGISTERED_ACTION_URL);
			assertTrue(e.getMessage().equals("Another action with URL " + duplicateURL + " already exists"));
		}
		
		// now set the duplicate URL to be parametrized, and make sure it is detected as duplicated
		templateAction.setUrl("some/test/url/{param1}/action");
		try
		{
			actionService.saveOnEnv(templateAction, null, authData, env);
			fail("Saving two actions (one of them parametrized) with the same URL should fail");
		}
		catch (ActionCreationException e)
		{
			// expected
			assertEquals(e.getErrCode(), ActionCreationException.ERR_CODE_DUPLICATE_REGISTERED_ACTION_URL);
			assertTrue("Incorrect exception message: " + e.getMessage(), e.getMessage().startsWith("Another action with URL"));
		}
	}

	private void testCallingAction(Action action, EnvData env) throws KommetException
	{	
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(action.getUrl());
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		try
		{
			PageData pageData = (PageData)ActionUtil.callAction(action.getController().getQualifiedName(), action.getControllerMethod(), null, sysContextFactory.get(authData, env), request, resp, new PageData(env), compiler, dataService, sharingService, authData, env, appConfig);
			assertNotNull(pageData);
			assertNotNull(pageData.getValue("testKey"));
			assertEquals("testValue", pageData.getValue("testKey"));
		}
		catch (KommetException e)
		{
			e.getCause().printStackTrace();
			fail("Error calling page action: " + e.getMessage());
		}
	}

	private void testAddingActionToEnv(Action action, EnvData env) throws KommetException
	{
		env.addAction(action.getUrl().toUpperCase(), action);
		env.addAction(action.getUrl().toLowerCase(), action);
		
		// get the action by URL and make sure the URL is case insensitive
		Action foundAction = env.getActionForUrl(action.getUrl());
		assertNotNull(foundAction);
		assertEquals(action.getId(), foundAction.getId());
	}
	
	@Test
	public void testDeletePagedReferencedByStandardPage() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		// create pigeon type
		Type pigeonType = dataService.createType(dataHelper.getPigeonType(env), env);
		
		// create some controller and view
		Record pigeonListView = dataHelper.getPigeonListView(env);
		Record pigeonListKollFile = dataHelper.getPigeonListControllerClass(env);
		pigeonListView = dataService.save(pigeonListView, env);
		pigeonListKollFile = dataService.save(pigeonListKollFile, env);
		
		// create test list page
		ActionKType actionType = (ActionKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME));
		Record listActionRec = new Record(actionType);
		listActionRec.setField("url", "test/page/list");
		listActionRec.setField("isSystem", false);
		listActionRec.setField("isPublic", false);
		listActionRec.setField("view", pigeonListView);
		listActionRec.setField("name", "Page Name");
		listActionRec.setField("createdDate", new Date());
		listActionRec.setField("controller", pigeonListKollFile);
		listActionRec.setField("controllerMethod", "testMethodName");
		
		// save the page
		Action listAction = actionDao.save(new Action(listActionRec, env), dataHelper.getRootAuthData(env), env);
		
		KID adminProfileId = (KID)env.getRootUser().getField("profile.id");
		
		// make this new page a standard list page for type pigeon
		actionService.setStandardAction(pigeonType.getKID(), listAction, adminProfileId, StandardActionType.LIST, dataHelper.getRootAuthData(env), env);
		
		// now try to delete the page with option 'reassing' set to false
		OperationResult result = actionService.delete(listAction, false, dataHelper.getRootAuthData(env), env);
		assertFalse("The page should not have been deleted as it is referenced by standard actions", result.isResult());
		
		// make sure the page is still there
		Profile adminProfile = new Profile();
		adminProfile.setId(adminProfileId);
		StandardAction retrievedStdAction = actionService.getStandardListAction(pigeonType, adminProfile, env);
		assertNotNull(retrievedStdAction);
		assertEquals(listAction.getId(), retrievedStdAction.getAction().getId());
		
		TypeInfo pigeonTypeInfo = typeInfoService.getForType(pigeonType.getKID(), false, env);
		
		// now try to delete with the reassign option set to true
		result = actionService.delete(listAction, true, dataHelper.getRootAuthData(env), env);
		assertTrue(result.getMessage(), result.isResult());
		retrievedStdAction = actionService.getStandardListAction(pigeonType, adminProfile, env);
		// make sure the default list page for type pigeon is now assigned to this standard page
		assertEquals(pigeonTypeInfo.getDefaultListAction().getId(), retrievedStdAction.getAction().getId());
		
		// make sure the page has really been deleted
		assertNull(actionService.getAction(listAction.getId(), env));
	}

	@Test
	public void testSetStandardAction() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		AuthData authData = dataHelper.getRootAuthData(env);

		ActionKType actionType = (ActionKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME));
		
		Record pigeonListView = dataHelper.getPigeonListView(env);
		Record pigeonListKollFile = dataHelper.getPigeonListControllerClass(env);
		Type pigeonType = dataService.createType(dataHelper.getPigeonType(env), env);
		
		// make sure standard controller has been created for this type
		ClassFilter kollFilter = new ClassFilter();
		kollFilter.setQualifiedName(StandardTypeControllerUtil.getStandardControllerQualifiedName(pigeonType));
		List<Class> stdControllers = classService.getClasses(kollFilter, env);
		assertEquals("Expected to find exactlty one standard controller generated, instead found " + stdControllers.size(), 1, stdControllers.size());
		Class stdController = stdControllers.get(0);
		
		// there should exist a standard page for each action for the only existing profile - admin
		// there are 4 actions: create/edit, save, view and list
		assertEquals(4, actionService.getStandardActionsForType(pigeonType.getKID(), env).size());
		
		// save view and koll file
		pigeonListView = dataService.save(pigeonListView, env);
		pigeonListKollFile = dataService.save(pigeonListKollFile, env);
		
		// create test list page
		Record listActionRec = new Record(actionType);
		listActionRec.setField("url", "test/page/list");
		listActionRec.setField("isSystem", false);
		listActionRec.setField("isPublic", false);
		listActionRec.setField("view", pigeonListView);
		listActionRec.setField("name", "Page Name");
		listActionRec.setField("createdDate", new Date());
		listActionRec.setField("controller", pigeonListKollFile);
		listActionRec.setField("controllerMethod", "testMethodName");
		
		// save the page
		Action listAction = actionDao.save(new Action(listActionRec, env), dataHelper.getRootAuthData(env), env);
		
		// create test details page
		Record detailsPageRec = new Record(actionType);
		detailsPageRec.setField("url", "test/page/details");
		detailsPageRec.setField("isSystem", false);
		detailsPageRec.setField("isPublic", false);
		detailsPageRec.setField("view", pigeonListView);
		detailsPageRec.setField("name", "Page Name");
		detailsPageRec.setField("createdDate", new Date());
		detailsPageRec.setField("controller", pigeonListKollFile);
		detailsPageRec.setField("controllerMethod", "testMethodName");
		// save the page
		Action detailsAction = actionDao.save(new Action(detailsPageRec, env), dataHelper.getRootAuthData(env), env);
		
		// create test edit page
		Record editActionRec = new Record(actionType);
		editActionRec.setField("url", "test/page/edit");
		editActionRec.setField("isSystem", false);
		editActionRec.setField("isPublic", false);
		editActionRec.setField("view", pigeonListView);
		editActionRec.setField("name", "Page Name");
		editActionRec.setField("createdDate", new Date());
		// this time use standard controller
		editActionRec.setField("controller", RecordProxyUtil.generateRecord(stdController, env.getType(KeyPrefix.get(KID.CLASS_PREFIX)), 1, env));
		editActionRec.setField("controllerMethod", "edit");
		// save the action
		Action editAction = actionDao.save(new Action(editActionRec, env), dataHelper.getRootAuthData(env), env);
		
		KID adminProfileId = (KID)env.getRootUser().getField("profile.id");
		
		// set standard pages for the pigeon object
		StandardAction stdListAction = actionService.setStandardAction(pigeonType.getKID(), listAction, adminProfileId, StandardActionType.LIST, dataHelper.getRootAuthData(env), env);
		StandardAction stdViewPage = actionService.setStandardAction(pigeonType.getKID(), detailsAction, (KID)env.getRootUser().getField("profile.id"), StandardActionType.VIEW, dataHelper.getRootAuthData(env), env);
		StandardAction stdEditPage = actionService.setStandardAction(pigeonType.getKID(), editAction, (KID)env.getRootUser().getField("profile.id"), StandardActionType.VIEW, dataHelper.getRootAuthData(env), env);
		assertNotNull(stdListAction.getId());
		assertNotNull(stdViewPage.getId());
		
		// now get standard pages for this object
		StandardAction fetchedListAction = actionService.getStandardActionForTypeAndProfile(pigeonType.getKID(), adminProfileId, StandardActionType.LIST, env);
		assertNotNull(fetchedListAction);
		assertEquals(stdListAction.getId(), fetchedListAction.getId());
		assertNotNull(fetchedListAction.getProfile());
		assertNotNull(fetchedListAction.getTypeId());
		assertNotNull(fetchedListAction.getAction());
		assertEquals(pigeonType.getKID(), fetchedListAction.getTypeId());
		assertNotNull(fetchedListAction.getAction().getView());
		assertEquals(pigeonListView.getKID(), fetchedListAction.getAction().getView().getId());
		assertEquals(pigeonListView.getField("name"), fetchedListAction.getAction().getView().getName());
		assertNotNull(fetchedListAction.getAction().getController());
		
		// now create another profile
		Record newProfile = dataHelper.getTestProfile("NewProfile", env);
		newProfile = dataService.save(newProfile, env);
		// set standard page for this profile
		actionService.setStandardAction(pigeonType.getKID(), listAction, newProfile.getKID(), StandardActionType.LIST, dataHelper.getRootAuthData(env), env);
		
		// get all standard pages for all profiles
		List<StandardAction> pages = actionService.getStandardActionsForType(pigeonType.getKID(), env);
		assertEquals("There should exist 4 standard pages for profile 'System Administrator' and one page for 'New Profile'", 5, pages.size());
		
		// get standard details page
		// set standard page for this profile
		actionService.setStandardAction(pigeonType.getKID(), detailsAction, newProfile.getKID(), StandardActionType.VIEW, dataHelper.getRootAuthData(env), env);
		StandardAction stdDetailsAction = actionService.getStandardDetailsAction(pigeonType, (Profile)RecordProxyUtil.generateStandardTypeProxy(newProfile, env, compiler), env);
		
		// set listPage as standard details page
		actionService.setStandardAction(pigeonType.getKID(), listAction, newProfile.getKID(), StandardActionType.VIEW, dataHelper.getRootAuthData(env), env);
		stdDetailsAction = actionService.getStandardDetailsAction(pigeonType, (Profile)RecordProxyUtil.generateStandardTypeProxy(newProfile, env, compiler), env);
		
		assertNotNull(stdDetailsAction);
		assertNotNull(stdDetailsAction.getAction());
		// make sure listPage is the standard details page
		assertEquals(listAction.getId(), stdDetailsAction.getAction().getId());
		
		// now set standard action by action ID
		actionService.setStandardAction(pigeonType.getKID(), detailsAction, newProfile.getKID(), StandardActionType.VIEW, dataHelper.getRootAuthData(env), env);
		stdDetailsAction = actionService.getStandardDetailsAction(pigeonType, (Profile)RecordProxyUtil.generateStandardTypeProxy(newProfile, env, compiler), env);
		assertEquals(detailsAction.getId(), stdDetailsAction.getAction().getId());
		
		// now delete the type - we are testing deleting types with assigned standard pages because it failed at some point
		OperationResult result = dataService.deleteType(pigeonType, authData, env);
		assertFalse("Deleting type should fail because there are user-defined actions using its standard controller", result.isResult());
		
		// delete page that uses the standard controller
		actionService.deleteStandardAction(stdEditPage, env);
		actionService.deleteAction(editAction, env);
		
		// make sure now no actions are using the standard controller
		ActionFilter filter = new ActionFilter();
		filter.setControllerId(stdController.getId());
		filter.setIsSystem(false);
		List<Action> actionsUsingStandardController = actionService.getActions(filter, env);
		if (!actionsUsingStandardController.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			for (Action page : actionsUsingStandardController)
			{
				sb.append(page.getInterpretedName() + ", ");
			}
			fail("Although all non-system actions using standard controller should have been deleted, some of them still exist: " + sb.toString());
		}
		
		// try deleting again
		result = dataService.deleteType(pigeonType, authData, env);
		assertTrue("Could not delete type: " + result.getMessage(), result.isResult());
		
		List<String> stdActionsUrls = new ArrayList<String>();
		stdActionsUrls.add(stdDetailsAction.getAction().getUrl());
		TypeManipulationTest.assertTypeProperlyDeleted(pigeonType, env, actionService, typeInfoService, stdActionsUrls);
	}
}
