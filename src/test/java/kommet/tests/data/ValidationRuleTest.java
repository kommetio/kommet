/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.ValidationRule;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.ValidationErrorType;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.validationrules.ValidationRuleCompileException;
import kommet.data.validationrules.ValidationRuleException;
import kommet.data.validationrules.ValidationRuleService;
import kommet.data.validationrules.ValidationRuleUtil;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.labels.InvalidTextLabelReferenceException;
import kommet.labels.TextLabelReference;
import kommet.labels.TextLabelService;
import kommet.rel.RELSyntaxException;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class ValidationRuleTest extends BaseUnitTest
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
	KommetCompiler compiler;
	
	@Inject
	TextLabelService textLabelService;
	
	@Inject
	EnvService envService;
	
	@Test
	public void testValidationRuleCRUD() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		assertNotNull(authData.getLocale());
		assertNotNull(authData.getI18n());
		
		// before creating the rule, create a record that does not meet its criteria
		Record pigeon0 = new Record(pigeonType);
		pigeon0.setField("name", "john");
		pigeon0.setField("age", BigDecimal.valueOf(1));
		assertNotNull(dataService.save(pigeon0, env).getKID());
		
		ValidationRule vr = new ValidationRule();
		vr.setActive(true);
		vr.setCode("name <> 'mike'");
		vr.setTypeId(pigeonType.getKID());
		vr.setName("CheckName");
		vr.setIsSystem(false);
		
		try
		{
			vr = vrService.save(vr, authData, env);
			fail("Saving a validation rule with empty error message fields should fail");
		}
		catch (ValidationRuleException e)
		{
			assertEquals("Neither errorMessage nor errorMessageLabel is filled on validation rule", e.getMessage());
		}
		
		vr.setErrorMessage("Invalid name \"Quote\"");
		
		try
		{
			vr = vrService.save(vr, authData, env);
		}
		catch (ValidationRuleCompileException e)
		{
			e.printStackTrace();
			fail("Error compiling validation rule executor: " + e.getCompilationResult());
		}
		
		assertNotNull(vr.getId());
		assertNotNull(vrService.getByName(vr.getName(), authData, env));
		
		assertTrue("Validation rule not registered on the environment", env.hasValidationRules(pigeonType.getKID()));
		
		ValidationRule ageVR = new ValidationRule();
		ageVR.setActive(true);
		ageVR.setCode("age > 5");
		ageVR.setTypeId(pigeonType.getKID());
		ageVR.setName("CheckAge");
		ageVR.setIsSystem(false);
		ageVR.setErrorMessage("Incorrect age");
		ageVR = vrService.save(ageVR, authData, env);
		
		// now try to save a pigeon with correct name
		Record pigeon = new Record(pigeonType);
		pigeon.setField("name", "ariel");
		pigeon.setField("age", BigDecimal.valueOf(22));
		assertNotNull(dataService.save(pigeon, env).getKID());
		
		// now try to update a record that does not meet the validatio rule criteria,
		// but without the fields for which the rule is created being set
		pigeon0 = env.getSelectCriteriaFromDAL("select id, name from " + pigeonType.getQualifiedName() + " where id = '" + pigeon0.getKID() + "'").singleRecord();
		pigeon0.setField("name", "lee");
		dataService.save(pigeon0, env);
		
		// delete the age rule
		vrService.delete(ageVR, authData, env);
		
		// try to save a pigeon with name disallowed by the validation rule
		Record newPigeon = new Record(pigeonType);
		newPigeon.setField("name", "mike");
		newPigeon.setField("age", BigDecimal.valueOf(22));
		
		try
		{
			dataService.save(newPigeon, env);
			fail("Saving pigeon should fail because its name is disallowed by a validation rule");
		}
		catch (FieldValidationException e)
		{
			assertEquals(1, e.getMessages().size());
			assertEquals("Invalid name \"Quote\"", e.getMessages().get(0).getText());
			assertNull(e.getMessages().get(0).getFieldLabel());
			assertEquals(ValidationErrorType.VALIDATION_RULE_VIOLATION, e.getMessages().get(0).getErrorType());
		}
		
		// update the validation rule with a new error message label
		// that does not exist
		vr.setErrorMessageLabel("err.invalid.name");
		try
		{
			vr = vrService.save(vr, authData, env);
			fail("Saving pigeon should fail because its message references a non-existing message");
		}
		catch (InvalidTextLabelReferenceException e)
		{
			// make sure that the standard label was used if error message label is not defined
			assertEquals(TextLabelReference.VALIDATION_RULE, e.getReference());
			assertEquals("err.invalid.name", e.getTextLabelKey());
		}
		
		String errMessage = "Invalid name (i18)";
		
		// now add the text label
		textLabelService.createLabel("err.invalid.name", errMessage, authData.getLocale(), authData, env);
		vr = vrService.save(vr, authData, env);
		
		try
		{
			dataService.save(newPigeon, authData, env);
			fail("Saving pigeon should fail because its name is disallowed by a validation rule");
		}
		catch (FieldValidationException e)
		{
			assertEquals(1, e.getMessages().size());
			assertEquals(errMessage, e.getMessages().get(0).getText());
			assertNull(e.getMessages().get(0).getFieldLabel());
			assertEquals(ValidationErrorType.VALIDATION_RULE_VIOLATION, e.getMessages().get(0).getErrorType());
		}
		
		// change the pigeon name again to a valid one
		newPigeon.setField("name", "michel");
		dataService.save(newPigeon, env);
		
		// now add another validation rule
		ValidationRule vr2 = new ValidationRule();
		vr2.setActive(true);
		vr2.setCode("name <> 'abc'");
		vr2.setTypeId(pigeonType.getKID());
		vr2.setName("CheckAge");
		vr2.setErrorMessage("Name cannot be 'abc'");
		vr2.setIsSystem(false);
		vr2 = vrService.save(vr2, authData, env);
		
		newPigeon.setField("name", "abc");
		try
		{
			dataService.save(newPigeon, authData, env);
			fail("Saving pigeon should fail because its name is disallowed by a validation rule");
		}
		catch (FieldValidationException e)
		{
			assertEquals(1, e.getMessages().size());
			assertEquals("Name cannot be 'abc'", e.getMessages().get(0).getText());
			assertNull(e.getMessages().get(0).getFieldLabel());
			assertEquals(ValidationErrorType.VALIDATION_RULE_VIOLATION, e.getMessages().get(0).getErrorType());
		}
		
		// now delete one validation rule
		vrService.delete(vr2.getId(), authData, env);
		dataService.save(newPigeon, authData, env);
		
		// now deactivate the first validation rule
		vr.setActive(false);
		vrService.save(vr, authData, env);
		newPigeon.setField("name", "mike");
		dataService.save(newPigeon, env);
		assertFalse("Validation rules still visible on env although all have been deactivated", env.hasValidationRules(pigeonType.getKID()));
		
		// now activate the rule again and make sure it works
		vr.setActive(true);
		vrService.save(vr, authData, env);
		try
		{
			dataService.save(newPigeon, authData, env);
			fail("Saving pigeon should fail because its name is disallowed by a validation rule");
		}
		catch (FieldValidationException e)
		{
			assertEquals(1, e.getMessages().size());
			assertEquals(errMessage, e.getMessages().get(0).getText());
			assertNull(e.getMessages().get(0).getFieldLabel());
			assertEquals(ValidationErrorType.VALIDATION_RULE_VIOLATION, e.getMessages().get(0).getErrorType());
		}
		
		assertTrue(env.hasValidationRules(pigeonType.getKID()));
		
		// the test below should be activated, but it was deactivated because there were problems
		// with recreating the db connection during the second env initialization
		
		// now deactivate it again and make sure after env reinitialization it's still inactive
		vr.setActive(false);
		vrService.save(vr, authData, env);
		
		envService.clear(env.getId());
		env = envService.get(env.getId());
		newPigeon.setField("name", "mike");
		dataService.save(newPigeon, env);
		
		// now try saving the pigeon without the name field set
		Record pigeonWithoutSetFields = new Record(pigeonType);
		pigeonWithoutSetFields.setKID(newPigeon.getKID());
		dataService.save(pigeonWithoutSetFields, env);
		
		testBooleanConditions(pigeonType, authData, env);
	}
	
	private void testBooleanConditions(Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		// add boolean field to type
		Field isActive = new Field();
		isActive.setApiName("isActive");
		isActive.setLabel("Is Active");
		isActive.setDataType(new BooleanDataType());
		isActive.setRequired(false);
		pigeonType.addField(isActive);
		dataService.createField(isActive, env);
		
		// add rule with a boolean condition
		ValidationRule vr = new ValidationRule();
		vr.setActive(true);
		vr.setCode("isActive == true");
		vr.setTypeId(pigeonType.getKID());
		vr.setName("CheckActive");
		vr.setErrorMessage("Record must be active");
		vr.setIsSystem(false);
		vr = vrService.save(vr, authData, env);
		
		// try to save a pigeon and make sure it fails
		Record pigeon = new Record(pigeonType);
		pigeon.setField("name", "Blake");
		pigeon.setField("age", BigDecimal.valueOf(22));
		
		try
		{
			dataService.save(pigeon, env);
			fail("Saving record that violates a validation rule should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue(e.getMessage().startsWith(vr.getErrorMessage()));
		}
		
		// now set the value explicitly to false
		pigeon.setField("isActive", false);
		
		try
		{
			dataService.save(pigeon, env);
			fail("Saving record that violates a validation rule should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue(e.getMessage().startsWith(vr.getErrorMessage()));
		}
		
		vr.setCode("isActive == false");
		vr = vrService.save(vr, authData, env);
		assertNotNull(dataService.save(pigeon, env).getKID());
	}

	@Test
	public void testCreateVRE() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		String rel = "(name <> 'mike'";
		
		ValidationRule vr = new ValidationRule();
		vr.setActive(true);
		vr.setCode(rel);
		vr.setTypeId(pigeonType.getKID());
		vr.setName("CheckName");
		vr.setErrorMessage("Some message");
		vr.setIsSystem(false);
		
		try
		{
			vr = vrService.save(vr, authData, env);
			fail("Parsing REL with unclosed brackets should fail");
		}
		catch (RELSyntaxException e)
		{
			assertEquals("REL expression contains unterminated brackets: " + rel, e.getMessage());
		}
		
		vr.setCode("name <> 'mike'");
		vr = vrService.save(vr, authData, env);
		assertNotNull(vr.getId());
		
		Set<ValidationRule> vrs = new HashSet<ValidationRule>();
		vrs.add(vr);
		Class vre = ValidationRuleUtil.getValidationRuleExecutor(pigeonType, null, vrs, compiler, env);
		assertNotNull(vre);
		CompilationResult res = compiler.compile(vre, env);
		assertTrue(res.isSuccess());
		
		testDateValidationRule(pigeonType, authData, env);
		
		ValidationRule invalidRule = new ValidationRule();
		invalidRule.setActive(true);
		invalidRule.setCode("sth < 12 or age > 12 or this.that == 'av' or father.id == 'a' or father.name == 'dsds'");
		invalidRule.setTypeId(pigeonType.getKID());
		invalidRule.setName("CheckInvalidCondition");
		invalidRule.setErrorMessage("Some message");
		invalidRule.setIsSystem(false);
		
		try
		{
			vrService.save(invalidRule, authData, env);
			fail("Saving rule with invalid fields should fail");
		}
		catch (ValidationRuleException e)
		{
			assertTrue(e.getMessage().contains("Validation rule references invalid fields: "));
			assertTrue(e.getMessage().contains("sth"));
			assertTrue(e.getMessage().contains("this.that"));
			assertTrue(e.getMessage().contains("father.name"));
		}
		
		invalidRule.setCode("'aass' == 'sas'");
		try
		{
			vrService.save(invalidRule, authData, env);
			fail("Saving rule with invalid fields should fail");
		}
		catch (ValidationRuleException e)
		{
			assertEquals("Validation rule condition does not reference any fields", e.getMessage());
		}
	}

	private void testDateValidationRule(Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		ValidationRule vr = new ValidationRule();
		vr.setActive(true);
		vr.setCode("createdDate <= birthdate");
		vr.setTypeId(pigeonType.getKID());
		vr.setName("CheckBirthDate");
		vr.setErrorMessage("Some message");
		vr.setIsSystem(false);
		vr = vrService.save(vr, authData, env);
		assertNotNull(vr.getId());
		
		// TODO add a test that actually inserts records whose dates violate the rule, and make sure it works
	}
}
