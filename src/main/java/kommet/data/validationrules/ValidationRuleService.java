/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.validationrules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.ValidationRule;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.labels.InvalidTextLabelReferenceException;
import kommet.labels.TextLabelDao;
import kommet.labels.TextLabelFilter;
import kommet.labels.TextLabelReference;
import kommet.rel.RELParser;
import kommet.utils.MiscUtils;
import kommet.utils.ValidationUtil;

@Service
public class ValidationRuleService
{
	@Inject
	ValidationRuleDao vrDao;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	TextLabelDao textLabelDao;
	
	private static final Logger log = LoggerFactory.getLogger(ValidationRuleService.class);
	
	@Transactional(readOnly = true)
	public List<ValidationRule> get (ValidationRuleFilter filter, EnvData env) throws KommetException
	{
		return vrDao.get(filter, env);
	}
	
	@Transactional
	public ValidationRule saveSystemValidationRule (ValidationRule rule, AuthData authData, EnvData env) throws KommetException
	{
		return save(rule, true, true, authData, env);
	}
	
	@Transactional
	public ValidationRule save (ValidationRule rule, AuthData authData, EnvData env) throws KommetException
	{
		return save(rule, false, false, authData, env);
	}
	
	@Transactional
	public ValidationRule save (ValidationRule rule, boolean skipTriggers, boolean skipSharing, AuthData authData, EnvData env) throws KommetException
	{	
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(rule.getName()))
		{
			throw new KommetException("Invalid validation rule name " + rule.getName());
		}
		
		if (StringUtils.hasText(rule.getErrorMessageLabel()))
		{
			// make sure the text label exists
			TextLabelFilter filter = new TextLabelFilter();
			filter.addKey(rule.getErrorMessageLabel());
			if (textLabelDao.get(filter, env).isEmpty())
			{
				throw new InvalidTextLabelReferenceException(rule.getErrorMessageLabel(), TextLabelReference.VALIDATION_RULE);
			}
		}
		else if (!StringUtils.hasText(rule.getErrorMessage()))
		{
			throw new ValidationRuleException("Neither errorMessage nor errorMessageLabel is filled on validation rule");
		}
		
		// parse the validation rule to find the fields which are used in it
		initReferencedFields(rule, env);
		
		rule = vrDao.save(rule, skipTriggers, skipSharing, false, false, authData, env);
		
		// reinitialize all validation rules for this type on environment
		// do this before actually saving the VR to detect any REL syntax errors
		CompilationResult result = initTypeValidationRulesOnEnv(rule.getTypeId(), env);
		
		if (!result.isSuccess())
		{
			log.debug(result.getDescription());
			throw new ValidationRuleCompileException(result);
		}
		
