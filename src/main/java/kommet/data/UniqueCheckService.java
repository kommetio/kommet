/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.UniqueCheck;
import kommet.dao.FieldDefinitionException;
import kommet.dao.UniqueCheckDao;
import kommet.dao.UniqueCheckFilter;
import kommet.data.datatypes.DataType;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;
import kommet.utils.ValidationUtil;

@Service
public class UniqueCheckService
{
	@Inject
	UniqueCheckDao uniqueCheckDao;
	
	@Transactional
	public UniqueCheck save (UniqueCheck uc, AuthData authData, EnvData env) throws KommetException
	{
		return save(uc, false, authData, env);
	}
	
	@Transactional
	public UniqueCheck save (UniqueCheck uc, boolean isSkipSharing, AuthData authData, EnvData env) throws KommetException
	{
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(uc.getName()))
		{
			throw new KommetException("Invalid unique check name " + uc.getName());
		}
		
		// make sure fields used in the unique check have correct data types
		validateFields(uc, env);
		
		if (!StringUtils.hasText(uc.getDbName()))
		{
			uc.setDbName(UniqueCheck.generateDbName(uc.getTypeId(), env));
		}
		
		if (uc.getIsSystem() == null)
		{
			// unique checks are non-system by default
			uc.setIsSystem(false);
		}
		
		// make sure a unique check for this type and fields does not already exist
		Type envType = env.getType(uc.getTypeId());
		
		if (envType.getUniqueChecks() != null)
		{
			for (UniqueCheck existingCheck : envType.getUniqueChecks())
			{
				// if this is not the same check, but has the same set of fields
				if (!existingCheck.getId().equals(uc.getId()) && existingCheck.getParsedFieldIds().containsAll(uc.getParsedFieldIds()) && uc.getParsedFieldIds().size() == existingCheck.getParsedFieldIds().size())
				{
					throw new KommetException("Duplicate unique check for the given field set");
				}
			}
		}
		
		uc = uniqueCheckDao.save(uc, true, isSkipSharing, true, false, authData, env);
		
		String createCheckSQL = getCreateUniqueCheckSQL(uc, env);
		env.getJdbcTemplate().execute(createCheckSQL);
		
		// register the unique check with the type on env
		envType.addUniqueCheck(uc);
		
		// register the type again - simply adding it to the type using addUniqueCheck above only works if the unique check array list already exists on the type
		// if it does not, the call to addUniqueCheck creates it, but it is created on the cloned type fetched from env using env.getType(), so it has no effect on the instance of
		// type on the env
		// with non-empty unique check lists it works, because cloned type instance contains a reference to an array list that is not cloned
		env.registerType(envType);
		env.addTypeMapping(envType);
		
