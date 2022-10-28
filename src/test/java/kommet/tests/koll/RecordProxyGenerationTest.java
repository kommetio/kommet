/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.koll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyClassGenerator;
import kommet.basic.RecordProxyUtil;
import kommet.basic.User;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.ClassService;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.MiscUtils;

public class RecordProxyGenerationTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ClassService classService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	UserService userService;
	
	@Inject
	EnvService envService;
	
	@Test
	public void testAssociationWithUserType() throws KommetException, ClassNotFoundException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		// We want to make sure that proxies created in this test are really created, and not cached proxies used.
		// This is why we delete and create anew the KOLL dir where all proxies are stored.
		compiler.clearKollCache(env);
		
		// restore KOLL dir and proxies by calling getEnv
		envService.resetEnv(env.getId());
		env = envService.get(env.getId());
		
		Type pigeonType = dataService.createType(dataHelper.getPigeonType(env), env);
		
		Type pigeonOwnerAssignmentType = getPigeonOwnerAssignmentType(pigeonType, env);
		
		// add association field to owner useing pigeon-owner assignment as linking type
		Field ownersField = new Field();
		ownersField.setApiName("owners");
		ownersField.setLabel("Owners");
		ownersField.setDataType(new AssociationDataType(pigeonOwnerAssignmentType, env.getType(KeyPrefix.get(KID.USER_PREFIX)), "pigeon", "owner"));
		pigeonType.addField(ownersField);
		
		ownersField = dataService.createField(ownersField, env);
		assertNotNull(ownersField.getKID());
		
		// make sure an owner list has been generated in the pigeon proxy class
		java.lang.Class<?> pigeonProxyClass = compiler.getClass(pigeonType.getQualifiedName(), true, env);
		assertNotNull(pigeonProxyClass);
		
		// make sure users can still be queries using standard object proxies
		// created at compile time
		User user = userService.getUser(env.getRootUser().getKID(), env);
		assertNotNull(user);
	}
	
	private Type getPigeonOwnerAssignmentType(Type pigeonType, EnvData env) throws KommetException
	{
		// create an assignment of pigeon to owner
		Type pigeonOwnerAssignment = new Type();
		pigeonOwnerAssignment.setApiName("PigeonOwnerAssignment");
		pigeonOwnerAssignment.setLabel("Pigeon Owner Assignment");
		pigeonOwnerAssignment.setPluralLabel("Pigeon Owner Assignments");
		pigeonOwnerAssignment.setPackage(pigeonType.getPackage());
		
		// add reference to type user
		Field userField = new Field();
		userField.setApiName("owner");
		userField.setLabel("Owner");
		userField.setDataType(new TypeReference(env.getType(KeyPrefix.get(KID.USER_PREFIX))));
		userField.setRequired(true);
		pigeonOwnerAssignment.addField(userField);
		
		// add reference to type pigeon
		Field pigeonField = new Field();
		pigeonField.setApiName("pigeon");
		pigeonField.setLabel("Pigeon");
		pigeonField.setDataType(new TypeReference(pigeonType));
		pigeonField.setRequired(true);
		pigeonOwnerAssignment.addField(pigeonField);
		
		return dataService.createType(pigeonOwnerAssignment, env);
	}
	
	@Test
	public void testGenerateObjectProxyClass() throws KommetException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, ClassNotFoundException
	{		
		EnvData env = dataHelper.configureFullTestEnv();
		
		// We want to make sure that proxies created in this test are really created, and not cached proxies used.
		// This is why we delete and create anew the KOLL dir where all proxies are stored.
		compiler.clearKollCache(env);
		
		// restore KOLL dir and proxies by calling getEnv
		envService.resetEnv(env.getId());
		env = envService.get(env.getId());
		
		// create pigeon type
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// now create a proxy class to represent the pigeon object
		Class kollFile = RecordProxyClassGenerator.getProxyKollClass(pigeonType, true, true, classService, authData, env);
		
		NumberDataType numberDataType = ((NumberDataType)pigeonType.getField("age").getDataType());
		
		assertTrue(kollFile.getKollCode().contains("public " + numberDataType.getJavaType() + " getAge()"));
		assertTrue(kollFile.getKollCode().contains("setInitialized(\"name\");"));
		
		// make sure the classes name is the same as the type's API name
		assertEquals(pigeonType.getApiName(), kollFile.getName());
		
		// now compile the class
		CompilationResult compilationResult = compiler.compile(kollFile, env);
		if (!compilationResult.isSuccess())
		{	
			fail("Compilation failed: " + compilationResult.getDescription());
		}
		
		// now make sure a setter and getter have been generated for each object's field
		java.lang.Class<?> pigeonStubClass = compiler.getClass(pigeonType.getQualifiedName(), true, env);
		assertNotNull(pigeonStubClass);
		assertEquals(pigeonType.getApiName(), pigeonStubClass.getSimpleName());
		
		Object pigeonUncast  = pigeonStubClass.newInstance();

		if (!(pigeonUncast instanceof RecordProxy))
		{
			fail("Error casting proxy class to " + RecordProxy.class.getName() + " - the proxy class does not extend " + RecordProxy.class.getSimpleName());
		}
		
		// create an instance of the proxy class
		RecordProxy pigeon = (RecordProxy)pigeonUncast;
		
		Method isSetMethod = pigeon.getClass().getMethod("isSet", String.class);
		assertNotNull(isSetMethod);
		
		// check that all properties can be set and read
		assertNull(BeanUtils.getProperty(pigeon, "name"));
		assertFalse((Boolean)isSetMethod.invoke(pigeon, "name"));
		PropertyUtils.setProperty(pigeon, "name", "Jasiek");
		assertEquals("Jasiek", PropertyUtils.getProperty(pigeon, "name"));
		assertTrue((Boolean)isSetMethod.invoke(pigeon, "name"));
		
		assertNull(BeanUtils.getProperty(pigeon, "age"));
		PropertyUtils.setProperty(pigeon, "age", 4);
		assertEquals(4, PropertyUtils.getProperty(pigeon, "age"));
		
		assertNull(PropertyUtils.getProperty(pigeon, "colour"));
		assertFalse((Boolean)isSetMethod.invoke(pigeon, "colour"));
		PropertyUtils.setProperty(pigeon, "colour", "red");
		assertTrue((Boolean)isSetMethod.invoke(pigeon, "colour"));
		assertEquals("red", PropertyUtils.getProperty(pigeon, "colour"));
		
		Date birthDate = new Date();
		assertNull(BeanUtils.getProperty(pigeon, "birthdate"));
		assertFalse((Boolean)isSetMethod.invoke(pigeon, "birthdate"));
		PropertyUtils.setProperty(pigeon, "birthdate", birthDate);
		assertTrue((Boolean)isSetMethod.invoke(pigeon, "birthdate"));
		assertEquals(birthDate, PropertyUtils.getProperty(pigeon, "birthdate"));
		
		assertNull(BeanUtils.getProperty(pigeon, "father"));
		PropertyUtils.setProperty(pigeon, "father", pigeon);
		assertEquals("Jasiek", PropertyUtils.getProperty(pigeon, "father.name"));
		
		// generate a record from the stub
		Record pigeonRec = RecordProxyUtil.generateRecord(pigeon, pigeonType, 2, env);
		assertNotNull(pigeonRec);
		
		// now extend the pigeon object with some property and compile it again
		Field boughtField = new Field();
		boughtField.setApiName("isBought");
		boughtField.setLabel("Is Bought");
		boughtField.setDataType(new BooleanDataType());
		pigeonType.addField(boughtField);
		
		Class newKollFile = RecordProxyClassGenerator.getProxyKollClass(pigeonType, true, true, classService, authData, env);
		
		// recompile class
		compiler.compile(newKollFile, env);
		// refetch the class by reloading the class loader
		compiler.resetClassLoader(env);
		java.lang.Class<?> newPigeonStubClass = compiler.getClass(pigeonType.getQualifiedName(), true, env);
		RecordProxy newPigeon = (RecordProxy)newPigeonStubClass.newInstance();
		
		assertNull(BeanUtils.getProperty(newPigeon, "isBought"));
		assertFalse((Boolean)isSetMethod.invoke(newPigeon, "isBought"));
		assertNull(PropertyUtils.getProperty(newPigeon, "isBought"));
		PropertyUtils.setProperty(newPigeon, "isBought", true);
		assertTrue((Boolean)isSetMethod.invoke(newPigeon, "isBought"));
		assertEquals(true, PropertyUtils.getProperty(newPigeon, "isBought"));
		
		testInverseCollectionProxies(pigeonType, env);
	}
	
	@SuppressWarnings("unchecked")
	private void testInverseCollectionProxies(Type pigeonType, EnvData env) throws KommetException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
	{
		// add pigeon field to user
		Field pigeonField = new Field();
		pigeonField.setApiName("pigeon");
		pigeonField.setDataType(new TypeReference(pigeonType));
		pigeonField.setLabel("Pigeon");
		pigeonField.setType(env.getType(KeyPrefix.get(KID.USER_PREFIX)));
		
		dataService.createField(pigeonField, env);
		
		// add an inverse collection field "users" to profile type
		Field usersField = new Field();
		usersField.setApiName("users");
		usersField.setDataType(new InverseCollectionDataType(env.getType(KeyPrefix.get(KID.USER_PREFIX)), "pigeon"));
		usersField.setLabel("Users");
		usersField.setType(pigeonType);
		
		assertNull(pigeonType.getField("users"));
		dataService.createField(usersField, env);
		assertNull(pigeonType.getField("users"));
		
		pigeonType = env.getType(pigeonType.getKID());
		assertNotNull(pigeonType.getField("users"));
		
		// create a test pigeon
		Record pigeon = new Record(pigeonType);
		pigeon.setField("name", "Hank");
		pigeon.setField("age", 1);
		pigeon = dataService.save(pigeon, env);
		assertNotNull(pigeon.getKID());
		List<Record> insertedPigeons = env.getSelectCriteriaFromDAL("select id, users.id, users.userName from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + pigeon.getKID() + "'").list();
		assertEquals(1, insertedPigeons.size());
		assertEquals(0, ((List<Record>)insertedPigeons.get(0).getField("users")).size());
		
		Record adminProfile = (Record)env.getRootUser().getField("profile");
		assertNotNull(adminProfile);
		Record newUserOne = dataHelper.getTestUser("UserOne", "UserOne@kolmu.com", adminProfile, env);
		newUserOne.setField("pigeon", pigeon);
		Record newUserTwo = dataHelper.getTestUser("UserTwo", "UserTwo@kolmu.com", adminProfile, env);
		newUserTwo.setField("pigeon", pigeon);
		
		dataService.save(newUserOne, env);
		dataService.save(newUserTwo, env);
		
		// select pigeon with users
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, users.id, users.userName from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + pigeon.getKID() + "'").list();
		assertEquals(1, pigeons.size());
		Record retrievedPigeon = pigeons.get(0);
		assertEquals(pigeon.getKID(), retrievedPigeon.getKID());
		assertNotNull(retrievedPigeon.getField("users"));
		List<Record> usersCollection = (List<Record>)retrievedPigeon.getField("users");
		assertEquals(2, usersCollection.size());
		
		// now generate a proxy for the pigeon object
		java.lang.Class<? extends RecordProxy> pigeonProxyClass = (java.lang.Class<? extends RecordProxy>)compiler.getClass(pigeonType.getQualifiedName(), true, env);
		assertNotNull(pigeonProxyClass);
		Object pigeonProxy = RecordProxyUtil.generateCustomTypeProxy(pigeonProxyClass, retrievedPigeon, true, env);
		assertNotNull(pigeonProxy);
		assertTrue(pigeonProxy.getClass().isAssignableFrom(pigeonProxyClass));
		
		Method userGetter = null;
		try
		{
			userGetter = pigeonProxy.getClass().getMethod("getUsers");
		}
		catch (SecurityException e)
		{
			fail("Method getUsers is not accessible in generated proxy class");
		}
		catch (NoSuchMethodException e)
		{
			fail("Method getUsers does not exist in generated proxy class");
		}
		
		Object userProxyCollection = userGetter.invoke(pigeonProxy);
		assertNotNull(userProxyCollection);
		assertTrue(userProxyCollection.getClass().isAssignableFrom(ArrayList.class));
		assertEquals(2, ((ArrayList<?>)userProxyCollection).size());
		RecordProxy userProxy = (RecordProxy)((ArrayList<?>)userProxyCollection).get(0);
		assertEquals(MiscUtils.userToEnvPackage(env.getType(KeyPrefix.get(KID.USER_PREFIX)).getQualifiedName(), env), userProxy.getClass().getName());
		
		// read username from user
		Method usernameGetter = userProxy.getClass().getMethod("getUserName");
		assertNotNull(usernameGetter);
		String userName = (String)usernameGetter.invoke(userProxy);
		assertTrue(newUserOne.getField("userName").equals(userName) || newUserTwo.getField("userName").equals(userName));
		
		// since password was not included in the select query, it should not be initialized in the proxy
		Method pwdGetter = userProxy.getClass().getMethod("getPassword");
		assertNotNull(pwdGetter);
		String pwd = (String)pwdGetter.invoke(userProxy);
		assertNull(pwd);
		
		// now delete child records and make sure proxy reflects this
		dataService.deleteRecord(newUserOne, env);
		dataService.deleteRecord(newUserTwo, env);
		
		List<Record> pigeonsWithoutUsers = env.getSelectCriteriaFromDAL("select id, users.id, users.userName from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + pigeon.getKID() + "'").list();
		assertEquals(1, pigeonsWithoutUsers.size());
		Record pigeonWithoutUsers = pigeonsWithoutUsers.get(0);
		assertEquals(pigeon.getKID(), pigeonWithoutUsers.getKID());
		assertNotNull(pigeonWithoutUsers.getField("users"));
		usersCollection = (List<Record>)pigeonWithoutUsers.getField("users");
		assertEquals("Inverse collection should be empty, but it contains records", 0, usersCollection.size());
		
		// generate proxy
		pigeonProxy = RecordProxyUtil.generateCustomTypeProxy(pigeonProxyClass, pigeonWithoutUsers, true, env);
		userProxyCollection = userGetter.invoke(pigeonProxy);
		assertNotNull(userProxyCollection);
		assertTrue(userProxyCollection.getClass().isAssignableFrom(ArrayList.class));
		assertEquals(0, ((ArrayList<?>)userProxyCollection).size());
		
		Class kollFile = RecordProxyClassGenerator.getProxyKollClass(pigeonType, true, true, classService, dataHelper.getRootAuthData(env), env);
		assertTrue("Invalid java code: " + kollFile.getJavaCode(), kollFile.getJavaCode().contains("public java.util.ArrayList<kommet.envs.env" + env.getId() + ".kommet.basic.User> getUsers()"));
	}
}
