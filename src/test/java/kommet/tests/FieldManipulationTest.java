/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.TypeTrigger;
import kommet.dao.FieldDefinitionException;
import kommet.dao.FieldFilter;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.ValidationErrorType;
import kommet.data.ValidationMessage;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.DateDataType;
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.tests.data.TriggerTest;
import kommet.triggers.TriggerService;
import kommet.triggers.TypeTriggerFilter;
import kommet.utils.MiscUtils;

public class FieldManipulationTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	TriggerService triggerService;
	
	@Inject
	ClassService classService;
	
	@Test
	public void testAddTypeReferenceField() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		int initialFieldCount = dataService.getFields(new FieldFilter(), env).size();
		
		pigeonType = dataService.createType(pigeonType, env);
		
		// add type reference to itself
		Field grandmaField = new Field();
		grandmaField.setApiName("grandma");
		grandmaField.setDataType(new TypeReference(pigeonType));
		grandmaField.setLabel("Grandma");
		pigeonType.addField(grandmaField);
		dataService.createField(grandmaField, env);
		
		pigeonType = env.getType(pigeonType.getKID());
		
		assertEquals(initialFieldCount + pigeonType.getFields().size(), dataService.getFields(new FieldFilter(), env).size());
		
		assertNotNull(pigeonType.getKeyPrefix());
		assertNotNull(pigeonType.getKID());
		
		Type addressType = dataHelper.getAddressType(env);
		Field pigeonField = new Field();
		pigeonField.setApiName("pigeon");
		pigeonField.setDataType(new TypeReference(pigeonType));
		pigeonField.setLabel("Pigeon");
		addressType.addField(pigeonField);
		
		addressType = dataService.createType(addressType, env);
		
		// now test finding the field using filter
		FieldFilter filter = new FieldFilter();
		filter.setDataType(new TypeReference());
		filter.setObjectRefTypeId(pigeonType.getKID());
		List<Field> foundFields = dataService.getFields(filter, env);
		assertEquals(4, foundFields.size());
		
		// find field by name
		filter = new FieldFilter();
		filter.setApiName(pigeonField.getApiName());
		assertFalse(dataService.getFields(filter, env).isEmpty());
		
		pigeonField = env.getType(addressType.getKeyPrefix()).getField("pigeon");
		assertNotNull(pigeonField);
		
		// find field by type KID
		filter.setTypeKID(pigeonField.getType().getKID());
		foundFields = dataService.getFields(filter, env);
		assertEquals(1, foundFields.size());
		assertEquals(pigeonField.getKID(), foundFields.get(0).getKID());
		
		// find field by type name
		filter = new FieldFilter();
		filter.setApiName(pigeonField.getApiName());
		filter.setTypeQualifiedName(pigeonField.getType().getQualifiedName());
		foundFields = dataService.getFields(filter, env);
		assertEquals(1, foundFields.size());
		assertEquals(pigeonField.getKID(), foundFields.get(0).getKID());
		
		Field foundField = null;
		
		for (Field field : foundFields)
		{
			assertNotNull(field.getType());
			assertNotNull(field.getType().getKID());
			assertNull(field.getDescription());
			assertNull(field.getDefaultValue());
			if (field.getType().getKID().equals(addressType.getKID()))
			{
				foundField = field;
				break;
			}
		}
		
		if (foundField == null)
		{
			fail("One of the type reference fields has not been found by a filter search");
		}
		
		assertEquals((Integer)DataType.TYPE_REFERENCE, foundField.getDataTypeId());
		assertEquals(addressType.getKID(), foundField.getType().getKID());
		assertEquals(pigeonType.getKID(), ((TypeReference)foundField.getDataType()).getTypeId());
	}
	
	@Test
	public void makeFieldRequiredFailure() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		
		// add some non-required field
		Field lengthField = new Field();
		lengthField.setApiName("length");
		lengthField.setDataType(new TextDataType(30));
		lengthField.setLabel("Length");
		lengthField.setRequired(false);
		type.addField(lengthField);
		
		type = dataService.createType(type, env);
		assertNotNull(type.getKID());
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(type.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		
		// make sure saving the field with empty length succeeded
		oldPigeon = dataService.save(oldPigeon, env);
		assertNotNull(oldPigeon.getKID());
		
		// now try to make the field required and make sure it failed because there are empty values
		try
		{
			dataService.setFieldRequired(lengthField, true, env);
			fail("Making the field required should have failed because it contains empty values");
		}
		catch (Exception e)
		{
			// exception expected
		}
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testChangeTextFieldLength() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		
		type = dataService.createType(type, env);
		
		testCloneField(type, type.getField("name"), env);
		
		assertEquals((Integer)100, ((TextDataType)type.getField("name").getDataType()).getLength());
		
		// now change the length of the name field - get copy of the field from the env
		Field nameField = env.getFieldForUpdate(type.getKeyPrefix(), "name");
		nameField.setDataType(new TextDataType(5));
		
		Field fieldInstanceBefore = nameField;
		Type typeInstanceBefore = nameField.getType();
		assertTrue(nameField != type.getField("name"));
		
		Field updatedField = dataService.updateField(nameField, AuthData.getRootAuthData(env), env);
		
		// make sure the same instance of type and field are accessed before and after the save
		assertTrue(fieldInstanceBefore == nameField);
		assertTrue(updatedField == nameField);
		assertTrue(typeInstanceBefore == nameField.getType());
		assertFalse(typeInstanceBefore == type);
		assertTrue(fieldInstanceBefore != type.getField("name"));
		
		// fetch updated type from the env
		type = env.getType(type.getKID());
		
		assertEquals("Field length has not been updated on the instance of field bound to the type instance", (Integer)5, ((TextDataType)type.getField("name").getDataType()).getLength());
		
		// make sure it is not possible to create a pigeon with name longer than 5 characters
		Record pigeon = new Record(type);
		pigeon.setField("name", "123456");
		pigeon.setField("age", 3);
		
		try
		{
			dataService.save(pigeon, env);
			fail("Creating record with field value exceeding the maximum length should fail");
		}
		catch (FieldValidationException e)
		{
			boolean correctMsg = false;
			for (ValidationMessage msg : e.getMessages())
			{
				if (msg.getText().contains("exceeds the maximum length"))
				{
					correctMsg = true;
					break;
				}
			}
			// expected exception
			assertTrue(correctMsg);
		}
		
		nameField = env.getType(type.getKID()).getField("name");
		assertEquals((Integer)5, ((TextDataType)nameField.getDataType()).getLength());
		
		testRenameField (type, env);
		
		// fetch the type again so that it contains renamed fields
		type = env.getType(type.getKID());
		
		testFieldDescription (type, env);
		
		// delete all existing pigeons to prepare for subsequent tests
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		dataService.deleteRecords(pigeons, true, dataHelper.getRootAuthData(env), env);
		
		testCreateInvalidNameField(type, env);
		testTextDataTypes(type, env);
		testFieldDefaultTextValue (type, env);
		testFieldDefaultNumericValue (type, env);
		testFieldDefaultObjectReferenceValue (type, env);
		testFieldDefaultCollectionValue (type, env);
		testFieldDefaultValue(type, new DateDataType(), "dateField", "2015-02-07", new Date(115, 1, 7), env);
		testFieldDefaultValue(type, new DateTimeDataType(), "dateTimeField", "2015-02-08", new Date(115, 1, 8), env);
		testFieldDefaultValue(type, new BooleanDataType(), "booleanField", "true", Boolean.TRUE, env);
		testFieldDefaultValue(type, new EnumerationDataType("One\nTwo\nThree"), "enumField", "Two", "Two", env);
		testValidateEnumValues(type, env);
		testValidateFieldName (type, env);
		testGetNonExistingField(type, env);
		testDeleteField(type.getField("age"), type, dataHelper.getRootAuthData(env), env);
	}
	
	@Test
	public void testRequiredField() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create type anew
		pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		Field ageField = pigeonType.getField("age");
		assertNotNull(ageField);
		assertTrue(ageField.isRequired());
		
		// try to create a pigeon with empty age field
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Grzes");
		
		try
		{
			dataService.save(pigeon1, env);
			fail("Saving record with empty required field 'age' should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue(e.getMessage().contains("Required field"));
			assertTrue(e.getMessage().contains("is empty"));
		}
		
		// make the field non-required
		ageField = dataService.getFieldForUpdate(ageField.getKID(), env);
		ageField.setRequired(false);
		dataService.updateField(ageField, authData, env);
		
		pigeonType = env.getType(pigeonType.getKeyPrefix());
		pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Grzes");
		pigeon1 = dataService.save(pigeon1, env);
		assertNotNull(pigeon1.getKID());
	}

	private void testTextDataTypes(Type type, EnvData env) throws KommetException
	{
		Field longTextField = new Field();
		longTextField.setApiName("longText");
		longTextField.setLabel("Long text");
		longTextField.setRequired(false);
		longTextField.setDataType(new TextDataType(2, true, false));
		longTextField.setType(type);
		
		longTextField = dataService.createField(longTextField, env);
		assertNotNull(longTextField.getKID());
		
		Field richTextField = new Field();
		richTextField.setApiName("richText");
		richTextField.setLabel("Rich text");
		richTextField.setRequired(false);
		richTextField.setDataType(new TextDataType(2, true, true));
		richTextField.setType(type);
		
		richTextField = dataService.createField(richTextField, env);
		assertNotNull(richTextField.getKID());
	}

	private void testCreateInvalidNameField(Type type, EnvData env) throws KommetException
	{
		Field field = new Field();
		field.setApiName(Field.ACCESS_TYPE_FIELD_NAME);
		field.setLabel("Test1");
		field.setRequired(false);
		field.setDataType(new TextDataType(2));
		field.setType(type);
		
		try
		{
			dataService.createField(field, env);
			fail("Saving field with name " + Field.ACCESS_TYPE_FIELD_NAME + " should fail");
		}
		catch (KommetException e)
		{
			assertEquals("API name " + field.getApiName() + " is reserved for system fields", e.getMessage());
		}
		
		type = env.getType(type.getKeyPrefix());
		assertEquals((Integer)DataType.NUMBER, type.getField(Field.ACCESS_TYPE_FIELD_NAME).getDataTypeId());
	}

	private void testValidateEnumValues(Type type, EnvData env) throws KommetException
	{
		EnumerationDataType dt = new EnumerationDataType();
		Field field = new Field();
		field.setRequired(false);
		field.setApiName("numbers");
		field.setLabel("Numbers");
		field.setDataType(dt);
		type.addField(field);
		
		try
		{
			field = dataService.createField(field, env);
			fail("Saving enumeration field with no values should fail");
		}
		catch (FieldDefinitionException e)
		{
			// expected
			assertTrue(e.getMessage().startsWith("Enumeration field"));
		}
		
		dt = new EnumerationDataType("Eins\nZwei\nDrei\nVier"); 
		dt.setValidateValues(true);
		field.setDataType(dt);
		field = dataService.createField(field, env);
		
		// create record
		Record pigeon = new Record(type);
		pigeon.setField("firstName", "elli");
		pigeon.setField("age", 3);
		pigeon.setField("numbers", "Zwei");
		pigeon = dataService.save(pigeon, env);
		assertNotNull(pigeon.getKID());
		
		Record pigeon2 = new Record(type);
		pigeon2.setField("firstName", "elli");
		pigeon2.setField("age", 3);
		pigeon2.setField("numbers", "Fuenf");
		
		try
		{
			pigeon2 = dataService.save(pigeon2, env);
			fail("Saving invalid enum value should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
			assertEquals(ValidationErrorType.INVALID_ENUM_VALUE, e.getMessages().get(0).getErrorType());
			assertEquals("Numbers", e.getMessages().get(0).getFieldLabel());
		}
		
		// make sure saving empty value into the validate enum field is possible
		Record pigeon4 = new Record(type);
		pigeon4.setField("firstName", "elli");
		pigeon4.setField("age", 3);
		pigeon4 = dataService.save(pigeon4, env);
		assertNotNull(pigeon4.getKID());
		
		// now make the field not validated
		field = dataService.getFieldForUpdate(field.getKID(), env);
		dt.setValidateValues(false);
		field.setDataType(dt);
		field = dataService.updateField(field, dataHelper.getRootAuthData(env), env);
		
		// create record - this time the value of enum should not be validated
		Record pigeon3 = new Record(env.getType(type.getKeyPrefix()));
		pigeon3.setField("firstName", "elli");
		pigeon3.setField("age", 3);
		pigeon3.setField("numbers", "Fuenf");
		pigeon3 = dataService.save(pigeon3, env);
		assertNotNull(pigeon3.getKID());
	}

	private void testFieldDefaultCollectionValue(Type type, EnvData env) throws KommetException
	{
		type = env.getType(type.getKeyPrefix());
		assertNotNull(type.getFieldsWithDefaultValues());
		assertEquals(0, type.getFieldsWithDefaultValues().size());
		
		Field field = new Field();
		field.setDataType(new InverseCollectionDataType(type, "cousin"));
		field.setRequired(false);
		field.setApiName("cousins");
		field.setLabel("Cousins");
		field.setDefaultValue(type.getKeyPrefix() + "0000000001");
		type.addField(field);
		
		try
		{
			field = dataService.createField(field, env);
			fail("Setting default value for a collection field should fail");
		}
		catch (FieldDefinitionException e)
		{
			// expected
			assertEquals("Collection field cannot have default values", e.getMessage());
		}
	}

	private void testFieldDefaultObjectReferenceValue(Type type, EnvData env) throws KommetException
	{
		type = env.getType(type.getKeyPrefix());
		assertNotNull(type.getFieldsWithDefaultValues());
		assertEquals(0, type.getFieldsWithDefaultValues().size());
		
		String fieldName = "cousin";
		
		// create a cousin record
		Record cousin = new Record(type);
		cousin.setField("firstName", "elli");
		cousin.setField("age", 3);
		cousin = dataService.save(cousin, env);
		
		type = env.getType(type.getKeyPrefix());
		assertNotNull(type.getFieldsWithDefaultValues());
		assertEquals(0, type.getFieldsWithDefaultValues().size());
		
		Field field = new Field();
		field.setDataType(new TypeReference(type));
		field.setRequired(false);
		field.setApiName(fieldName);
		field.setLabel(fieldName);
		field.setDefaultValue(cousin.getKID().getId());
		type.addField(field);
		field = dataService.createField(field, env);
		
		// create new pigeon
		Record pigeon = new Record(type);
		pigeon.setField("firstName", "mikey");
		pigeon.setField("age", 3);
		dataService.save(pigeon, env);
		assertNotNull(pigeon.attemptGetKID());
		
		Type updatedPigeonType = env.getType(type.getKeyPrefix());
		assertNotNull(updatedPigeonType.getFieldsWithDefaultValues());
		assertEquals(1, updatedPigeonType.getFieldsWithDefaultValues().size());
		assertEquals(field.getApiName(), updatedPigeonType.getFieldsWithDefaultValues().get(field.getApiName()).getApiName());
		
		String pigeonTypeName = type.getQualifiedName();
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select " + fieldName + ".id from " + pigeonTypeName + " where id = '" + pigeon.getKID() + "'").list();
		pigeon = pigeons.get(0);
		assertTrue(cousin.getKID().equals((KID)pigeon.getField("cousin.id")));
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// now unset the default value of the field
		field = env.getFieldForUpdate(type.getKeyPrefix(), field.getApiName());
		field.setDefaultValue(null);
		dataService.updateField(field, authData, env);
		
		// make sure the list of fields with default values has been updated on the env
		updatedPigeonType = env.getType(type.getKeyPrefix());
		assertNotNull(updatedPigeonType.getFieldsWithDefaultValues());
		assertEquals(0, updatedPigeonType.getFieldsWithDefaultValues().size());
		
		Record pigeon2 = new Record(updatedPigeonType);
		pigeon2.setField("firstName", "kelly");
		pigeon2.setField("age", 3);
		dataService.save(pigeon2, env);
		assertNotNull(pigeon2.attemptGetKID());
		
		pigeons = env.getSelectCriteriaFromDAL("select " + fieldName + ".id from " + pigeonTypeName + " where id = '" + pigeon2.getKID() + "'").list();
		pigeon2 = pigeons.get(0);
		assertNull("Field should be empty, but has value " + pigeon2.getField(fieldName), pigeon2.attemptGetField(fieldName));
		assertTrue(pigeon2.isSet(fieldName));
	}

	private void testFieldDefaultValue(Type type, DataType dt, String fieldName, String sFieldValue, Object fieldValue, EnvData env) throws KommetException
	{
		type = env.getType(type.getKeyPrefix());
		assertNotNull(type.getFieldsWithDefaultValues());
		assertEquals(0, type.getFieldsWithDefaultValues().size());
		
		Field field = new Field();
		field.setDataType(dt);
		field.setRequired(false);
		field.setApiName(fieldName);
		field.setLabel(fieldName);
		field.setDefaultValue(sFieldValue);
		type.addField(field);
		field = dataService.createField(field, env);
		
		// create new pigeon
		Record pigeon = new Record(type);
		pigeon.setField("firstName", "mikey");
		pigeon.setField("age", 3);
		dataService.save(pigeon, env);
		assertNotNull(pigeon.attemptGetKID());
		
		Type updatedPigeonType = env.getType(type.getKeyPrefix());
		assertNotNull(updatedPigeonType.getFieldsWithDefaultValues());
		assertEquals(1, updatedPigeonType.getFieldsWithDefaultValues().size());
		assertEquals(field.getApiName(), updatedPigeonType.getFieldsWithDefaultValues().get(field.getApiName()).getApiName());
		
		String pigeonTypeName = type.getQualifiedName();
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select " + fieldName + " from " + pigeonTypeName + " where id = '" + pigeon.getKID() + "'").list();
		pigeon = pigeons.get(0);
		assertTrue("Values not equal: " + fieldValue + "(" + fieldValue.getClass().getName() + ") vs. " + pigeon.getField(fieldName), fieldValue.equals(pigeon.getField(fieldName)));
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// now unset the default value of the field
		field = env.getFieldForUpdate(type.getKeyPrefix(), field.getApiName());
		field.setDefaultValue(null);
		dataService.updateField(field, authData, env);
		
		// make sure the list of fields with default values has been updated on the env
		updatedPigeonType = env.getType(type.getKeyPrefix());
		assertNotNull(updatedPigeonType.getFieldsWithDefaultValues());
		assertEquals(0, updatedPigeonType.getFieldsWithDefaultValues().size());
		
		Record pigeon2 = new Record(updatedPigeonType);
		pigeon2.setField("firstName", "kelly");
		pigeon2.setField("age", 3);
		dataService.save(pigeon2, env);
		assertNotNull(pigeon2.attemptGetKID());
		
		pigeons = env.getSelectCriteriaFromDAL("select " + fieldName + " from " + pigeonTypeName + " where id = '" + pigeon2.getKID() + "'").list();
		pigeon2 = pigeons.get(0);
		assertNull("Field should be empty, but has value " + pigeon2.getField(fieldName), pigeon2.attemptGetField(fieldName));
		assertTrue(pigeon2.isSet(fieldName));
	}
	
	private void testFieldDefaultNumericValue(Type type, EnvData env) throws KommetException
	{
		type = env.getType(type.getKeyPrefix());
		assertNotNull(type.getFieldsWithDefaultValues());
		assertEquals(0, type.getFieldsWithDefaultValues().size());
		
		String fieldName = "numericField";
		
		Field numericField = new Field();
		numericField.setDataType(new NumberDataType(2, Double.class));
		numericField.setRequired(false);
		numericField.setApiName(fieldName);
		numericField.setLabel(fieldName);
		numericField.setDefaultValue("1.23");
		type.addField(numericField);
		numericField = dataService.createField(numericField, env);
		
		// create new pigeon
		Record pigeon = new Record(type);
		pigeon.setField("firstName", "mikey");
		pigeon.setField("age", 3);
		dataService.save(pigeon, env);
		assertNotNull(pigeon.attemptGetKID());
		
		Type updatedPigeonType = env.getType(type.getKeyPrefix());
		assertNotNull(updatedPigeonType.getFieldsWithDefaultValues());
		assertEquals(1, updatedPigeonType.getFieldsWithDefaultValues().size());
		assertEquals(numericField.getApiName(), updatedPigeonType.getFieldsWithDefaultValues().get(numericField.getApiName()).getApiName());
		
		String pigeonTypeName = type.getQualifiedName();
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select " + fieldName + " from " + pigeonTypeName + " where id = '" + pigeon.getKID() + "'").list();
		pigeon = pigeons.get(0);
		assertEquals(1.23, pigeon.getField(fieldName));
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// now unset the default value of the field
		numericField = env.getFieldForUpdate(type.getKeyPrefix(), numericField.getApiName());
		numericField.setDefaultValue(null);
		dataService.updateField(numericField, authData, env);
		
		// make sure the list of fields with default values has been updated on the env
		updatedPigeonType = env.getType(type.getKeyPrefix());
		assertNotNull(updatedPigeonType.getFieldsWithDefaultValues());
		assertEquals(0, updatedPigeonType.getFieldsWithDefaultValues().size());
		
		Record pigeon2 = new Record(updatedPigeonType);
		pigeon2.setField("firstName", "kelly");
		pigeon2.setField("age", 3);
		dataService.save(pigeon2, env);
		assertNotNull(pigeon2.attemptGetKID());
		
		pigeons = env.getSelectCriteriaFromDAL("select " + fieldName + " from " + pigeonTypeName + " where id = '" + pigeon2.getKID() + "'").list();
		pigeon2 = pigeons.get(0);
		assertNull("Field should be empty, but has value " + pigeon2.getField(fieldName), pigeon2.attemptGetField(fieldName));
		assertTrue(pigeon2.isSet(fieldName));
	}
	
	private void testFieldDefaultTextValue(Type type, EnvData env) throws KommetException
	{
		assertNotNull(type.getFieldsWithDefaultValues());
		assertEquals(0, type.getFieldsWithDefaultValues().size());
		
		Field textField = new Field();
		textField.setDataType(new TextDataType(100));
		textField.setRequired(false);
		textField.setApiName("textField");
		textField.setLabel("Text Field");
		textField.setDefaultValue("any text");
		textField.setRequired(true);
		type.addField(textField);
		textField = dataService.createField(textField, env);
		
		assertNotNull(type.getField("firstName"));
		
		// create new pigeon
		Record pigeon = new Record(type);
		pigeon.setField("firstName", "mikey");
		pigeon.setField("age", 3);
		dataService.save(pigeon, env);
		assertNotNull(pigeon.attemptGetKID());
		
		Type updatedPigeonType = env.getType(type.getKeyPrefix());
		assertNotNull(updatedPigeonType.getFieldsWithDefaultValues());
		assertEquals(1, updatedPigeonType.getFieldsWithDefaultValues().size());
		assertEquals(textField.getApiName(), updatedPigeonType.getFieldsWithDefaultValues().get(textField.getApiName()).getApiName());
		
		String pigeonTypeName = type.getQualifiedName();
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select textField from " + pigeonTypeName + " where id = '" + pigeon.getKID() + "'").list();
		pigeon = pigeons.get(0);
		assertEquals("any text", pigeon.getField("textField"));
		
		testDefaultValuesInTriggers(type, textField, env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// now unset the default value of the field
		textField = env.getFieldForUpdate(type.getKeyPrefix(), textField.getApiName());
		textField.setDefaultValue(null);
		textField.setRequired(false);
		dataService.updateField(textField, authData, env);
				
		// make sure the list of fields with default values has been updated on the env
		updatedPigeonType = env.getType(type.getKeyPrefix());
		assertNotNull(updatedPigeonType.getFieldsWithDefaultValues());
		assertEquals(0, updatedPigeonType.getFieldsWithDefaultValues().size());
		
		Record pigeon2 = new Record(updatedPigeonType);
		pigeon2.setField("firstName", "kelly");
		pigeon2.setField("age", 3);
		dataService.save(pigeon2, env);
		assertNotNull(pigeon2.attemptGetKID());
		
		pigeons = env.getSelectCriteriaFromDAL("select textField from " + pigeonTypeName + " where id = '" + pigeon2.getKID() + "'").list();
		pigeon2 = pigeons.get(0);
		assertNull("Field should be empty, but has value " + pigeon2.getField("textField"), pigeon2.attemptGetField("textField"));
		assertTrue(pigeon2.isSet("textField"));
	}

	/**
	 * This method checks that in the before insert/update trigger the value of the default field is not yet set
	 * on the record.
	 * @param type
	 * @param textField
	 * @param env
	 * @throws KommetException
	 */
	private void testDefaultValuesInTriggers(Type type, Field textField, EnvData env) throws KommetException
	{
		Record pigeon = new Record(type);
		pigeon.setField("firstName", "jill");
		pigeon.setField("age", 3);
		dataService.save(pigeon, env);
		assertNotNull(pigeon.attemptGetKID());
		
		// make sure the default field value has been set
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select textField from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + pigeon.getKID() + "'").list();
		pigeon = pigeons.get(0);
		assertEquals("any text", pigeon.getField(textField.getApiName()));
		
		// import pigeon proxy class
		List<String> imports = new ArrayList<String>();
		imports.add(MiscUtils.userToEnvPackage(type.getQualifiedName(), env));

		List<String> annotations = new ArrayList<String>();
		annotations.add("@BeforeInsert");
		annotations.add("@BeforeUpdate");
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create trigger that sets the name of every pigeon to "Se eu te pego"
		Class file = TriggerTest.getTriggerFile(type, "for (" + type.getApiName() + " proxy : getNewValues()) { if (proxy.getTextField() == null) { proxy.setTextField(\"value from trigger\"); } }", imports, annotations, classService, authData, env, false);
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		TypeTriggerFilter ttFilter = new TypeTriggerFilter();
		ttFilter.addTriggerFileId(file.getId());
		List<TypeTrigger> typeTriggers = triggerService.find(ttFilter, env);
		assertEquals(1, typeTriggers.size());
		TypeTrigger typeTrigger = typeTriggers.get(0);
		assertTrue(typeTrigger.getIsBeforeInsert());
		assertTrue(typeTrigger.getIsBeforeUpdate());

		Map<KID, TypeTrigger> pigeonTriggers = env.getTriggers(type.getKID());
		assertEquals(1, pigeonTriggers.size());
		
		Record pigeon2 = new Record(type);
		pigeon2.setField("firstName", "kelly");
		pigeon2.setField("age", 3);
		dataService.save(pigeon2, env);
		assertNotNull(pigeon2.attemptGetKID());
		
		pigeon2 = new Record(type);
		pigeon2.setField("firstName", "jill");
		pigeon2.setField("age", 3);
		dataService.save(pigeon2, env);
		assertNotNull(pigeon2.attemptGetKID());
		
		// make sure the default field value has not been set, and instead the value from the trigger has been applied
		pigeons = env.getSelectCriteriaFromDAL("select textField from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + pigeon2.getKID() + "'").list();
		pigeon2 = pigeons.get(0);
		assertEquals("value from trigger", pigeon2.getField(textField.getApiName()));
		
		// unregister trigger
		triggerService.unregisterTriggerWithType(typeTrigger.getId(), env);
		
		pigeonTriggers = env.getTriggers(type.getKID());
		assertEquals(0, pigeonTriggers.size());
		
		// now add the same trigger as after insert trigger
		annotations = new ArrayList<String>();
		annotations.add("@AfterInsert");
		annotations.add("@AfterUpdate");
		
		// delete the old trigger file
		classService.delete(file, dataService, dataHelper.getRootAuthData(env),env);
		
		// build the trigger in such way that is will cause an exception if the text field is not null
		file = TriggerTest.getTriggerFile(type, "for (" + type.getApiName() + " proxy : getNewValues()) { System.out.println(\"a = \" + proxy.getTextField()); if (proxy.getTextField() != null) { Integer a = null; if (true) { String s = a.toString(); } } }", imports, annotations, classService, authData, env, false);
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		
		ttFilter = new TypeTriggerFilter();
		ttFilter.addTriggerFileId(file.getId());
		typeTriggers = triggerService.find(ttFilter, env);
		assertEquals(1, typeTriggers.size());
		typeTrigger = typeTriggers.get(0);
		assertTrue(typeTrigger.getIsAfterInsert());
		assertTrue(typeTrigger.getIsAfterUpdate());
		
		pigeonTriggers = env.getTriggers(type.getKID());
		assertEquals(1, pigeonTriggers.size());
		
		Record pigeon3 = new Record(type);
		pigeon3.setField("firstName", "bill");
		pigeon3.setField("age", 3);
		
		try
		{
			dataService.save(pigeon3, env);
			fail("Trigger should cause a null pointer exception because the default value should be set in it");
		}
		catch (Exception e)
		{
			// expected
		}
		
		triggerService.unregisterTriggerWithType(typeTrigger.getId(), env);
	}

	private void testFieldDescription(Type type, EnvData env) throws KommetException
	{
		String desc = "Pigeon's favourite meal";
		
		Field foodField = new Field();
		foodField.setDataType(new TextDataType(100));
		foodField.setRequired(false);
		foodField.setApiName("favouriteFood");
		foodField.setLabel("Favourite food");
		foodField.setDescription(desc);
		type.addField(foodField);
		foodField = dataService.createField(foodField, env);
		
		foodField = dataService.getField(foodField.getKID(), env);
		assertNotNull(foodField);
		assertEquals(desc, foodField.getDescription());
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// update description
		foodField.setDescription(desc + " - update");
		foodField.setType(type);
		foodField = dataService.updateField(foodField, authData, env);
		assertEquals(desc + " - update", foodField.getDescription());
		
		foodField = env.getFieldForUpdate(type.getKeyPrefix(), foodField.getApiName());
		
		// nullify description
		foodField.setDescription(null);
		foodField = dataService.updateField(foodField, authData, env);
		assertNull(foodField.getDescription());
		
		String tooLongDesc = "";
		for (int i = 0; i <= 512; i++)
		{
			tooLongDesc += "a";
		}
		
		foodField = env.getFieldForUpdate(type.getKeyPrefix(), foodField.getApiName());
		foodField.setDescription(tooLongDesc);
		
		try
		{
			foodField = dataService.updateField(foodField, authData, env);
			fail("Saving field with a too long description should fail");
		}
		catch (KommetException e)
		{
			// expected
			assertEquals("Field description is longer than the allowed 512 characters", e.getMessage());
		}
	}

	private void testGetNonExistingField(Type type, EnvData env) throws KIDException, KommetException
	{
		assertNull("An attempt to get a non-existing field should return null", dataService.getField(KID.get("003abcdeffgtz"), env));
	}

	private void testDeleteField(Field field, Type type, AuthData authData, EnvData env) throws KommetException
	{
		// make sure the field exists
		assertNotNull(env.getType(type.getKeyPrefix()).getField(field.getApiName()));
		
		// make sure the field can be queries in SQL
		env.getJdbcTemplate().execute("select " + field.getDbColumn() + " from " + type.getDbTable() + " limit 1");
		
		// delete the field
		dataService.deleteField(field, true, false, authData, env);
		
		// make sure the field is not longer in the type's representation in env
		assertNull(env.getType(type.getKeyPrefix()).getField(field.getApiName()));
		
		// make sure the field cannot be referenced in a DAL query
		try
		{
			env.getSelectCriteriaFromDAL("select " + field.getApiName() + " from " + type.getQualifiedName() + " limit 1").list();
			fail("Querying deleted field in DAL should have failed");
		}
		catch (KommetException e)
		{
			// expected
		}
		
		// now try creating a new field with the same name
		Field newAgeField = new Field();
		newAgeField.setApiName(field.getApiName());
		newAgeField.setLabel(field.getLabel());
		newAgeField.setRequired(false);
		newAgeField.setDataType(field.getDataType());
		type.addField(newAgeField);
		
		newAgeField = dataService.createField(newAgeField, env);
		assertNotNull(newAgeField.getKID());
		
		// make sure the field exists
		assertNotNull(env.getType(type.getKeyPrefix()).getField(newAgeField.getApiName()));
		
		// make sure the field can be queried through DAL
		List<Record> records = env.getSelectCriteriaFromDAL("select " + field.getApiName() + " from " + type.getQualifiedName() + " limit 1").list();
		assertNotNull(records);
		
		// delete the field again
		dataService.deleteField(newAgeField, true, false, authData, env);
		
		// make sure the field's column has been deleted as well
		try
		{
			env.getJdbcTemplate().execute("select " + newAgeField.getDbColumn() + " from " + type.getDbTable() + " limit 1");
			fail("Querying column for deleted field should have failed, the column should have been deleted with the field");
		}
		catch (BadSqlGrammarException e)
		{
			// expected
		}
	}

	private void testCloneField(Type type, Field field, EnvData env) throws KommetException
	{
		Field clonedField = MiscUtils.cloneField(field);
		assertTrue(clonedField != field);
		assertEquals(field.getId(), clonedField.getId());
		assertEquals(field.getApiName(), clonedField.getApiName());
		assertTrue(field.getType() == clonedField.getType());
		
		// test the getFieldForUpdate method in the same way
		clonedField = dataService.getFieldForUpdate(field.getKID(), env);
		assertTrue(clonedField != field);
		assertEquals(field.getId(), clonedField.getId());
		assertEquals(field.getApiName(), clonedField.getApiName());
		assertFalse(field.getType() == clonedField.getType());
	}

	private void testValidateFieldName(Type type, EnvData env) throws KommetException
	{
		// make sure adding fields with non-English characters is not possible
		Field field = new Field();
		field.setApiName("płęć");
		field.setDataType(new TextDataType(10));
		field.setLabel("Test");
		field.setType(type);
		
		try
		{
			this.dataService.createField(field, env);
			fail("Adding field with API name containing non-English characters should fail");
		}
		catch (KommetException e)
		{
			// expected exception
			assertTrue("Incorrect exception message " + e.getMessage(), e.getMessage().startsWith("Invalid field API name"));
		}
	}

	private void testRenameField(Type type, EnvData env) throws KommetException
	{
		// add some pigeon
		Record pigeon = new Record(type);
		pigeon.setField("name", "hank");
		pigeon.setField("age", 3);
		dataService.save(pigeon, env);
		assertNotNull(pigeon.attemptGetKID());
		
		// rename "name" field to "firstName"
		Field nameField = env.getFieldForUpdate(type.getKeyPrefix(), "name");
		nameField.setApiName("firstName");
		
		// make sure that before the update the original field on env still has the old API name
		assertTrue(!nameField.getApiName().equals(type.getField(nameField.getKID())));
		
		dataService.updateField(nameField, AuthData.getRootAuthData(env), env);
		
		// query the new field name
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select firstName from " + type.getQualifiedName()).list();
		assertEquals(1, pigeons.size());
		Record queriedPigeon = pigeons.get(0);
		assertEquals("hank", queriedPigeon.getField("firstName"));
		assertEquals(pigeon.getField("name"), queriedPigeon.getField("firstName"));
		
		Type updatedType = env.getType(type.getKID());
		Field updatedField = dataService.getField(updatedType.getField("firstName").getKID(), env);
		assertNotNull(updatedField);
		assertNull(updatedType.getField("name"));
		
		// make sure that the old field is also not available in iteration
		// checking this because at some point there was such bug
		for (Field field : updatedType.getFields())
		{
			if (field.getApiName().equals("name"))
			{
				fail("Old field still present in type definion on env");
			}
		}
		
		// now perform a query using the updated field in an order by clause
		// (testing this because it failed at some point)
		env.getSelectCriteriaFromDAL("select id from " + updatedType.getQualifiedName() + " order by firstName");
		
		env.getSelectCriteriaFromDAL("select firstName from " + updatedType.getQualifiedName());
		
		// make sure the actual DB column for the field has also been renamed
		env.getJdbcTemplate().execute("select firstName from " + updatedType.getDbTable());
		
		// make sure the DB info about the field name has been updated as well
		SqlRowSet rowSet = env.getJdbcTemplate().queryForRowSet("select dbcolumn from fields where kid = '" + nameField.getKID() + "'");
		rowSet.next();
		assertEquals("firstName".toLowerCase(), rowSet.getString("dbcolumn").toLowerCase());
	}

	@Test
	public void testMakeFieldRequired() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		
		// add some non-required field
		Field lengthField = new Field();
		lengthField.setApiName("length");
		lengthField.setDataType(new TextDataType(30));
		lengthField.setLabel("Length");
		lengthField.setRequired(false);
		type.addField(lengthField);
		
		type = dataService.createType(type, env);
		lengthField = type.getField(lengthField.getApiName());
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(type.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		oldPigeon.setField("length", "11");
		
		// make sure saving the field with empty length succeeded
		oldPigeon = dataService.save(oldPigeon, env);
		assertNotNull(oldPigeon.getKID());
		
		// make sure that the field can be set to required
		dataService.setFieldRequired(lengthField, true, env);
		assertTrue("Field not made required in the global store (when extracted by ID)", env.getType(type.getKID()).getField("length").isRequired());
		assertTrue("Field not made required in the global store (when extracted by name)", env.getType(type.getQualifiedName()).getField("length").isRequired());
		
		// make sure saving a record with an empty length fails
		Record youngPigeon = dataService.instantiate(type.getKID(), env);
		youngPigeon.setField("name", "Heniek");
		youngPigeon.setField("age", 2);
		
		try
		{
			dataService.save(youngPigeon, env);
			fail("Saving record with empty required field should have failed");
		}
		catch (FieldValidationException e)
		{
			// exception expected
		}
		
		youngPigeon.setField("length", "13");
		dataService.save(youngPigeon, env);
		assertNotNull(youngPigeon.getKID());
	}
}
