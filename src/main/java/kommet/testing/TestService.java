/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.testing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.data.DataService;
import kommet.data.Env;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.EnvFilter;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;

@Service
public class TestService
{
	@Inject
	EnvService envService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ClassService classService;
	
	@Inject
	UserService userService;
	
	@Inject
	DataService dataService;
	
	public TestResults run (String className, List<String> methods, EnvData env) throws KommetException
	{
		TestResults results = new TestResults();
		
		if (methods == null)
		{
			methods = new ArrayList<String>();
		}
		
		String methodList = MiscUtils.implode(methods, ", ");
		
		Class<?> testClass = null;
		try
		{
			testClass = compiler.getClass(className, true, env);
		}
		catch (ClassNotFoundException e)
		{
			results.addError(className, methodList, "Class not found");
		}
		catch (KommetException e)
		{
			results.addError(className, methodList, e.getMessage(), e);
			return results;
		}
		
		if (!testClass.isAnnotationPresent(Test.class))
		{
			results.addError(className, methodList, "Class " + testClass + " not annotated with @" + Test.class.getSimpleName());
			return results;
		}
		
		for (String method : methods)
		{
			Method testMethod = MiscUtils.getMethodByName(testClass, method);
			if (testMethod == null)
			{
				results.addError(className, method, "Method " + method + " not found on class " + className);
				return results;
			}
		}
		
		try
		{
			Object testInstance = testClass.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			results.addError(className, methodList, "Error instantiating test class " + className + ": " + e.getMessage(), e);
			return results;
		}
		
		kommet.basic.Class testClassFile = null;
		try
		{
			testClassFile = classService.getClass(className, env);
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			results.addError(className, methodList, "Error reading test class from database: " + e.getMessage(), e);
			return results;
		}
		
		// the name of the test environment is always the same as the name of the main env
		// only with "[testing]-" prefix
		EnvFilter filter = new EnvFilter();
		filter.setName("[testing]-" + env.getName());
		List<Env> testEnvs = envService.find(filter, envService.getMasterEnv());
		
		if (testEnvs.isEmpty())
		{
			results.addError(className, methodList, "Test environment not found, must be created first using the spanTestEnv() method", null);
			return results;
		}
		
		EnvData testEnv = null;
	
		// read in test env configuration
		try
		{
			testEnv = envService.get(testEnvs.get(0).getKID());
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			results.addError(className, methodList, "Error reading in test environment: " + e.getMessage(), e);
			return results;
		}
		
		results.setTestEnv(testEnv);
		
		try
		{
			deployTestClassToTestEnv(testClassFile, env, testEnv);
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			results.addError(className, methodList, "Error deploying test to test environment: " + e.getMessage(), e);
			return results;
		}
		
		runTests(className, methods, results, testEnv);
		
		return results;
	}
	
	private void deployTestClassToTestEnv(kommet.basic.Class cls, EnvData env, EnvData testEnv) throws KIDException, KommetException
	{
		cls.uninitializeId();
		AuthData authData = userService.getAuthData(userService.getUser(AppConfig.getRootUserId(), testEnv), env);
		
		kommet.basic.Class existingClass = classService.getClass(cls.getQualifiedName(), testEnv);
		
		if (existingClass != null)
		{
			cls.setId(existingClass.getId());
		}
		
		classService.fullSave(cls, dataService, authData, testEnv);	
	}

	@SuppressWarnings("deprecation")
	private TestResults runTests(String className, List<String> methods, TestResults results, EnvData testEnv)
	{
		// the new env should already have the same test class
		Class<?> testClass = null;
		try
		{
			testClass = compiler.getClass(className, true, testEnv);
		}
		catch (ClassNotFoundException | KommetException e)
		{
			results.addError(className, MiscUtils.implode(methods, ", "), "Test class " + className + " not found on spanned environment");
			return results;
		}
		
		Object testInstance = null;
		try
		{
			testInstance = testClass.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			results.addError(className, MiscUtils.implode(methods, ", "), "Error instantiating test class " + className + ": " + e.getMessage(), e);
			return results;
		}
		
		for (String methodName : methods)
		{
			Method method = MiscUtils.getMethodByName(testClass, methodName);
			runSingleTestMethod(testInstance, method, results, testEnv);
		}
		
		return results;
	}

	private void runSingleTestMethod(Object testInstance, Method method, TestResults results, EnvData testEnv)
	{
		try
		{
			method.invoke(testInstance);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			results.addError(testInstance.getClass().getName(), method.getName(), "Error running test method: " + e.getMessage(), e);
		}
	}

	public EnvData spanTestEnv (EnvData env) throws DataAccessException, KIDException, PropertyUtilException, KommetException
	{
		EnvData testEnv = null;
		try
		{
			String testEnvName = "[testing]-" + env.getName();
			
			System.out.println("[test] Deleting old env " + testEnvName);
			
			// delete any old testing environment that existed
			if (envService.deleteEnv(testEnvName))
			{
				System.out.println("[test] Old env deleted");
			}
			else
			{
				System.out.println("[test] Old env didn't exist");
			}
			
			String envNameHash = MiscUtils.getHash(10).toLowerCase();
			
			System.out.println("[test] Creating test env " + envNameHash);
			
			// create new env - do not initialize it, because it's a copy of an existing env and so it's already initialized
			testEnv = envService.createEnv(testEnvName, KID.get("001" + envNameHash), false, true, "env" + env.getId());
			
			System.out.println("[test] New env created");
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			System.out.println("[test] Restoring connection to main env");
			// reconnect to the main env, because it has been disconnected for copying the env db
			envService.reconnectToDatabase(env);
			System.out.println("[test] Connection restored");			
		}
		
		return testEnv;
	}
}