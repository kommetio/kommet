/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.koll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.PageData;
import kommet.data.DataService;
import kommet.data.Env;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.GenericAction;
import kommet.koll.ClassCompilationException;
import kommet.koll.ClassDao;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.koll.InvalidClassCodeException;
import kommet.koll.KollUtil;
import kommet.koll.annotations.Action;
import kommet.koll.annotations.Auth;
import kommet.koll.annotations.Controller;
import kommet.koll.annotations.Public;
import kommet.koll.annotations.ResponseBody;
import kommet.koll.annotations.Rest;
import kommet.koll.annotations.ReturnsFile;
import kommet.koll.annotations.View;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class KollTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	ClassDao classDao;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	ClassService classService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@Test
	public void testSaveClass() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Class file = getTestKollFile(env);
		file = classDao.save(file, dataHelper.getRootAuthData(env), env);
		assertNotNull(file.getId());
		
		// now find the inserted file
		ClassFilter filter = new ClassFilter();
		filter.setSimpleName(file.getName());
		List<Class> files = classDao.find(filter, env);
		assertNotNull(files);
		assertEquals(1, files.size());
		
		// find again, this time by full qualified name
		filter = new ClassFilter();
		filter.setQualifiedName(file.getPackageName() + "." + file.getName());
		files = classDao.find(filter, env);
		assertNotNull(files);
		assertEquals("Searching file by qualified name failed, found " + files.size() + " files instead of 1", 1, files.size());
		
		// find again, this time by full qualified name
		filter = new ClassFilter();
		filter.setContentLike("class Test");
		files = classDao.find(filter, env);
		assertNotNull(files);
		assertEquals("Searching file by content failed, found " + files.size() + " files instead of 1", 1, files.size());
		
		// testing finding by ID
		Class fetchedFile = classDao.get(file.getId(), env);
		assertNotNull(fetchedFile);
		assertEquals(file.getJavaCode(), fetchedFile.getJavaCode());
		
		// make sure it's not possible to create two files with the same package and name
		Class duplicateFile = new Class();
		duplicateFile.setIsSystem(false);
		
		String kollCode = "package com.name; test";
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		duplicateFile.setKollCode(kollCode);
		duplicateFile.setJavaCode(classService.getKollTranslator(env).kollToJava(kollCode, true, authData, env));
		duplicateFile.setAccessLevel("Editable");
		duplicateFile.setName(file.getName());
		duplicateFile.setPackageName(file.getPackageName());
		
		// now test fetching file using the getKollFile method
		file = classService.getClass(file.getId(), env);
		assertNotNull(file.getId());
		assertNotNull(file.getCreatedBy());
		assertNotNull(file.getCreatedDate());
		assertNotNull(file.getLastModifiedBy());
		assertNotNull(file.getLastModifiedDate());
		
		try
		{
			classDao.save(duplicateFile, dataHelper.getRootAuthData(env), env);
			fail("Saving file with duplicate name-package combination should fail");
		}
		catch (UniqueCheckViolationException e)
		{
			// expected
		}
		
		testDeduceClassName(env);
		testRewriteClassNameFromCode(env);
		testAuthHandler(env);
	}
	
	private void testAuthHandler(EnvData env) throws KommetException
	{
		Class handlerFile = new Class();
		handlerFile.setName("CustomHandler");
		handlerFile.setPackageName("com.test");
		handlerFile.setIsSystem(false);
		
		String method = "public kommet.data.KID check(java.lang.String token) throws kommet.data.KommetException { return kommet.data.KID.get(\"0040000000005\"); };";
		
		handlerFile.setKollCode("package " + handlerFile.getPackageName() + ";\n\npublic class " + handlerFile.getName() + " extends kommet.auth.AuthHandler\n{\n" + method + " }");
		handlerFile = classService.fullSave(handlerFile, dataService, AuthData.getRootAuthData(env), env);
		assertNotNull(handlerFile.getId());
		
		Class crlrFile = new Class();
		crlrFile.setName("TestActionController");
		crlrFile.setPackageName("com.test");
		crlrFile.setIsSystem(false);
		crlrFile.setKollCode(getAuthActionCode("OK", handlerFile));
		crlrFile = classService.fullSave(crlrFile, dataService, AuthData.getRootAuthData(env), env);
		assertNotNull(crlrFile.getId());
		
		// get generic action
		GenericAction sampleAction = env.getGenericAction("testrest");
		assertNotNull(sampleAction);
		assertNotNull(sampleAction.getAuthHandler(compiler, env));
		assertNotNull(sampleAction.getAuthHandlerConfig());
		assertNotNull("token_header", sampleAction.getAuthHandlerConfig().getTokenHeader());
		assertTrue(KID.get("0040000000005").equals(sampleAction.getAuthHandler(compiler, env).check("test")));
		
		// update controller file
		crlrFile.setKollCode(getAuthActionCode("YEP", handlerFile));
		sampleAction = env.getGenericAction("testrest");
		assertNotNull(sampleAction);
		assertNotNull(sampleAction.getAuthHandler(compiler, env));
		assertTrue(KID.get("0040000000005").equals(sampleAction.getAuthHandler(compiler, env).check("test")));
		
		// update handler
		method = "public kommet.data.KID check(java.lang.String token) throws kommet.data.KommetException { return kommet.data.KID.get(\"0040000000006\"); };";
		handlerFile.setKollCode("package " + handlerFile.getPackageName() + ";\n\npublic class " + handlerFile.getName() + " extends kommet.auth.AuthHandler\n{\n" + method + " }");
		handlerFile = classService.fullSave(handlerFile, dataService, AuthData.getRootAuthData(env), env);
		
		sampleAction = env.getGenericAction("testrest");
		assertNotNull(sampleAction);
		assertNotNull(sampleAction.getAuthHandler(compiler, env));
		assertEquals("0040000000006", sampleAction.getAuthHandler(compiler, env).check("test").getId());
	}
	
	private static String getAuthActionCode (String returnVal, Class handlerFile)
	{
		// create generic action that uses this handler
		String imports = "import " + Controller.class.getName() + ";\n";
		imports += "import " + Auth.class.getName() + ";\n";
		imports += "import " + Rest.class.getName() + ";\n";
		imports += "import " + ResponseBody.class.getName() + ";\n";
		imports += "import " + Public.class.getName() + ";\n";
		imports += "import " + PageData.class.getName() + ";\n";
		imports += "import " + View.class.getName() + ";\n";
		imports += "import " + BaseController.class.getName() + ";\n";
		
		String code = "package com.test;\n";
		String classCode = "@Controller\npublic class TestActionController ";
		classCode += " extends " + BaseController.class.getSimpleName();
		classCode += "{ ";
		classCode += "@Rest(url = \"testrest\")\n";
		classCode += "@ResponseBody\n";
		classCode += "@Auth(handler = \"" + handlerFile.getQualifiedName() + "\", header = \"token_header\")\n";
		classCode += "public java.lang.String testAction() { return \"" + returnVal + "\"; }";
		classCode += "}";
		
		code += imports + classCode;
		
		return code;
	}

	private void testRewriteClassNameFromCode(EnvData env) throws KommetException
	{
		Class file = new Class();
		file.setName("MyClass");
		file.setPackageName("kommet");
		file.setIsSystem(false);
		
		String method = "public void test() throws kommet.data.KommetException { String id = \"aa\"; kommet.basic.User person = { select id from User where id = '#id' }; String a = \"\\\"\"; };";
		
		file.setKollCode("package " + file.getPackageName() + ";\n\npublic class " + file.getName() + "{\nprivate String name = \"public class " + file.getName() + "\"; private String a = \"\\\"\"; \n" + method + " }");
		file = classService.fullSave(file, dataService, AuthData.getRootAuthData(env), env);
		assertNotNull(file.getId());
		
		String newClassName = "FunkyController";
		String packageName = "com.pk.one";
		
		String newCode = file.getKollCode().replace("public class " + file.getName(), "public class " + newClassName);
		newCode = newCode.replace("package " + file.getPackageName(), "package " + packageName);
		
		// change class name and package in file code
		file.setKollCode(newCode);
		
		classService.fullSave(file, dataService, true, AuthData.getRootAuthData(env), env);
		file = classService.getClass(file.getId(), env);
		
		assertEquals(newClassName, file.getName());
		assertEquals(packageName, file.getPackageName());
	}

	private void testDeduceClassName(EnvData env) throws KommetException
	{
		String code = "package my.pack; public class MyClass {}";
		Class cls = KollUtil.getClassFromCode(code, env);
		assertEquals("my.pack", cls.getPackageName());
		assertEquals("MyClass", cls.getName());
	}

	private void testSavingControllerWithActions(EnvData env) throws KommetException
	{
		String imports = "import " + Controller.class.getName() + ";\n";
		imports += "import " + Action.class.getName() + ";\n";
		imports += "import " + Rest.class.getName() + ";\n";
		imports += "import " + ReturnsFile.class.getName() + ";\n";
		imports += "import " + PageData.class.getName() + ";\n";
		imports += "import " + BaseController.class.getName() + ";\n";
		
		Class file = new Class();
		file.setName("TestControllerOne");
		file.setPackageName("com.test");
		assertTrue(compiler.compile(file, env).isSuccess());
		
		String code = "package com.test;\n";
		
		String classCode = "@Controller\npublic class TestControllerOne extends " + BaseController.class.getSimpleName() + " { ";
		classCode += "@Action(url = \"get/names\")\n";
		classCode += "public String getText() { return \"kamila\"; }";
		classCode += "}";
		
		String kollCode = code + imports + classCode;
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(kollCode, true, authData, env));
		file.setKollCode(kollCode);
		file.setIsSystem(false);
		
		CompilationResult res = compiler.compile(file, env);
		assertTrue(res.getDescription(), res.isSuccess());
		
		try
		{
			classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
			fail("Saving controller with invalid action return type should fail");
		}
		catch (KommetException e)
		{
			// expected exception
			assertTrue(e.getMessage().startsWith("Method getText is annotated with @" + Action.class.getSimpleName()));
		}
		
		classCode = "@Controller\npublic class TestControllerOne extends " + BaseController.class.getSimpleName() + " { ";
		classCode += "@Action(url = \"get/names\")\n";
		classCode += "public PageData getText() { return null; }";
		classCode += "}";
		
		// change file definition to extend basic controller
		file.setJavaCode(code + imports + classCode);
		file.setKollCode(file.getJavaCode());
		
		// this compilation should be successful
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertNotNull(file.getId());
		
		// now check saving controller action with @ReturnsFile
		classCode = "@Controller\npublic class TestControllerOne extends " + BaseController.class.getSimpleName() + " { ";
		classCode += "@Action(url = \"get/names\")\n";
		classCode += "@ReturnsFile\n";
		classCode += "public PageData getText() { return null; }";
		classCode += "}";
		
		// change file definition to extend basic controller
		file.setJavaCode(code + imports + classCode);
		file.setKollCode(file.getJavaCode());
		
		try
		{
			classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
			fail("Saving controller with invalid action return type should fail");
		}
		catch (KommetException e)
		{
			// expected exception
			assertTrue(e.getMessage().startsWith("Method getText is annotated with @" + ReturnsFile.class.getSimpleName()));
		}
		
		classCode = "@Controller\npublic class TestControllerOne extends " + BaseController.class.getSimpleName() + " { ";
		classCode += "@Action(url = \"get/names\")\n";
		classCode += "@ReturnsFile\n";
		classCode += "public byte[] getText() { return new byte[0]; }";
		classCode += "}";
		
		// change file definition to extend basic controller
		file.setJavaCode(code + imports + classCode);
		file.setKollCode(file.getJavaCode());
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertNotNull(file.getId());
	}

	/**
	 * Test different aspects of saving a controller class.
	 * @param env
	 * @throws KommetException
	 */
	private void testSavingController(EnvData env) throws KommetException
	{
		String imports = "import " + Controller.class.getName() + ";\n";
		imports += "import " + Action.class.getName() + ";\n";
		imports += "import " + Rest.class.getName() + ";\n";
		imports += "import " + BaseController.class.getName() + ";\n";
		
		Class file = new Class();
		file.setName("TestController");
		file.setPackageName("com.test.incorrect");
		assertTrue(compiler.compile(file, env).isSuccess());
		
		String code = "package com.test;\n";
		String classCode = "@Controller\npublic class TestController { ";
		classCode += "@Rest(url = \"get/name\")\n";
		classCode += "public String getText() { return \"kamila\"; }";
		classCode += "}";
		
		String kollCode = code + imports + classCode;
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(kollCode, true, authData, env));
		file.setKollCode(kollCode);
		file.setIsSystem(false);
		
		CompilationResult res = compiler.compile(file, env);
		assertTrue(res.getDescription(), res.isSuccess());
		
		try
		{
			classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
			fail("Saving controller class with invalid package name in KOLL code should fail");
		}
		catch (InvalidClassCodeException e)
		{
			// expected exception
			assertTrue(e.getMessage().startsWith("Package in the class code"));
		}
		
		file.setPackageName("com.test");
		
		try
		{
			classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
			fail("Saving controller class that does not extend " + BaseController.class.getSimpleName() + " should fail");
		}
		catch (KommetException e)
		{
			// expected exception
			assertTrue(e.getMessage().startsWith("Controller class " + file.getName() + " does not extend " + BaseController.class.getSimpleName()));
		}
		
		// change file definition to extend basic controller
		file.setJavaCode(code + imports + classCode.replaceAll("TestController", "TestController extends " + BaseController.class.getSimpleName()));
		file.setKollCode(file.getJavaCode());
		
		// this compilation should be successful
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertNotNull(file.getId());
	}

	private Class getTestKollFile(EnvData env) throws KommetException
	{
		Class file = new Class();
		file.setIsSystem(false);
		file.setJavaCode("test code");
		file.setName("TestName");
		file.setPackageName("kommet.test");
		file.setKollCode("package some.packag.name;\npublic class Test {}");
		file.setAccessLevel("Editable");
		file.setIsDraft(false);
		return file;
	}
	
	@Test
	public void compileKollFileTest() throws KommetException, MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		
		String packageName = "kommet.tests";
		
		String code = "package " + MiscUtils.userToEnvPackage(packageName, env) + ";\n";
		code += "public class CompilationTest { ";
		code += "public String getText() { return \"kamila\"; }";
		code += "}";
		
		Class file = new Class();
		file.setName("CompilationTest");
		file.setJavaCode(code);
		file.setPackageName("kommet.tests");
		
		CompilationResult compilationResult = compiler.compile(file, env);
		
		if (!compilationResult.isSuccess())
		{
			fail("Compilation failed: " + compilationResult.getDescription());
		}
		
		// now instantiate the compiled class
		Object instance = classService.instantiate(file, env);
		
		// call test method
		Method method = instance.getClass().getMethod("getText");
		Object result = method.invoke(instance);
		assertEquals("kamila", result);
	}
	
	@Test
	public void testCompilationWithErrors() throws KommetException, MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		
		String code = "package kommet.tests;\n";
		code += "public class CompilationTest { ";
		
		// intended syntax error here
		code += "publiv String getText() { return \"kamila\"; }";

		code += "}";
		
		Class file = new Class();
		file.setName("CompilationTest");
		file.setJavaCode(code);
		file.setPackageName("kommet.tests");
		
		CompilationResult result = compiler.compile(file, env);
		
		assertFalse("Compilation succeeded though it should have failed", result.isSuccess());
	}
	
	@Test
	public void testUsingClassesFromDifferentClassloaders() throws KommetException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		String packageName = "kommet.tests";
		
		String code = "package " + MiscUtils.userToEnvPackage(packageName, env) + ";\n";
		// reference a file from a different class loader
		code += "import " + Env.class.getName() + ";\n";
		code += "public class CompilationTest { ";
		code += "public Env getEnv() { return new Env(); }";
		code += "}";
		
		Class file = new Class();
		file.setName("CompilationTest");
		file.setJavaCode(code);
		file.setPackageName("kommet.tests");
		
		CompilationResult compilationResult = compiler.compile(file, env);
		
		if (!compilationResult.isSuccess())
		{
			fail("Compilation failed: " + compilationResult.getDescription());
		}
		
		compiler.resetClassLoader(env);
		
		// now instantiate the compiled class
		Object instance = classService.instantiate(file, env);
		
		// call test method
		assertEquals(MiscUtils.userToEnvPackage("kommet.tests.CompilationTest", env), instance.getClass().getName());
		
		Method method = instance.getClass().getMethod("getEnv");
		Object result = method.invoke(instance);
		assertEquals(Env.class.getName(), result.getClass().getName());
		PropertyUtils.setProperty(result, "name", "some name");
		
		testSavingController(env);
		testSavingControllerWithActions(env);
		
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		testAutoConvertQualifiedNames(pigeonType, env, file);
		// testAutoConvertDAL(pigeonType, env, file);
	}
	
	/**
	 * Checks that qualified type and package names are automatically converted to env-specific names.
	 * @param env
	 * @param existingClass
	 * @throws KommetException
	 */
	private void testAutoConvertQualifiedNames(Type pigeonType, EnvData env, Class existingClass) throws KommetException
	{	
		Class file = new Class();
		file.setName("TestClass");
		file.setPackageName("com.test");
		assertTrue(compiler.compile(file, env).isSuccess());
		
		String pigeonQuery = pigeonType.getApiName() + " rec = { select id from " + pigeonType.getQualifiedName() + " limit 1};";
		
		String classCode = "package com.test;\n\n";
		classCode += "import " + pigeonType.getQualifiedName() + ";\n";
		classCode += "import " + existingClass.getQualifiedName() + ";\n\n";
		classCode += "public class TestClass { ";
		
		// make sure that all env types are automatically imported and can be used
		classCode += "private " + pigeonType.getApiName() + " bird;\n";
		classCode += "private " + existingClass.getName() + " classInstance;\n";
		classCode += "public void testMethod() { " + existingClass.getName() + " myvar; }";
		
		// add embedded class
		classCode += "\nclass PrivateClass {\n";
		// add disallowed pigeon query (queries are not allowed in inner classes)
		classCode += pigeonQuery;
		classCode += "}\n";
		
		classCode += "}";
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		file.setKollCode(classCode);
		
		try
		{
			file.setJavaCode(classService.getKollTranslator(env).kollToJava(classCode, true, authData, env));
			fail("Converting to Java code should fail because an inner class contains a DAL query");
		}
		catch (IllegalArgumentException e)
		{
			assertEquals("DAL query cannot be placed within an inner class", e.getMessage());
		}
		
		classCode = classCode.replace(pigeonQuery, "");
		
		// remove the DAL query from code
		file.setKollCode(classCode);
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(classCode, true, authData, env));
		file.setIsSystem(false);
		
		CompilationResult res = compiler.compile(file, env);
		assertTrue(res.getDescription(), res.isSuccess());
		
		classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
	}
	
	private void testAutoConvertDAL(Type pigeonType, EnvData env, Class existingClass) throws KommetException
	{	
		Class file = new Class();
		file.setName("TestClass");
		file.setPackageName("com.test");
		assertTrue(compiler.compile(file, env).isSuccess());
		
		String classCode = "package com.test;\n\n";
		classCode += "import " + pigeonType.getQualifiedName() + ";\n";
		classCode += "import " + BaseController.class.getName() + ";\n";
		classCode += "import " + existingClass.getQualifiedName() + ";\n\n";
		classCode += "import " + java.util.List.class.getName() + ";\n\n";
		classCode += "public class TestClassOne extends " + BaseController.class.getSimpleName() + " { ";
		
		// make sure that all env types are automatically imported and can be used
		classCode += "private " + pigeonType.getApiName() + " bird;\n";
		classCode += "private " + existingClass.getName() + " classInstance;\n";
		classCode += "public void testMethod() { \"abc\"; List<Record> records = [select id from User]; }";
		classCode += "}";
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		file.setKollCode(classCode);
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(classCode, true, authData, env));
		file.setIsSystem(false);
		
		CompilationResult res = compiler.compile(file, env);
		assertTrue(res.getDescription(), res.isSuccess());
		
		classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
	}

	@Test
	public void testValidateFileBeforeSaving() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		String code = "package some.sample.pack;\n\npublic class CompilationTest { ";
		code += "public String getText() { return \"kamila\"; }";
		code += "}";
		
		Class file = new Class();
		file.setName("CompilationTest");
		file.setKollCode(code);
		file.setIsSystem(false);
		file.setPackageName("com.test.packagename.eee");
		file.setJavaCode("package " + file.getPackageName() + ".blaa; " + code);
		
		try
		{
			classService.save(file, dataHelper.getRootAuthData(env), env);
			fail("Saving Koll file whose Java code does declares a different package than the package property of the file should fail");
		}
		catch (ClassCompilationException e)
		{
			assertTrue(e.getMessage().startsWith("Package declared in the Java code"));
		}
		
		file.setJavaCode("package " + MiscUtils.userToEnvPackage(file.getPackageName(), env) + "; " + code);
		file = classService.save(file, dataHelper.getRootAuthData(env), env);
		assertNotNull(file.getId());
	}
	
	@Test
	public void testGetActionFromControllerClass() throws KommetException, MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		String code = "package " + MiscUtils.userToEnvPackage("kommet.tests", env) + ";\n";
		code += "public class TestController { ";
		code += "public String getText() { return \"kamila\"; }";
		code += "}";
		
		String imports = "import " + Controller.class.getName() + ";\n";
		imports += "import " + Action.class.getName() + ";\n";
		
		Class file = new Class();
		file.setName("TestController");
		file.setJavaCode(code);
		file.setPackageName("kommet.tests");
		assertTrue(compiler.compile(file, env).isSuccess());
		
		try
		{
			// try to extract action from a non-existing annotation
			classService.getActionMethod(file, "getText", env);
			fail("Getting action should fail because the controller is not annotated with @Controller");
		}
		catch (KommetException e)
		{
			assertTrue("Invalid error message: " + e.getMessage(), e.getMessage().contains("Cannot get action method from class"));
		}
		
		// create the class loader anew, because the old one contained the previous compiled
		// version of the class
		compiler.resetClassLoader(env);
		
		code = "package kommet.tests;\n";
		String classCode = "public class TestController { ";
		classCode += "@Action\n";
		classCode += "public String getText() { return \"kamila\"; }";
		classCode += "}";
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(code + imports + classCode, true, authData, env));
		assertTrue(compiler.compile(file, env).isSuccess());
		
		try
		{
			classService.getActionMethod(file, "getText", env);
			fail("Getting action should fail because the controller is not annotated with @Controller");
		}
		catch (KommetException e)
		{
			// expected
		}
		
		// create the class loader anew, because the old one contained the previous compiled
		// version of the class
		compiler.resetClassLoader(env);
		
		classCode = "@Controller\n" + classCode;
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(code + imports + classCode, true, authData, env));
		assertTrue(compiler.compile(file, env).isSuccess());
		
		// create the class loader anew, because the old one contained the previous compiled
		// version of the class
		compiler.resetClassLoader(env);
		
		Method actionMethod = classService.getActionMethod(file, "getText", env);
		assertNotNull(actionMethod);
		assertEquals("getText", actionMethod.getName());
	}
}
