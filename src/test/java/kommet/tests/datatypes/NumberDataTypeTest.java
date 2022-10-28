/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.datatypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyClassGenerator;
import kommet.basic.RecordProxyUtil;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.NumberDataType;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class NumberDataTypeTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ClassService classService;
	
	@Test
	public void testDateAndDateTimeFields() throws KommetException, SecurityException, NoSuchMethodException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		// add numeric field with decimal places to pigeon type
		Field priceField = new Field();
		priceField.setApiName("price");
		priceField.setLabel("Price");
		priceField.setDataType(new NumberDataType(2, Double.class));
		priceField.setRequired(false);
		pigeonType.addField(priceField);
		
		pigeonType = dataService.createType(pigeonType, env);
		
		// read numeric field from DB
		Field refetchedPriceField = dataService.getField(env.getType(pigeonType.getKID()).getField("price").getKID(), env);
		assertNotNull(refetchedPriceField);
		assertEquals((Integer)DataType.NUMBER, refetchedPriceField.getDataTypeId());
		assertEquals((Integer)2, ((NumberDataType)refetchedPriceField.getDataType()).getDecimalPlaces());
		
		// create a pigeon object
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("name", "Ziutek");
		oldPigeon.setField("age", BigDecimal.valueOf(11));
		oldPigeon.setField("price", BigDecimal.valueOf(201.74));
		oldPigeon = dataService.save(oldPigeon, env);
		
		// create another pigeon object
		Record youngPigeon = new Record(pigeonType);
		youngPigeon.setField("name", "Zenek");
		youngPigeon.setField("age", 3);
		youngPigeon.setField("price", 22.06);
		youngPigeon = dataService.save(youngPigeon, env);
		
		// create another pigeon object with null price
		Record noPricePigeon = new Record(pigeonType);
		noPricePigeon.setField("name", "Zenek");
		noPricePigeon.setField("age", 3);
		noPricePigeon = dataService.save(noPricePigeon, env);
		
		// now test retrieving by date, and make sure hour, minute and second information is not taken into account
		// while comparing date fields
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, age, price from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + oldPigeon.getKID() + "'").list();
		assertEquals(1, pigeons.size());
		assertEquals(201.74, pigeons.get(0).getField("price"));
		assertEquals(11, pigeons.get(0).getField("age"));
	
		// test searching by number value with "equals" comparison
		pigeons = env.getSelectCriteriaFromDAL("select id, age, price from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where price = 22.06").list();
		assertEquals(1, pigeons.size());
		assertEquals(youngPigeon.getKID(), pigeons.get(0).getKID());
		assertEquals(22.06, pigeons.get(0).getField("price"));
		assertEquals(3, pigeons.get(0).getField("age"));
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		assertEquals("22.06", pigeons.get(0).getFieldStringValue("price", authData.getLocale()));
		
		// test searching by number value with "gt" comparison
		pigeons = env.getSelectCriteriaFromDAL("select id, age, price from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where price > 22.06").list();
		assertEquals(1, pigeons.size());
		assertEquals(oldPigeon.getKID(), pigeons.get(0).getKID());
		
		// test searching by number value with "ge" comparison
		pigeons = env.getSelectCriteriaFromDAL("select id, age, price from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where price >= 22.06").list();
		assertEquals(2, pigeons.size());
		
		// add a big decimal field to the type
		Field bigDecimalField = new Field();
		NumberDataType dt = new NumberDataType(3, BigDecimal.class);
		bigDecimalField.setDataType(dt);
		bigDecimalField.setApiName("bigDecimalField");
		bigDecimalField.setLabel("BDF");
		pigeonType.addField(bigDecimalField);
		bigDecimalField = dataService.createField(bigDecimalField, env);
		assertNotNull(bigDecimalField.getKID());
		
		// create another pigeon object
		Record lastPigeon = new Record(pigeonType);
		lastPigeon.setField("name", "Zenek");
		lastPigeon.setField("age", 3);
		lastPigeon.setField("price", 22.06);
		lastPigeon.setField(bigDecimalField.getApiName(), new BigDecimal(33.122));
		lastPigeon = dataService.save(lastPigeon, env);
		
		testNumberFieldsInObjectProxy(pigeonType, lastPigeon, env);
	}

	private void testNumberFieldsInObjectProxy(Type pigeonType, Record pigeon, EnvData env) throws KommetException, SecurityException, NoSuchMethodException
	{
		// generate object proxy for pigeon type
		RecordProxy pigeonProxy = RecordProxyUtil.generateCustomTypeProxy(pigeon, env, compiler);
		assertNotNull(pigeonProxy);
		assertEquals(pigeon.getKID(), pigeonProxy.getId());
		
		Method priceGetter = pigeonProxy.getClass().getMethod("getPrice");
		assertNotNull(priceGetter);
		assertEquals(appConfig.getDefaultFloatJavaType(), priceGetter.getReturnType().getName());
		
		Method ageGetter = pigeonProxy.getClass().getMethod("getAge");
		assertNotNull(ageGetter);
		assertEquals(Integer.class.getName(), ageGetter.getReturnType().getName());
		
		Method bigDecimalGetter = pigeonProxy.getClass().getMethod("getBigDecimalField");
		assertNotNull(bigDecimalGetter);
		assertEquals(BigDecimal.class.getName(), bigDecimalGetter.getReturnType().getName());
		
		// test querying fields with different java types
		env.getSelectCriteriaFromDAL("select age, bigDecimalField from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME);
		
		Class proxyFile = RecordProxyClassGenerator.getProxyKollClass(pigeonType, true, true, classService, dataHelper.getRootAuthData(env), env);
		assertNotNull(proxyFile);
		assertTrue(proxyFile.getJavaCode().contains(appConfig.getDefaultIntJavaType() + " getAge"));
	}

	private void assertTrue(boolean contains)
	{
		// TODO Auto-generated method stub
		
	}
}