		return rule;
	}
	
	public static void initReferencedFields(ValidationRule rule, EnvData env) throws KommetException
	{
		Set<String> fieldsUsedInCondition = RELParser.getRecordFields(rule.getCode(), env);
		
		if (fieldsUsedInCondition.isEmpty())
		{
			throw new ValidationRuleException("Validation rule condition does not reference any fields");
		}
		
		// list of fields that does not be in the condition, either because they don't exist, or are too nested
		Set<String> invalidFieldsInCondition = new HashSet<String>();
		List<String> usedFieldIds = new ArrayList<String>();
		Type type = env.getType(rule.getTypeId());
		
		// make sure the validation rule does not use any nested fields, except IDs of type reference fields
		for (String fieldName : fieldsUsedInCondition)
		{
			String fieldToFetch = null;
			if (fieldName.contains("."))
			{
				List<String> subProps = MiscUtils.splitAndTrim(fieldName, "\\.");
				if (subProps.size() > 2 || !subProps.get(subProps.size() - 1).equals(Field.ID_FIELD_NAME))
				{
					invalidFieldsInCondition.add(fieldName);
				}
				else
				{
					fieldToFetch = subProps.get(0);
				}
			}
			else
			{
				fieldToFetch = fieldName;
			}
			
			if (fieldToFetch != null)
			{
				Field field = type.getField(fieldToFetch);
				
				if (field == null)
				{
					invalidFieldsInCondition.add(fieldName);
				}
				else
				{
					usedFieldIds.add(field.getKID().getId());
				}
			}
		}
		
		if (!invalidFieldsInCondition.isEmpty())
		{
			throw new ValidationRuleException("Validation rule references invalid fields: " + MiscUtils.implode(invalidFieldsInCondition, ", "));
		}
		
		rule.setReferencedFields(MiscUtils.implode(usedFieldIds, ","));
	}

	private CompilationResult initTypeValidationRulesOnEnv(KID typeId, EnvData env) throws KommetException
	{
		// find all validation rules for this type
		ValidationRuleFilter filter = new ValidationRuleFilter();
		filter.addTypeId(typeId);
		filter.setIsActive(true);
		
		Set<ValidationRule> rules = new HashSet<ValidationRule>();
		rules.addAll(get(filter, env));
		
		return initTypeValidationRulesOnEnv(typeId, rules, env);
	}

	@Transactional(readOnly = true)
	public ValidationRule get (KID ruleId, EnvData env) throws KommetException
	{
		return vrDao.get(ruleId, env);
	}
	
	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		delete(vrDao.get(id, env), authData, env);
	}

	@Transactional
	public void delete(ValidationRule vr, AuthData authData, EnvData env) throws KommetException
	{
		vrDao.delete(vr.getId(), authData, env);
		
		// reinitialize all validation rules for this type on environment
		// do this before actually saving the VR to detect any REL syntax errors
		CompilationResult result = initTypeValidationRulesOnEnv(vr.getTypeId(), env);
		
		if (!result.isSuccess())
		{
			System.out.println(result.getDescription());
			throw new ValidationRuleCompileException(result);
		}
	}

	public void initValidationRulesOnEnv(EnvData env) throws KommetException
	{
		// find only active rules
		ValidationRuleFilter filter = new ValidationRuleFilter();
		filter.setIsActive(true);
		
		// find all validation rules
		List<ValidationRule> vrs = get(filter, env);
		
		// divide validation rules by type
		Map<KID, Set<ValidationRule>> rulesByTypeId = new HashMap<KID, Set<ValidationRule>>();
		for (ValidationRule vr : vrs)
		{
			if (!rulesByTypeId.containsKey(vr.getTypeId()))
			{
				rulesByTypeId.put(vr.getTypeId(), new HashSet<ValidationRule>());
			}
			rulesByTypeId.get(vr.getTypeId()).add(vr);
		}
		
		for (KID typeId : rulesByTypeId.keySet())
		{
			initTypeValidationRulesOnEnv(typeId, rulesByTypeId.get(typeId), env);
		}
	}

	private CompilationResult initTypeValidationRulesOnEnv(KID typeId, Set<ValidationRule> vrs, EnvData env) throws KommetException
	{	
		// compile a validation rule executor for this type
		CompilationResult res = createValidationRuleExecutor(env.getType(typeId), vrs, env);
		
		env.setHasValidationRules(typeId, !vrs.isEmpty());
		
		return res;
	}

	private CompilationResult createValidationRuleExecutor(Type type, Set<ValidationRule> vrs, EnvData env) throws KommetException
	{
		Class vre = ValidationRuleUtil.getValidationRuleExecutor(type, null, vrs, compiler, env);
		CompilationResult result = compiler.compile(vre, env);
		
		if (!result.isSuccess())
		{
			System.out.print(vre.getJavaCode());
			log.debug("CODE " + vre.getJavaCode());
		}
		
		// reset class loader so that the new VRE can be loaded
		compiler.resetClassLoader(env);
		
		return result;
	}

	@Transactional(readOnly = true)
	public ValidationRule getByName(String name, AuthData authData, EnvData env) throws KommetException
	{
		ValidationRuleFilter filter = new ValidationRuleFilter();
		filter.setName(name);
		List<ValidationRule> rules = vrDao.get(filter, env);
		return !rules.isEmpty() ? rules.get(0) : null;
	}
}