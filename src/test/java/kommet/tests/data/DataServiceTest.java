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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.types.SystemTypes;
import kommet.dao.DaoFacade;
import kommet.dao.KommetPersistenceException;
import kommet.dao.TypePersistenceConfig;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.JoinType;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SelectQuery;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.GlobalSettings;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeFilter;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.GlobalSettingsService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class DataServiceTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DaoFacade daoFacade;
	
	@Inject
	DataService dataService;
	
	@Inject
	TypePersistenceConfig persistence;
	
	@Inject
	GlobalSettingsService settingService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Inject
	KommetCompiler compiler;
	
	//private static final Logger log = LoggerFactory.getLogger(UpdateQuery.class);
	
	private void testIllegalPackageName(EnvData env) throws KommetException
	{
		Type type = dataHelper.getPigeonType(env);
		
		try
		{
			type.setPackage("test.package.one");
			fail("Setting package name containing illegal string 'package' should fail");
		}
		catch (KommetException e)
		{
			// expected
		}
	}
	
	@Test
	public void createType() throws KIDException, KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		Long kidSeq = settingService.getSettingAsLong(GlobalSettings.TYPE_SEQ, env);
		
		testIllegalPackageName(env);
		
		Type pigeonType = dataHelper.getPigeonType(env);
		assertFalse(pigeonType.isPersisted());
		
		// make sure the object has no fields before creation
		assertTrue(pigeonType.getFields().isEmpty());
		
		// create type
		pigeonType = dataService.createType (pigeonType, env);
		
		Long updatedRidSeq = settingService.getSettingAsLong(GlobalSettings.TYPE_SEQ, env);
		assertEquals((Long)(kidSeq + 1), updatedRidSeq);
		assertNotNull(pigeonType.getCreated());
		assertNotNull(pigeonType.getKeyPrefix());
		assertNotNull(pigeonType.getKID());
		assertNotNull(pigeonType.getApiName());
		assertTrue(pigeonType.isPersisted());
		assertNotNull(pigeonType.getId());
		
		// make sure an ID field has been automatically added to the type
		Field idField = pigeonType.getField(Field.ID_FIELD_NAME);
		assertNotNull(idField);
		assertNotNull(idField.getKID());
		assertNotNull(idField.getId());
		
		// make sure a table has been created for the type
		try
		{
			@SuppressWarnings("deprecation")
			Integer objCount = env.getJdbcTemplate().queryForObject("SELECT COUNT(id) FROM " + pigeonType.getDbTable(), Integer.class);
			assertEquals(Integer.valueOf(0), objCount);
		}
		catch (Exception e)
		{
			fail("Querying newly created table '" + pigeonType.getDbTable() + "' failed. Probably table not created for new type");
		}
		
		// make sure a DB column for the ID field has been created
		try
		{
			env.getJdbcTemplate().execute("SELECT " + Field.ID_FIELD_DB_COLUMN + ", " + Field.CREATEDDATE_FIELD_DB_COLUMN + ", " + Field.CREATEDBY_FIELD_DB_COLUMN + " FROM " + pigeonType.getDbTable() + " LIMIT 1");
		}
		catch (Exception e)
		{
			fail("Retrieving one of the required columns from the newly created table '" + pigeonType.getDbTable() + "' failed. Probably DB column not added. Nested: " + e.getMessage());
		}
		
		// now try to insert the object again and make sure it fails
		try
		{
			dataService.createType (dataHelper.getPigeonType(env), env);
			fail("Inserting two types with the same API name succeeded, which is an error");
		}
		catch (KommetException e)
		{
			// error expected
			assertEquals("Type with name " + dataHelper.getPigeonType(env).getQualifiedName() + " already exists", e.getMessage());
		}
	}
	
	@Test
	public void testReadInObjectFromDb() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType (pigeonType, env);
		
		// read object configuration from DB and make sure it is the identical to the original one
		Type refetchedType = dataService.getByKID(pigeonType.getKID(), env);
		
		assertEquals(pigeonType.getKID(), refetchedType.getKID());
		assertEquals(pigeonType.getApiName(), refetchedType.getApiName());
		assertEquals(pigeonType.getLabel(), refetchedType.getLabel());
		assertEquals(pigeonType.getPluralLabel(), refetchedType.getPluralLabel());
		assertEquals(pigeonType.getDbTable(), refetchedType.getDbTable());
		assertEquals(pigeonType.getKeyPrefix(), refetchedType.getKeyPrefix());
		assertEquals(pigeonType.getPackage(), refetchedType.getPackage());
		assertEquals(pigeonType.getFields().size(), refetchedType.getFields().size());
		
		for (Field field : pigeonType.getFields())
		{
			Field refetchedField = refetchedType.getField(field.getApiName());
			assertEquals(field.getDataType().getId(), refetchedField.getDataType().getId());
			assertEquals(field.getDbColumn(), refetchedField.getDbColumn());
			assertEquals(field.getKID(), refetchedField.getKID());
			assertEquals(field.getLabel(), refetchedField.getLabel());
		}
	}
	
	@Test
	public void createField() throws KIDException, KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		// create type
		Type pigeonType = dataService.createType (dataHelper.getPigeonType(env), env);
		
		Field nameField = dataHelper.getNameField();
		
		try
		{
			dataService.createField(nameField, env);
			fail("Creating field should have failed because the field has no type assigned");
		}
		catch (KommetPersistenceException e)
		{
			// exception expected
			assertTrue(e.getMessage().startsWith("Type not set"));
		}
		
		nameField.setType(pigeonType);
		nameField = dataService.createField(nameField, env);
		
		assertNotNull(nameField.getKID());
		assertNotNull(nameField.getId());
		assertNotNull(nameField.getCreated());
		assertTrue(nameField.getKID().toString().startsWith(KID.FIELD_PREFIX));
		
		// get field by ID
		Field refetchedField = dataService.getField(nameField.getKID(), env);
		assertNotNull("Field not retrieved by ID", refetchedField);
		assertEquals(nameField.getApiName(), refetchedField.getApiName());
		
		// add a reference field to pigeon
		Field field = new Field();
		field.setApiName("father");
		field.setLabel("Father");
		// the other referenced object is also a pigeon
		field.setDataType(new TypeReference(pigeonType));
		field.setType(pigeonType);
		
		field = dataService.createField(field, env);
		
		// test getting type reference field
		Field fetchedField = dataService.getField(field.getKID(), env);
		assertNotNull(((TypeReference)fetchedField.getDataType()).getType());
		
		// add inverse collection field to type pigeon
		Field childrenField = new Field();
		childrenField.setApiName("children");
		childrenField.setLabel("Children");
		// the other referenced object is also a pigeon
		childrenField.setDataType(new InverseCollectionDataType(pigeonType, "father"));
		childrenField.setType(pigeonType);
		childrenField = dataService.createField(childrenField, env);
		
		fetchedField = dataService.getField(childrenField.getKID(), env);
		assertNotNull(((InverseCollectionDataType)fetchedField.getDataType()).getInverseType());
	}
	
	@Test
	public void getTypeById() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Type type = dataHelper.getPigeonType(env);
		type = dataService.createType(type, env);
		
		// get by KID
		Type fetchedType = dataService.getType(type.getKID(), env);
		assertNotNull(fetchedType);
		assertEquals(type.getId(), fetchedType.getId());
		
		// get by DB ID
		fetchedType = dataService.getById(type.getId(), env);
		assertNotNull(fetchedType);
		assertEquals(type.getId(), fetchedType.getId());
		assertEquals(type.getKID(), fetchedType.getKID());
	}
	
	@Test
	public void createTypeWithMultipleFields() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Type pigeonType = dataHelper.getPigeonType(env);
		
		Field nameField = dataHelper.getNameField();
		Field ageField = dataHelper.getAgeField();
		pigeonType.addField(nameField);
		pigeonType.addField(ageField);
		
		// save object
		pigeonType = dataService.createType(pigeonType, env);
		
		// test querying the object table
		try
		{
			env.getJdbcTemplate().execute("SELECT " + Field.ID_FIELD_DB_COLUMN + ", " + nameField.getDbColumn() + ", " + ageField.getDbColumn() + " FROM " + pigeonType.getDbTable() + " LIMIT 1");
		}
		catch (Exception e)
		{
			fail("Querying object with multiple fields failed. Nested: " + e.getMessage());
		}
		
		// now add another field
		Field emailField = new Field();
		emailField.setApiName("user_Email");
		emailField.setLabel("Email");
		emailField.setDataType(new TextDataType(30));
		pigeonType.addField(emailField);
		
		dataService.createField(emailField, env);
		
		try
		{
			env.getJdbcTemplate().execute("SELECT " + emailField.getDbColumn() + " FROM " + pigeonType.getDbTable() + " LIMIT 1");
		}
		catch (Exception e)
		{
			fail("Querying object with field added after object creation failed. Nested: " + e.getMessage());
		}
		
		// make sure the object's metadata in the env has been updated with the new field
		assertNotNull("Object's state in env not updated with a newly added field", env.getType(pigeonType.getKID()).getField("user_Email"));
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// test recompiling standard controller for this type
		List<Type> types = MiscUtils.toList(pigeonType);
		dataService.createStandardControllers(false, types, authData, env);
		
		// test recompiling them again with generating code anew
		dataService.createStandardControllers(true, types, authData, env);
	}
	
	@Test
	public void testInsertTypeRecord() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		type = dataService.createType(type, env);
		
		// test finding type by qualified name
		TypeFilter filter = new TypeFilter();
		filter.setQualifiedName(type.getQualifiedName());
		assertEquals(1, dataService.getTypes(filter, false, false, env).size());
		assertNotNull(dataService.getTypeByName(type.getQualifiedName(), false, env));
		
		assertNotNull("Raimme Admin is null", env.getRootUser());
		assertNotNull("Raimme Admin has null KID", appConfig.getRootUserId());
		
		// get the next KID sequence for object of type "pigeon"
		Long kidSequence = env.getJdbcTemplate().queryForObject("select nextval('" + type.getKIDSeqName() + "'::regclass)", Long.class);
		
		// make sure that just after the object has been created, its KID sequence has values 1
		assertEquals(Long.valueOf(1), kidSequence);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(type.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		
		try
		{
			// test required field validation
			dataService.save(oldPigeon, env);
			fail("Field 'age' is required but has been left empty - save should not succeed");
		}
		catch (FieldValidationException e)
		{
			// exception expected
			
			// make sure that after a failed save, the sequence on the type
			// has increased
			assertEquals((Long)(kidSequence + 1), env.getJdbcTemplate().queryForObject("select nextval('" + type.getKIDSeqName() + "'::regclass)", Long.class));
		}
		
		oldPigeon.setField("age", 8);
		dataService.save(oldPigeon, env);
		
		Long seqAfterFirstInsert = env.getJdbcTemplate().queryForObject("select nextval('" + type.getKIDSeqName() + "'::regclass)", Long.class);
		
		// make sure that after creating a new pigeon, the sequence on the type has increased by 1
		assertEquals((Long)(kidSequence + 3), seqAfterFirstInsert);
		
		// insert another pigeon
		Record youngPigeon = dataService.instantiate(type.getKID(), env);
		youngPigeon.setField("name", "Bronek");
		youngPigeon.setField("age", 3);
		dataService.save(youngPigeon, env);
		
		Long seqAfterSecondInsert = env.getJdbcTemplate().queryForObject("select nextval('" + type.getKIDSeqName() + "'::regclass)", Long.class);
		
		// make sure that after creating a new pigeon, the sequence on the type has increased by 1
		assertEquals((Long)(seqAfterFirstInsert + 2), seqAfterSecondInsert);
		
		
		// make sure the object has been inserted into the pigeons table
		try
		{
			Long pigeonCount = env.getJdbcTemplate().queryForObject("SELECT COUNT(id) FROM " + type.getDbTable(), Long.class);
			assertEquals(Long.valueOf(2), pigeonCount);
		}
		catch (Exception e)
		{
			fail("Querying object failed. Nested: " + e.getMessage());
		}
	}
	
	@Test
	public void testSelectDalQuery() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type obj = dataHelper.getFullPigeonType(env);
		obj = dataService.createType(obj, env);
		
		String nameWithSingleQuote = "Bro'nek";
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(obj.getKID(), env);
		oldPigeon.setField("name", nameWithSingleQuote);
		oldPigeon.setField("age", 8);
		
		dataService.save(oldPigeon, env);
		
		// now query the object
		List<Record> pigeons = dataService.select("SELECT id, name, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME, env);// WHERE Id = '" + oldPigeon.getKID() + "' AND Age > 5");
		assertNotNull(pigeons);
		assertEquals(1, pigeons.size());
		assertEquals(oldPigeon.getKID(), pigeons.get(0).getKID());
		assertNotNull(oldPigeon.getField("age"));
		assertEquals(oldPigeon.getField("age"), pigeons.get(0).getField("age"));
		assertEquals(nameWithSingleQuote, pigeons.get(0).getField("name"));
	}
	
	@Test
	public void testUpdateObjectReferenceField() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type obj = dataHelper.getFullPigeonType(env);
		
		// add owner field
		Field ownerField = new Field();
		ownerField.setApiName("owner");
		ownerField.setLabel("Owner");
		ownerField.setDataType(new TypeReference(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_API_NAME))));
		ownerField.setRequired(false);
		obj.addField(ownerField);
		
		obj = dataService.createType(obj, env);
		
		// create instance of pigeon object
		Record oldPigeon = new Record(obj);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		
		dataService.save(oldPigeon, env);
		assertNotNull(oldPigeon.getKID());
		
		// now set the owner for the pigeon
		oldPigeon.setField("owner", env.getRootUser());
		dataService.save(oldPigeon, env);
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, owner.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE id = '" + oldPigeon.getKID() + "'").list();
		assertEquals(1, pigeons.size());
		assertNotNull(pigeons.get(0).getField("owner"));
		assertEquals(env.getRootUser().getKID(), ((Record)pigeons.get(0).getField("owner")).getKID());
	}
	
	/**
	 * Tests that nested properties of different type than the parent object can be set even when they are nested
	 * two levels down.
	 * This test was added because this feature failed at some point.
	 * 
	 * @param oldPigeon
	 * @throws KommetException 
	 */
	@Test
	public void testNestedPropertyOfDifferentType() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Type addressType = dataHelper.getAddressType(env);
		addressType.setPackage(addressType.getPackage());
		addressType = dataService.createType(addressType, env);
		
		Type farmType = dataHelper.getFarmType(env);
		farmType.setPackage(farmType.getPackage());
		
		// add reference to address
		Field addressField = new Field();
		addressField.setApiName("address");
		addressField.setLabel("Address");
		addressField.setDataType(new TypeReference(addressType));
		
		farmType.addField(addressField);
		
		// save farm type
		farmType = dataService.createType(farmType, env);
		
		Type pigeonType = dataHelper.getPigeonType(env);
		
		// add reference to farm
		Field farmField = new Field();
		farmField.setApiName("farm");
		farmField.setLabel("Farm");
		farmField.setDataType(new TypeReference(farmType));
		
		pigeonType.addField(farmField);
		
		pigeonType = dataService.createType(pigeonType, env);
		
		// Create pigeon type
		// It's essential for the test to retrieve the object definition from the env using "env.getType"
		Record pigeon = new Record(env.getType(pigeonType.getKID()));
		pigeon.setField("farm.address.street", "Mragowska", env);
		
		assertEquals("Mragowska", pigeon.getField("farm.address.street"));
	}

	@Test
	public void testUpdateRecord() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		type = dataService.createType(type, env);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(type.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		
		Record youngPigeon = dataService.instantiate(type.getKID(), env);
		youngPigeon.setField("name", "Zenek");
		youngPigeon.setField("age", 2);
		
		dataService.save(oldPigeon, env);
		dataService.save(youngPigeon, env);
		
		assertTrue("Two records have been inserted with identical KID", !oldPigeon.getKID().equals(youngPigeon.getKID()));
		
		// now try to save old pigeon only by ID
		Record pigeonCopy = new Record(type);
		pigeonCopy.setField(Field.ID_FIELD_NAME, oldPigeon.getKID());
		// set only age, but leave name empty
		pigeonCopy.setField("age", 3);
		dataService.save(pigeonCopy, env);
		
		Criteria criteria = env.getSelectCriteria(type.getKID());
		criteria.addProperty("age");
		// now query the object
		List<Record> pigeons = criteria.list();
		assertNotNull(pigeons);
		assertEquals(2, pigeons.size());
		assertNotNull(pigeons.get(0).getField("age"));
		
		// either the first pigeon should have age 2 and the other one 10, or the other way around
		boolean firstPermutation = ((Integer)pigeons.get(0).getField("age")).equals(3) && ((Integer)pigeons.get(1).getField("age")).equals(2);
		boolean secondPermutation = ((Integer)pigeons.get(1).getField("age")).equals(3) && ((Integer)pigeons.get(0).getField("age")).equals(2);
		
		assertTrue(firstPermutation || secondPermutation);
		assertNotNull(pigeons.get(0).getField("age"));
		assertNotNull(pigeons.get(1).getField("age"));
		
		// now update the pigeon's age
		oldPigeon.setField("age", 10);
		dataService.save(oldPigeon, env);
		
		Criteria pigeonCriteria = env.getSelectCriteria(type.getKID());
		pigeonCriteria.addProperty("age");
		pigeonCriteria.add(Restriction.gt("age", 1));
		
		// refetch object
		pigeons = pigeonCriteria.list();
		assertNotNull(pigeons);
		assertEquals(2, pigeons.size());
		assertNotNull(pigeons.get(0).getField("age"));
		assertNotNull(pigeons.get(1).getField("age"));
		
		// either the first pigeon should have age 2 and the other one 10, or the other way around
		boolean firstChangedPermutation = ((Integer)pigeons.get(0).getField("age")).equals(10) && ((Integer)pigeons.get(1).getField("age")).equals(2);
		boolean secondChangedPermutation = ((Integer)pigeons.get(1).getField("age")).equals(10) && ((Integer)pigeons.get(0).getField("age")).equals(2);
		assertTrue(firstChangedPermutation || secondChangedPermutation);
		
		// now try fetching the record and updating only one field
		oldPigeon = env.getSelectCriteriaFromDAL("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE " + Field.ID_FIELD_NAME + " = '" + oldPigeon.getKID() + "'").list().get(0);
		assertTrue(oldPigeon.isSet(Field.ID_FIELD_NAME));
		assertFalse(oldPigeon.isSet("name"));
		assertFalse(oldPigeon.isSet("age"));
		oldPigeon.setField("name", "Kajtek");
		dataService.save(oldPigeon, env);
		Record updatedOldPigeon = env.getSelectCriteriaFromDAL("select id, name from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE " + Field.ID_FIELD_NAME + " = '" + oldPigeon.getKID() + "'").list().get(0);
		assertEquals("Kajtek", updatedOldPigeon.getField("name"));
		
		// now test deleting record by ID
		dataService.deleteRecord(oldPigeon.getKID(), null, env);
		assertTrue("Record seems to not be deleted as it should", env.getSelectCriteriaFromDAL("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE id = '" + oldPigeon.getKID() + "'").list().isEmpty());
	}

	@Test
	public void testNestedProperty() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		Field nestedField = type.getField("father.age", env);
		assertNotNull(nestedField);
		assertEquals((Integer)DataType.NUMBER, nestedField.getDataType().getId());
		assertEquals("age", nestedField.getApiName());
	}
	
	@Test
	public void testReferenceField() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		
		type = dataService.createType(type, env);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(type.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		
		dataService.save(oldPigeon, env);
		
		Record youngPigeon = dataService.instantiate(type.getKID(), env);
		youngPigeon.setField("name", "Zenek");
		youngPigeon.setField("age", 2);
		youngPigeon.setField("father", oldPigeon);
		
		dataService.save(youngPigeon, env);
		
		// now find the inserted pigeon with its type reference field set
		Criteria pigeonCriteria = env.getSelectCriteria(type.getKID());
		pigeonCriteria.addProperty("father");
		pigeonCriteria.createAlias("father", "father", JoinType.LEFT_JOIN);
		pigeonCriteria.add(Restriction.eq("father.age", 8));
		pigeonCriteria.addProperty("father.age");
		pigeonCriteria.addProperty("father.name");
		
		List<String> nestedProperties = new ArrayList<String>();
		nestedProperties.add("father.age");
		nestedProperties.add("father.name");
		
		// test SQL built from this criteria, make sure it contains joins for the type references
		String generatedSQL = SelectQuery.buildFromCriteria(pigeonCriteria, nestedProperties, env).getSqlQuery();
		String quote = pigeonCriteria.isQuoteTableAndColumnNames() ? "\"" : "";
		
		Pattern sqlPattern = Pattern.compile(" FROM " + type.getDbTable() + " AS this LEFT JOIN " + type.getDbTable() + " AS " + quote + "father_[0-9]+" + quote + " ON " + quote + "this" + quote + "." + quote + "father" + quote + " = " + quote + "father_[0-9]+" + quote + "." + quote + Field.ID_FIELD_DB_COLUMN + quote);
		
		assertTrue("Incorrect generated SQL: " + generatedSQL, sqlPattern.matcher(MiscUtils.normalizeWhiteSpace(generatedSQL)).find());
		assertTrue("Incorrect generated SQL for select fields: " + generatedSQL, MiscUtils.normalizeWhiteSpace(generatedSQL).contains("father.age"));;
		
		List<Record> pigeons = pigeonCriteria.list();
		
		assertNotNull(pigeons);
		assertEquals(1, pigeons.size());
		assertEquals(youngPigeon.getKID(), ((Record)pigeons.get(0)).getKID());
		assertEquals(oldPigeon.getKID(), ((Record)pigeons.get(0).getField("father")).getKID());
		
		// now try retrieving with greater than operator
		Criteria ageCriteria = env.getSelectCriteria(type.getKID());
		ageCriteria.createAlias("father", "father", JoinType.LEFT_JOIN);
		ageCriteria.add(Restriction.gt("father.age", 7));
		ageCriteria.addProperty("father.age");
		ageCriteria.addProperty("father.id");
		ageCriteria.addProperty("father.name");
		
		pigeons = ageCriteria.list();
		
		assertNotNull(pigeons);
		assertEquals(1, pigeons.size());
		assertEquals(oldPigeon.getKID(), ((Record)pigeons.get(0).getField("father")).getKID());
		assertEquals(oldPigeon.getField("name"), pigeons.get(0).getField("father.name"));
		assertEquals(oldPigeon.getField("age"), pigeons.get(0).getField("father.age"));
		
		testDeleteObjectWithNonRequiredReference(oldPigeon, env);
	}
	
	/**
	 * Test that checks that deleting a record does not delete its child objects (if they have property cascade delete set to false)
	 * @param oldPigeon
	 * @throws KommetException 
	 */
	private void testDeleteObjectWithNonRequiredReference(Record oldPigeon, EnvData env) throws KommetException
	{
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select father.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		int initialCount = pigeons.size();
		
		assertNotNull(oldPigeon.getKID());
		
		dataService.deleteRecord(oldPigeon, env);
		
		// select young pigeons and make sure the object to father is empty
		pigeons = env.getSelectCriteriaFromDAL("select father.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		
		assertEquals(initialCount - 1, pigeons.size());
		
		for (Record pigeon : pigeons)
		{
			assertTrue("Expected reference field to be set to null, actual value = " + pigeon.getField("father.id"), pigeon.getField("father.id") == null);
		}
	}
	
	/**
	 * Test that checks that deleting a record also deletes its child objects (if they have property cascade delete set to false)
	 * @param oldPigeon
	 * @throws KommetException 
	 */
	@Test
	public void testDeleteObjectWithRequiredReference() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type obj = dataHelper.getFullPigeonType(env);
		
		// add required reference field
		Field field = new Field();
		field.setApiName("brother");
		field.setLabel("Brother");
		// the other referenced object is also a pigeon
		TypeReference dt = new TypeReference(obj);
		dt.setCascadeDelete(true);
		field.setDataType(dt);
		obj.addField(field);
		
		obj = dataService.createType(obj, env);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(obj.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		dataService.save(oldPigeon, env);
		
		Record youngPigeon1 = dataService.instantiate(obj.getKID(), env);
		youngPigeon1.setField("name", "Zenek");
		youngPigeon1.setField("age", 2);
		youngPigeon1.setField("brother", oldPigeon);
		dataService.save(youngPigeon1, env);
		
		Record youngPigeon2 = dataService.instantiate(obj.getKID(), env);
		youngPigeon2.setField("name", "Franek");
		youngPigeon2.setField("age", 2);
		dataService.save(youngPigeon2, env);
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		assertEquals(3, pigeons.size());
		
		// delete the old pigeon and make sure the young pigeon is removed as well
		dataService.deleteRecord(oldPigeon, env);
		pigeons = env.getSelectCriteriaFromDAL("select id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		assertEquals(1, pigeons.size());
	}

	@Test
	public void testReferenceFieldFetchingWithEmptyCriteria() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type obj = dataHelper.getFullPigeonType(env);
		obj = dataService.createType(obj, env);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(obj.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		
		dataService.save(oldPigeon, env);
		
		Record youngPigeon = dataService.instantiate(obj.getKID(), env);
		youngPigeon.setField("name", "Zenek");
		youngPigeon.setField("age", 2);
		youngPigeon.setField("father", oldPigeon);
		
		dataService.save(youngPigeon, env);
		
		// create criteria that does not contain any conditions
		// now find the inserted pigeon with its type reference field set
		Criteria pigeonCriteria = env.getSelectCriteria(obj.getKID());
		pigeonCriteria.createAlias("father", "father");
		pigeonCriteria.addProperty("father.age");
		pigeonCriteria.addProperty("father.name");
		
		List<Record> pigeons = pigeonCriteria.list();
		assertEquals(2, pigeons.size());
		
		// make sure the nested property of the father object are set even though
		// there were no conditions on them
		for (Record pigeon : pigeons)
		{
			if (pigeon.getKID().equals(youngPigeon.getKID()))
			{
				assertNotNull("Nested property 'father.age' not retrieved", pigeon.getField("father.age"));
				assertEquals(oldPigeon.getField("age"), pigeon.getField("father.age"));
			}
		}
	}
	
	/**
	 * Test the Restriction.isNull criteria.
	 * @throws KommetException
	 */
	@Test
	public void testIsNullCriteria() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type obj = dataHelper.getFullPigeonType(env);
		obj = dataService.createType(obj, env);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(obj.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		
		dataService.save(oldPigeon, env);
		
		Record youngPigeon = dataService.instantiate(obj.getKID(), env);
		youngPigeon.setField("name", "Zenek");
		youngPigeon.setField("age", 2);
		youngPigeon.setField("father", oldPigeon);
		
		dataService.save(youngPigeon, env);
		
		Criteria c = env.getSelectCriteria(obj.getKID());
		c.add(Restriction.isNull("father"));
		List<Record> pigeons = c.list();
		assertEquals(1, pigeons.size());
		assertEquals(oldPigeon.getKID(), pigeons.get(0).getKID());
	}
	
	/**
	 * Test the Restriction.inot criteria.
	 * @throws KommetException
	 */
	@Test
	public void testNotCriteria() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type obj = dataHelper.getFullPigeonType(env);
		obj = dataService.createType(obj, env);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(obj.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		
		dataService.save(oldPigeon, env);
		
		Record youngPigeon = dataService.instantiate(obj.getKID(), env);
		youngPigeon.setField("name", "Zenek");
		youngPigeon.setField("age", 2);
		youngPigeon.setField("father", oldPigeon);
		
		dataService.save(youngPigeon, env);
		
		Criteria c = env.getSelectCriteria(obj.getKID());
		c.add(Restriction.not(Restriction.eq("name", "Zenek")));
		List<Record> pigeons = c.list();
		assertEquals(1, pigeons.size());
		assertEquals(oldPigeon.getKID(), pigeons.get(0).getKID());
	}
}

