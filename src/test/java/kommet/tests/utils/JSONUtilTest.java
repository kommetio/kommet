/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.Notification;
import kommet.basic.RecordProxyUtil;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.basic.types.SystemTypes;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.env.EnvData;
import kommet.env.GenericAction;
import kommet.json.JSON;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.UserGroupService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.MiscUtils;

public class JSONUtilTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	UserGroupService userGroupService;
	
	@Test
	public void testStringEscape() throws KommetException
	{
		assertEquals("\\\\aa\\\\", JSON.escape("\\aa\\"));
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testJSONSerialization() throws KommetException, JsonParseException, JsonMappingException, IOException
	{	
		EnvData env = dataHelper.configureFullTestEnv();
		
		Notification n = new Notification();
		n.setText("abc efd");
		n.setTitle("this is a notification");
		
		User u = new User();
		u.setId(KID.get("0040000000111"));
		u.setUserName("radek");
		n.setAssignee(u);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		String json = JSON.serializeObjectProxy(n, MiscUtils.toSet("text", "title", "createdDate", "assignee.id", "assignee.userName"), authData);
		
		assertFalse(json.contains("\"assignee.userName\": \"radek\""));
		assertFalse(json.contains("\"createdDate\": null"));
		assertTrue("JSON does not contain property \"assignee\":\n" + json , json.contains("\"assignee\": { \""));
		assertTrue(json.contains("\"userName\": \"radek\""));
		assertTrue(json.startsWith("{"));
		assertTrue(json.endsWith("}"));
		
		// make sure null values are properly serialized
		n.setAssignee(null);
		n.nullify("assignee");
		json = JSON.serializeObjectProxy(n, MiscUtils.toSet("text", "title", "createdDate", "assignee.id", "assignee.userName"), authData);
		assertTrue("Property assignee not serialized properly: " + json, json.contains("\"assignee\": null"));
		
		// restore the previous assignee value for other tests
		n.setAssignee(u);
		
		// now serialize all initialized properties
		json = JSON.serializeObjectProxy(n, authData);
		
		assertFalse(json.contains("\"assignee.userName\": \"radek\""));
		assertFalse("Uninitialized properties should not be serialized", json.contains("\"createdDate\""));
		assertTrue("Initialized record property should be serialized", json.contains("\"assignee\""));
		assertTrue(json.contains("\"text\": \"abc efd\""));
		assertTrue(json.contains("\"userName\": \"radek\""));
		assertTrue(json.startsWith("{"));
		assertTrue(json.endsWith("}"));
		
		// now serialize a collection of proxies
		List<Notification> notifications = new ArrayList<Notification>();
		notifications.add(n);
		json = JSON.serialize(notifications, authData);
		assertTrue(json.startsWith("["));
		assertTrue(json.endsWith("]"));
		assertFalse(json.contains("\"assignee.userName\": \"radek\""));
		assertFalse("Uninitialized properties should not be serialized", json.contains("\"createdDate\""));
		assertTrue("Initialized record property should be serialized", json.contains("\"assignee\""));
		assertTrue(json.contains("\"text\": \"abc efd\""));
		assertTrue(json.contains("\"userName\": \"radek\""));
		
		// now try to serialize a list of non-object proxies and make sure it fails
		// TODO this should be removed when non-proxy serialization is implemented
		GenericAction action = new GenericAction();
		action.setActionMethod("method1");
		json = JSON.serialize(Arrays.asList(action), authData);
		assertTrue("Invalid JSON: " + json, json.contains("\"actionMethod\":\"method1\""));
		
		json = JSON.serialize(notifications, authData);
		
		Record profile = dataService.save(dataHelper.getTestProfile("SomeProfile", env), env);
		Record user = dataService.save(dataHelper.getTestUser("user@kommet.io", "user@kommet.io", profile, env), env);
		
		// try serializing record to JSON
		String recordJSON = JSON.serialize(user, authData);
		assertTrue(recordJSON.startsWith("{"));
		assertTrue(recordJSON.endsWith("}"));
		assertTrue(recordJSON.contains("\"userName\": \"user@kommet.io\""));
		assertTrue(recordJSON.contains("\"profile\": {"));
		assertTrue(recordJSON.contains("\"name\": \"SomeProfile\""));
		assertTrue(recordJSON.contains("\"createdDate\": \""));
		
		// check serialization of null values
		user.setField("userName", null);
		recordJSON = JSON.serialize(user, authData);
		assertTrue(recordJSON.contains("\"userName\": null"));
		
		// restore previous value for other tests
		user.setField("userName", "user@kommet.io");
		
		// test deserialization
		json = JSON.serializeObjectProxy(RecordProxyUtil.generateStandardTypeProxy(user, env, compiler), MiscUtils.toSet("userName", "id", "createdDate", "profile", "profile.id", "profile.name"), authData);
		
		Record deserializedUser = JSON.toRecord(json, false, env.getType(KeyPrefix.get(KID.USER_PREFIX)), env);
		assertNotNull(deserializedUser);
		assertEquals(user.getKID(), deserializedUser.getKID());
		
		Record deserializedProfile = (Record)deserializedUser.getField("profile");
		assertNotNull(deserializedProfile);
		assertEquals(profile.getKID(), deserializedProfile.getKID());
		assertEquals(user.getField("userName"), deserializedUser.getField("userName"));
		assertEquals(profile.getField("name"), deserializedProfile.getField("name"));
		assertNull(deserializedProfile.attemptGetField("createdDate"));
		
		// now test getting JSON from records that contain collections
		UserGroup group = new UserGroup();
		group.setName("SomeGroup");
		userGroupService.save(group, authData, env);
		
		userGroupService.assignUserToGroup(user.getKID(), group.getId(), authData, env);
		
		List<Record> refetechedGroups = env.getSelectCriteriaFromDAL("select id, name, users.id, users.userName from " + SystemTypes.USER_GROUP_API_NAME + " where id = '" + group.getId() + "'").list();
		assertEquals(1, refetechedGroups.size());
		
		List<UserGroup> groups = new ArrayList<UserGroup>();
		for (Record r : refetechedGroups)
		{
			groups.add((UserGroup)RecordProxyUtil.generateStandardTypeProxy(r, env, compiler));
		}
		
		// get JSON
		String groupJSON = JSON.serializeObjectProxies(groups, MiscUtils.toSet("id", "name", "users.id", "users.userName"), authData);
		assertTrue(groupJSON.contains("\"users\": ["));
		assertTrue(groupJSON.contains("\"userName\": \"" + user.getFieldStringValue("userName", authData.getLocale()) + "\""));
		
		List<Record> groupRecords = env.getSelectCriteriaFromDAL("select id, name, users.id, users.userName from UserGroup").list();
		
		// now get JSON of group records, not proxies
		groupJSON = JSON.serialize(groupRecords, authData);
		assertTrue("Invalid JSON:\n" + groupJSON, groupJSON.contains("\"users\": ["));
		assertTrue(groupJSON.contains("\"userName\": \"" + user.getFieldStringValue("userName", authData.getLocale()) + "\""));
		
		// test serializing LinkedHashMap
		LinkedHashMap<String, Object> sampleRecord = new LinkedHashMap<String, Object>();
		sampleRecord.put("id", KID.get("004000000043a"));
		sampleRecord.put("name", "John");
		sampleRecord.put("age", BigDecimal.valueOf(33));
		sampleRecord.put("isCustomer", true);
		sampleRecord.put("parent", null);
		sampleRecord.put("birthdate", new Date(100, 1, 1));
		
		String parsedRecord = JSON.serialize(sampleRecord, authData);
		assertNotNull(parsedRecord);
		assertTrue("Incorrect JSON: " + parsedRecord, parsedRecord.contains("\"name\": \"John\""));
		assertTrue("Incorrect JSON: " + parsedRecord, parsedRecord.contains("\"id\": \"004000000043a\""));
		assertTrue("Incorrect JSON: " + parsedRecord, parsedRecord.contains("\"age\": 33"));
		assertTrue("Incorrect JSON: " + parsedRecord, parsedRecord.contains("\"isCustomer\": true"));
		assertTrue("Incorrect JSON: " + parsedRecord, parsedRecord.contains("\"birthdate\": \"2000-02-01 0"));
		
		// check serialization of null values
		sampleRecord.put("birthdate", null);
		parsedRecord = JSON.serialize(sampleRecord, authData);
		assertTrue("Incorrect JSON: " + parsedRecord, parsedRecord.contains("\"birthdate\": null"));
		
		// check parsing of tab values in object proxy field values
		User userWithTabs = new User();
		userWithTabs.setUserName("tab	value\nnew line");
		String userJSON = JSON.serialize(userWithTabs, authData);
		assertTrue(userJSON.contains("\"userName\": \"tab\\tvalue\\nnew line\""));
		
		// test serializing array of types
		List<Type> types = new ArrayList<Type>();
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		types.add(userType);
		types.add(env.getType(KeyPrefix.get(KID.PROFILE_PREFIX)));
		
		String serializedTypes = JSON.serialize(types, authData);
		assertNotNull(serializedTypes);
		assertTrue(serializedTypes.startsWith("["));
		assertTrue(serializedTypes.endsWith("]"));
		assertTrue(serializedTypes.contains("\"label\": \"User\""));
		assertTrue(serializedTypes.contains("\"label\": \"Profile\""));
		assertTrue(serializedTypes.contains("\"qualifiedName\": \"" + userType.getQualifiedName() + "\""));
		assertTrue(serializedTypes.contains("\"id\": \"" + userType.getKID().getId() + "\""));
	}
	
	@Test
	public void testSerializeCollection() throws KommetException
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
		oldPigeon = dataService.save(oldPigeon, env);
		
		Record youngPigeon1 = dataService.instantiate(type.getKID(), env);
		youngPigeon1.setField("name", "Zenek");
		youngPigeon1.setField("age", 2);
		dataService.save(youngPigeon1, env);
		
		Record youngPigeon2 = dataService.instantiate(type.getKID(), env);
		youngPigeon2.setField("name", "Heniek");
		youngPigeon2.setField("age", 2);
		youngPigeon2.setField("father", oldPigeon);
		
		dataService.save(youngPigeon2, env);
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, children.id, children.name, children.father.name from " + type.getQualifiedName()).list();
		
		for (Record pigeon : pigeons)
		{
			JSON.serializeRecord(pigeon, dataHelper.getRootAuthData(env));
		}
		
		// now query the same collection, but do not query the "children.id" field
		for (Record pigeon : env.getSelectCriteriaFromDAL("select id, children.name, children.father.name from " + type.getQualifiedName()).list())
		{
			JSON.serializeRecord(pigeon, dataHelper.getRootAuthData(env));
		}
	}
}