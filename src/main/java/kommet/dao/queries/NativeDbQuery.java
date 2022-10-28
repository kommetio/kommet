/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import java.util.List;

import org.springframework.util.StringUtils;

import kommet.basic.UniqueCheck;
import kommet.basic.UniqueCheckViolationException;
import kommet.dao.TypePersistenceMapping;
import kommet.dao.dal.CannotModifyAccessTypeException;
import kommet.dao.dal.InsufficientPrivilegesException;
import kommet.data.NotNullConstraintViolationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

/**
 * A query to the native DB engine, i.e. Postgresql.
 * @author Radek Krawiec
 */
public abstract class NativeDbQuery
{
	private TypePersistenceMapping typeMapping;
	private EnvData env;
	
	private static final String POSTGRES_UNIQUE_CONSTRAINT_VIOLATION_SQLSTATE = "23505";
	private static final String POSTGRES_NOT_NULL_CONSTRAINT_VIOLATION_SQLSTATE = "23502";
	private static final String POSTGRES_FKEY_CONSTRAINT_VIOLATION_SQLSTATE = "23503";
	private static final String UNKNOWN_TABLE_SQLSTATE = "42P01";
	
	public NativeDbQuery (TypePersistenceMapping typeMapping, EnvData env)
	{
		this.typeMapping = typeMapping;
		this.env = env;
	}

	protected TypePersistenceMapping getTypeMapping()
	{
		return typeMapping;
	}

	protected EnvData getEnv()
	{
		return env;
	}
	
	/**
	 * Calls an update/delete query wrapped in a "execute_update" database procedure and processes exceptions
	 * returned by this operation.
	 * @param query
	 * @param successStatusCode
	 * @throws KommetException
	 */
	protected void wrapUpdateDeleteQueryCall (String query, String successStatusCode) throws KommetException
	{	
		// even through it is really an update, it is performed as a select query of a function
		List<String> returnCodes = (List<String>)getEnv().getJdbcTemplate().queryForList(query, String.class);
		if (!returnCodes.isEmpty())
		{
			DbOperationResult result = new DbOperationResult(returnCodes.get(0));
			
			if (successStatusCode.equals(result.getStatusCode()) || ("(" + successStatusCode + ")").equals(result.getStatusCode()))
			{
				return;
			}
			else if (InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_ERRCODE.equals(result.getStatusCode()))
			{
				throw new InsufficientPrivilegesException(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_RECORD_MSG);
			}
			else if (InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_ERRCODE.equals(result.getStatusCode()))
			{
				throw new InsufficientPrivilegesException(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_RECORD_MSG);
			}
			else if (InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_SYSTEM_IMMUTABLE_ERRCODE.equals(result.getStatusCode()))
			{
				throw new InsufficientPrivilegesException(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_SYSTEM_IMMUTABLE_RECORD_MSG);
			}
			else if (InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_SYSTEM_IMMUTABLE_ERRCODE.equals(result.getStatusCode()))
			{
				throw new InsufficientPrivilegesException(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_SYSTEM_IMMUTABLE_RECORD_MSG);
			}
			else if (InsufficientPrivilegesException.CANNOT_MODIFY_ACCESS_TYPE.equals(result.getStatusCode()))
			{
				throw new CannotModifyAccessTypeException(CannotModifyAccessTypeException.CANNOT_MODIFY_ACCESS_TYPE_MSG);
			}
			else if (POSTGRES_UNIQUE_CONSTRAINT_VIOLATION_SQLSTATE.equals(result.getStatusCode()))
			{
				throw new UniqueCheckViolationException("Unique check violation", parseFieldsFromUniqueCheck(result.getConstraintName(), env));
			}
			else if (POSTGRES_NOT_NULL_CONSTRAINT_VIOLATION_SQLSTATE.equals(result.getStatusCode()))
			{
				throw new NotNullConstraintViolationException("Not null constraint violation [table " + result.getTableName() + "][column " + result.getColumnName() + "]");
			}
			else if (POSTGRES_FKEY_CONSTRAINT_VIOLATION_SQLSTATE.equals(result.getStatusCode()))
			{
				throw new KommetException("Foreign key constraint violation [table " + result.getTableName() + "][constraint " + result.getConstraintName() + "]");
			}
			else if (UNKNOWN_TABLE_SQLSTATE.equals(result.getStatusCode()))
			{
				throw new KommetException("Unknown table [table " + result.getTableName() + "]");
			}
			else
			{
				throw new KommetException("Uncategorized exception: " + result.getStatusCode() + ". Query:\n" + query);
			}
		}
	}
	
