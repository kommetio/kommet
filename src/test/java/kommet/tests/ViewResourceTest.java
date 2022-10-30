/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.ViewResource;
import kommet.basic.keetle.ViewUtil;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.ViewResourceFilter;
import kommet.services.ViewResourceService;
import kommet.utils.AppConfig;
import kommet.web.RequestAttributes;

public class ViewResourceTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	AppConfig config;
	
	@Inject
	EnvService envService;
	
	@Test
	public void testCRUD() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		// create view resource
		ViewResource resource1 = new ViewResource();
		resource1.setName("styles.css");
		resource1.setPath("4738493742.css");
		resource1.setMimeType("text/css");
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		resource1 = viewResourceService.save(resource1, authData, env);
		assertNotNull(resource1.getId());
		assertNotNull(viewResourceService.getByName(resource1.getName(), authData, env));
		
		// add some content
		resource1.setContent("one two three");
		viewResourceService.save(resource1, authData, env);
		
		assertEquals(1, viewResourceService.find(null, env).size());
		
		// create another resource
		ViewResource resource2 = new ViewResource();
		resource2.setName("styles2.css");
		resource2.setPath("473849374.css");
		resource2.setMimeType("text/css");
		// make sure empty content is allowed
		resource2.setContent(null);
		resource2 = viewResourceService.save(resource2, authData, env);
		assertNotNull("View content should have been automatically set to empty string by the save method", resource2.getContent());
		
		// make sure the view resource has been stored on disk
		File resourceDiskFile2 = new File(env.getViewResourceEnvDir(config.getViewResourceDir()) + "/" + resource2.getPath());
		assertTrue(resourceDiskFile2.exists());
		
		ViewResourceFilter filter = new ViewResourceFilter();
		filter.setContentLike("on");
		List<ViewResource> resources = viewResourceService.find(filter, env);
		assertEquals(1, resources.size());
		
		// make sure view resources are fetched without content by default
		assertNull("View resources should be fetched without content by default", resources.get(0).getContent());
		
		// now fetch resources with content
		filter.setFetchContent(true);
		resources = viewResourceService.find(filter, env);
		assertNotNull(resources.get(0).getContent());
		
		// make sure creating another resource with the same name fails
		ViewResource resource3 = new ViewResource();
		resource3.setName("styles3.css");
		resource3.setPath("47384aaa.css");
		resource3.setMimeType("text/css");
		resource3.setContent("elele");
		resource3 = viewResourceService.save(resource3, authData, env);
		assertNotNull(resource3.getId());
		
		resource3.setName(resource2.getName());
		
		try
		{
			resource3 = viewResourceService.save(resource3, authData, env);
			fail("Saving two view resources with the same name should fail");
		}
		catch (UniqueCheckViolationException e)
		{
			// expected
		}
		
		// uncomment the rest of the test below once unique check errors are handles in insert query properly
		
		resource3.setName("newname.txt");
		resource3.setPath(resource2.getPath());
		
		try
		{
			resource3 = viewResourceService.save(resource3, authData, env);
			fail("Saving two view resources with the same path should fail");
		}
		catch (UniqueCheckViolationException e)
		{
			// expected
		}
		
		resource3.setPath("abc.txt");
		resource3 = viewResourceService.save(resource3, authData, env);
		assertNotNull(resource3.getId());
		
		File resourceDiskFile3 = new File(env.getViewResourceEnvDir(config.getViewResourceDir()) + "/" + resource3.getPath());
		Long resource3DiskFileCreatedDate = resourceDiskFile3.lastModified();
		
		assertEquals(resource2.getPath(), env.getViewResource(resource2.getName()).getPath());
		
		String ktlCode = "Some <div>code{{$viewresource.path." + resource1.getName() + "}} and</div>";
		String jsp = ViewUtil.keetleToJSP(ktlCode, config, env);
		assertTrue("Incorrect JSP code:\n" + jsp, jsp.contains("Some <div>code${pageContext.request.contextPath}/${" + RequestAttributes.ENV_ATTR_NAME + ".getViewResourcePath('" + resource1.getName() + "', " + RequestAttributes.APP_CONFIG_ATTR_NAME + ")} and</div>"));
		
		// now delete one of the view resources
		viewResourceService.delete(resource2.getId(), authData, env);
		// make sure the view disk file is deleted together with the view
		assertFalse("View resource disk file has not been deleted together with the view resource", resourceDiskFile2.exists());
		
		// reinitialize the env
		// now test clearing environment
		envService.clear(env.getId());
				
		env = envService.get(env.getId());
		
		assertNotNull(env.getViewResource(resource3.getName()));
		assertNotNull(env.getViewResource(resource1.getName()));
		
		resourceDiskFile3 = new File(env.getViewResourceEnvDir(config.getViewResourceDir()) + "/" + resource3.getPath());
		assertTrue(resourceDiskFile3.exists());
		assertTrue("Resource disk file should have been created anew when environment was reinitialized", resourceDiskFile3.lastModified() > resource3DiskFileCreatedDate);
		
		// try getting resource by ID
		resource2 = viewResourceService.get(resource3.getId(), env);
		assertNotNull(resource3);
		assertNotNull(resource3.getPath());
		assertNotNull("When a single view resource is fetched, it should be retrieved with content", resource3.getContent());
		assertNotNull(resource3.getName());
		
		// update resource 2 with empty value
		resource3.setContent(null);
		viewResourceService.save(resource3, authData, env);
		viewResourceService.initViewResourcesOnDisk(env, true);
	}
}
