/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.RecordAccessType;
import kommet.data.KeyPrefixException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.koll.KollUtil;

public class StandardTypeControllerUtil
{
	public static Class getController (Type type, ClassService classService, AuthData authData, EnvData env) throws KommetException
	{
		Class cls = new Class();
		cls.setIsSystem(true);
		
		String code = getStandardControllerCode(type, env);
		
		cls.setKollCode(code);
		cls.setJavaCode(classService.getKollTranslator(env).kollToJava(code, false, authData, env));
		cls.setPackageName(type.getPackage());
		cls.setName(getStandardControllerSimpleName(type));
		
		// access type has to be SYSTEM, even for system types, because the controller is recompiled when the type changes
		cls.setAccessType(RecordAccessType.SYSTEM.getId());
		
		return cls;
	}

	private static String getStandardControllerCode(Type type, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		code.append("package ").append(type.getPackage()).append(";\n\n");
		
		KollUtil.addImports(code);
		
		// since the standard controller will be placed in the same package as the type
		// we can use the same name for the controller as for the class, with only "Controller" appended,
		// without fearing duplicate names. We don't need to use a fully qualified name.
		String controllerName = getStandardControllerSimpleName(type);
		
		code.append("@Controller\n");
		code.append("public class ").append(controllerName).append(" extends ").append(StandardObjectController.class.getName());
		code.append("\n{\n");
		
		// create constructor that calls the parent constructor from class StandardObjectController
		code.append("public ").append(controllerName).append("() throws ").append(KeyPrefixException.class.getSimpleName()).append("\n");
		code.append("{ ").append("super(KeyPrefix.get(\"").append(type.getKeyPrefix().getPrefix()).append("\")); }\n");
		code.append("\n}\n");
		
		return code.toString();
	}
	
	public static String getStandardControllerSimpleName (Type type)
	{
		return type.getApiName() + "Controller";
	}
	
	public static String getStandardControllerQualifiedName (Type type)
	{
		return type.getPackage() + "." + getStandardControllerSimpleName(type);
	}
}
