/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.TypeTrigger;
import kommet.basic.TypeWithTriggersDeleteException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.TextDataType;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.ClassService;
import kommet.koll.KollUtil;
import kommet.koll.annotations.Disabled;
import kommet.koll.annotations.triggers.AfterInsert;
import kommet.koll.annotations.triggers.AfterUpdate;
import kommet.koll.annotations.triggers.BeforeInsert;
import kommet.koll.annotations.triggers.BeforeUpdate;
import kommet.koll.annotations.triggers.OldValues;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.triggers.DatabaseTrigger;
import kommet.triggers.TriggerDeclarationException;
import kommet.triggers.TriggerService;
import kommet.triggers.TriggerUtil;
import kommet.triggers.TypeTriggerFilter;
import kommet.utils.MiscUtils;

public class TriggerTest extends BaseUnitTest
{
	@Inject
	TriggerService triggerService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	ClassService classService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Test
	public void testCreatingTrigger() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		createTrigger(env, false, false);
		createTrigger(env, true, false);
		createTrigger(env, false, true);
		createTrigger(env, true, true);
		
		testDeleteTriggerClass(env);
		testDisableTrigger(env);
		testDeleteType(env);
	}
	
	/**
	 * This test makes sure that type for which triggers exist cannot be deleted
	 * @param env
	 * @throws KommetException 
	 */
	private void testDeleteType(EnvData env) throws KommetException
	{
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		Field nameWithAge = new Field();
		nameWithAge.setApiName("nameWithAge");
		nameWithAge.setLabel("Name with age");
		nameWithAge.setRequired(false);
		nameWithAge.setDataType(new TextDataType(40));
		pigeonType.addField(nameWithAge);
		
		pigeonType = dataService.createType(pigeonType, env);
		
		List<String> annotations = new ArrayList<String>();
		annotations.add("@BeforeInsert");
		annotations.add("@BeforeUpdate");
		
		// import pigeon proxy class
		List<String> imports = new ArrayList<String>();
		imports.add(MiscUtils.userToEnvPackage(pigeonType.getQualifiedName(), env));
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create trigger that sets the name of every pigeon to "Se eu te pego"
		Class file = getTriggerFile(pigeonType, "for (" + pigeonType.getApiName() + " proxy : getNewValues()) { proxy.setNameWithAge(\"Se eu te pego\"); }", imports, annotations, classService, authData,  env, false);
				
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertTrue(triggerService.isTriggerRegisteredWithType(file.getId(), pigeonType.getKID(), env));
		
		try
		{
			dataService.deleteType(pigeonType, false, dataHelper.getRootAuthData(env), env);
			fail("Deleting type for which triggers exist should fail");
		}
		catch (TypeWithTriggersDeleteException e)
		{
			assertEquals("Type cannot be deleted because triggers exist for it", e.getMessage());
		}
		
		dataService.deleteType(pigeonType, true, dataHelper.getRootAuthData(env), env);
		assertTrue(env.getTriggers(pigeonType.getKID()).isEmpty());
	}

	private void testDeleteTriggerClass(EnvData env) throws KommetException
	{
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		Field nameWithAge = new Field();
		nameWithAge.setApiName("nameWithAge");
		nameWithAge.setLabel("Name with age");
		nameWithAge.setRequired(false);
		nameWithAge.setDataType(new TextDataType(40));
		pigeonType.addField(nameWithAge);
		
		pigeonType = dataService.createType(pigeonType, env);
		
		List<String> annotations = new ArrayList<String>();
		annotations.add("@BeforeInsert");
		annotations.add("@BeforeUpdate");
		
		// import pigeon proxy class
		List<String> imports = new ArrayList<String>();
		imports.add(MiscUtils.userToEnvPackage(pigeonType.getQualifiedName(), env));
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create trigger that sets the name of every pigeon to "Se eu te pego"
		Class file = getTriggerFile(pigeonType, "for (" + pigeonType.getApiName() + " proxy : getNewValues()) { proxy.setNameWithAge(\"Se eu te pego\"); }", imports, annotations, classService, authData, env, false);
				
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertTrue(triggerService.isTriggerRegisteredWithType(file.getId(), pigeonType.getKID(), env));
		
		// now create a pigeon and make sure the trigger worked on it
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("age", BigDecimal.valueOf(2));
		oldPigeon.setField("name", "Malin");
		
		Record savedPigeon = dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		
		// trigger is enabled, so it should have set the value
		assertEquals("Se eu te pego", savedPigeon.getField("nameWithAge"));
		dataService.deleteRecord(savedPigeon, env);
		
		// now delete the trigger file
		classService.delete(file, dataService, dataHelper.getRootAuthData(env), env);
		assertFalse(triggerService.isTriggerRegisteredWithType(file.getId(), pigeonType.getKID(), env));
		assertTrue(env.getTriggers(pigeonType.getKID()).isEmpty());
		
		// now create a pigeon and make sure the trigger worked on it
		oldPigeon = new Record(pigeonType);
		oldPigeon.setField("age", BigDecimal.valueOf(4));
		oldPigeon.setField("name", "Malina");
		
		savedPigeon = dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		
		// trigger is enabled, so it should have set the value
		assertFalse(savedPigeon.isSet("nameWithAge"));
		
		dataService.deleteType(pigeonType, dataHelper.getRootAuthData(env), env);
	}
	
	private void testDisableTrigger(EnvData env) throws KommetException
	{
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		Field nameWithAge = new Field();
		nameWithAge.setApiName("nameWithAge");
		nameWithAge.setLabel("Name with age");
		nameWithAge.setRequired(false);
		nameWithAge.setDataType(new TextDataType(40));
		pigeonType.addField(nameWithAge);
		
		pigeonType = dataService.createType(pigeonType, env);
		
		List<String> annotations = new ArrayList<String>();
		annotations.add("@BeforeInsert");
		annotations.add("@BeforeUpdate");
		
		// import pigeon proxy class
		List<String> imports = new ArrayList<String>();
		imports.add(MiscUtils.userToEnvPackage(pigeonType.getQualifiedName(), env));
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create trigger that sets the name of every pigeon to "Se eu te pego"
		Class file = getTriggerFile(pigeonType, "for (" + pigeonType.getApiName() + " proxy : getNewValues()) { proxy.setNameWithAge(\"Se eu te pego\"); }", imports, annotations, classService, authData, env, false);
				
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		KID fileId = file.getId();
		assertTrue(triggerService.isTriggerRegisteredWithType(file.getId(), pigeonType.getKID(), env));
		assertEquals(1, env.getTriggers(pigeonType.getKID()).size());
		
		// now create a pigeon and make sure the trigger worked on it
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("age", BigDecimal.valueOf(2));
		oldPigeon.setField("name", "Malin");
		
		Record savedPigeon = dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		
		// trigger is enabled, so it should have set the value
		assertEquals("Se eu te pego", savedPigeon.getField("nameWithAge"));
		dataService.deleteRecord(savedPigeon, env);
		
		// now update the trigger file
		annotations.add("@" + Disabled.class.getSimpleName());
		file = getTriggerFile(pigeonType, "for (" + pigeonType.getApiName() + " proxy : getNewValues()) { proxy.setNameWithAge(\"Se eu te pego\"); }", imports, annotations, classService, authData, env, false);
		file.setId(fileId);
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertFalse(triggerService.isTriggerRegisteredWithType(file.getId(), pigeonType.getKID(), env));
		assertTrue(env.getTriggers(pigeonType.getKID()).isEmpty());
		
		// now create a pigeon and make sure the trigger did not work on it
		oldPigeon = new Record(pigeonType);
		oldPigeon.setField("age", BigDecimal.valueOf(4));
		oldPigeon.setField("name", "Malina");
		
		savedPigeon = dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		
		// trigger is enabled, so it should have set the value
		assertFalse(savedPigeon.isSet("nameWithAge"));
		
		// reenabled trigger
		annotations = new ArrayList<String>();
		annotations.add("@BeforeInsert");
		annotations.add("@BeforeUpdate");
		file = getTriggerFile(pigeonType, "for (" + pigeonType.getApiName() + " proxy : getNewValues()) { proxy.setNameWithAge(\"Se eu te pego\"); }", imports, annotations, classService, authData, env, false);
		file.setId(fileId);
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertTrue(triggerService.isTriggerRegisteredWithType(file.getId(), pigeonType.getKID(), env));
		
		oldPigeon = new Record(pigeonType);
		oldPigeon.setField("age", BigDecimal.valueOf(4));
		oldPigeon.setField("name", "Malina");
		
		savedPigeon = dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		assertEquals("Se eu te pego", savedPigeon.getField("nameWithAge"));
		
		dataService.deleteType(pigeonType, dataHelper.getRootAuthData(env), env);
		classService.delete(file, dataService, dataHelper.getRootAuthData(env), env);
	}

	private void createTrigger(EnvData env, boolean isUseFullSave, boolean isDisabled) throws KommetException
	{
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		Field nameWithAge = new Field();
		nameWithAge.setApiName("nameWithAge");
		nameWithAge.setLabel("Name with age");
		nameWithAge.setRequired(false);
		nameWithAge.setDataType(new TextDataType(40));
		pigeonType.addField(nameWithAge);
		
		pigeonType = dataService.createType(pigeonType, env);
		
		testConvertTriggerKollCode(pigeonType, env);
		
		// make sure the list of triggers is empty but not null for type User
		Map<KID, TypeTrigger> pigeonTriggers = env.getTriggers(pigeonType.getKID());
		assertNotNull(pigeonTriggers);
		assertTrue(pigeonTriggers.isEmpty());
		
		// import pigeon proxy class
		List<String> imports = new ArrayList<String>();
		imports.add(MiscUtils.userToEnvPackage(pigeonType.getQualifiedName(), env));

		List<String> annotations = new ArrayList<String>();
		annotations.add("@BeforeInsert");
		annotations.add("@BeforeUpdate");
		
		if (isDisabled)
		{
			annotations.add("@" + Disabled.class.getSimpleName());
		}
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create trigger that sets the name of every pigeon to "Se eu te pego"
		Class file = getTriggerFile(pigeonType, "for (" + pigeonType.getApiName() + " proxy : getNewValues()) { proxy.setNameWithAge(\"Se eu te pego\"); }", imports, annotations, classService, authData, env, false);
		
		CompilationResult result = compiler.compile(file, env);
		assertTrue("Trigger compilation failed:\n" + result.getDescription() + "\n" + file.getJavaCode(), result.isSuccess());
		
		if (isUseFullSave)
		{
			List<Class> candidates = TriggerUtil.getTriggerCandidates(pigeonType, false, classService, compiler, env);
			assertEquals(0, candidates.size());
			
			file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
			
			candidates = TriggerUtil.getTriggerCandidates(pigeonType, false, classService, compiler, env);
			assertEquals(1, candidates.size());
			assertEquals(!isDisabled, triggerService.isTriggerRegisteredWithType(file.getId(), pigeonType.getKID(), env));
		}
		else
		{
			List<Class> candidates = TriggerUtil.getTriggerCandidates(pigeonType, false, classService, compiler, env);
			assertEquals(0, candidates.size());
			
			file = classService.save(file, dataHelper.getRootAuthData(env), env);
			
			try
			{
				candidates = TriggerUtil.getTriggerCandidates(pigeonType, false, classService, compiler, env);
				fail(ClassService.class.getSimpleName() + ".save does not convert KOLL file declaration (specifically, it does not swap trigger annotation type attribute), so this method should fail due to operating on an invalid trigger declaration");
			}
			catch (TriggerDeclarationException e)
			{
				// method 
				assertTrue("Incorrect exception message: " + e.getMessage(), e.getMessage().startsWith("Invalid type ID " + pigeonType.getQualifiedName() + " in trigger annotation on class"));
			}
		}
		assertNotNull(file.getId());
		
		if (!isUseFullSave)
		{
			if (isDisabled)
			{
				try
				{
					triggerService.registerTriggerWithType(file, pigeonType, dataHelper.getRootAuthData(env), env);
					fail("Registering disabled trigger should fail");
				}
				catch (KommetException e)
				{
					assertEquals("Trigger class with @Disabled annotation cannot be registered", e.getMessage());
				}
			}
			else
			{
				// register trigger with the type
				TypeTrigger typeTrigger = triggerService.registerTriggerWithType(file, pigeonType, dataHelper.getRootAuthData(env), env);
				assertTrue(typeTrigger.getIsBeforeInsert());
				assertTrue(triggerService.isTriggerRegisteredWithType(file.getId(), pigeonType.getKID(), env));
				assertTrue(typeTrigger.getIsBeforeUpdate());
				assertFalse(typeTrigger.getIsBeforeDelete());
				assertFalse(typeTrigger.getIsAfterInsert());
				assertFalse(typeTrigger.getIsAfterUpdate());
			}
		}
		
		// make sure a TypeTrigger record has been created
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTypeId(pigeonType.getKID());
		List<TypeTrigger> typeTriggers = triggerService.find(filter, env);
		
		if (isDisabled)
		{
			assertEquals(0, typeTriggers.size());
			pigeonTriggers = env.getTriggers(pigeonType.getKID());
			assertEquals(0, pigeonTriggers.size());
		}
		else
		{
			assertEquals(1, typeTriggers.size());
			assertEquals(pigeonType.getKID(), typeTriggers.get(0).getTypeId());
			assertEquals(file.getId(), typeTriggers.get(0).getTriggerFile().getId());
			assertEquals(file.getName(), typeTriggers.get(0).getTriggerFile().getName());
			assertEquals(file.getPackageName(), typeTriggers.get(0).getTriggerFile().getPackageName());
			assertTrue(typeTriggers.get(0).getIsBeforeInsert());
			assertTrue(typeTriggers.get(0).getIsBeforeUpdate());
			assertFalse(typeTriggers.get(0).getIsBeforeDelete());
			assertFalse(typeTriggers.get(0).getIsAfterInsert());
			assertFalse(typeTriggers.get(0).getIsAfterUpdate());
		
			// make sure the trigger is registered in the environment
			pigeonTriggers = env.getTriggers(pigeonType.getKID());
			assertEquals(1, pigeonTriggers.size());
			assertNotNull(pigeonTriggers.get(file.getId()));
			Class envTriggerFile = pigeonTriggers.values().iterator().next().getTriggerFile();
			assertEquals(typeTriggers.get(0).getTriggerFile().getId(), envTriggerFile.getId());
			assertEquals(file.getName(), envTriggerFile.getName());
			assertEquals(file.getPackageName(), envTriggerFile.getPackageName());
		}
		
		// now create a pigeon and make sure the trigger worked on it
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("age", BigDecimal.valueOf(2));
		oldPigeon.setField("name", "Malin");
		
		Record savedPigeon = dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		
		if (isDisabled)
		{
			// trigger is disabled, so it should not have set the value
			assertFalse(savedPigeon.isSet("nameWithAge"));
		}
		else
		{
			// trigger is enabled, so it should have set the value
			assertEquals("Se eu te pego", savedPigeon.getField("nameWithAge"));
		}
		
		assertNotNull("ID not set on the original record passed to the save method", oldPigeon.attemptGetKID());
		
		// now delete the type and make sure the trigger type assignment is deleted as well
		dataService.deleteType(pigeonType, dataHelper.getRootAuthData(env), env);
		assertTrue(env.getTriggers(pigeonType.getKID()).isEmpty());
		
		filter = new TypeTriggerFilter();
		filter.addTypeId(pigeonType.getKID());
		assertTrue(triggerService.find(filter, env).isEmpty());
		
		// delete trigger file
		classService.delete(file, dataService, dataHelper.getRootAuthData(env), env);
	}
	
	@Test
	public void testUnregisterTrigger() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		Field nameWithAge = new Field();
		nameWithAge.setApiName("nameWithAge");
		nameWithAge.setLabel("Name with age");
		nameWithAge.setRequired(false);
		nameWithAge.setDataType(new TextDataType(40));
		pigeonType.addField(nameWithAge);
		
		// save type
		pigeonType = dataService.createType(pigeonType, env);
		
		// import pigeon proxy class
		List<String> imports = new ArrayList<String>();
		imports.add(MiscUtils.userToEnvPackage(pigeonType.getQualifiedName(), env));
		
		List<String> annotations = new ArrayList<String>();
		annotations.add("@BeforeInsert");
		annotations.add("@BeforeUpdate");
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create trigger that sets the name of every pigeon to "Se eu te pego"
		Class file = getTriggerFile(pigeonType, "for (" + pigeonType.getApiName() + " proxy : getNewValues()) { proxy.setNameWithAge(\"Se eu te pego\"); }", imports, annotations, classService, authData, env);
		
		CompilationResult result = compiler.compile(file, env);
		assertTrue("Trigger compilation failed:\n" + result.getDescription() + "\n" + file.getJavaCode(), result.isSuccess());
		
		file = classService.save(file, dataHelper.getRootAuthData(env), env);
		assertNotNull(file.getId());
		
		// register trigger with the type
		TypeTrigger typeTrigger = triggerService.registerTriggerWithType(file, pigeonType, dataHelper.getRootAuthData(env), env);
		assertTrue(typeTrigger.getIsBeforeInsert());
		assertTrue(typeTrigger.getIsBeforeUpdate());
		assertFalse(typeTrigger.getIsBeforeDelete());
		assertFalse(typeTrigger.getIsAfterInsert());
		assertFalse(typeTrigger.getIsAfterUpdate());
		
		// make sure a TypeTrigger record has been created
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTypeId(pigeonType.getKID());
		List<TypeTrigger> typeTriggers = triggerService.find(filter, env);
		assertEquals(1, typeTriggers.size());
		assertEquals(pigeonType.getKID(), typeTriggers.get(0).getTypeId());
		assertEquals(file.getId(), typeTriggers.get(0).getTriggerFile().getId());
		assertEquals(file.getName(), typeTriggers.get(0).getTriggerFile().getName());
		assertEquals(file.getPackageName(), typeTriggers.get(0).getTriggerFile().getPackageName());
		assertTrue(typeTriggers.get(0).getIsBeforeInsert());
		assertTrue(typeTriggers.get(0).getIsBeforeUpdate());
		assertFalse(typeTriggers.get(0).getIsBeforeDelete());
		assertFalse(typeTriggers.get(0).getIsAfterInsert());
		assertFalse(typeTriggers.get(0).getIsAfterUpdate());
		
		// make sure the trigger is registered in the environment
		Map<KID, TypeTrigger> pigeonTriggers = env.getTriggers(pigeonType.getKID());
		assertEquals(1, pigeonTriggers.size());
		assertNotNull(pigeonTriggers.get(file.getId()));
		Class envTriggerFile = pigeonTriggers.values().iterator().next().getTriggerFile();
		assertEquals(typeTrigger.getTriggerFile().getId(), envTriggerFile.getId());
		assertEquals(file.getName(), envTriggerFile.getName());
		assertEquals(file.getPackageName(), envTriggerFile.getPackageName());
		
		// now create a pigeon and make sure the trigger worked on it
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("age", BigDecimal.valueOf(2));
		oldPigeon.setField("name", "Malin");
		
		Record savedPigeon = dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		assertEquals("Se eu te pego", savedPigeon.getField("nameWithAge"));
		assertNotNull("ID not set on the original record passed to the save method", oldPigeon.attemptGetKID());
		
		// now unregister the trigger
		triggerService.unregisterTriggerWithType(file.getId(), pigeonType.getKID(), env);
		
		filter = new TypeTriggerFilter();
		filter.addTypeId(pigeonType.getKID());
		assertTrue(triggerService.find(filter, env).isEmpty());
		
		// now create a pigeon and make sure the trigger did not work on it
		Record youngPigeon = new Record(pigeonType);
		youngPigeon.setField("age", BigDecimal.valueOf(4));
		youngPigeon.setField("name", "Leo");
		
		savedPigeon = dataService.save(youngPigeon, dataHelper.getRootAuthData(env), env);
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select name, age, nameWithAge from " + pigeonType.getQualifiedName() + " where id = '" + savedPigeon.getKID() + "'").list();
		assertNull(pigeons.get(0).getField("nameWithAge"));
	}
	
	@Test
	public void testAfterDeleteTrigger() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		// save type
		pigeonType = dataService.createType(pigeonType, env);
		
		// make sure the list of triggers is empty but not null for type User
		Map<KID, TypeTrigger> pigeonTriggers = env.getTriggers(pigeonType.getKID());
		assertNotNull(pigeonTriggers);
		assertTrue(pigeonTriggers.isEmpty());
		
		Type logType = getLogType(env);
		
		// import pigeon proxy class
		List<String> imports = new ArrayList<String>();
		imports.add(MiscUtils.userToEnvPackage(pigeonType.getQualifiedName(), env));
		imports.add(MiscUtils.userToEnvPackage(logType.getQualifiedName(), env));
		imports.add(KommetException.class.getName());
		
		List<String> annotations = new ArrayList<String>();
		annotations.add("@AfterDelete");
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create trigger that create a LogInfo object with the name of the deleted pigeon
		Class file = getTriggerFile(pigeonType, "System.out.println(\"Trigger called for size \" + getOldValues().size());\nfor (" + pigeonType.getApiName() + " proxy : getOldValues())\n{ try { \nLogInfo log = new LogInfo(); \nlog.setMessage(proxy.getName()); System.out.println(\"Saving...\"); getSys().save(log); \nSystem.out.println(\"Saved\"); } catch (KommetException e) { e.printStackTrace(); }\n}", imports, annotations, classService, authData, env);
		
		CompilationResult result = compiler.compile(file, env);
		assertTrue("Trigger compilation failed:\n" + result.getDescription() + "\n" + file.getJavaCode(), result.isSuccess());
		
		file = classService.save(file, dataHelper.getRootAuthData(env), env);
		assertNotNull(file.getId());
		
		// register trigger with the type
		TypeTrigger typeTrigger = triggerService.registerTriggerWithType(file, pigeonType, dataHelper.getRootAuthData(env), env);
		assertFalse(typeTrigger.getIsBeforeInsert());
		assertFalse(typeTrigger.getIsBeforeUpdate());
		assertFalse(typeTrigger.getIsBeforeDelete());
		assertTrue(typeTrigger.getIsAfterDelete());
		assertFalse(typeTrigger.getIsAfterInsert());
		assertFalse(typeTrigger.getIsAfterUpdate());
		
		// now create a pigeon and make sure the trigger worked on it
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("age", BigDecimal.valueOf(2));
		oldPigeon.setField("name", "Malin");
		
		dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		
		// make sure no logs have been inserted
		List<Record> logs = env.getSelectCriteriaFromDAL("select id, message from " + logType.getQualifiedName()).list();
		assertTrue(logs.isEmpty());
		
		// delete pigeon
		dataService.deleteRecord(oldPigeon, dataHelper.getRootAuthData(env), env);
		
		logs = env.getSelectCriteriaFromDAL("select id, message from " + logType.getQualifiedName()).list();
		assertEquals(1, logs.size());
		assertEquals("Malin", logs.get(0).getField("message"));
	}
	
	@Test
	public void testTriggerWithOldValues() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		// make sure the list of triggers is empty but not null for type User
		Map<KID, TypeTrigger> pigeonTriggers = env.getTriggers(pigeonType.getKID());
		assertNotNull(pigeonTriggers);
		assertTrue(pigeonTriggers.isEmpty());
		
		Type logType = getLogType(env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		env = testOldValuesWithAnnotation(BeforeInsert.class, true, false, false, true, logType, pigeonType, env, authData);
		env = testOldValuesWithAnnotation(BeforeUpdate.class, true, false, true, false, logType, pigeonType, env, authData);
		env = testOldValuesWithAnnotation(AfterInsert.class, true, true, false, true, logType, pigeonType, env, authData);
		env = testOldValuesWithAnnotation(AfterUpdate.class, true, true, true, false, logType, pigeonType, env, authData);
		
		// without the @OldValues annotation
		env = testOldValuesWithAnnotation(BeforeInsert.class, false, false, false, true, logType, pigeonType, env, authData);
		env = testOldValuesWithAnnotation(BeforeUpdate.class, false, false, true, false, logType, pigeonType, env, authData);
		env = testOldValuesWithAnnotation(AfterInsert.class, false, true, false, true, logType, pigeonType, env, authData);
		env = testOldValuesWithAnnotation(AfterUpdate.class, false, true, true, false, logType, pigeonType, env, authData);
	}
	
	private EnvData testOldValuesWithAnnotation(java.lang.Class<?> annotation, boolean addOldValuesAnnot, boolean isAfter, boolean isUpdate, boolean expectError, Type logType, Type pigeonType, EnvData env, AuthData authData) throws KommetException
	{
		// check flags
		assertFalse(env.hasTypeAfterTriggersWithOldProxies(pigeonType.getKID()));
		assertFalse(env.hasTypeBeforeTriggersWithOldProxies(pigeonType.getKID()));
		
		// import pigeon proxy class
		List<String> imports = new ArrayList<String>();
		imports.add(MiscUtils.userToEnvPackage(pigeonType.getQualifiedName(), env));
		imports.add(MiscUtils.userToEnvPackage(logType.getQualifiedName(), env));
		imports.add(KommetException.class.getName());
				
		List<String> annotations = new ArrayList<String>();
		annotations.add("@" + annotation.getName());
		
		if (addOldValuesAnnot)
		{
			annotations.add("@" + OldValues.class.getName());
		}
		
		// create trigger that create a LogInfo object with the name of the deleted pigeon
		Class file = getTriggerFile(pigeonType, "if (getOldValues() == null) { LogInfo log = new LogInfo(); \nlog.setMessage(\"Old values are null \" + getNewValues().get(0).getName()); log = getSys().save(log); } else { for (" + pigeonType.getApiName() + " proxy : getOldValues())\n{ try { \nLogInfo log = new LogInfo(); \nlog.setMessage(\"Old name \" + proxy.getName()); System.out.println(\"Saving...\"); getSys().save(log); \nSystem.out.println(\"Saved\"); } catch (KommetException e) { e.printStackTrace(); }\n} \n}", imports, annotations, classService, authData, env);
				
		file = classService.fullSave(file, dataService, authData, env);
		assertNotNull(file.getId());
		
		// check flags that say whether old values should be injected to triggers of this type
		assertEquals(isAfter && addOldValuesAnnot, env.hasTypeAfterTriggersWithOldProxies(pigeonType.getKID()));
		assertEquals(!isAfter && addOldValuesAnnot, env.hasTypeBeforeTriggersWithOldProxies(pigeonType.getKID()));
		
		String oldName = MiscUtils.getHash(10);
		
		// now create a pigeon and make sure the trigger worked on it
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("age", BigDecimal.valueOf(2));
		oldPigeon.setField("name", oldName);
		
		oldPigeon = dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		
		if (isUpdate)
		{
			// we want to test update, so we need to save again
			dataService.save(oldPigeon, dataHelper.getRootAuthData(env), env);
		}
		
		// error is expected when the flag is set, or if the @OldValues annotation is not present, because in this case old values will never be injected
		if (expectError || !addOldValuesAnnot)
		{	
			// expected error in logs
			List<Record> logs = env.getSelectCriteriaFromDAL("select id from " + logType.getQualifiedName() + " where message = 'Old values are null " + oldName + "'").list();
			if (logs.size() != 1)
			{
				logs = env.getSelectCriteriaFromDAL("select id, message from " + logType.getQualifiedName() + " where message = 'Old name " + oldName + "'").list();
				if (logs.size() == 1)
				{
					fail("Old values injected, but were expected not to");
				}
				else
				{
					fail("Trigger not executed as expected");
				}
			}
		}
		else
		{	
			List<Record> logs = env.getSelectCriteriaFromDAL("select id, message from " + logType.getQualifiedName() + " where message = 'Old name " + oldName + "'").list();
			
			if (logs.isEmpty())
			{
				logs = env.getSelectCriteriaFromDAL("select id from " + logType.getQualifiedName() + " where message = 'Old values are null " + oldName + "'").list();
				if (logs.size() == 1)
				{
					fail("Old values not injected");
				}
				else
				{
					fail("Trigger not executed as expected");
				}
			}
		}
		
		// reinitialize environment
		envService.resetEnv(env.getId());
		env = envService.get(env.getId());
		
		// check flags after env has been reinitialized
		assertEquals(isAfter && addOldValuesAnnot, env.hasTypeAfterTriggersWithOldProxies(pigeonType.getKID()));
		assertEquals(!isAfter && addOldValuesAnnot, env.hasTypeBeforeTriggersWithOldProxies(pigeonType.getKID()));
		
		// try to save another pigeon
		String oldName2 = MiscUtils.getHash(10);
		
		// now create a pigeon and make sure the trigger worked on it
		Record oldPigeon2 = new Record(pigeonType);
		oldPigeon2.setField("age", BigDecimal.valueOf(2));
		oldPigeon2.setField("name", oldName2);
		
		oldPigeon2 = dataService.save(oldPigeon2, dataHelper.getRootAuthData(env), env);
		
		if (isUpdate)
		{
			// we want to test update, so we need to save again
			dataService.save(oldPigeon2, dataHelper.getRootAuthData(env), env);
		}
		
		if (expectError || !addOldValuesAnnot)
		{	
			// expected error in logs
			List<Record> logs = env.getSelectCriteriaFromDAL("select id, message from " + logType.getQualifiedName() + " where message = 'Old values are null " + oldName2 + "'").list();
			
			if (logs.size() != 1)
			{
				logs = env.getSelectCriteriaFromDAL("select id, message from " + logType.getQualifiedName() + " where message = 'Old name " + oldName + "'").list();
				if (logs.size() == 1)
				{
					fail("Old values injected, but were expected not to");
				}
				else
				{
					fail("Trigger not executed as expected");
				}
			}
		}
		else
		{	
			List<Record> logs = env.getSelectCriteriaFromDAL("select id, message from " + logType.getQualifiedName() + " where message = 'Old name " + oldName + "'").list();
			
			if (logs.isEmpty())
			{
				logs = env.getSelectCriteriaFromDAL("select id from " + logType.getQualifiedName() + " where message = 'Old values are null " + oldName + "'").list();
				if (logs.size() == 1)
				{
					fail("Old values not injected");
				}
				else
				{
					fail("Trigger not executed as expected");
				}
			}
		}
		
		Class triggerWithOldValues = null;
		
		if (!addOldValuesAnnot)
		{
			// if old values were not injected in the main trigger, inject them in another one
			annotations.add("@" + OldValues.class.getName());
			
			triggerWithOldValues = getTriggerFile("AnotherTrigger", pigeonType, "if (getOldValues() == null) { LogInfo log = new LogInfo(); \nlog.setMessage(\"Old values are null \" + getNewValues().get(0).getName()); log = getSys().save(log); } else { for (" + pigeonType.getApiName() + " proxy : getOldValues())\n{ try { \nLogInfo log = new LogInfo(); \nlog.setMessage(\"Old name \" + proxy.getName()); System.out.println(\"Saving...\"); getSys().save(log); \nSystem.out.println(\"Saved\"); } catch (KommetException e) { e.printStackTrace(); }\n} \n}", imports, annotations, classService, authData, env);
			
			triggerWithOldValues = classService.fullSave(triggerWithOldValues, dataService, authData, env);
			assertNotNull(triggerWithOldValues.getId());
			
			// check flags that say whether old values should be injected to triggers of this type
			assertEquals(isAfter, env.hasTypeAfterTriggersWithOldProxies(pigeonType.getKID()));
			assertEquals(!isAfter, env.hasTypeBeforeTriggersWithOldProxies(pigeonType.getKID()));
		}
		
		// remove the first trigger file
		classService.delete(file, dataService, authData, env);
		
		if (addOldValuesAnnot)
		{
			// check flags
			assertFalse(env.hasTypeAfterTriggersWithOldProxies(pigeonType.getKID()));
			assertFalse(env.hasTypeBeforeTriggersWithOldProxies(pigeonType.getKID()));
		}
		else
		{
			assertEquals(isAfter, env.hasTypeAfterTriggersWithOldProxies(pigeonType.getKID()));
			assertEquals(!isAfter, env.hasTypeBeforeTriggersWithOldProxies(pigeonType.getKID()));
			
			// remove the second trigger
			classService.delete(triggerWithOldValues, dataService, authData, env);
			
			// now there are no triggers with old values so flags should be null
			assertFalse(env.hasTypeAfterTriggersWithOldProxies(pigeonType.getKID()));
			assertFalse(env.hasTypeBeforeTriggersWithOldProxies(pigeonType.getKID()));
		}
		
		return env;
	}

	private Type getLogType(EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName("LogInfo");
		type.setLabel("Log Info");
		type.setPluralLabel("Log Infos");
		type.setPackage(TestDataCreator.PIGEON_TYPE_PACKAGE);
		
		Field msgField = new Field();
		msgField.setApiName("message");
		msgField.setLabel("Message");
		msgField.setRequired(true);
		msgField.setDataType(new TextDataType(100));
		type.addField(msgField);
		
		dataService.createType(type, env);
		return type;
	}

	private void testConvertTriggerKollCode(Type type, EnvData env) throws KommetException
	{
		String originalCode = "import some.thing;\n\n@Trigger (type = \"" + type.getQualifiedName() + "\")\npublic class SomeTrigger extends " + DatabaseTrigger.class.getName() + "<" + type.getQualifiedName() + ">\n{ }";
		String convertedCode = TriggerUtil.convertTriggerKollCode(originalCode, "SomeTrigger", type, env);
		//assertTrue("Trigger KOLL code not converted:\n" + convertedCode + "\n", convertedCode.contains("public class SomeTrigger extends " + TriggerFile.class.getName() + "<" + type.getQualifiedName() + ">"));
		assertTrue("Type name not changed to ID in @Trigger annotation:\n" + convertedCode + "\n", convertedCode.contains("@Trigger(type=\"" + type.getKID() + "\")"));
	}
	
	private Class getTriggerFile(String triggerName, Type type, String executeMethodCode, List<String> imports, List<String> annotations, ClassService classService, AuthData authData, EnvData env) throws KommetException
	{
		return getTriggerFile(triggerName, type, executeMethodCode, imports, annotations, classService, authData, env, true);
	}
	
	private Class getTriggerFile(Type type, String executeMethodCode, List<String> imports, List<String> annotations, ClassService classService, AuthData authData, EnvData env) throws KommetException
	{
		return getTriggerFile("TestTrigger", type, executeMethodCode, imports, annotations, classService, authData, env, true);
	}
	
	public static Class getTriggerFile(Type type, String executeMethodCode, List<String> imports, List<String> annotations, ClassService classService, AuthData authData, EnvData env, boolean isConvertCode) throws KommetException
	{
		return getTriggerFile("TestTrigger", type, executeMethodCode, imports, annotations, classService, authData, env, isConvertCode);
	}

	public static Class getTriggerFile(String triggerName, Type type, String executeMethodCode, List<String> imports, List<String> annotations, ClassService classService, AuthData authData, EnvData env, boolean isConvertCode) throws KommetException
	{
		String packageName = "kommet.pckg";
		Class file = new Class();
		file.setIsSystem(false);
		file.setName(triggerName);
		file.setPackageName(packageName);
		file.setKollCode(getTriggerCode(triggerName, packageName, imports, type, annotations, executeMethodCode, env));
		
		if (isConvertCode)
		{
			file.setJavaCode(classService.getKollTranslator(env).kollToJava(TriggerUtil.convertTriggerKollCode(file.getKollCode(), file.getName(), type, env), true, authData, env));
		}
		else
		{
			file.setJavaCode(classService.getKollTranslator(env).kollToJava(file.getKollCode(), true, authData, env));
		}
		return file;
	}

	private static String getTriggerCode(String triggerName, String packageName, List<String> imports, Type type, List<String> annotations, String executeMethodCode, EnvData env) throws KommetException
	{
		StringBuilder innerCode = new StringBuilder();
		innerCode.append("public void execute() throws " + Exception.class.getName() + "\n{ ").append(executeMethodCode).append(" }");
		
		return KollUtil.getTemplateKollCode(triggerName + " extends " + DatabaseTrigger.class.getName() + "<" + MiscUtils.userToEnvPackage(type.getQualifiedName(), env) + ">", packageName, imports, "@Trigger(type = \"" + type.getQualifiedName() + "\")\n" + MiscUtils.implode(annotations, "\n"), innerCode.toString(), env);
	}
}
