/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.validationrules;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyType;
import kommet.basic.RecordProxyUtil;
import kommet.basic.ValidationRule;
import kommet.dao.KommetPersistenceException;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.validationrules.executors.ValidationRuleExecutor;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetCompiler;
import kommet.rel.RELParser;
import kommet.utils.MiscUtils;

public class ValidationRuleUtil
{
	private static final String RECORD_VAR = "record";
	private static final String VRE_NAME_PREFIX = "ValidationRuleExecutor_";
	private static final String VRE_PACKAGE = "kommet.validationrules.executors";
	private static final String VRE_METHOD_PREFIX = "runValidationRule_";
	
	public static String getValidationRuleMethod (ValidationRule rule, String methodName, boolean isPublic, java.lang.Class<? extends RecordProxy> proxyClass, String recordVar, Type type, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder("\t");
		code.append(isPublic ? "public" : "private").append(" ").append(ValidationRuleError.class.getSimpleName()).append(" ").append(methodName).append("(").append(proxyClass.getName()).append(" ").append(recordVar).append(", String mode)");
		code.append(" throws ").append(KommetException.class.getSimpleName()).append("\n");
		code.append("{\n");
		
		StringBuilder returnErrCode = getReturnErrorCode(rule, null);
		
		// build a condition that checks if all fields used by this rule have been initialized
		code.append("boolean fieldsInitialized = ").append(getFieldsInitializedCondition(rule, recordVar, env)).append(";");
		code.append("if (!ValidationRuleUninitializedFieldsMode.EVALUATE.getMode().equals(mode) && !fieldsInitialized)\n{");
		code.append("if (ValidationRuleUninitializedFieldsMode.IGNORE.getMode().equals(mode)) { return null; }\n");
		code.append("else if (ValidationRuleUninitializedFieldsMode.FAIL.getMode().equals(mode)) { ").append(getReturnErrorCode(null, "Fields used in the validation rule " + rule.getName() + " are not set, so the rule cannot be evaluated")).append(" }\n");
		code.append("else { throw new KommetException(\"Mode \" + mode + \" not supported\"); }");
		code.append("\n}\n");
		
		code.append("\n\t\tif (");
		code.append(RELParser.relToJava(rule.getCode(), recordVar, type, true, true, env)).append(") { return null; } else { ");
		code.append(returnErrCode);
		code.append("\t}\n}\n");
		return code.toString();
	}
	
	private static StringBuilder getReturnErrorCode(ValidationRule rule, String err)
	{
		StringBuilder returnErrCode = new StringBuilder();
		returnErrCode.append(ValidationRuleError.class.getSimpleName()).append(" result = new ").append(ValidationRuleError.class.getSimpleName()).append("();");
		
		if (StringUtils.hasText(err))
		{
			returnErrCode.append("\t\tresult.setMessage(\"").append(err).append("\");");
		}
		else
		{
			// we need to escape the error message in case it contains quotes, as it will be used as a string literal
			// in the VR executor
			returnErrCode.append("\t\tresult.setMessage(").append(StringUtils.hasText(rule.getErrorMessage()) ? "\"" + StringEscapeUtils.escapeJava(rule.getErrorMessage()) + "\"" : "null").append(");\n");
			
			returnErrCode.append("\t\tresult.setMessageLabel(").append(StringUtils.hasText(rule.getErrorMessageLabel()) ? "\"" + rule.getErrorMessageLabel() + "\"" : "null").append(");\n");
		}
		
		returnErrCode.append("\t\treturn result;\n");
		
		return returnErrCode;
	}
	
	private static String getFieldsInitializedCondition(ValidationRule rule, String proxyVar, EnvData env) throws KommetException
	{
		List<String> conditionParts = new ArrayList<String>();
		List<String> fieldIds = MiscUtils.splitAndTrim(rule.getReferencedFields(), ",");
		Type type = env.getType(rule.getTypeId());
		
		for (String sFieldId : fieldIds)
		{
			Field field = type.getField(KID.get(sFieldId));
			if (field == null)
			{
				throw new KommetException("Field " + sFieldId + " not found for validation rule " + rule.getId());
			}
			conditionParts.add(proxyVar + ".isSet(\"" + field.getApiName() + "\")");
		}
		
		return MiscUtils.implode(conditionParts, " && ");
	}

	public static Class getValidationRuleExecutor(Type type, String executorClassName, Set<ValidationRule> vrs, KommetCompiler compiler, EnvData env) throws KommetException
	{
		if (!StringUtils.hasText(executorClassName))
		{
			executorClassName = VRE_NAME_PREFIX + type.getKeyPrefix();
		}
		
		String code = getValidationRuleExecutorCode(type, executorClassName, vrs, compiler, env);
		//System.out.println(code);
		Class file = new Class();
		file.setName(executorClassName);
		file.setPackageName(VRE_PACKAGE);
		file.setKollCode(code);
		file.setJavaCode(code);
		file.setIsSystem(true);
		return file;
	}

