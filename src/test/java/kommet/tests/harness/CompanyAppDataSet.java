/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.harness;

import java.util.Date;

import org.junit.Test;

import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;

public class CompanyAppDataSet
{
	public static final String COMPANY_PACKAGE = "kommet.company";
	public static final String COMPANY_TYPE_API_NAME = "Company";
	public static final String EMPLOYEE_TYPE_API_NAME = "Employee";
	public static final String ADDRESS_TYPE_API_NAME = "Address";
	private Type companyType;
	private Type employeeType;
	private Type addressType;
	
	@Test
	public void stubTestMethod()
	{
		// empty
	}
	
	public static CompanyAppDataSet getInstance(DataService dataService, EnvData env) throws KommetException
	{
		CompanyAppDataSet set = new CompanyAppDataSet();
		
		set.setAddressType(createAddressType(env));
		if (dataService != null)
		{
			set.setAddressType(dataService.createType(set.getAddressType(), env));
		}
		
		set.setCompanyType(createCompanyType(set.getAddressType(), env));
		if (dataService != null)
		{
			set.setCompanyType(dataService.createType(set.getCompanyType(), env));
		}
		
		set.setEmployeeType(createEmployeeType(set.getCompanyType(), set.getAddressType(), env));
		if (dataService != null)
		{
			set.setEmployeeType(dataService.createType(set.getEmployeeType(), env));
		}
		
		// add inverse collection to company to address type
		Field companyField = new Field();
		companyField.setApiName("companies");
		companyField.setLabel("Companies");
		companyField.setDataType(new InverseCollectionDataType(set.getCompanyType(), "address"));
		companyField.setRequired(false);
		set.getAddressType().addField(companyField);
		
		// add inverse collection to employee to address type
		Field employeeField = new Field();
		employeeField.setApiName("employees");
		employeeField.setLabel("Employees");
		employeeField.setDataType(new InverseCollectionDataType(set.getEmployeeType(), "address"));
		employeeField.setRequired(false);
		set.getAddressType().addField(employeeField);
		
		if (dataService != null)
		{
			dataService.createField(set.getAddressType().getField("companies"), env);
			dataService.createField(set.getAddressType().getField("employees"), env);
		}
		
		return set;
	}

	private static Type createAddressType(EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName(ADDRESS_TYPE_API_NAME);
		type.setPackage(COMPANY_PACKAGE);
		type.setLabel("Employee");
		type.setPluralLabel("Employees");
		type.setCreated(new Date());
		
		type.addField(getCityField());
		
		return type;
	}

	private static Type createEmployeeType(Type companyType, Type addressType, EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName(EMPLOYEE_TYPE_API_NAME);
		type.setPackage(COMPANY_PACKAGE);
		type.setLabel("Employee");
		type.setPluralLabel("Employees");
		type.setCreated(new Date());
		
		type.addField(getFirstNameField());
		type.addField(getLastNameField());
		type.addField(getMiddleNameField());
		type.addField(getCompanyField(companyType));
		type.addField(getAddressField(addressType));
		
		return type;
	}

	private static Field getCompanyField(Type companyType) throws KommetException
	{
		Field field = new Field();
		field.setApiName("company");
		field.setDataType(new TypeReference(companyType));
		field.setLabel("Company");
		field.setRequired(false);
		return field;
	}
	
	private static Field getAddressField(Type addressType) throws KommetException
	{
		Field field = new Field();
		field.setApiName("address");
		field.setDataType(new TypeReference(addressType));
		field.setLabel("Company");
		field.setRequired(false);
		return field;
	}

	private static Type createCompanyType(Type addressType, EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName(COMPANY_TYPE_API_NAME);
		type.setPackage(COMPANY_PACKAGE);
		type.setLabel("Company");
		type.setPluralLabel("Companies");
		type.setCreated(new Date());
		
		type.addField(getNameField());
		type.addField(getAddressField(addressType));
		
		return type;
	}
	
	private static Field getFirstNameField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("firstName");
		field.setDataType(new TextDataType(100));
		field.setLabel("First Name");
		field.setRequired(true);
		return field;
	}
	
	private static Field getCityField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("city");
		field.setDataType(new TextDataType(20));
		field.setLabel("City");
		field.setRequired(true);
		return field;
	}
	
	private static Field getLastNameField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("lastName");
		field.setDataType(new TextDataType(100));
		field.setLabel("Last Name");
		field.setRequired(true);
		return field;
	}
	
	private static Field getMiddleNameField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("middleName");
		field.setDataType(new TextDataType(100));
		field.setLabel("Middle Name");
		field.setRequired(false);
		return field;
	}
	
	private static Field getNameField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("name");
		field.setDataType(new TextDataType(100));
		field.setLabel("Name");
		field.setRequired(true);
		return field;
	}

	public void setCompanyType(Type companyType)
	{
		this.companyType = companyType;
	}

	public Type getCompanyType()
	{
		return companyType;
	}

	public void setAddressType(Type addressType)
	{
		this.addressType = addressType;
	}

	public Type getAddressType()
	{
		return addressType;
	}

	public void setEmployeeType(Type employeeType)
	{
		this.employeeType = employeeType;
	}

	public Type getEmployeeType()
	{
		return employeeType;
	}

	public Record getTestCompany(String name, Record address) throws KommetException
	{
		Record company = new Record(this.getCompanyType());
		company.setField("name", name);
		if (address != null)
		{
			company.setField("address", address);
		}
		return company;
	}

	public Record getTestEmployee(String firstName, String lastName, String middleName, Record company, Record address) throws KommetException
	{
		Record employee = new Record(getEmployeeType());
		employee.setField("firstName", firstName);
		employee.setField("lastName", lastName);
		employee.setField("middleName", middleName);
		if (company != null)
		{
			employee.setField("company", company);
		}
		if (address != null)
		{
			employee.setField("address", address);
		}
		return employee;
	}

	public Record getTestAddress(String city) throws KommetException
	{
		Record address = new Record(getAddressType());
		address.setField("city", city);
		return address;
	}
}
