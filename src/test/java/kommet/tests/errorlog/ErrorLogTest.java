/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.errorlog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.ErrorLog;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.errorlog.ErrorLogFilter;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class ErrorLogTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	ErrorLogService	errorLogService;
	
	@Test
	public void testErrorLog() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		errorLogService.logException(new KommetException("Some exception"), ErrorLogSeverity.ERROR, ErrorLogTest.class.getName(), 11, authData.getUser().getId(), authData, env);
	
		List<ErrorLog> logs = errorLogService.get(new ErrorLogFilter(), env); 
		assertEquals(1, logs.size());
		assertEquals("Some exception", logs.get(0).getMessage());
		assertNotNull(logs.get(0).getDetails());
		
		// find by severity
		ErrorLogFilter filter = new ErrorLogFilter();
		filter.addSeverity("Warning");
		assertEquals(0, errorLogService.get(filter, env).size());
		
		filter.addSeverity("Error");
		assertEquals(1, errorLogService.get(filter, env).size());
	}
}