	protected String wrapInsertQueryCall (String query, String successStatusCodePrefix) throws KommetException
	{
		// even through it's really an update, it is performed as a select query of a function
		List<String> returnCodes = (List<String>)getEnv().getJdbcTemplate().queryForList(query, String.class);
		
		if (!returnCodes.isEmpty())
		{
			DbOperationResult result = new DbOperationResult(returnCodes.get(0));
			if (result.getStatusCode().startsWith(successStatusCodePrefix))
			{
				// if the insert operation is successful, the execute_insert Postgres function returns the new record's ID prefixed with the success code.
				// Eg. if the success code is RM.STATUS.OK, the returned string will be RM.STATUS.OK<record-id>
				// All we need to do is to remove the prefix from the returned string to obtain the actual ID
				return result.getStatusCode().substring(successStatusCodePrefix.length());
			}
			else if (InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_ERRCODE.equals(result.getStatusCode()))
			{
				throw new InsufficientPrivilegesException(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_RECORD_MSG);
			}
			else if (InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_ERRCODE.equals(result.getStatusCode()))
			{
				throw new InsufficientPrivilegesException(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_RECORD_MSG);
			}
			else if (InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_SYSTEM_IMMUTABLE_ERRCODE.equals(result.getStatusCode()))
			{
				throw new InsufficientPrivilegesException(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_SYSTEM_IMMUTABLE_RECORD_MSG);
			}
			else if (InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_SYSTEM_IMMUTABLE_ERRCODE.equals(result.getStatusCode()))
			{
				throw new InsufficientPrivilegesException(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_SYSTEM_IMMUTABLE_RECORD_MSG);
			}
			else if (POSTGRES_UNIQUE_CONSTRAINT_VIOLATION_SQLSTATE.equals(result.getStatusCode()))
			{
				throw new UniqueCheckViolationException("Unique check violation", parseFieldsFromUniqueCheck(result.getConstraintName(), env));
			}
			else if (POSTGRES_NOT_NULL_CONSTRAINT_VIOLATION_SQLSTATE.equals(result.getStatusCode()))
			{
				throw new NotNullConstraintViolationException("Not null constraint violation [table " + result.getTableName() + "][column " + result.getColumnName() + "]");
			}
			else if (POSTGRES_FKEY_CONSTRAINT_VIOLATION_SQLSTATE.equals(result.getStatusCode()))
			{
				throw new KommetException("Foreign key constraint violation [table " + result.getTableName() + "][constraint " + result.getConstraintName() + "]");
			}
			else if (UNKNOWN_TABLE_SQLSTATE.equals(result.getStatusCode()))
			{
				throw new KommetException("Unknown table [table " + result.getTableName() + "] in query: " + query);
			}
			else
			{
				throw new KommetException("Uncategorized exception: " + result.getStatusCode() + ". Query:\n" + query);
			}
		}
		else
		{
			throw new KommetException("SQL query " + query + " returned no return codes");
		}
	}
	
	/**
	 * Parses a unique check name, as generated by method {@link UniqueCheck#generateDbName}.
	 * @param uniqueCheckName
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private UniqueCheck parseFieldsFromUniqueCheck(String uniqueCheckName, EnvData env) throws KommetException
	{
		if (!StringUtils.hasText(uniqueCheckName))
		{
			throw new KommetException("Cannot extract fields from empty unique check name");
		}
		
		String[] bits = uniqueCheckName.split("_");
		
		if (bits.length == 0)
		{
			throw new KommetException("Unparseable unique check name " + uniqueCheckName);
		}
		
		Type type = env.getType(KID.get(bits[1]));
		if (type == null)
		{
			throw new KommetException("Type with ID " + bits[0] + " not found");
		}
		
		if (type.getUniqueChecks() != null)
		{
			for (UniqueCheck uc : type.getUniqueChecks())
			{
				if (uc.getDbName().equals(uniqueCheckName))
				{
					return uc;
				}
			}
		}
		else
		{
			throw new KommetException("Unique checks not initialized on type " + type.getQualifiedName());
		}
		
		// if this point is reached, unique check not found
		throw new KommetException("Unique check with DB name " + uniqueCheckName + " not found on type " + type.getQualifiedName());
	}

	class DbOperationResult
	{
		private String statusCode;
		private String constraintName;
		private String tableName;
		private String columnName;
		
		public DbOperationResult(String resultCodes)
		{
			List<String> parts = MiscUtils.splitAndTrim(resultCodes, "\\:\\:\\:\\:\\:");
			this.statusCode = parts.get(0);
			
			if (parts.size() > 1)
			{
				this.constraintName = parts.get(1);
				
				if (parts.size() > 2)
				{
					this.tableName = parts.get(2);
					
					if (parts.size() > 3)
					{
						this.columnName = parts.get(3);
					}
				}
			}
		}
		
		public String getStatusCode()
		{
			return statusCode;
		}
	
		public String getConstraintName()
		{
			return constraintName;
		}
		
		public String getColumnName()
		{
			return columnName;
		}
		
		public String getTableName()
		{
			return tableName;
		}
	}
}