/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.datatypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.SystemException;

import org.junit.Test;

import kommet.dao.FieldDefinitionException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class MultienumerationTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppConfig config;
	
	private void testTypeErrorRollback (Type type, EnvData env) throws KommetException, SystemException
	{
		assertNull(type.getKID());
		assertNull(type.getId());
		assertNull(type.getCreated());
		assertNull(type.getDbTable());
		assertNull(type.getKeyPrefix());
		
		type = dataService.createType(type, env);
		fail("It should not be possible to save a multi-enum field with null value list");
	}

	@Test
	public void testMultiEnumFields() throws KommetException, SystemException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		// add multi-enum field
		Field labelsField = new Field();
		labelsField.setApiName("labels");
		labelsField.setLabel("Labels");
		labelsField.setDataType(new MultiEnumerationDataType());
		labelsField.setRequired(false);
		pigeonType.addField(labelsField);
		
		try
		{
			testTypeErrorRollback(pigeonType, env);
			fail("It should not be possible to save a multi-enum field with null value list");
		}
		catch (FieldDefinitionException e)
		{
			assertTrue(e.getMessage().startsWith("Multi-enumeration field"));
		}
		
		// make sure the failed save operation had no effect on the type
		assertNull(pigeonType.getKID());
		assertNull(pigeonType.getId());
		assertNull(pigeonType.getCreated());
		assertNull(pigeonType.getDbTable());
		assertNull(pigeonType.getKeyPrefix());
		
		pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType.setApiName("ChangedPigeon");
		labelsField = new Field();
		labelsField.setApiName("labels");
		labelsField.setLabel("Labels");
		labelsField.setDataType(new MultiEnumerationDataType(MiscUtils.toSet("one")));
		labelsField.setRequired(false);
		pigeonType.addField(labelsField);
		pigeonType.setDbTable(null);
		pigeonType = dataService.createType(pigeonType, env);
		assertNotNull(pigeonType.getId());
		
		// now create a pigeon
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Black Adder");
		pigeon1.setField("age", BigDecimal.valueOf(4));
		
		Set<String> labels = new HashSet<String>();
		labels.add("Animal");
		labels.add("Bird");
		labels.add("Pet");
		
		pigeon1.setField("labels", labels);
		pigeon1 = dataService.save(pigeon1, env);
		assertNotNull(pigeon1.attemptGetKID());
		
		// query pigeon
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, name, labels from " + TestDataCreator.PIGEON_TYPE_PACKAGE + ".ChangedPigeon" + " WHERE id = '" + pigeon1.getKID() + "'").list();
		assertEquals(1, pigeons.size());
		
		Record fetchedPigeon = pigeons.get(0);
		Object labelsVal = fetchedPigeon.getField("labels");
		assertNotNull(labelsVal);
		assertTrue("Invalid type of multi-enum field: " + fetchedPigeon.getField("labels").getClass().getName(), fetchedPigeon.getField("labels") instanceof Set);
		assertEquals(3, ((Set<?>)labelsVal).size());
		assertTrue(((Set<?>)labelsVal).contains("Animal"));
		assertTrue(((Set<?>)labelsVal).contains("Bird"));
		assertTrue(((Set<?>)labelsVal).contains("Pet"));
		
		labels.remove("Animal");
		labels.add("Toy");
		labels.add("Car");
		pigeon1.setField("labels", labels);
		pigeon1 = dataService.save(pigeon1, env);
		pigeons = env.getSelectCriteriaFromDAL("select id, name, labels from " + TestDataCreator.PIGEON_TYPE_PACKAGE + ".ChangedPigeon" + " WHERE id = '" + pigeon1.getKID() + "'").list();
		assertEquals(1, pigeons.size());
		labelsVal = pigeons.get(0).getField("labels");
		assertEquals(4, ((Set<?>)labelsVal).size());
		assertTrue(((Set<?>)labelsVal).contains("Toy"));
		assertTrue(((Set<?>)labelsVal).contains("Bird"));
		assertTrue(((Set<?>)labelsVal).contains("Pet"));
		assertTrue(((Set<?>)labelsVal).contains("Car"));
		assertFalse(((Set<?>)labelsVal).contains("Animal"));
	}
}
