/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.BasicSetupService;
import kommet.basic.Profile;
import kommet.basic.User;
import kommet.dao.ProfileDao;
import kommet.dao.UserDao;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class DaoTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService typeService;
	
	@Inject
	UserDao userDao;
	
	@Inject
	ProfileDao profileDao;
	
	@Inject
	KommetCompiler compiler;
	
	@Test
	public void testGenericDao() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		env.addCustomTypeProxyMapping(User.class);
		
		// create test profile
		Profile testProfile = new Profile();
		testProfile.setName("testProfile");
		testProfile.setLabel("testProfile");
		testProfile.setSystemProfile(false);
		
		testProfile = profileDao.save(testProfile, dataHelper.getRootAuthData(env), env);
		assertNotNull(testProfile.getId());
		
		User testUser = new User();
		testUser.setUserName("test1");
		testUser.setPassword("sdfnfklfj");
		testUser.setEmail("some.email@kolmu.com");
		testUser.setProfile(testProfile);
		testUser.setTimezone("GMT");
		testUser.setLocale("EN_US");
		testUser.setIsActive(true);
		
		testUser = userDao.save(testUser, dataHelper.getRootAuthData(env), env);
		assertNotNull(testUser.getId());
		assertEquals("test1", testUser.getUserName());
	}
	
	/*@Test
	public void testProxyDaoOperations() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		KObject pigeonObj = dataHelper.getFullPigeonObject(env);
		kObjectService.create(pigeonObj, env.getRootUser().getKolmuId(), env);
		
		// generate and compile proxy class
		KollFile pigeonProxyKollFile = ObjectProxyClassGenerator.getProxyKollClass(pigeonObj, true, env);
		compiler.compile(pigeonProxyKollFile, env);
		compiler.resetClassLoader(env);
		Class<? extends ObjectProxy> pigeonProxyClass = (Class<? extends ObjectProxy>)compiler.getClass(pigeonProxyKollFile, true, env);
		
		// get DAO to manipulate the proxy
		ObjectProxyDao pigeonDao = env.getProxyDao(pigeonProxyClass);
		
		// create new pigeon
		ObjectProxy youngPigeon = pigeonProxyClass.newInstance();
		
	}*/
}
