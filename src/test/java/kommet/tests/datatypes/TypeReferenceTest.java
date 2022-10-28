/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.datatypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.FormulaReturnType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.tests.harness.CompanyAppDataSet;
import kommet.utils.AppConfig;

public class TypeReferenceTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppConfig config;
	
	@Test
	public void testNullObjectReference() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		CompanyAppDataSet dataSet = CompanyAppDataSet.getInstance(dataService, env);
		
		// insert some companies
		dataService.save(dataSet.getTestCompany("company-1", null), env);
		
		// insert some employees
		Record employee1 = dataService.save(dataSet.getTestEmployee("first name 1", "last name 1", "middle name 1", null, null), env);
		
		// select employee with null company
		List<Record> employees = env.getSelectCriteriaFromDAL("select id, company.id, company.name from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.EMPLOYEE_TYPE_API_NAME + " where id = '" + employee1.getKID() + "'").list();
		assertEquals(1, employees.size());
		assertNull(employees.get(0).getField("company"));
		assertTrue(employees.get(0).isSet("company"));
		
		// query just this single property - we are checking this because such query used to return errors
		env.getSelectCriteriaFromDAL("select company.name from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.EMPLOYEE_TYPE_API_NAME + " where id = '" + employee1.getKID() + "'").list();
		
		// select employee with null company, but do not query the ID field of the company
		employees = env.getSelectCriteriaFromDAL("select id, company.name from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.EMPLOYEE_TYPE_API_NAME + " where id = '" + employee1.getKID() + "'").list();
		assertEquals(1, employees.size());
		assertNull(employees.get(0).getField("company"));
		assertTrue(employees.get(0).isSet("company"));
		
		testQueryingTypeReferenceWithNullFormula(dataSet, env);
		testDeepDalQueries(dataSet, env);
	}

	/**
	 * This method tests queries that reach 4 levels deep
	 * @param dataSet
	 * @param env
	 * @throws KommetException 
	 */
	private void testDeepDalQueries(CompanyAppDataSet dataSet, EnvData env) throws KommetException
	{
		// we already have a data set with employee type that has reference to company
		// now we want to add a pigeon type with reference to employee, and add a reference to pigeon on address type
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		Field employeeField = new Field();
		employeeField.setApiName("employee");
		employeeField.setLabel("Employee");
		employeeField.setDataType(new TypeReference(dataSet.getEmployeeType()));
		employeeField.setRequired(false);
		pigeonType.addField(employeeField);
		
		dataService.createField(employeeField, env);
		
		// now create a pigeon field on the address type
		Field pigeonField = new Field();
		pigeonField.setApiName("pigeon");
		pigeonField.setLabel("Pigeon");
		pigeonField.setDataType(new TypeReference(pigeonType));
		pigeonField.setRequired(false);
		dataSet.getAddressType().addField(pigeonField);
		
		dataService.createField(pigeonField, env);
		
		// test deep conditions in DAL
		List<Record> companies = env.getSelectCriteriaFromDAL("select id, pigeon.employee.company.id from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.ADDRESS_TYPE_API_NAME + " where pigeon.employee.company.id = '0010000000001'").list();
		assertNotNull(companies);
	}

	private void testQueryingTypeReferenceWithNullFormula(CompanyAppDataSet dataSet, EnvData env) throws KommetException
	{
		// add some other text field to company
		Field textField = new Field();
		textField.setApiName("testText");
		textField.setLabel("Test Text");
		textField.setDataType(new TextDataType(20));
		dataSet.getCompanyType().addField(textField);
		dataService.createField(textField, env);
		
		// update company type to reflect the added text field
		dataSet.setCompanyType(env.getType(dataSet.getCompanyType().getKID()));
		
		// add formula field to company
		Field formulaField = new Field();
		formulaField.setApiName("testFormula");
		formulaField.setLabel("Test Formula");
		formulaField.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "name + \" \" + testText", dataSet.getCompanyType(), env));
		dataSet.getCompanyType().addField(formulaField);
		dataService.createField(formulaField, env);
		
		// query null type reference with formula field
		// this scenario is tested because it failed at some point
		List<Record> employees = env.getSelectCriteriaFromDAL("select id, company.name, company.testFormula from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.EMPLOYEE_TYPE_API_NAME).list();
		assertEquals(1, employees.size());
		assertNull(employees.get(0).getField("company"));
		assertTrue(employees.get(0).isSet("company"));
	}
}
