/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.envs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;
import org.springframework.dao.DataAccessException;

import kommet.basic.BasicSetupService;
import kommet.basic.types.SystemTypes;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.TestConfig;

public class EnvCreationTest extends BaseUnitTest
{
	@Inject
	EnvService envService;
	
	@Inject
	DataService typeService;
	
	@Inject
	TestConfig testConfig;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	BasicSetupService basicSetupService;
	
	private static final String newEnvName = "mytestenv";
	private static final String newEnvId = "0010000000123";
	
	@Test
	public void testCreatingNewEnv() throws DataAccessException, KommetException
	{	
		assertEquals((Integer)0, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE name = '" + newEnvName + "'", Integer.class));
		assertEquals((Integer)0, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE kid = '" + newEnvId + "'", Integer.class));
		assertEquals((Integer)0, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(*) from pg_database WHERE datname= 'env" + newEnvId + "'", Integer.class));
		
		// create a totally new environment
		EnvData env = envService.createEnv(newEnvName, KID.get(newEnvId), true);
		assertNotNull(env);
		
		// make sure the new env can be found in the master database
		assertEquals((Integer)1, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE name = '" + newEnvName + "'", Integer.class));
		assertEquals((Integer)1, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE kid = '" + newEnvId + "'", Integer.class));
		assertEquals((Integer)1, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(*) from pg_database WHERE datname= 'env" + newEnvId + "'", Integer.class));
		
		try
		{
			// try to create an environment with the same name
			envService.createEnv(newEnvName, KID.get("0010000000abc"), true);
			fail("Creating env with a duplicate name should fail");
		}
		catch (KommetException e)
		{
			assertTrue(e.getMessage().startsWith("Environment with name " + newEnvName + " already exists"));
		}
		
		try
		{
			// try to create an environment with the same ID
			envService.createEnv(newEnvName + "123", KID.get(newEnvId), true);
			fail("Creating env with a duplicate ID should fail");
		}
		catch (KommetException e)
		{
			assertTrue(e.getMessage().startsWith("Environment with ID " + newEnvId + " already exists"));
		}
		
		env = envService.get(env.getId());
		
		// try querying the new env
		assertFalse(env.getSelectCriteriaFromDAL("select id from " + SystemTypes.USER_API_NAME).list().isEmpty());
	}
	
	@After
	public void deletedCreatedEnvs() throws DataAccessException, KommetException
	{
		envService.deleteEnv(KID.get(newEnvId));
		
		// make sure the env is actually deleted
		assertEquals((Integer)0, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE name = '" + newEnvName + "'", Integer.class));
		assertEquals((Integer)0, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE kid = '" + newEnvId + "'", Integer.class));
		assertEquals((Integer)0, (Integer)envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(*) from pg_database WHERE datname= 'env" + newEnvId + "'", Integer.class));
	}
}
