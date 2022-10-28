/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.labels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.TextLabel;
import kommet.basic.ValidationRule;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.validationrules.ValidationRuleService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.i18n.Locale;
import kommet.labels.ManipulatingReferencedLabelException;
import kommet.labels.TextLabelFilter;
import kommet.labels.TextLabelReference;
import kommet.labels.TextLabelService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class TextLabelTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Inject
	TextLabelService labelService;
	
	@Inject
	ValidationRuleService vrService;
	
	@Test
	public void testDeletingUsedLabel() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		String labelKey = "some.key";
		
		TextLabel label = new TextLabel();
		label.setKey(labelKey);
		label.setValue("wartość po polsku");
		label = labelService.save(label, authData, env);
		assertNotNull(label);
		assertNotNull(label.getId());
		
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		ValidationRule vr = new ValidationRule();
		vr.setActive(true);
		vr.setCode("name <> 'mike'");
		vr.setTypeId(pigeonType.getKID());
		vr.setName("CheckName");
		vr.setErrorMessageLabel(labelKey);
		vr.setIsSystem(false);
		vr = vrService.save(vr, authData, env);
		assertNotNull(vr.getId());
		
		// now try do change the text label and make sure it is not possible
		try
		{
			label.setKey("new.key");
			labelService.save(label, authData, env);
			fail("Changing key of label that is used in a validation rule should fail");
		}
		catch (ManipulatingReferencedLabelException e)
		{
			assertTrue(e.getMessage().startsWith("Trying to change text label used by the following validation rules: " + vr.getName()));
			assertEquals(TextLabelReference.VALIDATION_RULE, e.getReference());
		}
		
		// now try to delete the label
		try
		{
			labelService.delete(label.getId(), authData, env);
			fail("Deleting label used in a validation rule should fail");
		}
		catch (ManipulatingReferencedLabelException e)
		{
			assertTrue(e.getMessage().startsWith("Trying to delete text label used by the following validation rules: " + vr.getName()));
			assertEquals(TextLabelReference.VALIDATION_RULE, e.getReference());
		}
		
		TextLabel label1 = new TextLabel();
		label1.setKey("any.value");
		label1.setValue("wartość po polsku");
		label1 = labelService.save(label1, authData, env);
		assertNotNull(label1.getId());
		
		// now update the error message label on the VR
		vr.setErrorMessageLabel("any.value");
		vrService.save(vr, authData, env);
		
		labelService.delete(label.getId(), authData, env);
	}
	
	@Test
	public void testLabelCRUD() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
	
		// add comment to admin user
		TextLabel label = new TextLabel();
		label.setKey("some.key");
		label.setValue("wartość po polsku");
		label = labelService.save(label, authData, env);
		assertNotNull(label);
		assertNotNull(label.getId());
		
		// update label with locale
		label.setLocale(Locale.PL_PL);
		label = labelService.save(label, authData, env);
		
		// add another label with different locale
		labelService.createLabel("some.key", "value in English", Locale.EN_US, authData, env);
		// add another label with the same locale
		labelService.createLabel("another.key", "inna wartość po polsku", Locale.PL_PL, authData, env);
		
		assertEquals(3, labelService.get(new TextLabelFilter(), env).size());
		
		// find labels by key
		TextLabelFilter filter = new TextLabelFilter();
		filter.addKey("some.key");
		List<TextLabel> labels = labelService.get(filter, env);
		assertNotNull(labels);
		assertEquals(2, labels.size());
		
		// find labels by locale
		filter = new TextLabelFilter();
		filter.setLocale(Locale.PL_PL);
		labels = labelService.get(filter, env);
		assertNotNull(labels);
		assertEquals(2, labels.size());
		
		// delete label
		labelService.delete(label.getId(), authData, env);
		assertEquals(2, labelService.get(new TextLabelFilter(), env).size());
		
		// create label without locale
		labelService.createLabel("no.locale", "123", null, authData, env);
		
		// test getting labels from cached dictionary in env
		assertEquals("value in English", env.getTextLabelDictionary().get("some.key", Locale.EN_US));
		assertEquals("inna wartość po polsku", env.getTextLabelDictionary().get("another.key", Locale.PL_PL));
		assertEquals("123", env.getTextLabelDictionary().get("no.locale", null));
		assertEquals("123", env.getTextLabelDictionary().get("no.locale", Locale.PL_PL));
		assertEquals("123", env.getTextLabelDictionary().get("no.locale", Locale.EN_US));
		assertNull(env.getTextLabelDictionary().get("another.key", Locale.EN_US));
		
		try
		{
			labelService.createLabel("key withspace", "value in English", Locale.EN_US, authData, env);
			fail("Creating label whose key contains whitespace should fail");
		}
		catch (KommetException e)
		{
			// expected
		}
		
		// make sure creating two labels with the same locale and same key is not possible
		try
		{
			labelService.createLabel("another.key", "random value", Locale.PL_PL, authData, env);
			fail("Creating two labels with the same locale and same key should fail");
		}
		catch (Exception e)
		{
			// expected
		}
	}
	
	/*@Test
	public void testValidLabelKey() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getAdminAuthData(env);
		
		try
		{
			labelService.createLabel("key withspace", "value in English", Locale.EN_US, authData, env);
			fail("Creating label whose key contains whitespace should fail");
		}
		catch (KommetException e)
		{
			
		}
	}*/
}
