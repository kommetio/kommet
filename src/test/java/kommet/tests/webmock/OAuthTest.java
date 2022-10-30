/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.webmock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
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

import kommet.data.Record;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@WebAppConfiguration
@Rollback	
public class OAuthTest extends BasicWebMockTest
{
	@SuppressWarnings("unchecked")
	@Test
    public void testGetAccessToken() throws Exception
    {	
		EnvData env = dataHelper.getTestEnvData(false);
    	basicSetupService.runBasicSetup(env);
		envService.add(env);
		
		// create session with auth data
		MockHttpSession session = new MockHttpSession();
		//AuthUtil.storePrimaryAuthData(dataHelper.getAdminAuthData(env), session);
		
		Record profile = dataService.save(dataHelper.getTestProfile("Test Profile", env), env);
		Record user = dataHelper.getTestUser("user@kommet.io", "user@kommet.io", profile, env);
		user.setField("password", MiscUtils.getSHA1Password("abc"));
		user = dataService.save(user, env);
		
		MvcResult result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.OAUTH_GET_TOKEN_URL).accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
    		.andExpect(status().is(HttpServletResponse.SC_BAD_REQUEST))
    		.andReturn();
    	
        result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.OAUTH_GET_TOKEN_URL)
        	.param("grant_type", "password")
        	.param("username", (String)user.getField("userName"))
        	.param("password", "abc")
        	.param("client_id", "any")
        	.param("client_secret", "any")
        	.param("env", env.getId().getId())
        	//.session(session)
        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
        	.andExpect(status().isOk())
          	.andReturn();
        
        // make sure GET method is not supported
        this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.OAUTH_GET_TOKEN_URL)
            	.param("grant_type", "password")
            	.param("username", (String)user.getField("userName"))
            	.param("password", (String)user.getField("password"))
            	.param("client_id", "any")
            	.param("client_secret", "any")
            	.param("env", env.getId().getId())
            	.session(session).accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
            	.andExpect(status().isForbidden());
        
        String responseBody = result.getResponse().getContentAsString();
        
        //System.out.println("Body " + responseBody);
        
        assertTrue("Invalid response: " + responseBody, responseBody.contains("\"access_token\": \""));
        
        Map<String, Object> tokenJSON = new ObjectMapper().readValue(responseBody, HashMap.class);
        String accessToken = (String)tokenJSON.get("access_token");
        assertNotNull("Access token not found in response: " + responseBody, accessToken);
        
        // query user type through rest api
        String query = "select id, userName from User limit 100";
        String url = "/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DAL_URL + "?q=" + query + "&env=" + env.getId();
        result = this.mockMvc.perform(get(url)
        	.session(session)
        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
			.andExpect(status().is(HttpServletResponse.SC_FORBIDDEN))
			.andReturn();
        
        responseBody = result.getResponse().getContentAsString();
        
        //System.out.println("Body 1" + responseBody);
        
        result = this.mockMvc.perform(get(url)
        		.param("access_token", accessToken)
            	.session(session)
            	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
    			.andExpect(status().isOk())
    			.andReturn();
        
        responseBody = result.getResponse().getContentAsString();
        
        //System.out.println("Body 2" + responseBody);
    }
}
