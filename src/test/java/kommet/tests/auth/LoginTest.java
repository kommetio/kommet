/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import kommet.tests.BaseUnitTest;

@WebAppConfiguration
public class LoginTest extends BaseUnitTest
{
	@Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setup()
    {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }
    
    @Test
    public void testListView() throws Exception
    {
        MvcResult result = this.mockMvc.perform(get("/textlabels/list").accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
        	.andExpect(status().isOk())
          	.andReturn();
        
        assertEquals("textlabels/list", result.getModelAndView().getViewName());
        assertEquals("textlabels/list", result.getResponse().getForwardedUrl());
        
        Map<String, Object> model = result.getModelAndView().getModel();
        assertNotNull(model.get("labels"));
    }
}
