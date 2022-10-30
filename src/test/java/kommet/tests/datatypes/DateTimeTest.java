/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.datatypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DateDataType;
import kommet.data.datatypes.DateTimeDataType;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class DateTimeTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppConfig config;
	
	@SuppressWarnings("deprecation")
	@Test
	public void testDateAndDateTimeFields() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		// add date/time field to pigeon type
		Field lastSeenField = new Field();
		lastSeenField.setApiName("lastSeen");
		lastSeenField.setLabel("Last Seen");
		lastSeenField.setDataType(new DateTimeDataType());
		lastSeenField.setRequired(true);
		pigeonType.addField(lastSeenField);
		
		// add date field to pigeon type
		Field dobField = new Field();
		dobField.setApiName("dob");
		dobField.setLabel("Date Of Birth");
		dobField.setDataType(new DateDataType());
		dobField.setRequired(true);
		pigeonType.addField(dobField);
		
		pigeonType = dataService.createType(pigeonType, env);
		
		Date lastSeenDate = new Date();
		
		Date dob = new Date(84, 3, 11);
		
		// create a pigeon object
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("name", "Ziutek");
		oldPigeon.setField("age", BigDecimal.valueOf(3));
		oldPigeon.setField("dob", dob);
		oldPigeon.setField("lastSeen", lastSeenDate);
		oldPigeon = dataService.save(oldPigeon, env);
		
		oldPigeon = dataService.getRecords(Arrays.asList(oldPigeon.getKID()), pigeonType, Arrays.asList("id", "createdDate"), dataHelper.getRootAuthData(env), env).get(0);
		
		// now test retrieving by date
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, name, dob, lastSeen from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where dob = '1984-04-11'").list();
		assertEquals(1, pigeons.size());
		assertTrue(lastSeenDate.compareTo((Date)pigeons.get(0).getField("lastSeen")) == 0);
		
		// query by timestamp
		pigeons = env.getSelectCriteriaFromDAL("select id, name, dob, lastSeen from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where dob > " + (dob.getTime() - 100) + " and dob < " + (dob.getTime() + 100)).list();
		assertEquals(1, pigeons.size());
		assertTrue(lastSeenDate.compareTo((Date)pigeons.get(0).getField("lastSeen")) == 0);
		
		pigeons = env.getSelectCriteriaFromDAL("select id, name, dob, lastSeen from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where createdDate = " + ((Date)oldPigeon.getField("createdDate")).getTime()).list();
		assertEquals(1, pigeons.size());
		assertTrue(lastSeenDate.compareTo((Date)pigeons.get(0).getField("lastSeen")) == 0);
		
		// now test retrieving by date, and make sure hour, minute and second information is not taken into account
		// while comparing date fields
		pigeons = env.getSelectCriteriaFromDAL("select id, name, dob, lastSeen from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where dob = '1984-04-11 05:31:17'").list();
		assertEquals(1, pigeons.size());
		
		// test parsing from string
		Date parsedDate = (Date)(new DateTimeDataType()).getJavaValue("2016-10-22 09:32");
		assertEquals(9, parsedDate.getHours());
		
		// test parsing date from long
		Date testDate = new Date(1900, 2, 3);
		parsedDate = (Date)(new DateTimeDataType()).getJavaValue(testDate.getTime());
		assertEquals(3, parsedDate.getDate());
	}
}
