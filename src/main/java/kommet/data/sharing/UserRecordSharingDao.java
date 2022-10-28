/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.sharing;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.basic.RecordProxyType;
import kommet.basic.UserRecordSharing;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class UserRecordSharingDao extends GenericDaoImpl<UserRecordSharing>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public UserRecordSharingDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<UserRecordSharing> find (UserRecordSharingFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new UserRecordSharingFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_RECORD_SHARING_API_NAME)).getKID());
		// do not retrieve the code field in the query because it's too large
		c.addProperty("recordId, user.id, isGeneric, reason, read, edit, delete, userGroupAssignmentId, groupRecordSharingId, groupSharingHierarchy, sharingRule.id");
		c.addStandardSelectProperties();
		
		if (filter.isInitUser())
		{
			c.addProperty("user.userName, user.email");
			c.createAlias("user", "user");
		}
		
		if (filter.getRecordIds() != null && !filter.getRecordIds().isEmpty())
		{
			c.add(Restriction.in("recordId", filter.getRecordIds()));
		}
		
		if (filter.getUserIds() != null && !filter.getUserIds().isEmpty())
		{
			c.add(Restriction.in("user.id", filter.getUserIds()));
		}
		
		if (filter.getSharingRuleIds() != null && !filter.getSharingRuleIds().isEmpty())
		{
			c.add(Restriction.in("sharingRule.id", filter.getSharingRuleIds()));
		}
		
		if (filter.getGroupRecordSharingIds() != null && !filter.getGroupRecordSharingIds().isEmpty())
		{
			c.add(Restriction.in("groupRecordSharingId", filter.getGroupRecordSharingIds()));
		}
		
		if (filter.getUserGroupAssignmentIds() != null && !filter.getUserGroupAssignmentIds().isEmpty())
		{
			c.add(Restriction.in("userGroupAssignmentId", filter.getUserGroupAssignmentIds()));
		}
		
		if (StringUtils.hasText(filter.getGroupSharingHierarchy()))
		{
			c.add(Restriction.ilike("groupSharingHierarchy", "%" + filter.getGroupSharingHierarchy() + "%"));
		}
		else if (filter.getEmptyGroupSharingHierarchy())
		{
			c.add(Restriction.isNull("groupSharingHierarchy"));
		}
		
		if (filter.getReasons() != null && !filter.getReasons().isEmpty())
		{
			c.add(Restriction.in("reason", filter.getReasons()));
		}
		
		if (filter.getIsGeneric() != null)
		{
			c.add(Restriction.eq("isGeneric", filter.getIsGeneric()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		return getObjectStubList(c.list(), env);
	}
	
	private static List<UserRecordSharing> getObjectStubList(List<Record> records, EnvData env) throws KommetException
	{
		List<UserRecordSharing> sharings = new ArrayList<UserRecordSharing>();
		
		for (Record r : records)
		{
			sharings.add(new UserRecordSharing(r, env));
		}
		
		return sharings;
	}
	
	public boolean canPerformActionOnRecord(KID recordId, KID userId, String action, EnvData env) throws KommetException
	{		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT EXISTS (SELECT id FROM obj_").append(KID.USER_RECORD_SHARING_PREFIX);
		sql.append(" WHERE assigneduser = '").append(userId).append("' AND ");
		
		Type type = env.getTypeByRecordId(recordId); 
		if (type.getSharingControlledByFieldId() == null)
		{
			sql.append(" recordid = '").append(recordId).append("'");
		}
		else
		{
			if (type.isCombineRecordAndCascadeSharing())
			{
				sql.append(" (recordid = '").append(recordId).append("' OR recordid = (");
				sql.append("SELECT " + type.getSharingControlledByField().getDbColumn()).append(" FROM ").append(type.getDbTable()).append(" WHERE ").append(Field.ID_FIELD_DB_COLUMN);
				sql.append(" = '").append(recordId).append("'");
				sql.append("))");
			}
			else
			{
				sql.append("recordid = (SELECT " + type.getSharingControlledByField().getDbColumn()).append(" FROM ").append(type.getDbTable()).append(" WHERE ").append(Field.ID_FIELD_DB_COLUMN);
				sql.append(" = '").append(recordId).append("')");
			}
		}
		
		sql.append(" AND ").append(action).append(" = true limit 1)");
		
		return (Boolean)env.getJdbcTemplate().queryForObject(sql.toString(), Boolean.class);
	}

	public boolean canEditRecord(KID recordId, KID userId, EnvData env) throws KommetException
	{		
		return canPerformActionOnRecord(recordId, userId, "edit", env);
	}
	
	public boolean canDeleteRecord(KID recordId, KID userId, EnvData env) throws KommetException
	{		
		return canPerformActionOnRecord(recordId, userId, "delete", env);
	}
	
	public boolean canViewRecord(KID recordId, KID userId, EnvData env) throws KommetException
	{	
		return canPerformActionOnRecord(recordId, userId, "read", env);
	}

	public void deleteSharingsForHierarchy (String id, EnvData env) throws KommetException
	{
		String query = "DELETE FROM " + env.getTypeMapping(env.getType(KeyPrefix.get(KID.USER_RECORD_SHARING_PREFIX)).getKID()).getTable() + " where groupsharinghierarchy like '%" + id + "%'";
		env.getJdbcTemplate().execute(query);
	}
	
	public int deleteSharingsForHierarchy (String id, int limit, EnvData env) throws KommetException
	{
		String dbTable = env.getTypeMapping(env.getType(KeyPrefix.get(KID.USER_RECORD_SHARING_PREFIX)).getKID()).getTable();
		String query = "DELETE FROM " + dbTable + " WHERE kid IN (SELECT kid from " + dbTable + " WHERE groupsharinghierarchy like '%" + id + "%' LIMIT " + limit + ")";
		env.getJdbcTemplate().execute(query);
		
		// check how many sharings remain
		return env.getJdbcTemplate().queryForObject("SELECT count(kid) FROM " + dbTable + " WHERE groupsharinghierarchy like '%" + id + "%'", Integer.class);
	}
}