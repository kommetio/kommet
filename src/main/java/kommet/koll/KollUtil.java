/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.springframework.util.StringUtils;

import kommet.basic.CustomTypeRecordProxy;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.StandardObjectController;
import kommet.config.Constants;
import kommet.dao.queries.QueryResult;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.koll.annotations.Action;
import kommet.koll.annotations.ActionConfig;
import kommet.koll.annotations.Auth;
import kommet.koll.annotations.Controller;
import kommet.koll.annotations.CrossOrigin;
import kommet.koll.annotations.DataType;
import kommet.koll.annotations.Disabled;
import kommet.koll.annotations.Field;
import kommet.koll.annotations.Header;
import kommet.koll.annotations.Param;
import kommet.koll.annotations.Params;
import kommet.koll.annotations.Public;
import kommet.koll.annotations.RequestBody;
import kommet.koll.annotations.ResponseBody;
import kommet.koll.annotations.Rest;
import kommet.koll.annotations.SharingRule;
import kommet.koll.annotations.Type;
import kommet.koll.annotations.View;
import kommet.koll.annotations.triggers.AfterDelete;
import kommet.koll.annotations.triggers.AfterInsert;
import kommet.koll.annotations.triggers.AfterUpdate;
import kommet.koll.annotations.triggers.BeforeDelete;
import kommet.koll.annotations.triggers.BeforeInsert;
import kommet.koll.annotations.triggers.BeforeUpdate;
import kommet.koll.annotations.triggers.Trigger;
import kommet.testing.Test;
import kommet.triggers.DatabaseTrigger;
import kommet.utils.MiscUtils;

public class KollUtil
{
	public static String getTemplateCode (String name, String userPackageName, EnvData env)
	{
		return getTemplateKollCode(name, userPackageName, null, null, null, env);
	}
	
	public static boolean isValidClassPackageName (String packageName)
	{
		return !packageName.startsWith(Constants.KOMMET_BASE_PACKAGE + ".");
	}
	
