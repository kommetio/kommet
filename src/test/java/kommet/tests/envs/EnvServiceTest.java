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

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.dao.DataAccessException;

import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.View;
import kommet.basic.keetle.ViewService;
import kommet.basic.types.SystemTypes;
import kommet.config.Constants;
import kommet.data.DataService;
import kommet.data.Env;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.EnvFilter;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.TestConfig;

public class EnvServiceTest extends BaseUnitTest
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
	
	@Inject
	UserService userService;
	
	@Inject
	ViewService viewService;
	
	@Test
	public void testMasterEnv() throws DataAccessException, KommetException
	{
		assertNotNull(envService.getMasterEnv());
		
		// execute a test generic query on the master env
		Integer objCount = envService.getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs", Integer.class);
		assertNotNull(objCount);
		
		// make sure a test env is configured on the master env and can be retrieved
		/*EnvData masterEnv = envManager.getMasterEnv();
		List<Record> envs = masterEnv.getSelectCriteriaFromDAL("select id, name from envs where name = 'test001'").list();
		assertNotNull(envs);
		assertEquals("Test env with name 'test001' not found on the master env", 1, envs);*/
	}
	
	@Test
	public void testGetTestEnv() throws KommetException
	{
		EnvFilter filter = new EnvFilter();
		filter.setName(testConfig.getTestEnv());
		List<Env> envs = envService.find(filter, envService.getMasterEnv());
		assertEquals(1, envs.size());
	}
	
	@Test
	public void testGetTestEnvById() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		env = envService.get(testConfig.getTestEnvId());
		assertNotNull(env.getEnv());
		assertEquals(testConfig.getTestEnvId().getId(), env.getEnv().getKID().getId());
		assertEquals(testConfig.getTestEnvId(), env.getEnv().getKID());
		
		long timeInitialized = env.getTimeInitialized();
		assertNotNull(timeInitialized);
		
		assertNotNull(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_API_NAME)));
		assertNotNull(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_PERMISSION_API_NAME)));
		assertNotNull(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.TYPE_PERMISSION_API_NAME)));
		assertNotNull(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.FIELD_PERMISSION_API_NAME)));
		assertNotNull(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.PROFILE_API_NAME)));
		
		// now test clearing environment
		envService.clear(env.getId());
		
		env = envService.get(env.getId());
		
		for (Type type : env.getAllTypes())
		{
			// make sure unique checks are initialized on types
			assertNotNull("No unique checks exist for type " + type.getQualifiedName(), type.getUniqueChecks());
			assertTrue("No unique checks on type " + type.getQualifiedName(), !type.getUniqueChecks().isEmpty());
		}
		
		for (View view :viewService.getAllViews(env))
		{
			assertNotNull(env.getView(view.getId()));
		}
		
		assertNotNull(env.getGuestUser());
		assertEquals(userService.get(Constants.UNAUTHENTICATED_USER_NAME, env).getId(), env.getGuestUser().getId());
		
		// make sure the initialization time has changed
		assertFalse("When environment is reinitialized, its initialization time should change", timeInitialized == env.getTimeInitialized());
	}
}
