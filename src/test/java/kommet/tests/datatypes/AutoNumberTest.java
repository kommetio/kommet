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

import javax.inject.Inject;

import org.junit.Test;

import kommet.dao.FieldDefinitionException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AutoNumber;
import kommet.data.datatypes.TextDataType;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class AutoNumberTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Test
	public void testCreateAssociationFromExistingLinkingType() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		Type type = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		// add autonumber field
		Field numField = new Field();
		numField.setApiName("number");
		numField.setDataType(new AutoNumber("PIG-{000}"));
		numField.setLabel("Number");
		numField.setRequired(false);
		type.addField(numField);
		
		try
		{
			numField = dataService.createField(numField, env);
		}
		catch (KommetException e)
		{
			assertEquals("AutoNumber fields must be required", e.getMessage());
		}
		
		numField.setRequired(true);
		numField = dataService.createField(numField, env);
		assertNotNull(numField.getKID());
		
		// create a record
		Record pigeon1 = new Record(type);
		pigeon1.setField("name", "Kamilka");
		pigeon1.setField("age", 28);
		pigeon1 = dataService.save(pigeon1, env);
		
		pigeon1 = env.getSelectCriteriaFromDAL("select id, number from " + type.getQualifiedName() + " where id = '" + pigeon1.getKID() + "'").singleRecord();
		assertNotNull(pigeon1.getField("number"));
		assertEquals("PIG-001", pigeon1.getField("number"));
		
		Record pigeon2 = new Record(type);
		pigeon2.setField("name", "Kamilka2");
		pigeon2.setField("age", 28);
		pigeon2 = dataService.save(pigeon2, env);
		
		pigeon2 = env.getSelectCriteriaFromDAL("select id, number from " + type.getQualifiedName() + " where id = '" + pigeon2.getKID() + "'").singleRecord();
		assertNotNull(pigeon2.getField("number"));
		assertEquals("PIG-002", pigeon2.getField("number"));
		
		// update record
		pigeon2.setField("name", "Ang");
		pigeon2 = dataService.save(pigeon2, env);
		pigeon2 = env.getSelectCriteriaFromDAL("select id, number from " + type.getQualifiedName() + " where id = '" + pigeon2.getKID() + "'").singleRecord();
		assertNotNull(pigeon2.getField("number"));
		assertEquals("PIG-002", pigeon2.getField("number"));
		
		numField = dataService.getFieldForUpdate(numField.getKID(), env);
		assertTrue(numField.isRequired());
		
		try
		{
			numField.setDataType(new AutoNumber("PIG"));
		}
		catch (FieldDefinitionException e)
		{
			assertEquals("Invalid auto-number format PIG", e.getMessage());
		}
		
		try
		{
			numField.setDataType(new AutoNumber("{0000}"));
		}
		catch (FieldDefinitionException e)
		{
			assertEquals("Invalid auto-number format {0000}", e.getMessage());
		}
		
		numField.setDataType(new AutoNumber("LEE-{0000}"));
		dataService.updateField(numField, dataHelper.getRootAuthData(env), env);
		
		Record pigeon3 = new Record(type);
		pigeon3.setField("name", "Kamilka3");
		pigeon3.setField("age", 28);
		pigeon3 = dataService.save(pigeon3, env);
		
		pigeon3 = env.getSelectCriteriaFromDAL("select id, number from " + type.getQualifiedName() + " where id = '" + pigeon3.getKID() + "'").singleRecord();
		assertNotNull(pigeon3.getField("number"));
		assertEquals("LEE-0003", pigeon3.getField("number"));
		
		// make sure autonumber field data type cannot be changed to anything else
		numField.setDataType(new TextDataType());
		
		try
		{
			dataService.updateField(numField, dataHelper.getRootAuthData(env), env);
		}
		catch (KommetException e)
		{
			assertTrue("Invalid error message: " + e.getMessage(), e.getMessage().startsWith("Cannot change data type of field "));
		}
		
		type = env.getType(type.getKeyPrefix());
		
		numField.setDataType(new AutoNumber("LEE-{0}"));
		dataService.updateField(numField, dataHelper.getRootAuthData(env), env);
		
		for (int i = 0; i < 15; i++)
		{
			Record pigeonX = new Record(type);
			pigeonX.setField("name", "Kamilka3");
			pigeonX.setField("age", 28);
			pigeonX = dataService.save(pigeonX, env);
			
			// test saving pigeons whose sequence exceeds the number of 0's in the autonumber format
			pigeonX = env.getSelectCriteriaFromDAL("select id, number from " + type.getQualifiedName() + " where id = '" + pigeonX.getKID() + "'").singleRecord();
			assertNotNull(pigeonX.getField("number"));
			assertEquals("LEE-" + (i + 4), pigeonX.getField("number"));
		}
		
		// set autoformat field as default
		type.setDefaultFieldId(numField.getKID());
		dataService.updateType(type, dataHelper.getRootAuthData(env), env);
		
		// try to create another autonumber field
		Field numField2 = new Field();
		numField2.setApiName("numberTwo");
		numField2.setDataType(new AutoNumber("PIG-{000}"));
		numField2.setLabel("Number");
		numField2.setRequired(true);
		type.addField(numField2);
		
		try
		{
			numField2 = dataService.createField(numField2, env);
		}
		catch (KommetException e)
		{
			assertEquals("Cannot create autonumber field on type because this type already contains an autonumber field 'number'", e.getMessage());
		}
		
	}
}
