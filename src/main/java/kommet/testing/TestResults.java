/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.testing;

import java.util.ArrayList;
import java.util.List;

import kommet.env.EnvData;

public class TestResults
{
	private List<TestError> errors;
	private EnvData testEnv;
	
	public TestResults()
	{
		this.errors = new ArrayList<TestError>();
	}
	
	public void addError(String className, String method, String msg)
	{
		this.addError(className, method, msg, null);
	}
	
	public void addError(String className, String method, String msg, Exception e)
	{
		TestError err = new TestError();
		err.setClassName(className);
		err.setMethod(method);
		err.setMessage(msg);
		err.setException(e);
		
		this.errors.add(err);
	}

	public List<TestError> getErrors()
	{
		return this.errors;
	}

	public EnvData getTestEnv()
	{
		return testEnv;
	}

	public void setTestEnv(EnvData testEnv)
	{
		this.testEnv = testEnv;
	}

}