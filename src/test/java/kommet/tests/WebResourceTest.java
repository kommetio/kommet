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

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.File;
import kommet.basic.FileRevision;
import kommet.basic.WebResource;
import kommet.basic.keetle.ViewUtil;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.files.FileService;
import kommet.filters.WebResourceFilter;
import kommet.services.WebResourceService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.UrlUtil;
import kommet.web.RequestAttributes;

public class WebResourceTest extends BaseUnitTest
{
	@Inject
	WebResourceService webResourceService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	FileService fileService;
	
	@Inject
	AppConfig config;
	
	@Inject
	SharingService sharingService;
	
	@Test
	public void testCRUD() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		// create a file
		File testFile = new File();
		testFile.setAccess(File.PUBLIC_ACCESS);
		testFile.setName("FileName");
		
		// save file
		testFile = fileService.saveFile(testFile, dataHelper.getRootAuthData(env), env);
		assertNotNull(testFile.getId());
		
		// create a revision for this file
		FileRevision testRevision = new FileRevision();
		testRevision.setName("My journal");
		testRevision.setSize(1);
		testRevision.setRevisionNumber(1);
		testRevision.setFile(testFile);
		testRevision.setPath("38293nhj323k3hb323bbj23b232kj3b223k21");
		testRevision = fileService.saveRevision(testRevision, dataHelper.getRootAuthData(env), env);
		assertNotNull(testRevision.getId());
		
		String resourceName = "AnyName";
		
		// make sure there are no cached web resources on the env
		assertTrue(env.getWebResources().isEmpty());
		
		WebResource resource = new WebResource();
		resource.setFile(testFile);
		resource.setMimeType("application/javascript");
		resource.setName(resourceName);
		resource.setName("Invalid-name");
		
		try
		{
			resource = webResourceService.save(resource, true, dataHelper.getRootAuthData(env), env);
			fail("Saving web resource with invalid name should fail");
		}
		catch (KommetException e)
		{
			assertEquals("Invalid web resource name", e.getMessage());
		}
		
		resource.setName(resourceName);
		resource = webResourceService.save(resource, true, dataHelper.getRootAuthData(env), env);
		assertNotNull(resource.getId());
		assertNotNull(env.getWebResource(resourceName));
		assertNotNull(env.getWebResource(resourceName).getDiskFilePath());
		assertEquals(1, env.getWebResources().size());
		
		// find resource by file name
		WebResourceFilter filter = new WebResourceFilter();
		filter.setName(resourceName);
		List<WebResource> foundResources = webResourceService.find(filter, env);
		assertEquals(1, foundResources.size());
		assertEquals(testFile.getId(), foundResources.get(0).getFile().getId());
		assertEquals(resource.getId(), foundResources.get(0).getId());
		
		// test getting web resource by ID
		WebResource fetchedResource = webResourceService.get(resource.getId(), env);
		assertNotNull(fetchedResource);
		assertNotNull(fetchedResource.getIsPublic());
		assertFalse(fetchedResource.getIsPublic());
		assertFalse(sharingService.canViewRecord(resource.getId(), env.getGuestUser().getId(), env));
		assertFalse(sharingService.canViewRecord(testFile.getId(), env.getGuestUser().getId(), env));
		assertNotNull(fetchedResource.getFile().getName());
		assertNotNull(fetchedResource.getName());
		assertEquals(resourceName, fetchedResource.getName());
		assertNotNull(fetchedResource.getFile().getId());
		assertNull(fetchedResource.getDiskFilePath());
		assertEquals(resource.getFile().getName(), fetchedResource.getFile().getName());
		
		// now fetch resource with disk file path
		List<WebResource> fetchedResources = webResourceService.find(filter, env);
		fetchedResources = webResourceService.initFilePath(fetchedResources, dataHelper.getRootAuthData(env), env);
		assertEquals(1, fetchedResources.size());
		assertNotNull(fetchedResources.get(0).getDiskFilePath());
		
		// delete web resource
		webResourceService.delete(resource.getId(), true, dataHelper.getRootAuthData(env), env);
		
		// make sure the resource has been deleted
		assertTrue("Web resource not deleted", webResourceService.find(null, env).isEmpty());
		
		// make sure the file has also been deleted
		assertNull(fileService.getFileById(testFile.getId(), env));
		
		String ktlCode = "Some <div>code{{$resource.path.somename}} and</div>";
		String jsp = ViewUtil.keetleToJSP(ktlCode, config, env);
		assertTrue("Incorrect JSP code:\n" + jsp, jsp.contains("Some <div>code${pageContext.request.contextPath}/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/downloadresource?name=${" + RequestAttributes.ENV_ATTR_NAME + ".getWebResource('somename').getName()} and</div>"));
		
		ktlCode = "Some <div>code{{$resource.path.somename.MyOne}} and</div>";
		jsp = ViewUtil.keetleToJSP(ktlCode, config, env);
		assertTrue("Incorrect JSP code:\n" + jsp, jsp.contains("Some <div>code${pageContext.request.contextPath}/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/downloadresource?name=${" + RequestAttributes.ENV_ATTR_NAME + ".getWebResource('somename.MyOne').getName()} and</div>"));
		
		testPublicWebResource(env);
	}

	private void testPublicWebResource(EnvData env) throws KommetException
	{
		// create a file
		File testFile = new File();
		testFile.setAccess(File.PUBLIC_ACCESS);
		testFile.setName("FileName");
		
		// save file
		testFile = fileService.saveFile(testFile, dataHelper.getRootAuthData(env), env);
		assertNotNull(testFile.getId());
		
		// create a revision for this file
		FileRevision testRevision = new FileRevision();
		testRevision.setName("My journal");
		testRevision.setSize(1);
		testRevision.setRevisionNumber(1);
		testRevision.setFile(testFile);
		testRevision.setPath("38293nhj323k3hb323bbj23b232kj3b223k21");
		testRevision = fileService.saveRevision(testRevision, dataHelper.getRootAuthData(env), env);
		assertNotNull(testRevision.getId());
		
		WebResource resource = new WebResource();
		resource.setFile(testFile);
		resource.setMimeType("application/javascript");
		resource.setName("AnyName");
		resource.setPublic(true);
		resource.uninitializeId();
		resource = webResourceService.save(resource, true, dataHelper.getRootAuthData(env), env);
		WebResource fetchedResource = webResourceService.get(resource.getId(), env);
		assertNotNull(fetchedResource);
		assertNotNull(fetchedResource.getIsPublic());
		assertTrue(fetchedResource.getIsPublic());
		
		assertTrue(sharingService.canViewRecord(resource.getId(), env.getGuestUser().getId(), env));
		assertTrue(sharingService.canViewRecord(testFile.getId(), env.getGuestUser().getId(), env));
		
		// set the resource as non-public
		resource.setPublic(false);
		webResourceService.save(resource, true, dataHelper.getRootAuthData(env), env);
		assertFalse(sharingService.canViewRecord(resource.getId(), env.getGuestUser().getId(), env));
		assertFalse(sharingService.canViewRecord(testFile.getId(), env.getGuestUser().getId(), env));
	}
}
