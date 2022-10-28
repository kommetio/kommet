/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldRemovalException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeRemovalException;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;

public class InverseCollectionTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@SuppressWarnings("unchecked")
	@Test
	public void testInverseCollection() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		
		// add children collection field
		Field field = new Field();
		field.setApiName("children");
		field.setLabel("Children");
		field.setDataType(new InverseCollectionDataType(type, "father"));
		field.setRequired(false);
		type.addField(field);
		
		type = dataService.createType(type, dataHelper.getRootAuthData(env), env);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(type.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		
		Record youngPigeon1 = dataService.instantiate(type.getKID(), env);
		youngPigeon1.setField("name", "Zenek");
		youngPigeon1.setField("age", 2);
		
		Record youngPigeon2 = dataService.instantiate(type.getKID(), env);
		youngPigeon2.setField("name", "Heniek");
		youngPigeon2.setField("age", 2);
		
		dataService.save(oldPigeon, env);
		
		// make sure empty inverse collections are handled properly
		List<Record> pigeonsWithoutChildren = env.getSelectCriteriaFromDAL("select id, children.id from " + type.getQualifiedName()).list();
		assertEquals(1, pigeonsWithoutChildren.size());
		assertTrue(((List<Record>)pigeonsWithoutChildren.get(0).getField("children")).isEmpty());
		
		youngPigeon1.setField("father", oldPigeon);
		youngPigeon2.setField("father", oldPigeon);
		dataService.save(youngPigeon1, env);
		dataService.save(youngPigeon2, env);
		
		Criteria c = env.getSelectCriteria(type.getKID());
		c.addProperty("children.id");
		c.addProperty("children.name");
		c.addProperty("children.age");
		c.addProperty("children.father");
		c.addProperty("id");
		c.addProperty("name");
		c.createAlias("children", "children");
		c.add(Restriction.eq("age", 8));
		List<Record> pigeons = c.list();
		assertNotNull(pigeons);
		assertEquals(1, pigeons.size());
		assertNotNull(pigeons.get(0).getField("children"));
		
		for (Record pigeon : (List<Record>)pigeons.get(0).getField("children"))
		{
			assertEquals(2, pigeon.getField("age"));
			assertEquals(oldPigeon.getKID(), pigeon.getField("father.id"));
		}
		
		try
		{
			// make sure referencing a relationship of an inverse collection item ("children.father") is not allowed,
			// unless by ID ("children.father.id")
			env.getSelectCriteriaFromDAL("select id, name, age, children.age, children.id, children.name, children.father FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE age = 8").list();
			fail("Referencing a relationship field ('children.father') of an inverse collection item should fail");
		}
		catch (Exception e)
		{
			// expected
		}
		
		// now run exactly the same query as above, but using DAL
		pigeons = env.getSelectCriteriaFromDAL("select id, name, age, children.age, children.id, children.name, children.father.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE age = 8").list();
		assertNotNull(pigeons);
		assertEquals(1, pigeons.size());
		
		for (Record pigeon : (List<Record>)pigeons.get(0).getField("children"))
		{
			assertEquals(2, pigeon.getField("age"));
			assertEquals(oldPigeon.getKID(), pigeon.getField("father.id"));
		}
		
		// Now run exactly the same query as above, but put a condition on age such that no record will be returned.
		// This is done to run a check on initialization of empty values on inverse collections, which failed for some
		// cases before it was fixed, and we want to make sure it won't fail again.
		pigeons = env.getSelectCriteriaFromDAL("select id, name, age, children.id, children.name, children.father.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE age = 2").list();
		assertNotNull(pigeons);
		assertEquals(2, pigeons.size());
		
		for (Record pigeon : pigeons)
		{
			assertNotNull(pigeon.getField("children"));
		}
		
		testManualCollectionAssignment(oldPigeon, youngPigeon1);
		testUpdateQuery(env, oldPigeon, youngPigeon1, youngPigeon2);
		testPrintObject(oldPigeon);
		testOrderByQuery(type, env);
		testMultipleCollections(type, env);
		testDeleteTypeReference(type, env);
	}

	@SuppressWarnings("unchecked")
	private void testMultipleCollections(Type pigeonType, EnvData env) throws KommetException
	{
		// create category type
		Type categoryType = new Type();
		categoryType.setApiName("Category");
		categoryType.setPackage("com.test");
		categoryType.setLabel("Category");
		categoryType.setPluralLabel("Categories");
		categoryType.setBasic(false);
		
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setDataType(new TextDataType(100));
		nameField.setLabel("Name");
		nameField.setRequired(true);
		categoryType.addField(nameField);
		
		Field pigeonField = new Field();
		pigeonField.setApiName("pigeon");
		pigeonField.setDataType(new TypeReference(pigeonType));
		pigeonField.setLabel("Pigeon");
		pigeonField.setRequired(true);
		categoryType.addField(pigeonField);
		
		categoryType = dataService.createType(categoryType, env);
		
		Field categoriesField = new Field();
		categoriesField.setApiName("categories");
		
		InverseCollectionDataType categoriesDT = new InverseCollectionDataType(categoryType, "pigeon");
		categoriesField.setDataType(categoriesDT);
		categoriesField.setLabel("Categories");
		categoriesField.setRequired(false);
		pigeonType.addField(categoriesField);
		dataService.createField(categoriesField, env);
		
		Record pigeon1 = dataService.instantiate(pigeonType.getKID(), env);
		pigeon1.setField("name", "P1");
		pigeon1.setField("age", 8);
		pigeon1 = dataService.save(pigeon1, env);
		
		Record pigeon2 = dataService.instantiate(pigeonType.getKID(), env);
		pigeon2.setField("name", "P2");
		pigeon2.setField("age", 8);
		pigeon2.setField("father", pigeon1);
		pigeon2 = dataService.save(pigeon2, env);
		
		Record pigeon3 = dataService.instantiate(pigeonType.getKID(), env);
		pigeon3.setField("name", "P3");
		pigeon3.setField("age", 8);
		pigeon3.setField("father", pigeon1);
		pigeon3 = dataService.save(pigeon3, env);
		
		// add two categories
		for (int i = 0; i < 3; i++)
		{
			Record cat = new Record(categoryType);
			cat.setField("name", "Cat " + i);
			cat.setField("pigeon", pigeon1);
			dataService.save(cat, env);
		}
		
		pigeon1 = env.getSelectCriteriaFromDAL("select id, name, categories.id, children.id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + pigeon1.getKID() + "'").singleRecord();
		assertNotNull(pigeon1);
		assertNotNull(pigeon1.getField("categories"));
		assertEquals(3, ((List<Record>)pigeon1.getField("categories")).size());
		assertEquals(2, ((List<Record>)pigeon1.getField("children")).size());
	}

	private void testDeleteTypeReference(Type type, EnvData env) throws KommetException
	{
		Field fieldToDelete = type.getField("father");
		int fieldCount = type.getFields().size();
		
		try
		{
			dataService.deleteField(fieldToDelete, dataHelper.getRootAuthData(env), env);
			fail("Deleting field referenced by an inverse collection should fail");
		}
		catch (FieldRemovalException e)
		{
			assertEquals("Field " + type.getQualifiedName() + "." + fieldToDelete.getApiName() + " cannot be removed because it is used by an inverse collection " + type.getQualifiedName() + ".children", e.getMessage());
		}
		
		type = env.getType(type.getKeyPrefix());
		
		Type typeFromDatabase = dataService.getType(type.getKID(), env);
		
		// make sure the type has not been deleted and all fields on it still exist
		assertEquals(fieldCount, type.getFields().size());
		assertEquals(fieldCount, typeFromDatabase.getFields().size());
		
		// now try to delete the whole type
		try
		{
			dataService.deleteType(type, dataHelper.getRootAuthData(env), env);
			fail("Deleting field referenced by an inverse collection should fail");
		}
		catch (TypeRemovalException e)
		{
			assertTrue("Incorrect error message: " + e.getMessage(), e.getMessage().startsWith("Type cannot be deleted because it is referenced by field "));
		}
		
		type = env.getType(type.getKeyPrefix());
		typeFromDatabase = dataService.getType(type.getKID(), env);
		
		// make sure the type has not been deleted and all fields on it still exist
		assertEquals(fieldCount, type.getFields().size());
		assertEquals(fieldCount, typeFromDatabase.getFields().size());
		
		// now first delete the inverse collection field
		dataService.deleteField(type.getField("children"), dataHelper.getRootAuthData(env), env);
		dataService.deleteField(type.getField("father"), dataHelper.getRootAuthData(env), env);
	}

	private void testOrderByQuery(Type pigeonType, EnvData env) throws KommetException
	{
		// Make sure that when a column appears in an ORDER BY clause, an aplias is added to it.
		// If it wasn't, this query would fail because both parent and child object have property 'createdDate'
		env.getSelectCriteriaFromDAL("select id, children." + Field.CREATEDDATE_FIELD_NAME + " from " + pigeonType.getQualifiedName() + " ORDER BY " + Field.CREATEDDATE_FIELD_NAME + " DESC").list();
	}

	private void testPrintObject(Record oldPigeon)
	{
		// there were some issues with printing inverse collections, so we want to test it
		// just to make sure it does not end in an inifite loop as it did at some point
		oldPigeon.toString();
	}

	@SuppressWarnings("unchecked")
	private void testUpdateQuery (EnvData env, Record oldPigeon, Record youngPigeon1, Record youngPigeon2) throws KommetException
	{
		// update some random, non-collection field on the pigeon object
		oldPigeon.setField("age", 10);
		dataService.save(oldPigeon, env);
		
		// make sure the collection has not changed on that object
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, children.id FROM " + TestDataCreator.PIGEON_TYPE_PACKAGE + "." + TestDataCreator.PIGEON_TYPE_API_NAME + " WHERE id = '" + oldPigeon.getKID() + "'").list();
		assertNotNull(pigeons);
		assertEquals(1, pigeons.size());
		assertNotNull(pigeons.get(0).getField("children"));
		assertEquals(2, ((List<Record>)pigeons.get(0).getField("children")).size());
		
		// now update some random field on the child pigeon and make sure nothing has changed
		youngPigeon1.setField("age", 1);
		dataService.save(youngPigeon1, env);
		pigeons = env.getSelectCriteriaFromDAL("select id, children.id FROM " + TestDataCreator.PIGEON_TYPE_PACKAGE + "." + TestDataCreator.PIGEON_TYPE_API_NAME + " WHERE id = '" + oldPigeon.getKID() + "'").list();
		assertNotNull(pigeons);
		assertEquals(1, pigeons.size());
		assertNotNull(pigeons.get(0).getField("children"));
		assertEquals(2, ((List<Record>)pigeons.get(0).getField("children")).size());
		List<Record> children = env.getSelectCriteriaFromDAL("select id, children.id FROM " + TestDataCreator.PIGEON_TYPE_PACKAGE + "." + TestDataCreator.PIGEON_TYPE_API_NAME + " WHERE father.id = '" + oldPigeon.getKID() + "'").list();
		assertEquals(2, children.size());
		
		// now set the father field to null on one of the child pigeons and make sure this change
		// is reflected in the inverse collection
		youngPigeon2.setField("father", SpecialValue.NULL);
		dataService.save(youngPigeon2, env);
		children = env.getSelectCriteriaFromDAL("select id, children.id FROM " + TestDataCreator.PIGEON_TYPE_PACKAGE + "." + TestDataCreator.PIGEON_TYPE_API_NAME + " WHERE father.id = '" + oldPigeon.getKID() + "'").list();
		assertEquals(1, children.size());
	}
	
	/*@Test
	public void testAddInverseCollectionField() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		envService.add(env);
		
		Type addressType = dataHelper.getAddressType(env);
		addressType = dataService.createType(addressType, dataHelper.getRootAuthData(env), env);
		
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, dataHelper.getRootAuthData(env), env);
		
		Field addressField = new Field();
		addressField.setApiName("address");
		addressField.setDataType(new TypeReference(addressType));
		addressField.setLabel("Address");
		pigeonType.addField(addressField);
		dataService.createField(addressField, dataHelper.getRootAuthData(env), false, false, env);
		
		// add inverse collection
		Field pigeonsField = new Field();
		pigeonsField.setApiName("pigeons");
		pigeonsField.setLabel("Pigeons");
		pigeonsField.setDataType(new InverseCollectionDataType(env.getType(pigeonType.getKID()), "address"));
		addressType.addField(pigeonsField);
		dataService.createField(pigeonsField, dataHelper.getRootAuthData(env), false, false, env);
		
		Type envAddressType = env.getType(addressType.getKID());
		Field envPigeonsField = envAddressType.getField("pigeons");
		assertNotNull(envPigeonsField);
		assertNotNull(((InverseCollectionDataType)envPigeonsField.getDataType()).getInverseType());
		assertNotNull(((InverseCollectionDataType)envPigeonsField.getDataType()).getInverseTypeId());
		assertNotNull(((InverseCollectionDataType)envPigeonsField.getDataType()).getInverseProperty());
		
		// now reinitialize environment and make sure
		EnvData reinitializedEnv = envService.get(env.getId(), true, true, false, true, true, true, true, true, true, true, true);
		assertNotNull("Failed to read in env with ID " + env.getId(), reinitializedEnv);
		envAddressType = reinitializedEnv.getType(addressType.getKID());
		assertNotNull("Address type not read in during env reinitialization", envAddressType);
		envPigeonsField = envAddressType.getField("pigeons");
		assertNotNull(envPigeonsField);
		assertNotNull(((InverseCollectionDataType)envPigeonsField.getDataType()).getInverseType());
		assertNotNull(((InverseCollectionDataType)envPigeonsField.getDataType()).getInverseTypeId());
		assertNotNull(((InverseCollectionDataType)envPigeonsField.getDataType()).getInverseProperty());
		
		assertNotNull(reinitializedEnv.getSetting(SystemSettingKey.BLANK_LAYOUT_ID));
		assertNotNull(reinitializedEnv.getSetting(SystemSettingKey.IGNORE_NON_EXISTING_FIELD_LABELS));
	}*/

	@SuppressWarnings("unchecked")
	private void testManualCollectionAssignment(Record oldPigeon, Record youngPigeon) throws KommetException
	{
		List<Record> children = new ArrayList<Record>();
		children.add(youngPigeon);
		oldPigeon.setField("children", children);
		assertNotNull(oldPigeon.getField("children"));
		assertEquals(1, ((List<Record>)oldPigeon.getField("children")).size());
		assertEquals(youngPigeon.getKID(), ((List<Record>)oldPigeon.getField("children")).get(0).getKID());
	}
	
	/*@SuppressWarnings("unchecked")
	@Test
	public void testMultipleInverseCollectionsOnType() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		CompanyAppDataSet dataSet = CompanyAppDataSet.getInstance(dataService, env);
		
		// create some address
		Record address1 = dataSet.getTestAddress("Mragowo");
		Record address2 = dataSet.getTestAddress("Piecki");
		dataService.save(address1, env);
		dataService.save(address2, env);
		
		// create some companies
		Record company1 = dataSet.getTestCompany("company-1", address1);
		Record company2 = dataSet.getTestCompany("company-2", address1);
		dataService.save(company1, env);
		dataService.save(company2, env);
		
		// create some employees for company 1
		List<Record> company1Employees = new ArrayList<Record>();
		for (int i = 0; i < 5; i++)
		{
			company1Employees.add(dataSet.getTestEmployee("first name " + i, "last name " + i, null, company1, address1));
			dataService.save(company1Employees.get(i), env);
		}
		
		// now select address with all associated employees and companies
		List<Record> addresses = env.getSelectCriteriaFromDAL("select id, city, companies.name, employees.firstName, employees.lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.ADDRESS_TYPE_API_NAME).list();
		assertEquals(2, addresses.size());
		
		for (Record address : addresses)
		{
			if (address.getKID().equals(address1))
			{
				assertEquals(2, ((List<Record>)address2.getField("companies")).size());
				assertEquals(5, ((List<Record>)address2.getField("employees")).size());
			}
			else if (address.getKID().equals(address2))
			{
				assertEquals(0, ((List<Record>)address2.getField("companies")).size());
				assertEquals(0, ((List<Record>)address2.getField("employees")).size());
			} 
		}
	}*/
}
