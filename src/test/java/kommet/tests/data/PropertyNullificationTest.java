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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.BasicSetupService;
import kommet.basic.RecordProxy;
import kommet.dao.DaoFacade;
import kommet.dao.TypePersistenceConfig;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.NullifiedRecord;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.EmailDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TextDataType;
import kommet.env.EnvData;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.koll.compiler.KommetCompiler;
import kommet.persistence.CustomTypeRecordProxyDao;
import kommet.services.GlobalSettingsService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class PropertyNullificationTest extends BaseUnitTest
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
	
	/**
	 * Make sure properties of all data types can be nullified using SpecialValue.NULL and only this.
	 * @throws KommetException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testNullifyProperty() throws KommetException, InterruptedException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		
		// add a field of each data type to object
		addFieldToObject(new BooleanDataType(), type, "booleanField");
		addFieldToObject(new DateTimeDataType(), type, "datetimeField");
		addFieldToObject(new EmailDataType(), type, "emailField");
		addFieldToObject(new EnumerationDataType("one"), type, "picklistField");
		addFieldToObject(new MultiEnumerationDataType(MiscUtils.toSet("one")), type, "multienumField");
		addFieldToObject(new NumberDataType(0, Integer.class), type, "numberField");
		addFieldToObject(new TextDataType(20), type, "textField");
		
		type = dataService.createType(type, env);
		
		// create an instance of the type
		Record r = new Record(type);
		r.setField("booleanField", true);
		Date testDate = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime();
		r.setField("datetimeField", testDate);
		r.setField("emailField", "user@kommet.io");
		r.setField("picklistField", "value 1");
		r.setField("numberField", 2);
		r.setField("textField", "test string");
		r.setField("age", 5);
		r.setField("multienumField", MiscUtils.toSet("one", "two"));
		r.setField("name", "Zoe");
		
		// The case we are testing here is that the non-ID field on the nested property (i.e. father.name)
		// is set before the ID field is nullified
		r.setField("father.name", "Gregor", env);
		r.setField("father.id", SpecialValue.NULL, env);
		
		assertNotNull(r.attemptGetField("father"));
		assertTrue(r.attemptGetField("father") instanceof NullifiedRecord);
		
		// The case we are testing here is that the ID field is nullified before the name field.
		r.setField("mother.id", SpecialValue.NULL, env);
		r.setField("mother.name", "Lilla", env);
		
		assertNotNull(r.attemptGetField("mother"));
		assertTrue(r.attemptGetField("mother") instanceof NullifiedRecord);
		
		// need to wait one second because unix systems round up the file.lastModified property to the closest full seconds
		Thread.sleep(1000);
		
		dataService.save(r, env);
		
		// fetch the record and make sure it has the properties assigned
		List<Record> records = env.getSelectCriteriaFromDAL("select id, createdDate, booleanField, datetimeField, emailField, multienumField, picklistField, numberField, textField FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE id = '" + r.getKID() + "'").list();
		assertEquals(1, records.size());
		Record fetchedRecord = records.get(0);
		assertEquals(true, fetchedRecord.getField("booleanField"));
		
		// for some reason there is a difference of a few milliseconds in dates, so we use the modulo comparison
		assertTrue("Dates are different: " + testDate.getTime() + " and " + ((Date)fetchedRecord.getField("datetimeField")).getTime(), (testDate.getTime() / 1000) == (((Date)fetchedRecord.getField("datetimeField")).getTime() / 1000));
		assertEquals("user@kommet.io", fetchedRecord.getField("emailField"));
		assertEquals("value 1", fetchedRecord.getField("picklistField"));
		assertFalse(((Set<?>)fetchedRecord.getField("multienumField")).isEmpty());
		assertEquals("test string", fetchedRecord.getField("textField"));
		
		// now set all fields to null using Java null, and make sure this nullifies them
		r.setField("datetimeField", null);
		r.setField("emailField", null);
		r.setField("picklistField", null);
		r.setField("multienumField", null);
		r.setField("numberField", null);
		r.setField("textField", null);
		assertTrue(r.isSet("numberField"));
		//assertFalse("Assigning Java null value to record field should not be considered as setting value", r.isSet("numberField"));
		dataService.save(r, env);
		
		// fetch the record and make sure it has the properties assigned
		records = env.getSelectCriteriaFromDAL("select id, booleanField, datetimeField, emailField, multienumField, picklistField, numberField, textField FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE id = '" + r.getKID() + "'").list();
		assertEquals(1, records.size());
		fetchedRecord = records.get(0);
		assertEquals(true, fetchedRecord.getField("booleanField"));
		assertEquals("user@kommet.io", fetchedRecord.getField("emailField"));
		
		// for some reason there is a difference of a few milliseconds in dates, so we use the modulo comparison
		assertTrue((testDate.getTime() / 1000) == (((Date)fetchedRecord.getField("datetimeField")).getTime() / 1000));
		assertEquals("value 1", fetchedRecord.getField("picklistField"));
		assertFalse(((Set<?>)fetchedRecord.getField("multienumField")).isEmpty());
		assertEquals("test string", fetchedRecord.getField("textField"));
		
		// now nullify fields using SpecialValue.NULL and make sure this DID nullify them
		r.setField("booleanField", SpecialValue.NULL);
		r.setField("datetimeField", SpecialValue.NULL);
		r.setField("emailField", SpecialValue.NULL);
		r.setField("picklistField", SpecialValue.NULL);
		r.setField("multienumField", SpecialValue.NULL);
		r.setField("numberField", SpecialValue.NULL);
		r.setField("textField", SpecialValue.NULL);
		dataService.save(r, env);
		
		// fetch the object and make sure the properties are null
		records = env.getSelectCriteriaFromDAL("select id, booleanField, datetimeField, emailField, multienumField, picklistField, numberField, textField FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE id = '" + r.getKID() + "'").list();
		assertEquals(1, records.size());
		fetchedRecord = records.get(0);
		assertEquals(null, fetchedRecord.getField("booleanField"));
		assertEquals(null, fetchedRecord.getField("datetimeField"));
		assertEquals(null, fetchedRecord.getField("emailField"));
		assertEquals(null, fetchedRecord.getField("picklistField"));
		assertEquals(null, fetchedRecord.getField("multienumField"));
		assertEquals(null, fetchedRecord.getField("textField"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testNullifyProxyProperty() throws KommetException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		dataService.createType(type, env);
		
		// get type proxy
		Class<? extends RecordProxy> proxyClass = (Class<? extends RecordProxy>) compiler.getClass(type.getQualifiedName(), true, env);
		assertNotNull(proxyClass);
		
		RecordProxy pigeon = (RecordProxy)proxyClass.newInstance();
		assertEquals(pigeon.getClass(), proxyClass);
		
		Method nameSetter = proxyClass.getMethod("setName", String.class);
		assertNotNull(nameSetter);
		nameSetter.invoke(pigeon, "Dzidek");
		
		Method ageSetter = proxyClass.getMethod("setAge", Integer.class);
		assertNotNull(ageSetter);
		ageSetter.invoke(pigeon, 32);
		
		Method colourSetter = proxyClass.getMethod("setColour", String.class);
		assertNotNull(colourSetter);
		colourSetter.invoke(pigeon, "brown");
		
		CustomTypeRecordProxyDao<RecordProxy> pigeonDao = (CustomTypeRecordProxyDao<RecordProxy>) env.getCustomProxyDao(proxyClass, envPersistence);
		//ParameterizedType parameterizedType = (ParameterizedType)getClass().getGenericSuperclass();
	    //Class genericType = (Class)parameterizedType.getActualTypeArguments()[0];
	    //assertEquals(proxyClass, genericType);
		assertNotNull(pigeonDao);
		
		assertNull(pigeon.getId());
		pigeonDao.save(pigeon, dataHelper.getRootAuthData(env), env);
		assertNotNull(pigeon.getId());
		
		Method colourGetter = proxyClass.getMethod("getColour");
		assertNotNull(colourGetter);
		
		RecordProxy updatedPigeon = pigeonDao.get(pigeon.getId(), env);
		assertEquals(proxyClass, updatedPigeon.getClass());
		assertEquals(pigeon.getClass().getName(), updatedPigeon.getClass().getName());
		assertEquals(pigeon.getClass(), updatedPigeon.getClass());
		assertNotNull(updatedPigeon);
		assertNotNull(colourGetter.invoke(updatedPigeon));
		
		assertEquals("brown", (String)colourGetter.invoke(updatedPigeon));
		
		// nullify the colour property
		colourSetter.invoke(pigeon, (Object)null);
		pigeonDao.save(pigeon, dataHelper.getRootAuthData(env), env);
		
		// refetch pigeon
		updatedPigeon = pigeonDao.get(pigeon.getId(), env);
		assertNotNull(updatedPigeon);
		Object colourValue = colourGetter.invoke(updatedPigeon);
		assertNull(colourValue);
	}
	
	private void addFieldToObject(DataType dataType, Type obj, String fieldName) throws KommetException
	{
		Field field = new Field();
		field.setApiName(fieldName);
		field.setLabel(fieldName);
		field.setRequired(false);
		field.setDataType(dataType);
		obj.addField(field);
	}
}
