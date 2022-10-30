/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.RecordProxyUtil;
import kommet.dao.dal.DALSyntaxException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class TypeCodeDeclarationTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	ClassService clsService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Inject
	KommetCompiler compiler;
	
	@Test
	public void testTypeCodeDeclaration() throws KommetException, ClassNotFoundException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		testCodeDeclaration("com.test.Bee", false, authData, env);
		testCodeDeclaration("com.birds.Owl", true, authData, env);
	}
	
	private void testCodeDeclaration(String typeName, boolean isAutoSetters, AuthData authData, EnvData env) throws KommetException
	{
		List<Field> fields = new ArrayList<Field>();
		
		Class typeClass = getTypeClass(typeName, "Bee", "Bees", fields, new HashSet<String>(), true, true, true, !isAutoSetters, authData, env);
		
		// add some fields
		fields.addAll(getTestFields());
		typeClass = getTypeClass(typeName, "Bee", "Bees", fields, new HashSet<String>(), true, true, true, !isAutoSetters, authData, env);
		
		// save the file
		typeClass = clsService.fullSave(typeClass, dataService, authData, env);
		assertNotNull(typeClass.getId());
		
		// make sure type has been created
		Type beeType = env.getType(typeName);
		assertNotNull(beeType);
		
		for (Field field : beeType.getFields())
		{
			assertNotNull(field.getKID());
		}
		
		assertNotNull(beeType.getField(Field.ID_FIELD_NAME));
		assertNotNull(beeType.getField(Field.CREATEDBY_FIELD_NAME));
		
		// now insert a new bee
		Record bee = new Record(env.getType(typeName));
		bee.setField("name", "Maja");
		bee.setField("age", 3);
		bee = dataService.save(bee, env);
		assertNotNull(bee.getKID());
		
		// make sure generating proxy works
		RecordProxyUtil.generateCustomTypeProxy(bee, env, compiler);
		
		// query the bee
		List<Record> insertedBees = env.getSelectCriteriaFromDAL("select id, name, age from " + typeName + " where name = '" + bee.getField("name") + "'").list();
		assertEquals(1, insertedBees.size());
		assertEquals(bee.getField("name"), insertedBees.get(0).getField("name"));
		assertEquals(bee.getField("age"), insertedBees.get(0).getField("age"));
		assertEquals(bee.getKID(), insertedBees.get(0).getKID());
		
		// update the type without any modifications
		typeClass = clsService.fullSave(typeClass, dataService, authData, env);
		
		beeType = env.getType(typeName);
		assertNotNull(beeType);
		
		for (Field field : beeType.getFields())
		{
			assertNotNull(field.getKID());
		}
		
		// refresh env
		envService.resetEnv(env.getId());
		EnvData refreshedEnv = envService.get(env.getId());
		
		beeType = refreshedEnv.getType(typeName);
		assertNotNull(beeType);
		assertNotNull(beeType.getField("name"));
		assertNotNull(beeType.getField("age"));
		
		assertNotNull(dataService.getField(beeType.getField("age").getKID(), refreshedEnv));
		
		for (Field field : beeType.getFields())
		{
			assertNotNull(field.getKID());
		}
		
		// update the type without any modifications
		typeClass = clsService.fullSave(typeClass, dataService, authData, refreshedEnv);
		beeType = refreshedEnv.getType(typeName);
		assertNotNull(beeType);
		for (Field field : beeType.getFields())
		{
			assertNotNull(field.getKID());
		}
		
		// add one more field
		Field salaryField = new Field();
		salaryField.setApiName("salary");
		salaryField.setLabel("Salary");
		salaryField.setDataType(new NumberDataType(0, Integer.class));
		salaryField.setRequired(false);
		fields.add(salaryField);
		
		Class newTypeClass = getTypeClass(typeName, "Bee", "Bees", fields, new HashSet<String>(), true, true, true, !isAutoSetters, authData, env);
		typeClass.setKollCode(newTypeClass.getKollCode());
		typeClass = clsService.fullSave(typeClass, dataService, authData, refreshedEnv);
		beeType = refreshedEnv.getType(typeName);
		assertNotNull(beeType.getField("name"));
		assertNotNull(beeType.getField("salary"));
		assertNotNull(dataService.getField(beeType.getField("salary").getKID(), refreshedEnv));
		
		// now generate the same code, but remove the @Field annotation from the salary field
		Set<String> unannotatedFields = new HashSet<String>();
		unannotatedFields.add("salary");
		Class newCode = getTypeClass(typeName, "Bee", "Bees", fields, unannotatedFields, true, true, true, !isAutoSetters, authData, refreshedEnv);
		typeClass.setKollCode(newCode.getKollCode());
		typeClass = clsService.fullSave(typeClass, dataService, authData, refreshedEnv);
		beeType = refreshedEnv.getType(typeName);
		assertNotNull(beeType);
		assertNotNull(beeType.getKID());
		assertNotNull(beeType.getField("name"));
		assertNull("Salary field should have been removed", beeType.getField("salary"));
		
		// add the salary field anew
		newCode = getTypeClass(typeName, "Bee", "Bees", fields, new HashSet<String>(), true, true, true, !isAutoSetters, authData, refreshedEnv);
		typeClass.setKollCode(newCode.getKollCode());
		typeClass = clsService.fullSave(typeClass, dataService, authData, refreshedEnv);
		beeType = refreshedEnv.getType(typeName);
		assertNotNull(beeType);
		assertNotNull(beeType.getKID());
		assertNotNull(beeType.getField("name"));
		assertNotNull("Salary field should have been added anew", beeType.getField("salary"));
		
		newCode = getTypeClass(typeName, "Bee", "Bees", getTestFields(), new HashSet<String>(), true, true, true, !isAutoSetters, authData, refreshedEnv);
		typeClass.setKollCode(newCode.getKollCode());
		typeClass = clsService.fullSave(typeClass, dataService, authData, refreshedEnv);
		beeType = refreshedEnv.getType(typeName);
		assertNotNull(beeType);
		assertNotNull(beeType.getKID());
		assertNotNull(beeType.getField("name"));
		assertNull("Salary field should have been removed", beeType.getField("salary"));
		
		// add the salary field anew again
		fields = getTestFields();
		fields.add(salaryField);
		newCode = getTypeClass(typeName, "Bee", "Bees", fields, new HashSet<String>(), true, true, true, !isAutoSetters, authData, refreshedEnv);
		typeClass.setKollCode(newCode.getKollCode());
		typeClass = clsService.fullSave(typeClass, dataService, authData, refreshedEnv);
		beeType = refreshedEnv.getType(typeName);
		assertNotNull(beeType);
		assertNotNull(beeType.getKID());
		assertNotNull(beeType.getField("name"));
		assertNotNull("Salary field should have been added anew", beeType.getField("salary"));
		
		// now remove the @Type annotation and make sure the type is deleted
		Class undeclaredTypeClass = getTypeClass(typeName, "Bee", "Bees", fields, new HashSet<String>(), false, true, true, !isAutoSetters, authData, refreshedEnv);
		typeClass.setKollCode(undeclaredTypeClass.getKollCode());
		typeClass = clsService.fullSave(typeClass, dataService, authData, refreshedEnv);
		assertNotNull(typeClass.getId());
		assertNull(refreshedEnv.getType(typeName));
		
		try
		{
			refreshedEnv.getSelectCriteriaFromDAL("select name, age from " + typeName + " where name = '" + bee.getField("name") + "'").list();
			fail("Query non-existing type should fail");
		}
		catch (DALSyntaxException e)
		{
			assertTrue(e.getMessage().startsWith("No type found with API name " + typeName));
		}
		
		testDeletingTypeDeclaration(authData, env); 
	}

	/**
	 * Make sure that when a class containing a type declaration is removed, this type is removed as well.
	 * @param typeName
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	private void testDeletingTypeDeclaration(AuthData authData, EnvData env) throws KommetException
	{	
		String typeName = "com.animals.Dog";
		
		// add some fields
		List<Field> fields = new ArrayList<Field>();
		fields.addAll(getTestFields());
		Class typeClass = getTypeClass(typeName, "Dog", "Dogs", fields, new HashSet<String>(), true, true, true, true, authData, env);
		
		// save the file
		typeClass = clsService.fullSave(typeClass, dataService, authData, env);
		assertNotNull(typeClass.getId());
		
		// make sure type has been created
		Type declaredType = env.getType(typeName);
		assertNotNull(declaredType);
		assertNotNull(dataService.getTypeByName(typeName, false, env));
		
		// now delete the declaring class
		clsService.delete(typeClass, dataService, authData, env);
		assertNull(env.getType(typeName));
		assertNull(dataService.getTypeByName(typeName, false, env));
	}

	private List<Field> getTestFields() throws KommetException
	{
		List<Field> fields = new ArrayList<Field>();
		
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(100));
		nameField.setRequired(true);
		fields.add(nameField);
		
		Field ageField = new Field();
		ageField.setApiName("age");
		ageField.setLabel("Age");
		ageField.setDataType(new NumberDataType(0, Integer.class));
		ageField.setRequired(false);
		fields.add(ageField);
		
		Field isMaleField = new Field();
		isMaleField.setApiName("isMale");
		isMaleField.setLabel("Is Male");
		isMaleField.setDataType(new BooleanDataType());
		isMaleField.setRequired(false);
		fields.add(isMaleField);
		
		return fields;
	}

	private Class getTypeClass(String name, String label, String plLabel, List<Field> fields, Set<String> unannotatedFields, boolean isGenerateTypeAnnotation, boolean isAddDataTypeAnnotation, boolean isGenerateSetters, boolean isAnnotateSetters, AuthData authData, EnvData env) throws KommetException
	{
		List<String> nameParts = MiscUtils.splitByLastDot(name);
		
		Class file = new Class();
		file.setName(nameParts.get(1));
		file.setPackageName(nameParts.get(0));
		
		StringBuilder code = new StringBuilder("package " + nameParts.get(0) + ";\n\n");
		
		code.append("import ").append(kommet.koll.annotations.DataType.class.getName()).append(";\n");
		code.append("import ").append(kommet.koll.annotations.Type.class.getName()).append(";\n");
		code.append("import ").append(kommet.koll.annotations.Field.class.getName()).append(";\n");
		code.append("import ").append(kommet.koll.annotations.Setter.class.getName()).append(";\n");
		
		if (isGenerateTypeAnnotation)
		{
			// add type annotation
			code.append("@Type (label = \"").append(label).append("\", pluralLabel = \"").append(plLabel).append("\")\n");
		}
		
		// add class body
		code.append("public class " + nameParts.get(1)).append(" {");
		
		for (Field field : fields)
		{
			code.append(getFieldDeclaration(field, !unannotatedFields.contains(field.getApiName()), isAddDataTypeAnnotation, isGenerateSetters, isAnnotateSetters));
		}
		
		// end class body
		code.append("\n}");
		
		//file.setJavaCode(clsService.getKollTranslator(env).kollToJava(code.toString(), true, authData, env));
		file.setKollCode(code.toString());
		file.setIsSystem(false);
		
		return file;
	}

	private String getFieldDeclaration(Field field, boolean isAddFieldAnnotation, boolean isAddDataTypeAnnotation, boolean isGenerateSetters, boolean isAnnotateSetters)
	{
		String javaType = field.getDataType().getJavaType();
		
		/*if (field.getDataTypeId().equals(kommet.data.datatypes.DataType.NUMBER))
		{
			
		}*/
		
		StringBuilder code = new StringBuilder();
		
		if (isAddFieldAnnotation)
		{
			code.append("@Field(label = \"").append(field.getLabel() + "\", name = \"").append(field.getApiName() + "\", required = ").append(field.isRequired()).append(")");
			code.append("\n");
			
			if (isAddDataTypeAnnotation)
			{
				code.append("@DataType(id = ").append(field.getDataTypeId()).append(")\n");
			}
		}
		
		code.append("public ").append(javaType).append(" get").append(org.springframework.util.StringUtils.capitalize(field.getApiName()));
		code.append("()");
		
		// add throws
		code.append(" throws " + KommetException.class.getName()).append("\n");
		
		code.append("{ return null; }");
		
		if (isGenerateSetters)
		{
			if (isAnnotateSetters)
			{
				// add setter
				code.append("@Setter (field = \"").append(field.getApiName()).append("\")\n");
			}
			
			code.append("public void set").append(org.springframework.util.StringUtils.capitalize(field.getApiName())).append(" (").append(javaType).append(" arg) {}");
		}
		
		return code.toString();
	}
}
