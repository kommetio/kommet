/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.data.DataService;
import kommet.data.NoSuchFieldException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.tests.harness.CompanyAppDataSet;

public class RecordTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Test
	public void testRecordOperations() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType (pigeonType, env);
		
		dataService.setDefaultField(pigeonType.getKID(), env.getType(pigeonType.getKID()).getField("name").getKID(), dataHelper.getRootAuthData(env), env);
		
		Record pigeon = new Record(pigeonType);
		
		// try getting a nested property that does not exist
		try
		{
			pigeon.getField("nonExisting.id");
			fail("Referencing a non-existing field should fail");
		}
		catch (NoSuchFieldException e)
		{
			assertTrue(e.getMessage().contains("nonExisting"));
		}
		
		/*pigeon.setField("name", "Rob");
		pigeon.setField("age", BigDecimal.valueOf(2));
		dataService.save(pigeon, env);
		
		pigeon = dataService.select("select id, name, age from " + MiscUtils.envSpecificBasePackageToUserPackage(pigeonType.getQualifiedName(), env) + " where id = '" + pigeon.getKolmuId() + "'", env).get(0);
		
		assertNotNull(pigeon.attemptGetKolmuId());
		assertEquals("Rob", pigeon.getDefaultFieldValue());
		assertEquals("Rob", dataService.getDefaultValue(pigeon.getKolmuId(), env));*/
	}
	
	@Test
	public void testGetRecordMap() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		CompanyAppDataSet dataSet = CompanyAppDataSet.getInstance(dataService, env);
		
		// insert some companies
		Record company1 = dataService.save(dataSet.getTestCompany("company-1", null), env);
		Record company2 = dataService.save(dataSet.getTestCompany("company-2", null), env);
		Record company3 = dataService.save(dataSet.getTestCompany("company-3", null), env);
		Record company4 = dataService.save(dataSet.getTestCompany("company-4", null), env);
		List<KID> companyIds = new ArrayList<KID>();
		companyIds.add(company1.getKID());
		companyIds.add(company2.getKID());
		companyIds.add(company3.getKID());
		companyIds.add(company4.getKID());
		
		// insert some employees
		Record employee1 = dataService.save(dataSet.getTestEmployee("first name 1", "last name 1", "middle name 1", null, null), env);
		Record employee2 = dataService.save(dataSet.getTestEmployee("first name 2", "last name 2", "middle name 2", null, null), env);
		Record employee3 = dataService.save(dataSet.getTestEmployee("first name 3", "last name 3", "middle name 3", null, null), env);
		// it is crucial for tests to have one employee with empty middle name
		Record employee4 = dataService.save(dataSet.getTestEmployee("first name 4", "last name 4", null, null, null), env);
		List<KID> employeeIds = new ArrayList<KID>();
		employeeIds.add(employee1.getKID());
		employeeIds.add(employee2.getKID());
		employeeIds.add(employee3.getKID());
		employeeIds.add(employee4.getKID());
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// get only employees
		Map<KID, Record> employees = dataService.getRecordMap(employeeIds, authData, env);
		assertNotNull(employees);
		assertEquals(employeeIds.size(), employees.size());
		assertNotNull(employee2.getKID());
		assertNotNull(employees.get(employee2.getKID()));
		assertEquals(employee2.getKID(), employees.get(employee2.getKID()).getKID());
		
		List<String> fieldNames = new ArrayList<String>();
		fieldNames.add("id");
		fieldNames.add("firstName");
		// also check querying nested fields
		fieldNames.add("createdBy.userName");
		fieldNames.add("lastModifiedBy");
		List<Record> employeeList = dataService.getRecords(employeeIds, dataSet.getEmployeeType(), fieldNames, authData, env);
		assertNotNull(employeeList);
		assertEquals(employeeIds.size(), employeeList.size());
		
		List<KID> ids = new ArrayList<KID>();
		ids.addAll(employeeIds);
		ids.addAll(companyIds);
		
		// get both employees and companies in one call
		Map<KID, Record> mixedRecords = dataService.getRecordMap(ids, authData, env);
		assertEquals(ids.size(), mixedRecords.size());
		assertEquals(employee2.getKID(), mixedRecords.get(employee2.getKID()).getKID());
		assertEquals(company3.getKID(), mixedRecords.get(company3.getKID()).getKID());
	}
}
