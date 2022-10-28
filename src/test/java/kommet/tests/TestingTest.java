/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.testing.TestError;
import kommet.testing.TestResults;
import kommet.testing.TestService;
import kommet.utils.MiscUtils;

public class TestingTest extends BaseUnitTest
{
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	ClassService classService;
	
	@Inject
	DataService dataService;
	
	@Inject
	TestService testService;
	
	@Test
	public void testSaveClass() throws KommetException
	{
		/*EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		String clsName = "SampleTest";
		String method = "runTest";
		String packageName = "rm.sampletest";
		String fullClsName = packageName + "." + clsName;
		
		Class file = getTestClass(clsName, packageName, method, dataHelper.getRootAuthData(env), env);
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertNotNull(file.getId());
		
		// create environment
		TestResults results = testService.run(fullClsName, MiscUtils.toList(method), env);
		
		if (!results.getErrors().isEmpty())
		{
			for (TestError err : results.getErrors())
			{
				if (err.getException() != null)
				{
					err.getException().printStackTrace();
					System.out.println("\n\n--------------------------------------------------------------\n\n");
				}
			}
			assertTrue("Errors: " + MiscUtils.implode(results.getErrors(), "\n"), false);
		}
		
		assertNotNull(results.getTestEnv());*/
	}
	
	private Class getTestClass(String clsName, String packageName, String method, AuthData authData, EnvData env) throws KommetException
	{
		Class file = new Class();
		file.setIsSystem(false);
		
		String kollCode = getTestClassCode(clsName, packageName, method);
		
		file.setKollCode(kollCode);
		file.setName(clsName);
		file.setPackageName(packageName);
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(kollCode, true, authData, env));
		file.setAccessLevel("Editable");
		file.setIsDraft(false);
		return file;
	}

	private String getTestClassCode(String clsName, String packageName, String method)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("package " + packageName + ";\n\n");
		sb.append("import " + kommet.testing.Test.class.getName() + ";\n\n");
		
		sb.append("@Test\npublic class " + clsName + "\n{\n");
		
		sb.append("public void ").append(method).append("()\n{\n");
		
		// end method
		sb.append("}\n");
		
		// end class body
		sb.append("}");
		
		return sb.toString();
	}
}
