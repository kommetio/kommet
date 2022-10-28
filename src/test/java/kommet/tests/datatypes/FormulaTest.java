/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.datatypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyUtil;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldRemovalException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.FormulaParser;
import kommet.data.datatypes.FormulaReturnType;
import kommet.data.datatypes.FormulaSyntaxException;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class FormulaTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppConfig config;
	
	@Inject
	KommetCompiler compiler;
	
	@SuppressWarnings("unchecked")
	@Test
	public void testFormulaField() throws KommetException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getPigeonType(env);
		
		Field firstNameField = new Field();
		firstNameField.setApiName("firstName");
		firstNameField.setDataType(new TextDataType(100));
		firstNameField.setLabel("First Name");
		firstNameField.setRequired(true);
		type.addField(firstNameField);
		
		Field lastNameField = new Field();
		lastNameField.setApiName("lastName");
		lastNameField.setDataType(new TextDataType(100));
		lastNameField.setLabel("Last Name");
		lastNameField.setRequired(true);
		type.addField(lastNameField);
		
		Field ageField = new Field();
		ageField.setApiName("age");
		ageField.setDataType(new NumberDataType(0, Integer.class));
		ageField.setLabel("Age");
		ageField.setRequired(false);
		type.addField(ageField);
		
		type = dataService.createType(type, env);
		assertNotNull(type.getKID());
		
		// create address type that references pigeon type
		Type addressType = dataHelper.getAddressType(env);
		
		Field pigeonField = new Field();
		pigeonField.setApiName("pigeon");
		pigeonField.setDataType(new TypeReference(type));
		pigeonField.setLabel("Pigeon");
		pigeonField.setRequired(false);
		addressType.addField(pigeonField);
		
		addressType = dataService.createType(addressType, env);
		
		// add full address field
		Field fullAddressField = new Field();
		fullAddressField.setApiName("fullAddress");
		fullAddressField.setLabel("Full Address");
		fullAddressField.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "city + \", \" + street", addressType, env));
		addressType.addField(fullAddressField);
		
		dataService.createField(fullAddressField, env);
		
		// add full address field
		Field testFormulaField = new Field();
		testFormulaField.setApiName("testField");
		testFormulaField.setLabel("Test Field");
		testFormulaField.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "city + \" something\"", addressType, env));
		addressType.addField(testFormulaField);
		
		dataService.createField(testFormulaField, env);
		
		// add address collection to pigeon type
		Field addressField = new Field();
		addressField.setApiName("addresses");
		addressField.setDataType(new InverseCollectionDataType(addressType, "pigeon"));
		addressField.setLabel("Addresses");
		addressField.setRequired(false);
		type.addField(addressField);
		
		dataService.createField(addressField, env);
		
		type = env.getType(type.getKID());
		
		testFormulaParser(env, type);
		
		Field formulaField = new Field();
		formulaField.setApiName("fullName");
		formulaField.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "firstName + \" \" + lastName", type, env));
		formulaField.setLabel("Full Name");
		formulaField.setRequired(true);
		type.addField(formulaField);
		
		// create another formula field without a space between plus and property name
		Field formulaField2 = new Field();
		formulaField2.setApiName("fullName2");
		formulaField2.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "firstName+\" \" + lastName", type, env));
		formulaField2.setLabel("Full Name 2");
		formulaField2.setRequired(false);
		type.addField(formulaField2);
		
		try
		{
			dataService.createField(formulaField, env);
			fail("Making formula field required should have failed");
		}
		catch (KommetException e)
		{
			assertTrue(e.getMessage().equals("Formula field fullName cannot be made required"));
		}
		
		formulaField.setRequired(false);
		formulaField = dataService.createField(formulaField, env);
		formulaField2 = dataService.createField(formulaField2, env);
		assertNotNull(formulaField.getKID());
		assertNotNull(formulaField2.getKID());
		
		Record record = new Record(type);
		record.setField("firstName", "Grzegorz");
		record.setField("lastName", "Krawiec");
		record = dataService.save(record, env);
		assertNotNull(record.getKID());
		
		List<Record> records = env.getSelectCriteriaFromDAL("select fullName, fullName2, addresses.id, addresses.fullAddress from " + type.getQualifiedName()).list();
		assertEquals(1, records.size());
		assertEquals("Grzegorz Krawiec", records.get(0).getField("fullName"));
		assertEquals("Grzegorz Krawiec", records.get(0).getField("fullName2"));
		List<Record> retrievedAddresses = (List<Record>)records.get(0).getField("addresses");
		assertEquals(0, retrievedAddresses.size());
		
		// now generate a proxy for the pigeon object
		Class<? extends RecordProxy> pigeonProxyClass = (Class<? extends RecordProxy>)compiler.getClass(type.getQualifiedName(), true, env);
		assertNotNull(pigeonProxyClass);
		Object typeProxy = RecordProxyUtil.generateCustomTypeProxy(pigeonProxyClass, records.get(0), true, env);
		assertNotNull(typeProxy);
		assertTrue(typeProxy.getClass().isAssignableFrom(pigeonProxyClass));
		
		Method fullNameGetter = null;
		try
		{
			fullNameGetter = typeProxy.getClass().getMethod("getFullName");
		}
		catch (SecurityException e)
		{
			fail("Method getFullName is not accessible in generated proxy class");
		}
		catch (NoSuchMethodException e)
		{
			fail("Method getFullName does not exist in generated proxy class");
		}
		
		assertEquals(String.class.getName(), fullNameGetter.getReturnType().getName());
		Object fullName = fullNameGetter.invoke(typeProxy);
		assertNotNull(fullName);
		assertEquals("Grzegorz Krawiec", fullName);
		
		// All previous tests operated on pigeon records to which no addresses where assigned
		// This next test tests fetching pigeon object with related addresses which contain a formula field.
		// It is essential that if the formula field depend on two fields (in this case street and city),
		// and that one of them be null.
		Record address = new Record(addressType);
		address.setField("city", "Piecki");
		address.setField("pigeon", record);
		address = dataService.save(address, env);
		assertNotNull(address.getKID());
		
		// now fetch parent record with address collection
		records = env.getSelectCriteriaFromDAL("select fullName, fullName2, addresses.id, addresses.fullAddress from " + type.getQualifiedName() + " WHERE " + Field.ID_FIELD_NAME + " = '" + record.getKID() + "'").list();
		assertEquals(1, records.size());
		Record retrievedRecord = records.get(0);
		assertEquals("Grzegorz Krawiec", retrievedRecord.getField("fullName"));
		assertEquals("Grzegorz Krawiec", retrievedRecord.getField("fullName2"));
		retrievedAddresses = (List<Record>)retrievedRecord.getField("addresses");
		assertEquals(1, retrievedAddresses.size());
		assertEquals("Piecki, ", retrievedAddresses.get(0).getField("fullAddress"));
		
		List<Field> formulaFields = dataService.getFormulaFieldsUsingField(addressType.getField("street").getKID(), env);
		assertEquals(1, formulaFields.size());
		assertEquals(env.getType(addressType.getKID()).getField("fullAddress").getKID(), formulaFields.get(0).getKID());
		
		// now try to remove a field used in the formula field and make sure it's not possible
		try
		{
			dataService.deleteField(addressType.getField("street"), dataHelper.getRootAuthData(env), env);
			fail("Deleting field used in formula fields should fail");
		}
		catch (FieldRemovalException e)
		{
			assertEquals("Field cannot be deleted because it is used in formula fields", e.getMessage());
		}
		
		// add full address field
		Field invalidField = new Field();
		invalidField.setApiName("fullAddress");
		invalidField.setLabel("Full Address");
		
		try
		{
			invalidField.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "city + \", \" + fullAddress", addressType, env));
			fail("Saving formula field that uses another formula field should fail");
		}
		catch (KommetException e)
		{
			assertEquals("Field fullAddress cannot be used in formula field because it is a formula itself", e.getMessage());
		}
		
		try
		{
			invalidField.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "city + \", \" street", addressType, env));
			fail("Saving formula with invalid syntax should fail");
		}
		catch (FormulaSyntaxException e)
		{
			assertEquals("Misplaced token street", e.getMessage());
		}
		
		try
		{
			invalidField.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "city \", \" + street", addressType, env));
			fail("Saving formula with invalid syntax should fail");
		}
		catch (FormulaSyntaxException e)
		{
			assertEquals("Misplaced token \", \"", e.getMessage());
		}
	}

	private void testFormulaParser(EnvData env, Type type) throws KommetException
	{
		String userDef = "firstName + \" \" + lastName";
		String parsedDef = FormulaParser.parseDefinition(FormulaReturnType.TEXT, userDef, type, env);
		assertEquals(FormulaParser.FIELD_VAR_PREFIX + "{" + type.getField("firstName").getKID() + "} || ' ' || " + FormulaParser.FIELD_VAR_PREFIX + "{" + type.getField("lastName").getKID() + "}" , parsedDef);
		
		String sqlDef = FormulaParser.getSQLFromParsedDefinition(parsedDef, type, null);
		assertEquals("case when kid is null then null else coalesce(" + type.getField("firstName").getDbColumn() + ",'') || ' ' || coalesce(" + type.getField("lastName").getDbColumn() + ",'') end", sqlDef);
		
		// try creating formula for non-existing field
		try
		{
			FormulaParser.parseDefinition(FormulaReturnType.TEXT, "firstName + nonExistingField", type, env);
			fail("Creating formula for non-existing field should have failed");
		}
		catch (FormulaSyntaxException e)
		{
			assertTrue(e.getMessage().startsWith("Unknown field nonExistingField"));
		}
		
		try
		{
			FormulaParser.parseDefinition(FormulaReturnType.TEXT, "firstName + 'some string'", type, env);
			fail("Creating formula that uses single quotes should fail");
		}
		catch (FormulaSyntaxException e)
		{
			assertEquals("Single quotes are not allowed in formula definitions. Use double quotes instead.", e.getMessage());
		}
	}
}
