/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.GroupRecordSharing;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyUtil;
import kommet.basic.SharingRule;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.basic.UserRecordSharing;
import kommet.dao.SharingRuleDao;
import kommet.data.DataAccessUtil;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.sharing.GroupRecordSharingFilter;
import kommet.data.sharing.SharingService;
import kommet.data.sharing.UserRecordSharingFilter;
import kommet.env.EnvData;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.filters.SharingRuleFilter;
import kommet.koll.SharingRuleException;
import kommet.koll.annotations.QueriedTypes;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.MiscUtils;

@Service
public class SharingRuleService
{
	@Inject
	SharingRuleDao dao;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	ErrorLogService logService;
	
	@Transactional
	public SharingRule save (SharingRule rule, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{	
		rule = dao.save(rule, authData, env);
		
		// recalculate sharing for the type for which sharing is created
		recalculateSharing(rule, null, false, dataService, env);
		
		// if sharing recalculation succeeded, at sharing rule to the env cache
		refreshSharingRulesForType(rule.getReferencedType(), env);
		
		return rule;
	}
	
	/**
	 * For this sharing rule, apply the sharings.
	 * This is a time-consuming operation.
	 * @param rule
	 * @param isNewRecord 
	 * @param records 
	 * @param authData
	 * @param env
	 * @throws KommetException 
	 */
	private void recalculateSharing(SharingRule rule, List<Record> records, boolean isDependent, DataService dataService, EnvData env) throws KommetException
	{
		if (!rule.getType().equals("Code"))
		{
			throw new SharingRuleException("Cannot recalculate sharings for (yet) unsupported sharing rule type " + rule.getType());
		}
		
		// always use root auth data while recalculating sharings
		AuthData rootAuthData = AuthData.getRootAuthData(env);
		
		// call the sharing rule method
		Method method = compiler.getMethod(MiscUtils.userToEnvPackage(rule.getFile().getQualifiedName(), env), rule.getMethod(), env);
		
		Set<KID> recordIds = null;
		
		if (records != null && !records.isEmpty())
		{
			recordIds = new HashSet<KID>();
			for (Record r : records)
			{
				recordIds.add(r.getKID());
			}
		}
		
		// delete all the user sharings for this rule - we will create them anew
		removeSharingsForRules(Arrays.asList(rule.getId()), recordIds, env);
		
		String methodName = rule.getFile().getQualifiedName() + "." + rule.getMethod();
		
		if (records == null)
		{
			Type referencedType = env.getType(rule.getReferencedType());
			
			// get all records of this type
			records = dataService.getRecords(null, referencedType, DataAccessUtil.getReadableFieldApiNamesForQuery(referencedType, rootAuthData, env, false), rootAuthData, env);
		}
		else if (isDependent)
		{
			throw new SharingRuleException("Records cannot be passed to recalculating method for dependent types, because these records have to extracted for each sharing rule separately");
		}
		
		// recalculate sharing for each record
		for (Record r : records)
		{
			try
			{
				// if the sharing method contains queries, they will be automatically run with current auth data, not root auth data
				// but we want to run them will full root permissions, so we will temporarily switch auth data
				AuthData currentAuthData = env.currentAuthData();
				env.addAuthData(AuthData.getRootAuthData(env));
				
				Object returnedItems = method.invoke(null, RecordProxyUtil.generateCustomTypeProxy(r, env, compiler));
				
				// restore current user's auth data
				env.addAuthData(currentAuthData);
				
				if (returnedItems == null)
				{
					continue;
				}
				
				if (!(returnedItems instanceof List<?>))
				{
					throw new SharingRuleException("Sharing rule method " + methodName + " did not return a list");
				}
				
				List<?> itemsToShareWith = (List<?>)returnedItems;
				
				Class<?> userClass = compiler.getClass(User.class.getName(), true, env);
				Class<?> userGroupClass = compiler.getClass(UserGroup.class.getName(), true, env);
				
				// get the getter of the ID property so that we can obtain the ID of the user/user group
				Class<?> recordProxyClass = compiler.getClass(RecordProxy.class.getName(), false, env);
				Method idGetter = recordProxyClass.getMethod("getId");
				
				for (Object o : itemsToShareWith)
				{
					if (o == null)
					{
						// ignore null values
						continue;
					}
					else if (userClass.equals(o.getClass()))
					{
						sharingService.shareRecord(r.getKID(), (KID)idGetter.invoke(o), rule.getIsEdit(), rule.getIsDelete(), "Sharing rule " + methodName, false, rule.getId(), null, rootAuthData, env);
					}
					else if (userGroupClass.equals(o.getClass()))
					{
						sharingService.shareRecordWithGroup(r.getKID(), (KID)idGetter.invoke(o), rule.getIsEdit(), rule.getIsDelete(), "Sharing rule " + methodName, false, rule.getId(), null, rootAuthData, env);
					}
					else
					{
						throw new SharingRuleException("Sharing rule method " + methodName + " returned a list of " + o.getClass().getName());
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, rootAuthData.getUserId(), AuthData.getRootAuthData(env), env);
				throw new SharingRuleException("Error recalculating sharing rule method " + methodName + ". Nested: " + e.getMessage());
			}
		}
	}
	
	@Transactional(readOnly = true)
	public List<SharingRule> get (SharingRuleFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return dao.get(filter, authData, env);
	}

	@Transactional
	public void delete(Collection<SharingRule> rules, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{	
		if (rules == null || rules.isEmpty())
		{
			return;
		}
		
		List<KID> deletedRules = new ArrayList<KID>();
		for (SharingRule rule : rules)
		{
			deletedRules.add(rule.getId());
		}
		
		sharingService.find(null, env);
		dao.delete(rules, authData, env);
		
		// no need to manually delete URS records referencing this sharing rule - they will be cascaded thanks to the setting on the relationship between URS and sharing rule
		// removeSharingsForRules(deletedRules, env);
		
		for (SharingRule rule : rules)
		{	
			env.unregisterSharingRule(rule, compiler);
			
			/*recalculateSharingForType(rule.getReferencedType(), null, false, dataService, env);
			
			List<String> dependentTypeIds = MiscUtils.splitAndTrim(rule.getDependentTypes(), ";");
			for (String sTypeId : dependentTypeIds)
			{
				recalculateDependentSharingForType(KID.get(sTypeId), dataService, env);
			}*/
			
			refreshSharingRulesForType(rule.getReferencedType(), env);
		}
	}

	private void refreshSharingRulesForType(KID typeId, EnvData env) throws KommetException
	{
		// find all rules for this type
		SharingRuleFilter filter = new SharingRuleFilter();
		filter.addReferencedType(typeId);
		env.getSharingRulesByType().put(typeId, get(filter, AuthData.getRootAuthData(env), env));
	}

	/**
	 * Removes all URS records related to the given sharing rules.
	 * @param ruleIds
	 * @param env
	 * @throws KommetException
	 */
	private void removeSharingsForRules(Collection<KID> ruleIds, Set<KID> recordIds, EnvData env) throws KommetException
	{
		UserRecordSharingFilter ursFilter = new UserRecordSharingFilter();
		Set<KID> ruleIdSet = new HashSet<KID>();
		ruleIdSet.addAll(ruleIds);
		ursFilter.setSharingRuleIds(ruleIdSet);
		ursFilter.setRecordIds(recordIds);
		
		for (UserRecordSharing urs : sharingService.find(ursFilter, env))
		{
			sharingService.deleteSharing(urs.getId(), env);
		}
		
		GroupRecordSharingFilter grsFilter = new GroupRecordSharingFilter();
		grsFilter.setSharingRuleIds(ruleIdSet);
		grsFilter.setRecordIds(recordIds);
		
		for (GroupRecordSharing grs : sharingService.get(grsFilter, env))
		{
			sharingService.deleteGroupSharings(Arrays.asList(grs), env);
		}
	}
	
	@Transactional
	public void recalculateSharingForType (KID typeId, KID recordId, boolean isNewRecord, DataService dataService, EnvData env) throws KommetException
	{
		AuthData rootAuthData = AuthData.getRootAuthData(env);
		
		if (!env.getSharingRulesByType().containsKey(typeId))
		{
			return;
		}
		
		Type type = env.getType(typeId);
		
		List<KID> recordIds = new ArrayList<KID>();
		if (recordId != null)
		{
			recordIds.add(recordId);
		}
		
		// get all records of this type
		List<Record> records = dataService.getRecords(recordIds, type, DataAccessUtil.getReadableFieldApiNamesForQuery(type, rootAuthData, env, false), rootAuthData, env);
		
		if (env.getSharingRulesByType().get(typeId) != null)
		{
			for (SharingRule rule : env.getSharingRulesByType().get(typeId))
			{
				if (isNewRecord)
				{
					// recalculate this rule if it depends on the record, or if the record is new
					recalculateSharing(rule, records, false, dataService, env);
				}
			}
		}
	}

	public void recalculateDependentSharingForType(KID typeId, DataService dataService, EnvData env) throws KommetException
	{
		if (env.getDependentSharingRulesByType().get(typeId) == null)
		{
			return;
		}
		
		for (SharingRule rule : env.getDependentSharingRulesByType().get(typeId))
		{
			// recalculate this rule if it depends on the record, or if the record is new
			recalculateSharing(rule, null, true, dataService, env);
		}
	}

	public static List<Type> getQueriedTypes(SharingRule rule, KommetCompiler compiler, EnvData env) throws KommetException
	{
		java.lang.Class<?> cls = null;
		
		// find rule class
		try
		{
			cls = compiler.getClass(rule.getFile(), false, env);
		}
		catch (Exception e)
		{
			throw new KommetException("Could not get class " + rule.getFile().getId() + "/" + rule.getFile().getQualifiedName() + " for sharing rule: " + e.getMessage());
		}
		
		// find rule method
		Method method = MiscUtils.getMethodByName(cls, rule.getMethod());
		
		if (method == null)
		{
			throw new KommetException("Method " + cls.getName() + "/" + rule.getMethod() + " not found");
		}
		
		List<Type> queriedTypes = new ArrayList<Type>();
		
		if (!method.isAnnotationPresent(QueriedTypes.class))
		{
			return queriedTypes;
		}
		
		QueriedTypes annot = method.getAnnotation(QueriedTypes.class);
		
		for (String typeName : annot.value())
		{
			queriedTypes.add(env.getType(typeName));
		}
		
		return queriedTypes;
	}

	/**
	 * Initializes cached sharing rule on the environment.
	 * @param env
	 * @throws KommetException
	 */
	public void initSharingRules(EnvData env) throws KommetException
	{
		// find all sharing rules
		List<SharingRule> rules = get(new SharingRuleFilter(), AuthData.getRootAuthData(env), env);
		
		for (SharingRule rule : rules)
		{
			env.registerSharingRule(rule);
		}
	}

	@Transactional(readOnly = true)
	public SharingRule get(String ruleName, AuthData authData, EnvData env) throws KommetException
	{
		SharingRuleFilter filter = new SharingRuleFilter();
		filter.setName(ruleName);
		List<SharingRule> rules = get(filter, authData, env);
		return rules.isEmpty() ? null : rules.get(0);
	}
}