	/**
	 * Replace all DAL query in Java code with DAL query calls.
	 * @param code
	 * @return
	 */
	public static String replaceDALQueries(String code, String queryMethod)
	{
		//String simpleToken = "[^.;?!\\s\"]+";
		//String quotedToken = "(?:\"\\\\.|[^\\\\\r\n\"]*\")";
		//String queryToken = "(\\{\\s*(select\\s+[^\\}]+)\\})";
		//String varToken = "(#[A-z\\_\\.\\(\\)0-9]+)";
		
		//Pattern queryExactPattern = Pattern.compile(queryToken);
		//Pattern varExactPattern = Pattern.compile(varToken);
		//Pattern dalQueryPattern = Pattern.compile(quotedToken + "|" + queryToken);
		//Pattern kollVarPattern = Pattern.compile(quotedToken + "|" + varToken);
		//Pattern dalQueryPattern = Pattern.compile("\\{\\s*(select\\s+[^\\}]+)\\}(?=((\\[\\\"]|[^\\\"])*\"(\\[\\\"]|[^\\\"])*\")*(\\[\\\"]|[^\\\"])*$)");
		
		Pattern dalQueryPattern = Pattern.compile(MiscUtils.toNonQuoteRegex("\\{\\s*(select\\s+[^\\}]+)\\}"));
		Pattern kollVarPattern = Pattern.compile(MiscUtils.toNonQuoteRegex("#[A-z\\_\\.\\(\\)0-9]+"));
		Matcher m = dalQueryPattern.matcher(code);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{	
			String token = m.group(0);
			
			// ignore quotes strings
			if (token.startsWith("\"") && token.endsWith("\""))
			{
				continue;
			}
			
			String query = m.group(2).trim();
			
			Matcher varMatcher = kollVarPattern.matcher(query);
			StringBuffer varRewrite = new StringBuffer();
			
			while (varMatcher.find())
			{
				String varToken = varMatcher.group(0);
				
				// ignore quotes strings
				if (varToken.startsWith("\"") && varToken.endsWith("\""))
				{
					continue;
				}
				
				varMatcher.appendReplacement(varRewrite, "\" + " + varMatcher.group(1).trim().substring(1) + " + \"");
			}
			
			varMatcher.appendTail(varRewrite);
			
			query = varRewrite.toString();
			
			// if query ended with an infix var, e.g. "select id from User where id = #someId", then after the replacement
			// the string will end with a single quote
			// in this case we just close the string
			
			m.appendReplacement(sb, queryMethod + "(\"" + query + "\")");
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	public static String getTemplateKollCode (String name, String userPackageName, List<String> imports, String annotations, String innerCode, EnvData env)
	{
		StringBuilder code = new StringBuilder();
		code.append("package ").append(userPackageName).append(";\n\n");
		addImports(code);
		
		if (imports != null)
		{
			for (String importedItem : imports)
			{
				addImport(importedItem, code);
			}
		}
		
		if (StringUtils.hasText(annotations))
		{
			code.append(annotations).append("\n");
		}
		else
		{
			code.append("\n");
		}
		
		code.append("public class ").append(name).append("\n{\n");
		
		if (StringUtils.hasText(innerCode))
		{
			code.append(innerCode);
		}
		
		code.append("}");
		return code.toString();
	}
	
	public static List<Class<?>> getAnnotations()
	{
		List<Class<?>> annotations = new ArrayList<Class<?>>();
		
		annotations.add(Controller.class);
		annotations.add(Action.class);
		annotations.add(ActionConfig.class);
		annotations.add(Public.class);
		annotations.add(Param.class);
		annotations.add(Params.class);
		annotations.add(Rest.class);
		annotations.add(CrossOrigin.class);
		annotations.add(Header.class);
		annotations.add(ResponseBody.class);
		annotations.add(RequestBody.class);
		annotations.add(Auth.class);
		annotations.add(View.class);
		annotations.add(Disabled.class);
		annotations.add(Type.class);
		annotations.add(Field.class);
		annotations.add(DataType.class);
		annotations.add(SharingRule.class);
		annotations.add(Test.class);
		
		// add trigger annotations
		annotations.add(Trigger.class);
		annotations.add(BeforeInsert.class);
		annotations.add(BeforeUpdate.class);
		annotations.add(BeforeDelete.class);
		annotations.add(AfterInsert.class);
		annotations.add(AfterUpdate.class);
		annotations.add(AfterDelete.class);
		
		// business process
		annotations.add(kommet.businessprocess.annotations.BusinessAction.class);
		annotations.add(kommet.businessprocess.annotations.Execute.class);
		annotations.add(kommet.businessprocess.annotations.Input.class);
		annotations.add(kommet.businessprocess.annotations.Output.class);
		
		return annotations;
	}
	
	public static List<String> getImports()
	{
		List<String> imports = new ArrayList<String>();
		
		for (Class<?> annot : getAnnotations())
		{
			imports.add(annot.getName());
		}
	
		imports.add(DatabaseTrigger.class.getName());
		
		imports.add(KeyPrefix.class.getName());
		imports.add(KID.class.getName());
		imports.add(Record.class.getName());
		imports.add(QueryResult.class.getName());
		imports.add(KommetException.class.getName());
		imports.add(PageData.class.getName());
		imports.add(KeyPrefixException.class.getName());
		imports.add(StandardObjectController.class.getName());
		imports.add(BaseController.class.getName());
		imports.add(CustomTypeRecordProxy.class.getName());
		
		return imports;
	}
	
	public static void addImports(StringBuilder code)
	{
		for (String importedClass : getImports())
		{
			addImport(importedClass, code);
		}
	}
	
	private static void addImport(String importedItem, StringBuilder code)
	{
		code.append("import ").append(importedItem).append(";\n");
	}
	
	public static String extractPackageName (String code) throws InvalidClassCodeException
	{
		if (!StringUtils.hasText(code))
		{
			throw new InvalidClassCodeException("Empty class code");
		}
		
		if (!code.startsWith("package"))
		{
			throw new InvalidClassCodeException("Class code must start with package definition");
		}
		
		return code.substring("package".length(), code.indexOf(';')).trim();
	}

	public static void validateKollCode(String code) throws InvalidClassCodeException
	{
		String packageName = extractPackageName(code);
		if (packageName.startsWith("kommet.envs."))
		{
			throw new InvalidClassCodeException("Class code contains env-specific package name " + packageName);
		}
	}
	
	private static ASTVisitor getClassNameDeductionAstVisitor(EnvData env, AST ast, final CompilationUnit cu, final kommet.basic.Class cls)
	{
		return new ASTVisitor()
	    {
			public boolean visit(TypeDeclaration decl)
	        {
				cls.setName(decl.getName().toString());
				
				if (decl.getParent() instanceof CompilationUnit)
				{
					try
					{
						cls.setPackageName(((CompilationUnit)decl.getParent()).getPackage().getName().toString());
					}
					catch (KommetException e)
					{
						// ignore
					}
				}
				return false;
			}
	    };
	}

	/**
	 * Deduces class and package name based on the passed class code.
	 * @param code
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static kommet.basic.Class getClassFromCode(String code, EnvData env) throws KommetException
	{
		ASTParser parser = ASTParser.newParser(AST.JLS11);
		parser.setStatementsRecovery(true);
		parser.setBindingsRecovery(true);
		parser.setResolveBindings(true);
		parser.setUnitName("AnyName.java");
		
		parser.setCompilerOptions(JavaCore.getOptions());
		parser.setEnvironment(new String[] { env.getCompileClassPath() }, null, null, true);
		
		parser.setSource(code.toCharArray());
		
		final CompilationUnit cu = (CompilationUnit)parser.createAST(null);
		
		AST ast = cu.getAST();
		
		kommet.basic.Class cls = new kommet.basic.Class();
		
		cu.accept(getClassNameDeductionAstVisitor(env, ast, cu, cls));
		
		return cls;
	}
}