/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.CustomTypeRecordProxy;
import kommet.basic.RecordProxyException;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.basic.types.SystemTypes;
import kommet.config.Constants;
import kommet.config.UserSettingKeys;
import kommet.dao.dal.DALSyntaxException;
import kommet.dao.queries.Criteria;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.annotations.QueriedTypes;
import kommet.koll.annotations.Setter;
import kommet.koll.annotations.SharedWith;
import kommet.koll.annotations.SharingRule;
import kommet.koll.annotations.SystemContextVar;
import kommet.koll.compiler.KommetClassLoader;
import kommet.koll.compiler.KommetCompiler;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.MiscUtils;

public class SimpleKollTranslator implements KollJavaTranslator
{
	private KommetClassLoader classLoader;
	private UserCascadeHierarchyService uchService;
	
	public SimpleKollTranslator (KommetClassLoader classLoader, UserCascadeHierarchyService uchService)
	{
		this.classLoader = classLoader;
		this.uchService = uchService;
	}
	
	@Override
	public Class kollToJava (Class file, boolean isInterpreteKoll, AuthData authData, EnvData env) throws KollParserException
	{
		if (file == null)
		{
			throw new KollParserException("Class file is null");
		}
		
		if (!StringUtils.hasText(file.getKollCode()))
		{
			throw new KollParserException("Koll code in class file is null or empty");
		}
		
		if (env == null)
		{
			throw new KollParserException("Env is null in Koll file");
		}
		
		file.setJavaCode(kollToJava(file.getKollCode(), isInterpreteKoll, authData, env));
		return file;
	}
	
	@Override
	public String kollToJava(String code, boolean isInterpreteKoll, AuthData authData, EnvData env) throws KollParserException
	{
		code = replacePackageName(code, env.getId());
		
		String mockQueryMethod = "dalQuery" + (new Random()).nextInt(1000000);
		
		if (isInterpreteKoll)
		{
			// avoid interpreting KOLL when possible, because it is time consuming, especially the use
			// of ASTVisitor
			code = KollUtil.replaceDALQueries(code, mockQueryMethod);
			try
			{
				code = replaceTypeDeclarations(code, mockQueryMethod, authData, env);
			}
			catch (KommetException e)
			{
				throw new KollParserException(e.getMessage());
			}
			catch (AstIllegalArgumentException e)
			{
				if (e.isCompiledError())
				{
					throw new KollParserException(e.getMessage());
				}
				else
				{
					throw e;
				}
			}
		}
		
		return code;
	}

