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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.Action;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.Profile;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyException;
import kommet.basic.RecordProxyUtil;
import kommet.basic.User;
import kommet.basic.UserSettings;
import kommet.basic.types.SystemTypes;
import kommet.dao.dal.InsufficientPrivilegesException;
import kommet.data.DataService;
import kommet.data.EnvSpecificTypeException;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.NullifiedRecord;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetCompiler;

public class RecordProxyTest extends BaseUnitTest
{
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UserService userService;
	
	@Test
	public void testGenerateRecordFromProxy() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Profile profile = new Profile();
		assertFalse(profile.isSet("name"));
		assertFalse(profile.isSet("id"));
		assertFalse(profile.isSet("systemProfile"));
		assertFalse(profile.isSet(Field.CREATEDDATE_FIELD_NAME));
		profile.setCreatedDate(new Date());
		assertTrue(profile.isSet(Field.CREATEDDATE_FIELD_NAME));
		profile.setName("test");
		profile.setSystemProfile(true);
		assertTrue(profile.isSet("systemProfile"));
		
		Record profileRec = RecordProxyUtil.generateRecord(profile, env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.PROFILE_API_NAME)), 1, env);
		assertNotNull(profileRec);
		assertEquals(profile.getName(), profileRec.getField("name"));
		assertNull(profileRec.attemptGetField("id"));
		assertEquals(profile.getCreatedDate(), profileRec.getField("createdDate"));
		assertEquals(profile.getSystemProfile(), profileRec.getField("systemProfile"));
		
		// make sure that a field that is not initialized on a proxy will also not be initialized
		// on the generated record
		Profile profile2 = new Profile();
		profile2.setName("test");
		assertFalse(profile2.isSet("systemProfile"));
		Record profileRec2 = RecordProxyUtil.generateRecord(profile2, env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.PROFILE_API_NAME)), 1, env);
		assertFalse("Field that was not initialized on proxy should also be not initialized on generated record", profileRec2.isSet("systemProfile"));
		
		User testUser = new User();
		testUser.setUserName("Mikey");
		
		Record testUserRec = RecordProxyUtil.generateRecord(testUser, env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_API_NAME)), 3, env);
		assertEquals("Mikey", testUserRec.getField("userName"));
		assertFalse(testUserRec.isSet("profile"));
		
		// now set profile on the user
		testUser.setProfile(profile2);
		testUserRec = RecordProxyUtil.generateRecord(testUser, env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_API_NAME)), 3, env);
		assertEquals("Mikey", testUserRec.getField("userName"));
		assertTrue(testUserRec.isSet("profile"));
		profileRec2 = (Record)testUserRec.getField("profile"); 
		assertTrue(profileRec2.isSet("name"));
		assertFalse(profileRec2.isSet("systemProfile"));
	}

	@Test
	public void testGenerateProxyFromRecord() throws KommetException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		// make sure that field that are not initialized on the record
		// will also not be initialized on a proxy generated from that record
		Record profile1 = new Record(env.getType(KeyPrefix.get(KID.PROFILE_PREFIX)));
		profile1.setField("name", "Profile One");
		assertTrue(profile1.isSet("name"));
		assertFalse(profile1.isSet("systemProfile"));
		Profile profileProxy1 = (Profile)RecordProxyUtil.generateStandardTypeProxy(Profile.class, profile1, true, env);
		assertTrue(profileProxy1.isSet("name"));
		assertFalse(profileProxy1.isSet("systemProfile"));
		
		Record newProfile = dataHelper.getTestProfile("NewProfile", env);
		Profile profile = (Profile)RecordProxyUtil.generateStandardTypeProxy(Profile.class, newProfile, true, env);
		assertNotNull(profile);
		assertTrue(profile.isSet("name"));
		assertEquals(newProfile.getField("name"), profile.getName());
		
		Type pigeonType = testTwoWayConversion(env);
		testSavingProxy(pigeonType, env);
	}

	private void testSavingProxy(Type pigeonType, EnvData env) throws KommetException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		java.lang.Class<?> proxyClass = compiler.getClass(pigeonType.getQualifiedName(), true, env);
		assertNotNull(proxyClass);
		
		// instantiate proxy
		RecordProxy proxy = (RecordProxy)proxyClass.newInstance();
	
		Method nameSetter = proxyClass.getMethod("setName", String.class);
		nameSetter.invoke(proxy, "Laila");
		
		Method ageSetter = proxyClass.getMethod("setAge", Integer.class);
		ageSetter.invoke(proxy, 3);
		
		// save pigeon proxy
		proxy = dataService.save(proxy, dataHelper.getRootAuthData(env), env);
		assertNotNull(proxy.getId());
		
		testSavingStandardProxy(env);
	}

	/**
	 * Standard proxies are handled a bit different by the save method. There was once a problem with saving them
	 * due to their package name not being env-specific, so we are testing it here.
	 * @param env
	 * @throws KommetException
	 */
	private void testSavingStandardProxy(EnvData env) throws KommetException
	{
		Profile rootProfile = profileService.getProfileByName(Profile.ROOT_NAME, env);
		assertNotNull(rootProfile);
		
		try
		{
			dataService.save(rootProfile, dataHelper.getRootAuthData(env), env);
			fail("Saving standard proxy using DataService.save method should fail. Only custom proxies can be saved in this way.");
		}
		catch (EnvSpecificTypeException e)
		{
			assertTrue(e.getMessage().startsWith("Qualified type name "));
		}
		
		try
		{
			profileService.save(rootProfile, dataHelper.getRootAuthData(env), env);
			fail("Saving system immutable record should fail");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertEquals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_SYSTEM_IMMUTABLE_RECORD_MSG, e.getMessage());
		}
		
		// try creating the profile object manually, because when created manually, standard proxies
		// have non-env-specific package name, which caused an error at some time
		Profile newProfile = new Profile();
		newProfile.setName("NewProfile");
		newProfile.setLabel("New Profile");
		newProfile.setSystemProfile(false);
		profileService.save(newProfile, dataHelper.getRootAuthData(env), env);
		assertNotNull(newProfile.getId());
		
		UserSettings settings = new UserSettings();
		settings.setProfile(newProfile);
		userService.save(settings, dataHelper.getRootAuthData(env), env);
	}

	@SuppressWarnings("unchecked")
	private Type testTwoWayConversion(EnvData env) throws KommetException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		Record oldPigeon = new Record(pigeonType);
		Record fatherPigeon = new Record(pigeonType);
		
		// set name field to null
		oldPigeon.setField("name", null);
		assertTrue(oldPigeon.isSet("name"));
		// nullify age field
		oldPigeon.setField("age", SpecialValue.NULL);
		assertTrue(oldPigeon.isSet("age"));
		assertTrue(!oldPigeon.isSet("birthdate"));
		// set two values of field reference - one null, and one set
		oldPigeon.setField("father", fatherPigeon);
		// nullify mother field
		oldPigeon.setField("mother", new NullifiedRecord(pigeonType));
		assertTrue(oldPigeon.isSet("father"));
		assertTrue(oldPigeon.isSet("mother"));
		
		// generate proxy
		RecordProxy proxy = RecordProxyUtil.generateCustomTypeProxy(oldPigeon, true, env, compiler);
		
		Method nameGetter = proxy.getClass().getMethod("getName");
		assertNull(nameGetter.invoke(proxy));
		assertTrue(proxy.isSet("name"));
		
		Method ageGetter = proxy.getClass().getMethod("getAge");
		assertNull(ageGetter.invoke(proxy));
		assertTrue(proxy.isSet("age"));
		assertTrue(proxy.isNull("age"));
		
		Method dobGetter = proxy.getClass().getMethod("getBirthdate");
		assertNull(dobGetter.invoke(proxy));
		assertFalse(proxy.isSet("birthdate"));
		assertFalse(proxy.isNull("birthdate"));
		
		Method fatherGetter = proxy.getClass().getMethod("getFather");
		assertNotNull(fatherGetter.invoke(proxy));
		assertTrue(proxy.isSet("father"));
		assertFalse(proxy.isNull("father"));
		
		Method motherGetter = proxy.getClass().getMethod("getMother");
		assertNull(motherGetter.invoke(proxy));
		assertTrue(proxy.isSet("mother"));
		assertTrue(proxy.isNull("mother"));
		
		// now convert back to record and make sure the value on the record are exactly as they
		// were before conversion to object proxy
		Record reconvertedRecord = RecordProxyUtil.generateRecord(proxy, pigeonType, 2, env);
		assertEquals(SpecialValue.NULL, reconvertedRecord.getField("name"));
		assertTrue(reconvertedRecord.isSet("name"));
		assertEquals(SpecialValue.NULL, reconvertedRecord.getField("age"));
		assertTrue(reconvertedRecord.isSet("age"));
		assertFalse(reconvertedRecord.isSet("birthdate"));
		assertTrue(reconvertedRecord.isSet("father"));
		assertTrue(reconvertedRecord.isSet("mother"));
		assertTrue(reconvertedRecord.getField("mother") instanceof NullifiedRecord);
		assertNotNull(((Record)reconvertedRecord.getField("father")));
		
		// save father pigeon
		fatherPigeon.setField("name", "Grzegorz");
		fatherPigeon.setField("age", BigDecimal.valueOf(3));
		fatherPigeon = dataService.save(fatherPigeon, env);
		reconvertedRecord.setField("father", fatherPigeon);
		
		// try to build an insert query for the reconverted record
		// we are doing this because at some stage building insert queries for nullified type references failed
		reconvertedRecord.setField("name", "Robert");
		reconvertedRecord.setField("age", BigDecimal.valueOf(5));
		reconvertedRecord = dataService.save(reconvertedRecord, env);
		assertNotNull(reconvertedRecord.attemptGetKID());
		
		// not create children field on type pigeon
		Field childrenField = new Field();
		childrenField.setApiName("children");
		childrenField.setLabel("Children");
		childrenField.setDataType(new InverseCollectionDataType(pigeonType, "father"));
		pigeonType.addField(childrenField);
		childrenField = dataService.createField(childrenField, env);
		assertNotNull(childrenField.getKID());
		
		pigeonType = env.getType(pigeonType.getKID());
		assertNotNull("Newly created field 'children' not registered with the environment", pigeonType.getField("children"));
		
		// fetch pigeon with children
		List<Record> fathersWithChildren = env.getSelectCriteriaFromDAL("select id, children.id, children.name from " + pigeonType.getQualifiedName() + " where id = '" + fatherPigeon.getKID() + "'").list();
		assertEquals(1, fathersWithChildren.size());
		fatherPigeon = fathersWithChildren.get(0);
		assertNotNull(fatherPigeon.getField("children"));
		assertTrue(fatherPigeon.getField("children") instanceof List);
		Record childPigeon = ((List<Record>)fatherPigeon.getField("children")).get(0);
		assertNotNull(childPigeon);
		assertNotNull(childPigeon.attemptGetField("name"));
		assertEquals(reconvertedRecord.getField("name"), childPigeon.getField("name"));
		assertNotNull(childPigeon.attemptGetField("id"));
		assertEquals(reconvertedRecord.getKID(), childPigeon.getKID());
		
		// generate proxy of a record that contains a collection
		RecordProxy fatherProxy = RecordProxyUtil.generateCustomTypeProxy(fatherPigeon, env, compiler);
		Method childrenGetter = fatherProxy.getClass().getMethod("getChildren");
		List<RecordProxy> childrenProxies = (List<RecordProxy>)childrenGetter.invoke(fatherProxy);
		assertNotNull(childrenProxies);
		assertEquals(1, childrenProxies.size());
		assertTrue("Expected child object to be an instance of ObjectProxy, but found " + childrenProxies.get(0).getClass().getName(), childrenProxies.get(0) instanceof RecordProxy);
		assertNotNull(childrenProxies.get(0).getId());
		assertEquals(reconvertedRecord.getKID(), childrenProxies.get(0).getId());
		
		// now convert back to record
		Record fatherRecord = RecordProxyUtil.generateRecord(fatherProxy, pigeonType, 100, env);
		assertNotNull(fatherRecord);
		assertNotNull(fatherRecord.getField("children"));
		assertTrue(fatherRecord.getField("children") instanceof List<?>);
		assertEquals(1, ((List<Record>)fatherRecord.getField("children")).size());
		assertTrue("Incorrect object type " + ((List<Record>)fatherRecord.getField("children")).get(0).getClass().getName(), ((List<Record>)fatherRecord.getField("children")).get(0) instanceof Record);
		
		return pigeonType;
	}

	@Test
	public void testGetNullInstance() throws RecordProxyException
	{
		Class instance = RecordProxy.getNullObject(Class.class);
		assertNotNull(instance);
		assertTrue(instance.isNull());
	}
	
	@Test
	public void testGetPropertiesByFields() throws KommetException
	{
		Class file = new Class();
		file.setJavaCode("test code");
		file.setName("test name");
		
		Map<String, Object> properties = RecordProxyUtil.getPropertyValuesByField(file);
		assertNotNull(properties);
		assertEquals(file.getJavaCode(), properties.get("javaCode"));
		assertEquals(file.getName(), properties.get("name"));
		assertTrue(properties.containsKey("kollCode"));
		assertTrue(properties.get("kollCode") == null);
	}
	
	
	@Test
	public void testInitEmptyPropertiesOnObjectStub() throws KommetException
	{
		Action action = new Action();
		Class file = action.getController();
		assertNull(file);
	}
}
