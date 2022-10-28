/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.js;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.PIR;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.js.jsrc.JSRC;
import kommet.js.jsrc.JSRUtil;
import kommet.js.jsti.JSTI;
import kommet.js.jsti.JSTIField;
import kommet.js.jsti.JSTIType;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class JSTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Test
	public void runTests() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		// add children relationship
		dataHelper.addChildrenRelationship(pigeonType, env);
		
		Field childrenField = new Field();
		childrenField.setApiName("children");
		childrenField.setLabel("Children");
		childrenField.setRequired(false);
		childrenField.setDataType(new InverseCollectionDataType(pigeonType, "father"));
		pigeonType.addField(childrenField);
		dataService.createField(childrenField, env);
		
		testJSTI(pigeonType, dataHelper.getRootAuthData(env), env);
		testJSRC(pigeonType, dataHelper.getRootAuthData(env), env);
		testJSRUtil();
	}

	private void testJSRUtil()
	{
		LinkedHashMap<String, Object> record = new LinkedHashMap<String, Object>();
		LinkedHashMap<String, Object> subrecord1 = new LinkedHashMap<String, Object>();
		LinkedHashMap<String, Object> subrecord2 = new LinkedHashMap<String, Object>();
		subrecord2.put("0030000000001", "Some Value 1");
		subrecord1.put("0030000000002", "Some Value 2");
		subrecord1.put("0030000000003", subrecord2);
		record.put("0030000000004", subrecord1);
		assertEquals("Some Value 1", JSRUtil.getFieldValue(record, new PIR("0030000000004.0030000000003.0030000000001")));
	}

	private void testJSRC(Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		// create some pigeons
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "A");
		pigeon1.setField("age", BigDecimal.valueOf(4));
		dataService.save(pigeon1, env);
		
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "B");
		pigeon2.setField("age", BigDecimal.valueOf(5));
		pigeon2.setField("father", pigeon1);
		pigeon2 = dataService.save(pigeon2, env);
		
		List<Record> records = env.getSelectCriteriaFromDAL("select id, name, age, children.id, father.id, father.name from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		assertEquals(2, records.size());
		JSRC jsrc = JSRC.build(records, pigeonType, 2, env, authData);
		assertEquals(records.size(), jsrc.getRecords().size());
		assertNotNull(jsrc.getJsti());
		
		JSTI jsti = jsrc.getJsti();
		
		assertNotNull(jsti.getTypes().get(pigeonType.getKID()));
		
		// make sure all fields have been added to the JSTI
		for (Field field : pigeonType.getFields())
		{
			assertFalse(jsti.getFields().isEmpty());
			assertNull(jsti.getFields().get(field.getKID()));
			JSTIField jstiField = jsti.getFields().get(new PIR(field.getKID().getId()));
			assertNotNull(jstiField);
			assertEquals(jstiField.getId(), field.getKID());
		}
		
		// serialize JSRC to JSON
		String json = JSRC.serialize(jsrc, dataHelper.getRootAuthData(env));
		assertNotNull(json);
		
		JSRC deserializedJSRC = JSRC.deserialize(json);
		assertNotNull(deserializedJSRC.getJsti());
		assertNotNull(deserializedJSRC.getRecords());
		
		jsti = deserializedJSRC.getJsti();
		
		// make sure all fields have been added to the JSTI during deserialization
		for (Field field : pigeonType.getFields())
		{
			assertFalse(jsti.getFields().isEmpty());
			assertNull(jsti.getFields().get(field.getKID()));
			JSTIField jstiField = jsti.getFields().get(new PIR(field.getKID().getId()));
			assertNotNull(jstiField);
			assertEquals(jstiField.getId(), field.getKID());
		}
		
		assertEquals(records.size(), deserializedJSRC.getRecords().size());
		
		// make sure records have been fully restored during deserialization
		int i = 0;
		for (Record r : records)
		{
			LinkedHashMap<String, Object> deserializedRec = jsrc.getRecords().get(i);
			assertEquals(deserializedRec.get(new PIR(pigeonType.getField(Field.ID_FIELD_NAME).getKID().getId()).getValue()), r.getKID());
			
			for (String fieldName : r.getFieldValues().keySet())
			{
				// compare all property values
				DataType dt = pigeonType.getField(fieldName).getDataType();
				
				// TODO also test deserialization of non-primitive data types
				if (dt.isPrimitive())
				{
					assertEquals(r.getField(fieldName), deserializedRec.get(PIR.get(fieldName, pigeonType, env).getValue()));
				}
			}
			
			i++;
		}
		
		// test serializing properties that contain special characters such as tab
		pigeon2.setField("name", "string with	tab\nnewline");
		List<Record> pigeons = new ArrayList<Record>();
		pigeons.add(pigeon2);
		jsrc = JSRC.build(pigeons, pigeonType, 2, env, authData);
		// serialize JSRC to JSON
		json = JSRC.serialize(jsrc, dataHelper.getRootAuthData(env));
		assertNotNull(json);
		assertTrue("Special characters in JSON not escaped: " + json, json.contains("\"string with\\ttab\\nnewline\""));
	}

	/**
	 * Test JSTI serialization and operations.
	 * @param pigeonType
	 * @param env
	 * @throws KommetException
	 */
	private void testJSTI(Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		JSTI jsti = new JSTI();
		jsti.addType(pigeonType, env, true, false, authData);
		
		assertNotNull(jsti.getPirs());
		assertEquals(jsti.getPirs().get(TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + ".name"), pigeonType.getField("name").getKID().getId());
		
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
	
		Field userNameField = userType.getField("userName");
		jsti.addField(userNameField, userType, false, dataHelper.getRootAuthData(env), env);
		
		assertEquals((Integer)DataType.INVERSE_COLLECTION, pigeonType.getField("children").getDataTypeId());
		
		// make sure all fields have been added to the JSTI
		for (Field field : pigeonType.getFields())
		{
			assertFalse(jsti.getFields().isEmpty());
			assertNull(jsti.getFields().get(field.getKID()));
			JSTIField jstiField = jsti.getFields().get(new PIR(field.getKID().getId()));
			assertNotNull(jstiField);
			assertEquals(jstiField.getId(), field.getKID());
			assertEquals(jstiField.getTypeId(), pigeonType.getKID().getId());
		}
		
		// now serialize the JSTI
		String json = JSTI.serialize(jsti);
		
		//System.out.println(json);
		
		assertTrue(json.contains("\"id\":\"" + pigeonType.getKID().getId() + "\""));
		assertTrue(json.contains("\"id\":\"" + pigeonType.getField("name").getKID().getId() + "\""));
		assertTrue(json.contains("\"apiName\":\"age\""));
		assertTrue(json.contains("\"apiName\":\"name\""));
		assertTrue(json.contains("\"apiName\":\"father\""));
		assertTrue(json.contains("\"apiName\":\"mother\""));
		assertTrue(json.contains("\"apiName\":\"children\""));
		assertTrue(json.contains("\"label\":\"Age\""));
		assertTrue(json.contains("\"label\":\"Mother\""));
		assertTrue(json.contains("\"label\":\"Name\""));
		assertTrue(json.contains("\"label\":\"Children\""));
		assertTrue("Invalid JSON: " + json, json.contains("\"typeId\":\"" + pigeonType.getKID() + "\""));
		assertTrue(json.contains("\"id\":\"" + pigeonType.getField("children").getKID().getId() + "\""));
		
		// now deserialize JSON back to JSTI object
		JSTI deserializedJSTI = JSTI.deserialize(json);
		assertNotNull(deserializedJSTI);
		assertEquals(jsti.getFields().size(), deserializedJSTI.getFields().size());
		assertEquals(jsti.getTypes().size(), deserializedJSTI.getTypes().size());
		assertEquals(jsti.getPirs().size(), deserializedJSTI.getPirs().size());
		
		for (JSTIType type : jsti.getTypes().values())
		{
			assertNotNull(type.getIdFieldId());
		}
		
		boolean childrenFieldFound = false;
		
		// make sure all fields exist in the deserialized JSTI
		for (Field field : pigeonType.getFields())
		{
			assertFalse(deserializedJSTI.getFields().isEmpty());
			assertNull(deserializedJSTI.getFields().get(field.getKID()));
			JSTIField jstiField = deserializedJSTI.getFields().get(new PIR(field.getKID().getId()));
			assertNotNull(jstiField);
			assertEquals(jstiField.getId(), field.getKID());
			
			if (jstiField.getApiName().equals("children"))
			{
				childrenFieldFound = true;
				assertEquals((Integer)DataType.INVERSE_COLLECTION, jstiField.getDataType().getId());
				assertNotNull(jstiField.getDataType().getInverseFieldId());
				assertNotNull(jstiField.getDataType().getInverseTypeId());
				assertEquals(pigeonType.getKID(), jstiField.getDataType().getInverseTypeId());
				assertEquals(pigeonType.getField("father").getKID(), jstiField.getDataType().getInverseFieldId());
			}
			else if (jstiField.getApiName().equals("father"))
			{
				childrenFieldFound = true;
				assertEquals((Integer)DataType.TYPE_REFERENCE, jstiField.getDataType().getId());
				assertNotNull(jstiField.getDataType().getTypeId());
			}
			
			// TODO add unit test for deserializing association field in JSTI just like
			// we verified inverse collection above
		}
		
		assertTrue(childrenFieldFound);
		
		// now test getting JSTI with initNestedTypes = true
		jsti = new JSTI();
		jsti.addType(pigeonType, env, true, true, authData);
	}
}
