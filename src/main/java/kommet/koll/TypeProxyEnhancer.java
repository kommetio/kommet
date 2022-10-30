/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.springframework.util.StringUtils;

import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.persistence.Property;
import kommet.utils.MiscUtils;

public class TypeProxyEnhancer
{
	private static Set<String> systemFieldAccessors;
	
	
	@SuppressWarnings("unchecked")
	public static void addGetter(ASTRewrite rewrite, AST ast, TypeDeclaration decl, String fieldName, String returnTypeName) throws KommetException
	{
		MethodDeclaration getter = ast.newMethodDeclaration();
		getter.setConstructor(false);
		getter.setName(ast.newSimpleName("get" + StringUtils.capitalize(fieldName)));
		getter.setReturnType2(getType(ast, returnTypeName));
		rewrite.getListRewrite(decl, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertLast(getter, null);
		
		Block code = ast.newBlock();
		
		ReturnStatement rs = ast.newReturnStatement();
		rs.setExpression(ast.newSimpleName(fieldName));
       	code.statements().add(rs);
		
		// add empty body to the method
		getter.setBody(code);
		
		// annotate the getter with @Property
		Map<String, Object> annotValues = new HashMap<String, Object>();
		annotValues.put("field", fieldName);
		NormalAnnotation propertyAnnotation = getNormalAnnotation(ast, Property.class, annotValues);
		rewrite.getListRewrite(getter, MethodDeclaration.MODIFIERS2_PROPERTY).insertLast(propertyAnnotation, null);
	}
	
	@SuppressWarnings("unchecked")
	public static NormalAnnotation getNormalAnnotation(AST ast, java.lang.Class<Property> cls, Map<String, Object> values)
	{
		NormalAnnotation annot = ast.newNormalAnnotation();
		annot.setTypeName(ast.newName(cls.getName()));
		
		for (String attr : values.keySet())
		{
			MemberValuePair mvp = ast.newMemberValuePair();
            mvp.setName(ast.newSimpleName(attr));
            Expression exp = ast.newStringLiteral();
            
            Object attrVal = values.get(attr);
            
        	if (attrVal instanceof String)
        	{
        		((StringLiteral)exp).setLiteralValue((String)attrVal);
        	}
        	else
        	{
        		throw new IllegalArgumentException("Unsupported annotation attribute type " + attrVal.getClass().getName());
        	}
            
            mvp.setValue(exp);
            annot.values().add(mvp);
		}
		
		return annot;
	}

	@SuppressWarnings("unchecked")
	public static void addFieldDeclaration(ASTRewrite rewrite, AST ast, TypeDeclaration decl, String name, String typeName, EnvData env) throws KommetException
	{
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(name));
		
		FieldDeclaration field = ast.newFieldDeclaration(fragment);
		field.modifiers().addAll(ast.newModifiers(Modifier.PRIVATE));
			
		field.setType(getType(ast, typeName));
		
		// append field declaration to the class body
		rewrite.getListRewrite(decl, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertFirst(field, null);
	}
	
	public static Type getType (AST ast, String qualifiedName) throws KommetException
	{
		List<String> typeNameParts = MiscUtils.splitByLastDot(qualifiedName);
		return ast.newSimpleType(ast.newQualifiedName(ast.newName(typeNameParts.get(0)), ast.newSimpleName(typeNameParts.get(1))));
	}

	@SuppressWarnings("unchecked")
	public static void addSetter(ASTRewrite rewrite, AST ast, TypeDeclaration decl, String fieldName, String typeName) throws KommetException
	{
		MethodDeclaration setter = ast.newMethodDeclaration();
		setter.setConstructor(false);
		setter.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		setter.setName(ast.newSimpleName("set" + StringUtils.capitalize(fieldName)));
		rewrite.getListRewrite(decl, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertLast(setter, null);
		
		// add parameters
		SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
		param.setType(getType(ast, typeName));
		param.setName(ast.newSimpleName("arg"));
		setter.parameters().add(param);
		
		Block code = ast.newBlock();
		
		Assignment a = ast.newAssignment();
		a.setLeftHandSide(ast.newSimpleName(fieldName));
		a.setRightHandSide(ast.newSimpleName("arg"));
       	code.statements().add(ast.newExpressionStatement(a));
       	
       	// add call to set initialized
       	MethodInvocation methodCall = ast.newMethodInvocation();
		methodCall.setName(ast.newSimpleName("setInitialized"));
		Statement initializerStatement = ast.newExpressionStatement(methodCall);
		code.statements().add(initializerStatement);
		
		// add empty body to the method
		setter.setBody(code);
	}

	public static boolean isSystemFieldAccessor(SimpleName name)
	{
		return getSystemFieldAccessors().contains(name);
	}

	private static Set<String> getSystemFieldAccessors()
	{
		if (systemFieldAccessors == null)
		{
			systemFieldAccessors = new HashSet<String>();
			//systemFieldAccessors.add()
		}
		
		return systemFieldAccessors;
	}

}