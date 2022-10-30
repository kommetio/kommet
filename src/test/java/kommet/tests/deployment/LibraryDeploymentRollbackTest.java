/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Rollback;

import kommet.auth.AuthData;
import kommet.basic.Action;
import kommet.basic.App;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.Library;
import kommet.basic.LibraryItem;
import kommet.basic.UserGroup;
import kommet.basic.actions.ActionService;
import kommet.basic.types.ActionKType;
import kommet.basic.types.SystemTypes;
import kommet.dao.UserGroupFilter;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.deployment.Deployable;
import kommet.deployment.DeploymentConfig;
import kommet.deployment.DeploymentService;
import kommet.deployment.OverwriteHandling;
import kommet.deployment.PackageDeploymentStatus;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.AppFilter;
import kommet.koll.ClassService;
import kommet.services.AppService;
import kommet.services.LibraryService;
import kommet.services.UserGroupService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.MiscUtils;

/**
 * This is a complicated test class whose goal is to check that when package deployment fails, all changes are reverted. Since deployment methods use Propagation.REQUIRES_NEW,
 * they cannot use a database configured in the current transaction, because such changes have not been committed yet. This is why this test class has
 * to use a pre-configured env database.
 * 
 * The methods have to be executed in specific order, so annotation @FixMethodOrder is used.
 * 
 * Note that if this test fails, the env004 database if dirtied - it contains one created app. It has to be cleaned by removing the app manually.
 * 
 * This unit test also requires a non-zero setting of max_prepared_transactions in Postgres configuration.
 * 
 * @author Radek Krawiec
 * @since 30/03/2016
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LibraryDeploymentRollbackTest extends BaseUnitTest
{
	private static final String TEST_ACTION_NAME = "com.lib.actions.TestAction";

	private static final String MANUALLY_CREATE_APP_NAME_1 = "kommet.ManuallyCreatedApp";
	private static final String DUPLICATE_APP_LABEL = "Some app";

	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	LibraryService libService;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppService appService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	DeploymentService deploymentService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ClassService classService;
	
	@Inject
	UserGroupService ugService;
	
	// stores the prepared deployment package
	private static byte[] libPackage;
	
	// initial action count on the dest env
	private static Integer initialActionCount;
	
	private static boolean isPackagePrepared = false;
	private static int packageDeploymentCounter = 0;
	
	private static String destEnvName;
	private static String destEnvId;
	private static EnvData destEnv;
	
	@Test
	@Rollback(false)
	public void testMethod1PrepareDestEnv() throws KommetException, IOException
	{	
		if (destEnv == null)
		{
			destEnvName = "a" + MiscUtils.getHash(10);
			Random rand = new Random();
			destEnvId = "0010000001" + MiscUtils.padLeft(String.valueOf(rand.nextInt(1000)), 3, '0');
			destEnv = envService.createEnv(destEnvName, KID.get(destEnvId), true);
			
			for (Type type : destEnv.getAllTypes())
			{
				// make sure unique checks are initialized on types
				assertNotNull("No unique checks on type " + type.getQualifiedName(), type.getUniqueChecks());
				assertTrue("No unique checks on type " + type.getQualifiedName(), !type.getUniqueChecks().isEmpty());
			}

		}
	}
	
	@Test
	@Rollback(true)
	public void testMethod2PreparePackage() throws KommetException, IOException
	{
		// build test environment that will be the source for the deployment package
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create library
		Library lib = new Library();
		lib.setName("label library");	
		lib.setName("kommet.libs.labels");
		lib.setVersion("0.1");
		lib.setStatus("Not installed");
		lib.setDescription("Test lib");
		lib.setSource("External (manual deployment)");
		lib.setAccessLevel("Editable");
		lib.setIsEnabled(true);
		
		List<Deployable> deployableItems = new ArrayList<Deployable>();
		
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		// make sure the env contains no apps
		assertEquals(0, appService.find(null, env).size());
	
		lib = addUserGroupToLibrary(lib, deployableItems, authData, env);
		// create a test app and add it to the library
		lib = addAppToLibrary(lib, deployableItems, authData, env);
		// create a test action and add it to the library
		lib = addActionToLibrary(lib, pigeonType, deployableItems, authData, env);
		// create some test classes
		lib = addClassesToLibrary(lib, deployableItems, authData, env);
		
		// now prepare the package and store it in the class variable so that its available for other methods
		libPackage = libService.createLibraryPackage(lib, authData, env);
		isPackagePrepared = true;
	}
	
	/**
	 * This method creates a few records that simulate records existing in the env before deployment. We want to have such records to make sure
	 * that when deployment is rolled back, any records that existed on the env before the deployment will still be there.
	 * 
	 * This has to be done in a separate method - not the same as the one that calls the deployment. Previously we tried something like this:
	 * 
	 * - new method (transaction: required)
	 * -- created permanent records
	 * -- perform deployment (transaction: requires_new)
	 * --- if here modification is made to any of the permanent records, locks creates on these records does not allow for such modification and the whole method freezes
	 * -- exit deployment and go back to the enclosing transaction
	 * 
	 * @throws Exception
	 */
	@Test
	@Rollback(false)
	public void testMethod3CreatePermanentData() throws Exception
	{
		AuthData authData = dataHelper.getRootAuthData(destEnv);
		
		// make sure the env contains no apps if its the first run of the method, otherwise expect one app
		assertEquals(packageDeploymentCounter == 0 ? 0 : 1, appService.find(new AppFilter(), authData, destEnv).size());
		initialActionCount = actionService.getActions(null, destEnv).size();
		
		App manuallyCreatedApp1 = new App();
		manuallyCreatedApp1.setType("Internal app");
		manuallyCreatedApp1.setName(MANUALLY_CREATE_APP_NAME_1);
		manuallyCreatedApp1.setLabel(DUPLICATE_APP_LABEL);
		manuallyCreatedApp1 = appService.save(manuallyCreatedApp1, authData, destEnv);
		assertNotNull(manuallyCreatedApp1.getId());
		
		UserGroup group1 = new UserGroup();
		group1.setName("com.rm.InternalGroup");
		ugService.save(group1, authData, destEnv);
	}

	/**
	 * Try deploying the prepared package. Rollback is turned off for this method, so that we are sure that if any changes would be made by the package
	 * deployment, they would be possible to detect in the subsequent method "testMethod3CheckRollback". However, if the test is successful, no such changes
	 * should be detected because the deployment method {@link DeploymentService.deployZip} should rollback on deployment error.
	 * @throws Exception
	 */
	@Test
	@Repeat(2)
	@Rollback(false)
	public void testMethod4LibraryPackageCreation() throws Exception
	{	
		// first make sure the previous test method completed succesfully
		assertTrue("Test method preparing deployment package did not complete successfully, so the test scenario cannot be continued", isPackagePrepared);
		assertNotNull(libPackage);
		
		// Create destination env for the deployment.
		// For this purpose we will use an existing preconfigured env. We cannot configure this env here, because the deployment method is executed using
		// Propagation.REQUIRES_NEW	and would not see the data inserted in this test method, had it not been created before.
		//EnvData destEnv = dataHelper.getTestEnv3Data(false);
		//destEnv = envService.get(destEnv.getId());
		AuthData authData = dataHelper.getRootAuthData(destEnv);
		
		// make sure the destination env contains action type just to make sure it is properly configured
		assertNotNull(destEnv.getType(KeyPrefix.get(KID.ACTION_PREFIX)));
		
		// create app on the destination env using regular service, not via deployment
		// just to make sure later that this action does exist
		/*if (packageDeploymentCounter == 0)
		{
			App manuallyCreatedApp1 = new App();
			manuallyCreatedApp1.setType("Internal app");
			manuallyCreatedApp1.setName(MANUALLY_CREATE_APP_NAME_1);
			manuallyCreatedApp1.setLabel(DUPLICATE_APP_LABEL);
			manuallyCreatedApp1 = appService.save(manuallyCreatedApp1, authData, destEnv);
			assertNotNull(manuallyCreatedApp1.getId());
			
			UserGroup group1 = new UserGroup();
			group1.setName("com.rm.InternalGroup");
			ugService.save(group1, authData, destEnv);
			
			UserGroup group2 = new UserGroup();
			group2.setName("com.rm.InternalGroup");
			deploymentService.testMethod(group2, authData, destEnv);
		}*/
		
		PackageDeploymentStatus deployStatus = deploymentService.deployZip(libPackage, new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), authData, destEnv);
		assertFalse("Deployment should have failed because the deployed action references missing class", deployStatus.isSuccess());
		
		assertEquals(1, appService.find(new AppFilter(), authData, destEnv).size());
		assertEquals((Integer)initialActionCount, (Integer)actionService.getActions(null, destEnv).size());
		
		packageDeploymentCounter++;
	}
	
	@Test
	@Rollback(true)
	public void testMethod5CheckRollback() throws KommetException
	{
		// first make sure the previous test method completed succesfully
		assertTrue("Test method executing deployment did not complete successfully, so the test scenario cannot be continued", packageDeploymentCounter  == 2);
		assertNotNull(initialActionCount);
		
		AuthData authData = dataHelper.getRootAuthData(destEnv);
		
		List<App> existingApps = appService.find(new AppFilter(), authData, destEnv);
		assertEquals(1, existingApps.size());
		assertEquals(MANUALLY_CREATE_APP_NAME_1, existingApps.get(0).getName());
		assertEquals((Integer)initialActionCount, (Integer)actionService.getActions(null, destEnv).size());
		assertEquals((Integer)1, (Integer)ugService.get(new UserGroupFilter(), authData, destEnv).size());
		
		// now delete the manually created env and its database
		if (destEnv != null)
		{
			envService.deleteEnv(KID.get(destEnvId));
			assertEquals((Integer)0, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE kid = '" + destEnvId + "'", Integer.class));
			
			destEnv = null;
		}
	}
	
	private Library addUserGroupToLibrary(Library lib, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException
	{
		// create app
		UserGroup group1 = new UserGroup();
		group1.setName("com.rm.InternalGroup");
		ugService.save(group1, authData, env);
		
		UserGroup group2 = new UserGroup();
		group2.setName("com.rm.InternalGroup1");
		ugService.save(group2, authData, env);
		
		deployableItems.add(group1);
		deployableItems.add(group2);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, true, env));
		
		return lib;
	}

	private Library addAppToLibrary(Library lib, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException
	{
		// create app
		App app1 = new App();
		app1.setType("Internal app");
		app1.setName("kommet.MyAppOne");
		app1.setLabel(DUPLICATE_APP_LABEL);
		app1 = appService.save(app1, authData, env);
		assertNotNull(app1.getId());
		
		App app2 = new App();
		app2.setType("Internal app");
		app2.setName("kommet.MyAppTwo");
		app2.setLabel("Some app two");
		app2 = appService.save(app2, authData, env);
		assertNotNull(app2.getId());
		
		deployableItems.add(app1);
		deployableItems.add(app2);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, true, env));
		
		return lib;
	}
	
	private Library addClassesToLibrary(Library lib, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException
	{
		Class kollFile1 = getTestClassFile("TestFile", "com.some", env);
		kollFile1 = classService.fullSave(kollFile1, dataService, dataHelper.getRootAuthData(env), env);
		assertNotNull(kollFile1.getId());
		
		Class kollFile2 = getTestClassFile("TestFile2", "com.some", env);
		kollFile2 = classService.fullSave(kollFile2, dataService, dataHelper.getRootAuthData(env), env);
		assertNotNull(kollFile2.getId());
		
		deployableItems.add(kollFile1);
		deployableItems.add(kollFile2);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, true, env));
		
		return lib;
	}
	
	private Class getTestClassFile(String name, String userPackageName, EnvData env) throws KommetException
	{
		String code = "package " + userPackageName + ";\n";
		code += "public class " + name + " { ";
		code += "public String getText() { return \"kamila\"; }";
		code += "}";
		
		Class file = new Class();
		file.setIsSystem(false);
		file.setJavaCode("test code");
		file.setName(name);
		file.setPackageName(userPackageName);
		file.setKollCode(code);
		return file;
	}

	private Library addActionToLibrary(Library lib, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		ActionKType actionType = (ActionKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME));
		Record testAction = new Record(actionType);
		testAction.setField("url", "some/url");
		testAction.setField("isSystem", false);
		testAction.setField("isPublic", false);
		testAction.setField("name", TEST_ACTION_NAME);
		testAction.setField("createdDate", new Date());
		testAction.setField("controllerMethod", "pigeonList");
		
		Record pigeonListView = dataHelper.getPigeonListView(env);
		testAction.setField("view", pigeonListView);
		pigeonListView = dataService.save(pigeonListView, env);
		
		Record pigeonListKollFile = dataHelper.getPigeonListControllerClass(env);
		testAction.setField("controller", pigeonListKollFile);
		pigeonListKollFile = dataService.save(pigeonListKollFile, env);
		
		testAction.setField("view", pigeonListView);
		testAction.setField("controller", pigeonListKollFile);
		
		Action savedAction = actionService.save(new Action(testAction, env), dataHelper.getRootAuthData(env), env);
		assertNotNull(savedAction.getId());
		
		deployableItems.add(savedAction);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, true, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + 1, libPackageFile);
		assertItemsInPackage(originalItemCount + 2, libPackageFile);
		
		return lib;
	}

	private void assertItemsInPackage(int expectedItemCount, byte[] libPackageFile) throws IOException
	{	
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(libPackageFile));
		int itemCount = 0;
    	// iterate over files in the zip
    	while (zis.getNextEntry() != null)
    	{	
    		itemCount++;
    	}
    	
    	assertEquals(expectedItemCount, itemCount);
	}
}
