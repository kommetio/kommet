/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.ProfileService;
import kommet.basic.Action;
import kommet.basic.BusinessAction;
import kommet.basic.BusinessProcess;
import kommet.basic.Class;
import kommet.basic.CustomTypeRecordProxy;
import kommet.basic.Dictionary;
import kommet.basic.Profile;
import kommet.basic.RecordAccessType;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyClassGenerator;
import kommet.basic.RecordProxyUtil;
import kommet.basic.StandardTypeRecordProxy;
import kommet.basic.TypeInfo;
import kommet.basic.TypeTrigger;
import kommet.basic.TypeWithTriggersDeleteException;
import kommet.basic.UniqueCheck;
import kommet.basic.View;
import kommet.basic.actions.ActionDao;
import kommet.basic.actions.ActionFilter;
import kommet.basic.actions.ActionService;
import kommet.basic.actions.StandardActionDao;
import kommet.basic.actions.StandardActionType;
import kommet.basic.keetle.StandardTypeControllerUtil;
import kommet.basic.keetle.ViewDao;
import kommet.basic.keetle.ViewUtil;
import kommet.basic.types.ProfileKType;
import kommet.basic.types.SystemTypes;
import kommet.basic.types.UniqueCheckKType;
import kommet.basic.types.UserKType;
import kommet.businessprocess.BusinessProcessExecutor;
import kommet.config.UserSettingKeys;
import kommet.dao.AnyRecordDao;
import kommet.dao.ConstraintViolationException;
import kommet.dao.DaoFacade;
import kommet.dao.FieldDao;
import kommet.dao.FieldDefinitionException;
import kommet.dao.FieldFilter;
import kommet.dao.KommetPersistenceException;
import kommet.dao.UniqueCheckDao;
import kommet.dao.UniqueCheckFilter;
import kommet.dao.dal.InsufficientPrivilegesException;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.FormulaParser;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.data.sharing.SharingService;
import kommet.data.sharing.UserRecordSharingDao;
import kommet.data.validationrules.ValidationRuleError;
import kommet.data.validationrules.ValidationRuleUninitializedFieldsMode;
import kommet.data.validationrules.ValidationRuleUtil;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.errorlog.ErrorLogService;
import kommet.filters.AnyRecordFilter;
import kommet.koll.ClassService;
import kommet.koll.SystemContextFactory;
import kommet.koll.annotations.triggers.AfterDelete;
import kommet.koll.annotations.triggers.AfterInsert;
import kommet.koll.annotations.triggers.AfterUpdate;
import kommet.koll.annotations.triggers.BeforeDelete;
import kommet.koll.annotations.triggers.BeforeInsert;
import kommet.koll.annotations.triggers.BeforeUpdate;
import kommet.koll.annotations.triggers.OldValues;
import kommet.koll.annotations.triggers.Trigger;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.FieldHistoryService;
import kommet.services.SharingRuleService;
import kommet.services.SystemSettingService;
import kommet.triggers.DatabaseTrigger;
import kommet.triggers.InvalidClassForTriggerException;
import kommet.triggers.TriggerException;
import kommet.triggers.TypeTriggerDao;
import kommet.triggers.TypeTriggerFilter;
import kommet.uch.UserCascadeHierarchyDao;
import kommet.utils.AppConfig;
import kommet.utils.JavaCompilerUtils;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;
import kommet.utils.ValidationUtil;

@Service
public class DataService
{	
	@Inject
	TypeDao typeDao;
	
	@Inject
	FieldDao fieldDao;
	
	@Inject
	DaoFacade daoFacade;
	
	@Inject
	UniqueCheckDao uniqueCheckDao;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ClassService classService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ActionService actionService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	ViewDao viewDao;
	
	@Inject
	TypeInfoDao typeInfoDao;
	
	@Inject
	ActionDao actionDao;
	
	@Inject
	StandardActionDao stdActionDao;
	
	@Inject
	FieldHistoryService fieldHistoryService;
	
	@Inject
	SystemSettingService systemSettingService;
	
	@Inject
	TypeTriggerDao typeTriggerDao;
	
	@Inject
	EmailService emailService;
	
	@Inject
	SystemContextFactory systemContextFactory;
	
	@Inject
	UserRecordSharingDao userRecordSharingDao;
	
	@Inject
	AnyRecordDao anyRecordDao;
	
	@Inject
	UserCascadeHierarchyDao uchDao;
	
	@Inject
	SharingRuleService sharingRuleService;
	
	@Inject
	ErrorLogService logService;
	
	private static final Logger log = LoggerFactory.getLogger(DataService.class);
	
	public Record instantiate (KID typeId, EnvData env) throws KommetException
	{
		if (typeId == null)
		{
			throw new KommetException("Attempt to instantiate type by null ID");
		}
		return new Record(getType(typeId, env));
	}
	
	@Transactional
	public void setDefaultField (KID typeId, KID fieldId, AuthData authData, EnvData env) throws KommetException
	{
		Type type = env.getType(typeId);
		type.setDefaultFieldId(fieldId);
		typeDao.update(type, env.getType(typeId), authData, env);
	}
	
	@Transactional
	public Record save (Record record, EnvData env) throws KommetException
	{
		return save(record, false, false, false, false, getRootAuthData(env), env);
	}
	
	public AuthData getRootAuthData(EnvData env) throws KommetException
	{
		return AuthData.getRootAuthData(env);
	}
	
	@Transactional
	public Record save (Record record, boolean skipTriggers, boolean skipSharing, EnvData env) throws KommetException
	{
		return save(record, skipTriggers, skipSharing, false, false, getRootAuthData(env), env);
	}
	
	@Transactional
	public <T extends RecordProxy> T save (T proxy, AuthData authData, EnvData env) throws KommetException
	{
		return save(proxy, authData, false, false, env);
	}
	
