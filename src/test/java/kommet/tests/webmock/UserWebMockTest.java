/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.webmock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import kommet.basic.DocTemplate;
import kommet.data.Record;
import kommet.docs.DocTemplateService;
import kommet.env.EnvData;
import kommet.utils.AppConfig;
import kommet.utils.UrlUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@WebAppConfiguration
@Rollback
public class UserWebMockTest extends BasicWebMockTest
{
	@Inject
	AppConfig config;
	
	@Inject
	DocTemplateService docTemplateService;
	
	@SuppressWarnings("unchecked")
	@Test
    public void testRemindPasswordForm() throws Exception
    {
		MvcResult result = this.mockMvc.perform(get("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/forgottenpassword").accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
    	.andExpect(status().isOk())
      	.andReturn();
    
		assertEquals("users/forgottenpassword", result.getModelAndView().getViewName());
		assertEquals("users/forgottenpassword", result.getResponse().getForwardedUrl());
		
		EnvData env = dataHelper.getTestEnvData(false);
    	basicSetupService.runBasicSetup(env);
		envService.add(env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create restore password email template user by the restore password action
		DocTemplate restorePwdTemplate = new DocTemplate();
		restorePwdTemplate.setContent("Any content");
		restorePwdTemplate.setName(config.getRestorePasswordEmailTemplate());
		restorePwdTemplate = docTemplateService.save(restorePwdTemplate, authData, env);
		assertNotNull(restorePwdTemplate.getId());
		
		// create session with auth data
		MockHttpSession session = new MockHttpSession();
		AuthUtil.storePrimaryAuthData(authData, session);
		
		Record profile = dataService.save(dataHelper.getTestProfile("Test Profile", env), env);
		Record user = dataService.save(dataHelper.getTestUser("user@kommet.io", "user@kommet.io", profile, env), env);
		
        result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/sendpasswordlink")
        	.param("email", (String)user.getField("email"))
        	.param("envId", env.getId().getId())
        	.session(session).accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
        	.andExpect(status().isOk())
          	.andReturn();
        
        assertEquals("common/msg", result.getModelAndView().getViewName());
		
		// now deactivate the user and make sure the password remind feature cannot be used
		// for inactive users
		user.setField("isActive", false);
		dataService.save(user, env);
		
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/sendpasswordlink")
	        	.param("email", (String)user.getField("email"))
	        	.param("envId", env.getId().getId())
	        	.session(session).accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
	          	.andReturn();
		
		assertEquals("users/forgottenpassword", result.getModelAndView().getViewName());
		
		List<String> errors = (List<String>)result.getModelAndView().getModel().get("errorMsgs");
		assertEquals(1, errors.size());
		assertEquals(authData.getI18n().get("auth.cannot.remind.pwd.inactive.user"), errors.get(0));
    }
}