	private ASTVisitor getASTVisitor (final EnvData env, final AST ast, final CompilationUnit cu, final ASTRewrite rewrite, final String mockQueryMethod, final String argSysCtxVar)
	{	
		return new ASTVisitor()
	    {	
			private String actualSysCtxVar = argSysCtxVar;
			
			// tell whether SystemContextVar annotation was added in the source code the user
			private boolean isSysCtxVarAnnotationPresent = false;
			
			private boolean isEnhanceProxy = false;
			private MethodDeclaration defaultConstructor;
			private Map<String, MethodDeclaration> setters = new HashMap<String, MethodDeclaration>();
			private List<MethodDeclaration> potentialSetters = new ArrayList<MethodDeclaration>();
			private Map<String, String> fieldJavaTypes = new HashMap<String, String>();
			
			// for each method (the key is the method referenced), list a set of names of types which are queried within this method
			private Map<MethodDeclaration, Set<String>> queriedTypesByMethod = new HashMap<MethodDeclaration, Set<String>>();
			
			public boolean visit(QualifiedName state)
	        {
				String qualifiedName = state.getFullyQualifiedName();
				
				try
				{
					if (!MiscUtils.isEnvSpecific(qualifiedName))
					{
						if (env.getType(qualifiedName) != null)
						{
							kommet.data.Type type = env.getType(qualifiedName);
							
							// do not translate system types, unless they are annotated with @Entity and are accessible to users
							if (!type.isBasic() || !SystemTypes.isInaccessibleSystemType(type))
							{
								String updatedPackageName = MiscUtils.userToEnvPackage(type.getPackage(), env);
								rewrite.replace(state, ast.newQualifiedName(ast.newName(updatedPackageName), ast.newSimpleName(type.getApiName())), null);
							}
							else if (type.isBasic() && SystemTypes.isInaccessibleSystemType(type))
							{
								throw new IllegalArgumentException("Cannot import system type " + type.getQualifiedName());
							}
						}
						else
						{
							// try to convert the class name to env-specific and see, it such class exists on the env
							String updatedPackageName = MiscUtils.userToEnvPackage(qualifiedName, env);
							java.lang.Class<?> cls;
							try
							{
								cls = java.lang.Class.forName(updatedPackageName, true, classLoader);
								
								// if ClassNotFoundException is not thrown, this means that the class exists
								rewrite.replace(state, ast.newQualifiedName(ast.newName(cls.getPackage().getName()), ast.newSimpleName(cls.getSimpleName())), null);
							}
							catch (ClassNotFoundException e)
							{
								// ignore
							}
						}
					}
				}
				catch (KommetException e)
				{
					e.printStackTrace();
				}
				
	            return true;
	        }
			
			public boolean visit (MethodInvocation call)
			{
				MethodDeclaration enclosingMethod = getEnclosingMethod(call);
				
				if (call.getName().getIdentifier().equals(mockQueryMethod))
				{
					if (isInInnerClass(call))
					{
						throw new IllegalArgumentException("DAL query cannot be placed within an inner class");
					}
					
					if (call.getParent() instanceof ReturnStatement)
					{
						MethodDeclaration methodDeclaration = getEnclosingMethod(call);
						if (methodDeclaration == null)
						{
							throw new IllegalArgumentException("DAL query placed next to return keyword, but not in a method declaration");
						}
						
						String queryMethod = getQueryMethodByBoundType(methodDeclaration.getReturnType2());
						replaceQueryMethodCall(call, enclosingMethod, queryMethod);
					}
					else if (call.getParent().getParent() instanceof VariableDeclarationStatement)
					{
						VariableDeclarationStatement decl = (VariableDeclarationStatement)call.getParent().getParent();
						String queryMethod = getQueryMethodByBoundType(decl.getType());
						
						replaceQueryMethodCall(call, enclosingMethod, queryMethod);
					}
					else if (call.getParent().getParent() instanceof ExpressionStatement && ((ExpressionStatement)call.getParent().getParent()).getExpression() instanceof Assignment)
					{
						ITypeBinding binding = (ITypeBinding)((Assignment)((ExpressionStatement)call.getParent().getParent()).getExpression()).getLeftHandSide().resolveTypeBinding();
						
						String queryMethod = null;
						if (binding.getTypeDeclaration() != null && binding.getTypeDeclaration().getQualifiedName().equals(java.util.List.class.getName()))
						{
							queryMethod = "query";
						}
						else
						{
							queryMethod = "queryUniqueResult";
						}
						
						replaceQueryMethodCall(call, enclosingMethod, queryMethod);
					}
					
				}
				return true;
			}
			
			private boolean isInInnerClass(MethodInvocation call)
			{
				TypeDeclaration enclosingType = getEnclosingTypeDeclaration(call);
				return enclosingType != null && enclosingType.getParent() instanceof TypeDeclaration;
			}

			@SuppressWarnings("unchecked")
			private void replaceQueryMethodCall(MethodInvocation call, MethodDeclaration enclosingMethod, String queryMethod)
			{
				// rewrite query method call 
				MethodInvocation newQueryCall = ast.newMethodInvocation();
				newQueryCall.setExpression(ast.newSimpleName(actualSysCtxVar));
				newQueryCall.setName(ast.newSimpleName(queryMethod));
				
				if (!call.arguments().isEmpty())
				{
					String queryForCriteriaValidation = null;
					
					Object queryArg = null;
					
					if (call.arguments().get(0) instanceof StringLiteral)
					{
						StringLiteral queryString = ast.newStringLiteral();
						queryString.setLiteralValue(((StringLiteral)call.arguments().get(0)).getLiteralValue());
						queryArg = queryString;
						queryForCriteriaValidation = queryString.getLiteralValue();
					}
					else if (call.arguments().get(0) instanceof InfixExpression)
					{
						// clone the query argument
						InfixExpression oldInfixExpression = (InfixExpression)call.arguments().get(0);
						queryArg = (InfixExpression)ASTNode.copySubtree(ast, oldInfixExpression);
						
						// the query string provided as parameter is taken from InfixExpression.toString value and is enclosed in quotes
						// which we need to remove here
						String query = MiscUtils.trim(call.arguments().get(0).toString(), '\"');
						
						// infix expressions are those that consist of multiple concatenated strings, e.g. "select id from User where name = '" + username + "'"
						queryForCriteriaValidation = removeInfixVariables(query);
					}
					else
					{
						throw new IllegalArgumentException("Incorrect argument in call to query method");
					}
					
					Criteria c = null;
					
					// validate query
					try
					{
						c = env.getSelectCriteriaFromDAL(queryForCriteriaValidation);
					}
					catch (DALSyntaxException e)
					{
						throw new IllegalArgumentException("DAL syntax error: " + e.getMessage());
					}
					catch (KommetException e)
					{
						throw new AstIllegalArgumentException(e.getMessage(), true);
					}
					
					// add types to the list of types queried by this method
					try
					{
						addQueriedTypes(c, enclosingMethod);
					}
					catch (KommetException e)
					{
						e.printStackTrace();
						throw new IllegalArgumentException("Cannot add queried type to method: " + e.getMessage());
					}
					
					newQueryCall.arguments().add(queryArg);
					rewrite.replace(call, newQueryCall, null);
				}
				else
				{
					// the query method has incorrect arguments
					throw new IllegalArgumentException("Incorrect query arguments");
				}
			}

			/**
			 * Infix expressions are those that consist of multiple concatenated strings, e.g. "select id from User where name = '" + username + "'"
			 * To evaluate the query's type we don't really need the variable values, so we just replace those expressions with an empty string
			 * @param query
			 * @return
			 */
			private String removeInfixVariables(String query)
			{
				Pattern kollVarPattern = Pattern.compile("\"\\s*\\+[^\\+]+\\+\\s*\"");
				Matcher m = kollVarPattern.matcher(query);
				
				StringBuffer sb = new StringBuffer();
				while (m.find())
				{
					// regardless of what the expression is, just replace it with a mock string
					// the mock string needs to has a form of a KID
					// the reason for this is the following: if we have a query that has a condition "someProperty = 'somestring'", then some property
					// can be either a text field or a KID. If it is a text field, than it can be compared to any string. But if it a KID, query will be parsed
					// only if the string is a valid KID. This is why it's safer to put some valid KID value into the string.
					m.appendReplacement(sb, "0010000000001");
				}
				m.appendTail(sb);
	
				return sb.toString();
			}

			private void addQueriedTypes(Criteria c, MethodDeclaration method) throws KommetException
			{
				if (!this.queriedTypesByMethod.containsKey(method))
				{
					this.queriedTypesByMethod.put(method, new HashSet<String>());
				}
				
				this.queriedTypesByMethod.get(method).add(c.getType().getQualifiedName());
				
				// add types from joins
				if (c.getJoinedTypes() != null)
				{
					for (kommet.data.Type type : c.getJoinedTypes())
					{
						this.queriedTypesByMethod.get(method).add(type.getQualifiedName());
					}
				}
			}

			private String getQueryMethodByBoundType(Type type)
			{
				if (type instanceof ParameterizedType)
				{
					return ((ParameterizedType)type).resolveBinding().getQualifiedName().startsWith(java.util.List.class.getName() + "<") ? "query" : "queryUniqueResult";
				}
				else
				{
					// if we want to use the "query" method which returns java.util.List, we would expect a parameterized type
					// so if the type is not parameterized, we are using the queryUniqueResult method
					return "queryUniqueResult";
				}
			}
			
			public void endVisit (MethodDeclaration method)
			{	
				NormalAnnotation setterAnnot = null;
				NormalAnnotation sharingRuleNormalAnnotation = null;
				MarkerAnnotation sharingRuleMarkerAnnotation = null;
				
				// make sure setters and getters for system fields are not declared explicitly by user
				if (TypeProxyEnhancer.isSystemFieldAccessor(method.getName()))
				{
					throw new IllegalArgumentException("Field accessor " + method.getName() + " has name reserved for system fields");
				}
				
				// find setter annotation
				for (Object o : method.modifiers())
				{
					if (o instanceof NormalAnnotation)
					{
						String annotationTypeName = ((NormalAnnotation)o).resolveTypeBinding().getQualifiedName();
						if (annotationTypeName.equals(Setter.class.getName()))
						{
							setterAnnot = ((NormalAnnotation)o);
						}
						else if (annotationTypeName.equals(SharingRule.class.getName()))
						{
							sharingRuleNormalAnnotation = ((NormalAnnotation)o);
						}
					}
					else if (o instanceof MarkerAnnotation)
					{
						String annotationTypeName = ((MarkerAnnotation)o).resolveTypeBinding().getQualifiedName();
						if (annotationTypeName.equals(SharingRule.class.getName()))
						{
							sharingRuleMarkerAnnotation = ((MarkerAnnotation)o);
						}
					}
				}
				
				if (setterAnnot != null)
				{
					if (setterAnnot.values().size() == 1)
					{
						if (setterAnnot.values().get(0) instanceof MemberValuePair)
						{
							MemberValuePair pair = (MemberValuePair)setterAnnot.values().get(0);
							
							// this method is a valid setter for our field
							setters.put(((StringLiteral)pair.getValue()).getLiteralValue(), method);
						}
					}
					else
					{
						throw new IllegalArgumentException("Missing field name of @" + Setter.class.getSimpleName() + " annotation");
					}
				}
				else if (sharingRuleNormalAnnotation != null || sharingRuleMarkerAnnotation != null)
				{
					// sharing rule declarations are validated in ClassService, but there is one thing we cannot check there - actual type parameters of generic list types
					if (method.getReturnType2().isParameterizedType())
					{
						String methodReturnType = method.getReturnType2().resolveBinding().getQualifiedName();
						
						String sharedWith = null;
						
						if (methodReturnType.equals(List.class.getName() + "<" + User.class.getName() + ">"))
						{
							sharedWith = "User";
						}
						else if (methodReturnType.equals(List.class.getName() + "<" + UserGroup.class.getName() + ">"))
						{
							sharedWith = "UserGroup";
						}
						else
						{
							throw new IllegalArgumentException("Method annotated with @" + kommet.koll.annotations.SharingRule.class.getSimpleName() + " must return a collection of either User or UserGroup");
						}
						
						addSharedWithAnnotation(sharedWith, method);
					}
				}
				else
				{
					String methodName = method.getName().toString();
					
					// the method is not annotated with @Setter, but it can still be a valid setter candidate
					if (methodName.startsWith("set") && methodName.length() > 3 && method.parameters().size() == 1 && ((PrimitiveType)method.getReturnType2()).getPrimitiveTypeCode() == PrimitiveType.VOID)
					{
						potentialSetters.add(method);
					}
				}
				
				if (isEnhanceProxy)
				{
					checkConstructorCandidate(method);
				}
			}

			private void addSharedWithAnnotation(String sharedWith, MethodDeclaration method)
			{
				addImport(SharedWith.class);
					
				SingleMemberAnnotation annot = ast.newSingleMemberAnnotation();
				annot.setTypeName(ast.newName(SharedWith.class.getName()));
				
				StringLiteral sharedWithName = ast.newStringLiteral();
				sharedWithName.setLiteralValue(sharedWith);
				
                annot.setValue(sharedWithName);
				
				rewrite.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY).insertLast(annot, null);
			}

			private void checkConstructorCandidate(MethodDeclaration method)
			{
				if (!method.isConstructor())
				{
					return;
				}
				
				if (method.parameters().isEmpty())
				{
					defaultConstructor = method;
				}
			}

			// add import to file
			private void addImport(java.lang.Class<?> cls)
			{
				ImportDeclaration id = ast.newImportDeclaration();
				id.setName(ast.newName(cls.getName()));
				rewrite.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY).insertLast(id, null);
			}

