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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.http.MediaType;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.Action;
import kommet.basic.App;
import kommet.basic.AppUrl;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.File;
import kommet.basic.Layout;
import kommet.basic.Library;
import kommet.basic.LibraryException;
import kommet.basic.LibraryItem;
import kommet.basic.Profile;
import kommet.basic.RecordAccessType;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyUtil;
import kommet.basic.ScheduledTask;
import kommet.basic.UniqueCheck;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.basic.ValidationRule;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.basic.WebResource;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewService;
import kommet.basic.types.ActionKType;
import kommet.basic.types.SystemTypes;
import kommet.dao.FieldFilter;
import kommet.data.ComponentType;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.UniqueCheckService;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.data.validationrules.ValidationRuleService;
import kommet.deployment.Deployable;
import kommet.deployment.DeploymentConfig;
import kommet.deployment.DeploymentService;
import kommet.deployment.FailedPackageDeploymentException;
import kommet.deployment.FileDeploymentStatus;
import kommet.deployment.OverwriteHandling;
import kommet.deployment.PackageDeploymentStatus;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.files.FileService;
import kommet.filters.AppUrlFilter;
import kommet.filters.UserFilter;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.scheduler.ScheduledTaskFilter;
import kommet.scheduler.ScheduledTaskService;
import kommet.services.AppService;
import kommet.services.LibraryService;
import kommet.services.UserGroupService;
import kommet.services.ViewResourceService;
import kommet.services.WebResourceService;
import kommet.tests.TestDataCreator;
import kommet.tests.webmock.BasicWebMockTest;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@WebAppConfiguration
@Rollback
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LibraryTest extends BasicWebMockTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	LibraryService libService;
	
	@Inject
	WebResourceService webResourceService;
	
	@Inject
	FileService fileService;
	
	@Inject
	SchedulerFactoryBean schedulerFactory;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	ValidationRuleService vrService;
	
	@Inject
	DataService dataService;
	
	@Inject
	UniqueCheckService uniqueCheckService;
	
	@Inject
	AppService appService;
	
	@Inject
	UserGroupService ugService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	ScheduledTaskService stService;
	
	@Inject
	ClassService classService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	DeploymentService deploymentService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	EnvService envService;
	
	@Inject
	UserService userService;
	
	/*@Test
	@Rollback(false)
	public void method1PrepareDestEnv() throws Exception
	{	
		if (destEnv == null)
		{
			destEnvName = "a" + MiscUtils.getHash(10);
			Random rand = new Random();
			destEnvId = "0010000001" + MiscUtils.padLeft(String.valueOf(rand.nextInt(999)), 3, '0');
			destEnv = envService.createEnv(destEnvName, KID.get(destEnvId), true);
			
			// fetch the env again, because this will create standard controllers and other items
			// this does not have to be done, because it would be done automatically the first time the env is referenced,
			// but we want to do this so that we can count classes and later be sure how many there are
			destEnv = envService.get(KID.get(destEnvId));
			
			for (Type type : destEnv.getAllTypes())
			{
				// make sure unique checks are initialized on types
				assertNotNull("No unique checks on type " + type.getQualifiedName(), type.getUniqueChecks());
				assertTrue("No unique checks on type " + type.getQualifiedName(), !type.getUniqueChecks().isEmpty());
			}
			
			initialClassCount = classService.getClasses(new ClassFilter(), destEnv).size();
			
			// create test system administrator user
			Profile saProfile = profileService.getProfileByName(Profile.SYSTEM_ADMINISTRATOR_NAME, destEnv);
			saUser = userService.save(dataHelper.getTestUser("testsa@kommet.io", "testsa@kommet.io", "admin123", saProfile, destEnv), dataHelper.getRootAuthData(destEnv), destEnv);
			
			assertEquals(initialClassCount, classService.getClasses(new ClassFilter(), destEnv).size());
		}
	}*/

	@Test
	public void testLibraryCRUD() throws Exception
	{
		// reset scheduler
		//schedulerFactory.getScheduler().shutdown();
		//schedulerFactory.getScheduler().start();
		
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Library lib = new Library();
		lib.setName("label library");
		
		try
		{
			lib = libService.save(lib, authData, env);
			fail("Saving library with invalid name should fail");
		}
		catch (LibraryException e)
		{
			assertEquals("Invalid library name. Library names can only contain lowercase characters and dots", e.getMessage());
		}
		
		lib.setName("kommet.libs.Labels");
		lib.setVersion("0.1");
		lib.setStatus("Not installed");
		lib.setDescription("Test lib");
		lib.setSource("External (manual deployment)");
		
		try
		{
			lib = libService.save(lib, authData, env);
		}
		catch (LibraryException e)
		{
			assertEquals("Cannot save non-local library with no items", e.getMessage());
		}
		
		lib.setSource("Local");
		
		try
		{
			lib = libService.save(lib, authData, env);
			fail("Saving library with source = 'External' and no access level should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
		}
		
		lib.setAccessLevel("Editable");
		lib.setIsEnabled(true);
		
		lib = libService.save(lib, authData, env);
		
		assertNotNull(lib.getId());
		
		lib = libService.getLibrary(lib.getId(), authData, env);
		assertNotNull(lib);
		assertNotNull(lib.getItems());
		assertEquals(0, lib.getItems().size());
	
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(libPackageFile));
		
		int itemCount = 0;
    	// iterate over files in the zip
    	while (zis.getNextEntry() != null)
    	{
    		itemCount++;
    	}
    	
    	// make sure all items have been packaged, plus 1 additional item for the library.xml file
    	assertEquals(lib.getItems().size() + 1, itemCount);
    	
    	// create dest env
    	EnvData destEnv = dataHelper.getTestEnv2Data(false);
    	basicSetupService.runBasicSetup(destEnv);
    	AuthData destEnvAuthData = dataHelper.getRootAuthData(destEnv);
    	stService.clearAllScheduledTasks(destEnv.getId());
    	
    	// test all kinds of files in library package
    	testLibraryPackageCreation(lib, classService.getClasses(null, destEnv).size(), authData, env, destEnv, destEnvAuthData);
	}

	private Class getTestClassFile(String className, String packageName, EnvData env) throws KommetException
	{	
		Class file = new Class();
		file.setIsSystem(false);
		
		String kollCode = "package " + packageName + "; public class " + className + " { public void execute() { System.out.print(\"hello from scheduled task\"); } }"; 
		
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(kollCode, true, dataHelper.getRootAuthData(env), env));
		file.setName(className);
		file.setPackageName(packageName);
		file.setKollCode(kollCode);
		return file;
	}
	
	private File createFileWithContent(String fileName, String content, EnvData env) throws KommetException, IOException
	{
		java.io.File diskFile = null;
		
		if (content != null)
		{
			// create some file on disk
			diskFile = new java.io.File(appConfig.getFileDir() + "/" + fileName + ".123");
			FileWriter fw = new FileWriter(diskFile);
			fw.write(content);
			fw.close();
		}
		
		File file = fileService.saveFile(null, fileName, content != null ? diskFile.getName() : "random-path", File.PUBLIC_ACCESS, true, dataHelper.getRootAuthData(env), env);
		assertNotNull(file);
		assertNotNull("File not saved", file.getId());
		return file;
	}

	private void testLibraryPackageCreation(Library lib, int initialClassCount, AuthData authData, EnvData env, EnvData destEnv, AuthData destEnvAuthData) throws Exception
	{
		List<Deployable> deployableItems = new ArrayList<Deployable>();
		
		byte[] libPackage = libService.createLibraryPackage(lib, authData, env);
		
		// initially the library is empty and contains just the library.xml file
		assertItemsInPackage(1, libPackage);
		
		testPackageWebResource(lib, deployableItems, authData, env);
		
		// one web resource was added, but this caused also a file to be added to the library, so finally there are two files added
		assertItemsInPackage(3, libService.createLibraryPackage(lib, authData, env));
		
		String testLayoutName = "kommet.MyTestLayout";
		
		testPackageLayouts(lib, testLayoutName, 2, deployableItems, authData, env);
		
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		// deploy pigeon type, as it will be referenced by other items such as validation rules and unique checks
		deployableItems.add(pigeonType);
		
		for (Field field : pigeonType.getFields())
		{
			if (!Field.isSystemField(field.getApiName()))
			{
				deployableItems.add(field);
			}
		}
		
		assertScheduledJobs(0, destEnv, false);
		
		// create some records of type pigeon that will be packaged and deployed
		List<RecordProxy> pigeons = new ArrayList<RecordProxy>();
		for (int i = 0; i < 3; i++)
		{
			Record pigeon = new Record(pigeonType);
			pigeon.setField("name", "Mark" + i);
			pigeon.setField("age", 23);
			pigeon = dataService.save(pigeon, env);
			
			pigeons.add(RecordProxyUtil.generateCustomTypeProxy(pigeon, env, compiler));
		}
		
		testPackageValidationRules(lib, 2, pigeonType, deployableItems, authData, env);
		testPackageUniqueChecks(lib, 2, pigeonType, deployableItems, authData, env);
		testPackageApps(lib, 2, pigeonType, deployableItems, authData, env);
		testPackageViews(lib, testLayoutName, 2, pigeonType, deployableItems, authData, env);
		testPackageUserGroups(lib, 2, pigeonType, deployableItems, authData, env);
		testPackageProfiles(lib, 2, pigeonType, deployableItems, authData, env);
		testPackageScheduledTasks(lib, 2, pigeonType, deployableItems, authData, env);
		testPackageViewResources(lib, 2, pigeonType, deployableItems, authData, env);
		testPackageRecords(lib, 2, pigeonType, pigeons, deployableItems, authData, env);
		Action testAction = testPackageActions(lib, 2, pigeonType, deployableItems, authData, env);
		
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		lib.getItems().add(libService.getLibraryItemsFromRecords(pigeons, "list.pigeons", pigeonType, authData, env));
		
		// now deploy the library
		libPackage = libService.createLibraryPackage(lib, authData, env);
		
		// make sure the new environment is clean
		for (Deployable deployedItem : deployableItems)
		{
			if (deployedItem instanceof Class)
			{
				assertEquals(initialClassCount, classService.getClasses(new ClassFilter(), destEnv).size());
			}
			else if (deployedItem instanceof RecordProxy)
			{
				KID recordId = ((RecordProxy)deployedItem).getId();
				// find item by ID
				List<Record> records = dataService.getRecords(MiscUtils.toList(recordId), destEnv.getTypeByRecordId(recordId), MiscUtils.toList(Field.ID_FIELD_NAME), authData, destEnv);
				assertTrue("Record of type " + destEnv.getTypeByRecordId(recordId).getQualifiedName() + " should not have been found", records.isEmpty());
			}
			else if (deployedItem instanceof Type)
			{
				assertNull(destEnv.getType(((Type)deployedItem).getKeyPrefix()));
			}
			else if (deployedItem instanceof Field)
			{
				assertNull(destEnv.getType(((Field)deployedItem).getType().getKeyPrefix()));
			}
			else
			{
				fail("Unsupported deployed item type " + deployedItem.getClass().getName());
			}
		}
		
		// make sure no scheduled tasks exist on the dest env
		assertNull(stService.getScheduledJob("kommet.TaskOne", destEnv.getId()));
		
		PackageDeploymentStatus deployStatus = null;
		
		try
		{
			deployStatus = deploymentService.deployZip(libPackage, new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), authData, destEnv);
			fail("Deployment should have failed");
		}
		catch (FailedPackageDeploymentException e)
		{
			// expected
			deployStatus = e.getStatus();
		}
		
		for (FileDeploymentStatus fileStatus : deployStatus.getFileStatuses())
		{
			assertNull("When deployment failed, all file statuses must have null record IDs because no records were deployed", fileStatus.getDeployedComponentId());
			assertNotNull(fileStatus.getDefinition());
		}
		
		assertLibDeploymentData(deployStatus, false, lib, authData, destEnv);
		
		// make sure no scheduled tasks exist on the dest env
		assertNull(stService.getScheduledJob("kommet.TaskOne", destEnv.getId()));
		
		// make sure the action has not been created
		assertNull(destEnv.getActionForUrl(testAction.getUrl()));
		
		// No further tests are possible in this setup, because the deactivate method operates in a different transaction than the deployment method
		// so the fact that the library was deactivated will not be visible in the context of the deployment method's transaction (the library will be
		// still active in that transaction). This however clashes with the state of the env in the cache, because in the env cache the library is indeed deactivated.
		
		// now create the library again
		/*deployStatus = deploymentService.deployZip(libPackage, new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getAuthData(saUser, destEnv), destEnv);
		assertTrue("Deployment with SA profile failed. " + deployStatus.printStatus(), deployStatus.isSuccess());
		assertLibraryCorrectlyDeployed(lib, destEnv, env);
		// save deployed library
		deployedLib = libService.save(deployStatus.getLibrary(), destAuthData, destEnv);
		
		// TODO create a class that references a type from the library
		Class testClass = new Class();
		testClass.setName("TestClass");
		testClass.setPackageName("my.pack");
		testClass.setKollCode("package my.pack.TestClass\nimport " + pigeonType.getQualifiedName() + ";\nclass TestClass { private Pigeon somePigeonVar; }");
		testClass.setJavaCode(classService.getKollTranslator(env).kollToJava(testClass.getJavaCode(), env));
		testClass.setIsSystem(false);
		testClass = classService.fullSave(testClass, saAuthData, destEnv);*/
	}
	
	private Library getTestLib(AuthData authData, EnvData env) throws KommetException
	{
		Library lib = new Library();
		lib.setName("kommet.libs.Labels");
		lib.setVersion("0.1");
		lib.setStatus("Not installed");
		lib.setDescription("Test lib");
		lib.setSource("Local");
		lib.setAccessLevel("Editable");
		lib.setIsEnabled(true);
		
		lib = libService.save(lib, authData, env);
		assertNotNull(lib.getId());
		
		return lib;
	}
	
	@Test
	public void testDeployPackageWithAction() throws Exception
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Library lib = getTestLib(authData, env);
		
		// create dest env
    	EnvData destEnv = dataHelper.getTestEnv2Data(false);
    	basicSetupService.runBasicSetup(destEnv);
    	stService.clearAllScheduledTasks(destEnv.getId());
    	
    	Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		// set default field on type
		pigeonType.setDefaultFieldId(pigeonType.getField("name").getKID());
		pigeonType = dataService.updateType(pigeonType, authData, env);
		
		// create some records of type pigeon that will be packaged and deployed
		List<RecordProxy> pigeons = new ArrayList<RecordProxy>();
		for (int i = 0; i < 3; i++)
		{
			Record pigeon = new Record(pigeonType);
			pigeon.setField("name", "Mark" + i);
			pigeon.setField("age", 23);
			pigeon = dataService.save(pigeon, env);
			
			pigeons.add(RecordProxyUtil.generateCustomTypeProxy(pigeon, env, compiler));
		}
		
		List<Deployable> deployableItems = new ArrayList<Deployable>();
		
		// deploy pigeon type, as it will be referenced by other items such as validation rules and unique checks
		deployableItems.add(pigeonType);
		
		for (Field field : pigeonType.getFields())
		{
			if (!Field.isSystemField(field.getApiName()))
			{
				deployableItems.add(field);
			}
		}
		
		testPackageValidationRules(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageUniqueChecks(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageApps(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageUserGroups(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageProfiles(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageScheduledTasks(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageViewResources(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageRecords(lib, 1, pigeonType, pigeons, deployableItems, authData, env);
		Action testAction = testPackageActions(lib, 1, pigeonType, deployableItems, authData, env);
		
		// add missing class and view to the package
		deployableItems.add(testAction.getController());
		deployableItems.add(testAction.getView());
		
		String testLayoutName = "kommet.MyTestLayout";
		testPackageLayouts(lib, testLayoutName, 1, deployableItems, authData, env);
		testPackageViews(lib, testLayoutName, 1, pigeonType, deployableItems, authData, env);
		
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));

		// create test system administrator user on the destination environment
		Profile saProfile = profileService.getProfileByName(Profile.SYSTEM_ADMINISTRATOR_NAME, destEnv);
		User saUser = userService.save(dataHelper.getTestUser("testsa@kommet.io", "testsa@kommet.io", "admin123", saProfile, destEnv), dataHelper.getRootAuthData(destEnv), destEnv);
    	String saAccessToken = this.obtainAccessToken(saUser.getUserName(), "admin123", destEnv);
    	
    	byte[] libPackage = libService.createLibraryPackage(lib, authData, env);
    	
    	assertScheduledJobs(0, destEnv, false);
		
		this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DEPLOY_PACKAGE_URL)
				.param("access_token", saAccessToken)
				.param("env", destEnv.getId().getId())
				.content(libPackage)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().is(HttpServletResponse.SC_OK))
	        	.andReturn();
		
		// the mock HTTP rest method above operates internally on a new instance of EnvData, so the one we have in this method
		// will not be aware of the changes made by the HTTP method in the controller
		// this is why we need to reinitialize the env
		envService.resetEnv(destEnv.getId());
		destEnv = envService.get(env.getId());
		
		// for some (unknown) reason after the env has been reinitialized, the inserted SA user is not seen, so we need to insert them again
		saUser = userService.save(dataHelper.getTestUser("testsa@kommet.io", "testsa@kommet.io", "admin123", saProfile, destEnv), dataHelper.getRootAuthData(destEnv), destEnv);
		
		// make sure no scheduled tasks exist on the dest env
		assertScheduledJobs(2, destEnv, false);
		
		AuthData destAuthData = dataHelper.getRootAuthData(destEnv);
		
		// make sure class has been deployed
		Class deployedController = classService.getClass(testAction.getController().getQualifiedName(), destEnv);
		assertNotNull(deployedController);
		
		// find type in dest env's database
		Type pigeonTypeFromDb = dataService.getTypeByName(pigeonType.getQualifiedName(), false, destEnv);
		assertNotNull(pigeonTypeFromDb);
		
		Type deployedPigeonType = destEnv.getType(pigeonType.getQualifiedName());
		assertNotNull(deployedPigeonType);
		
		for (Field field : pigeonType.getFields())
		{
			assertNotNull("Field " + field.getApiName() + " not deployed", deployedPigeonType.getField(field.getApiName()));
		}
		
		// make sure the whole package has been deployed
		assertLibraryCorrectlyDeployed(lib, destEnv, env);
		
		// make sure the default field for type has also been correctly set
		Type deployedType = destEnv.getType(pigeonType.getQualifiedName());
		assertNotNull(deployedType);
		assertEquals("name", deployedType.getDefaultFieldApiName());
				
		PackageDeploymentStatus deployStatus = deploymentService.deployZip(libPackage, new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), destAuthData, destEnv);
		assertTrue("Deployment failed. " + deployStatus.printStatus(), deployStatus.isSuccess());
		assertLibDeploymentData(deployStatus, true, lib, authData, destEnv);
		assertTrue(deployStatus.getFailedStatuses().isEmpty());
		assertEquals("For each deployed file there has to be a status returned", lib.getItems().size(), deployStatus.getFileStatuses().size());
		assertScheduledJobs(2, destEnv, false);
		
		for (int i = 0; i < 3; i++)
		{
			// try the same deployment again with flag that allows overwriting
			deployStatus = deploymentService.deployZip(libPackage, new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), destAuthData, destEnv);
			assertTrue("Subsequent deployment of the same package failed. " + deployStatus.printStatus(), deployStatus.isSuccess());
			
			assertLibraryCorrectlyDeployed(lib, destEnv, env);
		}
		
		userService.get(new UserFilter(), destEnv);
		
		// now deploy again with system administrator user (not root)
		try
		{
			deployStatus = deploymentService.deployZip(libPackage, new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getAuthData(saUser, destEnv), destEnv);
		}
		catch (FailedPackageDeploymentException e)
		{
			deployStatus = e.getStatus();
			fail("Deployment failed" + deployStatus.printStatus());
		}
		
		assertTrue("Deployment with SA profile failed. " + deployStatus.printStatus(), deployStatus.isSuccess());
		assertLibraryCorrectlyDeployed(lib, destEnv, env);
		assertLibDeploymentData(deployStatus, true, lib, dataHelper.getAuthData(saUser, destEnv), destEnv);
		
		// now make sure that with no overwrite allowed, the deployment will fail
		try
		{
			deployStatus = deploymentService.deployZip(libPackage, new DeploymentConfig(OverwriteHandling.ALWAYS_REJECT), dataHelper.getAuthData(saUser, destEnv), destEnv);
			fail("Deployment should fail, because overwriting existing components is disallowed");
		}
		catch (FailedPackageDeploymentException e)
		{
			deployStatus = e.getStatus();
			assertNotNull(deployStatus.getError());
			assertTrue(deployStatus.getError().startsWith("Library with name"));
		}
		
		assertLibDeploymentData(deployStatus, true, lib, authData, destEnv);
		
		// TODO make sure deploying a scheduled task whose referenced class does not exist fails
		// TODO test OverwriteHandling
		// TODO prevent overwriting system profiles, types etc. in deployment
		// TODO make sure that all fields are deployed when a type is deployed
		// TODO optional: test invalid java type deployed for numeric fields
		// TODO test deactivation and deletion of libraries
		
		// deploy again
		deployStatus = deploymentService.deployZip(libPackage, new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getAuthData(saUser, destEnv), destEnv);
		assertTrue("Deployment with SA profile failed. " + deployStatus.printStatus(), deployStatus.isSuccess());
		assertLibraryCorrectlyDeployed(lib, destEnv, env);
		
		assertNull("Source of the library should not be set by the deployment method", deployStatus.getLibrary().getSource());
		deployStatus.getLibrary().setSource("External (manual deployment)");
		
		int initialLibItemCount = libService.getLibraryItems(null, destAuthData, destEnv).size();
		
		AuthData saAuthData = dataHelper.getAuthData(saUser, destEnv);
		
		// save deployed library
		Library deployedLib = libService.save(deployStatus.getLibrary(), saAuthData, destEnv);
		deployedLib = libService.getLibrary(deployedLib.getId(), saAuthData, destEnv);
		assertEquals((Integer)RecordAccessType.PUBLIC.getId(), deployedLib.getAccessType());
		
		// deactivate the library using system admin user
		testDeactivateLib(deployedLib, saAuthData, destEnv);
		
		// delete the deactivated library
		libService.delete(deployStatus.getLibrary().getId(), saAuthData, destEnv);
		
		assertNull(libService.getLibrary(deployStatus.getLibrary().getId(), saAuthData, destEnv));
		assertEquals(initialLibItemCount, libService.getLibraryItems(null, destAuthData, destEnv).size());
	}
	
	@Test
	public void testDeployAsSystemAdministrator() throws Exception
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Library lib = getTestLib(authData, env);
		
		// create dest env
    	EnvData destEnv = dataHelper.getTestEnv2Data(false);
    	basicSetupService.runBasicSetup(destEnv);
    	stService.clearAllScheduledTasks(destEnv.getId());
    	
    	Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		// create some records of type pigeon that will be packaged and deployed
		List<RecordProxy> pigeons = new ArrayList<RecordProxy>();
		for (int i = 0; i < 3; i++)
		{
			Record pigeon = new Record(pigeonType);
			pigeon.setField("name", "Mark" + i);
			pigeon.setField("age", 23);
			pigeon = dataService.save(pigeon, env);
			
			pigeons.add(RecordProxyUtil.generateCustomTypeProxy(pigeon, env, compiler));
		}
		
		List<Deployable> deployableItems = new ArrayList<Deployable>();
		
		// deploy pigeon type, as it will be referenced by other items such as validation rules and unique checks
		deployableItems.add(pigeonType);
		
		for (Field field : pigeonType.getFields())
		{
			if (!Field.isSystemField(field.getApiName()))
			{
				deployableItems.add(field);
			}
		}
    	
    	testPackageValidationRules(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageUniqueChecks(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageApps(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageUserGroups(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageProfiles(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageScheduledTasks(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageViewResources(lib, 1, pigeonType, deployableItems, authData, env);
		testPackageRecords(lib, 1, pigeonType, pigeons, deployableItems, authData, env);
		Action testAction = testPackageActions(lib, 1, pigeonType, deployableItems, authData, env);

		// create test system administrator user
		Profile saProfile = profileService.getProfileByName(Profile.SYSTEM_ADMINISTRATOR_NAME, destEnv);
		User saUser = userService.save(dataHelper.getTestUser("testsa@kommet.io", "testsa@kommet.io", "admin123", saProfile, destEnv), dataHelper.getRootAuthData(destEnv), destEnv);
		
    	String saAccessToken = this.obtainAccessToken(saUser.getUserName(), "admin123", destEnv);
    	
    	byte[] libPackage = libService.createLibraryPackage(lib, authData, env);
		
		// try to deploy the package, but it will fail because the package does not contain
    	// the controller and view referenced by action "testAction"
		this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DEPLOY_PACKAGE_URL)
				.param("access_token", saAccessToken)
				.param("env", destEnv.getId().getId())
				.content(libPackage)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8"))) // ???
	        	.andExpect(status().is(HttpServletResponse.SC_BAD_REQUEST))
	        	.andReturn();
		
		// make sure the action has not been created
		assertNull(destEnv.getActionForUrl(testAction.getUrl()));
		
		assertScheduledJobs(0, destEnv, true);
	}

	private void testPackageRecords(Library lib, int permanentFiles, Type pigeonType, List<RecordProxy> pigeons, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		// check the original number of items before new items are added to the package
		assertItemsInPackage(originalItemCount + permanentFiles, libService.createLibraryPackage(lib, authData, env));
		
		LibraryItem item = libService.getLibraryItemsFromRecords(pigeons, "list.pigeons", pigeonType, authData, env);
		
		lib.getItems().add(item);
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 1, libPackageFile);
	}

	private void testDeactivateLib(Library lib, AuthData authData, EnvData destEnv) throws KommetException
	{
		assertEquals("Installed", lib.getStatus());
		assertTrue(lib.getIsEnabled());
		List<LibraryService.LibraryItemDeleteStatus> deactivateStatuses = libService.deactivateLibrary(lib, false, authData, destEnv);
		assertEquals(lib.getItems().size(), deactivateStatuses.size());
		
		lib = libService.getLibrary(lib.getId(), authData, destEnv);
		assertEquals("Installed-Deactivated", lib.getStatus());
		assertFalse(lib.getIsEnabled());
		
		for (LibraryService.LibraryItemDeleteStatus deleteResult : deactivateStatuses)
		{
			// the files are not used anywhere, so it should be possible to delete all types of
			// items, even types and fields
			assertTrue(deleteResult.isDeleted());
			assertNull(deleteResult.getReason());
		}
		
		// make sure the items are really deleted from the env
		for (LibraryItem item : lib.getItems())
		{
			switch (ComponentType.values()[item.getComponentType()])
			{
				case CLASS: 
					
					assertNull(classService.getClass(item.getApiName(), destEnv));
					assertNull(item.getRecordId());
					break;
					
				case VIEW: 
					
					assertNull(viewService.getView(item.getApiName(), true, destEnv));
					assertNull(item.getRecordId());
					break;
					
				case TYPE:
					
					assertNull(destEnv.getType(item.getApiName()));
					assertNull(dataService.getTypeByName(item.getApiName(), false, destEnv));
					assertNull(item.getRecordId());
					break;
					
				case FIELD:
					
					FieldFilter fieldFilter = new FieldFilter();
					List<String> nameParts = MiscUtils.splitByLastDot(item.getApiName());
					fieldFilter.setTypeQualifiedName(nameParts.get(0));
					fieldFilter.setApiName(nameParts.get(1));
					assertTrue(dataService.getFields(fieldFilter, destEnv).isEmpty());
					assertNull(item.getRecordId());
					break;
					
				case LAYOUT:
					
					assertNull(layoutService.getByName(item.getApiName(), destEnv));
					assertNull(item.getRecordId());
					break;
					
				case VALIDATION_RULE:
					
					assertNull(vrService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv));
					assertNull(item.getRecordId());
					break;
					
				case UNIQUE_CHECK:
					
					assertNull(uniqueCheckService.getByName(item.getApiName(), dataService, AuthData.getRootAuthData(destEnv), destEnv));
					assertNull(item.getRecordId());
					break;
					
				case APP:
					
					// TODO make sure urls are removed as well
					
					assertNull(appService.getAppByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv));
					assertNull(item.getRecordId());
					break;
					
				case SCHEDULED_TASK:
					
					assertNull(stService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv));
					assertNull(item.getRecordId());
					
					// TODO item.getAPiName() will always return null, we need to search scheduled jobs by task.getQuartzJobName
					
					assertNull(stService.getScheduledJob(item.getApiName(), destEnv.getId()));
					break;
					
				case PROFILE:
					
					assertNull(profileService.getProfileByName(item.getApiName(), destEnv));
					assertNull(item.getRecordId());
					break;
					
				case USER_GROUP:
				
					assertNull(ugService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv));
					assertNull(item.getRecordId());
					break;
				
				case VIEW_RESOURCE:
					
					assertNull(viewResourceService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv));
					assertNull(item.getRecordId());
					break;
					
				case WEB_RESOURCE:
					
					assertNull(webResourceService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv));
					assertNull(item.getRecordId());
					break;
					
				case ACTION:
					
					assertNull(actionService.getActionByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv));
					assertNull(item.getRecordId());
					break;
					
				default: throw new KommetException("Cannot find extenstion for component type " + ComponentType.values()[item.getComponentType()]);
			}
		}
	}

	private void assertLibDeploymentData(PackageDeploymentStatus status, boolean isLibOverwrite, Library sourceLib, AuthData authData, EnvData destEnv) throws KommetException
	{
		Library destLib = status.getLibrary();
		assertNotNull(destLib);
		
		if (isLibOverwrite)
		{
			assertNotNull(destLib.getId());
		}
		else
		{
			assertNull(destLib.getId());
		}
		
		assertEquals(sourceLib.getName(), destLib.getName());
		assertEquals(sourceLib.getAccessLevel(), destLib.getAccessLevel());
		assertEquals(sourceLib.getProvider(), destLib.getProvider());
		assertEquals(sourceLib.getVersion(), destLib.getVersion());
		assertEquals(sourceLib.getDescription(), destLib.getDescription());
		assertNull(destLib.getSource());
		
		if (status.isSuccess())
		{
			assertEquals("Installed", destLib.getStatus());
			assertTrue(destLib.getIsEnabled());
		}
		else
		{
			assertEquals("Installation failed", destLib.getStatus());
			assertFalse(destLib.getIsEnabled());
		}
		
		assertNotNull(destLib.getItems());
		
		for (LibraryItem destItem : destLib.getItems())
		{
			assertNull(destItem.getId());
			assertNotNull(destItem.getComponentType());
			assertNotNull(destItem.getDefinition());
			
			if (status.isSuccess())
			{
				assertNotNull(destItem.getRecordId());
				
				if (!destItem.getComponentType().equals(ComponentType.TYPE.getId()) && !destItem.getComponentType().equals(ComponentType.FIELD.getId()))
				{
					Type type = destEnv.getTypeByRecordId(destItem.getRecordId());
					
					assertNotNull("Type not found for record ID " + destItem.getRecordId(), type);
					
					// make sure the user who deployed the library is set as the creator of the record
					List<Record> records = destEnv.getSelectCriteriaFromDAL("select " + Field.CREATEDBY_FIELD_NAME + ".id, " + Field.LAST_MODIFIED_BY_FIELD_NAME + ".id from " + type.getQualifiedName() + " where " + Field.ID_FIELD_NAME + " = '" + destItem.getRecordId() + "'").list();
					assertEquals(1, records.size());
					assertEquals("Incorrect owner of record " + destItem.getRecordId(), destEnv.getRootUser().getKID(), records.get(0).getField(Field.CREATEDBY_FIELD_NAME + "." + Field.ID_FIELD_NAME));
					assertEquals("Incorrect last modifier of record " + destItem.getRecordId(), authData.getUserId(), records.get(0).getField(Field.LAST_MODIFIED_BY_FIELD_NAME + "." + Field.ID_FIELD_NAME));
				}
			}
			else
			{
				assertNull(destItem.getRecordId());
			}
		}
		
		for (LibraryItem sourceItem : sourceLib.getItems())
		{
			boolean itemFound = false;
			
			for (LibraryItem destItem : destLib.getItems())
			{
				if (sourceItem.getApiName().equals(destItem.getApiName()) && sourceItem.getComponentType().equals(destItem.getComponentType()))
				{
					itemFound = true;
					break;
				}
			}
			
			assertTrue("Item " + sourceItem.getApiName() + " not present in dest lib", itemFound);
		}
		
		if (sourceLib.getItems().size() != destLib.getItems().size())
		{
			List<LibraryItem> largerList = null;
			List<LibraryItem> smallerList = null;
			
			if (sourceLib.getItems().size() > destLib.getItems().size())
			{
				largerList = sourceLib.getItems();
				smallerList = destLib.getItems();
			}
			else
			{
				smallerList = sourceLib.getItems();
				largerList = destLib.getItems();
			}
			
			List<String> missingItems = new ArrayList<String>();
			List<String> duplicateItems = new ArrayList<String>();
			
			// check which items are present in one list, but not the other
			for (LibraryItem item1 : largerList)
			{
				int found = 0;
				for (LibraryItem item2 : smallerList)
				{
					if (item1.getApiName().equals(item2.getApiName()) && item1.getComponentType().equals(item2.getComponentType()))
					{
						found++;
					}
				}
				
				if (found == 0)
				{
					missingItems.add(item1.getApiName() + " (" + item1.getComponentType() + ")");
				}
				else if (found > 1)
				{
					duplicateItems.add(item1.getApiName() + " (" + item1.getComponentType() + ")");
				}
			}
			
			fail("Some items are missing: " + MiscUtils.implode(missingItems, " ,") + ", some are duplicate " + MiscUtils.implode(duplicateItems, " ,"));
		}
		assertEquals(status.getFileStatuses().size(), destLib.getItems().size());
	}

	/**
	 * Makes sure all items contained in the library have been correctly deployed to the destination environment;
	 * @param lib
	 * @param destEnv2
	 * @throws KommetException 
	 */
	private void assertLibraryCorrectlyDeployed(Library lib, EnvData destEnv, EnvData sourceEnv) throws KommetException
	{
		for (LibraryItem item : lib.getItems())
		{
			switch (ComponentType.values()[item.getComponentType()])
			{
				case CLASS: 
					
					Class cls = classService.getClass(item.getApiName(), destEnv);
					assertNotNull("Class " + item.getApiName() + " not deployed", cls);
					
					Class sourceClass = classService.getClass(item.getApiName(), sourceEnv);
					
					// compare all properties
					assertEquals(sourceClass.getQualifiedName(), cls.getQualifiedName());
					assertEquals(sourceClass.getAccessLevel(), cls.getAccessLevel());
					assertEquals(sourceClass.getKollCode(), cls.getKollCode());
					
					// java code should be identical, apart from the package name which is env specific
					assertEquals(sourceClass.getJavaCode().replace(sourceEnv.getId().getId(), "xyz"), cls.getJavaCode().replace(destEnv.getId().getId(), "xyz"));
					assertEquals(sourceClass.getIsSystem(), cls.getIsSystem());
					
					break;
					
				case VIEW: 
					
					View sourceView = viewService.getView(item.getApiName(), true, sourceEnv);
					View destView = viewService.getView(item.getApiName(), true, destEnv);
					
					assertEquals(sourceView.getAccessLevel(), destView.getAccessLevel());
					assertEquals(sourceView.getIsSystem(), destView.getIsSystem());
					assertEquals(sourceView.getKeetleCode(), destView.getKeetleCode());
					assertEquals("JSP code of views should be identical, it should differ by hard-coded environment IDs in included layouts", sourceView.getJspCode(), destView.getJspCode());
					assertEquals(sourceView.getJspCode().replace("userlayouts/" + sourceEnv.getId(), "xyz"), destView.getJspCode().replace("userlayouts/" + destEnv.getId(), "xyz"));
					
					if (sourceView.getLayout() != null)
					{
						assertEquals(sourceView.getLayout().getName(), destView.getLayout().getName());
					}
					else
					{
						assertNull(destView.getLayout());
					}
					
					assertEquals(sourceView.getPackageName(), destView.getPackageName());
					assertEquals(sourceView.getQualifiedName(), destView.getQualifiedName());
					
					break;
					
				case TYPE:
					
					Type sourceType = sourceEnv.getType(item.getApiName());
					Type destType = destEnv.getType(item.getApiName());
					
					assertEquals(sourceType.getQualifiedName(), destType.getQualifiedName());
					assertEquals(sourceType.getDescription(), destType.getDescription());
					assertEquals(sourceType.getLabel(), destType.getLabel());
					assertEquals(sourceType.getPluralLabel(), destType.getPluralLabel());
					
					if (sourceType.getDefaultField() != null)
					{
						assertEquals(sourceType.getDefaultFieldApiName(), destType.getDefaultFieldApiName());
					}
					
					assertEquals(sourceType.getFields().size(), destType.getFields().size());
					assertEquals(sourceType.isCombineRecordAndCascadeSharing(), destType.isCombineRecordAndCascadeSharing());
					
					if (sourceType.getSharingControlledByFieldId() != null)
					{
						assertEquals(sourceType.getSharingControlledByField().getApiName(), destType.getSharingControlledByField().getApiName());
					}
					
					break;
					
				case FIELD:
					
					FieldFilter fieldFilter = new FieldFilter();
					List<String> nameParts = MiscUtils.splitByLastDot(item.getApiName());
					fieldFilter.setTypeQualifiedName(nameParts.get(0));
					fieldFilter.setApiName(nameParts.get(1));
					Field sourceField = dataService.getFields(fieldFilter, sourceEnv).get(0);
					Field destField = dataService.getFields(fieldFilter, destEnv).get(0);
					
					assertEquals(sourceField.getDataType().getId(), destField.getDataType().getId());
					assertEquals(sourceField.getDefaultValue(), destField.getDefaultValue());
					assertEquals(sourceField.isRequired(), destField.isRequired());
					assertEquals(sourceField.isTrackHistory(), destField.isTrackHistory());
					assertEquals(sourceField.getType().getQualifiedName(), destField.getType().getQualifiedName());
					
					// TODO compare specific properties of data types, e.g. length of numeric data type
					if (sourceField.getDataTypeId().equals(DataType.NUMBER))
					{
						assertEquals(((NumberDataType)sourceField.getDataType()).getDecimalPlaces(), ((NumberDataType)destField.getDataType()).getDecimalPlaces());
						assertEquals(((NumberDataType)sourceField.getDataType()).getJavaType(), ((NumberDataType)destField.getDataType()).getJavaType());
					}
					else if (sourceField.getDataTypeId().equals(DataType.TEXT))
					{
						assertEquals(((TextDataType)sourceField.getDataType()).getLength(), ((TextDataType)destField.getDataType()).getLength());
					}
					else if (sourceField.getDataTypeId().equals(DataType.TYPE_REFERENCE))
					{
						assertEquals(((TypeReference)sourceField.getDataType()).getType().getQualifiedName(), ((TypeReference)destField.getDataType()).getType().getQualifiedName());
					}
					else if (sourceField.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
					{
						assertEquals(((InverseCollectionDataType)sourceField.getDataType()).getInverseType().getQualifiedName(), ((InverseCollectionDataType)destField.getDataType()).getInverseType().getQualifiedName());
						assertEquals(((InverseCollectionDataType)sourceField.getDataType()).getInverseProperty(), ((InverseCollectionDataType)destField.getDataType()).getInverseProperty());
					}
					else if (sourceField.getDataTypeId().equals(DataType.ASSOCIATION))
					{
						assertEquals(((AssociationDataType)sourceField.getDataType()).getAssociatedType().getQualifiedName(), ((AssociationDataType)destField.getDataType()).getAssociatedType().getQualifiedName());
						assertEquals(((AssociationDataType)sourceField.getDataType()).getLinkingType().getQualifiedName(), ((AssociationDataType)destField.getDataType()).getLinkingType().getQualifiedName());
						assertEquals(((AssociationDataType)sourceField.getDataType()).getSelfLinkingField(), ((AssociationDataType)destField.getDataType()).getSelfLinkingField());
						assertEquals(((AssociationDataType)sourceField.getDataType()).getForeignLinkingField(), ((AssociationDataType)destField.getDataType()).getForeignLinkingField());
					}
					else if (sourceField.getDataTypeId().equals(DataType.ENUMERATION))
					{
						assertEquals(((EnumerationDataType)sourceField.getDataType()).getValues(), ((EnumerationDataType)destField.getDataType()).getValues());
						assertEquals(((EnumerationDataType)sourceField.getDataType()).isValidateValues(), ((EnumerationDataType)destField.getDataType()).isValidateValues());
					}
					else if (sourceField.getDataTypeId().equals(DataType.MULTI_ENUMERATION))
					{
						assertEquals(((MultiEnumerationDataType)sourceField.getDataType()).getValues(), ((MultiEnumerationDataType)destField.getDataType()).getValues());
					}
					
					break;
					
				case LAYOUT:
					
					Layout sourceLayout = layoutService.getByName(item.getApiName(), sourceEnv);
					Layout destLayout = layoutService.getByName(item.getApiName(), destEnv);
					
					assertEquals(sourceLayout.getAfterContent(), destLayout.getAfterContent());
					assertEquals(sourceLayout.getBeforeContent(), destLayout.getBeforeContent());
					assertEquals(sourceLayout.getCode(), destLayout.getCode());
					
					break;
					
				case VALIDATION_RULE:
					
					ValidationRule sourceRule = vrService.getByName(item.getApiName(), AuthData.getRootAuthData(sourceEnv), sourceEnv);
					ValidationRule destRule = vrService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv);
					
					assertEquals(sourceRule.getActive(), destRule.getActive());
					assertEquals(sourceRule.getIsSystem(), destRule.getIsSystem());
					assertEquals(sourceRule.getCode(), destRule.getCode());
					assertEquals(sourceRule.getErrorMessage(), destRule.getErrorMessage());
					assertEquals(sourceRule.getErrorMessageLabel(), destRule.getErrorMessageLabel());
					assertEquals(sourceEnv.getType(sourceRule.getTypeId()).getQualifiedName(), destEnv.getType(destRule.getTypeId()).getQualifiedName());
					
					break;
					
				case UNIQUE_CHECK:
					
					UniqueCheck sourceCheck = uniqueCheckService.getByName(item.getApiName(), dataService, AuthData.getRootAuthData(sourceEnv), sourceEnv);
					UniqueCheck destCheck = uniqueCheckService.getByName(item.getApiName(), dataService, AuthData.getRootAuthData(destEnv), destEnv);
					
					assertEquals(sourceCheck.getIsSystem(), destCheck.getIsSystem());
					assertEquals(sourceEnv.getType(sourceCheck.getTypeId()).getQualifiedName(), destEnv.getType(destCheck.getTypeId()).getQualifiedName());
					
					// compare field names
					assertEquals(sourceCheck.getParsedFieldIds().size(), destCheck.getParsedFieldIds().size());
					
					Type sourceCheckType = sourceEnv.getType(sourceCheck.getTypeId());
					Type destCheckType = destEnv.getType(destCheck.getTypeId());
					
					for (KID fieldId : sourceCheck.getParsedFieldIds())
					{
						Field field = sourceCheckType.getField(fieldId);
						
						boolean matchingFieldFound = false;
						
						// make sure this field is also included in the dest check
						for (KID destFieldId : destCheck.getParsedFieldIds())
						{
							Field destCheckField = destCheckType.getField(destFieldId);
							if (destCheckField.getApiName().equals(field.getApiName()))
							{
								matchingFieldFound = true;
								break;
							}
						}
						
						assertTrue("Field " + field.getApiName() + " not included in deployed unique check", matchingFieldFound);
					}
					
					break;
					
				case APP:
					
					App sourceApp = appService.getAppByName(item.getApiName(), AuthData.getRootAuthData(sourceEnv), sourceEnv);
					App destApp = appService.getAppByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv);
					
					assertEquals(sourceApp.getLabel(), destApp.getLabel());
					assertEquals(sourceApp.getLandingUrl(), destApp.getLandingUrl());
					assertEquals(sourceApp.getType(), destApp.getType());
					
					// find URLs for app
					AppUrlFilter urlFilter = new AppUrlFilter();
					urlFilter.addAppId(sourceApp.getId());
					List<AppUrl> sourceUrls = appService.find(urlFilter, sourceEnv);
					
					if (!sourceUrls.isEmpty())
					{
						urlFilter = new AppUrlFilter();
						urlFilter.addAppId(destApp.getId());
						List<AppUrl> destUrls = appService.find(urlFilter, destEnv);
						
						assertEquals(sourceUrls.size(), destUrls.size());
						
						for (AppUrl url : sourceUrls)
						{
							boolean urlFound = false;
							
							for (AppUrl destUrl : destUrls)
							{
								if (url.getUrl().equals(destUrl.getUrl()))
								{
									urlFound = true;
									break;
								}
							}
							
							assertTrue("URL " + url.getUrl() + " not deployed with the app", urlFound);
						}
					}
					
					break;
					
				case SCHEDULED_TASK:
					
					ScheduledTask sourceTask = stService.getByName(item.getApiName(), AuthData.getRootAuthData(sourceEnv), sourceEnv);
					ScheduledTask destTask = stService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv);
					
					assertEquals(sourceTask.getMethod(), destTask.getMethod());
					assertEquals(sourceTask.getFile().getQualifiedName(), destTask.getFile().getQualifiedName());
					assertEquals(sourceTask.getCronExpression(), destTask.getCronExpression());
					
					break;
					
				case PROFILE:
					
					Profile sourceProfile = profileService.getProfileByName(item.getApiName(), sourceEnv);
					Profile destProfile = profileService.getProfileByName(item.getApiName(), destEnv);
					
					assertEquals(sourceProfile.getLabel(), destProfile.getLabel());
					assertEquals(sourceProfile.getSystemProfile(), destProfile.getSystemProfile());
					
					break;
					
				case USER_GROUP:
					
					UserGroup sourceGroup = ugService.getByName(item.getApiName(), AuthData.getRootAuthData(sourceEnv), sourceEnv);
					UserGroup destGroup = ugService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv);
					
					assertEquals(sourceGroup.getDescription(), destGroup.getDescription());
					
					break;
				
				case VIEW_RESOURCE:
					
					ViewResource sourceViewRes = viewResourceService.getByName(item.getApiName(), AuthData.getRootAuthData(sourceEnv), sourceEnv);
					ViewResource destViewRes = viewResourceService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv);
					
					assertEquals(sourceViewRes.getMimeType(), destViewRes.getMimeType());
					assertEquals(sourceViewRes.getContent(), destViewRes.getContent());
					
					break;
					
				case WEB_RESOURCE:
					
					WebResource sourceWebRes = webResourceService.getByName(item.getApiName(), AuthData.getRootAuthData(sourceEnv), sourceEnv);
					WebResource destWebRes = webResourceService.getByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv);
					
					assertEquals(sourceWebRes.getMimeType(), destWebRes.getMimeType());
					
					// compare disk file length
					assertEquals((new java.io.File(appConfig.getFileDir() + "/" + sourceWebRes.getDiskFilePath())).length(), (new java.io.File(appConfig.getFileDir() + "/" + destWebRes.getDiskFilePath())).length());
					
					break;
					
				case ACTION:
					
					Action sourceAction = actionService.getActionByName(item.getApiName(), AuthData.getRootAuthData(sourceEnv), sourceEnv);
					Action destAction = actionService.getActionByName(item.getApiName(), AuthData.getRootAuthData(destEnv), destEnv);
					
					assertEquals(sourceAction.getIsPublic(), destAction.getIsPublic());
					assertEquals(sourceAction.getController().getQualifiedName(), destAction.getController().getQualifiedName());
					assertEquals(sourceAction.getUrl(), destAction.getUrl());
					assertEquals(sourceAction.getView().getQualifiedName(), destAction.getView().getQualifiedName());
					assertEquals(sourceAction.getControllerMethod(), destAction.getControllerMethod());
					
					break;
					
				default: throw new KommetException("Cannot find extenstion for component type " + ComponentType.values()[item.getComponentType()]);
			}
		}
	}

	private void assertScheduledJobs(int expectedCount, EnvData destEnv, boolean isSchedulerOnly) throws KommetException, SchedulerException
	{
		int actualCount = 0;
		
		if (!isSchedulerOnly)
		{
			// make sure no scheduled tasks exist on the dest env
			assertEquals(expectedCount, stService.get(new ScheduledTaskFilter(), destEnv).size());
		}
		
		for (String groupName : schedulerFactory.getScheduler().getJobGroupNames())
		{
			if (groupName.equals(ScheduledTaskService.getJobGroupNameForEnv(destEnv.getId())))
			{
				final GroupMatcher<JobKey> groupMatcher = GroupMatcher.groupEquals(groupName);
				actualCount = schedulerFactory.getScheduler().getJobKeys(groupMatcher).size();
				break;
			}
		}
		
		assertEquals("Expected and actual quartz job number differs", expectedCount, actualCount);
	}

	private Action testPackageActions(Library lib, int permanentFiles, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		ActionKType actionType = (ActionKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME));
		Record testAction = new Record(actionType);
		testAction.setField("url", "some/url");
		testAction.setField("isSystem", false);
		testAction.setField("isPublic", false);
		testAction.setField("name", "com.lib.actions.TestAction");
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
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 1, libPackageFile);
		
		return savedAction;
	}

	private void testPackageViewResources(Library lib, int permanentFiles, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		// create view resource
		ViewResource resource1 = new ViewResource();
		resource1.setName("styles.css");
		resource1.setPath("4738493742.css");
		resource1.setMimeType("text/css");
		resource1 = viewResourceService.save(resource1, authData, env);
		assertNotNull(resource1.getId());
		
		ViewResource resource2 = new ViewResource();
		resource2.setName("styles2.js");
		resource2.setPath("47384937421.css");
		resource2.setMimeType("text/javascript");
		resource2 = viewResourceService.save(resource2, authData, env);
		assertNotNull(resource2.getId());
		
		deployableItems.add(resource1);
		deployableItems.add(resource2);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 2, libPackageFile);
	}

	private void testPackageScheduledTasks(Library lib, int permanentFiles, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws IOException, KommetException
	{
		int originalItemCount = deployableItems.size();
		
		Class taskFile1 = getTestClassFile("TaskOne", "kommet", env);
		taskFile1 = classService.save(taskFile1, authData, env);
		CompilationResult result = compiler.compile(taskFile1, env);
		assertTrue(result.getDescription(), result.isSuccess());
		
		Class taskFile2 = getTestClassFile("TaskTwo", "kommet", env);
		taskFile2 = classService.save(taskFile2, authData, env);
		compiler.compile(taskFile2, env);
		assertTrue(result.getDescription(), result.isSuccess());
		
		ScheduledTask task1 = stService.schedule(taskFile1, "execute", "kommet.TaskOne", "0 0 * * * ?", authData, env);
		assertNotNull(task1.getId());
		ScheduledTask task2 = stService.schedule(taskFile2, "execute", "kommet.TaskTwo", "0 1 * * * ?", authData, env);
		assertNotNull(task2.getId());
		
		deployableItems.add(task1);
		deployableItems.add(task2);
		deployableItems.add(taskFile1);
		deployableItems.add(taskFile2);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 4, libPackageFile);
	}

	private void testPackageProfiles(Library lib, int permanentFiles, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		Profile testProfile1 = dataHelper.getTestProfileObject("com.lib.TestProfile1", env);
		Profile testProfile2 = dataHelper.getTestProfileObject("com.lib.TestProfile2", env);
		
		deployableItems.add(testProfile1);
		deployableItems.add(testProfile2);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 2, libPackageFile);
	}

	private void testPackageUserGroups(Library lib, int permanentFiles, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		UserGroup group1 = new UserGroup();
		group1.setName("kommet.Group1");
		group1.setDescription("Some description");
		group1 = ugService.save(group1, authData, env);
		assertNotNull(group1.getId());
		
		UserGroup group2 = new UserGroup();
		group2.setName("kommet.Group2");
		group2.setDescription("Some description");
		group2 = ugService.save(group2, authData, env);
		assertNotNull(group2.getId());
		
		deployableItems.add(group1);
		deployableItems.add(group2);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 2, libPackageFile);
	}

	private void testPackageApps(Library lib, int permanentFiles, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		// create app
		App app1 = new App();
		app1.setType("Internal app");
		app1.setName("kommet.MyAppOne");
		app1.setLabel("Some app");
		app1 = appService.save(app1, authData, env);
		assertNotNull(app1.getId());
		
		App app2 = new App();
		app2.setType("Internal app");
		app2.setName("kommet.MyAppTwo");
		app2.setLabel("Some app 2");
		app2 = appService.save(app2, authData, env);
		assertNotNull(app2.getId());
		
		deployableItems.add(app1);
		deployableItems.add(app2);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 2, libPackageFile);
	}
	
	private void testPackageViews(Library lib, String layoutName, int permanentFiles, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		// create app
		View view1 = new View();
		view1.setName("MyView1");
		view1.setPackageName("kommet");
		view1.setIsSystem(false);
		
		view1.setKeetleCode("<km:view name=\"MyView1\" layout=\"" + layoutName + "\" package=\"kommet\"></km:view>");
		view1 = viewService.fullSave(view1, view1.getKeetleCode(), false, authData, env);
		
		deployableItems.add(view1);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 1, libPackageFile);
	}

	private void testPackageUniqueChecks(Library lib, int permanentFiles, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KIDException, KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		// create a unique check
		UniqueCheck check1 = new UniqueCheck();
		check1.setName("com.checks.SomeCheck1");
		check1.setTypeId(pigeonType.getKID());
		check1.setFieldIds(pigeonType.getField("birthdate").getKID().getId());
		
		check1 = uniqueCheckService.save(check1, authData, env);
		assertNotNull(check1.getId());
		
		UniqueCheck check2 = new UniqueCheck();
		check2.setName("com.checks.SomeCheck");
		check2.setTypeId(pigeonType.getKID());
		check2.setFieldIds(pigeonType.getField("name").getKID().getId());
		
		check2 = uniqueCheckService.save(check2, authData, env);
		assertNotNull(check2.getId());
		
		deployableItems.add(check1);
		deployableItems.add(check2);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 2, libPackageFile);
	}

	/*private static int getNonSystemFields(List<Deployable> deployableItems)
	{
		int fieldCount = 0;
		
		for (Deployable c : deployableItems)
		{
			if (c instanceof Type)
			{
				Type type = (Type)c;
				for (Field f : type.getFields())
				{
					if (!Field.isSystemField(f.getApiName()))
					{
						fieldCount++;
					}
				}
			}
		}
		
		return fieldCount;
	}*/

	private void testPackageValidationRules(Library lib, int permanentFiles, Type pigeonType, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		int originalItemCount = deployableItems.size();
		
		ValidationRule vr = new ValidationRule();
		vr.setActive(true);
		vr.setCode("name <> 'mike'");
		vr.setTypeId(pigeonType.getKID());
		vr.setName("CheckName");
		vr.setIsSystem(false);
		vr.setErrorMessage("Invalid name \"Quote\"");
		vr = vrService.save(vr, authData, env);
		assertNotNull(vr.getId());
		
		deployableItems.add(vr);
		
		try
		{
			lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
			fail("It should not be possible to package a library item with a non-qualified API name");
		}
		catch (LibraryException e)
		{
			assertTrue(e.getMessage().startsWith("API name of a library item must be qualified"));
		}
		
		vr.setName("com.example.mylib.CheckName");
		vr = vrService.save(vr, authData, env);
		lib.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(lib.getItems().size() + permanentFiles, libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 1, libPackageFile);
	}

	private void testPackageLayouts(Library lib1, String layoutName, int permanentFiles, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{	
		int originalItemCount = deployableItems.size();
		
		Layout layout1 = new Layout();
		
		String code = "<km:layout name=\"" + layoutName + "\"><km:beforeContent><a href=\"aa\">some link</a></km:beforeContent></km:layout>";
		layout1.setCode(code);
		layout1.setName(layoutName);
		layout1 = layoutService.save(layout1, dataHelper.getRootAuthData(env), env);
		assertNotNull(layout1.getId());
		
		Layout layout2 = new Layout();
		code = "<km:layout name=\"kommet.TestLayout2\"><km:beforeContent><a href=\"aa\">some link</a></km:beforeContent></km:layout>";
		layout2.setCode(code);
		layout2.setName("kommet.TestLayout2");
		layout2 = layoutService.save(layout2, dataHelper.getRootAuthData(env), env);
		assertNotNull(layout2.getId());
		
		deployableItems.add(layout1);
		deployableItems.add(layout2);
		
		lib1.setItems((ArrayList<LibraryItem>)libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib1, authData, env);
		assertNotNull(libPackageFile);
		assertItemsInPackage(originalItemCount + permanentFiles + 2, libPackageFile);
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

	private void testPackageWebResource(Library lib1, List<Deployable> deployableItems, AuthData authData, EnvData env) throws KommetException, IOException
	{
		// create web resource
		File testFile = createFileWithContent("Some file", "some-content", env);
		
		String resourceName = "kommet.JavascriptLibrary";
		
		// make sure there are no cached web resources on the env
		assertTrue(env.getWebResources().isEmpty());
		
		WebResource resource = new WebResource();
		resource.setFile(testFile);
		resource.setMimeType("application/javascript");
		resource.setName(resourceName);
		resource = webResourceService.save(resource, true, dataHelper.getRootAuthData(env), env);
		assertNotNull(resource.getId());
		assertNotNull(env.getWebResource(resourceName));
		assertNotNull(env.getWebResource(resourceName).getDiskFilePath());
		assertEquals(1, env.getWebResources().size());
		
		deployableItems.add(resource);
		
		// add one file for library.xml
		int originalItemCount = lib1.getItems().size() + 1;
		
		lib1.getItems().addAll(libService.getLibraryItemsFromComponents(deployableItems, false, env));
		
		byte[] libPackageFile = libService.createLibraryPackage(lib1, authData, env);
		assertNotNull(libPackageFile);
		
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(libPackageFile));
		
		int itemCount = 0;
		ZipEntry ze = null;
		boolean isJsResourceFound = false;
		
    	// iterate over files in the zip
    	while ((ze = zis.getNextEntry()) != null)
    	{
    		if (ze.getName().endsWith(".js"))
    		{
    			String fileContent = IOUtils.toString(zis);
    			assertEquals("some-content", fileContent);
    			isJsResourceFound = true;
    		}
    		
    		itemCount++;
    	}
    	
    	assertTrue(isJsResourceFound);
    	assertEquals("Two new files should have been added to package: one web resource metadata XML file, and one web resource byte file", originalItemCount + 2, itemCount);
	}
}
