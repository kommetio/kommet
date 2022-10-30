/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.docs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.DocTemplate;
import kommet.basic.UniqueCheckViolationException;
import kommet.data.DataService;
import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.docs.DocTemplateFilter;
import kommet.docs.DocTemplateService;
import kommet.docs.DocTemplateUtil;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class DocTemplateTest extends BaseUnitTest	
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Inject
	DocTemplateService templateService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Test
	public void testDocTemplateCRUD() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create doc template
		DocTemplate sampleDoc = new DocTemplate();
		sampleDoc.setContent("Test");
		
		try
		{
			templateService.save(sampleDoc, authData, env);
			fail("Saving doc template with empty name should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
		}
		
		sampleDoc.setName("Sample");
		sampleDoc = templateService.save(sampleDoc, authData, env);
		assertNotNull(sampleDoc.getId());
		
		// make sure creating another template with the same name is not possible
		DocTemplate sampleDoc2 = new DocTemplate();
		sampleDoc2.setContent("Test");
		sampleDoc2.setName("Some other name");
		templateService.save(sampleDoc2, authData, env);
		
		DocTemplateFilter filter = new DocTemplateFilter();
		filter.setName(sampleDoc.getName());
		
		List<DocTemplate> templates = templateService.find(filter, env);
		assertEquals(1, templates.size());
		
		// make sure creating another template with the same name is not possible
		DocTemplate sampleDoc3 = new DocTemplate();
		sampleDoc3.setContent("Test");
		sampleDoc3.setName(sampleDoc.getName());
		
		// test getting template by name
		DocTemplate retrievedTemplate = templateService.getByName(sampleDoc2.getName(), env);
		assertNotNull(retrievedTemplate);
		assertEquals(sampleDoc2.getId(), retrievedTemplate.getId());
		
		Integer initialCount = templateService.find(null, env).size();
		assertEquals((Integer)2, initialCount);
		
		// now delete sample doc 2
		templateService.delete(sampleDoc2.getId(), env);
		assertEquals("Template not deleted", (Integer)1, (Integer)templateService.find(null, env).size());
		
		try
		{
			templateService.save(sampleDoc3, authData, env);
			fail("Creating two doc templates with the same name should not be possible");
		}
		catch (UniqueCheckViolationException e)
		{
			assertTrue("Incorrect error message " + e.getMessage(), e.getMessage().startsWith("Unique check violation"));
		}
	}
	
	@Test
	public void testInterpreteContent()
	{
		String rawContent = "Name {{my name}}, age{{age}} and {something}";
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("my name", "Radek");
		values.put("age", 11);
		values.put("unused", "test");
		
		assertEquals("Name Radek, age11 and {something}", DocTemplateUtil.interprete(rawContent, values));
	}
}
