/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.auth.UserService;
import kommet.basic.FieldHistory;
import kommet.basic.FieldHistoryOperation;
import kommet.basic.Profile;
import kommet.basic.User;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DateDataType;
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.env.EnvData;
import kommet.filters.FieldHistoryFilter;
import kommet.services.FieldHistoryService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.tests.harness.CompanyAppDataSet;
import kommet.utils.AppConfig;

public class FieldHistoryTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppConfig config;
	
	@Inject
	FieldHistoryService fieldHistoryService;
	
	@Inject
	UserService userService;
	
	@Inject
	PermissionService permissionService;
	
	@SuppressWarnings("deprecation")
	@Test
	public void testLogFieldHistory() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		// add date/time field to pigeon type
		Field lastSeenField = new Field();
		lastSeenField.setApiName("lastSeen");
		lastSeenField.setLabel("Last Seen");
		lastSeenField.setDataType(new DateTimeDataType());
		lastSeenField.setRequired(false);
		lastSeenField.setTrackHistory(true);
		pigeonType.addField(lastSeenField);
		
		// add date field to pigeon type
		Field dobField = new Field();
		dobField.setApiName("dob");
		dobField.setLabel("Date Of Birth");
		dobField.setDataType(new DateDataType());
		dobField.setRequired(true);
		dobField.setTrackHistory(false);
		pigeonType.addField(dobField);
		
		pigeonType = dataService.createType(pigeonType, env);
		
		assertEquals(0, fieldHistoryService.get(null, env).size());
		
		Date lastSeenDate = new Date(115, 1, 1);
		
		// create some pigeon
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("name", "Ziutek");
		oldPigeon.setField("age", BigDecimal.valueOf(3));
		oldPigeon.setField("dob", new Date(84, 3, 11));
		oldPigeon.setField("lastSeen", lastSeenDate);
		oldPigeon = dataService.save(oldPigeon, env);
		
		List<FieldHistory> fhs = fieldHistoryService.get(null, env);
		assertEquals(1, fhs.size());
		FieldHistory fhEntry = fhs.get(0);
		assertEquals(env.getType(pigeonType.getKeyPrefix()).getField("lastSeen").getKID(), fhEntry.getFieldId());
		assertNull("Expected old value to be null, but was '" + fhEntry.getOldValue() + "'", fhEntry.getOldValue());
		assertNotNull(fhEntry.getNewValue());
		
		// update the lastSeen field on pigeon, but assign the same value to it
		oldPigeon.setField("lastSeen", lastSeenDate);
		oldPigeon = dataService.save(oldPigeon, env);
		
		// make sure that if the actual field value has not changed, it will not be reflected in the field history
		assertEquals(1, fieldHistoryService.get(null, env).size());
		
		// now really update the lastSeen field
		oldPigeon.setField("lastSeen", new Date(114,1,6));
		oldPigeon = dataService.save(oldPigeon, env);
		fhs = fieldHistoryService.get(null, env);
		assertEquals(2, fhs.size());
		
		for (FieldHistory fh : fhs)
		{
			assertEquals(env.getType(pigeonType.getKeyPrefix()).getField("lastSeen").getKID(), fh.getFieldId());
		}
		
		// now try searching by filter only items for this field
		FieldHistoryFilter filter = new FieldHistoryFilter();
		filter.addFieldId(env.getType(pigeonType.getKeyPrefix()).getField("lastSeen").getKID());
		
		fhs = fieldHistoryService.get(filter, env);
		assertEquals(2, fhs.size());
		
		for (FieldHistory fh : fhs)
		{
			assertEquals(env.getType(pigeonType.getKeyPrefix()).getField("lastSeen").getKID(), fh.getFieldId());
			assertEquals(FieldHistoryOperation.UPDATE.toString(), fh.getOperation());
		}
		
		// now test logging field history by user who does not have permissions to create field history records
		Profile restrictedProfile = dataHelper.getTestProfileObject("RestrictedProfile", env);
		
		permissionService.setTypePermissionForProfile(restrictedProfile.getId(), pigeonType.getKID(), true, false, false, true, true, false, false, dataHelper.getRootAuthData(env), env);
		
		User restrictedUser = dataHelper.getTestUser("restr@kommet.io", "restr@kommet.io", restrictedProfile, env);
		AuthData restrictedAuthData = dataHelper.getAuthData(restrictedUser, env);
		
		Integer initialCount = fieldHistoryService.get(null, env).size();
		
		// create some pigeon
		Record anotherPigeon = new Record(pigeonType);
		anotherPigeon.setField("name", "Ziutek");
		anotherPigeon.setField("age", BigDecimal.valueOf(3));
		anotherPigeon.setField("dob", new Date(84, 3, 11));
		anotherPigeon.setField("lastSeen", lastSeenDate);
		anotherPigeon = dataService.save(anotherPigeon, restrictedAuthData, env);
		
		fhs = fieldHistoryService.get(null, env);
		assertEquals(initialCount + 1, fhs.size());
		
		testCollectionHistory(env);
	}
	
	/**
	 * Test logging field history on collections.
	 * @param env
	 * @throws KommetException 
	 */
	private void testCollectionHistory(EnvData env) throws KommetException
	{
		CompanyAppDataSet dataSet = CompanyAppDataSet.getInstance(dataService, env);
		
		// an inverse collection "employees" on company
		Field employeesField = new Field();
		employeesField.setApiName("employees");
		employeesField.setLabel("Employees");
		employeesField.setDataType(new InverseCollectionDataType(dataSet.getEmployeeType(), "company"));
		dataSet.getCompanyType().addField(employeesField);
		employeesField = dataService.createField(employeesField, env);
		
		// insert some companies
		Record company1 = dataService.save(dataSet.getTestCompany("company-1", null), env);
		Record company2 = dataService.save(dataSet.getTestCompany("company-2", null), env);
		List<Record> companies = new ArrayList<Record>();
		companies.add(company1);
		companies.add(company2);
		
		// insert some employees
		Record employee1 = dataService.save(dataSet.getTestEmployee("first name 1", "last name 1", "middle name 1", company1, null), env);
		Record employee2 = dataService.save(dataSet.getTestEmployee("first name 2", "last name 2", "middle name 2", company1, null), env);
		List<Record> employees = new ArrayList<Record>();
		employees.add(employee1);
		employees.add(employee2);
		
		fieldHistoryService.logCollectionUpdate(employeesField, company1.getKID(), null, employee1.getKID(), FieldHistoryOperation.ADD, dataHelper.getRootAuthData(env), env);
		
		FieldHistoryFilter filter = new FieldHistoryFilter();
		filter.addFieldId(employeesField.getKID());
		List<FieldHistory> historyLogs = fieldHistoryService.get(filter, env);
		assertEquals(1, historyLogs.size());
		FieldHistory fh = historyLogs.get(0);
		assertEquals(FieldHistoryOperation.ADD.toString(), fh.getOperation());
		
		// TODO test field history log for associations as well
	}

	@Test
	public void testFieldHistoryOperation()
	{
		assertEquals("Update", FieldHistoryOperation.UPDATE.toString());
		assertEquals("Add", FieldHistoryOperation.ADD.toString());
		assertEquals("Remove", FieldHistoryOperation.REMOVE.toString());
	}
}
