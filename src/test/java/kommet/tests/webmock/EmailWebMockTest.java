/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.webmock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import kommet.auth.AuthUtil;
import kommet.basic.BasicSetupService;
import kommet.data.DataService;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.tests.TestDataCreator;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@WebAppConfiguration
@Rollback
public class EmailWebMockTest
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

    private MockMvc mockMvc;
    
    @Before
    public void setup()
    {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }
    
    @Test
    public void testEmailList() throws Exception
    {
    	EnvData env = dataHelper.getTestEnvData(false);
    	basicSetupService.runBasicSetup(env);
		envService.add(env);
		
		// send email
		MockHttpSession session = new MockHttpSession();
		AuthUtil.storePrimaryAuthData(dataHelper.getRootAuthData(env), session);
		
        MvcResult result = this.mockMvc.perform(post("/mail/send")
        	.param("recipients", "test@kommet.io")
        	.param("subject", "something")
        	.param("content", "abcd")
        	.session(session).accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
        	//.andExpect(status().isOk())
          	.andReturn();
        
        ((Exception)result.getModelAndView().getModelMap().get("exception")).printStackTrace();
        
        //System.out.println(((Exception)result.getModelAndView().getModelMap().get("exception")).getMessage());
    }
}
