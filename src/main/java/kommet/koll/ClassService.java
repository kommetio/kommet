/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.BusinessAction;
import kommet.basic.Class;
import kommet.basic.RecordProxy;
import kommet.basic.SharingRule;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.ViewDao;
import kommet.businessprocess.BusinessProcessService;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.DateDataType;
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.EmailDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.env.EnvData;
import kommet.filters.SharingRuleFilter;
import kommet.koll.annotations.Action;
import kommet.koll.annotations.Controller;
import kommet.koll.annotations.Disabled;
import kommet.koll.annotations.QueriedTypes;
import kommet.koll.annotations.Rest;
import kommet.koll.annotations.ReturnsFile;
import kommet.koll.annotations.SharedWith;
import kommet.koll.annotations.triggers.Trigger;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.SharingRuleService;
import kommet.services.SystemActionService;
import kommet.triggers.TriggerService;
import kommet.triggers.TriggerUtil;
import kommet.triggers.TypeTriggerDao;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

@Service
public class ClassService
{
	@Inject
	ClassDao classDao;
	
	@Inject
	ViewDao viewDao;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	TypeTriggerDao typeTriggerDao;
	
	@Inject
	SystemActionService systemActionService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	SharingRuleService sharingRuleService;
	
	@Inject
	BusinessProcessService bpService;
	
	private static final Logger log = LoggerFactory.getLogger(ClassService.class);

	@Transactional(readOnly = true)
	public List<Class> getClasses(ClassFilter filter, EnvData env) throws KommetException
	{
		return classDao.find(filter, env);
	}

	@Transactional(readOnly = true)
	public Class getClass(KID fileId, EnvData env) throws KommetException
	{
		return classDao.get(fileId, env);
	}
	
	@Transactional
	public Class save(Class cls, AuthData authData, EnvData env) throws KommetException
	{
		return save(cls, false, authData, env);
	}

	private Class save(Class cls, boolean isSilentUpdate, AuthData authData, EnvData env) throws KommetException
	{
		if (!StringUtils.hasText(cls.getAccessLevel()))
		{
			// set default encryption
			cls.setAccessLevel("Editable");
		}
		
		validate(cls, env);
		return classDao.save(cls, false, false, false, isSilentUpdate, authData, env);
	}
	
	public SimpleKollTranslator getKollTranslator(EnvData env) throws KommetException
	{
		return new SimpleKollTranslator(compiler.getClassLoader(env), this.uchService);
	}
	
	@Transactional
	public Class fullSave(Class file, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		return fullSave(file, dataService, false, authData, env);
	}
	
	/**
	 * Performs a full save of the file. Full save means that before actually saving the file to DB,
	 * this method also performs code conversion for triggers.
	 * @param file
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public Class fullSave(Class file, DataService dataService, boolean overwriteNameFromCode, AuthData authData, EnvData env) throws KommetException
	{
		return fullSave(file, dataService, overwriteNameFromCode, false, authData, env);
	}
	
	@Transactional
	public Class fullSave (Class file, DataService dataService, boolean overwriteNameFromCode, boolean isOnlyUpdateJavaCode, AuthData authData, EnvData env) throws KommetException
	{
		validateKollCode(file, overwriteNameFromCode, env);
		
		String javaCode = getKollTranslator(env).kollToJava(file.getKollCode(), true, authData, env);
		
		if (overwriteNameFromCode)
		{
			// deduce the name from code, but do this before actual compilation
			Class tmpClass = KollUtil.getClassFromCode(javaCode, env);
			file.setName(tmpClass.getName());
			file.setPackageName(MiscUtils.envToUserPackage(tmpClass.getPackageName(), env));
		}
		
		file.setJavaCode(javaCode);
		
		CompilationResult result = compiler.compile(file, env);
		
		if (!result.isSuccess())
		{
			throw new ClassCompilationException("Compilation failed", result);
		}
		
		compiler.resetClassLoader(env);
		
		// if compilation is successful, check if this class is annotated with @Trigger
		// if so, compile again changing the Java code a little
		// TODO double compilation is something that needs to be changed
		java.lang.Class<?> compiledClass = null;
		
		try
		{
			compiledClass = compiler.getClass(file, false, env);
		}
		catch (MalformedURLException e)
		{
			throw new KommetException("Error getting compiled class " + file.getQualifiedName() + ": " + e.getMessage());
		}
		catch (ClassNotFoundException e)
		{
			throw new KommetException("Compiled class not found: " + file.getQualifiedName());
		}
		
		List<String> validationErrors = validateController(compiledClass);
		if (!validationErrors.isEmpty())
		{
			throw new KommetException(MiscUtils.implode(validationErrors, ", "));
		}
		 
		String oldFileName = file.getQualifiedName();
		
		boolean isNewFile = file.getId() == null;
		
		if (isNewFile)
		{
			// if it's a new file, we will need it's id for methods like updateGenericActionsForClass()
			// so in that case we save it already here, although we know that further processing might show that the file should
			// not be saved because of errors - in that case we will just delete the file
			// actually save the file in db - only after all triggers, actions etc. have been successfully processed
			// For existing classes we cannot save them here, because 1) we'd lose the old version 2) even if we restored the old version, the lastModifiedDate would still be changed
			file = save(file, isOnlyUpdateJavaCode, authData, env);
		}
		
		if (!isOnlyUpdateJavaCode)
		{
			try
			{
				//log.debug("Handling triggers");
				handleTypeTriggerAssignments(compiledClass, file, authData, env);
				
				//log.debug("Handling VRs");
				handleSharingRuleDeclarations(compiledClass, file, isNewFile, dataService, authData, env);
				
				//log.debug("Handling persistence annotations");
				handleTypePersistenceAnnotations(compiledClass, oldFileName, file, dataService, authData, env);
				
				//log.debug("Handling business actions");
				handleBusinessActionDeclarations(compiledClass, file, isNewFile, dataService, authData, env);
				
				//log.debug("Handling generic actions");
				// update generic action potentially declared in this time, before or after it was updated
				env.updateGenericActionsForClass(file, compiler, systemActionService.getSystemActionURLs(), authData.getI18n(), this.viewDao, false);
				
				//handleCustomAuthHandlers(compiledClass, file, systemActionService.getSystemActionURLs(), authData.getI18n(), env);
			}
			catch (Exception e)
			{
				if (isNewFile)
				{
					// revert the save
					delete(file, dataService, authData, env);
				}
				
				throw e;
			}
		}
		
		// actually save the file in db - only after all triggers, actions etc. have been successfully processed
		file = save(file, isOnlyUpdateJavaCode, authData, env);
				
		// return the saved file
		return file;
	}
	
	/*private void handleCustomAuthHandlers (java.lang.Class<?> compiledClass, Class file, Set<String> systemURLs, I18nDictionary i18n, EnvData env) throws KommetException
	{
		boolean isAuthHandler = false;
		
		java.lang.Class<?> cls = compiledClass;
		
		while (cls != null)
		{
			if (cls.getName().equals(AuthHandler.class.getName()))
			{
				isAuthHandler = true;
				break;
			}
			
			cls = cls.getSuperclass();
		}
		
		if (isAuthHandler)
		{
			// if this class is an AuthHandler, is may be buffered in some generic action
			for (GenericAction action : env.getGenericActions().values())
			{
				AuthHandler handler = action.getAuthHandler(compiler, env);
				if (handler != null && handler.getClass().getName().equals(compiledClass.getName()))
				{
					// we need to reinitialize the AuthHandler for this generic action, so that it references the new compiled handler
					env.updateGenericActionsForClass(file, compiler, systemURLs, i18n, viewDao, false);
				}
			}
		}
	}*/

