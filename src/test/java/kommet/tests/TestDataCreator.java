/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.auth.RootAuthData;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.Profile;
import kommet.basic.SystemContextAware;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.ViewUtil;
import kommet.basic.types.ProfileKType;
import kommet.basic.types.SystemTypes;
import kommet.basic.types.UniqueCheckKType;
import kommet.data.DataService;
import kommet.data.Env;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.DataSourceFactory;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.exceptions.NotImplementedException;
import kommet.i18n.InternationalizationService;
import kommet.koll.ClassService;
import kommet.koll.annotations.Action;
import kommet.koll.annotations.Controller;
import kommet.koll.annotations.Public;
import kommet.koll.annotations.Rest;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.GlobalSettingsService;
import kommet.services.UserGroupService;
import kommet.tests.harness.UserGroupHierarchyDataSet;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.TestConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@Rollback(true)
@Service
public class TestDataCreator
{
	@Inject
	TestConfig testConfig;
	
	@Inject
	DataSourceFactory dsFactory;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	GlobalSettingsService settingService;
	
	@Inject
	KommetCompiler kommetCompiler;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	InternationalizationService i18nService;
	
	@Inject
	UserGroupService userGroupService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UserService userService;
	
	@Inject
	ClassService classService;
	
	@Inject
	EnvService envService;
	
	EnvData sharedTestEnv;

	public static final String PIGEON_TYPE_API_NAME = "Pigeon";
	public static final String PIGEON_TYPE_PACKAGE = "krawiec.birds";
	public static final String PIGEON_TYPE_QUALIFIED_NAME = PIGEON_TYPE_PACKAGE + "." + PIGEON_TYPE_API_NAME;

	public static final String PIGEON_LIST_VIEW = "pigeonList";
	public static final String PIGEON_LIST_VIEW_PACKAGE = "com.tests.birds";
	public static final String PIGEON_LIST_CONTROLLER = "PigeonListController";
	public static final String WEB_TEST_BASE_URL = "http://localhost:8080/km-1.0/";
	public static final String WEB_TEST_ADMIN_USER = "root";
	public static final String WEB_TEST_ADMIN_PASSWORD = "test";
	public static final String WEB_COMPANY_API_NAME = "Company";
	public static final String WEB_COMPANY_COMPANY_LABEL = "Company";
	public static final String WEB_COMPANY_COMPANY_PLURAL_LABEL = "Companies";
	public static final String WEB_VIEW_LIST_URL = "views/list";
	public static final String WEB_NEW_VIEW_URL = "views/new";
	public static final String WEB_TEST_ENV_ID = "001hqc2lv4ex7";
	public static final String WEB_TEST_ENV_NAME = "sady";
	
	private AuthData rootAuthData;
	
	@Test
	public void emptyTest()
	{
		//
	}
	
	public static String getRestServiceController (List<String> serviceMethods, String controller, String packageName, List<String> additionalImports)
	{
		String imports = "import " + Controller.class.getName() + ";\n";
		imports += "import " + Action.class.getName() + ";\n";
		imports += "import " + Rest.class.getName() + ";\n";
		imports += "import " + Public.class.getName() + ";\n";
		imports += "import " + BaseController.class.getName() + ";\n";
		
		if (additionalImports != null && !additionalImports.isEmpty())
		{
			for (String i : additionalImports)
			{
				imports += "import " + i + ";\n";
			}
		}
		
		String code = "package " + packageName + ";\n";
		String classCode = "@Controller\npublic class " + controller + " extends " + BaseController.class.getSimpleName() + "\n{\n";
	
		for (String method : serviceMethods)
		{
			classCode += method + "\n";
		}
		
		classCode += "}";
		
		return code + imports + classCode;
	}
	
