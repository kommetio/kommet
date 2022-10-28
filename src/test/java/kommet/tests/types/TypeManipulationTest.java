/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomikos.beans.PropertyException;
import com.atomikos.beans.PropertyUtils;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.basic.Action;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.Profile;
import kommet.basic.RecordProxy;
import kommet.basic.StandardAction;
import kommet.basic.TypeInfo;
import kommet.basic.UniqueCheck;
import kommet.basic.View;
import kommet.basic.actions.ActionService;
import kommet.basic.actions.StandardActionType;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.StandardObjectController;
import kommet.basic.keetle.StandardTypeControllerUtil;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.keetle.ViewService;
import kommet.basic.types.SystemTypes;
import kommet.dao.UniqueCheckFilter;
import kommet.dao.dal.DALSyntaxException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.NoSuchFieldException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeDefinitionException;
import kommet.data.TypeInfoFilter;
import kommet.data.TypeInfoService;
import kommet.data.UniqueCheckService;
import kommet.data.datatypes.TextDataType;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.koll.annotations.Controller;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class TypeManipulationTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	UniqueCheckService uniqueCheckService;
	
	@Inject
	ClassService classService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	TypeInfoService typeInfoService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	EnvService envService;
	
	private static final Logger log = LoggerFactory.getLogger(TypeManipulationTest.class);
	
	@Test
	public void testDeleteType() throws KommetException, ClassNotFoundException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		File ktlDir = new File(env.getKeetleDir(appConfig.getKeetleDir()));
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		
		// create the dir anew - the page create methods won't do this
		ktlDir.mkdir();
		
		Long initialTypePermissionCount = env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.TYPE_PERMISSION_API_NAME).count();
		Long initialFieldPermissionCount = env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.FIELD_PERMISSION_API_NAME).count();
		Long initialActionPermissionCount = env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.ACTION_PERMISSION_API_NAME).count();
		
		// create pigeon type
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		assertNotNull(pigeonType.getKID());
		assertNotNull(pigeonType.getId());
		assertNotNull(dataService.getType(pigeonType.getId(), env));
		
		// add some records
		for (int i = 0; i < 3; i++)
		{
			Record pigeon = new Record(pigeonType);
			pigeon.setField("name", "abc");
			pigeon.setField("age", 3);
			dataService.save(pigeon, dataHelper.getRootAuthData(env), env);
		}
		
		// create a list of URLs of standard actions
		List<String> stdActionUrls = new ArrayList<String>();
		List<StandardAction> stdActions = actionService.getStandardActionsForType(pigeonType.getKID(), env);
		Record adminProfile = profileService.getProfileRecordByName(Profile.ROOT_NAME, env);
		for (StandardAction stdAction : stdActions)
		{
			stdActionUrls.add(stdAction.getAction().getUrl());
			// assign some page permissions
			permissionService.setActionPermissionForProfile(adminProfile.getKID(), stdAction.getAction().getId(), true, dataHelper.getRootAuthData(env), env);
		}
		
		// add some permission assignments to type and its fields
		permissionService.setTypePermissionForProfile(adminProfile.getKID(), pigeonType.getKID(), true, false, false, false, true, false, false, dataHelper.getRootAuthData(env), env);
		permissionService.setFieldPermissionForProfile(adminProfile.getKID(), pigeonType.getField("name").getKID(), true, false, dataHelper.getRootAuthData(env), env);
		permissionService.setFieldPermissionForProfile(adminProfile.getKID(), pigeonType.getField("age").getKID(), true, false, dataHelper.getRootAuthData(env), env);
		
		// delete the type
		dataService.deleteType(pigeonType, dataHelper.getRootAuthData(env), env);
		
		// make sure all permissions have been deleted
		assertEquals(initialTypePermissionCount, env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.TYPE_PERMISSION_API_NAME).count());
		assertEquals(initialFieldPermissionCount, env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.FIELD_PERMISSION_API_NAME).count());
		assertEquals(initialActionPermissionCount, env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.ACTION_PERMISSION_API_NAME).count());
		
		// TODO should we also make sure that permissions have been deleted from user sessions?
		
		assertTypeProperlyDeleted(pigeonType, env, actionService, typeInfoService, stdActionUrls);
		
		// to be sure everything succeeded, create the type again
		//Type recreatedType = dataService.createType(dataHelper.getFullPigeonType(env), env.getRootUser().getKolmuId(), env);
		//assertNotNull(recreatedType.getKolmuId());
	}
	
	public static void assertTypeProperlyDeleted(Type type, EnvData env, ActionService pageService, TypeInfoService typeInfoService, Collection<String> stdPageUrls) throws KommetException
	{
		// make sure the type is deleted from the env
		assertNull(env.getType(type.getKeyPrefix()));
		
		// make sure type info record is deleted
		// get type info
		TypeInfoFilter tiFilter = new TypeInfoFilter();
		tiFilter.addTypeId(type.getKID());
		assertTrue(typeInfoService.find(tiFilter, env).isEmpty());
		
		// make sure standard pages have been removed for this type
		assertTrue(pageService.getStandardActionsForType(type.getKID(), env).isEmpty());
		
		// make sure pages have been removed from the env action mapping
		for (String url : stdPageUrls)
		{
			assertNull(env.getActionForUrl(url));
		}
		
		// make sure the DB table is deleted for the type
		try
		{
			env.getJdbcTemplate().execute("select id from " + type.getDbTable());
			fail("Querying table of a deleted type should fail");
		}
		catch (Exception e)
		{
			// expected
		}
		
		assertNull(env.getCustomTypeProxyMapping(type.getKID()));
	}
	
	@Test
	public void createTypeWithPackage() throws KommetException, ClassNotFoundException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		// first make sure that the env exists - if not, it means that the test shared database is misconfigured
		EnvData refreshedEnv = envService.get(env.getId());
		
		File ktlDir = new File(env.getKeetleDir(appConfig.getKeetleDir()));
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		
		// create the dir anew - the page create methods won't do this
		ktlDir.mkdir();
		
		// remove env info to read it again
		envService.resetEnv(env.getId());
		
		refreshedEnv = envService.get(env.getId());
		
		// create pigeon object
		Type pigeonType = dataHelper.getFullPigeonType(refreshedEnv);
		assertNull(env.getType(pigeonType.getQualifiedName()));
	}
	
	@Test
	public void createTypeWithDuplicateName() throws KommetException, ClassNotFoundException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		File ktlDir = new File(env.getKeetleDir(appConfig.getKeetleDir()));
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		
		// create the dir anew - the page create methods won't do this
		ktlDir.mkdir();
		
		// create pigeon object
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType.setUchLabel("label.pigeon.name");
		String correctAPIName = pigeonType.getApiName();
		// set invalid API name
		pigeonType.setApiName("aPigeon");
		assertNull(pigeonType.getDbTable());
		
		try
		{
			dataService.createType(pigeonType, env);
			fail("Saving type with illegal API name should fail because the name starts with a lower case letter");
		}
		catch (TypeDefinitionException e)
		{
			// expected exception
			assertTrue("Incorrect error message while saving type with illegal API name: " + e.getMessage(), e.getMessage().contains("API names must start with an uppercase letter and can contain only letters, digits and an underscore, and must not end with an underscore"));
		}
		
		// create another profile so that we can test how permissions and standard pages are set
		// for two profiles when a new type is created
		Profile secondProfile = new Profile();
		secondProfile.setName("SecondProfile");
		secondProfile.setLabel("SecondProfile");
		secondProfile.setSystemProfile(false);
		secondProfile = profileService.save(secondProfile, dataHelper.getRootAuthData(env), env);
		assertNotNull("Profile not created", secondProfile.getId());
		
		// restore correct API name
		pigeonType.setApiName(correctAPIName);
		// save again
		pigeonType = dataService.createType(pigeonType, env);
		assertNotNull(pigeonType.getKID());
		assertNotNull("Default field not set on newly created type", pigeonType.getDefaultField());
		assertEquals("New types should have ID field set as their default field, in this case the default field's ID is " + pigeonType.getDefaultField().getKID(), pigeonType.getField(Field.ID_FIELD_NAME).getKID(), pigeonType.getDefaultField().getKID());
		
		java.lang.Class<?> pigeonController = null;
		// make sure a controller has been created for this type, and it has the same package as the type
		try
		{
			pigeonController = compiler.getClass(StandardTypeControllerUtil.getStandardControllerQualifiedName(pigeonType), true, env);
		}
		catch (ClassNotFoundException e)
		{
			fail("Standard controller named " + StandardTypeControllerUtil.getStandardControllerQualifiedName(pigeonType) + " not generated or generated with wrong name for type " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME);
		}
		
		Class stdController = validateStandardController(pigeonController, env);
		
		// "Second Profile" is not the admin profile so make sure no standard pages are created for it
		assertNull(actionService.getStandardActionForTypeAndProfile(pigeonType.getKID(), secondProfile.getId(), StandardActionType.LIST, env));
		assertNull(actionService.getStandardActionForTypeAndProfile(pigeonType.getKID(), secondProfile.getId(), StandardActionType.CREATE, env));
		assertNull(actionService.getStandardActionForTypeAndProfile(pigeonType.getKID(), secondProfile.getId(), StandardActionType.EDIT, env));
		assertNull(actionService.getStandardActionForTypeAndProfile(pigeonType.getKID(), secondProfile.getId(), StandardActionType.VIEW, env));
		
		// verify that standard pages and views have been properly created
		testStandardActionsForType(pigeonType, stdController, env);
		
		// now create another profile and make sure that no standard pages are created for it
		// because we will use the default ones unless non-standard ones are defined
		Profile thirdProfile = new Profile();
		thirdProfile.setName("ThirdProfile");
		thirdProfile.setLabel("ThirdProfile");
		thirdProfile.setSystemProfile(false);
		thirdProfile = profileService.save(thirdProfile, dataHelper.getRootAuthData(env), env);
		assertNull(actionService.getStandardActionForTypeAndProfile(pigeonType.getKID(), thirdProfile.getId(), StandardActionType.LIST, env));
		assertNull(actionService.getStandardActionForTypeAndProfile(pigeonType.getKID(), thirdProfile.getId(), StandardActionType.CREATE, env));
		assertNull(actionService.getStandardActionForTypeAndProfile(pigeonType.getKID(), thirdProfile.getId(), StandardActionType.EDIT, env));
		assertNull(actionService.getStandardActionForTypeAndProfile(pigeonType.getKID(), thirdProfile.getId(), StandardActionType.VIEW, env));
		
		testCreateTypeWithInvalidSharingControlledByField(pigeonType, env);
		
		// make sure saving another object with the same API name fails
		try
		{
			dataService.createType(dataHelper.getPigeonType(env), env);
			fail("Saving another type with the same API name should fail");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.debug("Expected exception: " + e.getMessage());
		}
		
		// we cannot continue the test because the transaction is aborted.
	}
	
	private void testCreateTypeWithInvalidSharingControlledByField(Type pigeonType, EnvData env) throws KommetException
	{
		pigeonType.setSharingControlledByFieldId(pigeonType.getField("name").getKID());
		
		try
		{
			dataService.updateType(pigeonType, dataHelper.getRootAuthData(env), env);
			fail("Assigning invalid sharingControlledByField should throw an exception");
		}
		catch (TypeDefinitionException e)
		{
			assertTrue(e.getMessage().startsWith("Field name cannot be set as sharingControlledByField"));
		}
		
		// now try to assign field that does not exist on type
		pigeonType.setSharingControlledByFieldId(env.getType(KeyPrefix.get(KID.USER_PREFIX)).getField("userName").getKID()); 
		
		try
		{
			dataService.updateType(pigeonType, dataHelper.getRootAuthData(env), env);
			fail("Assigning invalid sharingControlledByField should throw an exception");
		}
		catch (TypeDefinitionException e)
		{
			assertTrue(e.getMessage().startsWith("Field cannot be set as sharingControlledByField"));
		}
		
		pigeonType.setSharingControlledByFieldId(pigeonType.getField("father").getKID()); 
		dataService.updateType(pigeonType, dataHelper.getRootAuthData(env), env);
		
		// create a new type
		Type boxType = new Type();
		boxType.setApiName("Box");
		boxType.setBasic(false);
		boxType.setLabel("Box");
		boxType.setPluralLabel("Boxes");
		boxType.setPackage("com.my.boxes");
		
		boxType.setSharingControlledByFieldId(env.getType(KeyPrefix.get(KID.USER_PREFIX)).getField("userName").getKID());
		try
		{
			dataService.createType(boxType, dataHelper.getRootAuthData(env), env);
			fail("Assigning invalid sharingControlledByField should throw an exception");
		}
		catch (TypeDefinitionException e)
		{
			assertTrue(e.getMessage().startsWith("Field cannot be set as sharingControlledByField"));
		}
	}

	/**
	 * Tests that standard pages and views have been correctly created for the type.
	 * @param pigeonType
	 * @param secondProfile
	 * @param env
	 * @throws KommetException 
	 */
	private void testStandardActionsForType(Type pigeonType, Class stdController, EnvData env) throws KommetException
	{	
		File ktlDir = new File(env.getKeetleDir(appConfig.getKeetleDir()));
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		
		// create the dir anew - the page create methods won't do this
		ktlDir.mkdir();
		
		// select standard pages for both profiles
		List<StandardAction> stdActions = actionService.getStandardActionsForType(pigeonType.getKID(), env);
		
		// there are two profiles (System Administrator and Second Profile), so there should exist 8
		// standard actions for the type
		assertEquals("There should exist 4 standard actions for the type, since standard actions are created only for System Administrator profiles. Actual number is " + stdActions.size(), 4, stdActions.size());
		
		View editView = null;
		View createView = null;
		View listView = null;
		View detailsView = null;
		
		// make sure pages for this object have been registered on the env
		Action envListAction = env.getActionForUrl("list/" + pigeonType.getKeyPrefix());
		assertNotNull(envListAction);
		Action envDetailsAction = env.getActionForUrl("view/" + pigeonType.getKeyPrefix());
		assertNotNull(envDetailsAction);
		Action envEditAction = env.getActionForUrl("edit/" + pigeonType.getKeyPrefix());
		assertNotNull(envEditAction);
		Action envCreateAction = env.getActionForUrl("new/" + pigeonType.getKeyPrefix());
		assertNotNull(envCreateAction);
		
		for (StandardAction action : stdActions)
		{
			assertEquals("Standard action for operation type " + action.getStandardPageType() + " does not use " + stdController.getName() + " as controller. Instead, it uses " + action.getAction().getController().getName(), stdController.getId(), action.getAction().getController().getId());
			
			assertNotNull("The view assigned to the action is null", action.getAction().getView());
			assertNotNull(action.getAction().getTypeId());
			assertNotNull(action.getAction().getType());
			assertEquals(pigeonType.getKID(), action.getAction().getType().getKID());
			assertTrue(action.getAction().getInterpretedName().endsWith(pigeonType.getLabel()));
			
			assertNotNull(action.getAction().getView().getTypeId());
			assertNotNull(action.getAction().getView().getType());
			assertNotNull(action.getAction().getView().getName());
			assertEquals(pigeonType.getKID(), action.getAction().getView().getType().getKID());
			assertTrue(action.getAction().getView().getInterpretedName().endsWith(pigeonType.getLabel()));
			
			assertTrue(action.getAction().getView().getIsSystem());
			assertTrue(action.getAction().getController().getIsSystem());
			
			// now we select the controller files using the "systemFile" option in the filter to make sure
			// the filter works
			ClassFilter kollFilter = new ClassFilter();
			kollFilter.setSystemFile(true);
			kollFilter.addId(action.getAction().getController().getId());
			List<Class> systemFiles = classService.getClasses(kollFilter, env);
			assertEquals(1, systemFiles.size());
			assertEquals(action.getAction().getController().getId(), systemFiles.get(0).getId());
			
			// now we select the views using the "systemFView" option in the filter to make sure
			// the filter works
			ViewFilter ktlFilter = new ViewFilter();
			ktlFilter.setSystemView(true);
			ktlFilter.setKID(action.getAction().getView().getId());
			List<View> systemViews = viewService.getViews(ktlFilter, env);
			assertEquals(1, systemViews.size());
			assertEquals(action.getAction().getView().getId(), systemViews.get(0).getId());
			
			// make sure for each standard page type a new view has been created for every type, but that
			// the same view is by default used for all profiles
			switch (action.getStandardPageType())
			{
				case VIEW:	assertEquals(envDetailsAction.getId(), action.getAction().getId());
					
							if (detailsView == null)
							{
								detailsView = action.getAction().getView();
							}
							else
							{
								assertEquals(detailsView.getId(), action.getAction().getView().getId());
							}
							break;
							
				case LIST:	assertEquals(envListAction.getId(), action.getAction().getId());
					
							if (listView == null)
							{
								listView = action.getAction().getView();
							}
							else
							{
								assertEquals(listView.getId(), action.getAction().getView().getId());
							}
							break;
							
				case EDIT:	assertEquals(envEditAction.getId(), action.getAction().getId());
					
							if (editView == null)
							{
								editView = action.getAction().getView();
							}
							else
							{
								assertEquals(editView.getId(), action.getAction().getView().getId());
							}
							break;
							
				case CREATE: assertEquals(envCreateAction.getId(), action.getAction().getId());
				
							if (createView == null)
							{
								createView = action.getAction().getView();
							}
							else
							{
								assertEquals(createView.getId(), action.getAction().getView().getId());
							}
							break;
			}
		}
		
		assertNotNull("Details view not found", detailsView);
		assertNotNull("List view not found", listView);
		assertNotNull("Edit view not found", editView);
		assertNotNull("Create view not found", createView);
		
		// query full view info
		detailsView = viewService.getView(detailsView.getId(), env);
		editView = viewService.getView(editView.getId(), env);
		createView = viewService.getView(createView.getId(), env);
		listView = viewService.getView(listView.getId(), env);
		
		// make sure the view contains the KTL message tag
		assertTrue("Incorrect view code: " + detailsView.getKeetleCode(), detailsView.getKeetleCode().contains("<km:view"));
		assertTrue("Incorrect view code: " + listView.getKeetleCode(), listView.getKeetleCode().contains("<km:view"));
		assertTrue("Incorrect view code: " + editView.getKeetleCode(), editView.getKeetleCode().contains("<km:view"));
		assertTrue("Incorrect view code: " + createView.getKeetleCode(), createView.getKeetleCode().contains("<km:view"));
		//assertTrue(detailsView.getCode().contains("<km:errors />"));
		//assertTrue(listView.getCode().contains("<km:errors />"));
		//assertTrue(editView.getCode().contains("<km:errors />"));
		//assertTrue(createView.getCode().contains("<km:errors />"));
		// make sure the view contains the default stylesheet
		//assertTrue(detailsView.getCode().contains("<link href=\"${pageContext.request.contextPath}/resources/layout.css\" rel=\"stylesheet\" type=\"text/css\" />"));
		//assertTrue(listView.getCode().contains("<link href=\"${pageContext.request.contextPath}/resources/layout.css\" rel=\"stylesheet\" type=\"text/css\" />"));
		//assertTrue(editView.getCode().contains("<link href=\"${pageContext.request.contextPath}/resources/layout.css\" rel=\"stylesheet\" type=\"text/css\" />"));
		//assertTrue(createView.getCode().contains("<link href=\"${pageContext.request.contextPath}/resources/layout.css\" rel=\"stylesheet\" type=\"text/css\" />"));
		
		//String jQueryInclude = "<script type=\"text/javascript\" src=\"${pageContext.request.contextPath}/resources/js/jquery-1.9.1.js\"></script>";
		//assertTrue(detailsView.getCode().contains(jQueryInclude));
		//assertTrue(listView.getCode().contains(jQueryInclude));
		//assertTrue(editView.getCode().contains(jQueryInclude));
		//assertTrue(createView.getCode().contains(jQueryInclude));
		
		// assert view properties
		assertNotNull(detailsView.getCreatedBy());
		assertNotNull(listView.getCreatedBy());
		assertNotNull(editView.getCreatedBy());
		assertNotNull(createView.getCreatedBy());
		assertEquals(env.getRootUser().getKID(), detailsView.getCreatedBy().getId());
		assertEquals(env.getRootUser().getKID(), listView.getCreatedBy().getId());
		assertEquals(env.getRootUser().getKID(), editView.getCreatedBy().getId());
		assertEquals(env.getRootUser().getKID(), createView.getCreatedBy().getId());
		assertEquals(env.getRootUser().getKID(), detailsView.getLastModifiedBy().getId());
		assertEquals(env.getRootUser().getKID(), listView.getLastModifiedBy().getId());
		assertEquals(env.getRootUser().getKID(), editView.getLastModifiedBy().getId());
		assertEquals(env.getRootUser().getKID(), createView.getLastModifiedBy().getId());
		
		assertTrue(listView.getKeetleCode().contains("<km:dataTable"));
		assertTrue(listView.getKeetleCode().contains("title=\"{{pluralLabel}}\""));
		assertTrue(editView.getKeetleCode().contains("<km:objectDetails"));
		assertTrue(createView.getKeetleCode().contains("<km:objectDetails"));
		assertTrue(editView.getKeetleCode().contains("failOnUninitializedFields=\"false\""));
		assertTrue(createView.getKeetleCode().contains("failOnUninitializedFields=\"false\""));
		assertTrue(editView.getKeetleCode().contains("mode=\"edit\""));
		assertTrue(createView.getKeetleCode().contains("mode=\"edit\""));
		assertTrue(detailsView.getKeetleCode().contains("<km:objectDetails"));
		
		// make sure views are stored on disk
		// note: this test does not always make sense because these views can altready exist from previous test runs
		// TODO clear keetle dir before this test
		File file = new File(env.getKeetleDir(appConfig.getKeetleDir()) + "/" + detailsView.getId() + ".jsp");
		assertTrue(file.exists());
		file = new File(env.getKeetleDir(appConfig.getKeetleDir()) + "/" + editView.getId() + ".jsp");
		assertTrue(file.exists());
		file = new File(env.getKeetleDir(appConfig.getKeetleDir()) + "/" + listView.getId() + ".jsp");
		assertTrue(file.exists());
		file = new File(env.getKeetleDir(appConfig.getKeetleDir()) + "/" + createView.getId() + ".jsp");
		assertTrue(file.exists());
		
		// additionally, make sure a save action has been created for this type
		Action saveAction = env.getActionForUrl("save/" + pigeonType.getKeyPrefix());
		assertNotNull("Save page(action) not created for newly created type", saveAction);
		assertNotNull(saveAction.getView());
		assertEquals(editView.getId(), saveAction.getView().getId());
		assertNotNull(saveAction.getController());
		assertEquals("save", saveAction.getControllerMethod());
		
		// make sure type info object has been created
		TypeInfoFilter filter = new TypeInfoFilter();
		filter.addTypeId(pigeonType.getKID());
		List<TypeInfo> typeInfos = typeInfoService.find(filter, env);
		assertEquals(1, typeInfos.size());
		TypeInfo typeInfo = typeInfos.get(0);
		assertEquals(envEditAction.getId(), typeInfo.getDefaultEditAction().getId());
		assertEquals(envDetailsAction.getId(), typeInfo.getDefaultDetailsAction().getId());
		assertEquals(envListAction.getId(), typeInfo.getDefaultListAction().getId());
		assertEquals(envCreateAction.getId(), typeInfo.getDefaultCreateAction().getId());
		assertEquals(saveAction.getId(), typeInfo.getDefaultSaveAction().getId());
		assertEquals(saveAction.getController().getId(), typeInfo.getStandardController().getId());
		assertEquals(5, typeInfo.getDefaultActions().size());
		
		// make sure type and interpreted name are set on default pages/view retrieved in the type info
		for (Action defaultAction : typeInfo.getDefaultActions())
		{
			assertNotNull(defaultAction.getType());
			assertNotNull(defaultAction.getInterpretedName());
			assertNotNull(defaultAction.getView().getType());
			assertNotNull(defaultAction.getView().getInterpretedName());
		}
	}

	private Class validateStandardController(java.lang.Class<?> controller, EnvData env) throws KommetException, ClassNotFoundException
	{
		// make sure the standard controller is annotated with @Controller
		assertTrue(controller.isAnnotationPresent(Controller.class));
		
		// make sure standard controller extends StandardObjectController
		assertTrue(StandardObjectController.class.isAssignableFrom(controller));
		// make sure standard controller extends BaseController
		assertTrue(BaseController.class.isAssignableFrom(controller));
	
		try
		{
			// make sure save method exists and is annotated with @Action 
			Method saveMethod = controller.getMethod("save", String.class);
			assertTrue(saveMethod.isAnnotationPresent(kommet.koll.annotations.Action.class));
			assertTrue(saveMethod.getReturnType().equals(PageData.class));
			
			// make sure create method exists and is annotated with @Action 
			Method createMethod = controller.getMethod("create");
			assertTrue(createMethod.isAnnotationPresent(kommet.koll.annotations.Action.class));
			assertTrue(createMethod.getReturnType().equals(PageData.class));
			
			// make sure edit method exists and is annotated with @Action 
			Method editMethod = controller.getMethod("edit", String.class);
			assertTrue(editMethod.isAnnotationPresent(kommet.koll.annotations.Action.class));
			assertTrue(editMethod.getReturnType().equals(PageData.class));
			
			// make sure details method exists and is annotated with @Action 
			Method detailsMethod = controller.getMethod("details", String.class);
			assertTrue(detailsMethod.isAnnotationPresent(kommet.koll.annotations.Action.class));
			assertTrue(detailsMethod.getReturnType().equals(PageData.class));
			
			// make sure delete method exists and is annotated with @Action 
			Method deleteMethod = controller.getMethod("delete", String.class);
			assertTrue(deleteMethod.isAnnotationPresent(kommet.koll.annotations.Action.class));
			assertTrue(deleteMethod.getReturnType().equals(PageData.class));
		}
		catch (SecurityException e)
		{
			fail("Method cannot be accessed on standard controller " + controller.getName() + ": " + e.getMessage());
		}
		catch (NoSuchMethodException e)
		{
			fail("Method does not exist on standard controller " + controller.getName() + ": " + e.getMessage());
		}
		
		// make sure a class file for the standard controller has been stored in the DB
		Class stdControllerFile = classService.getClass(MiscUtils.envToUserPackage(controller.getName(), env), env);
		assertNotNull(stdControllerFile);
		assertEquals(controller.getSimpleName(), stdControllerFile.getName());
		assertEquals(MiscUtils.envToUserPackage(controller.getPackage().getName(), env), stdControllerFile.getPackageName());
		assertNotNull(stdControllerFile.getCreatedBy());
		assertEquals(env.getRootUser().getKID(), stdControllerFile.getCreatedBy().getId());
		
		return stdControllerFile;
	}
	
	/**
	 * Makes sure that a different instance of type and its fields is returned by the createType method, and that
	 * the original instance is not modified.
	 * @param originalType
	 * @param savedType
	 */
	private void assertInstanceModification (Type originalType, Type savedType)
	{
		// make sure the returned type has all properties set
		assertNull(originalType.getId());
		assertNull(originalType.getKID());
		assertNull(originalType.getDbTable());
		assertNull(originalType.getCreated());
		
		// make sure original field instances have not been modified
		for (Field field : originalType.getFields())
		{
			assertNull(field.getKID());
			assertNull(field.getId());
			assertNull(field.getCreated());
			assertNull(field.getDbColumn());
			// make sure instances are the same
			assertTrue(field.getType() == originalType);
		}
		
		// make sure the returned type has all properties set
		assertNotNull(savedType.getId());
		assertNotNull(savedType.getKID());
		assertNotNull(savedType.getDbTable());
		assertNotNull(savedType.getCreated());
		
		// make sure original field instances have not been modified
		for (Field field : savedType.getFields())
		{
			assertNotNull(field.getKID());
			assertNotNull(field.getId());
			assertNotNull(field.getCreated());
			assertNotNull(field.getDbColumn());
			// make sure instances are the same
			assertTrue(field.getType() == savedType);
		}
	}

	@Test
	public void createUpdateType() throws KommetException, ClassNotFoundException, InstantiationException, IllegalAccessException, PropertyException, SecurityException, NoSuchMethodException, InvocationTargetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		File ktlDir = new File(env.getKeetleDir(appConfig.getKeetleDir()));
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		
		// create the dir anew - the page create methods won't do this
		ktlDir.mkdir();
		
		// create pigeon object
		Type newPigeonType = dataHelper.getFullPigeonType(env);
		
		String typeDesc = "Type desc\n\nAAA&236&*& 902";
		newPigeonType.setDescription(typeDesc);
		Type pigeonType = dataService.createType(newPigeonType, env);
		
		assertEquals(typeDesc, dataService.getType(pigeonType.getKID(), env).getDescription());
		assertFalse("The instance returned by createType should not be the same as the one passed to the method", pigeonType == newPigeonType);
		
		assertInstanceModification(newPigeonType, pigeonType);
		
		// a copy is required for the updateType method
		pigeonType = MiscUtils.cloneType(pigeonType);
		
		assertNotNull(pigeonType.getId());
		
		// change the type's package and make sure we can save an object with the same API name but different
		// package names
		Type diffPackagePigeonType = dataHelper.getPigeonType(env);
		diffPackagePigeonType.setPackage("some.other.packagename");
		
		int initialKollFileCount = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.CLASS_API_NAME).list().size();
		
		diffPackagePigeonType = dataService.createType(diffPackagePigeonType, env);
		
		// Make sure exactly one Koll file have been created while saving a type - a standard controller.
		// Note: proxy files are compiled, but not stored in the DB, because we never modify them nor provide access to them.
		assertEquals(initialKollFileCount + 1, env.getSelectCriteriaFromDAL("select id from " + SystemTypes.CLASS_API_NAME).list().size());
		
		// get both types from the environment
		assertNotNull(env.getType(pigeonType.getQualifiedName()));
		assertNotNull(env.getType(diffPackagePigeonType.getQualifiedName()));
		assertNotNull(env.getType(pigeonType.getKID()));
		assertNotNull(env.getType(pigeonType.getKeyPrefix()));
		
		// make sure object proxies have been created for the type
		java.lang.Class<?> pigeonProxyClass = compiler.getClass(pigeonType.getQualifiedName(), true, env);
		assertNotNull("Proxy class not generated", pigeonProxyClass);
		
		// make sure object proxy KOLL file is stored in the DB
		Class proxyKollFile = classService.getClass(pigeonProxyClass.getName(), env);
		assertNull("Proxy KOLL file has been stored in the database, but it should not have been", proxyKollFile);
		
		RecordProxy proxyPigeon = (RecordProxy)pigeonProxyClass.newInstance();
		PropertyUtils.setProperty(proxyPigeon, "age", 10);
		
		try
		{
			pigeonProxyClass.getMethod("getEyeColour");
			fail("Method getEyeColour should not exist on object");
		}
		catch (NoSuchMethodException e)
		{
			// expected
		}
		
		// add property eyeColour to the object and make sure it is not recompiled
		Field eyeField = new Field();
		eyeField.setApiName("eyeColour");
		eyeField.setLabel("Eye Colour");
		eyeField.setDataType(new TextDataType(30));
		pigeonType.addField(eyeField);
		
		assertNotNull(eyeField.getType());
		assertNotNull(eyeField.getType().getKID());
		assertEquals(pigeonType.getKID(), eyeField.getType().getKID());
		
		Date originalCreateDate = new Date(pigeonType.getCreated().getTime());
		
		assertEquals(typeDesc, dataService.getType(pigeonType.getKID(), env).getDescription());
		
		pigeonType.setDescription(typeDesc + "abc");
		
		// fetch type and field again to obtain their saved version
		Type updatedPigeonType = dataService.updateType(pigeonType, dataHelper.getRootAuthData(env), env);
		
		assertTrue(updatedPigeonType == pigeonType);
		assertNotNull(pigeonType.getKID());
		assertEquals(typeDesc + "abc", dataService.getType(pigeonType.getKID(), env).getDescription());
		
		testUpdateTypeWithoutFields(updatedPigeonType, dataHelper.getRootAuthData(env), env);
		
		assertTrue(originalCreateDate.compareTo(dataService.getType(pigeonType.getKID(), env).getCreated()) == 0);
		
		pigeonProxyClass = compiler.getClass(pigeonType.getQualifiedName(), true, env);
		proxyPigeon = (RecordProxy)pigeonProxyClass.newInstance();
		try
		{
			pigeonProxyClass.getMethod("getEyeColour");
			fail("Method getEyeColour should not exist on object. Type should not be updated with new fields.");
		}
		catch (NoSuchMethodException e)
		{
			// expected
		}
		
		eyeField = dataService.createField(eyeField, env);
		assertNotNull(eyeField.getKID());
		assertNotNull(eyeField.getId());
		
		// make sure the type definition on the env has been updated with the new field
		assertNotNull("Type not updated with new field on env", env.getType(pigeonType.getKID()).getField("eyeColour"));
		
		// make sure a record with the new property can be inserted
		Record youngPigeon = new Record(pigeonType);
		youngPigeon.setField("name", "Arek");
		youngPigeon.setField("age", 10);
		youngPigeon.setField("eyeColour", "brown");
		youngPigeon = dataService.save(youngPigeon, dataHelper.getRootAuthData(env), env);
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select eyeColour from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + youngPigeon.getKID() + "'").list();
		
		assertNotNull(youngPigeon.getKID());
		assertEquals("brown",  pigeons.get(0).getField("eyeColour"));
		
		pigeonProxyClass = compiler.getClass(pigeonType.getQualifiedName(), true, env);
		proxyPigeon = (RecordProxy)pigeonProxyClass.newInstance();
		try
		{
			assertNotNull(pigeonProxyClass.getMethod("getEyeColour"));
		}
		catch (NoSuchMethodException e)
		{
			fail("Method 'getEyeColour' not found on proxy. Probably proxy has not been recompiled after the type's definition changed.");
		}
		
		// make the pigeonType object the one from env, not the previously created copy
		pigeonType = env.getType(pigeonType.getKID());
		
		testDeleteField (pigeonType, eyeField, youngPigeon, dataHelper.getRootAuthData(env), env);
		testRenameField(pigeonType, youngPigeon, env);
		testRenameType(pigeonType, env);
		testChangeTypeLabelAndDefaultField(pigeonType, env);
	}

	private void testUpdateTypeWithoutFields(Type type, AuthData authData, EnvData env) throws KommetException
	{
		int initialFieldCount = type.getFields().size();
		
		// get type from env without fields initialized
		Type typeWithoutFields = dataService.getTypeByName(type.getQualifiedName(), false, env);
		typeWithoutFields.setDescription("New description");
		dataService.updateType(typeWithoutFields, authData, env);
		
		// get type anew and make sure its fields have not been deleted
		Type refetchedType = env.getType(type.getKID());
		assertEquals("Updating type deleted some of its fields", initialFieldCount, refetchedType.getFields().size());
	}

	private void testChangeTypeLabelAndDefaultField(Type pigeonType, EnvData env) throws KommetException
	{	
		pigeonType.setLabel("NewLabel");
		assertNotNull(pigeonType.getDefaultFieldId());
		assertNotNull("Field not found by its ID - probably an issue with cloning type definition while getting it from the env", pigeonType.getField(pigeonType.getField("name").getKID()));
		pigeonType.setDefaultFieldId(pigeonType.getField("name").getKID());
		dataService.updateType(pigeonType, dataHelper.getRootAuthData(env), env);
		
		Type updatedType = env.getType(pigeonType.getKID());
		assertEquals("NewLabel", updatedType.getLabel());
		assertEquals(pigeonType.getField("name").getKID(), updatedType.getDefaultFieldId());
	}

	private void testRenameType(Type pigeonType, EnvData env) throws KommetException
	{
		String oldQualifiedName = pigeonType.getQualifiedName();
		// do some test query
		env.getSelectCriteriaFromDAL("select id from " + oldQualifiedName);
		
		// now change type API name
		pigeonType.setApiName("UpdatedPigeon");
		dataService.updateType(pigeonType, dataHelper.getRootAuthData(env), env);
		
		assertNull(env.getType(oldQualifiedName));
		assertNotNull(env.getType(pigeonType.getQualifiedName()));
		
		// make sure pigeons can no longer be queried by their old type name
		try
		{
			env.getSelectCriteriaFromDAL("select id from " + oldQualifiedName);
			fail("Query type by old type '" + oldQualifiedName + "' name should fail");
		}
		catch (DALSyntaxException e)
		{
			// expected
		}
		
		// query by new name
		env.getSelectCriteriaFromDAL("select id from " + pigeonType.getQualifiedName() + " limit 1");
		assertTrue(!oldQualifiedName.equals(pigeonType.getQualifiedName()));
	}

	private void testRenameField(Type pigeonType, Record pigeonRecord, EnvData env) throws KommetException
	{
		Integer pigeonAge = (Integer)pigeonRecord.getField("age");
		Field ageField = env.getFieldForUpdate(pigeonType.getKeyPrefix(), "age");
		if (ageField == null)
		{
			throw new KommetException("Test is not properly configured. Age field should exist on object " + pigeonType.getApiName());
		}
		
		// rename field
		ageField.setApiName("newAge");
		dataService.updateField(ageField, AuthData.getRootAuthData(env), env);
		
		// make sure the field has been updated on the env
		assertNotNull(env.getType(pigeonType.getKID()).getField("newAge"));
		
		// make sure the new field can be queried and returns the same results as the old one
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select newAge from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + pigeonRecord.getKID() + "'").list();
		assertEquals(1, pigeons.size());
		assertEquals(pigeonAge, pigeons.get(0).getField("newAge"));
		
		// make sure the age field no longer exists on the type
		assertNull("Age field should not be accessible after is has been deleted", env.getType(pigeonType.getKID()).getField("age"));
		
	}

	/**
	 * Test deleting a field.
	 * @param pigeonType
	 * @param eyeField
	 * @param existingPigeon 
	 * @param env
	 * @throws KommetException 
	 */
	private void testDeleteField(Type pigeonType, Field eyeField, Record existingPigeon, AuthData authData, EnvData env) throws KommetException
	{
		UniqueCheckFilter filter = new UniqueCheckFilter();
		filter.addTypeId(pigeonType.getKID());
		Integer initialUniqueCheckCount = uniqueCheckService.find(filter, env, this.dataService).size();
		
		// add a unique check for the field
		UniqueCheck check = new UniqueCheck();
		check.setName("SomeCheck");
		check.setDbName(UniqueCheck.generateDbName(pigeonType.getKID(), env));
		check.setTypeId(pigeonType.getKID());
		check.setFieldIds(pigeonType.getField(eyeField.getApiName()).getKID().getId());
		check.setIsSystem(false);
		uniqueCheckService.save(check, dataService.getRootAuthData(env), env);
		
		// add a unique check for the field and another
		UniqueCheck check2 = new UniqueCheck();
		check2.setName("SomeCheck2");
		check2.setDbName(UniqueCheck.generateDbName(pigeonType.getKID(), env));
		check2.setTypeId(pigeonType.getKID());
		check2.setFieldIds(pigeonType.getField(eyeField.getApiName()).getKID().getId() + ";" + pigeonType.getField("age").getKID().getId());
		check2.setIsSystem(false);
		uniqueCheckService.save(check2, dataService.getRootAuthData(env), env);
		
		// make sure two unique checks exist for the type
		List<UniqueCheck> uniqueChecks = uniqueCheckService.find(filter, env, this.dataService);
		assertEquals(initialUniqueCheckCount + 2, uniqueChecks.size());
		
		// delete the field
		dataService.deleteField(eyeField, authData, env);
		
		// make sure the type definition has been updated on the environment
		Type updatedPigeoType = env.getType(pigeonType.getKID());
		assertNull("Type definition not updated after deleting a field", updatedPigeoType.getField(eyeField.getApiName()));
		
		// make sure you cannot use the field in DAL
		try
		{
			env.getSelectCriteriaFromDAL("select " + eyeField.getApiName() + " from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME);
			fail("Querying deleted field " + eyeField.getApiName() + " should fail");
		}
		catch (NoSuchFieldException e)
		{
			// expected
			assertEquals(e.getMessage(), "Field " + eyeField.getApiName() + " not found on type " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME);
		}
		
		// TODO 
		// Implement updates of types on existing records when fields are deleted.
		// Right now when a field is deleted, all existing records can still have references to it.
		// Type of a record is updated properly, but the getField method does not check the type
		// definition for changes, it just looks up fields on the record.
		// ...
		// make sure the type definition has been updated on previously created records as well
		// assertNull("Field " + eyeField.getApiName() + " should not exist on object of type " + pigeonType.getApiName() + " after it has been deleted", existingPigeon.attemptGetField(eyeField.getApiName()));
		
		// make sure the unique check that existed on this field has been removed
		assertEquals("Unique checks not deleted after a field they referenced has been deleted", initialUniqueCheckCount, (Integer)uniqueCheckService.find(filter, env, this.dataService).size());
		
		// now add the field anew
		eyeField.setKID(null);
		eyeField.setDbColumn(null);
		dataService.createField(eyeField, authData, env);
	}
}
