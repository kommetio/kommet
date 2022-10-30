/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.RecordAccessType;
import kommet.basic.UniqueCheck;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.types.SystemTypes;
import kommet.dao.FieldDefinitionException;
import kommet.dao.UniqueCheckFilter;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.UniqueCheckService;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.BlobDataType;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.FormulaReturnType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class UniqueCheckTest extends BaseUnitTest
{	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService typeService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	UniqueCheckService uniqueCheckService;
	
	@Inject
	AppConfig appConfig;
	
	@Test
	public void testSaveAndFindUniqueCheck() throws KommetException, InterruptedException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);

		for (Type type : env.getAllTypes())
		{	
			// make sure unique checks are initialized on types
			assertNotNull("No unique checks on type " + type.getQualifiedName(), type.getUniqueChecks());
			assertTrue("No unique checks on type " + type.getQualifiedName(), !type.getUniqueChecks().isEmpty());
		}
		
		// create/clear KTL dir - most tests create types, hence they will need
		// a KTL dir to store views that are created for these types
		File ktlDir = new File(env.getKeetleDir(appConfig.getKeetleDir()));
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		ktlDir.mkdir();
		
		List<Record> initialUniqueCheckList = env.getSelectCriteriaFromDAL("select typeId, isSystem from " + SystemTypes.UNIQUE_CHECK_API_NAME).list();
		int initialUniqueChecks = initialUniqueCheckList.size();
		
		// there should be one unique check for each type, plus:
		// - two additional unique checks for file revision
		// - one for type info
		// - one for view (name field)
		// - one for action (URL field)
		// - one for type trigger
		// - one for document template
		// - two for scheduled task
		// - one for text label
		// - one for validation rule
		// - one for email
		// - one for report type (name field)
		// - one for user group type (name field)
		// - two for user group assignment
		// - one for user name
		// - one for profile name
		// - one for web resource name
		// - two for view resource name and path
		// - one for app name
		// - two for app url
		// - one for deployment package name
		// - one for unique check itself
		// - one for event guest
		// - one for label text
		// - one for label assignment
		// - two for sharing rule
		// - one for business process
		// - two for business process action
		// - one for business process output
		// - one for business process input
		// - one for business process param assignment
		assertTrue("Expected the number of unique checks to be at least thirty seven more than the number of types, but there are " + initialUniqueCheckList.size(), env.getAllTypes().size() + 37 < initialUniqueCheckList.size());
		
		// make sure all system unique checks have the isSystem flag set to true
		for (Record uc : initialUniqueCheckList)
		{
			assertTrue(Boolean.TRUE.equals(uc.getField("isSystem")));
		}
		
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = typeService.createType(pigeonType, env);
		
		assertEquals(1, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		assertEquals("A newly created type should have exactly one unique check - on the ID column", initialUniqueChecks + 1, env.getSelectCriteriaFromDAL("select typeId from " + SystemTypes.UNIQUE_CHECK_API_NAME).list().size());
		
		// create a unique check
		UniqueCheck check = new UniqueCheck();
		check.setName("SomeCheck");
		check.setDbName("some_check");
		check.setTypeId(pigeonType.getKID());
		check.setIsSystem(false);
		check.setFieldIds(pigeonType.getField("id").getKID().getId());
		
		try
		{
			check = uniqueCheckService.save(check, dataHelper.getRootAuthData(env), env);
			fail("Saving duplicate unique check for the same field should fail");
		}
		catch (KommetException e)
		{
			assertTrue(e.getMessage().startsWith("Duplicate unique check for the given field set"));
		}
		
		check.setFieldIds(pigeonType.getField("birthdate").getKID().getId());
		Thread.sleep(1000);
		check = uniqueCheckService.save(check, dataHelper.getRootAuthData(env), env);
		
		// try creating another check with the same name
		UniqueCheck duplicateNameCheck = new UniqueCheck();
		duplicateNameCheck.setName("SomeCheck");
		duplicateNameCheck.setDbName("some_check_test");
		duplicateNameCheck.setTypeId(pigeonType.getKID());
		duplicateNameCheck.setIsSystem(false);
		duplicateNameCheck.setFieldIds(pigeonType.getField(Field.CREATEDBY_FIELD_NAME).getKID().getId());
		
		try
		{
			uniqueCheckService.save(duplicateNameCheck, dataHelper.getRootAuthData(env), env);
			fail("Saving two unique checks with the same name should fail");
		}
		catch (KommetException e)
		{
			assertTrue(e.getMessage().startsWith("Unique check violation"));
		}
		
		assertNotNull(check.getId());
		assertNotNull(uniqueCheckService.getByName(check.getName(), typeService, dataHelper.getRootAuthData(env), env));
		assertEquals(pigeonType.getKID(), check.getTypeId());
		assertEquals(2, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		// insert another check for the same type
		UniqueCheck anotherCheck = new UniqueCheck();
		anotherCheck.setName("Some Check2");
		anotherCheck.setTypeId(pigeonType.getKID());
		anotherCheck.setIsSystem(false);
		anotherCheck.addField(pigeonType.getField("createdDate"));
		anotherCheck.addField(pigeonType.getField("age"));
		
		try
		{
			check = uniqueCheckService.save(anotherCheck, dataHelper.getRootAuthData(env), env);
			fail("Saving a unique check with invalid name should fail");
		}
		catch (KommetException e)
		{
			assertTrue(e.getMessage().startsWith("Invalid unique check name"));
		}
		
		anotherCheck.setName("SomeCheck2");
		Thread.sleep(1000);
		anotherCheck = uniqueCheckService.save(anotherCheck, dataHelper.getRootAuthData(env), env);
		assertEquals(3, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		// insert another check for a different type
		UniqueCheck differentCheck = new UniqueCheck();
		differentCheck.setName("SomeCheck3");
		differentCheck.setIsSystem(false);
		
		Type ucType = env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.UNIQUE_CHECK_API_NAME));
		
		differentCheck.setTypeId(ucType.getKID());
		differentCheck.setFieldIds(ucType.getField("createdDate").getKID().getId());
		Thread.sleep(1000);
		uniqueCheckService.save(differentCheck, dataHelper.getRootAuthData(env), env);
		
		assertEquals(3, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		// find checks for the field createdDate on the first object
		List<UniqueCheck> checksForCreatedDate = uniqueCheckService.findForField(pigeonType.getField("createdDate"), env, typeService);
		assertEquals(1, checksForCreatedDate.size());
		assertEquals(anotherCheck.getId(), checksForCreatedDate.get(0).getId());
		
		// find checks for this type
		UniqueCheckFilter filter = new UniqueCheckFilter();
		filter.addTypeId(pigeonType.getKID());
		
		// select using DAL
		List<Record> checkRecords = env.getSelectCriteriaFromDAL("select typeId, fieldIds, accessType from " + SystemTypes.UNIQUE_CHECK_API_NAME + " WHERE typeId IN ('" + pigeonType.getKID() + "')").list();
		assertEquals(3, checkRecords.size());
		assertEquals(pigeonType.getKID(), checkRecords.get(0).getField("typeId"));
		assertEquals(pigeonType.getKID(), checkRecords.get(1).getField("typeId"));
		
		// make sure unique checks have appropriate access type
		for (Record uc : checkRecords)
		{
			if (uc.getField("fieldIds").equals(pigeonType.getField(Field.ID_FIELD_NAME).getKID().getId()))
			{
				// if it is a unique check on the ID column, it has been added by the system
				assertEquals(RecordAccessType.SYSTEM.getId(), uc.getField(Field.ACCESS_TYPE_FIELD_NAME));
			}
			else
			{
				assertEquals(RecordAccessType.PUBLIC.getId(), uc.getField(Field.ACCESS_TYPE_FIELD_NAME));
			}
		}
		
		// select using DAO
		List<UniqueCheck> checks = uniqueCheckService.find(filter, env, typeService);
		assertEquals(3, checks.size());
		assertEquals(pigeonType.getKID(), checks.get(0).getTypeId());
		assertEquals(pigeonType.getKID(), checks.get(1).getTypeId());
		
		assertEquals(3, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		// now get all types from the env
		List<Type> typesWithChecks = typeService.getTypes(true, true, env);
		
		boolean objectRetrieved = false;
		
		for (Type typeWithCheck : typesWithChecks)
		{
			if (typeWithCheck.getKID().equals(pigeonType.getKID()))
			{
				objectRetrieved = true;
				assertTrue(!typeWithCheck.getUniqueChecks().isEmpty());
				assertEquals(3, typeWithCheck.getUniqueChecks().size());
			}
		}
		
		assertTrue("Not all types fetched from the env", objectRetrieved);
		
		Integer uniqueChecksCount = uniqueCheckService.find(null, env, typeService).size();
		
		// now create a check for field name
		Field nameField = pigeonType.getField("name");
		assertNotNull(nameField);
		UniqueCheck nameCheck = new UniqueCheck();
		nameCheck.setName("NameCheck");
		nameCheck.setIsSystem(false);
		nameCheck.setTypeId(pigeonType.getKID());
		nameCheck.setFieldIds(nameField.getKID().getId());
		
		uniqueCheckService.save(nameCheck, typeService.getRootAuthData(env), env);
		
		assertEquals(uniqueChecksCount + 1, uniqueCheckService.find(null, env, typeService).size());
		assertEquals(4, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		// make sure you cannot create two records with the same name
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Mike");
		pigeon1.setField("age", 21);
		typeService.save(pigeon1, env);
		assertNotNull(pigeon1.attemptGetKID());
		
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Mike");
		pigeon2.setField("age", 11);
		
		// now delete the unique constraint on the name field
		uniqueCheckService.deleteForField(nameField, env, typeService);
		typeService.save(pigeon2, env);
		assertNotNull(pigeon2.attemptGetKID());
		
		assertEquals(uniqueChecksCount, (Integer)uniqueCheckService.find(null, env, typeService).size());
		assertEquals("Type unique check list not updated after unique check has been removed", 3, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		// delete the record that violated the unique check so that the check can be created again
		typeService.deleteRecord(pigeon2, env);
		
		// now create the same check again
		nameCheck.uninitializeId();
		nameCheck = uniqueCheckService.save(nameCheck, typeService.getRootAuthData(env), env);
		assertNotNull(nameCheck.getId());
		assertEquals(uniqueChecksCount + 1, uniqueCheckService.find(null, env, typeService).size());
		assertEquals("Type unique check list not updated after unique check has been readded", 4, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		uniqueCheckService.delete(nameCheck.getId(), typeService.getRootAuthData(env), env);
		pigeon2.setKID(null);
		typeService.save(pigeon2, env);
		assertNotNull(pigeon2.attemptGetKID());
		assertEquals(uniqueChecksCount, (Integer)uniqueCheckService.find(null, env, typeService).size());
		assertEquals("Type unique check list not updated after unique check has been removed", 3, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		testUpdateUniqueCheck(pigeonType, env);
		testInvalidDataTypeInUniqueCheck(pigeonType, typeService.getRootAuthData(env), env);
	}
	
	/**
	 * Make sure unique checks cannot be created for certain data types.
	 * @param tyoe
	 * @param authData
	 * @param env
	 * @throws KommetException 
	 */
	private void testInvalidDataTypeInUniqueCheck(Type type, AuthData authData, EnvData env) throws KommetException
	{
		List<Field> invalidFields = new ArrayList<Field>();
		
		// test inverse collection field
		Field childrenField = new Field();
		childrenField.setApiName("children");
		childrenField.setDataType(new InverseCollectionDataType(type, "father"));
		childrenField.setLabel("Children");
		type.addField(childrenField);
		childrenField = typeService.createField(childrenField, env);
		invalidFields.add(childrenField);
		
		// test formula field
		Field nameFormulaField = new Field();
		nameFormulaField.setApiName("nameFormula");
		nameFormulaField.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "name + \"test\"", type, env));
		nameFormulaField.setLabel("Name Formula");
		type.addField(nameFormulaField);
		nameFormulaField = typeService.createField(nameFormulaField, env);
		invalidFields.add(nameFormulaField);
		
		// test formula field
		Field blobField = new Field();
		blobField.setApiName("blobField");
		blobField.setDataType(new BlobDataType());
		blobField.setLabel("Blob Field");
		type.addField(blobField);
		blobField = typeService.createField(blobField, env);
		invalidFields.add(blobField);
		
		// test multi-enum field
		Field multiEnumField = new Field();
		multiEnumField.setApiName("meField");
		multiEnumField.setDataType(new MultiEnumerationDataType(MiscUtils.toSet("One", "Two")));
		multiEnumField.setLabel("ME Field");
		type.addField(multiEnumField);
		multiEnumField = typeService.createField(multiEnumField, env);
		invalidFields.add(multiEnumField);
		
		// test association field
		Type addressType = typeService.createType(dataHelper.getAddressType(env), authData, env);
		
		// create linking type
		Type linkingType = new Type();
		linkingType.setApiName("PigeonAddress");
		linkingType.setPackage("com.test");
		linkingType.setLabel("Pigeon Address");
		linkingType.setPluralLabel("Pigeon Addresses");
		linkingType.setBasic(false);
		linkingType = typeService.createType(linkingType, env);
		
		Field pigeonField = new Field();
		pigeonField.setApiName("pigeon");
		pigeonField.setDataType(new TypeReference(type));
		pigeonField.setLabel("Pigeon");
		linkingType.addField(pigeonField);
		pigeonField = typeService.createField(pigeonField, env);
		
		Field addressField = new Field();
		addressField.setApiName("address");
		addressField.setDataType(new TypeReference(addressType));
		addressField.setLabel("address");
		linkingType.addField(addressField);
		addressField = typeService.createField(addressField, env);
		
		Field assocField = new Field();
		assocField.setApiName("addresses");
		assocField.setDataType(new AssociationDataType(linkingType, addressType, "pigeon", "address"));
		assocField.setLabel("Addresses");
		type.addField(assocField);
		assocField = typeService.createField(assocField, env);
		invalidFields.add(assocField);
		
		for (Field invalidField : invalidFields)
		{
			UniqueCheck uc = new UniqueCheck();
			uc.setTypeId(type.getKID());
			uc.setIsSystem(false);
			uc.setName("test.check.ABC");
			uc.addField(invalidField);
			
			try
			{
				uniqueCheckService.save(uc, authData, env);
				fail("Saving unique check for invalid field data type " + invalidField.getDataType().getName() + " should fail");
			}
			catch (FieldDefinitionException e)
			{
				assertEquals("Invalid field data types in unique check: " + invalidField.getApiName() + " (" + invalidField.getDataType().getName() + ")", e.getMessage());
			}
		}
	}

	/**
	 * This method makes sure that a unique check can be updated by changin its fields, and it is properly handled at the db constraint level.
	 * @param pigeonType
	 * @param env
	 * @throws KIDException
	 * @throws KommetException
	 */
	private void testUpdateUniqueCheck(Type pigeonType, EnvData env) throws KIDException, KommetException
	{
		AuthData authData = typeService.getRootAuthData(env);
		
		Field descField = new Field();
		descField.setApiName("description");
		descField.setLabel("Desc");
		descField.setDataType(new TextDataType(30));
		pigeonType.addField(descField);
		descField = typeService.createField(descField, env);
		
		Field typeField = new Field();
		typeField.setApiName("type");
		typeField.setLabel("Type");
		typeField.setDataType(new TextDataType(30));
		pigeonType.addField(typeField);
		typeField = typeService.createField(typeField, env);
		
		UniqueCheck typeCheck = new UniqueCheck();
		typeCheck.setName("TypeCheck");
		typeCheck.setIsSystem(false);
		typeCheck.setTypeId(pigeonType.getKID());
		typeCheck.setFieldIds(typeField.getKID().getId());
		uniqueCheckService.save(typeCheck, authData, env);
		
		// update the check by changing its field
		typeCheck.clearFields();
		typeCheck.addField(descField);
		uniqueCheckService.save(typeCheck, authData, env);
		
		// create two pigeons with the same description
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Marus");
		pigeon1.setField("age", 11);
		pigeon1.setField("type", "type1");
		pigeon1.setField("description", "test");
		pigeon1 = typeService.save(pigeon1, env);
		assertNotNull(pigeon1.getKID());
		
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Marus");
		pigeon2.setField("age", 12);
		pigeon2.setField("type", "type1");
		pigeon2.setField("description", "test");
		
		try
		{
			pigeon2 = typeService.save(pigeon2, env);
			fail("Inserting two records with the same description should fail");
		}
		catch (UniqueCheckViolationException e)
		{
			
		}
		
		pigeon2.setField("description", "test1");
		pigeon2 = typeService.save(pigeon2, env);
		assertNotNull(pigeon2.attemptGetKID());
	}

	@Test
	public void testUniqueCheckViolation() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		// create/clear KTL dir - most tests create types, hence they will need
		// a KTL dir to store views that are created for these types
		File ktlDir = new File(env.getKeetleDir(appConfig.getKeetleDir()));
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		
		ktlDir.mkdir();
		
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = typeService.createType(pigeonType, typeService.getRootAuthData(env), env);
		
		// add a unique check on field age
		UniqueCheck check = new UniqueCheck();
		check.setTypeId(pigeonType.getKID());
		check.setFieldIds(pigeonType.getField("age").getKID().getId());
		check.setName("Test");
		check.setIsSystem(false);
		
		assertEquals("At this point type should only contain one unique check - for the ID field", 1, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		uniqueCheckService.save(check, typeService.getRootAuthData(env), env);
		
		assertEquals("At this point type should contain two unique checks - for the ID field and for the age field", 2, env.getType(pigeonType.getKeyPrefix()).getUniqueChecks().size());
		
		// now try adding two pigeons with the same age
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Marus");
		pigeon1.setField("age", 11);
		
		pigeon1 = typeService.save(pigeon1, env);
		assertNotNull(pigeon1.getKID());
		
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Marysia");
		pigeon2.setField("age", 11);
		
		try
		{
			typeService.save(pigeon2, env);
			fail("There is a unique check on field age, but saving two objects with the same value of this field succeeded");
		}
		catch (UniqueCheckViolationException e)
		{
			// expected exception
			assertTrue("Invalid error message: " + e.getMessage(), e.getMessage().startsWith("Unique check violation"));
			assertNotNull(e.getUniqueCheck());
			assertFalse(e.getUniqueCheck().getParsedFieldIds().isEmpty());
			assertEquals(1, e.getUniqueCheck().getParsedFieldIds().size());
			assertEquals(pigeonType.getField("age").getKID(), e.getUniqueCheck().getParsedFieldIds().get(0));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail("Unique check violation exception should have been thrown, but instead caught exception: " + e.getClass().getName() + ": " + e.getMessage());
		}
	}
}