			/**
			 * Gets the enclosing method of the current node
			 * @param call
			 * @return
			 */
			private MethodDeclaration getEnclosingMethod(ASTNode argNode)
			{
				ASTNode node = argNode;
				
				while (node.getParent() != null)
				{
					if (node.getParent() instanceof MethodDeclaration)
					{
						return (MethodDeclaration)node.getParent();
					}
					else
					{
						// go up the tree
						node = node.getParent();
					}
				}
				
				return null;
			}
			
			private TypeDeclaration getEnclosingTypeDeclaration(ASTNode argNode)
			{
				ASTNode node = argNode;
				
				while (node.getParent() != null)
				{
					if (node.getParent() instanceof TypeDeclaration)
					{
						return (TypeDeclaration)node.getParent();
					}
					else
					{
						// go up the tree
						node = node.getParent();
					}
				}
				
				return null;
			}
			
			public boolean visit (NormalAnnotation annot)
			{
				if (annot == null)
				{
					throw new IllegalArgumentException("Annotation is null");
				}
				else if (annot.resolveTypeBinding() == null)
				{
					throw new IllegalArgumentException("Type binding of annotation is null");
				}
				else if (annot.resolveTypeBinding().getQualifiedName() == null)
				{
					throw new IllegalArgumentException("Type binding qualified name of annotation is null");
				}
				
				if (annot.resolveTypeBinding().getQualifiedName().equals(kommet.koll.annotations.Type.class.getName()))
				{
					isEnhanceProxy = true;
					
					// add import for the custom type proxy
					addImport(CustomTypeRecordProxy.class);
				}
				else if (annot.resolveTypeBinding().getQualifiedName().equals(kommet.koll.annotations.Field.class.getName()))
				{	
					if (!(annot.getParent() instanceof MethodDeclaration))
					{
						throw new IllegalArgumentException("Annotation @" + kommet.koll.annotations.Field.class.getSimpleName() + " must be placed on a method");
					}
					
					MethodDeclaration getter = (MethodDeclaration)annot.getParent();
					String methodReturnType = getter.getReturnType2().resolveBinding().getQualifiedName();
					
					// found a potential getter
					for (Object val : annot.values())
					{
						if (val instanceof MemberValuePair)
						{
							if (((MemberValuePair)val).getName().toString().equals("name"))
							{
								String fieldName = ((StringLiteral)((MemberValuePair)val).getValue()).getLiteralValue();
								
								fieldJavaTypes.put(fieldName, methodReturnType);
								
								// annotate the getter with @Property
								Map<String, Object> annotValues = new HashMap<String, Object>();
								annotValues.put("field", fieldName);
								NormalAnnotation propertyAnnotation = TypeProxyEnhancer.getNormalAnnotation(ast, Property.class, annotValues);
								
								// append annotation to the getter
								rewrite.getListRewrite(getter, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(propertyAnnotation, null);
							}
						}
					}
				}
				
				return true;
			}

			public boolean visit (SingleMemberAnnotation annot)
			{
				if (annot.resolveTypeBinding().getQualifiedName().equals(SystemContextVar.class.getName()))
				{
					isSysCtxVarAnnotationPresent = true;
					
					if (annot.getValue() instanceof StringLiteral)
					{
						actualSysCtxVar = ((StringLiteral)annot.getValue()).getLiteralValue();
					}
					else
					{
						throw new IllegalArgumentException("Cannot read value of annotation @" + SystemContextVar.class.getSimpleName());
					}
				}
				return true;
			}
			
			@SuppressWarnings("unchecked")
			public void endVisit (TypeDeclaration decl)
			{
				if (!(decl.getParent() instanceof TypeDeclaration))
				{
					// check if this is a top-level or inner class
					// inner classes in Java may not declare static members, so we cannot inject system context into them
					if (!isSysCtxVarAnnotationPresent && !(decl.getParent() instanceof TypeDeclaration))
					{
						// add @SystemContextVar annotation to the class
						SingleMemberAnnotation sysCtxAnnotation = ast.newSingleMemberAnnotation();
						sysCtxAnnotation.setTypeName(ast.newName(SystemContextVar.class.getName()));
						StringLiteral sysLiteral = ast.newStringLiteral();
						sysLiteral.setLiteralValue(actualSysCtxVar);
						sysCtxAnnotation.setValue(sysLiteral);
						
						rewrite.getListRewrite(cu, CompilationUnit.TYPES_PROPERTY).insertBefore(sysCtxAnnotation, decl, null);
						
						// the below commented line could be used to add an annotation to an inner class
						// but as stated in a comment above, inner classes may not have static members
						// rewrite.getListRewrite(decl, TypeDeclaration.MODIFIERS2_PROPERTY).insertLast(sysCtxAnnotation, null);
					}
					
					// add system context public field to the class
					VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
					fragment.setName(ast.newSimpleName(actualSysCtxVar));
					
					FieldDeclaration field = ast.newFieldDeclaration(fragment);
					field.modifiers().addAll(ast.newModifiers(Modifier.PUBLIC | Modifier.STATIC));
					field.setType(ast.newSimpleType(ast.newQualifiedName(ast.newName(SystemContext.class.getPackage().getName()), ast.newSimpleName(SystemContext.class.getSimpleName()))));
					
					// append field declaration to the class body
					rewrite.getListRewrite(decl, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertFirst(field, null);
					
					addQueriedTypesAnnotations(decl);
				}
				
				// enhance proxies
				if (isEnhanceProxy)
				{	
					NormalAnnotation entityAnnot = ast.newNormalAnnotation();
					entityAnnot.setTypeName(ast.newName(Entity.class.getName()));
					
					ITypeBinding binding = decl.resolveBinding();
					
					MemberValuePair mvp = ast.newMemberValuePair();
                    mvp.setName(ast.newSimpleName("type"));
                    Expression exp = ast.newStringLiteral();
                    
                    try
					{
                    	// class proxy names are env-specific, so we need to change them to user-specific
						((StringLiteral)exp).setLiteralValue(MiscUtils.envToUserPackage(binding.getQualifiedName(), env));
					}
					catch (KommetException e)
					{
						throw new IllegalArgumentException("Cannot translate env to user package " + binding.getQualifiedName() + ": " + e.getMessage());
					}
                    
                    mvp.setValue(exp);
                    
                    entityAnnot.values().add(mvp);
					
					rewrite.getListRewrite(cu, CompilationUnit.TYPES_PROPERTY).insertBefore(entityAnnot, decl, null);
					
					// add extends
					rewrite.set(decl, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, ast.newSimpleName(CustomTypeRecordProxy.class.getSimpleName()), null);
					
					addImport(CustomTypeRecordProxy.class);
					
					// if default constructor does not exist, create if
					if (defaultConstructor == null)
					{
						// create default constructor
						MethodDeclaration constructor = ast.newMethodDeclaration();
						constructor.setConstructor(true);
						constructor.modifiers().addAll(ast.newModifiers(Modifier.PUBLIC));
						constructor.setName(ast.newSimpleName(binding.getName()));
						rewrite.getListRewrite(decl, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertFirst(constructor, null);
						
						// add empty body to the method
						constructor.setBody(ast.newBlock());
						
						defaultConstructor = constructor;
					}
					
					// enhance default constructor
					// make sure the default constructor throws RecordProxyException and calls super()
					SuperConstructorInvocation methodCall = ast.newSuperConstructorInvocation();
					rewrite.getListRewrite(defaultConstructor.getBody(), Block.STATEMENTS_PROPERTY).insertFirst(methodCall, null);
					
					addImport(RecordProxyException.class);
			
					for (Object typeName : defaultConstructor.thrownExceptionTypes())
					{
						if (typeName instanceof SimpleName)
						{
							
						}
					}
					
					rewrite.getListRewrite(defaultConstructor, MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY).insertLast(ast.newSimpleName(RecordProxyException.class.getSimpleName()), null);
					
					try
					{
						enhanceSetters();
					}
					catch (KommetException e)
					{
						throw new IllegalArgumentException("Error enhancing setters: " + e.getMessage());
					}
					
					// add setters and getters for system fields
					try
					{
						addSystemFields(decl);
					}
					catch (KommetException e)
					{
						throw new IllegalArgumentException("Error generating setters for system fields: " + e.getMessage());
					}
				}
			}
			
			@SuppressWarnings("unchecked")
			private void addQueriedTypesAnnotations(TypeDeclaration decl)
			{
				addImport(QueriedTypes.class);
				
				for (MethodDeclaration method : this.queriedTypesByMethod.keySet())
				{
					Set<String> types = this.queriedTypesByMethod.get(method);
					if (types == null || types.isEmpty())
					{
						continue;
					}
					
					SingleMemberAnnotation annot = ast.newSingleMemberAnnotation();
					annot.setTypeName(ast.newName(QueriedTypes.class.getName()));
					
					ArrayInitializer values = ast.newArrayInitializer();
					
					for (String type : types)
					{
						StringLiteral typeName = ast.newStringLiteral();
						typeName.setLiteralValue(type);
						values.expressions().add(typeName);
					}
					
                    annot.setValue(values);
					
					rewrite.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY).insertLast(annot, null);
				}
			}

			private void addSystemFields(TypeDeclaration decl) throws KommetException
			{
				String userTypeName = MiscUtils.userToEnvPackage(env.getType(KeyPrefix.get(KID.USER_PREFIX)).getQualifiedName(), env);
		
				// add private fields
				TypeProxyEnhancer.addFieldDeclaration(rewrite, ast, decl, Field.CREATEDBY_FIELD_NAME, userTypeName, env);
				TypeProxyEnhancer.addFieldDeclaration(rewrite, ast, decl, Field.LAST_MODIFIED_BY_FIELD_NAME, userTypeName, env);
				
				// add getters and setters
				TypeProxyEnhancer.addGetter(rewrite, ast, decl, Field.CREATEDBY_FIELD_NAME, userTypeName);
				TypeProxyEnhancer.addGetter(rewrite, ast, decl, Field.LAST_MODIFIED_BY_FIELD_NAME, userTypeName);
				TypeProxyEnhancer.addSetter(rewrite, ast, decl, Field.CREATEDBY_FIELD_NAME, userTypeName);
				TypeProxyEnhancer.addSetter(rewrite, ast, decl, Field.LAST_MODIFIED_BY_FIELD_NAME, userTypeName);
			}

			private void enhanceSetters() throws KommetException
			{	
				// first go through fields declared in this class and make sure each of them has a setter
				for (String fieldName : fieldJavaTypes.keySet())
				{
					// check annotated setters
					if (setters.containsKey(fieldName))
					{
						// this is fine, we have a setter for this field
						continue;
					}
					else
					{
						boolean potentialSetterFound = false;
						
						// try to find a matching candidate among non-annotated methods
						for (MethodDeclaration potentialSetter : potentialSetters)
						{
							String methodName = potentialSetter.getName().toString();
							String setterFieldName = MiscUtils.getPropertyFromSetter(methodName);
							
							if (!setterFieldName.equals(fieldName))
							{
								continue;
							}
							
							// if has already been checked that this method takes exactly one parameter
							if (potentialSetter.parameters().get(0) instanceof SingleVariableDeclaration)
							{
								String varType = ((SingleVariableDeclaration)potentialSetter.parameters().get(0)).getType().resolveBinding().getQualifiedName();
								if (varType.equals(fieldJavaTypes.get(setterFieldName)))
								{
									if (potentialSetterFound)
									{
										throw new IllegalArgumentException("More than one setter candidate found for field " + fieldName);
									}
									
									// argument types match, so we can use this setter
									setters.put(fieldName, potentialSetter);
									potentialSetterFound = true;
								}
							}
						}
						
						if (!potentialSetterFound)
						{
							throw new IllegalArgumentException("Neither annotated nor not-annotated setter found for field " + fieldName);
						}
					}
				}
				
				for (MethodDeclaration setter : setters.values())
				{
					// append call to setInitialized() as last statement of the method
					MethodInvocation methodCall = ast.newMethodInvocation();
					methodCall.setName(ast.newSimpleName("setInitialized"));
					Statement initializerStatement = ast.newExpressionStatement(methodCall);
					rewrite.getListRewrite(setter.getBody(), Block.STATEMENTS_PROPERTY).insertLast(initializerStatement, null); 
				}
			}

			public boolean visit (TypeDeclaration decl)
			{	
				return true;
			}
	    };
	}

