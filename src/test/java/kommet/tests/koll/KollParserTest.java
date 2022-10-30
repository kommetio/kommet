/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.koll;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.keetle.BaseController;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.koll.KollUtil;
import kommet.koll.SimpleKollTranslator;
import kommet.koll.annotations.QueriedTypes;
import kommet.koll.annotations.SystemContextVar;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class KollParserTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	ClassService classService;
	
	@Inject
	DataService dataService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	KommetCompiler compiler;
	
	@Test
	public void testKollParser() throws KommetException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		String kollCode = "package one.two.three;\npublic class TestClass\n{\n}";
		Class file = new Class();
		file.setKollCode(kollCode);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		SimpleKollTranslator kollParser = classService.getKollTranslator(env);
		Class parsedFile = kollParser.kollToJava(file, true, authData, env);
		assertTrue("Invalid java code " + parsedFile.getJavaCode(), parsedFile.getJavaCode().startsWith("package kommet.envs.env" + env.getId() + ".one.two.three;\n"));
		assertTrue(parsedFile.getJavaCode().contains("import " + QueriedTypes.class.getName() + ";\n"));
		assertTrue(parsedFile.getJavaCode().contains("@" + SystemContextVar.class.getName() + "(\""));
		
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		String query = "select id from " + pigeonType.getQualifiedName() + " where age > 2";
		
		String classCode = "package com.test;\n\n";
		classCode += "import " + pigeonType.getQualifiedName() + ";\n";
		classCode += "import " + KommetException.class.getName() + ";\n";
		classCode += "public class TestClassOne extends " + BaseController.class.getName() + " { ";
		
		// make sure that DAL queries have been interpreted
		classCode += "private " + pigeonType.getApiName() + " bird;\n";
		classCode += "public void testMethod2() { Pigeon pigeon; }";
		classCode += "public void testMethod() throws KommetException { java.util.List<Pigeon> pigeons = {" + query + "}; ";
		classCode += "java.util.List<Pigeon> records;\n";
		classCode += "records = {" + query + "}; ";
		classCode += "TestClassOne subclass = new TestClassOne(); ";
		classCode += "Pigeon singlePigeon = {" + query + "};\n";
		classCode += "singlePigeon = {" + query + "};\n";
		classCode += "}";
		classCode += "public void methodWithQuotes() { String queryString = \"{select id from MyType}\"; String b = \"\\\"\"; }";
		classCode += "}";
		
		String mockSysVar = "sysCtx" + (new Random()).nextInt(1000000);
		
		String parsedCode = KollUtil.replaceDALQueries(classCode, mockSysVar);
		assertTrue("Invalid parsed code\n" + parsedCode, parsedCode.contains("List<Pigeon> pigeons = " + mockSysVar + "(\"" + query + "\");"));
		assertTrue(parsedCode.contains("String queryString = \"{select id from MyType}\";"));
		
		Class classWithQuery = new Class();
		classWithQuery.setKollCode(classCode);
		classWithQuery.setIsSystem(false);
		classWithQuery.setName("TestClassOne");
		classWithQuery.setPackageName("com.test");
		
		classWithQuery = classService.fullSave(classWithQuery, dataService, dataHelper.getRootAuthData(env), env);
		
		// fetch the class with java code
		classWithQuery = classService.getClass(classWithQuery.getId(), env);
		assertTrue(classWithQuery.getJavaCode().contains(".query(\"" + query + "\");"));
		assertTrue(classWithQuery.getJavaCode().contains("String queryString = \"{select id from MyType}\";"));
		
		// now create instance of the new class
		java.lang.Class<?> compiledClass = compiler.getClass("com.test.TestClassOne", true, env);
		assertNotNull(compiledClass);
		
		compiledClass.newInstance();
		
		
		// different class
		
		classCode = "package com.test;\n\n";
		classCode += "import " + pigeonType.getQualifiedName() + ";\n";
		classCode += "import " + KommetException.class.getName() + ";\n";
		classCode += "import " + SystemContextVar.class.getName() + ";\n";
		classCode += "@" + SystemContextVar.class.getSimpleName() + "(\"rm\")\n";
		classCode += "public class TestClassTwo extends " + BaseController.class.getName() + " { ";
		
		// make sure that DAL queries have been interpreted
		classCode += "private " + pigeonType.getApiName() + " bird;\n";
		classCode += "public void testMethod2() { Pigeon pigeon; }";
		classCode += "public void testMethod() throws KommetException { java.util.List<Pigeon> pigeons = {" + query + "}; ";
		classCode += "java.util.List<Pigeon> records;\n";
		classCode += "records = {" + query + "}; ";
		classCode += "TestClassOne subclass = new TestClassOne(); ";
		classCode += "Pigeon singlePigeon = {" + query + "};\n";
		classCode += "singlePigeon = {" + query + "};\n";
		classCode += "}";
		classCode += "public void methodWithQuotes() { String queryString = \"{select id from MyType}\"; }";
		classCode += "}";
		
		classWithQuery = new Class();
		classWithQuery.setKollCode(classCode);
		classWithQuery.setIsSystem(false);
		classWithQuery.setName("TestClassTwo");
		classWithQuery.setPackageName("com.test");
		
		classWithQuery = classService.fullSave(classWithQuery, dataService, dataHelper.getRootAuthData(env), env);
		
		// fetch the class with java code
		classWithQuery = classService.getClass(classWithQuery.getId(), env);
		assertTrue(classWithQuery.getJavaCode().contains(".query(\"" + query + "\");"));
		assertTrue(classWithQuery.getJavaCode().contains("String queryString = \"{select id from MyType}\";"));
		
		// now create instance of the new class
		compiledClass = compiler.getClass("com.test.TestClassTwo", true, env);
		assertNotNull(compiledClass);
	}
}
