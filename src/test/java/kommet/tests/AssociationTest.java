/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import kommet.auth.AuthData;
import kommet.dao.FieldFilter;
import kommet.dao.dal.DALSyntaxException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldRemovalException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.tests.harness.CompanyAppDataSet;

public class AssociationTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Test
	public void testCreateAssociationFromExistingLinkingType() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		CompanyAppDataSet dataSet = CompanyAppDataSet.getInstance(dataService, env);
		
		// insert some companies
		Record company1 = dataService.save(dataSet.getTestCompany("company-1", null), env);
		Record company2 = dataService.save(dataSet.getTestCompany("company-2", null), env);
		Record company3 = dataService.save(dataSet.getTestCompany("company-3", null), env);
		Record company4 = dataService.save(dataSet.getTestCompany("company-4", null), env);
		List<Record> companies = new ArrayList<Record>();
		companies.add(company1);
		companies.add(company2);
		companies.add(company3);
		companies.add(company4);
		
		// insert some employees
		Record employee1 = dataService.save(dataSet.getTestEmployee("first name 1", "last name 1", "middle name 1", null, null), env);
		Record employee2 = dataService.save(dataSet.getTestEmployee("first name 2", "last name 2", "middle name 2", null, null), env);
		Record employee3 = dataService.save(dataSet.getTestEmployee("first name 3", "last name 3", "middle name 3", null, null), env);
		// it is crucial for tests to have one employee with empty middle name
		Record employee4 = dataService.save(dataSet.getTestEmployee("first name 4", "last name 4", null, null, null), env);
		List<Record> employees = new ArrayList<Record>();
		employees.add(employee1);
		employees.add(employee2);
		employees.add(employee3);
		employees.add(employee4);
		
		// create a linking type
		Type linkingType = new Type();
		linkingType.setApiName("Employment");
		linkingType.setLabel("Employment");
		linkingType.setPluralLabel("Employments");
		linkingType.setPackage(CompanyAppDataSet.COMPANY_PACKAGE);
		
		// add reference to company type
		Field companyRef = new Field();
		companyRef.setApiName("company");
		companyRef.setLabel("Company");
		companyRef.setDataType(new TypeReference(dataSet.getCompanyType()));
		((TypeReference)companyRef.getDataType()).setCascadeDelete(true);
		companyRef.setRequired(true);
		linkingType.addField(companyRef);
		
		// add reference to employee type
		Field employeeRef = new Field();
		employeeRef.setApiName("employee");
		employeeRef.setLabel("Employee");
		employeeRef.setDataType(new TypeReference(dataSet.getEmployeeType()));
		((TypeReference)employeeRef.getDataType()).setCascadeDelete(true);
		employeeRef.setRequired(true);
		linkingType.addField(employeeRef);
		
		KID userId = env.getRootUser().getKID();
		
		linkingType = dataService.createType(linkingType, dataHelper.getRootAuthData(env), env);
		
		runChecksOnAssociation(dataSet, linkingType, companies, employees, userId, env);
		testSubqueryOnAssociation(dataSet, companies, employees, env);
		testDeleteAssociation(dataSet, linkingType, env);
		testCreateAssociationWithLinkingType(env);
	}

	private void testSubqueryOnAssociation(CompanyAppDataSet dataSet, List<Record> companies, List<Record> employees, EnvData env) throws KommetException
	{
		Field employeesField = dataSet.getCompanyType().getField("employees");
		assertNotNull(employeesField);
		
		KID companyId1 = companies.get(0).getKID();
		KID companyId2 = companies.get(2).getKID();
		KID employeeId1 = employees.get(0).getKID();
		KID employeeId2 = employees.get(1).getKID();
		KID employeeId3 = employees.get(2).getKID();
		
		AuthData authData = AuthData.getRootAuthData(env);
		
		// create a link between some companies and employees
		dataService.associate(employeesField.getKID(), companyId1, employeeId1, authData, env);
		dataService.associate(employeesField.getKID(), companyId1, employeeId2, authData, env);
		dataService.associate(employeesField.getKID(), companyId2, employeeId1, authData, env);
		dataService.associate(employeesField.getKID(), companyId2, employeeId3, authData, env);
		
		List<Record> fetchedEmployees = env.getSelectCriteriaFromDAL("select id, firstName, lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.EMPLOYEE_TYPE_API_NAME + " where id in (select employees.id from " + dataSet.getCompanyType().getQualifiedName() + " where id = '" + companyId2 + "') and firstName <> 'arthur' ORDER by firstName ASC" ).list();
		assertNotNull(fetchedEmployees);
		assertEquals(2, fetchedEmployees.size());
		
		boolean employee1Found = false;
		boolean employee3Found = false;
		
		for (Record emp : fetchedEmployees)
		{
			if (emp.getKID().equals(employeeId1))
			{
				employee1Found = true;
			}
			else if (emp.getKID().equals(employeeId3))
			{
				employee3Found = true;
			}
		}
		
		assertTrue(employee1Found);
		assertTrue(employee3Found);
		
		// now run a query where the subquery returns a text field
		fetchedEmployees = env.getSelectCriteriaFromDAL("select id, firstName, lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.EMPLOYEE_TYPE_API_NAME + " where firstName in (select employees.firstName from " + dataSet.getCompanyType().getQualifiedName() + " where id = '" + companyId2 + "') and firstName <> 'arthur' ORDER by firstName ASC" ).list();
		
		assertNotNull(fetchedEmployees);
		assertEquals(2, fetchedEmployees.size());
		
		employee1Found = false;
		employee3Found = false;
		
		for (Record emp : fetchedEmployees)
		{
			if (emp.getKID().equals(employeeId1))
			{
				employee1Found = true;
			}
			else if (emp.getKID().equals(employeeId3))
			{
				employee3Found = true;
			}
		}
		
		assertTrue(employee1Found);
		assertTrue(employee3Found);
		
		try
		{
			// now test incompatible subquery - one that returns an invalid field data type
			env.getSelectCriteriaFromDAL("select id, firstName, lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.EMPLOYEE_TYPE_API_NAME + " where firstName in (select employees.id from " + dataSet.getCompanyType().getQualifiedName() + " where id = '" + companyId2 + "') and firstName <> 'arthur' ORDER by firstName ASC" ).list();
			fail("Running query with incompatible subrestriction field should fail");
		}
		catch (DALSyntaxException e)
		{
			assertEquals("Data type of field queried in subcriteria does not match the field in the IN condition", e.getMessage());
		}
	}

	@SuppressWarnings("deprecation")
	private void testCreateAssociationWithLinkingType(EnvData env) throws KommetException
	{
		AuthData authData = AuthData.getRootAuthData(env);
		
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		DataService.LinkingTypeResult result = dataService.createLinkingType(pigeonType, pigeonType, authData, env);
		assertNotNull(result.getType());
		assertEquals(StringUtils.uncapitalise(pigeonType.getApiName()), result.getSelfLinkingField().getApiName());
		assertEquals(StringUtils.uncapitalise(pigeonType.getApiName()) + "Associated", result.getForeignLinkingField().getApiName());
		
		Type friendsLinkingType = result.getType();
		
		// create association field
		Field friendsField = new Field();
		friendsField.setApiName("friends");
		friendsField.setLabel("friends");
		
		AssociationDataType assocDT = new AssociationDataType();
		assocDT.setLinkingType(result.getType());
		assocDT.setSelfLinkingField(result.getSelfLinkingField().getApiName());
		assocDT.setForeignLinkingField(result.getForeignLinkingField().getApiName());
		assocDT.setAssociatedType(pigeonType);
		friendsField.setDataType(assocDT);
		pigeonType.addField(friendsField);
		
		friendsField = dataService.createField(friendsField, env);
		
		Field fetchedField = dataService.getFieldForUpdate(friendsField.getKID(), env);
		assertNotNull(fetchedField);
		
		AssociationDataType fetchedDT = (AssociationDataType)fetchedField.getDataType();
		assertNotNull(fetchedDT.getAssociatedType());
		assertNotNull(fetchedDT.getLinkingType());
		assertNotNull(fetchedDT.getSelfLinkingField());
		assertNotNull(fetchedDT.getForeignLinkingField());
		
		env.getSelectCriteriaFromDAL("select id, friends.id from " + pigeonType.getQualifiedName());
		
		// now create a pigeon and associate some friends with him
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Dirk");
		pigeon1.setField("age", 2);
		pigeon1 = dataService.save(pigeon1, env);
		assertNotNull(pigeon1.getKID());
		
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Brian");
		pigeon2.setField("age", 2);
		pigeon2 = dataService.save(pigeon2, env);
		assertNotNull(pigeon2.getKID());
		
		Record pigeon3 = new Record(pigeonType);
		pigeon3.setField("name", "Connor");
		pigeon3.setField("age", 2);
		pigeon3 = dataService.save(pigeon3, env);
		assertNotNull(pigeon3.getKID());
		
		dataService.associate(friendsField.getKID(), pigeon1.getKID(), pigeon2.getKID(), authData, env);
		dataService.associate(friendsField.getKID(), pigeon1.getKID(), pigeon3.getKID(), authData, env);
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, friends.id from " + pigeonType.getQualifiedName() + " where id = '" + pigeon1.getKID() + "'").list();
		assertEquals(1, pigeons.size());
		assertNotNull(pigeons.get(0).getField("friends"));
		List<Record> friends = (List<Record>)pigeons.get(0).getField("friends");
		assertEquals(2, friends.size());
		
		// now delete pigeon2
		dataService.deleteRecord(pigeon2, env);
		
		pigeons = env.getSelectCriteriaFromDAL("select id, friends.id from " + pigeonType.getQualifiedName() + " where id = '" + pigeon1.getKID() + "'").list();
		assertEquals(1, pigeons.size());
		assertNotNull(pigeons.get(0).getField("friends"));
		friends = (List<Record>)pigeons.get(0).getField("friends");
		assertEquals(1, friends.size());
		
		// create another linking type
		result = dataService.createLinkingType(pigeonType, pigeonType, AuthData.getRootAuthData(env), env);
		assertNotNull(result.getType());
		assertTrue(result.getType().getApiName().endsWith("1"));
		assertEquals(StringUtils.uncapitalise(pigeonType.getApiName()), result.getSelfLinkingField().getApiName());
		assertEquals(StringUtils.uncapitalise(pigeonType.getApiName()) + "Associated", result.getForeignLinkingField().getApiName());
		
		// now delete the field and make sure the linking type is deleted as well
		dataService.deleteField(friendsField, AuthData.getRootAuthData(env), env);
		assertNull(env.getType(friendsLinkingType.getQualifiedName()));
		assertNull(env.getType(friendsLinkingType.getKID()));
	}

	private void testDeleteAssociation(CompanyAppDataSet dataSet, Type linkingType, EnvData env) throws KommetException
	{
		// try to delete a type reference field on the association
		Field fieldToDelete = linkingType.getField("employee");
		
		try
		{
			dataService.deleteField(fieldToDelete, dataHelper.getRootAuthData(env), env);
			fail("Removing field used by association should fail");
		}
		catch (FieldRemovalException e)
		{
			assertEquals("Field " + fieldToDelete.getType().getQualifiedName() + "." + fieldToDelete.getApiName() + " cannot be removed because it is used by an association " + dataSet.getCompanyType().getQualifiedName() + ".employees", e.getMessage());
		}
		
		try
		{
			dataService.deleteField(linkingType.getField("company"), dataHelper.getRootAuthData(env), env);
			fail("Removing field used by association should fail");
		}
		catch (FieldRemovalException e)
		{
			assertEquals("Field " + fieldToDelete.getType().getQualifiedName() + ".company cannot be removed because it is used by an association " + dataSet.getCompanyType().getQualifiedName() + ".employees", e.getMessage());
		}
		
		dataService.deleteField(env.getType(dataSet.getCompanyType().getKID()).getField("employees"), dataHelper.getRootAuthData(env), env);
		dataService.deleteField(fieldToDelete, dataHelper.getRootAuthData(env), env);
	}

	@SuppressWarnings("unchecked")
	private void runChecksOnAssociation(CompanyAppDataSet dataSet, Type linkingType, List<Record> companies, List<Record> employees, KID userId, EnvData env) throws KommetException
	{
		// now create an association on company to employees
		Field association = new Field();
		association.setApiName("employees");
		association.setLabel("employees");
		association.setDataType(new AssociationDataType(linkingType, dataSet.getEmployeeType(), "company", "employee"));
		
		association.setType(dataSet.getCompanyType());
		association = dataService.createField(association, dataHelper.getRootAuthData(env), env);
		
		dataSet.setCompanyType(env.getType(dataSet.getCompanyType().getKID()));
		
		// find the new field using filter search
		FieldFilter fieldFilter = new FieldFilter();
		fieldFilter.setDataType(new AssociationDataType());
		fieldFilter.setAssociatedTypeId(employees.get(0).getType().getKID());
		List<Field> foundAssociationFields = dataService.getFields(fieldFilter, env);
		assertEquals(1, foundAssociationFields.size());
		assertEquals(association.getKID(), foundAssociationFields.get(0).getKID());
		
		// try to select companies with employees - should return empty collection
		List<Record> fetchedCompanies = env.getSelectCriteriaFromDAL("select id, name, employees.firstName, employees.middleName, employees.lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.COMPANY_TYPE_API_NAME).list();
		assertNotNull(fetchedCompanies);
		assertEquals(4, fetchedCompanies.size());
		
		for (Record company : fetchedCompanies)
		{
			assertNotNull("Empty association collection is null, but should be an empty list", company.getField("employees"));
			assertTrue(company.getField("employees") instanceof List);
		}
		
		// now create an employment for employee one in company one and two
		Record employment1 = new Record(linkingType);
		employment1.setField("company.id", companies.get(0).getKID(), env);
		employment1.setField("employee.id", employees.get(0).getKID(), env);
		dataService.save(employment1, env);
		
		Record employment2 = new Record(linkingType);
		employment2.setField("company.id", companies.get(1).getKID(), env);
		employment2.setField("employee.id", employees.get(0).getKID(), env);
		dataService.save(employment2, env);
		
		// now create an employment for employee two in company one and two
		Record employment3 = new Record(linkingType);
		employment3.setField("company.id", companies.get(1).getKID(), env);
		employment3.setField("employee.id", employees.get(1).getKID(), env);
		dataService.save(employment3, env);
		
		Record employment4 = new Record(linkingType);
		employment4.setField("company.id", companies.get(1).getKID(), env);
		employment4.setField("employee.id", employees.get(3).getKID(), env);
		dataService.save(employment4, env);
		
		// get the employment linking record
		Record refetchedEmployment4 = dataService.getAssociation(association.getKID(), companies.get(1).getKID(), employees.get(3).getKID(), env);
		assertNotNull("Linking record not retrieved. Either not created, or not found by method getAssociation", refetchedEmployment4);
		assertEquals(employment4.getKID(), refetchedEmployment4.getKID());
		
		// now search for companies again
		fetchedCompanies = env.getSelectCriteriaFromDAL("select id, name, employees.id, employees.firstName, employees.middleName, employees.lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.COMPANY_TYPE_API_NAME).list();
		assertNotNull(fetchedCompanies);
		assertEquals(4, fetchedCompanies.size());
		
		boolean employee1FoundInCompany2 = false;
		boolean employee2FoundInCompany2 = false;
		
		for (Record company : fetchedCompanies)
		{
			if (company.getKID().equals(companies.get(0).getKID()))
			{
				assertNotNull(company.getField("employees"));
				List<Record> fetchedEmployees = (List<Record>)company.getField("employees");
				assertEquals(1, fetchedEmployees.size());
				assertEquals(employees.get(0).getKID(), fetchedEmployees.get(0).getKID());
				assertEquals(employees.get(0).getField("firstName"), fetchedEmployees.get(0).getField("firstName"));
			}
			else if (company.getKID().equals(companies.get(1).getKID()))
			{
				assertNotNull(company.getField("employees"));
				List<Record> fetchedEmployees = (List<Record>)company.getField("employees");
				assertEquals(3, fetchedEmployees.size());
				assertTrue(employees.get(0).getKID().equals(employees.get(0).getKID()) || employees.get(1).getKID().equals(employees.get(0).getKID()) || employees.get(2).getKID().equals(employees.get(0).getKID()));
				
				for (Record emp : fetchedEmployees)
				{
					// make sure middle name field is initialized, even though it's null for some employees
					if (emp.getKID().equals(employment4.getField("employee.id")))
					{
						assertNull(emp.getField("middleName"));
					}
					else
					{
						assertNotNull(emp.getField("middleName"));
					}
					
					if (employees.get(0).getField("firstName").equals(emp.getField("firstName")))
					{
						employee1FoundInCompany2 = true;
					}
					else if (employees.get(1).getField("firstName").equals(emp.getField("firstName")))
					{
						employee2FoundInCompany2 = true;
					}
				}
			}
			else if (company.getKID().equals(companies.get(2).getKID()))
			{
				assertNotNull(company.getField("employees"));
				List<Record> fetchedEmployees = (List<Record>)company.getField("employees");
				assertEquals(0, fetchedEmployees.size());
			}
		}
		
		assertTrue(employee1FoundInCompany2);
		assertTrue(employee2FoundInCompany2);
		
		// remove env info to read it again
		envService.resetEnv(env.getId());
		
		// make sure that the association definition can be properly read in and restored from DB
		EnvData refreshedEnv = envService.get(env.getId());
		Type refreshedCompanyType = refreshedEnv.getType(dataSet.getCompanyType().getKID());
		assertNotNull(refreshedCompanyType);
		Field employeesField = refreshedCompanyType.getField("employees");
		assertNotNull(employeesField);
		assertEquals(dataSet.getEmployeeType().getKID(), ((AssociationDataType)employeesField.getDataType()).getAssociatedTypeId());
		assertEquals(dataSet.getEmployeeType().getKID(), ((AssociationDataType)employeesField.getDataType()).getAssociatedType().getKID());
		assertEquals(linkingType.getKID(), ((AssociationDataType)employeesField.getDataType()).getLinkingTypeId());
		assertEquals(linkingType.getKID(), ((AssociationDataType)employeesField.getDataType()).getLinkingType().getKID());
		assertEquals("company", ((AssociationDataType)employeesField.getDataType()).getSelfLinkingField());
		assertEquals("employee", ((AssociationDataType)employeesField.getDataType()).getForeignLinkingField());
		
		// now delete the company for which associated employees exist and make sure this succeeds
		KID company2Id = companies.get(1).getKID();
		dataService.deleteRecord(companies.get(1), null, refreshedEnv);
		assertEquals(Long.valueOf(0), refreshedEnv.getSelectCriteriaFromDAL("select count(id) from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.COMPANY_TYPE_API_NAME + " WHERE " + Field.ID_FIELD_NAME + " = '" + company2Id + "'").count());
		
		AuthData authData = dataHelper.getRootAuthData(refreshedEnv);
		
		// now associate records using the DataService.associate method
		Record company4 = companies.get(3);
		Record employee3 = employees.get(2);
		Record employee4 = employees.get(3);
		dataService.associate(association.getKID(), company4.getKID(), employee3.getKID(), authData, refreshedEnv);
		Record linkingRecord = dataService.associate(association.getKID(), company4.getKID(), employee4.getKID(), authData, refreshedEnv);
		
		assertNotNull(linkingRecord);
		assertNotNull(linkingRecord.attemptGetKID());
		
		// now query the company together with employees and make sure they have been correctly assigned
		fetchedCompanies = env.getSelectCriteriaFromDAL("select id, name, employees.firstName, employees.middleName, employees.lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.COMPANY_TYPE_API_NAME + " WHERE " + Field.ID_FIELD_NAME + " = '" + company4.getKID() + "'").list();
		assertNotNull(fetchedCompanies);
		assertEquals(1, fetchedCompanies.size());
		assertEquals("Association between company and employee not created, expected 2 employees on list, but found " + ((List<Record>)fetchedCompanies.get(0).getField("employees")).size(), 2, ((List<Record>)fetchedCompanies.get(0).getField("employees")).size());
		
		// now unassociate one employee from company 4
		dataService.unassociate(association.getKID(), company4.getKID(), employee3.getKID(), false, authData, refreshedEnv);
		
		// query the company together with employees and make sure one of them has been removed
		fetchedCompanies = env.getSelectCriteriaFromDAL("select id, name, employees.firstName, employees.middleName, employees.lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.COMPANY_TYPE_API_NAME + " WHERE " + Field.ID_FIELD_NAME + " = '" + company4.getKID() + "'").list();
		assertNotNull(fetchedCompanies);
		assertEquals(1, fetchedCompanies.size());
		List<Record> associatedEmployees = ((List<Record>)fetchedCompanies.get(0).getField("employees"));
		assertEquals("Association between company and employee not removed, expected 1 employee on list, but found " + associatedEmployees.size(), 1, associatedEmployees.size());
		assertNull(associatedEmployees.get(0).getField("middleName"));
		
		// now unassociate the last employee to see how the system deals with empty collections
		dataService.unassociate(association.getKID(), company4.getKID(), employee4.getKID(), false, authData, refreshedEnv);
		fetchedCompanies = env.getSelectCriteriaFromDAL("select id, name, employees.firstName, employees.middleName, employees.lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.COMPANY_TYPE_API_NAME + " WHERE " + Field.ID_FIELD_NAME + " = '" + company4.getKID() + "'").list();
		assertNotNull(fetchedCompanies);
		assertEquals(1, fetchedCompanies.size());
		associatedEmployees = ((List<Record>)fetchedCompanies.get(0).getField("employees"));
		assertEquals("Association between company and employee not removed, expected 1 employee on list, but found " + associatedEmployees.size(), 0, associatedEmployees.size());
	}
}
