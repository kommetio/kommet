/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;

import kommet.dao.EnvDao;
import kommet.services.EnvDataService;
import kommet.utils.AppConfig;
import kommet.utils.PropertyUtilException;
import kommet.utils.TestConfig;

public class ConfigTest extends BaseUnitTest
{
	@Inject
	AppConfig appConfig;
	
	@Inject
	TestConfig testConfig;
	
	@Inject
	EnvDao envDao;
	
	@Inject
	EnvDataService envService;
	
	@Test
	public void testConfig() throws PropertyUtilException
	{
		assertNotNull(appConfig.getEnvDBHost());
		assertNotNull(appConfig.getEnvDBPassword());
		assertNotNull(appConfig.getEnvDBPort());
		assertNotNull(appConfig.getEnvDBUser());
		
		assertNotNull(appConfig.getMasterDB());
		assertNotNull(appConfig.getMasterDBHost());
		assertNotNull(appConfig.getMasterDBPassword());
		assertNotNull(appConfig.getMasterDBPort());
		assertNotNull(appConfig.getMasterDBUser());
	}
}