	public UserGroupHierarchyDataSet createUserGroupHierarchy (AuthData authData, EnvData env) throws KommetException
	{
		// create three user groups
		UserGroup teacherGroup = new UserGroup();
		teacherGroup.setName("Teachers");
		teacherGroup = userGroupService.save(teacherGroup, authData, env);
		
		UserGroup studentGroup = new UserGroup();
		studentGroup.setName("Students");
		studentGroup = userGroupService.save(studentGroup, authData, env);
		
		UserGroup mathStudentGroup = new UserGroup();
		mathStudentGroup.setName("MathStudents");
		mathStudentGroup = userGroupService.save(mathStudentGroup, authData, env);
		
		UserGroup algebraStudentGroup = new UserGroup();
		algebraStudentGroup.setName("AlgebraStudents");
		algebraStudentGroup = userGroupService.save(algebraStudentGroup, authData, env);
		
		UserGroup geometryStudentGroup = new UserGroup();
		geometryStudentGroup.setName("GeometryStudents");
		geometryStudentGroup = userGroupService.save(geometryStudentGroup, authData, env);
		
		// create subgroups: Students > Math Students > Algebra Students
		userGroupService.assignGroupToGroup(mathStudentGroup.getId(), studentGroup.getId(), authData, env);
		userGroupService.assignGroupToGroup(geometryStudentGroup.getId(), mathStudentGroup.getId(), authData, env);
		userGroupService.assignGroupToGroup(algebraStudentGroup.getId(), mathStudentGroup.getId(), authData, env);
		
		// create some profile
		Record profile = dataService.save(getTestProfile("PersonProfile", env), env);
		
		Record teacher1 = dataService.save(getTestUser("teacher1", "teacher1@kommet.io", profile, env), env);
		Record teacher2 = dataService.save(getTestUser("teacher2", "teacher2@kommet.io", profile, env), env);
		Record mathStudent1 = dataService.save(getTestUser("math-student-1", "math-student-1@kommet.io", profile, env), env);
		Record algebraStudent1 = dataService.save(getTestUser("algebra-student-1", "algebra-student-1@kommet.io", profile, env), env);
		Record algebraStudent2 = dataService.save(getTestUser("algebra-student-2", "algebra-student-2@kommet.io", profile, env), env);
		Record geometryStudent1 = dataService.save(getTestUser("geometry-student-1", "geometry-student-1@kommet.io", profile, env), env);
		
		// assign users to groups
		userGroupService.assignUserToGroup(teacher1.getKID(), teacherGroup.getId(), authData, env);
		userGroupService.assignUserToGroup(teacher2.getKID(), teacherGroup.getId(), authData, env);
		userGroupService.assignUserToGroup(mathStudent1.getKID(), mathStudentGroup.getId(), authData, env);
		userGroupService.assignUserToGroup(algebraStudent1.getKID(), algebraStudentGroup.getId(), authData, env);
		userGroupService.assignUserToGroup(algebraStudent2.getKID(), algebraStudentGroup.getId(), authData, env);
		userGroupService.assignUserToGroup(geometryStudent1.getKID(), geometryStudentGroup.getId(), authData, env);
		
		UserGroupHierarchyDataSet ds = new UserGroupHierarchyDataSet();
		ds.setAlgebraStudentGroup(algebraStudentGroup);
		ds.setGeometryStudentGroup(geometryStudentGroup);
		ds.setMathStudentGroup(mathStudentGroup);
		ds.setProfile(profile);
		ds.setStudentGroup(studentGroup);
		ds.setTeacher1(teacher1);
		ds.setTeacher2(teacher2);
		ds.setTeacherGroup(teacherGroup);
		ds.setAlgebraStudent1(algebraStudent1); 
		ds.setAlgebraStudent2(algebraStudent2);
		ds.setMathStudent1(mathStudent1);
		ds.setGeometryStudent1(geometryStudent1);
		return ds;
	}
	
	public AuthData getAuthData (User user, EnvData env) throws KommetException
	{
		AuthData authData = new AuthData(user, env, permissionService, compiler);
		authData.initUserPermissions(env);
		return authData;
	}
	
	public AuthData getRootAuthData (EnvData env) throws KommetException
	{
		if (this.rootAuthData == null)
		{
			this.rootAuthData = basicSetupService.getRootAuthData(env);
			((RootAuthData)this.rootAuthData).setPermissionService(permissionService);
			this.rootAuthData.setI18n(i18nService.getDictionary(this.rootAuthData.getLocale()));
		}
		return this.rootAuthData;
	}
	