	/**
	 * Handles business actions declared in files
	 * @param compiledClass
	 * @param file
	 * @param isNewFile
	 * @param dataService
	 * @param authData
	 * @param env
	 * @throws KommetException 
	 */
	private void handleBusinessActionDeclarations(java.lang.Class<?> compiledClass, Class file, boolean isNewFile, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		// check if there is a business action declared previously in this file
		BusinessAction existingAction = isNewFile ? null : bpService.getActionForFile(file.getId(), authData, env);
		
		if (compiledClass.isAnnotationPresent(kommet.businessprocess.annotations.BusinessAction.class))
		{
			// create new action from file and save it
			bpService.createBusinessActionFromFile(compiledClass, file, existingAction, authData, env);
			
			if (!isNewFile)
			{
				// if action definition has been updated, its class has been recompiled so it's possible that
				// the cached executors contain class instances from a previous class loader - we need to clear them
				env.removeProcessExecutors();
			}
		}
		else
		{
			if (isNewFile)
			{
				return;
			}
			else
			{
				if (existingAction != null)
				{
					// action was previously declared in this file, but now the @BusinessAction annotation is removed, so the action should be removed if possible
					bpService.deleteAction(existingAction, authData, env);
				}
			}
		}
	}

	/**
	 * Checks for sharing rule declarations in this class.
	 * @param cls
	 * @param file
	 * @param authData
	 * @param env
	 * @throws KommetException 
	 */
	private void handleSharingRuleDeclarations (java.lang.Class<?> cls, Class file, boolean isNewFile, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		AuthData rootAuthData = AuthData.getRootAuthData(env);
		
		Map<String, SharingRule> existingSharingRulesByClassAndMethod = new HashMap<String, SharingRule>();
		
		if (!isNewFile)
		{
			// first find existing sharing rules for this class
			SharingRuleFilter filter = new SharingRuleFilter();
			filter.addFileId(file.getId());
			
			for (SharingRule rule : sharingRuleService.get(filter, authData, env))
			{
				existingSharingRulesByClassAndMethod.put(rule.getFile().getQualifiedName() + "." + rule.getMethod(), rule);
			}
		}
		
		// each key is a qualified name of a type, and the values is a set of method names that contain sharing rule declarations for this type
		Map<String, Set<String>> sharingRuleMethodsByType = new HashMap<String, Set<String>>();
		Map<String, SharingRule> newSharingRules = new HashMap<String, SharingRule>();
		Map<String, List<String>> sharingRulesByDependendType = new HashMap<String, List<String>>();
		
		// iterate over methods in the class in search of sharing rule annotations
		for (Method m : cls.getMethods())
		{
			if (!m.isAnnotationPresent(kommet.koll.annotations.SharingRule.class))
			{
				continue;
			}
			
			// if the method is annotated with @SharingRule, it must take one parameter - a RecordProxy
			// and it must return a collection of Users or UserGroups
			if (m.getParameterTypes().length != 1 || !RecordProxy.class.isAssignableFrom(m.getParameterTypes()[0]))
			{
				throw new SharingRuleException("Method annotated with @" + kommet.koll.annotations.SharingRule.class.getSimpleName() + " must take exactly one parameter that is a subclass of RecordProxy");
			}
			
			java.lang.Class<?> rt = m.getReturnType();
			
			// check return type
			// actually already during compilation we are checking whether the method  returns a list of User/UserGroup, because parameterized type parameters cannot be checked
			// by reflection
			// however, if at compile time it was discovered that the method does not return a parameterized type, it does not throw any error.
			// it throws an error only if the method returns a parameterized type, but this type is neither User nor UserGroup
			if (!Collection.class.isAssignableFrom(rt) || !(rt.getTypeParameters() != null && rt.getTypeParameters().length == 1))
			{
				throw new SharingRuleException("Method annotated with @" + kommet.koll.annotations.SharingRule.class.getSimpleName() + " must return a collection of either User or UserGroup");
			}
			
			if (!Modifier.isPublic(m.getModifiers()))
			{
				throw new SharingRuleException("Method annotated with @" + kommet.koll.annotations.SharingRule.class.getSimpleName() + " must be public");
			}
			
			if (!Modifier.isStatic(m.getModifiers()))
			{
				throw new SharingRuleException("Method annotated with @" + kommet.koll.annotations.SharingRule.class.getSimpleName() + " must be static");
			}
		
			// at this point we already know that the method is a valid sharing rule declaration
			
			// we already know that the parameter type is a subclass of RecordProxy, so we know it's env-specific and can safely convert it to user-specific
			String referencedType = MiscUtils.envToUserPackage(m.getParameterTypes()[0].getName(), env);
			
			if (!sharingRuleMethodsByType.containsKey(referencedType))
			{
				sharingRuleMethodsByType.put(referencedType, new HashSet<String>());
			}
			sharingRuleMethodsByType.get(referencedType).add(m.getName());
			
			kommet.koll.annotations.SharingRule annot = m.getAnnotation(kommet.koll.annotations.SharingRule.class);
			
			SharingRule newRule = new SharingRule();
			newRule.setReferencedType(env.getType(referencedType).getKID());
			newRule.setIsEdit(annot.edit());
			newRule.setIsDelete(annot.delete());
			
			if (!m.isAnnotationPresent(SharedWith.class))
			{
				throw new SharingRuleException("Sharing rule declaration method should be automatically annotated with @" + SharedWith.class.getSimpleName());
			}
			
			SharedWith sharedWithAnnot = m.getAnnotation(SharedWith.class);
			newRule.setSharedWith(sharedWithAnnot.value());
			
			String ruleKey = file.getQualifiedName() + "." + m.getName();
			
			// add rule key to the set of new rules
			newSharingRules.put(ruleKey, newRule);
			
			findDependendTypes(newRule, ruleKey, m, annot, sharingRulesByDependendType, env);
		}
		
		// although the rule may have already existed, it's definition may have changed and may require recalculation
		// so we'll just remove all sharing rules from this class and add them anew
		sharingRuleService.delete(existingSharingRulesByClassAndMethod.values(), dataService, rootAuthData, env);
		
		Map<String, SharingRule> savedRules = new HashMap<String, SharingRule>();
		
		// add all rules anew and recalculate sharing for them
		for (String ruleKey : newSharingRules.keySet())
		{
			// this is a new rule, it did not exist previously
			SharingRule rule = newSharingRules.get(ruleKey);
			rule.setName(ruleKey);
			rule.setFile(file);
			rule.setMethod(MiscUtils.splitByLastDot(ruleKey).get(1));
			rule.setType("Code");
			
			// insert the rule
			savedRules.put(ruleKey, sharingRuleService.save(rule, dataService, authData, env));
		}
		
		// register dependent rules on env
		for (String typeName : sharingRulesByDependendType.keySet())
		{
			KID typeId = env.getType(typeName).getKID();
			
			for (String ruleKey : sharingRulesByDependendType.get(typeName))
			{
				if (!env.getDependentSharingRulesByType().containsKey(typeId))
				{
					env.getDependentSharingRulesByType().put(typeId, new ArrayList<SharingRule>());
				}
				env.getDependentSharingRulesByType().get(typeId).add(savedRules.get(ruleKey));
			}
		}
	}

