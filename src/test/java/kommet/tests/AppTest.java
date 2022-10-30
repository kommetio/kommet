/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.App;
import kommet.basic.AppUrl;
import kommet.basic.BasicSetupService;
import kommet.basic.UniqueCheckViolationException;
import kommet.dao.DomainMappingDao;
import kommet.data.DomainMapping;
import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.AppUrlFilter;
import kommet.services.AppService;
import kommet.utils.AppConfig;

public class AppTest extends BaseUnitTest
{
	@Inject
	AppConfig config;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	AppService appService;
	
	@Inject
	EnvService envService;
	
	@Inject
	DomainMappingDao domainMappingDao;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Test
	public void testCreatApp() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		// create app
		App app = new App();
		app.setType("Internal ap - misspelled");
		app.setName("com.some.MyApp");
		app.setLabel("Some app 3");
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		try
		{
			app = appService.save(app, authData, env);
			fail("Inserting an app with misspelled type should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
		}
		
		app.setType("Internal app");
		app = appService.save(app, authData, env);
		assertNotNull(app.getId());
		assertNotNull(appService.getAppByName(app.getName(), authData, env));
		
		// try to insert a second app with the same label
		App app1 = new App();
		app1.setType("Internal app");
		app1.setName("com.some.MyAppTwo");
		app1.setLabel("Some app 3");
		
		try
		{
			app1 = appService.save(app1, authData, env);
			fail("Inserting app with a duplicate label should fail");
		}
		catch (UniqueCheckViolationException e)
		{
			assertNotNull(e.getUniqueCheck());
			assertEquals("UniqueAppLabel", e.getUniqueCheck().getName());
		}
		
		EnvData sharedEnv = envService.getMasterEnv();
		
		// assign some URL to the app
		AppUrl appUrl1 = new AppUrl();
		appUrl1.setApp(app);
		appUrl1.setUrl("my.app.kommet.io");
		appUrl1 = appService.save(appUrl1, authData, env, sharedEnv);
		assertNotNull(appUrl1.getId());
		
		// make sure a domain mapping has been created for this app URL
		DomainMapping mapping1 = domainMappingDao.getForURL(appUrl1.getUrl(), sharedEnv);
		assertNotNull(mapping1);
		assertEquals(env.getId(), mapping1.getEnv().getKID());
		
		// now create another mapping
		AppUrl appUrl2 = new AppUrl();
		appUrl2.setApp(app);
		appUrl2.setUrl("my.app2.kommet.io");
		appUrl2 = appService.save(appUrl2, authData, env, sharedEnv);
		assertNotNull(appUrl2.getId());
		assertNotNull("Domain mapping not created for AppURL", domainMappingDao.getForURL(appUrl2.getUrl(), sharedEnv));
		
		// delete app URL
		appService.deleteAppUrl(appUrl2.getId(), authData, env, sharedEnv);
		AppUrlFilter appUrlFilter = new AppUrlFilter();
		appUrlFilter.addAppUrlId(appUrl2.getId());
		assertTrue(appService.find(appUrlFilter, env).isEmpty());
		
		// make sure domain mapping has also been deleted
		assertNull("Domain mapping not deleted when AppURL was deleted", domainMappingDao.getForURL(appUrl2.getUrl(), sharedEnv));
		
		// try to create a duplicate app URL and make sure it fails
		AppUrl duplicateAppUrl = new AppUrl();
		duplicateAppUrl.setApp(app);
		duplicateAppUrl.setUrl("my.app.kommet.io");
		
		try
		{
			duplicateAppUrl = appService.save(duplicateAppUrl, authData, env, sharedEnv);
			fail("Inserting a duplicate app URL should fail");
		}
		catch (UniqueCheckViolationException e)
		{
			// expected
		}
	}
	
	@Test
	public void testDeleteApp() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create app
		App app = new App();
		app.setName("MyAppOne");
		app.setType("Internal app");
		app.setLabel("Some app 2");
		app = appService.save(app, authData, env);
		assertNotNull(app.getId());
		
		EnvData sharedEnv = envService.getMasterEnv();
		
		// assign some URL to the app
		AppUrl appUrl1 = new AppUrl();
		appUrl1.setApp(app);
		appUrl1.setUrl("my.app.kommet.io");
		appUrl1 = appService.save(appUrl1, authData, env, sharedEnv);
		assertNotNull(appUrl1.getId());
		
		// now delete the app
		appService.delete(app.getId(), authData, env, sharedEnv);
		
		// make sure all app URLs have been removed
		List<AppUrl> urls = appService.find(null, env);
		assertTrue("URLs not removed with the app", urls.isEmpty());
		assertNull(domainMappingDao.getForURL(appUrl1.getUrl(), sharedEnv));
	}
	
	@Test
	public void testPreventDuplicateDomainMappings() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create app
		App app = new App();
		app.setName("MyAppOne");
		app.setType("Internal app");
		app.setLabel("Some app 1");
		app = appService.save(app, authData, env);
		assertNotNull(app.getId());
		
		EnvData sharedEnv = envService.getMasterEnv();
		
		// assign some URL to the app
		AppUrl appUrl1 = new AppUrl();
		appUrl1.setApp(app);
		appUrl1.setUrl("my.app.kommet.io");
		appUrl1 = appService.save(appUrl1, authData, env, sharedEnv);
		assertNotNull(appUrl1.getId());
		
		assertNotNull(domainMappingDao.getForURL(appUrl1.getUrl(), sharedEnv));
		
		// create another env and make sure it is not possible to create a duplicate mapping for this env either
		EnvData env2 = dataHelper.getTestEnv2Data(false);
		basicSetupService.runBasicSetup(env2);
		
		AuthData authData2 = dataHelper.getRootAuthData(env2);
		
		// create app
		App app2 = new App();
		app2.setType("Internal app");
		app2.setName("MyAppOne");
		app2.setLabel("Some label");
		app2 = appService.save(app2, authData2, env2);
		
		// create a URL mapping that uses the same URL as one of the previous mappings
		AppUrl appUrl3 = new AppUrl();
		appUrl3.setApp(app);
		appUrl3.setUrl(appUrl1.getUrl());
		try
		{
			appUrl3 = appService.save(appUrl3, authData, env2, sharedEnv);
			fail("Inserting a duplicate app URL should fail");
		}
		catch (KommetException e)
		{
			assert(e.getMessage().startsWith("Error saving domain mapping"));
		}
	}
}