	/**
	 * Generates code of a ValidationRuleExecutor for the given type.
	 * @param type
	 * @param vrs
	 * @param compiler
	 * @param env
	 * @return
	 * @throws KommetException 
	 */
	private static String getValidationRuleExecutorCode(Type type, String executorClassName, Set<ValidationRule> vrs, KommetCompiler compiler, EnvData env) throws KommetException
	{
		StringBuilder sb = new StringBuilder();
		sb.append("package ").append(VRE_PACKAGE).append(";\n\n");
		sb.append("import ").append(List.class.getName()).append(";\n");
		sb.append("import ").append(HashSet.class.getName()).append(";\n");
		sb.append("import ").append(Set.class.getName()).append(";\n");
		sb.append("import ").append(String.class.getName()).append(";\n");
		sb.append("import ").append(KommetException.class.getName()).append(";\n");
		sb.append("import ").append(ValidationRuleError.class.getName()).append(";\n\n");
		sb.append("import ").append(ValidationRuleUninitializedFieldsMode.class.getName()).append(";\n\n");
		
		
		// start class body
		sb.append("public class ").append(executorClassName).append(" implements ");
		sb.append(ValidationRuleExecutor.class.getName()).append("<").append(MiscUtils.userToEnvPackage(type.getQualifiedName(), env)).append(">\n{\n");
		
		java.lang.Class<? extends RecordProxy> proxyClass = null;
		
		try
		{
			proxyClass = RecordProxyUtil.getProxyClass(type, RecordProxyType.CUSTOM, env);
		}
		catch (KommetPersistenceException e)
		{
			throw new ValidationRuleException("Error getting object proxy class for type " + type.getQualifiedName() + ": " + e.getMessage());
		}
		
		List<String> methodNames = new ArrayList<String>();
		
		for (ValidationRule vr : vrs)
		{
			if (vr.getReferencedFields() == null || vr.getReferencedFields().isEmpty())
			{
				throw new ValidationRuleException("Cannot compile validation rule because the list of referenced fields is not initialized, or is empty");
			}
			
			methodNames.add(VRE_METHOD_PREFIX + vr.getId());
			sb.append(getValidationRuleMethod(vr, VRE_METHOD_PREFIX + vr.getId(), false, proxyClass, RECORD_VAR, type, env));
		}
		
		String uninitFieldsModeVar = "uninitFieldsMode"; 
		
		// add execute method
		sb.append("\tpublic Set<").append(ValidationRuleError.class.getName()).append("> execute (").append(proxyClass.getName()).append(" obj, String ").append(uninitFieldsModeVar).append(") "); 
		sb.append(" throws ").append(KommetException.class.getName()).append("\n\t{");
		sb.append("\t\tSet<").append(ValidationRuleError.class.getSimpleName()).append("> errors = new ").append(HashSet.class.getSimpleName()).append("<").append(ValidationRuleError.class.getSimpleName()).append(">();\n");
		sb.append(ValidationRuleError.class.getSimpleName()).append(" err = null;\n");
		
		for (String methodName : methodNames)
		{
			sb.append("\t\t").append("err = ").append(methodName).append("(obj, ").append(uninitFieldsModeVar).append(");\n");
			sb.append("\t\tif (err != null) { errors.add(err); }\n");
		}
		sb.append("\t\treturn errors;\n");
		sb.append("\t}");
		
		// end class body
		sb.append("\n}");
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static Set<ValidationRuleError> runValidationRules(RecordProxy obj, String uninitFieldsMode, KeyPrefix keyPrefix, KommetCompiler compiler, AuthData authData, EnvData env) throws KommetException
	{
		java.lang.Class<?> vre = null;
		try
		{
			vre = compiler.getClass(VRE_PACKAGE + "." + VRE_NAME_PREFIX + keyPrefix, false, env);
		}
		catch (ClassNotFoundException e)
		{
			throw new ValidationRuleException("VRE not found for type with prefix " + keyPrefix);
		}
		
		java.lang.Class<?> proxyType = RecordProxyUtil.getProxyClass(env.getType(keyPrefix), RecordProxyType.CUSTOM, env);

		ValidationRuleExecutor<? extends RecordProxy> vreInstance = null;
		
		try
		{
			vreInstance = (ValidationRuleExecutor<? extends RecordProxy>)vre.newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new ValidationRuleException("Error instantiating VRE: " + e.getMessage());
		}
		
		Method executeMethod;
		try
		{
			executeMethod = vre.getMethod("execute", compiler.getClass(proxyType.getName(), false, env), String.class);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new ValidationRuleException("Execute method not found on VRE (looked for execute(" + proxyType.getName() + "))");
		}
		
		try
		{
			return (Set<ValidationRuleError>)executeMethod.invoke(vreInstance, obj, uninitFieldsMode != null ? uninitFieldsMode : "ignore");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new ValidationRuleException("Error calling validation rule " + e.getMessage());
		}
	}
}