	/**
	 * Save an object to the database. The object can be any object proxy - standard or custom. Standard proxies are generated
	 * for built-in types and saved from the non-system context (i.e. user context, such as user-defined code).
	 * @param proxy
	 * @param authData
	 * @param skipTriggers
	 * @param skipSharing
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	public <T extends RecordProxy> T save (T proxy, AuthData authData, boolean skipTriggers, boolean skipSharing, EnvData env) throws KommetException
	{
		Type type = null;
		
		// All proxies passed to this method have to be env-specific. Custom proxies are always env-specific, so that
		// is not a problem. Standard proxies can be both env-specific and non-env-specific, but this method can only be used to save non-env-specific standard proxies.
		if (!MiscUtils.isEnvSpecific(proxy.getClass().getName()))
		{
			if (proxy instanceof StandardTypeRecordProxy)
			{
				throw new EnvSpecificTypeException("Qualified type name " + proxy.getClass().getName() + " is env-specific. The reason is that operation save() has been called on a standard proxy from non-user code. Use service methods from type-specific services instead.", proxy.getClass().getName());
			}
			else
			{
				throw new EnvSpecificTypeException("Qualified custom type name " + proxy.getClass().getName() + " is env-specific", proxy.getClass().getName());
			}
		}
		
		type = env.getType(MiscUtils.envToUserPackage(proxy.getClass().getName(), env));
		
		Record savedRecord = save(RecordProxyUtil.generateRecord(proxy, type, 2, env), skipTriggers, skipSharing, false, false, authData, env);
	
		// convert back to proxy
		if (proxy instanceof CustomTypeRecordProxy)
		{
			return (T)RecordProxyUtil.generateCustomTypeProxy(savedRecord, env, compiler);
		}
		else if (proxy instanceof StandardTypeRecordProxy)
		{
			return (T)RecordProxyUtil.generateStandardTypeProxy(savedRecord, env, compiler);
		}
		else
		{
			throw new KommetException("Unsupported subclass of " + RecordProxy.class.getSimpleName() + ": " + proxy.getClass().getName());
		}
	}

	@Transactional
	public Record save (Record record, AuthData authData, EnvData env) throws KommetException
	{
		return save(record, false, false, false, false, authData, env);
	}
	
	/**
	 * Saves the record.
	 * @param record Record to be saved.
	 * @param skipTriggers If set to true, all triggers will be skipped during the save operation.
	 * @param skipSharing If set to true, user-specific permissions on the given record will be ignored during the edit operation.
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public Record save (Record record, boolean skipTriggers, boolean skipSharing, AuthData authData, EnvData env) throws KommetException
	{
		return save(record, skipTriggers, skipSharing, false, false, authData, env);
	}
	
	/**
	 * Save a record to database.
	 * <p>
	 * This is the main method that should be used to save a record. It contains all the logic regarding saving
	 * records, calling triggers and checking permissions.
	 * </p>
	 * @param record
	 * @param skipTriggers
	 * @param skipSharing
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public Record save (Record record, boolean skipTriggers, boolean skipSharing, boolean skipCreatePermissionCheck, boolean isSilentUpdate, AuthData authData, EnvData env) throws KommetException
	{	
		// Note that the type on the record may be different that the type on the env. In some rare situations
		// it is possible that a type on the env has changed after the record has been created. However, records
		// are so short lived that it does not matter and so we use the version of type cached on the record,
		// not the one on the env because it would require add additional operation to get that type using:
		// env.getType(record.getType().getKeyPrefix())
		Type type = record.getType();
		
		// track history
		boolean trackHistory = false;
		Criteria c = env.getSelectCriteria(type.getKID());
		for (Field field : env.getType(type.getKeyPrefix()).getFields())
		{
			// history can only be tracked for non-collection data types
			if (field.isTrackHistory())
			{
				c.addProperty(field.getApiName());
				trackHistory = true;
			}
		}
		
		// if any fields are supposed to be tracked, fetch the old version of the object before saving
		// (if it has already been saved before)
		Record oldRecord = null;
		if (trackHistory && record.attemptGetKID() != null)
		{
			c.add(Restriction.eq(Field.ID_FIELD_NAME, record.getKID()));
			oldRecord = c.list().get(0);
		}
		
		if (authData == null)
		{
			// auth data should not be null at this point
			// if it is, it usually means that the method has been called from a context that did not inject auth data to the thread
			// using EnvData.addAuthData
			throw new KommetException("AuthData is null. If called manually, authData was not passed to the method. If called from a system context, authData was not injected");
		}
		
		UserKType.validateUserId(authData.getUserId());
		
		Date modificationDate = new Date();
		boolean isInsert = record.attemptGetKID() == null;
		
		if (!isSilentUpdate)
		{	
			record.setLastModifiedBy(authData.getUserId(), env);
			record.setLastModifiedDate(modificationDate);
		}
		else if (isInsert)
		{
			throw new KommetException("Silent update cannot be used when a record is inserted");
		}
		
		if (isInsert)
		{
			record.setCreatedDate(modificationDate);
			record.setCreatedBy(authData.getUserId(), env);
		}
		
		// set default access type if this is an insert of a new record
		if (isInsert && !record.isSet(Field.ACCESS_TYPE_FIELD_NAME))
		{
			record.setAccessType(RecordAccessType.PUBLIC.getId());
		}
		
		Record clonedRecord = record;
		
		// make sure user has permissions to create records of this type
		if (isInsert && !skipCreatePermissionCheck && !authData.canCreateType(record.getType().getKID(), true, env))
		{
			throw new InsufficientPrivilegesException(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_INSERT_TYPE_MSG + " " + record.getType().getQualifiedName() + " for profile " + authData.getProfile().getName());
		}
		
		RecordProxy proxyForTriggers = null;
		List<RecordProxy> oldProxies = null;
		
		// triggers are executed for all custom types and accessible standard types
		if (!skipTriggers && !SystemTypes.isInaccessibleSystemType(type))
		{
			// clone the record
			clonedRecord = MiscUtils.shallowCloneRecord(record);
			
			if (!env.getTriggers(type.getKID()).isEmpty())
			{
				// generate proxy - use custom proxy since it will be used in KOLL trigger
				proxyForTriggers = RecordProxyUtil.generateCustomTypeProxy(clonedRecord, true, env, compiler);
				
				// old values are injected to @before triggers only if it's an update, not insert, because insert does not have old values 
				if (oldProxies == null && env.hasTypeBeforeTriggersWithOldProxies(type.getKID()) && !isInsert)
				{
					log.debug("Initializing old proxies for @Before triggers");
					oldProxies = initOldProxies(Arrays.asList(proxyForTriggers), type, authData, env);
				}
				
				// pass the cloned record to trigger
				callTriggers(MiscUtils.toList(proxyForTriggers), oldProxies, clonedRecord.getType(), isInsert, !isInsert, false, true, false, authData, env);
				
				// convert proxy back to record
				clonedRecord = RecordProxyUtil.generateRecord(proxyForTriggers, clonedRecord.getType(), 100, env);
			}			
		}
		
		// set default values on the record - only if it's an insert
		if (isInsert)
		{
			clonedRecord = setDefaultValues(clonedRecord, type);
		}
		
		FieldValidationException fieldValidationException = null;
		
		// field validation is run after triggers are executed and after default values are set
		try
		{
			runFieldValidation(clonedRecord, env);
		}
		catch (FieldValidationException e)
		{
			fieldValidationException = e;
		}
		
		// run validation rules
		if (env.hasValidationRules(type.getKID()))
		{
			if (proxyForTriggers == null)
			{
				proxyForTriggers = RecordProxyUtil.generateCustomTypeProxy(clonedRecord, true, env, compiler);
			}
			
			// describes how a validation rule should act if some of the fields used in its condition are not initialized on the object
			// if it's an insert, all fields need to be initialized, otherwise we can use a value from a setting
			String uninitializedFieldsMode = isInsert ? ValidationRuleUninitializedFieldsMode.EVALUATE.getMode() : getUserSettingValue(UserSettingKeys.KM_ROOT_SYS_VALIDATION_RULE_UNINITIALIZED_FIELDS_MODE, authData, AuthData.getRootAuthData(env), env);
			
			Set<ValidationRuleError> errors = ValidationRuleUtil.runValidationRules(proxyForTriggers, uninitializedFieldsMode, type.getKeyPrefix(), compiler, authData, env);
			
			if (!errors.isEmpty())
			{	
				if (fieldValidationException == null)
				{
					fieldValidationException = new FieldValidationException();
				}
				
				for (ValidationRuleError err : errors)
				{
					// if error message label is defined, use it, otherwise use standard message 
					fieldValidationException.addMessage((StringUtils.hasText(err.getMessageLabel()) && authData != null && authData.getLocale() != null) ? env.getTextLabelDictionary().get(err.getMessageLabel(), authData.getLocale()) : err.getMessage(), null, null, ValidationErrorType.VALIDATION_RULE_VIOLATION);
				}
			}
		}
		
		if (fieldValidationException != null)
		{
			throw fieldValidationException;
		}
		
		// perform the save on the cloned record, because it may contain
		// changes resulting from triggers
		Record savedRecord = null;
		
		savedRecord = daoFacade.save(clonedRecord, skipSharing, authData, env);
		
		// set the ID of the saved record to the record passed to the save method
		record.setKID(clonedRecord.getKID());
		
		// track history
		if (trackHistory)
		{
			for (Field field : env.getType(type.getKeyPrefix()).getFields())
			{
				if (!field.isTrackHistory() || !record.isSet(field.getApiName()))
				{
					continue;
				}
				
				Object oldValue = oldRecord != null ? oldRecord.attemptGetField(field.getApiName()) : null; 
				Object newValue = record.attemptGetField(field.getApiName());
				
				// TODO comparing string values is not optimal
				if (!field.getDataType().getStringValue(oldValue, authData.getLocale()).equals(field.getDataType().getStringValue(newValue, authData.getLocale())))
				{
					// TODO optimize this - perform one save for all fields
					fieldHistoryService.logFieldUpdate(field, record.getKID(), oldValue, record.getField(field.getApiName()), authData, env);
				}
			}
		}
		
		if (!type.isBasic() && isInsert && appConfig.isCreateAnyRecords())
		{
			Record anyRecord = new Record(env.getType(KeyPrefix.get(KID.ANY_RECORD_PREFIX)));
			anyRecord.setField("recordId", record.getKID());
			save(anyRecord, env);
		}
		
		// if a user created a record, automatically create a user-record sharing for the creating user
		//if (!type.isBasic() || !SystemTypes.isInaccessibleSystemType(type))
		if (!skipSharing && isInsert)
		{
			SharingService.shareRecord(record.getKID(), authData.getUserId(), true, true, "Record creator", false, null, null, compiler, userRecordSharingDao, this, authData, false, env);
		}
		
		// Call "after" triggers
		// Triggers are executed for all custom types and accessible standard types
		if (!skipTriggers && !SystemTypes.isInaccessibleSystemType(type))
		{
			// clone the record
			clonedRecord = MiscUtils.shallowCloneRecord(savedRecord);
			
			if (!env.getTriggers(type.getKID()).isEmpty())
			{
				// generate proxy - use custom proxy since it will be used in KOLL trigger
				RecordProxy proxy = RecordProxyUtil.generateCustomTypeProxy(clonedRecord, true, env, compiler);
				
				if (oldProxies == null && env.hasTypeAfterTriggersWithOldProxies(type.getKID()) && !isInsert)
				{
					log.debug("Initializing old proxies for @After triggers");
					oldProxies = initOldProxies(Arrays.asList(proxyForTriggers), type, authData, env);
				}
				
				// pass the cloned record to trigger
				callTriggers(MiscUtils.toList(proxy), isInsert ? null : oldProxies, savedRecord.getType(), isInsert, !isInsert, false, false, true, authData, env);
			}			
		}
		
		boolean isAvailableType = !type.isBasic() || !SystemTypes.isInaccessibleSystemType(type);
		
		// check if among triggerable processes there are processes for this type, or for any type
		if (isAvailableType && (env.getTriggerableBusinessProcesses().get(savedRecord.getType().getKID()) != null || env.getTriggerableBusinessProcesses().containsKey(RecordProxy.class.getName())))
		{
			List<BusinessProcess> processesForType = env.getTriggerableBusinessProcesses().get(savedRecord.getType().getKID());
			List<BusinessProcess> processesForAllTypes = env.getTriggerableBusinessProcesses().get(RecordProxy.class.getName());
			
			List<BusinessProcess> processes = new ArrayList<BusinessProcess>();
			if (processesForType != null)
			{
				processes.addAll(processesForType);
			}
			if (processesForAllTypes != null)
			{
				processes.addAll(processesForAllTypes);
			}
			
			if (processes != null)
			{
				for (BusinessProcess process : processes)
				{	
					// get cached process executor
					BusinessProcessExecutor processExecutor = env.getProcessExecutor(process, compiler, logService, classService, this);
					
					// for every process, we query the record with all its fields to make sure that
					// 1) it has all fields initialized
					// 2) changes made by previous processes run in this loop are reflected in the record
					Record recordForProcess = this.getRecords(Arrays.asList(savedRecord.getKID()), type, DataAccessUtil.getReadableFieldApiNamesForQuery(type, authData, env, false), authData, env).get(0);
					
					BusinessAction entryPoint = processExecutor.getEntryPoint().getInvokedAction();
				
					// check if the action performed on the record (insert/update) should trigger this process or not
					if ((entryPoint.getType().equals("RecordCreate") && isInsert) || ((entryPoint.getType().equals("RecordUpdate")) && !isInsert) || entryPoint.getType().equals("RecordSave"))
					{	
						Map<String, Object> processInputs = new HashMap<String, Object>();
						
						// process entry points take record proxies as parameters
						// TODO the record proxy was already created before - it should not be created again here
						processInputs.put(process.getSingleInput().getName(), RecordProxyUtil.generateCustomTypeProxy(recordForProcess, true, env, compiler));
						processExecutor.execute(processInputs, authData, true);
					}
				}
			}
		}
		
		if (isAvailableType)
		{
			// recalculate sharings for this record
			sharingRuleService.recalculateSharingForType(type.getKID(), savedRecord.getKID(), isInsert, this, env);
			sharingRuleService.recalculateDependentSharingForType(type.getKID(), this, env);
		}
		
		return savedRecord;
	}

	/**
	 * Sets the default values of fields.
	 * @param record
	 * @param type
	 * @return
	 * @throws KommetException
	 */
	private Record setDefaultValues(Record record, Type type) throws KommetException
	{
		// iterate over fields
		for (Field field : type.getFieldsWithDefaultValues().values())
		{
			if (field.getDefaultValue() != null && !record.isSet(field.getApiName()))
			{
				if (!field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					// apply the default value
					record.setField(field.getApiName(), field.getDefaultValue());
				}
				else
				{
					// default ID needs to be converted into a record and only then assigned to the field
					Record defaultRecord = new Record(((TypeReference)field.getDataType()).getType());
					defaultRecord.setKID(KID.get(field.getDefaultValue()));
					record.setField(field.getApiName(), defaultRecord);
				}
			}
		}
		
		return record;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void callTriggers(List<RecordProxy> proxies, List<RecordProxy> oldProxies, Type type, boolean isInsert, boolean isUpdate, boolean isDelete, boolean isBefore, boolean isAfter, AuthData authData, EnvData env) throws KommetException
	{
		if (!isDelete)
		{
			if (proxies == null)
			{	
				throw new KommetException("Null proxy list passed to callTriggers");
			}
			else if (proxies.isEmpty())
			{
				throw new KommetException("Non-null, empty proxy list passed to callTriggers");
			}
		}
		
		if (isDelete)
		{
			if (oldProxies == null)
			{	
				throw new KommetException("Null old proxy list passed to callTriggers");
			}
			else if (oldProxies.isEmpty())
			{
				throw new KommetException("Non-null, empty old proxy list passed to callTriggers");
			}
		}
		
		if (isBefore && isAfter)
		{
			throw new KommetException("Before and After triggers cannot be called simultaneously");
		}
		
		List<java.lang.Class<? extends Annotation>> applicableAnnotations = new ArrayList<java.lang.Class<? extends Annotation>>();
		if (isBefore)
		{
			if (isInsert)
			{
				applicableAnnotations.add(BeforeInsert.class);
			}
			if (isUpdate)
			{
				applicableAnnotations.add(BeforeUpdate.class);
			}
			if (isDelete)
			{
				applicableAnnotations.add(BeforeDelete.class);
			}
		}
		else if (isAfter)
		{
			if (isInsert)
			{
				applicableAnnotations.add(AfterInsert.class);
			}
			if (isUpdate)
			{
				applicableAnnotations.add(AfterUpdate.class);
			}
			if (isDelete)
			{
				applicableAnnotations.add(AfterDelete.class);
			}
		}
		
		Map<KID, TypeTrigger> triggers = env.getTriggers(type.getKID());
		for (TypeTrigger trigger : triggers.values())
		{
			String triggerName = trigger.getTriggerFile().getPackageName() + "." + trigger.getTriggerFile().getName();
			java.lang.Class<?> triggerClass = null;
			try
			{
				triggerClass = compiler.getClass(triggerName, true, env);
			}
			catch (ClassNotFoundException e)
			{
				throw new KommetException("Trigger class " + triggerName + " not found");
			}
			
			boolean triggerApplies = false;
			
			// check if the class's annotation apply to the current operation
			for (Annotation annotation : triggerClass.getAnnotations())
			{
				if (applicableAnnotations.contains(annotation.annotationType()))
				{
					triggerApplies = true;
					break;
				}
			}
			
			// if none of the trigger on this type applies to the current operation
			// continue to the next trigger
			if (!triggerApplies)
			{
				continue;
			}
			
			Method executeMethod = null;
			
			// instantiate trigger class
			Object triggerInstance;
			
			if (!triggerClass.getSuperclass().getName().equals(DatabaseTrigger.class.getName()))
			{
				throw new InvalidClassForTriggerException("Trigger class does not extend class " + DatabaseTrigger.class.getName() + ". Instead, it extends " + triggerClass.getSuperclass().getName());
			}
			
			try
			{
				triggerInstance = triggerClass.newInstance();
			}
			catch (Exception e)
			{
				throw new KommetException("Error instantiating trigger class: " + e.getMessage(), e);
			}
			
			((DatabaseTrigger<?>)triggerInstance).setInsert(isInsert);
			((DatabaseTrigger<?>)triggerInstance).setUpdate(isUpdate);
			((DatabaseTrigger<?>)triggerInstance).setDelete(isDelete);
			((DatabaseTrigger<?>)triggerInstance).setAfter(isAfter);
			((DatabaseTrigger<?>)triggerInstance).setBefore(isBefore);
			
			// inject KOLL context
			((DatabaseTrigger<?>)triggerInstance).setSystemContext(systemContextFactory.get(authData, env));
			
			try
			{
				executeMethod = triggerClass.getMethod("execute");
			}
			catch (Exception e)
			{
				throw new KommetException("No execute method with proper signature found in trigger class " + triggerName);
			}
			
			try
			{
				((DatabaseTrigger)triggerInstance).setNewValues(proxies);
				
				// only @Delete triggers or triggers annotated with @OldValues have old values injected
				((DatabaseTrigger)triggerInstance).setOldValues((isDelete || triggerClass.isAnnotationPresent(OldValues.class)) ? oldProxies : null);
				
				//Thread.currentThread().getId();
				
				executeMethod.invoke(triggerInstance);
				
				// rewrite updated records from the trigger class
				//updatedRecords = ((TriggerFile)triggerInstance).getNewValues();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				String msg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : "[no message available]";
				throw new KommetException("Error calling trigger: " + msg, e);
			}
		}
	}
	
	/**
	 * Initializes old proxies from database.
	 * @param proxies
	 * @param type
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private List<RecordProxy> initOldProxies(List<RecordProxy> proxies, Type type, AuthData authData, EnvData env) throws KommetException
	{
		Set<KID> recordIds = new HashSet<KID>();
		for (RecordProxy proxy : proxies)
		{
			if (proxy.getId() != null)
			{
				recordIds.add(proxy.getId());
			}
		}
		
		List<String> fieldNames = DataAccessUtil.getReadableFieldApiNamesForQuery(type, authData, env, true);
		List<Record> records = getRecords(recordIds, type, fieldNames, authData, env);
		
		List<RecordProxy> oldProxies = new ArrayList<RecordProxy>();
		for (Record r : records)
		{
			oldProxies.add(RecordProxyUtil.generateCustomTypeProxy(r, env, compiler));
		}
		
		return oldProxies;
	}

	@Transactional
	public OperationResult deleteType (Type type, AuthData authData, EnvData env) throws KommetException
	{
		return deleteType(type, true, authData, env);
	}
	
	@Transactional
	public OperationResult deleteType (Type type, boolean isUpdateTriggerClasses, AuthData authData, EnvData env) throws KommetException
	{
		return deleteType(type, isUpdateTriggerClasses, false, authData, env);
	}

	/**
	 * Deletes the type and all associated items (fields, type triggers etc.)
	 * @param type
	 * @param isUpdateTriggerClasses - tells how trigger should be treated. If set to true, @Trigger annotation is removed from the class files. If false, an error is
	 * thrown is triggers exist for the type.
	 * @param authData
	 * @param env
	 * @param forceCleanUp Ignores missing type information records and continues with the delete
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public OperationResult deleteType (Type type, boolean isUpdateTriggerClasses, boolean forceCleanUp, AuthData authData, EnvData env) throws KommetException
	{
		validateTypeRemoval(type, env);
		
		if (forceCleanUp && !AuthUtil.isRoot(authData))
		{
			throw new KommetException("Only root user can delete type with the forceCleanup flag");
		}
		
		// check if triggers exist for this type
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTypeId(type.getKID());
		filter.setInitClassCode(true);
		List<TypeTrigger> typeTriggers = typeTriggerDao.find(filter, env);
		if (!typeTriggers.isEmpty())
		{
			List<Class> triggerClasses = new ArrayList<Class>();
			for (TypeTrigger tt : typeTriggers)
			{
				triggerClasses.add(tt.getTriggerFile());
			}
			
			if (!isUpdateTriggerClasses)
			{	
				throw new TypeWithTriggersDeleteException("Type cannot be deleted because triggers exist for it", triggerClasses);
			}
			else
			{
				removeTriggerAnnotation(triggerClasses, authData, env);
			}
		}
		
		// get type info
		TypeInfoFilter tiFilter = new TypeInfoFilter();
		tiFilter.addTypeId(type.getKID());
		List<TypeInfo> typeInfos = typeInfoDao.find(tiFilter, env);
		
		TypeInfo typeInfo = null;
		
		if (typeInfos.isEmpty())
		{
			if (!forceCleanUp)
			{
				throw new KommetException("Type information object for type " + type.getQualifiedName() + " not found");
			}
		}
		else
		{
			typeInfo = typeInfos.get(0);
			
			OperationResult canDeleteTypeResult = canDeleteType(type, env, typeInfo);
			
			if (!canDeleteTypeResult.isResult())
			{
				return canDeleteTypeResult;
			}
			
			// remove type infos
			typeInfoDao.delete(typeInfos, true, null, env);
		}
		
		// delete all permissions for this type
		deletePermissionsForType(type.getKID(), authData, env);
		
		if (!type.isBasic())
		{	
			try
			{
				deleteTypeProxy(type, authData, env);
			}
			catch (KommetException e)
			{
				// ignore specific types of exceptions in force cleanup mode because these exceptions are allowed to happen
				if (!ExceptionErrorType.PROXY_MAPPING_NOT_FOUND.equals(e.getErrorType()) || !forceCleanUp)
				{
					throw e;
				}
			}
			
			if (typeInfo != null)
			{
				// generate standard actions for basic operations and for all profiles
				deleteStandardActionsForType(typeInfo, authData, env);
				
				classService.delete(typeInfo.getStandardController(), this, authData, env);
			}
		}
		
		// delete unique checks for this type
		UniqueCheckFilter ucFilter = new UniqueCheckFilter();
		ucFilter.addTypeId(type.getKID());
		uniqueCheckDao.delete(uniqueCheckDao.find(ucFilter, env, this), true, null, env);
		
		// delete all type fields
		Type deletedType = env.getType(type.getKeyPrefix());
		for (Field field : deletedType.getFields())
		{
			// delete field with option "do not delete unique checks" because they save already been deleted above
			deleteField(field, false, true, authData, env);
		}
		
		// delete DB table for type
		typeDao.deleteDbTableForType(deletedType, env);
		
		// delete type record
		typeDao.delete(deletedType.getKID(), env);
		
		// insert the object mapping into the env's data
		env.removeType(type);
		
		// delete triggers
		deleteTriggersForType(type.getKID(), env);
		
		return new OperationResult(true, null);
	}

	private void validateTypeRemoval(Type type, EnvData env) throws KommetException
	{
		// make sure this type is not referenced by any type reference or inverse collection fields
		List<Field> allFieldsOnEnv = getFields(new FieldFilter(), env);
		
		for (Field field : allFieldsOnEnv)
		{
			if (field.getType().getKID().equals(type.getKID()))
			{
				// do not check fields on the type that is being removed
				continue;
			}
			
			if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				if (((TypeReference)field.getDataType()).getTypeId().equals(type.getKID()))
				{
					throw new TypeRemovalException("Type cannot be deleted because it is referenced by field " + field.getType().getQualifiedName() + "." + field.getApiName());
				}
			}
			else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
			{
				if (((InverseCollectionDataType)field.getDataType()).getInverseTypeId().equals(type.getKID()))
				{
					throw new TypeRemovalException("Type cannot be deleted because it is referenced by field " + field.getType().getQualifiedName() + "." + field.getApiName());
				}
			}
			// TODO no unit tests exist for checking if this works
			else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				if (((AssociationDataType)field.getDataType()).getLinkingTypeId().equals(type.getKID()) || ((AssociationDataType)field.getDataType()).getAssociatedTypeId().equals(type.getKID()))
				{
					throw new TypeRemovalException("Type cannot be deleted because it is referenced by field " + field.getType().getQualifiedName() + "." + field.getApiName());
				}
			}
		}
	}

	private void removeTriggerAnnotation(List<Class> triggerClasses, AuthData authData, EnvData env) throws KommetException
	{
		for (Class cls : triggerClasses)
		{
			String code = cls.getKollCode();
			
			CompilationUnit cu = JavaCompilerUtils.getCompilationUnit(code);
		
			final AST ast = cu.getAST();
			final ASTRewrite rewrite = ASTRewrite.create(ast);
			Document document = new Document(code);
			
			cu.accept(new ASTVisitor()
		    {	
				public boolean visit(NormalAnnotation state)
		        {
					if (state.getClass().getName().equals(Trigger.class.getName()))
					{
						rewrite.remove(state, null);
					}
					
		            return true;
		        }
		    });
			
			// get text edits (updates to source code) created by the ASTRewriter
			TextEdit edits = rewrite.rewriteAST(document, null);

			try
			{
				// apply updates to the source code document
				edits.apply(document);
			}
			catch (MalformedTreeException e)
			{
				throw new KommetException("Could not rewrite class code. Nested: " + e.getMessage());
			}
			catch (BadLocationException e)
			{
				throw new KommetException("Could not rewrite class code. Nested: " + e.getMessage());
			}
			
			String updatedKollCode = document.get();
			cls.setKollCode(updatedKollCode);
			classService.fullSave(cls, this, authData, env);
		}
	}

	private void deleteTriggersForType(KID typeId, EnvData env) throws KommetException
	{
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTypeId(typeId);
		List<TypeTrigger> typeTriggers = typeTriggerDao.find(filter, env);
		typeTriggerDao.delete(typeTriggers, true, null, env);
	}

	private void deletePermissionsForType(KID typeId, AuthData authData, EnvData env) throws KommetException
	{
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.TYPE_PERMISSION_API_NAME + " where typeId = '" + typeId + "'").list();
		deleteRecords(permissions, true, authData, env);
	}
	
	private void deletePermissionsForField(KID fieldId, AuthData authData, EnvData env) throws KommetException
	{
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.FIELD_PERMISSION_API_NAME + " where fieldId = '" + fieldId + "'").list();
		deleteRecords(permissions, true, authData, env);
	}
	
	private void deletePermissionsForPage(KID actionId, AuthData authData, EnvData env) throws KommetException
	{
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.ACTION_PERMISSION_API_NAME + " where action.id = '" + actionId + "'").list();
		deleteRecords(permissions, true, authData, env);
	}

	private void deleteStandardActionsForType(TypeInfo typeInfo, AuthData authData, EnvData env) throws KommetException
	{	
		// remove standard page assignments
		stdActionDao.delete(actionService.getStandardActionsForType(typeInfo.getTypeId(), env), true, null, env);
		
		// delete pages for this type
		// (in the future perhaps more pages will be added to this list, e.g. pages that are not
		// standard pages for the deleted type but are not used by any other type and can be deleted as well?
		List<Action> actions = new ArrayList<Action>();
		actions.add(typeInfo.getDefaultCreateAction());
		actions.add(typeInfo.getDefaultEditAction());
		actions.add(typeInfo.getDefaultDetailsAction());
		actions.add(typeInfo.getDefaultListAction());
		actions.add(typeInfo.getDefaultSaveAction());
		
		List<View> viewsToDelete = new ArrayList<View>();
		
		for (Action action : actions)
		{
			deletePermissionsForPage(action.getId(), authData, env);
			viewsToDelete.add(action.getView());
			
			env.removeActionForUrl(action.getUrl());
		}
		
		// remove pages
		actionDao.delete(actions, null, env);
		
		// delete views associated with the default pages
		viewDao.delete(viewsToDelete, null, env);
	}

	private OperationResult canDeleteType(Type type, EnvData env, TypeInfo typeInfo) throws KommetException
	{
		Class stdController = typeInfo.getStandardController();
		
		// make sure the stdcontroller is not used by any pages except standard pages for this type
		ActionFilter filter = new ActionFilter();
		filter.setControllerId(stdController.getId());
		filter.setIsSystem(false);
		List<Action> pagesUsingController = actionService.getActions(filter, env);
		if (!pagesUsingController.isEmpty())
		{
			// TODO shouldn't we be removing users pages as well? or at least list them in the error msg below
			return new OperationResult(false, "Type cannot be deleted because its standard controller is used by user-defined actions");
		}
		
		return new OperationResult(true, null);
	}
	
	@Transactional
	public void deleteRecord (Record record, EnvData env) throws KommetException
	{
		deleteRecord(record, false, null, env);
	}
	
	@Transactional
	public void deleteRecord (Record record, AuthData authData, EnvData env) throws KommetException
	{
		deleteRecord(record, false, authData, env);
	}
	
	@Transactional
	public void deleteRecord (Record record, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{	
		// delete possible entries in association linking tables for this record
		// deleteAssociations(records, env);
		deleteRecords(MiscUtils.toList(record), skipTriggers, authData, env);
	}
	
	/*private void deleteAssociations(Collection<Record> records, EnvData env) throws KommetException
	{
		// find association fields on this record's type
		List<Type> linkingTypes = new ArrayList<Type>();
		Type recordType = env.getType(records.iterator().next().getType().getKID());
		for (Field field : recordType.getFields())
		{
			if (field.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				linkingTypes.add(((AssociationDataType)field.getDataType()).getLinkingType());
			}
		}
		
		
		
		// find association fields for which this records type is an associated type
		FieldFilter filter = new FieldFilter();
		filter.setDataType(new AssociationDataType());
		filter.setAssociatedTypeId(recordType.getKID());
		List<Field> foreignAssociationFields = getFields(filter, env);
	}*/

	@Transactional
	public void deleteRecords (Collection<Record> records, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{
		if (records == null)
		{
			throw new KommetException("Record list is null");
		}
		
		if (records.isEmpty())
		{
			return;
		}
		
		if (authData == null)
		{
			// assume root auth data
			authData = AuthData.getRootAuthData(env);
		}
		
		Type type = records.iterator().next().getType();
		
		List<Record> clonedRecords = new ArrayList<Record>();
		for (Record rec : records)
		{
			clonedRecords.add(MiscUtils.shallowCloneRecord(rec));
		}
		
		// triggers are executed for all custom types and accessible standard types
		if (!skipTriggers && !SystemTypes.isInaccessibleSystemType(type) && !env.getTriggers(type.getKID()).isEmpty())
		{
			if (authData == null)
			{
				throw new TriggerException("Cannot call trigger when no auth data is supplied");
			}
			
			// clone records
			List<RecordProxy> proxies = new ArrayList<RecordProxy>();
			for (Record rec : clonedRecords)
			{
				// generate proxy - use custom proxy since it will be used in KOLL trigger
				proxies.add(RecordProxyUtil.generateCustomTypeProxy(rec, true, env, compiler));
			}
				
			// pass the cloned record to trigger as old values
			callTriggers(null, proxies, type, false, false, true, true, false, authData, env);
			
			clonedRecords.clear();
			
			for (RecordProxy proxy : proxies)
			{
				// convert proxy back to record
				clonedRecords.add(RecordProxyUtil.generateRecord(proxy, type, 100, env));
			}
		}
		
		// delete possible entries in association linking tables for this record
		// deleteAssociations(records, env);
		daoFacade.delete(clonedRecords, authData, env);
		
		if (!skipTriggers && !SystemTypes.isInaccessibleSystemType(type) && !env.getTriggers(type.getKID()).isEmpty())
		{
			if (authData == null)
			{
				throw new TriggerException("Cannot call trigger when no auth data is supplied");
			}
			
			// clone records
			List<RecordProxy> proxies = new ArrayList<RecordProxy>();
			for (Record rec : clonedRecords)
			{
				// generate proxy - use custom proxy since it will be used in KOLL trigger
				proxies.add(RecordProxyUtil.generateCustomTypeProxy(rec, true, env, compiler));
			}
				
			// pass the cloned record to trigger as old values
			callTriggers(null, proxies, type, false, false, true, false, true, authData, env);
		}
		
		if (!type.isBasic())
		{
			AnyRecordFilter anyRecordFilter = new AnyRecordFilter();
			for (Record rec : records)
			{
				anyRecordFilter.addRecordId(rec.getKID());
			}
			anyRecordDao.delete(anyRecordDao.get(anyRecordFilter, AuthData.getRootAuthData(env), env), AuthData.getRootAuthData(env), env); 
		}
	}
	
	@Transactional (readOnly = true)
	public Type getByKID (KID rid, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		return typeDao.getByKID(rid, appConfig, env);
	}
	
	/**
	 * Deletes children of a given record whose parent is the given record and that have cascade
	 * property set.
	 * @param record
	 * @param env
	 * @throws KommetException 
	 */
	/*private void cascadeDeleteChildren(Record record, EnvData env)
	{
		Type obj = env.getType(record.getType().getKID());
		for (KField field : obj.getFields())
		{
			if (field.getDataType().getId().equals(DataType.REFERENCE) && ((ObjectReference)field.getDataType()).isCascadeDelete())
			{
				
			}
		}
	}*/
	
	/**
	 * Deletes the field and all related unique checks from the data base.
	 * @param field
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	@Transactional
	public void deleteField (Field field, AuthData authData, EnvData env) throws KommetException
	{
		deleteField(field, true, false, authData, env);
	}
	
	/**
	 * Deletes a field from a type
	 * @param field
	 * @param deleteUniqueChecks
	 * @param deletedWithType - tells if the field is deleted together with its whole type. If yes, some operations are skipped because
	 * they will be performed by the type delete operation
	 * @param env
	 * @throws KommetException
	 */
	public void deleteField (Field field, boolean deleteUniqueChecks, boolean deletedWithType, AuthData authData, EnvData env) throws KommetException
	{	
		// make sure the field can be removed
		validateFieldRemoval(field, env);
		
		Type type = field.getType();
		if (type == null)
		{
			throw new KommetException("Type not set on removed field");
		}
		
		// make sure default field is not removed
		if (!deletedWithType && env.getType(type.getKeyPrefix()).getDefaultFieldId().equals(field.getKID()))
		{
			throw new KommetException("Field " + field.getApiName() + " cannot be deleted because it is the default field for type " + type.getQualifiedName());
		}
		
		if (field.getKID() == null)
		{
			throw new KommetException("Trying to delete an unsaved field (one with null KID)");
		}
		
		// delete the field definition
		fieldDao.delete(field, env);
		
		deletePermissionsForField(field.getKID(), authData, env);
		
		// delete the field's database representation if it has any
		if (!deletedWithType && !field.getDataType().isCollection())
		{
			// delete db column unless it's an inverse collection or association which has no column
			// TODO write test for deleting inverse collection column
			fieldDao.deleteDbColumnForField(field, env);
		}
		
		// delete unique checks for the field
		List<UniqueCheck> uniqueChecks = uniqueCheckDao.findForField(field, env, this);
		
		if (!uniqueChecks.isEmpty())
		{
			if (deleteUniqueChecks)
			{
				uniqueCheckDao.delete(uniqueChecks, true, null, env);
			}
			else
			{
				throw new KommetException("Field " + field.getApiName() + " cannot be deleted because there are unique checks on it");
			}
		}
		
		// if the deleted field is an association that uses an automatic linking type, we need to delete this type as well
		if (field.getDataTypeId().equals(DataType.ASSOCIATION) && ((AssociationDataType)field.getDataType()).getLinkingType().isAutoLinkingType())
		{
			deleteType(((AssociationDataType)field.getDataType()).getLinkingType(), AuthData.getRootAuthData(env), env);
		}
		
		// remove the field from the type's definition on the env
		env.removeField(field.getType(), field.getApiName());
	}

	private void validateFieldRemoval (Field field, EnvData env) throws KommetException
	{
		// if it is a type reference, make sure it is not used by any inverse collection fields
		if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
		{
			TypeReference dt = (TypeReference)field.getDataType();
			Type refType = env.getType(dt.getTypeId());
			
			// scan fields on the referenced type
			for (Field refTypeField : refType.getFields())
			{
				if (refTypeField.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
				{
					InverseCollectionDataType inverseDT = (InverseCollectionDataType)refTypeField.getDataType(); 
					if (inverseDT.getInverseTypeId().equals(field.getType().getKID()) && field.getApiName().equals(inverseDT.getInverseProperty()))
					{
						throw new FieldRemovalException("Field " + field.getType().getQualifiedName() + "." + field.getApiName() + " cannot be removed because it is used by an inverse collection " + inverseDT.getInverseType().getQualifiedName() + "." + refTypeField.getApiName());
					}
				}
			}
			
			// find all fields on env and make sure this fields type is not used in any association relationship
			FieldFilter filter = new FieldFilter();
			filter.setDataType(new AssociationDataType());
			
			for (Field assocField : getFields(filter, env))
			{
				AssociationDataType assocDT = (AssociationDataType)assocField.getDataType();
				if (assocDT.getLinkingTypeId().equals(field.getType().getKID()))
				{
					if (field.getApiName().equals(assocDT.getSelfLinkingField()) || field.getApiName().equals(assocDT.getForeignLinkingField()))
					{
						throw new FieldRemovalException("Field " + field.getType().getQualifiedName() + "." + field.getApiName() + " cannot be removed because it is used by an association " + env.getType(assocField.getType().getKID()).getQualifiedName() + "." + assocField.getApiName());
					}
				}
			}
		}
		
		// make sure the field is not used in formula fields
		if (!getFormulaFieldsUsingField(field.getKID(), env).isEmpty())
		{
			throw new FieldRemovalException("Field cannot be deleted because it is used in formula fields");
		}
	}

	@Transactional(readOnly = true)
	public List<Field> getFormulaFieldsUsingField(KID rid, EnvData env) throws KommetException
	{
		FieldFilter filter = new FieldFilter();
		filter.setFormulaFieldId(rid);
		return getFields(filter, env);
	}

	private void runFieldValidation (Record record, EnvData env) throws KommetException
	{	
		List<ValidationError> errors = checkRequiredFields(record, true, record.attemptGetKID() != null);
		
		// check text fields length
		// TODO merge this check with the checkRequiredFields method because it performs the same iteration
		// over fields
		for (Field field : record.getType().getFields())
		{
			if (field.getDataType().getId().equals(DataType.TEXT))
			{
				Object value = record.getField(field.getApiName(), false);
				if (value != null && !SpecialValue.isNull(value) && ((String)value).length() > ((TextDataType)field.getDataType()).getLength())
				{
					errors.add(new ValidationError(field, "String value for field " + field.getApiName() + " exceeds the maximum length " + ((TextDataType)field.getDataType()).getLength(), ValidationErrorType.STRING_VALUE_TOO_LONG));
				}
			}
			else if (field.getDataType().getId().equals(DataType.EMAIL))
			{
				Object value = record.getField(field.getApiName(), false);
				if (value != null && !SpecialValue.isNull(value) && !ValidationUtil.isValidEmail((String)value))
				{
					errors.add(new ValidationError(field, "Invalid email format in field " + field.getApiName(), ValidationErrorType.INVALID_EMAIL));
				}
			}
			else if (field.getDataType().getId().equals(DataType.ENUMERATION))
			{
				EnumerationDataType enumDT = (EnumerationDataType)field.getDataType();
				
				if (enumDT.isValidateValues())
				{
					if (!enumDT.getValueList().isEmpty())
					{
						Object value = record.getField(field.getApiName(), false);
						if (value != null && !SpecialValue.isNull(value))
						{
							String[] enumValues = ((EnumerationDataType)field.getDataType()).getValues().split("\\r?\\n");
							boolean isValidEnumValue = false;
							for (String enumVal : enumValues)
							{
								if (enumVal.equals(value))
								{
									isValidEnumValue = true;
									break;
								}
							}
							
							if (!isValidEnumValue)
							{
								errors.add(new ValidationError(field, "Invalid enumeration value in field " + field.getApiName(), ValidationErrorType.INVALID_ENUM_VALUE));
							}
						}
					}
					else if (enumDT.getDictionary() != null)
					{
						Dictionary dict = env.getDictionaries().get(enumDT.getDictionary().getId());
						
						Object value = record.getField(field.getApiName(), false);
						if (value != null && !SpecialValue.isNull(value) && !dict.hasValue(value))
						{
							errors.add(new ValidationError(field, "Enumeration value '" + value + "' not found in dictionary in field " + field.getApiName(), ValidationErrorType.INVALID_ENUM_VALUE));
						}
					}
				}
			}
		}
		
		// add errors from record that may have been set in a trigger
		if (record.hasErrors())
		{
			errors.addAll(record.getErrors());
		}
		
		if (!errors.isEmpty())
		{
			FieldValidationException exception = new FieldValidationException("Field validation errors");
			for (ValidationError err : errors)
			{
				if (err.getField() != null)
				{
					exception.addMessage("Field " + err.getField().getLabel() + ": " + err.getMessage(), err.getField().getKID(), err.getField().getLabel(), err.getErrorType());
				}
				else
				{
					exception.addMessage(err.getMessage(), null, null, err.getErrorType());
				}
			}
			throw exception;
		}
	}

	/**
	 * This method runs all kinds of validation/constraint on record field.
	 * @param record
	 * @throws KommetException 
	 */
	public static List<ValidationError> checkRequiredFields (Record record, boolean checkAutoFilledFields, boolean isUpdate) throws KommetException
	{
		List<ValidationError> errors = new ArrayList<ValidationError>();
		
		for (Field field : record.getType().getFields())
		{
			if (field.isAutoSet() && !checkAutoFilledFields)
			{
				// do not validate auto set fields because at this stage they may still be empty
				continue;
			}
			
			if (field.getDataTypeId().equals(DataType.AUTO_NUMBER))
			{
				// autonumber fields are always required, but their value is set using the default value postgres clause
				// not from Java code, so they are always null at this point
				continue;
			}
			
			// is it's an update of an existing record, created by and created date fields can be empty
			// because they should only be set during new record creation
			if ((field.getApiName().equals(Field.CREATEDBY_FIELD_NAME) || field.getApiName().equals(Field.CREATEDDATE_FIELD_NAME)) && isUpdate)
			{
				continue;
			}
			
			if (!record.isSet(field.getApiName()) && (isUpdate || !field.isRequired()))
			{
				continue;
			}
			
			// TODO what about non-null, empty strings?
			if (field.getDataType().getId().equals(DataType.TYPE_REFERENCE))
			{
				Object value = record.attemptGetField(field.getApiName());
				
				// when object A is saved that has a field reference to object B, that object B
				// must already be saved - two related objects cannot be saved in one transaction
				if (value == null || SpecialValue.isNull(value))
				{
					if (field.isRequired())
					{
						errors.add(new ValidationError(field, "Required field " + record.getType().getApiName() + "." + field.getApiName() + " is empty", ValidationErrorType.FIELD_REQUIRED));
					}
				}
				else if (((Record)value).attemptGetKID() == null)
				{
					throw new KommetException("Field reference to " + field.getApiName() + " on type " + record.getType().getApiName() + " is unsaved. All reference fields must be saved before the parent object is saved");
				}
			}
			// Validate all required fields except ID, which will be set later.
			// Null values are allowed in an update, because in updates not all fields need to be set - only those which are
			// being modified. However, if it's an insert, all fields need to be set, so nulls in required fields are not allowed.
			else if (!field.getApiName().equals(Field.ID_FIELD_NAME) && field.isRequired() && ((record.attemptGetField(field.getApiName()) == null && !isUpdate) || SpecialValue.isNull(record.attemptGetField(field.getApiName()))))
			{
				errors.add(new ValidationError(field, "Required field " + record.getType().getApiName() + "." + field.getApiName() + " is empty", ValidationErrorType.FIELD_REQUIRED));
			}
		}
		
		return errors;
	}
	
	
	@Transactional(readOnly = true)
	public Field getField (KID id, EnvData env) throws KommetException
	{
		Field field = fieldDao.getByKID(id, appConfig, env);
		
		if (field == null)
		{
			return null;
		}
		
		if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
		{
			// initialize the inverse type property
			((InverseCollectionDataType)field.getDataType()).setInverseType(env.getType(((InverseCollectionDataType)field.getDataType()).getInverseTypeId()));
		}
		else if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
		{
			// initialize the inverse type property
			Type refType = env.getType(((TypeReference)field.getDataType()).getTypeId());
			((TypeReference)field.getDataType()).setType(refType);
		}
		else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			AssociationDataType dt = ((AssociationDataType)field.getDataType());
			
			// initialize associated field and link field properties
			dt.setAssociatedType(env.getType(dt.getAssociatedTypeId()));
			dt.setLinkingType(env.getType(dt.getLinkingTypeId()));
		}
		
		return field;
	}

	/**
	 * Get type by ID
	 * @param typeId
	 * @param env 
	 * @return
	 * @throws KIDException 
	 */
	@Transactional(readOnly = true)
	public Type getType (KID typeId, EnvData env) throws KommetException
	{
		return typeDao.getByKID(typeId, appConfig, env);
	}
	
	@Transactional
	public Type createType (Type type, EnvData env) throws KommetException
	{
		return createType(type, getRootAuthData(env), env);
	}
	
	@Transactional
	public Type createType (Type type, AuthData authData, EnvData env) throws KommetException
	{
		if (authData == null)
		{
			throw new KommetException("Auth data is null while creating type.");
		}
		return createType (type, true, true, true, true, true, true, authData, env);
	}
	
	@Transactional
	public Type createCoreType (Type type, EnvData env) throws KommetException
	{
		if (!type.getApiName().equals(SystemTypes.PROFILE_API_NAME) && !type.getApiName().equals(SystemTypes.USER_API_NAME))
		{
			throw new KommetException("Trying to create type " + type.getApiName() + " with method createCoreObject. Only user and profile objects can be created using this method. Use create() instead");
		}
		return createType (type, !(type instanceof ProfileKType || type instanceof UserKType), false, false, false, false, false, null, env);
	}
	
	@Transactional
	public Type createUniqueCheckType (UniqueCheckKType type, AuthData authData, EnvData env) throws KommetException
	{
		if (authData == null)
		{
			throw new KommetException("Auth data is null while creating type.");
		}
		
		type = (UniqueCheckKType)createType (type, true, false, false, false, false, false, authData, env);
		addUniqueChecks(type, type.isBasic() ? RecordAccessType.SYSTEM_IMMUTABLE : RecordAccessType.SYSTEM, authData, env);
		
		// unique checks have been added outside of the createType method, so the type containing the unique checks has to be re-registered with the env
		env.addTypeMapping(type);
		env.registerType(type);
		
		return type;
	}
	
	@Transactional
	@SuppressWarnings("unchecked")
	public void deleteTypeProxy(Collection<Type> types, AuthData authData, EnvData env) throws KommetException
	{	
		for (Type type : types)
		{
			try
			{
				env.deleteCustomTypeProxyMapping((java.lang.Class<? extends RecordProxy>)compiler.getClass(type.getQualifiedName(), true, env), type.isDeclaredInCode());
			}
			catch (ClassNotFoundException e)
			{
				throw new KommetException("Error generating type proxy: class " + type.getQualifiedName() + " not found");
			}
		}
		
		List<Class> classes = new ArrayList<Class>();
		
		for (Type type : types)
		{
			// do not delete classes that were code declarations of files
			// because users will still use them
			if (!type.isDeclaredInCode())
			{
				classes.add(RecordProxyClassGenerator.getProxyKollClass(type, true, true, classService, authData, env));
			}
		}
		
		compiler.deleteCompiledFiles(classes, env);
		
		// reset class loader so that the proxies are removed
		compiler.resetClassLoader(env);
	}
	
	@Transactional
	public void updateTypeProxy(Collection<Type> types, AuthData authData, EnvData env) throws KommetException
	{
		updateTypeProxy(types, true, authData, env);
	}
	
	@Transactional
	@SuppressWarnings("unchecked")
	public void updateTypeProxy(Collection<Type> types, boolean generateNestedProxies, AuthData authData, EnvData env) throws KommetException
	{
		if (types == null || types.isEmpty())
		{
			throw new KommetException("Method updateTypeProxy invoked on an empty type list");
		}
		
		List<Class> classes = new ArrayList<Class>();
		
		for (Type type : types)
		{
			if (!type.isDeclaredInCode())
			{
				// generate type proxy
				classes.add(RecordProxyClassGenerator.getProxyKollClass(type, generateNestedProxies, true, classService, authData, env));
			}
			else
			{
				// for type declared in code, the class defined by the user serves as proxy
				// since it is enhanced by the Kommet engine with @Entity annotation, setInitialized() calls on setters and other stuff needed for proxies
				Class typeClass = classService.getClass(type.getQualifiedName(), env);
				
				if (typeClass == null)
				{
					throw new KommetException("Type " + type.getQualifiedName() + " is declared in code, but not corresponding class was found");
				}
				
				classes.add(typeClass);
			}
		}
		
		CompilationResult compilationResult = compiler.compile(classes, env);
		
		if (!compilationResult.isSuccess())
		{
			for (Class file : classes)
			{
				log.debug(file.getJavaCode());
			}
			throw new KommetException("Error compiling type proxy: " + compilationResult.getDescription());
		}
		
		// reset class loader so that new proxy class can be loaded
		compiler.resetClassLoader(env);
		
		for (Type type : types)
		{
			try
			{
				env.addCustomTypeProxyMapping((java.lang.Class<? extends RecordProxy>)compiler.getClass(type.getQualifiedName(), true, env));
			}
			catch (ClassNotFoundException e)
			{
				throw new KommetException("Error generating type proxy: class " + type.getQualifiedName() + " not found");
			}
		}
	}
	
	@Transactional
	public void updateTypeProxy(Type type, AuthData authData, EnvData env) throws KommetException
	{
		updateTypeProxy(MiscUtils.toList(type), authData, env);
	}
	
	@Transactional
	public void deleteTypeProxy(Type type, AuthData authData, EnvData env) throws KommetException
	{
		deleteTypeProxy(Arrays.asList(type), authData, env);
	}
	
	@Transactional
	public Type updateCoreType(Type type, EnvData env) throws KommetException
	{
		return updateType(type, null, env);
	}
	
	/**
	 * Updates a definition of a type.
	 * @param type
	 * @param userId
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public Type updateType(Type type, AuthData authData, EnvData env) throws KommetException
	{
		// check duplicate in a separate query to avoid ugly error thrown by the type update query
		// the type update query is a pure SQL query, hence the error
		checkDuplicateType(type, env);
		
		validateType(type);
		
		if (!type.isFieldsInitialized())
		{
			// initialize fields on type
			Type envType = env.getType(type.getKeyPrefix());
			
			if (envType == null)
			{
				throw new KommetException("Type " + type.getQualifiedName() + " cannot be updated, because it was not found on the env by its prefix " + type.getKeyPrefix());
			}
			
			for (Field field : envType.getFields())
			{
				type.addField(field);
			}
		}
		
		if (type.getKID() == null)
		{
			throw new KommetException("Trying to call updateType on an unsaved type. If you are trying to create a new type, use createType.");
		}
		
		Type existingType = env.getType(type.getKID());
		if (existingType == null)
		{
			throw new KommetException("Trying to update type which is not registered with the environment. This indicates a serious inconsistency in the environments state as all saved types should be registered.");
		}
		else if (existingType == type)
		{
			// we need to make sure the type object is a different instance, because we need to compare it with
			// the env version to validate changes
			throw new KommetException("When type is updated, a copy of the type object must be passed to the method, not the original type instance from env");
		}
		
		Type updatedType = typeDao.update(type, existingType, authData, env);;
		
		// make sure the default field is required
		if (type.getDefaultFieldId() != null)
		{
			if (!isValidDefaultField(type.getDefaultField(), type))
			{
				if (!type.getDefaultField().getDataTypeId().equals(DataType.FORMULA))
				{
					throw new KommetException("Field " + type.getDefaultField().getApiName() + " cannot be set as default because it is not a required field");
				}
				else
				{
					throw new KommetException("Formula field " + type.getDefaultField().getApiName() + " cannot be set as default because not all fields used in it are required");
				}
			}
		}
		
		if (type.getSharingControlledByFieldId() != null)
		{
			validateSharingControlledByField(type.getSharingControlledByFieldId(), type);
		}
		
		// if type name has changed, update it on the env
		if (!type.getQualifiedName().equals(existingType.getQualifiedName()))
		{
			env.renameType(existingType.getQualifiedName(), type);
			
			// some views (e.g. list view) reference the type name, so when the name changes, views have to be generated anew
			createStandardActionsForType(type, null, env);
		}
		else
		{
			env.updateType(type);
		}
		
		log.debug("Updated type " + type.getQualifiedName());
		
		return updatedType;
	}

	/**
	 * Creates a new type on the environment.
	 * @param type
	 * @param env
	 * @throws KIDException 
	 * @throws  
	 * @throws KommetException 
	 */
	@Transactional
	public Type createType (Type newType, boolean createUserReferenceSystemFields, boolean createUniqueChecks, boolean createPermissionsTrigger, boolean createProfilePermissions, boolean checkLimits, boolean uchTypeExists, AuthData authData, EnvData env) throws KommetException
	{ 	
		// check duplicate in a separate query to avoid ugly error thrown by the type update query
		// the type update query is a pure SQL query, hence the error
		checkDuplicateType(newType, env);
		
		validateType(newType);
		
		// make sure user can create types
		if (checkLimits && !AuthUtil.isRoot(authData))
		{
			String sMaxTypes = getUserSettingValue(UserSettingKeys.KM_ROOT_MAX_TYPES, authData, getRootAuthData(env), env);
			if (sMaxTypes != null)
			{
				Integer maxTypes = Integer.parseInt(sMaxTypes);
				int customTypeCount = 0;
				
				for (Type type : getTypes(false, false, env))
				{
					if (!type.isBasic())
					{
						customTypeCount++;
					}
				}
				
				if ((customTypeCount + 1) > maxTypes)
				{
					throw new LimitExceededException(authData.getI18n().get("limits.maxtypes.exceeded"));
				}
			}
		}
		
		Type type = MiscUtils.cloneType(newType);
		
		// make sure the package name of the object is env-specific
		if (env.isEnvSpecificPackage(type.getPackage()))
		{
			throw new KommetException("Type package name should not be env-specific, i.e. it should not start with the environment prefix " + env.getEnv().getBasePackage() + ". Actual package name is " + type.getPackage());
		}
		
		Set<Field> systemFields = addSystemFields(createUserReferenceSystemFields, type, env);
		
		Type savedType = typeDao.insert(type, env);
		
		typeDao.createDbTableForType(savedType, env);
		if (createPermissionsTrigger)
		{
			typeDao.createCheckEditDeletePermissionsTriggers(savedType, env);
		}
		
		// insert fields
		for (Field field : type.getFields())
		{
			field.setType(savedType);
			
			// if this type contains fields with are type references to itself, we will their type/typeId properties
			if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				TypeReference dt = ((TypeReference)field.getDataType());
				if (dt.getType().getQualifiedName().equals(type.getQualifiedName()))
				{
					dt.setType(savedType);
				}
			}
			else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
			{
				InverseCollectionDataType dt = ((InverseCollectionDataType)field.getDataType()); 
				if (dt.getInverseType().getQualifiedName().equals(type.getQualifiedName()))
				{
					dt.setInverseType(savedType);
				}
			}
			else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				AssociationDataType dt = ((AssociationDataType)field.getDataType()); 
				if (dt.getAssociatedType().getQualifiedName().equals(type.getQualifiedName()))
				{
					dt.setAssociatedType(savedType);
				}
			}
			
			// note: the parameter systemFields.contains(field) checks, if the systemFields set
			// contains the given _instance_ (compares Java references, not IDs)
			field = createField (field, authData, systemFields.contains(field), true, uchTypeExists, env);
			
			// add field only after it is saved, because it has to be indexed by its ID
			savedType.addField(field);
		}
		
		if (savedType.getSharingControlledByFieldId() != null)
		{
			validateSharingControlledByField(savedType.getSharingControlledByFieldId(), savedType);
		}
		
		// for new types, the default field will always be the ID field
		type.setDefaultFieldId(type.getField(Field.ID_FIELD_NAME).getKID());
		// update type in DB with the default field
		typeDao.setDefaultField(type, env);
		
		if (createUniqueChecks)
		{
			// unique checks can be added only after the type and all fields have been saved because their IDs are used in creating unique checks
			addUniqueChecks(savedType, type.isBasic() ? RecordAccessType.SYSTEM_IMMUTABLE : RecordAccessType.SYSTEM, authData, env);
		}
		
		// insert the object mapping into the env's data
		env.addTypeMapping(type);
		
		// register the object with the global store
		env.registerType(type);
		
		// Update the proxy class for all types except basic types.
		// Proxy is not created for basic types because for them it is already created at compile time.
		updateTypeProxy(type, authData, env);
		
		if (!type.isBasic())
		{
			// Create standard controller and pages for type, unless it's a basic type, in which
			// case those entities won't be created because 1) we would need to use a standard
			// version of the method that doesn't use KommetAdmin (for core types) 2) users and profiles (which are the
			// only core types) have custom CRUD pages anyway 3) controllers for them are 
			// generate standard controller for this type
			Class controller = createStandardTypeController(savedType, authData, env);

			// generate standard actions for basic operations and for all profiles
			createStandardActionsForType(type, controller, env);
		}
		
		// Set type permissions for system administrator.
		// We skip types that have isBasic flag, but this does not mean that the system admin profile will not have this permission for these types.
		// They will be added later in the {@link BasicSetupService}, because at the time the basic types are created, the TypePermission type does not exist yet.
		if (createProfilePermissions && !type.isBasic())
		{
			giveTypeAccessToSysAdminProfile(type, env);
		}
		
		log.debug("Created type " + type.getQualifiedName());
		
		return env.getType(savedType.getKID());
	}
	
