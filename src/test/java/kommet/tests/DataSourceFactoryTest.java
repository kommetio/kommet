/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertNotNull;

import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.junit.Test;

import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.DataSourceFactory;
import kommet.env.EnvService;
import kommet.utils.TestConfig;

public class DataSourceFactoryTest extends BaseUnitTest
{
	@Inject
	DataSourceFactory dsFactory;
	
	@Inject
	TestConfig testConfig;
	
	@Inject
	EnvService envService;
	
	@Test
	public void getTestDataSource() throws KommetException
	{
		DataSource ds = dsFactory.getDataSource(KID.get("0010000000001"), envService, testConfig.getTestEnvDBHost(), testConfig.getTestEnvDBPort(), testConfig.getTestEnvDB(), testConfig.getTestEnvDBUser(), testConfig.getTestEnvDBPassword(), false);
		assertNotNull(ds);
	}
}
