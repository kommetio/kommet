/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.webmock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Class;
import kommet.basic.View;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.keetle.ViewService;
import kommet.comments.CommentService;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.utils.UrlUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@WebAppConfiguration
@Rollback
public class StandardActionTest extends BasicWebMockTest
{
	@Inject
	ViewService keetleService;
	
	@Inject
	ClassService kollService;
	
	@Inject
	CommentService commentService;
	
	@Test
    public void testSaveStandardAction() throws Exception
    {	
		EnvData env = dataHelper.getTestEnvData(false);
    	basicSetupService.runBasicSetup(env);
		envService.add(env);
	
		String accessToken = obtainAccessToken(true, env);
		
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create session with auth data
		MockHttpSession session = new MockHttpSession();
		AuthUtil.storePrimaryAuthData(authData, session);
		
		MvcResult result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/standardactions/savedefaultaction")
				.param("typeId", pigeonType.getKID().getId())
				.param("actionType", "list")
				.param("usedAction", "new")
				.param("profileId", authData.getProfile().getId().getId())
				.param("actionName", "SomeTestAction")
				.param("controllerOption", "new")
				.param("viewOption", "new")
				.param("newControllerName", "com.example.PigeonCustomController")
				.param("controllerMethod", "listPigeons")
				.param("newViewName", "pigeonListView")
				.param("url", "getPigeonList")
				.session(session)
				.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
				// make sure a redirect has been returned as a result
	        	.andExpect(status().isMovedTemporarily())
				.andReturn();
		
		//@SuppressWarnings("unchecked")
		//List<String> errorMsgs = (List<String>)result.getModelAndView().getModel().get("errorMsgs");
		//assertNull("Saving action failed. Errors are: " + MiscUtils.implode(errorMsgs, "\n"), errorMsgs);
		
		// make sure a controller has been created
		ClassFilter fileFilter = new ClassFilter();
		fileFilter.setQualifiedName("com.example.PigeonCustomController");
		List<Class> files = kollService.getClasses(fileFilter, env);
		assertTrue("Controller file not created", !files.isEmpty());
		assertTrue("Method not generated in the controller", files.get(0).getKollCode().contains("public " + PageData.class.getName() + " listPigeons"));
		
		// make sure view has been created
		ViewFilter viewFilter = new ViewFilter();
		viewFilter.setName("pigeonListView");
		List<View> views = keetleService.getViews(viewFilter, env);
		assertTrue("View file not created", !views.isEmpty());
		
		// make sure action has been created
		assertNotNull(env.getActionForUrl("getPigeonList"));
    }
}
