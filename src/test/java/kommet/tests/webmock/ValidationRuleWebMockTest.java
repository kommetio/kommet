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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.BasicSetupService;
import kommet.basic.ValidationRule;
import kommet.data.DataService;
import kommet.data.Type;
import kommet.data.validationrules.ValidationRuleFilter;
import kommet.data.validationrules.ValidationRuleService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.tests.TestDataCreator;
import kommet.utils.UrlUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@WebAppConfiguration
@Rollback
public class ValidationRuleWebMockTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ValidationRuleService vrService;
	
	@Inject
	EnvService envService;
	
	@Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    
    private static final Logger log = LoggerFactory.getLogger(ValidationRuleWebMockTest.class);

    @Before
    public void setup()
    {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }
    
    
    @Test
    public void testVRList() throws Exception
    {
    	EnvData env = dataHelper.getTestEnvData(false);
    	basicSetupService.runBasicSetup(env);
		envService.add(env);
		
		// there are some system VRs created during env initiatization
		int initialVRCount = vrService.get(new ValidationRuleFilter(), env).size();
		
		// create session with auth data
		MockHttpSession session = new MockHttpSession();
		AuthUtil.storePrimaryAuthData(dataHelper.getRootAuthData(env), session);
		
        MvcResult result = this.mockMvc.perform(get("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/validationrules/list").session(session).accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
        	.andExpect(status().isOk())
          	.andReturn();
        
        Map<String, Object> model = result.getModelAndView().getModel();
        assertEquals("vrs/list", result.getModelAndView().getViewName());
        assertNotNull(model.get("vrs"));
        assertTrue(model.get("vrs") instanceof List<?>);
        assertEquals(initialVRCount, ((List<?>)model.get("vrs")).size());
        
        // now create some VR
        Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		String rel = "name <> 'mike'";
		
		ValidationRule vr = new ValidationRule();
		vr.setActive(true);
		vr.setCode(rel);
		vr.setTypeId(pigeonType.getKID());
		vr.setName("com.rule.CheckName");
		vr.setErrorMessage("Some message");
		vr.setIsSystem(false);
		vr = vrService.save(vr, authData, env);
		
		result = this.mockMvc.perform(get("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/validationrules/list").session(session).accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
    		.andExpect(status().isOk())
    		.andReturn();
    
		model = result.getModelAndView().getModel();
		assertNotNull(model.get("vrs"));
		assertTrue(model.get("vrs") instanceof List<?>);
		assertEquals(initialVRCount + 1, ((List<?>)model.get("vrs")).size());
    }
}