		return uc;
	}
	
	private void validateFields(UniqueCheck uc, EnvData env) throws KommetException
	{
		Type type = env.getType(uc.getTypeId());
		List<String> errors = new ArrayList<String>();
		
		if (uc.getParsedFieldIds() == null || uc.getParsedFieldIds().isEmpty())
		{
			throw new KommetException("Cannot save unique check. Parsed field IDs collection is empty");
		}
		
		for (KID fieldId : uc.getParsedFieldIds())
		{
			Field field = type.getField(fieldId);
			
			if (!isValidUniqueCheckFieldDataType(field.getDataTypeId()))
			{
				errors.add(field.getApiName() + " (" + field.getDataType().getName() + ")");
			}
		}
		
		if (!errors.isEmpty())
		{
			throw new FieldDefinitionException("Invalid field data types in unique check: " + MiscUtils.implode(errors, ", "));
		}
	}
	
	public static boolean isValidUniqueCheckFieldDataType (int dataTypeId)
	{
		return dataTypeId != DataType.ASSOCIATION && dataTypeId != DataType.INVERSE_COLLECTION && dataTypeId != DataType.FORMULA && dataTypeId != DataType.BLOB && dataTypeId != DataType.MULTI_ENUMERATION;
	}

	/**
	 * Generates an SQL command to create a unique check from a given check object.
	 * @param check
	 * @return
	 * @throws KommetException 
	 */
	private static String getCreateUniqueCheckSQL(UniqueCheck check, EnvData env) throws KommetException
	{
		// get field IDs
		String[] fieldIds = check.getFieldIds().split(";");
		
		Type type = env.getType(check.getTypeId());
		if (type == null)
		{
			throw new KommetException("Type " + check.getTypeId() + " specified for unique check not found");
		}
		
		Map<KID, Field> fieldsByRid = type.getFieldsByKID();
		
		List<String> colNames = new ArrayList<String>();
		for (int i = 0; i < fieldIds.length; i++)
		{
			Field field = fieldsByRid.get(KID.get(fieldIds[i]));
			if (field == null)
			{
				throw new KommetException("Unique check includes field with ID " + fieldIds[i] + " that does not exist on the type.");
			}
			colNames.add(field.getDbColumn());
		}
		
		StringBuilder sql = new StringBuilder();
		// remove old constraint
		sql.append("ALTER TABLE ").append(type.getDbTable()).append(" DROP CONSTRAINT IF EXISTS ").append(check.getDbName()).append("; ");
		// add new constraint
		sql.append("ALTER TABLE ").append(type.getDbTable()).append("  ADD CONSTRAINT ").append(check.getDbName()).append(" UNIQUE(").append(MiscUtils.implode(colNames, ", ")).append(")");
		
		return sql.toString();
	}
	
	@Transactional(readOnly = true)
	public List<UniqueCheck> findForField(Field field, EnvData env, DataService dataService) throws KommetException
	{
		return uniqueCheckDao.findForField(field, env, dataService);
	}

	@Transactional
	public void deleteForField(Field field, EnvData env, DataService typeService) throws KommetException
	{
		List<UniqueCheck> checks = uniqueCheckDao.findForField(field, env, typeService);
		uniqueCheckDao.delete(checks, true, null, env);
		
		// delete the checks for type
		Type envType = env.getType(field.getType().getKID());
		for (UniqueCheck check : checks)
		{
			envType.removeUniqueCheckById(check.getId());
		}
		
		// remove from DB
		for (UniqueCheck check : checks)
		{
			removeCheckConstraint(check, env);
		}
	}

	private void removeCheckConstraint(UniqueCheck check, EnvData env) throws KommetException
	{
		StringBuilder sql = new StringBuilder("ALTER TABLE ");
		sql.append(env.getType(check.getTypeId()).getDbTable()).append(" DROP CONSTRAINT IF EXISTS ");
		sql.append(check.getDbName());
		
		env.getJdbcTemplate().execute(sql.toString());
	}

	@Transactional(readOnly = true)
	public UniqueCheck getByName(String name, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		UniqueCheckFilter filter = new UniqueCheckFilter();
		filter.setName(name);
		List<UniqueCheck> checks = uniqueCheckDao.find(filter, env, dataService);
		return !checks.isEmpty() ? checks.get(0) : null;
	}

	@Transactional(readOnly = true)
	public List<UniqueCheck> find(UniqueCheckFilter filter, EnvData env, DataService dataService) throws KommetException
	{
		return uniqueCheckDao.find(filter, env, dataService);
	}

	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		UniqueCheck check = uniqueCheckDao.get(id, env);
		
		if (check == null)
		{
			throw new KommetException("Unique check with ID " + id + " does not exist");
		}
		
		uniqueCheckDao.delete(id, true, null, env);
		
		// delete check from type
		Type envType = env.getType(check.getTypeId());
		
		if (envType == null)
		{
			throw new KommetException("Type with ID " + check.getTypeId() + " does not exist");
		}
		
		envType.removeUniqueCheckById(id);
		
		// delete constraint from database
		removeCheckConstraint(check, env);
	}

	@Transactional(readOnly = true)
	public UniqueCheck get(KID id, EnvData env) throws KommetException
	{
		return uniqueCheckDao.get(id, env);
	}
}