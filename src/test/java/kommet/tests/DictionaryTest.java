/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.Dictionary;
import kommet.basic.DictionaryItem;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.EnumerationDataType;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.services.DictionaryService;
import kommet.utils.AppConfig;

public class DictionaryTest extends BaseUnitTest
{
	@Inject
	AppConfig config;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DictionaryService dictionaryService;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Test
	public void testEnumFieldsWithDictionaries() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Dictionary cities = new Dictionary();
		cities.setName("crm.Cities");
		
		cities = dictionaryService.save(cities, authData, env);
		assertNotNull(cities.getId());
		
		DictionaryItem item1 = new DictionaryItem();
		item1.setName("Cracow");
		item1.setIndex(2);
		cities.addItem(item1);
		
		cities = dictionaryService.save(cities, authData, env);
		
		cities = dictionaryService.get(cities.getId(), authData, env);
		assertEquals(1, cities.getItems().size());
		
		assertNotNull(env.getDictionaries().get(cities.getId()));
		
		DictionaryItem item2 = new DictionaryItem();
		item2.setName("Warsaw");
		item2.setKey("key.warsaw");
		item2.setIndex(1);
		cities.addItem(item2);
		
		cities = dictionaryService.save(cities, authData, env);
		
		Dictionary citiesOnEnv = env.getDictionaries().get(cities.getId());
		assertEquals(2, citiesOnEnv.getItems().size());
		assertEquals("Warsaw", citiesOnEnv.getItems().get(0).getName());
		
		// create pigeon type
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		// create an enum field that references this dictionary
		Field citiesField = new Field();
		citiesField.setApiName("city");
		citiesField.setLabel("City");
		
		EnumerationDataType dt = new EnumerationDataType(cities);
		dt.setValidateValues(true);
		dt.setDictionary(citiesOnEnv);
		
		citiesField.setDataType(dt);
		citiesField.setRequired(false);
		pigeonType.addField(citiesField);
		
		citiesField = dataService.createField(citiesField, env);
		assertNotNull(citiesField.getKID());
		
		// reset env
		envService.resetEnv(env.getId());
		env = envService.get(env.getId());
		
		citiesOnEnv = env.getDictionaries().get(cities.getId());
		assertEquals(2, citiesOnEnv.getItems().size());
		assertEquals("Warsaw", citiesOnEnv.getItems().get(0).getName());
		
		pigeonType = env.getType(pigeonType.getKID());
		assertNotNull(pigeonType.getField("city"));
		
		// create a pigeon
		Record pigeon = new Record(pigeonType);
		pigeon.setField("name", "Drew");
		pigeon.setField("age", 2);
		pigeon.setField("city", "Poznan");
		
		try
		{
			dataService.save(pigeon, env);
			fail("Saving invalid dictionary value should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue("Invalid error message " + e.getMessage(), e.getMessage().contains("Enumeration value 'Poznan' not found in"));
		}
		
		citiesField = dataService.getFieldForUpdate(citiesField.getKID(), env);
		((EnumerationDataType)citiesField.getDataType()).setValidateValues(false);
		dataService.updateField(citiesField, authData, env);
		
		pigeonType = env.getType(pigeonType.getKID());
		pigeon = new Record(pigeonType);
		pigeon.setField("name", "Drew");
		pigeon.setField("age", 2);
		pigeon.setField("city", "Poznan");
		dataService.save(pigeon, env);
		
		((EnumerationDataType)citiesField.getDataType()).setValidateValues(true);
		dataService.updateField(citiesField, authData, env);
		
		try
		{
			dataService.save(pigeon, env);
			fail("Saving invalid dictionary value should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue("Invalid error message " + e.getMessage(), e.getMessage().contains("Enumeration value 'Poznan' not found in"));
		}
		
		Field fetchedCitiesField = dataService.getField(citiesField.getKID(), env);
		assertEquals(cities.getId(), ((EnumerationDataType)fetchedCitiesField.getDataType()).getDictionary().getId());
		
		// make sure dictionary referenced by a field cannot be removed
		try
		{
			dictionaryService.delete(cities.getId(), authData, env);
			fail("Deleting dictionary referenced by a field should fail");
		}
		catch (KommetException e)
		{
			assertEquals("Dictionary cannot be deleted because it is referenced by enumeration field(s): " + env.getType(pigeonType.getKID()).getQualifiedName() + ".city", e.getMessage());
		}
		
		assertNotNull(env.getDictionaries().get(cities.getId()));
		
		// delete the field and then the dictionary
		dataService.deleteField(citiesField, authData, env);
		dictionaryService.delete(cities.getId(), authData, env);
		assertNull(env.getDictionaries().get(cities.getId()));
	}
}