	public EnvData getTestEnvData (boolean addUniqueChecks) throws KommetException
	{
		EnvData env = new EnvData(getTestEnv(), getTestEnvDataSource(getTestEnv().getKID(), envService));
		env.setCompileClassPath(compiler.getCompileClassPath(env.getId()));
		
		if (addUniqueChecks)
		{
			// in order to add unique checks we need to have a root user assigned to the env
			// so first we need to create user and profile types
			
			// create root profile
			ProfileKType profileType = basicSetupService.createProfileType(env);
			basicSetupService.createUserType(env, profileType, rootAuthData);
			Record rootProfile = dataService.saveSystemProfile(BasicSetupService.getRootProfile(env), false, env);
			
			// create root authdata
			AuthData rootAuthData = new AuthData();
			User root = new User();
			root.setId(AppConfig.getRootUserId());
			
			Profile rootProfileObj = new Profile();
			rootProfileObj.setId(rootProfile.getKID());
			
			rootAuthData.setProfile(rootProfileObj);
			rootAuthData.setUser(root);
			
			Record rootProfileRecord = new Record(env.getType(KeyPrefix.get(KID.PROFILE_PREFIX)));
			rootProfileRecord.setKID(rootProfileObj.getId());
			
			Record rootUserRecord = new Record(env.getType(KeyPrefix.get(KID.USER_PREFIX)));
			rootUserRecord.setKID(root.getId());
			rootUserRecord.setField("profile", rootProfileRecord);
			
			env.setRootUser(rootUserRecord);
			
			UniqueCheckKType ucType = new UniqueCheckKType();
			ucType.setPackage(ucType.getPackage());
			// unique check must exist before we can add any other objects, so we add it to the most basic configuration
			dataService.createUniqueCheckType(ucType, rootAuthData, env);
		}
		
		return env;
	}
	
