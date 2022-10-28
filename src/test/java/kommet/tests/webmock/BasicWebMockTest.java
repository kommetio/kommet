/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.webmock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import kommet.basic.BasicSetupService;
import kommet.basic.RecordProxyUtil;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.Record;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.tests.TestDataCreator;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

public class BasicWebMockTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Inject
	EmailService emailService;
	
	@Inject
	EnvService envService;
	
	@Inject
    private WebApplicationContext wac;

    protected MockMvc mockMvc;
    
    @Before
    public void setup()
    {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }
    
    protected String obtainAccessToken(boolean isRoot, EnvData env) throws Exception
    {
    	return obtainAccessToken(dataHelper, dataService, mockMvc, isRoot, env);
    }
    
    @SuppressWarnings("unchecked")
	public static String obtainAccessToken(TestDataCreator dataHelper, DataService dataService, MockMvc mockMvc, boolean isRoot, EnvData env) throws Exception
    {
    	Record profile = null;
    	
    	if (isRoot)
    	{
    		// user root profile
    		profile = RecordProxyUtil.generateRecord(dataHelper.getRootAuthData(env).getProfile(), env.getType(KeyPrefix.get(KID.PROFILE_PREFIX)), 2, env);
    	}
    	else
    	{
    		// create new profile
    		profile = dataService.save(dataHelper.getTestProfile("TestProfile", env), env); 
    	}
    	
    	int rand = (new Random()).nextInt(10000);
    	
		Record user = dataHelper.getTestUser("user" + rand + "@kommet.io", "user" + rand + "@kommet.io", profile, env);
		user.setField("password", MiscUtils.getSHA1Password("abc"));
		user = dataService.save(user, env);
		
		MvcResult result = mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.OAUTH_GET_TOKEN_URL).accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
    		.andExpect(status().is(HttpServletResponse.SC_BAD_REQUEST))
    		.andReturn();
    	
        result = mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.OAUTH_GET_TOKEN_URL)
        	.param("grant_type", "password")
        	.param("username", (String)user.getField("userName"))
        	.param("password", "abc")
        	.param("client_id", "any")
        	.param("client_secret", "any")
        	.param("env", env.getId().getId())
        	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
        	.andExpect(status().isOk())
          	.andReturn();
        
        Map<String, Object> tokenJSON = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);
        return (String)tokenJSON.get("access_token");
    }
    
    protected String obtainAccessToken (String userName, String password, EnvData env) throws Exception
    {
    	MvcResult result = mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.OAUTH_GET_TOKEN_URL)
            	.param("grant_type", "password")
            	.param("username", userName)
            	.param("password", password)
            	.param("client_id", "any")
            	.param("client_secret", "any")
            	.param("env", env.getId().getId())
            	.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
            	.andExpect(status().isOk())
              	.andReturn();
            
        @SuppressWarnings("unchecked")
		Map<String, Object> tokenJSON = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);
        return (String)tokenJSON.get("access_token");
    }
}
