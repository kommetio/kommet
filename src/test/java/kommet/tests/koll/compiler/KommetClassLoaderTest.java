/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.koll.compiler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class KommetClassLoaderTest extends BaseUnitTest
{
	@Inject
	KommetCompiler compiler;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	ClassService kollService;
	
	@Test
	public void testClassLoadersForTwoEnvs() throws KommetException, MalformedURLException, ClassNotFoundException, SecurityException, NoSuchMethodException
	{
		EnvData env1 = dataHelper.getTestEnvData(false);
		EnvData env2 = dataHelper.getTestEnv2Data(false);
		
		// compile first class containing method testMethod
		String code = "package kommet.tests;\n";
		code += "public class TestClass { ";
		code += "public String testMethod() { return \"kamila\"; }";
		code += "}";
		
		Class file = new Class();
		file.setName("TestClass");
		file.setJavaCode(code);
		file.setPackageName("kommet.tests");
		
		compiler.compile(file, env1);
		
		// compile second class with the same name and package, but different method
		code = "package kommet.tests;\n";
		code += "public class TestClass { ";
		code += "public String testMethod2() { return \"kamila\"; }";
		code += "}";
		
		file = new Class();
		file.setName("TestClass");
		file.setJavaCode(code);
		file.setPackageName("kommet.tests");
		
		compiler.compile(file, env2);
		
		// get classes from their respective envs
		java.lang.Class<?> class1 = compiler.getClass("kommet.tests.TestClass", false, env1);
		java.lang.Class<?> class2 = compiler.getClass("kommet.tests.TestClass", false, env2);
		
		assertNotNull(class1);
		assertNotNull(class2);
		
		assertTrue(class1.getMethod("testMethod") != null);
		assertTrue(class2.getMethod("testMethod2") != null);
		
		try
		{
			class2.getMethod("testMethod");
			fail("Method 'testMethod' should not be found in class. It was compiled for another env");
		}
		catch (NoSuchMethodException e)
		{
			// expected
		}
		
		try
		{
			class1.getMethod("testMethod2");
			fail("Method 'testMethod' should not be found in class. It was compiled for another env");
		}
		catch (NoSuchMethodException e)
		{
			// expected
		}
	}
}
