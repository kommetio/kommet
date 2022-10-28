/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.webmock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import kommet.basic.Class;
import kommet.basic.Comment;
import kommet.basic.View;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.ViewUtil;
import kommet.comments.CommentFilter;
import kommet.comments.CommentService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.js.jsrc.JSRC;
import kommet.koll.ClassService;
import kommet.koll.KollUtil;
import kommet.utils.UrlUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@WebAppConfiguration
@Rollback
public class RestApiTest extends BasicWebMockTest
{
	@Inject
	ViewService viewService;
	
	@Inject
	ClassService classService;
	
	@Inject
	CommentService commentService;
	
	@SuppressWarnings("unchecked")
	@Test
    public void testSaveViewUniqueName() throws Exception
    {	
		EnvData env = dataHelper.getTestEnvData(false);
    	basicSetupService.runBasicSetup(env);
		envService.add(env);
		
		String accessToken = obtainAccessToken(true, env);
		
		String viewCode = ViewUtil.getEmptyViewCode("SampleView", "some.pack");
		
		// first try to save with access token of a non-admin user
		this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_VIEW_URL)
				.param("access_token", obtainAccessToken(false, env))
				.param("env", env.getId().getId())
				.param("code", viewCode)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().is(HttpServletResponse.SC_FORBIDDEN));
		
		// save view through rest api
		MvcResult result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_VIEW_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("code", viewCode)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		Map<String, Object> respJSON = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);
		String viewId = (String)respJSON.get("id");
		assertNotNull(viewId);
		
		// find view by ID
		View view = viewService.getView(KID.get(viewId), env);
		assertNotNull(view);
		assertEquals(viewCode, view.getKeetleCode());
		
		// update the view with new content
		viewCode = viewCode.replaceAll(ViewUtil.EMPTY_VIEW_CONTENT, "test123");
		
		// save view through rest api
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_VIEW_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("code", viewCode)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
				.andExpect(status().is(HttpServletResponse.SC_BAD_REQUEST))
				.andReturn();
		
		assertTrue(result.getResponse().getContentAsString().contains("Unique check violated. Probably a view with the given name/package already exists"));
    }
	
	@Test
	public void testAPIs() throws Exception
	{
		EnvData env = dataHelper.getTestEnvData(false);
    	basicSetupService.runBasicSetup(env);
		envService.add(env);
		
		String accessToken = obtainAccessToken(true, env);
		assertNotNull(accessToken);
		
		testSaveFile(env, accessToken);
		testSaveRecord(env);
		testGetControllerClasses(env, accessToken);
		testGetViews(env, accessToken);
	}

	private void testGetControllerClasses(EnvData env, String accessToken) throws Exception
	{
		MvcResult result = this.mockMvc.perform(get("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_GET_CONTROLLER_CLASSES)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		String resp = result.getResponse().getContentAsString();
		//assertTrue("Invalid content length: " + result.getResponse().getContentLength(), result.getResponse().getContentLength() > 0);
		assertNotNull(resp);
		assertTrue("Invalid JSON: '" + resp + "'", resp.contains("\"records\":"));
		assertTrue("Invalid JSON: '" + resp + "'", resp.contains("\"jsti\":"));
		assertTrue(resp.startsWith("{ \"success\": true, \"data\": "));
		
		// extract JSRC JSON from response JSON
		String jsrcJson = resp.replace("{ \"success\": true, \"data\": ", "");
		jsrcJson.substring(0, jsrcJson.length() - 2);
		
		JSRC jsrc = JSRC.deserialize(jsrcJson);
		assertNotNull(jsrc);
		assertNotNull(jsrc.getJsti());
		assertNotNull(jsrc.getRecords());
	}
	
	private void testGetViews(EnvData env, String accessToken) throws Exception
	{
		MvcResult result = this.mockMvc.perform(get("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_GET_VIEWS)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		String resp = result.getResponse().getContentAsString();
		//assertTrue("Invalid content length: " + result.getResponse().getContentLength(), result.getResponse().getContentLength() > 0);
		assertNotNull(resp);
		assertTrue("Invalid JSON: '" + resp + "'", resp.contains("\"records\":"));
		assertTrue("Invalid JSON: '" + resp + "'", resp.contains("\"jsti\":"));
		assertTrue(resp.startsWith("{ \"success\": true, \"data\": "));
		
		// extract JSRC JSON from response JSON
		String jsrcJson = resp.replace("{ \"success\": true, \"data\": ", "");
		jsrcJson.substring(0, jsrcJson.length() - 2);
		
		JSRC jsrc = JSRC.deserialize(jsrcJson);
		assertNotNull(jsrc);
		assertNotNull(jsrc.getJsti());
		assertNotNull(jsrc.getRecords());
	}

	@SuppressWarnings("unchecked")
	private void testSaveRecord(EnvData env) throws Exception
	{
		String accessToken = obtainAccessToken(true, env);
		
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		// save view through rest API
		MvcResult result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_RECORD_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("typeId", pigeonType.getKID().getId())
				.param("record", "{ \"name\": \"Lee\", \"age\": 12 }")
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		String resp = result.getResponse().getContentAsString();
		Map<String, Object> respJSON = new ObjectMapper().readValue(resp, HashMap.class);
		String objectId = (String)respJSON.get("id");
		assertNotNull(objectId);
		
		// query the record
		List<Record> records = dataService.getRecords(Arrays.asList(KID.get(objectId)), pigeonType, Arrays.asList("age", "name"), dataHelper.getRootAuthData(env), env);
		assertEquals(1, records.size());
		assertEquals(KID.get(objectId), records.get(0).getKID());
		assertEquals("Lee", records.get(0).getField("name"));
		assertEquals(Integer.valueOf(12), records.get(0).getField("age"));
		
		// now update the record using REST API
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_RECORD_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("id", objectId)
				.param("record", "{ \"name\": \"Lee-Ann\", \"age\": 12 }")
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		records = dataService.getRecords(Arrays.asList(KID.get(objectId)), pigeonType, Arrays.asList("age", "name"), dataHelper.getRootAuthData(env), env);
		assertEquals(1, records.size());
		assertEquals(KID.get(objectId), records.get(0).getKID());
		assertEquals("Lee-Ann", records.get(0).getField("name"));
		assertEquals(Integer.valueOf(12), records.get(0).getField("age"));
		
		testAddAndReadComment(env, records.get(0).getKID(), accessToken);
		
		// test deleting object
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DELETE_RECORD_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("id", objectId)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		records = dataService.getRecords(Arrays.asList(KID.get(objectId)), pigeonType, Arrays.asList("name"), dataHelper.getRootAuthData(env), env);
		assertEquals(0, records.size());
	}

	private void testAddAndReadComment(EnvData env, KID recordId, String accessToken) throws Exception
	{	
		assertNotNull(recordId);
		
		MvcResult result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_RECORD_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("typeId", env.getType(KeyPrefix.get(KID.COMMENT_PREFIX)).getKID().getId())
				.param("record", "{ \"content\": \"Comment text\", \"recordId\": \"" + recordId + "\" }")
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		CommentFilter filter = new CommentFilter();
		filter.addRecordId(recordId);
		List<Comment> comments = commentService.get(filter, env);
		assertEquals(1, comments.size());
		assertEquals("Comment text", comments.get(0).getContent());
		
		// get comments using REST API
		result = this.mockMvc.perform(get("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_RECORD_COMMENTS_URL + "/" + recordId)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		String commentJSON = result.getResponse().getContentAsString();
		assertTrue("Invalid comment JSON " + commentJSON, commentJSON.contains("\"status\": \"success\""));
		assertTrue("Invalid comment JSON " + commentJSON, commentJSON.contains("\"content\": \"Comment text\""));
		assertTrue("Invalid comment JSON " + commentJSON, commentJSON.contains("\"id\": \"" + comments.get(0).getId().getId() + "\""));
	}

	@SuppressWarnings("unchecked")
	private void testSaveFile(EnvData env, String accessToken) throws Exception
	{	
		int originalFileCount = classService.getClasses(null, env).size();
		
		String kollCode = KollUtil.getTemplateCode("TestClass", "some.pack", env);
		
		// first try to save with access token of a non-admin user
		this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_CLASS_URL)
				.param("access_token", obtainAccessToken(false, env))
				.param("env", env.getId().getId())
				.param("name", "some.pack.TestClass")
				.param("code", kollCode)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().is(HttpServletResponse.SC_FORBIDDEN));
		
		// save view through rest api
		MvcResult result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_CLASS_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("name", "some.pack.TestClass")
				.param("code", kollCode)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		Map<String, Object> respJSON = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);
		String fileId = (String)respJSON.get("id");
		assertNotNull(fileId);
		
		// find view by ID
		Class file = classService.getClass(KID.get(fileId), env);
		assertNotNull(file);
		assertEquals("TestClass", file.getName());
		assertEquals("some.pack", file.getPackageName());
		assertEquals(kollCode, file.getKollCode());
		
		// get updated file content
		kollCode = KollUtil.getTemplateKollCode(file.getName(), file.getPackageName(), null, null, "public void testMethod() {}", env);
		
		// save view through rest api
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_CLASS_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("code", kollCode)
				.param("id", fileId)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
				.andExpect(status().isOk())
				.andReturn();
		
		respJSON = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);
		String newViewId = (String)respJSON.get("id");
		assertNotNull(newViewId);
		assertEquals(fileId, newViewId);
		
		file = classService.getClass(KID.get(fileId), env);
		assertNotNull(file);
		assertEquals(kollCode, file.getKollCode());
		
		assertEquals(originalFileCount + 1, classService.getClasses(null, env).size());
		
		// now delete the class file
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DELETE_CLASS_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("code", kollCode)
				.param("id", fileId)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
				.andExpect(status().isOk())
				.andReturn();
		
		assertTrue(result.getResponse().getContentAsString().startsWith("{ \"success\": true"));
		
		assertEquals(originalFileCount, classService.getClasses(null, env).size());
	}

	@SuppressWarnings("unchecked")
	@Test
    public void testSaveView() throws Exception
    {	
		EnvData env = dataHelper.getTestEnvData(false);
    	basicSetupService.runBasicSetup(env);
		envService.add(env);
		
		int originalViewCount = viewService.getViews(null, env).size();
		
		String accessToken = obtainAccessToken(true, env);
		
		String viewCode = ViewUtil.getEmptyViewCode("SampleView", "some.pack");
		
		// save view through rest api
		MvcResult result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_VIEW_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("code", viewCode)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
				.andReturn();
		
		Map<String, Object> respJSON = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);
		String viewId = (String)respJSON.get("id");
		assertNotNull(viewId);
		
		// find view by ID
		View view = viewService.getView(KID.get(viewId), env);
		assertNotNull(view);
		assertEquals(viewCode, view.getKeetleCode());
		
		// update the view with new content
		viewCode = viewCode.replaceAll(ViewUtil.EMPTY_VIEW_CONTENT, "test123");
		
		// save view through rest api
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_SAVE_VIEW_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("code", viewCode)
				.param("id", viewId)
	        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
				.andExpect(status().isOk())
				.andReturn();
		
		respJSON = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);
		String newViewId = (String)respJSON.get("id");
		assertNotNull(newViewId);
		assertEquals(viewId, newViewId);
		
		view = viewService.getView(KID.get(viewId), env);
		assertNotNull(view);
		assertEquals(viewCode, view.getKeetleCode());
		
		assertEquals(originalViewCount + 1, viewService.getViews(null, env).size());
    }
}