	/**
	 * Find types which should trigger recalculation of this sharing rule.
	 * 
	 * @param rule
	 * @param ruleKey
	 * @param method
	 * @param annot
	 * @param sharingRulesByDependendType
	 * @param env
	 * @throws KommetException
	 */
	private void findDependendTypes(SharingRule rule, String ruleKey, Method method, kommet.koll.annotations.SharingRule annot, Map<String, List<String>> sharingRulesByDependendType, EnvData env) throws KommetException
	{
		Set<String> queriedTypeIds = new HashSet<String>();
		
		String[] queriedTypeNames = null;
		
		// updateForTypes attribute has priority over the @QueriedTypes annotation
		if (annot.updateForTypes().length > 0)
		{
			queriedTypeNames = annot.updateForTypes();
		}
		else if (method.isAnnotationPresent(QueriedTypes.class))
		{
			QueriedTypes queriedTypes = method.getAnnotation(QueriedTypes.class);
			queriedTypeNames = queriedTypes.value();
		}
			
		if (queriedTypeNames != null)
		{
			for (String typeName : queriedTypeNames)
			{
				if (!sharingRulesByDependendType.containsKey(typeName))
				{
					sharingRulesByDependendType.put(typeName, new ArrayList<String>());
				}
				
				queriedTypeIds.add(env.getType(typeName).getKID().getId());
				
				sharingRulesByDependendType.get(typeName).add(ruleKey);
			}
		}
		
		String sharedWithQualifiedName = null;
		
		// additionally, every rule should should be recalculated when records of its returned type are saved
		// this happens independent of whether the User/UserGroup type was queried or whether it was listed under
		// the @SharingRule(updateForTypes) attribute
		if (rule.getSharedWith().equals("User"))
		{
			sharedWithQualifiedName = env.getType(KeyPrefix.get(KID.USER_PREFIX)).getQualifiedName();
			queriedTypeIds.add(env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID().getId());
		}
		else if (rule.getSharedWith().equals("UserGroup"))
		{
			sharedWithQualifiedName = env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)).getQualifiedName();
			queriedTypeIds.add(env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)).getKID().getId());
		}
		else
		{
			throw new SharingRuleException("Unsupported sharing rule shared with type: " + rule.getSharedWith());
		}
		
		if (!sharingRulesByDependendType.containsKey(sharedWithQualifiedName))
		{
			sharingRulesByDependendType.put(sharedWithQualifiedName, new ArrayList<String>());
		}
		sharingRulesByDependendType.get(sharedWithQualifiedName).add(ruleKey);
		
		// mark the dependent type on the sharing rule to be saved
		rule.setDependentTypes(MiscUtils.implode(queriedTypeIds, ";"));
	}

	private void handleTypePersistenceAnnotations(java.lang.Class<?> cls, String oldClassName, Class file, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		if (cls.isAnnotationPresent(kommet.koll.annotations.Type.class))
		{
			kommet.koll.annotations.Type typeAnnotation = cls.getAnnotation(kommet.koll.annotations.Type.class);
			
			Type type = null;
			
			// perhaps this type already exists
			Type existingType = env.getType(oldClassName);
			
			if (existingType != null)
			{
				if (existingType.isDeclaredInCode())
				{
					type = existingType;
				}
				else
				{
					throw new CodeTypeDeclarationException("Type " + oldClassName + " already exists and is not declared in code, so it cannot be overridden by a code declaration");
				}
			}
			else
			{
				type = new Type();
				type.setDeclaredInCode(true);
			}
			
			type.setApiName(file.getName());
			type.setPackage(file.getPackageName());
			type.setLabel(typeAnnotation.label());
			type.setPluralLabel(typeAnnotation.pluralLabel());
			type.setBasic(false);
			
			// get field declarations from source code
			Set<String> newFields = getTypeFieldsFromCode(cls, type);
			
			// save type
			if (type.getKID() != null)
			{
				dataService.updateType(type, authData, env);
				
				// clone the field list of the type to avoid concurrent modification exception on this list when fields are removed
				List<Field> fieldsToCheckForDeletion = new ArrayList<Field>();
				fieldsToCheckForDeletion.addAll(type.getFields());
				
				// delete fields that are no longer in the type
				for (Field field : fieldsToCheckForDeletion)
				{
					if (!Field.isSystemField(field.getApiName()) && !newFields.contains(field.getApiName()))
					{
						dataService.deleteField(field, authData, env);
					}
				}
				
				// update all fields
				for (Field field : type.getFields())
				{
					if (!Field.isSystemField(field.getApiName()))
					{
						if (field.getKID() != null)
						{
							dataService.updateField(field, authData, env);
						}
						else
						{
							dataService.createField(field, authData, env);
						}
					}
				}
			}
			else
			{
				dataService.createType(type, authData, env);
			}
		}
		else
		{
			// remove types that may have been created for this class file
			if (env.getType(oldClassName) != null)
			{
				Type existingType = env.getType(oldClassName);
				if (existingType != null)
				{
					dataService.deleteType(existingType, authData, env);
				}
			}
		}
	}

	/**
	 * Returns a set of new field API names;
	 * @param cls
	 * @param type
	 * @return
	 * @throws KommetException
	 */
	private Set<String> getTypeFieldsFromCode(java.lang.Class<?> cls, Type type) throws KommetException
	{	
		Map<String, Method> setters = new HashMap<String, Method>();
		Map<String, Method> potentialNonAnnotatedSetters = new HashMap<String, Method>();
		Map<String, java.lang.Class<?>> fieldJavaTypes = new HashMap<String, java.lang.Class<?>>();
		
		for (Method method : cls.getMethods())
		{
			if (method.isAnnotationPresent(kommet.koll.annotations.Field.class))
			{
				if (method.getParameterTypes().length > 0)
				{
					throw new CodeTypeDeclarationException("Method " + method.getName() + " is annotated with @" + kommet.koll.annotations.Field.class.getSimpleName() + " so it should take no parameters");
				}
				
				kommet.koll.annotations.Field fieldAnnotation = method.getAnnotation(kommet.koll.annotations.Field.class);
				
				Field field = null;
				
				// check if the field already exists by API name
				if (type.getField(fieldAnnotation.name()) != null)
				{
					field = type.getField(fieldAnnotation.name());
					
					if (field.getKID() == null)
					{
						throw new KommetException("Field ID on an updated field " + field.getApiName() + " is null");
					}
				}
				else
				{
					field = new Field();
					field.setApiName(fieldAnnotation.name());
				}
				
				field.setLabel(fieldAnnotation.label());
				field.setRequired(fieldAnnotation.required());
				
				// TODO handle unique fields
				
				if (!method.isAnnotationPresent(kommet.koll.annotations.DataType.class))
				{
					field.setDataType(getDataTypeFromMethodReturnType(method));
				}
				else
				{
					field.setDataType(getDataTypeFromAnnotation(method));
				}
				
				fieldJavaTypes.put(field.getApiName(), method.getReturnType());
				type.addField(field);
			}
			else if (method.isAnnotationPresent(kommet.koll.annotations.Setter.class))
			{
				kommet.koll.annotations.Setter setterAnnotation = method.getAnnotation(kommet.koll.annotations.Setter.class);
				if (setters.containsKey(setterAnnotation.field()))
				{
					throw new CodeTypeDeclarationException("Duplicate setter for field " + setterAnnotation.field());
				}
				
				// make sure this method is a valid setter candidate
				if (!method.getReturnType().equals(Void.TYPE))
				{
					throw new CodeTypeDeclarationException("Setter method " + method.getName() + " does not return void");
				}
				
				if (method.getParameterTypes().length != 1)
				{
					throw new CodeTypeDeclarationException("Setter method " + method.getName() + " is expected to have exactly one parameter");
				}
				
				setters.put(setterAnnotation.field(), method);
			}
		}
		
		// go through setters
		for (Method method : cls.getMethods())
		{
			if (method.getReturnType().equals(Void.TYPE) && method.getName().startsWith("set") && method.getName().length() > 3 && method.getParameterTypes().length == 1)
			{
				String fieldName = MiscUtils.getPropertyFromSetter(method.getName());
				
				// find field
				java.lang.Class<?> fieldClass = fieldJavaTypes.get(fieldName);
				if (fieldClass != null)
				{
					// we already know that a field matching this setter exists, so we need to check if the arg type of the setter matches the java type of the field
					if (fieldClass.getName().endsWith(method.getParameterTypes()[0].getName()))
					{
						potentialNonAnnotatedSetters.put(fieldName, method);
					}
				}
			}
		}
		
		// now make sure that for every field there is a setter
		// which can be a method annotated with @Setter of any other matching method
		for (String fieldName : fieldJavaTypes.keySet())
		{
			// setters and getters for fields except createdby and lastmodifiedby are inherited from RecordProxy
			// so they don't have to be be checked
			// setters and getters for fields createdby and lastmodifiedby are created by the proxy enhancer, so we can be sure they will be there as well
			if (Field.isSystemField(fieldName))
			{
				continue;
			}
			
			if (setters.containsKey(fieldName))
			{
				Method setter = setters.get(fieldName);
				
				// make sure the argument type of the setter matches the field data type
				if (!setter.getParameterTypes()[0].getName().equals(fieldJavaTypes.get(fieldName).getName()))
				{
					throw new CodeTypeDeclarationException("Setter method " + setter.getName() + " for field " + fieldName + " has argument type  tetch the field type");
				}
				
				// setter valiation finished, we have a valid setter for this field
			}
			else
			{
				// no annotated setter found for this field, so we will search among non-annotated setter methods
				if (!potentialNonAnnotatedSetters.containsKey(fieldName))
				{
					throw new CodeTypeDeclarationException("Neither annotated nor not annotated setter method for field " + fieldName + " has been found found"); 
				}
			}
		}
		
		return fieldJavaTypes.keySet();
	}

	private DataType getDataTypeFromAnnotation(Method method) throws CodeTypeDeclarationException
	{
		kommet.koll.annotations.DataType dtAnnot = method.getAnnotation(kommet.koll.annotations.DataType.class);
		
		if (dtAnnot.id() == DataType.TEXT)
		{
			if (!method.getReturnType().getName().equals(String.class.getName()))
			{
				throw new CodeTypeDeclarationException("Method " + method.getName() + " is annotated with @" + kommet.koll.annotations.DataType.class.getSimpleName() + " and as text, but returns " + method.getReturnType().getName());
			}
			
			TextDataType dt = new TextDataType();
			dt.setLength(dtAnnot.length());
			return dt;
		}
		if (dtAnnot.id() == DataType.EMAIL)
		{
			if (!method.getReturnType().getName().equals(String.class.getName()))
			{
				throw new CodeTypeDeclarationException("Method " + method.getName() + " is annotated with @" + kommet.koll.annotations.DataType.class.getSimpleName() + " and as email, but returns " + method.getReturnType().getName());
			}
			
			return new EmailDataType();
		}
		if (dtAnnot.id() == DataType.KOMMET_ID)
		{
			if (!method.getReturnType().getName().equals(KID.class.getName()))
			{
				throw new CodeTypeDeclarationException("Method " + method.getName() + " is annotated with @" + kommet.koll.annotations.DataType.class.getSimpleName() + " and as KID, but returns " + method.getReturnType().getName());
			}
			
			return new KIDDataType();
		}
		else if (dtAnnot.id() == DataType.NUMBER)
		{
			NumberDataType dt = new NumberDataType();
			dt.setDecimalPlaces(dtAnnot.decimalPlaces());
			
			String returnedType = method.getReturnType().getName();
			
			if (dtAnnot.decimalPlaces() > 0)
			{
				if (returnedType.equals(Double.class.getName()) || returnedType.equals(BigDecimal.class.getName()))
				{
					dt.setJavaType(returnedType);
				}
				else
				{
					throw new CodeTypeDeclarationException("Method " + method.getName() + " is annotated with @" + kommet.koll.annotations.DataType.class.getSimpleName() + " and as numeric, but returns " + method.getReturnType().getName());
				}
			}
			else
			{
				if (returnedType.equals(Integer.class.getName()))
				{
					dt.setJavaType(returnedType);
				}
				else
				{
					throw new CodeTypeDeclarationException("Method " + method.getName() + " is annotated with @" + kommet.koll.annotations.DataType.class.getSimpleName() + " and as numeric integer, but returns " + method.getReturnType().getName());
				}
			}
			
			return dt;
		}
		else if (dtAnnot.id() == DataType.BOOLEAN)
		{
			return new BooleanDataType();
		}
		else if (dtAnnot.id() == DataType.ENUMERATION)
		{
			EnumerationDataType dt = new EnumerationDataType();
			dt.setValidateValues(dtAnnot.validateValues());
			dt.setValues(MiscUtils.implode(dtAnnot.values(), "\n"));
			return dt;
		}
		else if (dtAnnot.id() == DataType.DATETIME)
		{
			if (!method.getReturnType().getName().equals(Date.class.getName()))
			{
				throw new CodeTypeDeclarationException("Method " + method.getName() + " is annotated with @" + kommet.koll.annotations.DataType.class.getSimpleName() + " and as date/time, but returns " + method.getReturnType().getName());
			}
			
			return new DateTimeDataType();
		}
		else if (dtAnnot.id() == DataType.DATE)
		{
			if (!method.getReturnType().getName().equals(Date.class.getName()))
			{
				throw new CodeTypeDeclarationException("Method " + method.getName() + " is annotated with @" + kommet.koll.annotations.DataType.class.getSimpleName() + " and as date, but returns " + method.getReturnType().getName());
			}
			
			return new DateDataType();
		}
		else
		{
			throw new CodeTypeDeclarationException("Unsupported data type for code declaration: " + dtAnnot.id());
		}
	}

	private DataType getDataTypeFromMethodReturnType(Method method) throws CodeTypeDeclarationException
	{
		if (method.getReturnType().getName().equals(Integer.class.getName()))
		{
			return new NumberDataType(0, Integer.class);
		}
		else if (method.getReturnType().getName().equals(Double.class.getName()))
		{
			return new NumberDataType(2, Double.class);
		}
		else if (method.getReturnType().getName().equals(String.class.getName()))
		{
			return new TextDataType(255);
		}
		else if (method.getReturnType().getName().equals(Boolean.class.getName()))
		{
			return new BooleanDataType();
		}
		else
		{
			throw new CodeTypeDeclarationException("Cannot deduce data type from return type " + method.getReturnType().getName() + " of method " + method.getName());
		}
	}

	private void handleTypeTriggerAssignments(java.lang.Class<?> compiledClass, Class file, AuthData authData, EnvData env) throws KommetException
	{
		Trigger triggerAnnotation = compiledClass.getAnnotation(Trigger.class);
		if (triggerAnnotation != null)
		{	
			// unconverted KOLL code has type attribute on Trigger annotation set to type name
			String typeName = triggerAnnotation.type();
			Type typeForTrigger = env.getType(typeName);
			if (typeForTrigger == null)
			{
				throw new ClassCompilationException("Type with name " + triggerAnnotation.type() + " does not exist");
			}
			
			file.setJavaCode(getKollTranslator(env).kollToJava(TriggerUtil.convertTriggerKollCode(file.getKollCode(), file.getName(), typeForTrigger, env), true, authData, env));
			
			compiler.resetClassLoader(env);
			CompilationResult result = compiler.compile(file, env);
			
			if (!result.isSuccess())
			{
				throw new ClassCompilationException("Compilation failed", result);
			}
			
			boolean isTriggerRegistered = TriggerService.isTriggerRegisteredWithType(file.getId(), typeForTrigger.getKID(), typeTriggerDao,  env);
			
			// check if the trigger is disabled
			if (compiledClass.isAnnotationPresent(Disabled.class))
			{
				// check if the trigger is registered with the type
				if (isTriggerRegistered)
				{
					// trigger is registered with the type, so we need to disable/unregister it
					TriggerService.unregisterTriggerWithType(file.getId(), typeForTrigger.getKID(), typeTriggerDao, env);
				}
			}
			else
			{
				// trigger should be enabled, if it is not yet enabled
				// check if the trigger is registered with the type
				if (!isTriggerRegistered)
				{
					// trigger is not registered with the type, so we need to register it
					TriggerService.registerTriggerWithType(file, typeForTrigger, false, true, compiler, typeTriggerDao, authData, env);
				}
				else
				{
					TriggerService.updateTriggerWithType(file, typeTriggerDao, compiler, env);
				}
			}
		}
		else
		{
			// trigger annotation is not present, but perhaps it was present before? if so, unregister the trigger
			TriggerService.unregisterTrigger(file.getId(), typeTriggerDao, compiler, authData, env);
		}
	}

	private void validateKollCode(Class file, boolean overwriteNameFromCode, EnvData env) throws InvalidClassCodeException
	{
		String packageName = KollUtil.extractPackageName(file.getKollCode());
		
		if (!overwriteNameFromCode && !file.getPackageName().equals(packageName))
		{
			throw new InvalidClassCodeException("Package in the class code " + packageName + " does not correspond to the file package " + file.getPackageName());
		}
	}

	/**
	 * Check if this class is annotated with @Controller, and if yes, that it fulfills all the criteria
	 * a controller class should meet.
	 * 
	 * @param cls
	 * @throws ClassCompilationException
	 */
	private List<String> validateController(java.lang.Class<?> cls) throws ClassCompilationException
	{
		List<String> errors = new ArrayList<String>();
		
		if (cls.isAnnotationPresent(Controller.class))
		{
			if (!BaseController.class.isAssignableFrom(cls))
			{
				errors.add("Controller class " + cls.getSimpleName() + " does not extend " + BaseController.class.getSimpleName());
			}
		}
		
		boolean hasRestActions = false;
		boolean hasGenericActions = false;
		
		// look for methods annotated with @Rest
		for (Method method : cls.getMethods())
		{
			if (method.isAnnotationPresent(Rest.class))
			{
				hasRestActions = true;
			}
			if (method.isAnnotationPresent(Action.class))
			{
				hasGenericActions = true;
				
				// methods annotated with @Action can return either PageData or byte[] (the latter only if
				// they are also annotated with @ReturnsFile)
				if (method.isAnnotationPresent(ReturnsFile.class))
				{
					// must return byte[]
					if (!method.getReturnType().getName().equals((new byte[0]).getClass().getName()))
					{
						errors.add("Method " + method.getName() + " is annotated with @" + ReturnsFile.class.getSimpleName() + " but does not return byte array");
					}
				}
				else
				{
					// must return page data
					if (!PageData.class.isAssignableFrom(method.getReturnType()))
					{
						errors.add("Method " + method.getName() + " is annotated with @" + Action.class.getSimpleName() + " and is not annotated with @" + ReturnsFile.class.getSimpleName() + ", but does not return type " + PageData.class.getSimpleName());
					}
				}
			}
		}
		
		if (!BaseController.class.isAssignableFrom(cls))
		{
			if (hasRestActions)
			{
				errors.add("Class has methods annotated with @" + Rest.class.getSimpleName() + " but it does not extend " + BaseController.class.getSimpleName());
			}
			if (hasGenericActions)
			{
				errors.add("Class has methods annotated with @" + Action.class.getSimpleName() + " but it does not extend " + BaseController.class.getSimpleName());
			}
		}
		
		return errors;
	}

	@Transactional
	public Class saveSystemFile(Class cls, AuthData authData, EnvData env) throws KommetException
	{
		// system classes have restricted access, users are not allowed to see their code
		cls.setAccessLevel("Closed");
		
		validate(cls, env);
		return classDao.save(cls, true, true, authData, env);
	}

	private void validate(Class file, EnvData env) throws ClassCompilationException
	{
		// make sure the java code starts with the name of the package.
		Pattern p = Pattern.compile("^package\\s+([^;]+);");
		Matcher m = p.matcher(file.getJavaCode());
		if (m.find())
		{
		    if (!m.group(1).equals(MiscUtils.userToEnvPackage(file.getPackageName(), env)))
		    {
		    	throw new ClassCompilationException("Package declared in the Java code (" + m.group(1) + ") does not match the KOLL file package property (" + file.getPackageName() + ")");
		    }
		}
		else
		{
			throw new ClassCompilationException("The Java code of the Koll file does not start with a package declaration.");
		}
	}

	/**
	 * Creates and returns an instance of a class represented by the given file.
	 * @param file
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public Object instantiate (Class file, EnvData env) throws KommetException
	{
		try
		{
			java.lang.Class<?> cls = compiler.getClass(file, true, env);
			return cls.newInstance();
		}
		catch (Exception e)
		{
			throw new KommetException("Error instantiating class " + file.getName() + ": " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public Method getActionMethod (Class file, String methodName, EnvData env) throws KommetException
	{
		try
		{
			@SuppressWarnings("rawtypes")
			java.lang.Class controllerAnnotationClass = compiler.getClass(Controller.class.getName(), false, env);
			@SuppressWarnings("rawtypes")
			java.lang.Class actionAnnotationClass = compiler.getClass(Action.class.getName(), false, env);
			
			java.lang.Class<?> cls = compiler.getClass(file, false, env);
			
			Annotation controllerAnnotation = cls.getAnnotation(controllerAnnotationClass);
			
			if (controllerAnnotation == null)
			{
				Annotation[] controllerAnnotations = cls.getAnnotations();
				
				boolean annotationNameFound = false;
				
				for (int i = 0; i < controllerAnnotations.length; i++)
				{
					if (controllerAnnotations[i].getClass().getName().equals(controllerAnnotationClass.getName()))
					{
						annotationNameFound = true;
						break;
					}
				}
				
				if (!annotationNameFound)
				{
					throw new KommetException("Cannot get action method from class " + file.getName() + " because the class is not annotated with @Controller. If the annotation should be found, try reloading the class by reinitializing the class loader.");
				}
				else
				{
					throw new KommetException("Cannot get action method from class. Class is annotated with @Contructor, but the annotation instance is different (probably retrieved using a different classloader)");
				}
			}
			
			// Find methods annotated with @Action.
			// Note: use getMethods instead of getDeclaredMethods to be able to access also methods from supertypes
			for (Method method : cls.getMethods())
			{
				if (method.getName().equals(methodName) && method.isAnnotationPresent(actionAnnotationClass))
				{
					return method;
				}
			}
			
			return null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new KommetException("Error getting method " + methodName + " in class " + file.getName() + ": " + e.getMessage());
		}
	}

	public Class getClass (String qualifiedName, EnvData env) throws KommetException
	{
		ClassFilter filter = new ClassFilter();
		filter.setQualifiedName(qualifiedName);
		
		List<Class> classes = classDao.find(filter, env);
		if (classes.size() > 1)
		{
			throw new KommetException("More than one KOLL file found with name " + qualifiedName);
		}
		
		return classes.isEmpty() ? null : classes.get(0);
	}

	@Transactional
	public void delete(Class file, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		// fetch the class again to make sure all its fields are initialized
		Class cls = classDao.get(file.getId(), env);
		
		java.lang.Class<?> compiledClass = null;
		
		try
		{
			compiledClass = compiler.getClass(cls, false, env);
		}
		catch (MalformedURLException e)
		{
			throw new KommetException("Error getting compiled class " + cls.getQualifiedName() + ": " + e.getMessage());
		}
		catch (ClassNotFoundException e)
		{
			throw new KommetException("Compiled class not found: " + cls.getQualifiedName());
		}
		
		// check if class is annotated with @Trigger
		if (compiledClass.isAnnotationPresent(Trigger.class))
		{
			// unregister this trigger
			TriggerService.unregisterTrigger(cls.getId(), typeTriggerDao, compiler, authData, env);
		}
		
		env.updateGenericActionsForClass(cls, compiler, systemActionService.getSystemActionURLs(), authData.getI18n(), this.viewDao, true);
		updateSharingRules(file, dataService, authData, env);
		updateTypePersistenceAnnotations(compiledClass, file, dataService, authData, env);
		updateBusinessActionDeclarations(file, authData, env);
		
		classDao.delete(Arrays.asList(cls), null, env);
	}

	private void updateBusinessActionDeclarations(Class file, AuthData authData, EnvData env) throws KommetException
	{
		// check if there is a business action declared previously in this file
		BusinessAction action = bpService.getActionForFile(file.getId(), authData, env);
		
		if (action != null)
		{
			// action was previously declared in this file, but now the @BusinessAction annotation is removed, so the action should be removed if possible
			bpService.deleteAction(action, authData, env);
		}
	}

	private void updateTypePersistenceAnnotations(java.lang.Class<?> cls, Class file, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		if (!cls.isAnnotationPresent(kommet.koll.annotations.Type.class))
		{
			// no type was declared in this class
			return;
		}

		Type type = env.getType(file.getQualifiedName());
		if (type == null)
		{
			throw new KommetException("Type with name " + file.getQualifiedName() + " was declared in class file, but no type definition existed on env");
		}
		dataService.deleteType(type, authData, env);
	}

	private void updateSharingRules(Class file, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		// find sharing rules declared in this file
		SharingRuleFilter filter = new SharingRuleFilter();
		filter.addFileId(file.getId());
		
		sharingRuleService.delete(sharingRuleService.get(filter, authData, env), dataService, authData, env);
	}

	@Transactional
	public void delete(List<Class> classes, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		for (Class cls : classes)
		{
			delete(cls, dataService, authData, env);
		}
	}

	@Transactional
	public void delete(KID id, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		Class cls = classDao.get(id, env);
		
		if (cls == null)
		{
			throw new KommetException("Class with ID " + id + " cannot be deleted because it does not exist");
		}
		
		delete(cls, dataService, authData, env);
	}

	@Transactional
	public Class updateJavaCode(Class cls, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		return fullSave(cls, dataService, true, true, authData, env);
	}
}