	/**
	 * Returns a test environment that is created only once for all tests.
	 * 
	 * One has to remember that specific tests can add/remove object to the environment, so no assumptions
	 * should be made as to the environments state, apart from that is is properly configured and possesses
	 * all basic objects created by the BasicSetupService.runBasicSetup method.
	 * 
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public EnvData getSharedTestEnv() throws KommetException
	{
		// TODO
		// When object are created on an env below, this transaction is rolled back after the
		// test method that calls it exits, so the next method trying to access the shared env
		// does not see the objects.
		// This needs to be fixed.
		throw new NotImplementedException("This method is not yet implemented");
		
		/*
		if (this.sharedTestEnv == null)
		{
			log.debug("Configuring shared test env");
			this.sharedTestEnv = getTestEnvData(false);
			basicSetupService.runBasicSetup(this.sharedTestEnv);
			
			KID adminId = this.sharedTestEnv.getRootUser().getKID();
			
			// create pigeon object
			kObjService.create(getFullPigeonObject(), adminId, this.sharedTestEnv);
			
			// create pigeon list view and controller
			kObjService.save(getPigeonListView(this.sharedTestEnv), adminId, this.sharedTestEnv);
			kObjService.save(getPigeonListKollFile(this.sharedTestEnv), adminId, this.sharedTestEnv);
		}
		
		return this.sharedTestEnv;*/
	}
	
	public EnvData getTestEnv2Data(boolean addUniqueChecks) throws KommetException
	{
		EnvData env = new EnvData(getTestEnv2(), getTestEnv2DataSource(getTestEnv2().getKID(), envService));
		env.setCompileClassPath(compiler.getCompileClassPath(env.getId()));
		
		if (addUniqueChecks)
		{
			// unique check must exist before we can add any other objects, so we add it to the most basic configuration
			dataService.createUniqueCheckType(new UniqueCheckKType(), getRootAuthData(env), env);
		}
		
		return env;
	}
	
	public EnvData getTestEnv3Data(boolean addUniqueChecks) throws KommetException
	{
		EnvData env = new EnvData(getTestEnv3(), getTestEnv3DataSource(getTestEnv3().getKID(), envService));
		env.setCompileClassPath(compiler.getCompileClassPath(env.getId()));
		
		if (addUniqueChecks)
		{
			// unique check must exist before we can add any other objects, so we add it to the most basic configuration
			dataService.createUniqueCheckType(new UniqueCheckKType(), getRootAuthData(env), env);
		}
		
		return env;
	}
	
	private DataSource getTestEnvDataSource(KID envId, EnvService envService) throws KommetException
	{
		return dsFactory.getDataSource(envId, envService, testConfig.getTestEnvDBHost(), testConfig.getTestEnvDBPort(), testConfig.getTestEnvDB(), testConfig.getTestEnvDBUser(), testConfig.getTestEnvDBPassword(), false);
	}
	
	private DataSource getTestEnv2DataSource(KID envId, EnvService envService) throws KommetException
	{
		return dsFactory.getDataSource(envId, envService, testConfig.getTestEnvDBHost(), testConfig.getTestEnvDBPort(), testConfig.getTestEnv2DB(), testConfig.getTestEnvDBUser(), testConfig.getTestEnvDBPassword(), false);
	}
	
	private DataSource getTestEnv3DataSource(KID envId, EnvService envService) throws KommetException
	{
		return dsFactory.getDataSource(envId, envService, testConfig.getTestEnvDBHost(), testConfig.getTestEnvDBPort(), testConfig.getTestEnv3DB(), testConfig.getTestEnvDBUser(), testConfig.getTestEnvDBPassword(), false);
	}

	public Type getPigeonType(EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName(PIGEON_TYPE_API_NAME);
		type.setPackage(PIGEON_TYPE_PACKAGE);
		type.setLabel("Pigeon");
		type.setPluralLabel("Pigeons");
		type.setCreated(new Date());
		
		return type;
	}
	
	public Type getFarmType(EnvData env) throws KommetException
	{
		Type obj = new Type();
		obj.setApiName("Farm");
		obj.setPackage(PIGEON_TYPE_PACKAGE);
		obj.setLabel("Farm");
		obj.setPluralLabel("Farms");
		obj.setCreated(new Date());
		
		return obj;
	}
	
	public Type getAddressType(EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName("Address");
		type.setPackage(PIGEON_TYPE_PACKAGE);
		type.setLabel("Address");
		type.setPluralLabel("Addresses");
		type.setCreated(new Date());
		type.setBasic(false);
		
		// add street field
		Field streetField = new Field();
		streetField.setApiName("street");
		streetField.setLabel("Street");
		streetField.setDataType(new TextDataType(50));
		type.addField(streetField);
		
		// add city field
		Field cityField = new Field();
		cityField.setApiName("city");
		cityField.setLabel("City");
		cityField.setDataType(new TextDataType(50));
		type.addField(cityField);
		
		return type;
	}
	
	public Type getFullPigeonType (EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName(PIGEON_TYPE_API_NAME);
		type.setPackage(PIGEON_TYPE_PACKAGE);
		type.setLabel("Pigeon");
		type.setPluralLabel("Pigeons");
		
		type.addField(getAgeField());
		type.addField(getNameField());
		type.addField(getBirthDateField());
		type.addField(getFatherField(type));
		type.addField(getMotherField(type));
		type.addField(getColourField());
		
		return type;
	}

	private Field getColourField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("colour");
		field.setDataType(new EnumerationDataType("blue\ngreen\nbrown\nyellow"));
		field.setLabel("Colour");
		field.setRequired(false);
		return field;
	}

	private Field getBirthDateField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("birthdate");
		field.setDataType(new DateTimeDataType());
		field.setLabel("Date Of Birth");
		field.setRequired(false);
		return field;
	}

	private Field getFatherField(Type type) throws KommetException
	{
		// add a reference field to pigeon
		Field field = new Field();
		field.setApiName("father");
		field.setLabel("Father");
		// the other referenced object is also a pigeon
		field.setDataType(new TypeReference(type));
		return field;
	}
	
	private Field getMotherField(Type obj) throws KommetException
	{
		// add a reference field to pigeon
		Field field = new Field();
		field.setApiName("mother");
		field.setLabel("Mother");
		// the other referenced object is also a pigeon
		field.setDataType(new TypeReference(obj));
		return field;
	}

	public Env getTestEnv() throws KommetException
	{
		Env env = new Env();
		env.setName(testConfig.getTestEnv());
		env.setKID(testConfig.getTestEnvId());
		return env;
	}
	
	public Env getTestEnv2() throws KommetException
	{
		Env env = new Env();
		env.setName(testConfig.getTestEnv2());
		env.setKID(testConfig.getTestEnv2Id());
		return env;
	}
	
	public Env getTestEnv3() throws KommetException
	{
		Env env = new Env();
		env.setName(testConfig.getTestEnv3());
		env.setKID(testConfig.getTestEnv3Id());
		return env;
	}

	public Field getNameField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("name");
		field.setDataType(new TextDataType(100));
		field.setLabel("Name");
		field.setRequired(true);
		return field;
	}

	public Field getAgeField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("age");
		field.setDataType(new NumberDataType(0, Integer.class));
		field.setLabel("Age");
		field.setRequired(true);
		return field;
	}
	
	/**
	 * Configures a full test environment with all basic objects.
	 * @return
	 * @throws KommetException
	 */
	public EnvData configureFullTestEnv() throws KommetException
	{
		EnvData env = getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		// create/clear KTL dir - most tests create types, hence they will need
		// a KTL dir to store views that are created for these types
		File ktlDir = new File(env.getKeetleDir(appConfig.getKeetleDir()));
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		
		// create the dir anew - the page create methods won't do this
		ktlDir.mkdir();
		
		return env;
	}

	public Record getPigeonListView(EnvData env) throws KommetException
	{
		Record view = new Record(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.VIEW_API_NAME)));
		view.setField("name", PIGEON_LIST_VIEW);
		view.setField("path", "some/path");
		view.setField("packageName", PIGEON_LIST_VIEW_PACKAGE);
		view.setField("isSystem", false);
		
		String keetleCode = ViewUtil.wrapViewCode("some KTL code", PIGEON_LIST_VIEW, PIGEON_LIST_VIEW_PACKAGE);
		
		view.setField("keetleCode", keetleCode);
		view.setField("jspCode", ViewUtil.keetleToJSP(keetleCode, appConfig, env));
		view.setField("accessLevel", "Editable");
		
		return view;
	}

	public Record getPigeonListControllerClass(EnvData env) throws KommetException
	{
		String userPackageName = "com.pigeons.controllers";
		String controllerName = PIGEON_LIST_CONTROLLER;
		Record file = new Record(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.CLASS_API_NAME)));
		file.setField("name", controllerName);
		file.setField("packageName", userPackageName);
		file.setField("isSystem", false);
		
		String kollCode = getPigeonControllerCode(controllerName, userPackageName, env);
		AuthData authData = getRootAuthData(env);
		
		file.setField("kollCode", kollCode);
		file.setField("accessLevel", "Editable");
		file.setField("javaCode", classService.getKollTranslator(env).kollToJava(kollCode, true, authData, env));
		
		/*Class cls = new Class();
		cls.setName(controllerName);
		cls.setPackageName(userPackageName);
		cls.setIsSystem(false);
		cls.setKollCode("package com.pigeons.controllers;\nclass " + PIGEON_LIST_CONTROLLER + "{}");
		cls.setAccessLevel("Editable");
		cls.setJavaCode(getPigeonControllerCode(controllerName, userPackageName, env));*/
		
		return file;
	}

	/**
	 * Returns Java code for a test pigeon controller.
	 * @param controllerName
	 * @param packageName
	 * @return
	 */
	private String getPigeonControllerCode(String controllerName, String packageName, EnvData env)
	{
		StringBuilder code = new StringBuilder();
		
		code.append("package " + packageName + ";\n");
		code.append(kommetCompiler.getControllerImportsSection());
		code.append("\n@Controller\n");
		code.append("public class ").append(controllerName).append(" extends " + BaseController.class.getName()).append(" implements ").append(SystemContextAware.class.getName()).append("\n");
		code.append("{\n");
		
		// define action method "pigeonList"
		code.append("@Action\n");
		code.append("public PageData pigeonList()\n{");
		code.append("PageData pageData = new PageData(systemContext.getEnv());\n");
		code.append("pageData.setValue(\"testKey\", \"testValue\");\n");
		code.append("return pageData;\n");
		code.append("}\n");
		
		// define action method "pigeonDetails"
		code.append("@Action\n");
		code.append("public PageData pigeonDetails ()\n{");
		code.append("PageData pageData = new PageData(systemContext.getEnv());\n");
		code.append("pageData.setValue(\"testKey\", \"testValue\");\n");
		code.append("return pageData;\n");
		code.append("}\n");
		
		// add system context setter
		code.append("private SystemContext systemContext;");
		code.append("public void setSystemContext (SystemContext sys)\n{");
		code.append("this.systemContext = sys;");
		code.append("}\n");
		
		code.append("}");
		
		return code.toString();
	}

	public Record getSavedPigeonListAction(EnvData env, DataService typeService) throws KommetException
	{
		Record testAction = new Record(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME)));
		testAction.setField("url", "test/action/name");
		testAction.setField("isSystem", false);
		testAction.setField("controllerMethod", "someMethodName");
		testAction.setField("name", "TestName");
		testAction.setField("isPublic", false);
		
		Record pigeonListView = typeService.save(getPigeonListView(env), getRootAuthData(env), env);
		testAction.setField("view", pigeonListView);
		
		Record pigeonListKollFile = typeService.save(getPigeonListControllerClass(env), getRootAuthData(env), env);
		testAction.setField("controller", pigeonListKollFile);
		
		return typeService.save(testAction, getRootAuthData(env), env);
	}
	
	public Record getTestProfile (String name, EnvData env) throws KommetException
	{
		Record profileRecord = new Record(env.getType(KeyPrefix.get(KID.PROFILE_PREFIX)));
		profileRecord.setField("name", name);
		profileRecord.setField("label", name);
		profileRecord.setField(Field.CREATEDDATE_FIELD_NAME, new Date());
		profileRecord.setField("systemProfile", false);
		return profileRecord;
	}
	
	public Profile getTestProfileObject (String name, EnvData env) throws KommetException
	{
		Profile profile = new Profile();
		profile.setName(name);
		profile.setLabel(name);
		profile.setSystemProfile(false);
		return profileService.save(profile, getRootAuthData(env), env);
	}
	
	public static Record getTestUser (String userName, String email, Record profile, EnvData env) throws KommetException
	{	
		Record userRecord = new Record(env.getType(KeyPrefix.get(KID.USER_PREFIX)));
		userRecord.setField("userName", userName);
		userRecord.setField("email", email);
		userRecord.setField("profile", profile);
		userRecord.setField("password", "any-password");
		userRecord.setField("timezone", "GMT");
		userRecord.setField("locale", "EN_US");
		userRecord.setField("isActive", true);
		userRecord.setField(Field.CREATEDDATE_FIELD_NAME, new Date());
		return userRecord;
	}
	
	public User getTestUser (String userName, String email, Profile profile, EnvData env) throws KommetException
	{
		return getTestUser(userName, email, "admin123", profile, env);
	}
	
	public User getTestUser (String userName, String email, String password, Profile profile, EnvData env) throws KommetException
	{	
		User user = new User();
		user.setUserName(userName);
		user.setEmail(email);
		user.setProfile(profile);
		user.setPassword(MiscUtils.getSHA1Password(password));
		user.setTimezone("GMT");
		user.setLocale("EN_US");
		user.setIsActive(true);
		return userService.save(user, getRootAuthData(env), env);
	}

	public static Type getCompanyType() throws KommetException
	{
		String randomSuffix = String.valueOf((new Random()).nextInt(10000));
		Type type = new Type();
		type.setApiName("TestType" + randomSuffix);
		type.setLabel("TestType" + randomSuffix);
		type.setPluralLabel("TestTypes" + randomSuffix);
		
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setRequired(true);
		nameField.setDataType(new TextDataType(30));
		type.addField(nameField);
		
		Field yearField = new Field();
		yearField.setApiName("year");
		yearField.setLabel("Year");
		yearField.setRequired(false);
		yearField.setDataType(new NumberDataType(0, Integer.class));
		type.addField(yearField);
		
		return type;
	}

	public void addChildrenRelationship(Type pigeonType, EnvData env) throws KommetException
	{
		Field field = new Field();
		field.setApiName("children");
		field.setLabel("Children");
		field.setDataType(new InverseCollectionDataType(pigeonType, "father"));
		pigeonType.addField(field);
		
		dataService.createField(field, env);
	}
	
	public Type createProjectType(EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName("Project");
		type.setPackage("com.test");
		type.setLabel("Project");
		type.setPluralLabel("Projects");
		type.setBasic(false);
		
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(30));
		nameField.setRequired(true);
		type.addField(nameField);
		
		Field activeField = new Field();
		activeField.setApiName("isActive");
		activeField.setLabel("Is Active");
		activeField.setDataType(new BooleanDataType());
		activeField.setRequired(false);
		type.addField(activeField);
		
		Field budgetField = new Field();
		budgetField.setApiName("budget");
		budgetField.setLabel("Budget");
		budgetField.setDataType(new NumberDataType(2, Double.class));
		budgetField.setRequired(false);
		type.addField(budgetField);
		
		Field statusField = new Field();
		statusField.setApiName("status");
		statusField.setLabel("Status");
		statusField.setDataType(new EnumerationDataType("Open\nIn Progress\nClosed"));
		statusField.setRequired(false);
		type.addField(statusField);
		
		return dataService.createType(type, env);
	}
}