	/**
	 * Validate different properties of the inserted/updated type.
	 * @param type
	 * @throws KommetException
	 */
	private void validateType(Type type) throws KommetException
	{
		// validate type name
		if (type.getApiName() != null)
		{
			if (ValidationUtil.getReservedTypeNames().contains(type.getApiName().toLowerCase()))
			{
				throw new KommetException("Type API name " + type.getApiName() + " is reserved and cannot be used");
			}
		}
	}

	private void checkDuplicateType(Type type, EnvData env) throws KommetException
	{
		if (!StringUtils.hasText(type.getQualifiedName()))
		{
			throw new KommetException("Type name not set while saving type");
		}
		
		Type existingType = this.getTypeByName(type.getQualifiedName(), false, env); 
		
		// if a type with this name already exists, and it is not the same type as the one that is saved, throw an error
		if (existingType != null && (type.getKID() == null || !existingType.getKID().equals(type.getKID())))
		{
			throw new KommetException("Type with name " + type.getQualifiedName() + " already exists");
		}
	}

	private void giveTypeAccessToSysAdminProfile(Type type, EnvData env) throws KommetException
	{
		Record permission = new Record(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.TYPE_PERMISSION_API_NAME)));
		permission.setField("typeId", type.getKID(), env);
		permission.setField("read", true);
		permission.setField("edit", true);
		permission.setField("delete", true);
		permission.setField("create", true);
		permission.setField("readAll", true);
		permission.setField("editAll", true);
		permission.setField("deleteAll", true);
		permission.setAccessType(type.isBasic() ? RecordAccessType.SYSTEM_IMMUTABLE.getId() : RecordAccessType.SYSTEM.getId());
		
		// save this permission for the sa profile
		List<Record> saProfiles = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.PROFILE_API_NAME + " where name = '" + Profile.SYSTEM_ADMINISTRATOR_NAME + "'").list();
		if (saProfiles.isEmpty())
		{
			throw new KommetException("System administrator profile not found on env");
		}
				
		permission.setField("profile.id", saProfiles.get(0).getKID(), env);
		permission = save(permission, true, true, true, false, getRootAuthData(env), env);
	}

	/**
	 * Creates standard pages for the given type. All standard pages will use the passed standard controller.
	 * @param type
	 * @param controller
	 * @param isCoreType 
	 * @param env
	 * @throws KommetException 
	 */
	public void createStandardActionsForType (Type type, Class controller, EnvData env) throws KommetException
	{
		TypeInfo existingTypeInfo = null;
		
		if (type.getKID() != null)
		{
			TypeInfoFilter filter = new TypeInfoFilter();
			filter.addTypeId(type.getKID());
			List<TypeInfo> infos = typeInfoDao.find(filter, env);
			
			if (!infos.isEmpty())
			{
				existingTypeInfo = infos.get(0);
				
				if (controller == null)
				{
					controller = classService.getClass(infos.get(0).getStandardController().getId(), env);
				}
			}
		}
		else
		{
			throw new KommetException("Type must be saved before actions can be created for it");
		}
		
		if (controller == null)
		{
			throw new KommetException("Standard controller not passed to method generating actions");
		}
		
		List<Profile> profiles = new ArrayList<Profile>();
		// operate on a list for future possible changes, but for now only add standard pages for the admin profile
		profiles.add(profileService.getProfile((KID)env.getRootUser().getField("profile.id"), env));
		
		View detailsView = createDetailsView(type, existingTypeInfo != null ? existingTypeInfo.getDefaultDetailsAction().getView().getId() : null, env);
		View listView = createListView(type, existingTypeInfo != null ? existingTypeInfo.getDefaultListAction().getView().getId() : null, env);
		View editView = createEditView(type, existingTypeInfo != null ? existingTypeInfo.getDefaultEditAction().getView().getId() : null, env);
		View createView = createCreateView(type, existingTypeInfo != null ? existingTypeInfo.getDefaultCreateAction().getView().getId() : null, env);
		
		// store views on disk
		ViewUtil.storeView(detailsView, appConfig, env);
		ViewUtil.storeView(listView, appConfig, env);
		ViewUtil.storeView(editView, appConfig, env);
		ViewUtil.storeView(createView, appConfig, env);
		
		Action detailsAction = createDetailsAction(type, detailsView, existingTypeInfo != null ? existingTypeInfo.getDefaultDetailsAction().getId() : null, controller, env);
		Action listAction = createListAction(type, listView, existingTypeInfo != null ? existingTypeInfo.getDefaultListAction().getId() : null, controller, env);
		Action editAction = createEditAction(type, editView, existingTypeInfo != null ? existingTypeInfo.getDefaultEditAction().getId() : null, controller, env);
		Action createAction = createCreateAction(type, createView, existingTypeInfo != null ? existingTypeInfo.getDefaultCreateAction().getId() : null, controller, env);
		
		AuthData rootAuthData = getRootAuthData(env);
		
		RecordAccessType accessType = type.isBasic() ? RecordAccessType.SYSTEM_IMMUTABLE : RecordAccessType.SYSTEM;
		
		// TODO optimize this, because it performs 3 queries for each profiles, which is way too much
		// create standard pages for each profile
		for (Profile profile : profiles)
		{
			actionService.setStandardAction(type.getKID(), listAction.getId(), profile.getId(), StandardActionType.LIST, accessType, rootAuthData, env);
			actionService.setStandardAction(type.getKID(), detailsAction.getId(), profile.getId(), StandardActionType.VIEW, accessType, rootAuthData, env);
			actionService.setStandardAction(type.getKID(), editAction.getId(), profile.getId(), StandardActionType.EDIT, accessType, rootAuthData, env);
			actionService.setStandardAction(type.getKID(), createAction.getId(), profile.getId(), StandardActionType.CREATE, accessType, rootAuthData, env);
		}
		
		// additionally, create a save action
		Action saveAction = new Action();
		
		if (existingTypeInfo != null)
		{
			saveAction.setId(existingTypeInfo.getDefaultSaveAction().getId());
		}
		
		saveAction.setController(controller);
		saveAction.setControllerMethod("save");
		saveAction.setIsSystem(true);
		saveAction.setIsPublic(false);
		
		// when save action fails, users are redirected back to the edit page
		// otherwise details page is displayed
		saveAction.setView(editView);
		
		// TODO store the save action URL is some static var
		// TODO create a list of reserved URLs
		saveAction.setUrl("save/" + type.getKeyPrefix());
		saveAction.setTypeId(type.getKID());
		saveAction.setName("kommet.standardactions.StandardSaveActionForType" + type.getKeyPrefix());
		saveAction.setAccessType(accessType.getId());
		actionService.saveSystemActionOnEnv(saveAction, getRootAuthData(env), env);
		
		// create type info record
		TypeInfo typeInfo = existingTypeInfo != null ? existingTypeInfo : new TypeInfo();
		
		typeInfo.setTypeId(type.getKID());
		typeInfo.setDefaultCreateAction(createAction);
		typeInfo.setDefaultDetailsAction(detailsAction);
		typeInfo.setDefaultEditAction(editAction);
		typeInfo.setDefaultListAction(listAction);
		typeInfo.setDefaultSaveAction(saveAction);
		typeInfo.setStandardController(controller);
		typeInfo.setAccessType(accessType.getId());
		typeInfoDao.save(typeInfo, getRootAuthData(env), env);
	}
	
	private Action createDetailsAction (Type type, View detailsView, KID id, Class controller, EnvData env) throws KommetException
	{
		Action action = new Action();
		if (id != null)
		{
			action.setId(id);
		}
		action.setController(controller);
		action.setControllerMethod("details");
		action.setIsSystem(true);
		action.setView(detailsView);
		action.setTypeId(type.getKID());
		action.setIsPublic(false);
		action.setAccessType(type.isBasic() ? RecordAccessType.SYSTEM_IMMUTABLE.getId() : RecordAccessType.SYSTEM.getId());
		
		// URLs for standard pages are not really used, they are just an alternative to the standard URLs
		action.setUrl("view/" + type.getKeyPrefix());
		
		action.setName("kommet.standardactions.StandardDetailsActionForType" + type.getKeyPrefix());
		
		return actionService.saveSystemActionOnEnv(action, getRootAuthData(env), env);
	}
	
	private Action createListAction (Type type, View listView, KID id, Class controller, EnvData env) throws KommetException
	{
		Action action = new Action();
		if (id != null)
		{
			action.setId(id);
		}
		action.setController(controller);
		action.setControllerMethod("list");
		action.setIsSystem(true);
		action.setView(listView);
		action.setTypeId(type.getKID());
		action.setIsPublic(false);
		action.setAccessType(type.isBasic() ? RecordAccessType.SYSTEM_IMMUTABLE.getId() : RecordAccessType.SYSTEM.getId());
		
		// URLs for standard pages are not really used, they are just an alternative to the standard URLs
		action.setUrl("list/" + type.getKeyPrefix());
		action.setName("kommet.standardactions.StandardListActionForType" + type.getKeyPrefix());
		
		return actionService.saveSystemActionOnEnv(action, getRootAuthData(env), env);
	}
	
	private Action createEditAction (Type type, View editView, KID id, Class controller, EnvData env) throws KommetException
	{
		Action action = new Action();
		if (id != null)
		{
			action.setId(id);
		}
		action.setController(controller);
		action.setControllerMethod("edit");
		action.setIsSystem(true);
		action.setView(editView);
		action.setTypeId(type.getKID());
		action.setIsPublic(false);
		action.setAccessType(type.isBasic() ? RecordAccessType.SYSTEM_IMMUTABLE.getId() : RecordAccessType.SYSTEM.getId());
		
		// URLs for standard pages are not really used, they are just an alternative to the standard URLs
		action.setUrl("edit/" + type.getKeyPrefix());
		
		action.setName("kommet.standardactions.StandardEditActionForType" + type.getKeyPrefix());
		
		return actionService.saveSystemActionOnEnv(action, getRootAuthData(env), env);
	}
	
	private Action createCreateAction (Type type, View editView, KID id, Class controller, EnvData env) throws KommetException
	{
		Action action = new Action();
		if (id != null)
		{
			action.setId(id);
		}
		action.setController(controller);
		action.setControllerMethod("create");
		action.setIsSystem(true);
		action.setView(editView);
		action.setTypeId(type.getKID());
		action.setIsPublic(false);
		action.setAccessType(type.isBasic() ? RecordAccessType.SYSTEM_IMMUTABLE.getId() : RecordAccessType.SYSTEM.getId());
		
		// URLs for standard pages are not really used, they are just an alternative to the standard URLs
		action.setUrl("new/" + type.getKeyPrefix());
		
		action.setName("kommet.standardactions.StandardCreateActionForType" + type.getKeyPrefix());
		
		return actionService.saveSystemActionOnEnv(action, getRootAuthData(env), env);
	}

	private View createEditView (Type type, KID id, EnvData env) throws KommetException
	{
		View view = new View();
		if (id != null)
		{
			view.setId(id);
		}
		view.setIsSystem(true);
		view.setPackageName(type.getPackage());
		String viewName = "StandardEdit" + type.getKeyPrefix();
		view.initKeetleCode(ViewUtil.getStandardEditViewCode(type, viewName, type.getPackage()), appConfig, env);
		view.setTypeId(type.getKID());
		view.setPath(viewName);
		view.setName(viewName);
		
		// access type has to be SYSTEM, even for system types, because the view is recompiled when the type changes
		view.setAccessType(RecordAccessType.SYSTEM.getId());
		
		view.setAccessLevel("Closed");
		return viewDao.saveSystemView(view, getRootAuthData(env), env);
	}
	
	private View createCreateView (Type type, KID id, EnvData env) throws KommetException
	{
		View view = new View();
		if (id != null)
		{
			view.setId(id);
		}
		view.setIsSystem(true);
		view.setPackageName(type.getPackage());
		String viewName = "StandardCreate" + type.getKeyPrefix();
		view.initKeetleCode(ViewUtil.getStandardEditViewCode(type, viewName, type.getPackage()), appConfig, env);
		view.setTypeId(type.getKID());
		
		view.setPath(viewName);
		view.setName(viewName);
		
		// access type has to be SYSTEM, even for system types, because the view is recompiled when the type changes
		view.setAccessType(RecordAccessType.SYSTEM.getId());
		
		view.setAccessLevel("Closed");
		return viewDao.saveSystemView(view, getRootAuthData(env), env);
	}
	
	private View createListView (Type type, KID id, EnvData env) throws KommetException
	{
		View view = new View();
		
		if (id != null)
		{
			view.setId(id);
		}
		
		view.setIsSystem(true);
		view.setPackageName(type.getPackage());
		String viewName = "StandardList" + type.getKeyPrefix();
		view.initKeetleCode(ViewUtil.getStandardListViewCode(type, viewName, type.getPackage(), env), appConfig, env);
		view.setTypeId(type.getKID());
		
		view.setPath(viewName);
		view.setName(viewName);
		
		// access type has to be SYSTEM, even for system types, because the view is recompiled when the type changes
		view.setAccessType(RecordAccessType.SYSTEM.getId());
		
		view.setAccessLevel("Closed");
		return viewDao.saveSystemView(view, getRootAuthData(env), env);
	}

	private View createDetailsView (Type type, KID id, EnvData env) throws KommetException
	{
		View view = new View();
		
		if (id != null)
		{
			view.setId(id);
		}
		
		view.setIsSystem(true);
		view.setPackageName(type.getPackage());
		String viewName = "StandardView" + type.getKeyPrefix();
		view.initKeetleCode(ViewUtil.getStandardDetailsViewCode(type, viewName, type.getPackage()), appConfig, env);
		view.setTypeId(type.getKID());
		
		view.setPath(viewName);
		view.setName(viewName);
		
		// access type has to be SYSTEM, even for system types, because the view is recompiled when the type changes
		view.setAccessType(RecordAccessType.SYSTEM.getId());
		
		view.setAccessLevel("Closed");
		return viewDao.saveSystemView(view, getRootAuthData(env), env);
	}
	
	/**
	 * 
	 * @param generateNewCode Even if the controller already exists, generate its code anew.
	 * @param type
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private Class recompileStandardTypeController(boolean generateNewCode, Type type, AuthData authData, EnvData env) throws KommetException
	{
		// find KOLL file for the controller in the database
		Class cls = classService.getClass(StandardTypeControllerUtil.getStandardControllerQualifiedName(type), env);
		
		if (cls == null)
		{
			log.debug("Creating standard controller for type " + type.getQualifiedName());
			return createStandardTypeController(type, authData, env);
		}
		else if (generateNewCode)
		{
			// generate controller code anew
			Class newController = StandardTypeControllerUtil.getController(type, classService, authData, env);
			
			cls.setKollCode(newController.getKollCode());
			cls.setJavaCode(newController.getJavaCode());
			
			// save controller to system
			classService.saveSystemFile(cls, getRootAuthData(env), env);
		}
		
		CompilationResult compilationResult = compiler.compile(cls, env);
		
		if (!compilationResult.isSuccess())
		{
			log.debug(cls.getJavaCode());
			throw new KommetException("Error compiling object proxy: " + compilationResult.getDescription());
		}
		
		// reset class loader so that new proxy class can be loaded
		compiler.resetClassLoader(env);
		
		return cls;
	}

	public Class createStandardTypeController(Type type, AuthData authData, EnvData env) throws KommetException
	{
		// generate object proxy
		Class cls = StandardTypeControllerUtil.getController(type, classService, authData, env);
		CompilationResult compilationResult = compiler.compile(cls, env);
		
		if (!compilationResult.isSuccess())
		{
			log.debug(cls.getJavaCode());
			throw new KommetException("Error compiling object proxy: " + compilationResult.getDescription());
		}
		
		// reset class loader so that new proxy class can be loaded
		compiler.resetClassLoader(env);
		
		// save controller KOLL file
		return classService.saveSystemFile(cls, getRootAuthData(env), env);
	}

	private void addUniqueChecks (Type type, RecordAccessType recordAccessType, AuthData authData, EnvData env) throws KommetException
	{
		// add unique check for the ID field
		UniqueCheck idUniqueCheck = new UniqueCheck();
		
		Field idField = type.getField(Field.ID_FIELD_NAME);
		if (idField == null)
		{
			throw new KommetException("No such field 'id' on type " + type.getApiName());
		}
		else if (idField.getKID() == null)
		{
			throw new KommetException("Cannot add unique check to an unsaved field 'id' on type " + type.getApiName() + ".");
		}
		
		idUniqueCheck.setFieldIds(idField.getKID().getId());
		idUniqueCheck.setTypeId(type.getKID());
		idUniqueCheck.setName("UniqueIdType" + type.getKID());
		idUniqueCheck.setIsSystem(true);
		idUniqueCheck.setDbName(UniqueCheck.generateDbName(type.getKID(), env));
		idUniqueCheck.setAccessType(recordAccessType.getId());
		
		uniqueCheckDao.save(idUniqueCheck, env, authData);
		
		type.addUniqueCheck(idUniqueCheck);
	}

	private static Set<Field> addSystemFields(boolean createUserReferenceSystemFields, Type type, EnvData env) throws KommetException
	{
		Set<Field> systemFields = new HashSet<Field>();
		
		// get user type from env
		// note however that we cannot cast it to UserKType, because when the env is read in anew, system types (such as ProfileKType, EventKType)
		// are all stored as their superclass Type
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		
		if (userType == null && createUserReferenceSystemFields)
		{
			throw new KommetException("User type not found on env");
		}
		
		// check if ID field exists
		if (type.getField(Field.ID_FIELD_NAME) == null)
		{
			systemFields.add(getIdField());
		}
		
		// check if createddate field exists
		if (type.getField(Field.CREATEDDATE_FIELD_NAME) == null)
		{
			systemFields.add(getCreatedDateField());
		}
		
		if (createUserReferenceSystemFields)
		{
			// check if createdby field exists
			if (type.getField(Field.CREATEDBY_FIELD_NAME) == null)
			{
				systemFields.add(getCreatedByField(userType));
			}
		}
		
		// check if lastmodifieddate field exists
		if (type.getField(Field.LAST_MODIFIED_DATE_FIELD_NAME) == null)
		{
			systemFields.add(getLastModifiedDateField());
		}
		
		if (createUserReferenceSystemFields)
		{
			// check if createdby field exists
			if (type.getField(Field.LAST_MODIFIED_BY_FIELD_NAME) == null)
			{
				systemFields.add(getLastModifiedByField(userType));
			}
		}
		
		if (type.getField(Field.ACCESS_TYPE_FIELD_NAME) == null)
		{
			systemFields.add(getAccessTypeField());
		}
		
		for (Field field : systemFields)
		{
			type.addField(field);
		}
		
		return systemFields;
	}
	
	public void addUserReferencesOnProfileAndUser(UserKType userType, ProfileKType profileType, boolean isUchTypeExists, AuthData authData, EnvData env) throws KommetException
	{
		addUserReferencesOnProfile(userType, profileType, isUchTypeExists, authData, env);
		addUserReferencesOnUser(userType, isUchTypeExists, authData, env);
	}
	
	private void addUserReferencesOnProfile(UserKType userType, ProfileKType profileType, boolean isUchTypeExists, AuthData authData, EnvData env) throws KommetException
	{
		Set<Field> systemFields = new HashSet<Field>();
		systemFields.add(getLastModifiedByField(userType));
		systemFields.add(getCreatedByField(userType));
		
		Set<Field> savedSystemFields = new HashSet<Field>();
		
		// make the fields not required temporarily
		for (Field field : systemFields)
		{
			field.setRequired(false);
			profileType.addField(field);
			savedSystemFields.add(createField(field, authData, true, false, isUchTypeExists, env));
		}
		
		// set values on all profiles
		List<Record> profiles = env.getSelectCriteriaFromDAL("select id, name, " + Field.CREATEDBY_FIELD_NAME + ".id, " + Field.LAST_MODIFIED_BY_FIELD_NAME + ".id from Profile").list();
		for (Record profile : profiles)
		{
			if (profile.getField(Field.CREATEDBY_FIELD_NAME) == null)
			{
				profile.setField(Field.CREATEDBY_FIELD_NAME, env.getRootUser());
			}
			if (profile.getField(Field.LAST_MODIFIED_BY_FIELD_NAME) == null)
			{
				profile.setField(Field.LAST_MODIFIED_BY_FIELD_NAME, env.getRootUser());
			}
		
			save(profile, env);
		}
		
		// set fields to required
		for (Field field : savedSystemFields)
		{
			field.setRequired(true);
			updateField(field, false, AuthData.getRootAuthData(env), env);	
		}
	}
	
	private void addUserReferencesOnUser(UserKType userType, boolean isUchTypeExists, AuthData authData, EnvData env) throws KommetException
	{
		Set<Field> systemFields = new HashSet<Field>();
		systemFields.add(getLastModifiedByField(userType));
		systemFields.add(getCreatedByField(userType));
		
		Set<Field> savedSystemFields = new HashSet<Field>();
		
		// make the fields not required temporarily
		for (Field field : systemFields)
		{
			field.setRequired(false);
			userType.addField(field);
			savedSystemFields.add(createField(field, authData, true, false, isUchTypeExists, env));
		}
		
		// set values on all users
		List<Record> users = env.getSelectCriteriaFromDAL("select id, userName, " + Field.CREATEDBY_FIELD_NAME + ".id, " + Field.LAST_MODIFIED_BY_FIELD_NAME + ".id from User").list();
		for (Record user : users)
		{
			if (user.getField(Field.CREATEDBY_FIELD_NAME) == null)
			{
				user.setField(Field.CREATEDBY_FIELD_NAME, env.getRootUser());
			}
			if (user.getField(Field.LAST_MODIFIED_BY_FIELD_NAME) == null)
			{
				user.setField(Field.LAST_MODIFIED_BY_FIELD_NAME, env.getRootUser());
			}
			
			save(user, env);
		}
		
		// set fields to required
		for (Field field : savedSystemFields)
		{
			field.setRequired(true);
			updateField(field, false,AuthData.getRootAuthData(env), env);	
		}
	}

	private static Field getCreatedByField(Type userType) throws KommetException
	{
		Field field = new Field();
		field.setApiName(Field.CREATEDBY_FIELD_NAME);
		field.setLabel(Field.CREATEDBY_FIELD_LABEL);
		field.setDbColumn(Field.CREATEDBY_FIELD_DB_COLUMN);
		
		field.setDataType(new TypeReference(userType));
		field.setRequired(true);
		field.setAutoSet(true);
		return field;
	}
	
	private static Field getLastModifiedByField(Type userType) throws KommetException
	{
		Field field = new Field();
		field.setApiName(Field.LAST_MODIFIED_BY_FIELD_NAME);
		field.setLabel(Field.LAST_MODIFIED_BY_FIELD_LABEL);
		field.setDbColumn(Field.LAST_MODIFIED_BY_FIELD_DB_COLUMN);
		field.setDataType(new TypeReference(userType));
		field.setRequired(true);
		field.setAutoSet(true);
		return field;
	}
	
	private static Field getAccessTypeField() throws KommetException
	{
		Field field = new Field();
		field.setApiName(Field.ACCESS_TYPE_FIELD_NAME);
		field.setLabel(Field.ACCESS_TYPE_FIELD_LABEL);
		field.setDbColumn(Field.ACCESS_TYPE_FIELD_DB_COLUMN);
		NumberDataType dt = new NumberDataType();
		dt.setDecimalPlaces(0);
		field.setDataType(dt);
		field.setRequired(true);
		field.setAutoSet(true);
		return field;
	}

	private static Field getCreatedDateField() throws KommetException
	{
		Field field = new Field();
		field.setApiName(Field.CREATEDDATE_FIELD_NAME);
		field.setLabel(Field.CREATEDDATE_FIELD_LABEL);
		field.setDbColumn(Field.CREATEDDATE_FIELD_DB_COLUMN);
		field.setDataType(new DateTimeDataType());
		field.setRequired(true);
		field.setAutoSet(true);
		return field;
	}
	
	private static Field getLastModifiedDateField() throws KommetException
	{
		Field field = new Field();
		field.setApiName(Field.LAST_MODIFIED_DATE_FIELD_NAME);
		field.setLabel(Field.LAST_MODIFIED_DATE_FIELD_LABEL);
		field.setDbColumn(Field.LAST_MODIFIED_DATE_FIELD_DB_COLUMN);
		field.setDataType(new DateTimeDataType());
		field.setRequired(true);
		field.setAutoSet(true);
		return field;
	}

	private static Field getIdField() throws KommetException
	{
		Field field = new Field();
		field.setApiName(Field.ID_FIELD_NAME);
		field.setLabel(Field.ID_FIELD_LABEL);
		field.setDbColumn(Field.ID_FIELD_DB_COLUMN);
		field.setDataType(new KIDDataType());
		field.setRequired(true);
		field.setCreatedOnTypeCreation(true);
		field.setAutoSet(true);
		
		return field;
	}
	
	@Transactional
	public Field createField (Field field, EnvData env) throws KommetException
	{
		return createField(field, getRootAuthData(env), false, false, true, env);
	}
	
	@Transactional
	public Field createField (Field field, AuthData authData, EnvData env) throws KommetException
	{
		return createField(field, authData, false, false, true, env);
	}
	
	@Transactional
	public Field createField (Field newField, AuthData authData, boolean isSystemField, boolean isCreatedWithType, boolean isUchTypeExists, EnvData env) throws KommetException
	{
		return createField(newField, authData, isSystemField, isCreatedWithType, isUchTypeExists, false, env);
	}

	/**
	 * Creates a new field on a type.
	 * @param field
	 * @param env
	 * @param isCreatedWithType tells if the field is created in the same transaction as its type, or if the type already exists and the field is just being added
	 * @throws KIDException 
	 * @throws  
	 * @throws KommetException 
	 */
	@Transactional
	public Field createField (Field newField, AuthData authData, boolean isSystemField, boolean isCreatedWithType, boolean isUchTypeExists, boolean isAddedAsAlignment, EnvData env) throws KommetException
	{
		if (newField.getKID() != null)
		{
			throw new KommetException("Method createField called on an existing field");
		}
		
		Field field = MiscUtils.cloneField(newField);
		
		if (field.getType() == null)
		{
			throw new KommetPersistenceException("Type not set on field " + field.getApiName() + " that is being inserted");
		}
		
		if (field.getKID() != null)
		{
			throw new KommetException("Trying to call operation createField on an existing field. Use updateField instead.");
		}
		
		if (StringUtils.hasText(field.getDbColumn()))
		{
			if (!KommetDataValidation.isFieldAutoCreated(field.getApiName()) && !field.getType().isBasic())
			{
				throw new KommetException("DB column for new field '" + field.getApiName() + "' should not be set. It is done in the service method.");
			}
		}
		else
		{
			// create DB column name from API name
			field.setDbColumn(field.getApiName());
		}
		
		if (!StringUtils.hasText(field.getApiName()))
		{
			throw new ConstraintViolationException("API name of the new field is null");
		}
		else
		{
			if (!ValidationUtil.isValidFieldApiName(field.getApiName()))
			{
				throw new KommetException("Invalid field API name '" + field.getApiName() + "'. Field names must start with a lowercase letter and may contain only letter of the Latin alphabet, digits and an underscore");
			}
			
			if (!isSystemField && Field.isReservedFieldApiName(field.getApiName()))
			{
				throw new KommetException("API name " + field.getApiName() + " is reserved for system fields");
			}
		}
		
		validateField(field, null, isUchTypeExists, authData, env);
		
		// if the field is an association collection and linking type will be created for it
		// the new type will be stored in this variable
		//Type linkingType = null;
		
		try
		{
			// if it is an association field for which no explicit linking type has been specified,
			// we create an implicit one
			/*if (field.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				AssociationDataType dt = (AssociationDataType)field.getDataType();
				if (dt.getLinkingTypeId() == null)
				{
					linkingType = createLinkingType(field, userId, env);
					dt.setLinkingType(linkingType);
					dt.setLinkingTypeId(linkingType.getKID());
				}
			}*/
			
			// use default field java type
			applyDefaultJavaType(field);
			
			Field insertedField = fieldDao.insert(field, env);
			
			// create a column for the field, unless it's an inverse collection or an association or a formula
			if (!field.isCreatedOnTypeCreation() && Field.hasDatabaseRepresenation(field.getDataType()))
			{
				fieldDao.createDbColumnForField(field, env);
			}
			
			// if the field is created together with the type
			// it does not make sense to align the type because it will be aligned
			// anyway as it is being saved too
			if (!isCreatedWithType && !isAddedAsAlignment)
			{
				fieldPostUpdate(insertedField, null, authData, env);
			}
			
			//field.getType().addField(field);
			field.setId(insertedField.getId());
			field.setKID(insertedField.getKID());
			field.setDbColumn(insertedField.getDbColumn());
			field.setCreated(insertedField.getCreated());
			
			return field;
		}
		catch (KommetException e)
		{
			// if anything goes wrong, delete the linking type that was created for the field
			// TODO this should be rolled back automatically by transaction - check why it's not
			/*if (linkingType != null)
			{
				deleteType(linkingType, authData, env);
			}*/
			
			// unset the DB column
			field.setDbColumn(null);
			throw e;
		}
	}
	
	private void validateField(Field field, Field oldField, boolean isUchTypeExists, AuthData authData, EnvData env) throws KommetException
	{
		// before we even validate the new type, we need to make sure the data type change is allowed 
		if (oldField != null)
		{
			validateDataTypeChange(field, oldField);
		}
		
		if (DataType.TEXT == field.getDataTypeId())
		{
			TextDataType dt = (TextDataType)field.getDataType();
			
			String sMaxTextLength = null;
			
			if (isUchTypeExists)
			{
				getUserSettingValue(UserSettingKeys.KM_ROOT_MAX_TEXTFIELD_LENGTH, authData, AuthData.getRootAuthData(env), env);
			}
			
			Integer maxTextLength = null;
			
			if (sMaxTextLength == null)
			{
				maxTextLength = appConfig.getMaxTextFieldLength();
			}
			else
			{
				maxTextLength = Integer.parseInt(sMaxTextLength);
			}
			
			if (dt.getLength() == null)
			{
				throw new KommetException("Text field length not set");
			}
			else if (dt.getLength() > maxTextLength)
			{
				throw new KommetException("Text field length exceeds maximum allowed length " + maxTextLength);
			}
		}
		else if (DataType.FORMULA == field.getDataTypeId())
		{
			// make sure fields used in the formula are not formula fields themselves
			((FormulaDataType)field.getDataType()).getParsedDefinition();
			
			List<Field> usedFields = FormulaParser.getUsedFields(((FormulaDataType)field.getDataType()).getParsedDefinition(), field.getType());
			for (Field usedField : usedFields)
			{
				if (usedField.getDataTypeId().equals(DataType.FORMULA))
				{
					throw new KommetException("Field " + usedField.getApiName() + " cannot be used in formula field because it is a formula itself");
				}
			}
		}
		else if (DataType.AUTO_NUMBER == field.getDataTypeId())
		{
			Type type = env.getType(field.getType().getKID());
			
			if (type == null)
			{
				throw new KommetException("Type not found on env");
			}
			
			// make sure this is the only autonumber field on the env
			if (type.getAutoNumberFieldId() != null && (!type.getAutoNumberFieldId().equals(field.getKID())))
			{
				throw new KommetException("Cannot create autonumber field on type because this type already contains an autonumber field '" + type.getField(type.getAutoNumberFieldId()).getApiName() + "'");
			}
			
			if (!field.isRequired())
			{
				throw new KommetException("AutoNumber fields must be required");
			}
		}
	}

	private void validateDataTypeChange(Field field, Field oldField) throws FieldDefinitionException
	{
		if (field.getDataTypeId().equals(oldField.getDataTypeId()))
		{
			return;
		}
		
		if (oldField.getDataTypeId().equals(DataType.AUTO_NUMBER))
		{
			throw new FieldDefinitionException("Cannot change data type of field " + field.getApiName() + " from autonumber to something else");
		}
	}

	private void applyDefaultJavaType(Field field) throws FieldDefinitionException, PropertyUtilException
	{
		if (!field.getDataTypeId().equals(DataType.NUMBER))
		{
			return;
		}
		
		NumberDataType numericDataType = (NumberDataType)field.getDataType();
		
		if (numericDataType.getDecimalPlaces() == 0)
		{
			if (numericDataType.getJavaType() == null)
			{
				numericDataType.setJavaType(appConfig.getDefaultIntJavaType());
			}
			else if (!numericDataType.getJavaType().equals(Integer.class.getName()) && !numericDataType.getJavaType().equals(Long.class.getName()))
			{
				throw new FieldDefinitionException("Invalid Java type " + numericDataType.getJavaType() + " for integer numeric field");
			}
		}
		else
		{
			if (numericDataType.getJavaType() == null)
			{
				numericDataType.setJavaType(appConfig.getDefaultFloatJavaType());
			}
			else if (!numericDataType.getJavaType().equals(BigDecimal.class.getName()) && !numericDataType.getJavaType().equals(Double.class.getName()))
			{
				throw new FieldDefinitionException("Invalid Java type " + numericDataType.getJavaType() + " for floating point numeric field");
			}
		}
	}

	@Transactional
	public Field updateField (Field field, AuthData authData, EnvData env) throws KommetException
	{
		return updateField(field, true, authData, env);
	}
	
	/**
	 * This method returns a copy of the field object identified by the parameters.
	 * It is necessary to have a copy of the field while updating it so that it can be compared
	 * to the original on the environment in search of changes.
	 * @param typePrefix
	 * @param fieldId
	 * @return
	 * @throws KommetException
	 */
	public Field getFieldForUpdate (KID fieldId, EnvData env) throws KommetException
	{	
		if (fieldId == null)
		{
			throw new KommetException("Cannot get field by null KID");
		}
		
		Field field = getField(fieldId, env);
		
		if (field == null)
		{
			throw new KommetException("Field " + fieldId + " requested for update not found");
		}
		
		Type type = getType(field.getType().getId(), env);
		if (type == null)
		{
			throw new KommetException("Type with ID " + field.getType().getId() + " not found");
		}
		
		field.setType(type);
		
		return field;
	}

	@Transactional
	public Field updateField (Field field, boolean isUchTypeExists, AuthData authData, EnvData env) throws KommetException
	{
		if (field.getKID() == null)
		{
			throw new KommetException("Calling updateField on an unsaved field. Use createField instead.");
		}
		
		Type type = field.getType();
		
		if (type == null)
		{
			throw new KommetException("Type not set on updated field " + field.getApiName());
		}
		
		// get existing type from the env
		Type envType = env.getType(type.getKID());
		if (envType == null)
		{
			throw new KommetException("Type " + type.getApiName() + " not registered with the environment");
		}
		
		// get the previous definition of the field from the env to be able to discover changes
		// in the DB column name and be able to properly localize and update the old DB column
		Field oldField = envType.getField(field.getKID());
		if (oldField == null)
		{
			throw new KommetException("Trying to update field " + field.getApiName() + " that was not linked with its type");
		}
		
		if (!oldField.getApiName().equals(field.getApiName()))
		{
			if (!StringUtils.hasText(field.getApiName()))
			{
				throw new KommetException("Field name is empty");
			}
			else
			{
				if (!ValidationUtil.isValidFieldApiName(field.getApiName()))
				{
					throw new KommetException("Invalid field API name '" + field.getApiName() + "'. Field names must start with a lowercase letter and may contain only letter of the Latin alphabet, digits and an underscore");
				}
				
				if (Field.isReservedFieldApiName(field.getApiName()))
				{
					throw new KommetException("API name " + field.getApiName() + " is reserved");
				}
			}
		}
		
		if (!StringUtils.hasText(field.getDbColumn()))
		{
			throw new KommetException("DB column on an existing field is not set");
		}
		
		if (field.getCreated() == null)
		{
			throw new KommetException("Created date of an existing field is not set");
		}
		
		validateField(field, oldField, isUchTypeExists, authData, env);
		
		// update the DB column on the field to the (potentially) new one
		field.setDbColumn(field.getApiName());
		
		Field updatedField = fieldDao.update(field, env);
		
		// make sure the updated field passed to the method is a different instance than the old one
		// retrieved from the env (note the use of "==")
		if (field == oldField)
		{
			throw new KommetException("When field is updated, a copy of the field obtained through method EnvData.getFieldForUpdate() must be passed to the method, not the original field instance from env");
		}
		
		// update the DB column representing the field, unless it's an inverse collection
		// in which case a column for it does not exist
		if (!field.getDataType().getId().equals(DataType.INVERSE_COLLECTION))
		{
			fieldDao.updateDbColumnForField(field, oldField, env);
		}
		
		fieldPostUpdate(updatedField, oldField.getApiName().equals(field.getApiName()) ? null : oldField.getApiName(), authData, env);
		return updatedField;
	}

	/**
	 * Performs all the operations that need to be executed after a field is created or updated.
	 * 
	 * It updates the type definition in the environment, updates the proxy etc.
	 * 
	 * @param field
	 * @param env
	 * @throws KommetException
	 */
	private void fieldPostUpdate (Field field, String oldFieldName, AuthData authData, EnvData env) throws KommetException
	{
		if (oldFieldName != null)
		{
			env.renameField(env.getType(field.getType().getKID()), field, oldFieldName);
		}
		else
		{
			env.registerField(field);
		}
		
		Type type = env.getType(field.getType().getKID());
		if (type == null)
		{
			throw new KommetException("Type with ID " + field.getType().getKID() + " does not exist in the environment");
		}
		
		// insert the type mapping into the env data
		env.addTypeMapping(type);
		
		boolean hasAutoNumber = false;
		
		// update auto-number field on type
		for (Field typeField : type.getFields())
		{
			if (typeField.getDataTypeId().equals(DataType.AUTO_NUMBER))
			{
				if (hasAutoNumber)
				{
					throw new KommetException("More than one autonumber field on type");
				}
				
				type.setAutoNumberFieldId(typeField.getKID());
				hasAutoNumber = true;
			}
		}
		
		if (hasAutoNumber)
		{
			updateType(type, authData, env);
		}
		
		// update the proxy class if this is not a basic type
		// since basic types have proxies created at compile time
		updateTypeProxy(type, authData, env);
	}

	/**
	 * A special, separate method is created for saving the super admin, because it is the
	 * only user that has the createdBy field set to itself.
	 * @param rootUser
	 * @param env
	 * @throws KommetException 
	 */
	public Record saveRootUser(Record rootUser, EnvData env) throws KommetException
	{	
		Date modificationDate = new Date();
		rootUser.setCreatedDate(modificationDate);
		rootUser.setLastModifiedDate(modificationDate);
		rootUser.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		// last modified by and created by fields are not set, because they don't exist yet
		
		// run field validation
		runFieldValidation(rootUser, env);
		
		return daoFacade.save(rootUser, false, AuthData.getRootAuthData(env), env);
	}
	
	/**
	 * A special, separate method is created for saving the super admin profile, because it is the
	 * only user that has the createdBy field set to itself.
	 * @param profile
	 * @param env
	 * @throws KommetException 
	 */
	public Record saveSystemProfile(Record profile, boolean setUserReferenceFields, EnvData env) throws KommetException
	{	
		Date modificationDate = new Date();
		profile.setCreatedDate(modificationDate);
		profile.setLastModifiedDate(modificationDate);
		
		// profiles are created before system fields referencing user are added to the profile type
		// so we may not be able to set those fields yet
		if (setUserReferenceFields)
		{
			profile.setCreatedBy(AppConfig.getRootUserId(), env);
			profile.setLastModifiedBy(AppConfig.getRootUserId(), env);
		}
		
		profile.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		// run field validation
		runFieldValidation(profile, env);
		
		return daoFacade.save(profile, false, AuthData.getRootAuthData(env), env);
	}

	/**
	 * Retrieves objects according to a DAL query.
	 * @param dalQuery
	 * @return
	 * @throws KommetException 
	 */
	@Transactional (readOnly = true)
	public List<Record> select (String dalQuery, EnvData env) throws KommetException
	{
		return daoFacade.select(dalQuery, env);
	}
	
	public List<Type> getTypes (TypeFilter filter, boolean initFields, boolean initUniqueChecks, EnvData env) throws KommetException
	{
		List<Type> types = typeDao.getTypes(filter, appConfig, env, initFields);
		
		// find unique checks for these types
		if (initUniqueChecks)
		{
			types = initUniqueChecks(types, env);
		}
		
		return types;
	}

	public List<Type> getTypes (boolean initFields, boolean initUniqueChecks, EnvData env) throws KommetException
	{
		List<Type> types = typeDao.getTypes(null, appConfig, env, initFields);
		
		// find unique checks for these types
		if (initUniqueChecks)
		{
			types = initUniqueChecks(types, env);
		}
		
		return types;
	}

	/**
	 * Initialized the unique checks on types.
	 * @param types
	 * @return list of objects with initialized unique checks. Note: the order of the original list may not be preserved in the result.
	 * @throws KommetException 
	 */
	public List<Type> initUniqueChecks(List<Type> types, EnvData env) throws KommetException
	{
		UniqueCheckFilter filter = new UniqueCheckFilter();
		Map<KID, Type> typesById = new HashMap<KID, Type>();
		List<KID> originalTypeOrder = new ArrayList<KID>();
		
		// do not initialize createdBy and lastModifiedBy fields on unique checks,
		// because this method is sometimes called when User type is not yet created
		filter.setInitUserReferenceFields(false);
		
		for (Type type : types)
		{
			filter.addTypeId(type.getKID());
			typesById.put(type.getKID(), type);
			originalTypeOrder.add(type.getKID());
		}
		
		// find unique checks for these types
		List<UniqueCheck> checks = uniqueCheckDao.find(filter, env, this);
		
		for (UniqueCheck check : checks)
		{
			typesById.get(check.getTypeId()).addUniqueCheck(check);
		}
		
		List<Type> results = new ArrayList<Type>();
		// restore original type order on the list
		for (KID typeId : originalTypeOrder)
		{
			results.add(typesById.get(typeId));
		}
		return results;
	}

	@Transactional
	public void setFieldRequired(Field field, boolean isRequired, EnvData env) throws KommetException
	{
		if (field.getType() == null)
		{
			throw new KommetException("Field definition does not contain reference to a type");
		}
		
		fieldDao.makeFieldRequired(field, isRequired, env);
		
		// update field info in the env store
		Type type = env.getType(field.getType().getKID());
		if (type == null)
		{
			throw new KommetException("Type with ID " + field.getType().getKID() + " not found");
		}
		
		type.getField(field.getApiName()).setRequired(isRequired);
		
		// the type reference obtained from the env is read-only, so we need to update the type on the env
		// for the change on the field to have effect
		env.updateType(type);
	}

	@Transactional(readOnly = true)
	public Type getById(Long id, EnvData env) throws KommetException
	{
		return typeDao.getById(id, appConfig, env);
	}
	
	@Transactional(readOnly = true)
	public Type getTypeByName (String qualifiedName, boolean initFields, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		TypeFilter filter = new TypeFilter();
		filter.setQualifiedName(qualifiedName);
		List<Type> types = typeDao.getTypes(filter, appConfig, env, initFields);
		return types.isEmpty() ? null : types.get(0);
	}
	
	/**
	 * Performs a SELECT query for objects of a given type.
	 * @param type Type which is queries
	 * @param fields List of field API names to be queried
	 * @param dalConditions DAL WHERE clause (without the WHERE keyword) to be appended to the query
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional(readOnly = true)
	public List<Record> select (Type type, Collection<String> fields, String dalConditions, AuthData authData, EnvData env) throws KommetException
	{
		String query = "SELECT " + MiscUtils.implode(fields, ", ") + " FROM " + type.getQualifiedName();
		if (StringUtils.hasText(dalConditions))
		{
			query += " WHERE " + dalConditions;
		}
		
		return env.getSelectCriteriaFromDAL(query, authData).list();
	}
	
	@Transactional
	public void deleteRecord (KID recordId, AuthData authData, EnvData env) throws KommetException
	{
		// TODO auth data is not used, it will be used when trigger handling will be added to this method
		//daoFacade.delete(recordId, env);
		
		Type type = env.getTypeByRecordId(recordId);
		List<String> fields = Arrays.asList(Field.ID_FIELD_NAME);
		
		if (env.hasTypeAfterTriggersWithOldProxies(type.getKID()))
		{
			// query all fields to use them in the after-delete trigger
			fields = DataAccessUtil.getReadableFieldApiNamesForQuery(type, authData, env, false);
		}
		
		List<Record> records = select(type, fields, Field.ID_FIELD_NAME + " = '" + recordId.getId() + "'", authData, env);
		deleteRecords(records, false, authData, env);
	}

	@Transactional
	public List<Field> getFields(FieldFilter filter, EnvData env) throws KommetException
	{
		return fieldDao.getFields(filter, appConfig, env);
	}
	
	public static boolean isValidDefaultField(Field defaultField, Type type) throws KIDException
	{
		if (!defaultField.getDataTypeId().equals(DataType.FORMULA))
		{
			return defaultField.isRequired();
		}
		else
		{
			// formula fields can be made the default field if all fields used in them are required
			return FormulaParser.isFormulaNonNullable((FormulaDataType)defaultField.getDataType(), type);
		}
	}
	
	/**
	 * Creates a standard controller for every type in the collection. If a standard controller already exists in the
	 * database, but not in the file system, it is retrieved and compiled. If it does not exist in the database, it is
	 * created, inserted and then compiled.
	 * @param types
	 * @param env
	 * @throws KommetException
	 */
	@Transactional
	public void createStandardControllers (boolean generateCodeAnew, Collection<Type> types, AuthData authData, EnvData env) throws KommetException
	{
		for (Type type : types)
		{
			recompileStandardTypeController(generateCodeAnew, type, authData, env);
		}
	}

	/**
	 * Delete records represented by a list of object proxies.
	 * 
	 * TODO write tests for this method
	 * 
	 * @param objects
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	public <T extends RecordProxy> void delete(List<T> objects, AuthData authData, EnvData env) throws KommetException
	{
		daoFacade.delete(objects, authData, env);
	}
	
	/**
  	 * Create an association between the given record and a parent record using the given association field.
  	 * @param record
  	 * @param associationParentRecordId
  	 * @param assocFieldId
  	 * @param authData
  	 * @throws KommetException
  	 */
	@Transactional
  	public Record associate(KID assocFieldId, KID recordId, KID associatedRecordId, AuthData authData, EnvData env) throws KommetException
	{
		return associate(assocFieldId, recordId, associatedRecordId, authData, false, env);
	}
	
	/**
  	 * Create an association between the given record and a parent record using the given association field.
  	 * @param record
  	 * @param associationParentRecordId
  	 * @param assocFieldId
  	 * @param authData
  	 * @throws KommetException
  	 */
	@Transactional
  	public Record associate(KID assocFieldId, KID recordId, KID associatedRecordId, AuthData authData, boolean failIfAlreadyAssociated, EnvData env) throws KommetException
	{	
      	Field assocField = getField(assocFieldId, env);
      	
      	if (assocField == null)
      	{
      		throw new KommetException("Association field with ID " + assocFieldId + " does not exist");
      	}
      	
      	AssociationDataType dt = (AssociationDataType)assocField.getDataType();
      	Type associationLinkingType = env.getType(((AssociationDataType)assocField.getDataType()).getLinkingType().getKeyPrefix());
      	
      	Record assocLink = new Record(associationLinkingType);
      	assocLink.setField(dt.getSelfLinkingField() + "." + Field.ID_FIELD_NAME, recordId, env);
      	assocLink.setField(dt.getForeignLinkingField() + "." + Field.ID_FIELD_NAME, associatedRecordId, env);
      	
      	// TODO right now every time a link is created, it is queried first, which is not optimal.
      	// A better solution would be to allow for saving and in case database throws an exception
      	// for non-unique records, parse this exception or ignore it, depending on the failIfAlreadyAssociated flag.
      	Record linkingRecord = getAssociation(assocFieldId, recordId, associatedRecordId, env);
      	
      	if (linkingRecord != null)
      	{
      		if (failIfAlreadyAssociated)
      		{
      			throw new KommetException("Association between records " + recordId + " and " + associatedRecordId + " through " + associationLinkingType.getQualifiedName() + " already exists");
      		}
      		else
      		{
      			return linkingRecord;
      		}
      	}
      	
      	return save(assocLink, authData, env);
	}

	/**
	 * Returns a record of the linking type between two records.
	 * @param assocFieldId
	 * @param recordId
	 * @param associatedRecordId
	 * @param env
	 * @return
	 * @throws KommetException 
	 */
	@Transactional(readOnly = true)
	public Record getAssociation(KID assocFieldId, KID recordId, KID associatedRecordId, EnvData env) throws KommetException
	{
		// get association field by its ID
		Field assocField = getField(assocFieldId, env);
      	
      	if (assocField == null)
      	{
      		throw new KommetException("Association field with ID " + assocFieldId + " does not exist");
      	}
      	
      	// build conditions on the linking type
      	AssociationDataType dt = (AssociationDataType)assocField.getDataType();
      	Type associationLinkingType = env.getType(((AssociationDataType)assocField.getDataType()).getLinkingType().getKeyPrefix());
      	
      	String condition = dt.getSelfLinkingField() + "." + Field.ID_FIELD_NAME + " = '" + recordId + "' AND ";
      	condition += dt.getForeignLinkingField() + "." + Field.ID_FIELD_NAME + " = '" + associatedRecordId + "'";
      	
      	List<String> fields = new ArrayList<String>();
      	fields.add(Field.ID_FIELD_NAME);
      	
      	// query the linking type
      	List<Record> associatingRecords = env.select(associationLinkingType, fields, condition, null);
      	
      	return associatingRecords.isEmpty() ? null : associatingRecords.get(0);
	}

	@Transactional
	public void unassociate(KID associationFieldId, KID recordId, KID associatedRecordId, boolean failIfNotAssociated, AuthData authData, EnvData env) throws KommetException
	{
		Field associationField = getField(associationFieldId, env);
		
		if (!associationField.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			throw new KommetException("Field " + associationFieldId + " is not an association field");
		}
		
		// get linking type
		Type linkingType = ((AssociationDataType)associationField.getDataType()).getLinkingType();
		String selfLinkingField = ((AssociationDataType)associationField.getDataType()).getSelfLinkingField();
		String foreignLinkingField = ((AssociationDataType)associationField.getDataType()).getForeignLinkingField();
		
		// get linking record
		List<Record> linkingRecords = env.select(linkingType, MiscUtils.toList(Field.ID_FIELD_NAME), selfLinkingField + "." + Field.ID_FIELD_NAME + " = '" + recordId + "' AND " + foreignLinkingField + "." + Field.ID_FIELD_NAME + " = '" + associatedRecordId + "'", null);
		
		if (!linkingRecords.isEmpty())
		{
			deleteRecords(linkingRecords, false, authData, env);
		}
		else if (failIfNotAssociated)
		{
			throw new KommetException("Cannot unassociate records because they are not associated");
		}
	}

	public Map<KID, Record> getRecordMap(Collection<KID> ids, AuthData authData, EnvData env) throws KommetException
	{
		// first split queries by type
		Map<KeyPrefix, Set<KID>> idsByKeyPrefix = new HashMap<KeyPrefix, Set<KID>>();
		for (KID id : ids)
		{
			KeyPrefix prefix = KeyPrefix.get(id.getId().substring(0, KeyPrefix.LENGTH));
			if (!idsByKeyPrefix.containsKey(prefix))
			{
				idsByKeyPrefix.put(prefix, new HashSet<KID>());
			}
			
			idsByKeyPrefix.get(prefix).add(id);
		}
		
		Map<KID, Record> recordsById = new HashMap<KID, Record>();
		for (KeyPrefix prefix : idsByKeyPrefix.keySet())
		{
			List<Record> records = getRecords(idsByKeyPrefix.get(prefix), env.getType(prefix), DataAccessUtil.getReadableFieldApiNamesForQuery(env.getType(prefix), authData, env, false), null, env);
			for (Record rec : records)
			{
				recordsById.put(rec.getKID(), rec);
			}
		}
		
		return recordsById;
	}

	@Transactional(readOnly = true)
	public List<Record> getRecords (Collection<KID> ids, Type type, List<String> fieldApiNames, AuthData authData, EnvData env) throws KommetException
	{
		List<String> fields = new ArrayList<String>();
		
		if (authData != null)
		{
			// check to which properties this user has access
			fields.addAll(DataAccessUtil.getFieldsNamesForDisplay(type, authData, fieldApiNames, env));
		}
		else
		{
			fields.addAll(fieldApiNames);
		}
		
		if (ids == null)
		{
			ids = new HashSet<KID>();
		}
		
		List<String> sIds = new ArrayList<String>();
		for (KID id : ids)
		{
			sIds.add(id.getId());
		}
		
		return select(type, fields, ids.isEmpty() ? null : Field.ID_FIELD_NAME + " IN (" + MiscUtils.implode(sIds, ",", "'", null) + ")", authData, env);
	}

	/**
	 * Create triggers on the type's db table that will check whether the current lastmodifiedby user
	 * has rights to edit and delete the given record.
	 * @param type
	 * @param env
	 * @throws KommetException
	 */
	@Transactional
	public void createCheckPermissionsTriggers(Type type, EnvData env) throws KommetException
	{
		typeDao.createCheckEditDeletePermissionsTriggers(type, env);
	}

	public static void validateSharingControlledByField(KID fieldId, Type type) throws TypeDefinitionException
	{
		Field field = type.getField(fieldId);
		
		if (type.getField(fieldId) == null)
		{
			throw new TypeDefinitionException("Field cannot be set as sharingControlledByField because it does not exist on the given type");
		}
		
		if (!field.getDataTypeId().equals(DataType.KOMMET_ID) && !field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
		{
			throw new TypeDefinitionException("Field " + field.getApiName() + " cannot be set as sharingControlledByField because it is not of type KID/type reference");
		}
	}
	
	@Transactional
	public LinkingTypeResult createLinkingType(Type type, Type associatedType, AuthData authData, EnvData env) throws KommetException
	{
		String linkingTypeName = "LinkingType_" + type.getKID() + "_" + associatedType.getKID();
		String packageName = "kommet.linkingtypes";
		Integer suffixIndex = 0;
		
		if (env.getType(packageName + "." + linkingTypeName) != null)
		{
			// make sure the type name is unique, keep adding suffixes until a unique name is found
			do
			{
				suffixIndex++;
			}
			while (env.getType(packageName + "." + linkingTypeName + suffixIndex) != null);
			
			linkingTypeName += suffixIndex;
		}
		
		Type linkingType = new Type();
		linkingType.setApiName(linkingTypeName);
		linkingType.setPackage(packageName);
		linkingType.setBasic(false);
		linkingType.setLabel(linkingType.getApiName());
		linkingType.setPluralLabel(type.getApiName());
		linkingType.setAutoLinkingType(true);
		
		// create fields
		Field selfLinkingField = new Field();
		selfLinkingField.setApiName(StringUtils.uncapitalize(type.getApiName()));
		selfLinkingField.setLabel(type.getLabel());
		selfLinkingField.setRequired(true);
		TypeReference selfLinkingDT = new TypeReference(type);
		selfLinkingDT.setCascadeDelete(true);
		selfLinkingField.setDataType(selfLinkingDT);
		linkingType.addField(selfLinkingField);
		
		Field foreignLinkingField = new Field();
		
		String foreignFieldName = StringUtils.uncapitalize(associatedType.getApiName());
		if (type.getKID().equals(associatedType.getKID()))
		{
			foreignFieldName += "Associated";
		}
		
		foreignLinkingField.setApiName(foreignFieldName);
		foreignLinkingField.setLabel(associatedType.getLabel());
		foreignLinkingField.setRequired(true);
		TypeReference foreignLinkingDT = new TypeReference(associatedType);
		foreignLinkingDT.setCascadeDelete(true);
		foreignLinkingField.setDataType(foreignLinkingDT);
		linkingType.addField(foreignLinkingField);
		
		linkingType = createType(linkingType, authData, env);
		
		LinkingTypeResult result = new LinkingTypeResult();
		result.setType(linkingType);
		result.setSelfLinkingField(selfLinkingField);
		result.setForeignLinkingField(foreignLinkingField);
		return result;
	}

	@Transactional(readOnly = true)
	public Type getType(Long id, EnvData env) throws KommetException
	{
		return typeDao.getById(id, appConfig, env);
	}
	
	private String getUserSettingValue(String key, AuthData ctxAuthData, AuthData authData, EnvData env) throws KommetException
	{
		RecordProxy setting = uchDao.getSetting(env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX)), Arrays.asList("value"), "key", key, compiler, ctxAuthData, authData, env);
		return setting != null ? (String)setting.getField("value") : null;
	}
	
	public class LinkingTypeResult
	{
		private Type type;
		private Field selfLinkingField;
		private Field foreignLinkingField;
		public Type getType()
		{
			return type;
		}
		public void setType(Type type)
		{
			this.type = type;
		}
		public Field getSelfLinkingField()
		{
			return selfLinkingField;
		}
		public void setSelfLinkingField(Field selfLinkingField)
		{
			this.selfLinkingField = selfLinkingField;
		}
		public Field getForeignLinkingField()
		{
			return foreignLinkingField;
		}
		public void setForeignLinkingField(Field foreignLinkingField)
		{
			this.foreignLinkingField = foreignLinkingField;
		}
	}
}