	private String replaceTypeDeclarations(String code, String mockQueryMethod, AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		@SuppressWarnings("deprecation")
		ASTParser parser = ASTParser.newParser(AST.JLS11);
		parser.setStatementsRecovery(false);
		parser.setBindingsRecovery(true);
		parser.setResolveBindings(true);
		parser.setUnitName("AnyName.java");
		parser.setCompilerOptions(JavaCore.getOptions());		
		parser.setEnvironment(new String[] { env.getCompileClassPath() }, null, null, true);
		parser.setSource(code.toCharArray());
		
		final CompilationUnit cu = (CompilationUnit)parser.createAST(null);
		
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		Document document = new Document(code);
		
		// check sys ctx var defined for this user
		String sysCtxVar = uchService.getUserSettingAsString(UserSettingKeys.KM_SYS_SYS_CTX_VAR, authData, AuthData.getRootAuthData(env), env);
		
		if (!StringUtils.hasText(sysCtxVar))
		{
			sysCtxVar = Constants.SYSCTX_INJECT_VAR;
		}
				
		cu.accept(getASTVisitor(env, ast, cu, rewrite, mockQueryMethod, sysCtxVar));
		
		// get text edits (updates to source code) created by the ASTRewriter
		TextEdit edits = rewrite.rewriteAST(document, null);

		try
		{
			// apply updates to the source code document
			edits.apply(document);
		}
		catch (MalformedTreeException e)
		{
			throw new KollParserException("Could not rewrite class code. Nested: " + e.getMessage());
		}
		catch (BadLocationException e)
		{
			throw new KollParserException("Could not rewrite class code. Nested: " + e.getMessage());
		}
		
		// return source code with updated packages
		return document.get();
	}

	private static String replacePackageName(String code, KID envId)
	{
		Pattern p = Pattern.compile("package\\s+([^;]+);");
		Matcher m = p.matcher(code);
		if (m.find())
		{
		    // replace first number with "number" and second number with the first
		    return m.replaceFirst("package " + getKollPackage(envId, m.group(1)) + ";");
		}
		else
		{
			return code;
		}
	}

	private static String getKollPackage(KID envId, String packageName)
	{
		//return "kommet.env." + envName + ".koll." + packageName;
		return KommetCompiler.KOLL_BASE_PACKAGE + ".env" + envId + "." + packageName;
	}
}