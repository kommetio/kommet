/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.MiscUtils;

public class RecordProxyClassGenerator
{	
	private static String getProxyClassKollCode (Type type, boolean generatePersistentAnnotations, boolean generateNestedProxies, EnvData env) throws RecordProxyException
	{
		StringBuilder code = new StringBuilder();
		
		code.append("package " + type.getPackage() + ";").append("\n\n");
		
		addImports(code);
		
		// begin class
		if (generatePersistentAnnotations)
		{
			code.append("@" + Entity.class.getSimpleName()).append("(type = ").append("\"" + type.getQualifiedName() + "\"").append(")\n");
		}
		
		code.append("public class " + type.getApiName()).append(" extends ").append(CustomTypeRecordProxy.class.getName());
		code.append("\n{\n");
		
		// add default constructor
		addDefaultConstructor(type.getApiName(), code);
		
		for (Field field : type.getFields())
		{
			// Generate getter/setter for property, unless it's a system property created on type creation,
			// e.g. lastModifiedDate.
			if (!Field.isSystemField(field.getApiName()) || Field.LAST_MODIFIED_BY_FIELD_NAME.equals(field.getApiName()) || Field.CREATEDBY_FIELD_NAME.equals(field.getApiName()))
			{
				if (field.getDataType().isPrimitive() || generateNestedProxies)
				{
					generateProperty(code, generatePersistentAnnotations, field, env);
				}
			}
		}
		
		// end class
		code.append("\n}");
		
		return code.toString();
	}

	private static void addImports(StringBuilder code)
	{
		addImport(RecordProxyException.class.getName(), code);
		addImport(Entity.class.getName(), code);
		addImport(Property.class.getName(), code);
	}

	private static void addImport(String importedItem, StringBuilder code)
	{
		code.append("import ").append(importedItem).append(";\n");
	}

	private static void addDefaultConstructor(String className, StringBuilder code)
	{
		code.append("public ").append(className).append("() throws ").append(RecordProxyException.class.getSimpleName()).append("\n{\n");
		code.append("super();\n");
		code.append("}\n");
	}

	private static void generateProperty (StringBuilder code, boolean generatePersistentAnnotations, Field field, EnvData env) throws RecordProxyException
	{
		String javaType = null;
		
		// If the type is type reference or inverse collection, their java type will be a reference to
		// a proxy class. If the proxy class is a user-defined type, the package of the proxy will be the
		// same as the package of the type. However, with system types packages may differ. E.g. type 'User' has
		// standard package 'kommet.basic', but the User proxy is in package kommet.basic.proxies.
		// To be sure we have the correct package name, we need to get the proxy class from env mappings.
		// Otherwise we may end up with unresolved reference to proxy type during compilation.
		if (field.getDataType().isPrimitive())
		{
			javaType = field.getDataType().getJavaType();
		}
		else if (field.getDataType().getId().equals(DataType.TYPE_REFERENCE))
		{
			Type refType = ((TypeReference)field.getDataType()).getType();
			
			// if the class references itself, we cannot get its proxy from env mappings, because
			// it is possible that the class is being created with its fields, so at the time of building
			// its proxy the proxy is not yet registered with the env
			if (refType.getKID().equals(field.getType().getKID()))
			{
				javaType = MiscUtils.userToEnvPackage(field.getType().getQualifiedName(), env);
			}
			else
			{
				javaType = MiscUtils.userToEnvPackage(refType.getQualifiedName(), env);
			}
		}
		else if (field.getDataType().getId().equals(DataType.INVERSE_COLLECTION))
		{
			Type refType = ((InverseCollectionDataType)field.getDataType()).getInverseType();
			
			// if the class references itself, we cannot get its proxy from env mappings, because
			// it is possible that the class is being created with its fields, so at the time of building
			// its proxy the proxy is not yet registered with the env
			
			if (refType.getKID().equals(field.getType().getKID()))
			{
				javaType = "java.util.ArrayList<" + MiscUtils.userToEnvPackage(field.getType().getQualifiedName(), env) + ">";
			}
			else
			{
				javaType = "java.util.ArrayList<" + MiscUtils.userToEnvPackage(refType.getQualifiedName(), env) + ">";
			}
		}
		else if (field.getDataType().getId().equals(DataType.ASSOCIATION))
		{
			Type refType = ((AssociationDataType)field.getDataType()).getAssociatedType();
			
			// if the class references itself, we cannot get its proxy from env mappings, because
			// it is possible that the class is being created with its fields, so at the time of building
			// its proxy the proxy is not yet registered with the env
			if (refType.getKID().equals(field.getType().getKID()))
			{
				javaType = "java.util.ArrayList<" + MiscUtils.userToEnvPackage(field.getType().getQualifiedName(), env) + ">";
			}
			else
			{
				javaType = "java.util.ArrayList<" + MiscUtils.userToEnvPackage(refType.getQualifiedName(), env) + ">";
			}
		}
		
		if (javaType == null)
		{
			throw new RecordProxyException("Could not determine Java type for field " + field.getApiName() + " with data type " + field.getDataType().getName());
		}
		
		code.append("\tprivate ").append(javaType);
		code.append(" ");
		code.append(field.getApiName());
		code.append(";\n");
		
		// add getter
		if (generatePersistentAnnotations)
		{
			code.append("\t@").append(Property.class.getSimpleName()).append("(field = \"").append(field.getApiName()).append("\"").append(")\n");
		}
		code.append("\tpublic " + javaType + " get" + StringUtils.capitalize(field.getApiName()) + "()\n{");
		code.append("\t\treturn this." + field.getApiName() + ";");
		code.append("\n\t}\n");
		
		// add setter
		code.append("\tpublic void set" + StringUtils.capitalize(field.getApiName()) + "(" + javaType + " " + field.getApiName() + ")\n{");
		code.append("\t\tthis." + field.getApiName() + " = " + field.getApiName() + ";\n");
		code.append("setInitialized(\"" + field.getApiName() + "\");\n");
		code.append("\n\t}");
		
		// add nullifier
		/*code.append("\tpublic void set" + StringUtils.capitalize(field.getApiName()) + "(" + SpecialValue.class.getName() + " specialNull)\n{");
		code.append("\t\tthis." + field.getApiName() + " = null;\n");
		code.append("\t\tnullify(\"").append(field.getApiName()).append(");\n");
		code.append("setInitialized(\"" + field.getApiName() + "\");\n");
		code.append("\n\t}");*/
	}

	public static Class getProxyKollClass (Type type, boolean generateNestedProxies, boolean generatePersistentAnnotations, ClassService classService, AuthData authData, EnvData env) throws KommetException
	{
		Class cls = new Class();
		cls.setIsSystem(false);
		
		String kollCode = getProxyClassKollCode(type, generatePersistentAnnotations, generateNestedProxies, env);
		
		cls.setKollCode(kollCode);
		
		// do not interprete KOLL for proxy classes
		cls.setJavaCode(classService.getKollTranslator(env).kollToJava(kollCode, false, authData, env));
		
		try
		{
			cls.setPackageName(type.getPackage());
		}
		catch (KommetException e)
		{
			throw new RecordProxyException(e.getMessage());
		}
		cls.setName(type.getApiName());
		
		return cls;
	}
}