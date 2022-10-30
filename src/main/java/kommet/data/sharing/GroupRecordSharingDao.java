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

import kommet.basic.GroupRecordSharing;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class GroupRecordSharingDao extends GenericDaoImpl<GroupRecordSharing>
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
	
	public GroupRecordSharingDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<GroupRecordSharing> find (GroupRecordSharingFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new GroupRecordSharingFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.GROUP_RECORD_SHARING_API_NAME)).getKID());
		// do not retrieve the code field in the query because it's too large
		c.addProperty("recordId, group.id, isGeneric, reason, read, edit, delete, sharingRule.id");
		c.addStandardSelectProperties();
		
		if (filter.getRecordIds() != null && !filter.getRecordIds().isEmpty())
		{
			c.add(Restriction.in("recordId", filter.getRecordIds()));
		}
		
		if (filter.getGroupIds() != null && !filter.getGroupIds().isEmpty())
		{
			c.add(Restriction.in("group.id", filter.getGroupIds()));
		}
		
		if (filter.getReasons() != null && !filter.getReasons().isEmpty())
		{
			c.add(Restriction.in("reason", filter.getReasons()));
		}
		
		if (filter.getIsGeneric() != null)
		{
			c.add(Restriction.eq("isGeneric", filter.getIsGeneric()));
		}
		
		if (filter.getSharingRuleIds() != null && !filter.getSharingRuleIds().isEmpty())
		{
			c.add(Restriction.in("sharingRule.id", filter.getSharingRuleIds()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		if (filter.isExcludeSystemObjects())
		{
			// do not return sharing on some system objects
			c.add(Restriction.not(Restriction.ilike("recordId", "'" + KID.USER_GROUP_ASSIGNMENT_PREFIX + "%'")));
		}
		
		return getObjectStubList(c.list(), env);
	}
	
	private static List<GroupRecordSharing> getObjectStubList(List<Record> records, EnvData env) throws KommetException
	{
		List<GroupRecordSharing> sharings = new ArrayList<GroupRecordSharing>();
		
		for (Record r : records)
		{
			sharings.add(new GroupRecordSharing(r, env));
		}
		
		return sharings;
	}

	public boolean canEditRecord(KID recordId, KID groupId, EnvData env)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT EXISTS (SELECT id FROM obj_").append(KID.GROUP_RECORD_SHARING_PREFIX);
		sql.append(" WHERE usergroup = '").append(groupId).append("' AND recordid = '").append(recordId).append("' AND edit = true limit 1)");
		
		return (Boolean)env.getJdbcTemplate().queryForObject(sql.toString(), Boolean.class);
	}
	
	public boolean canViewRecord(KID recordId, KID groupId, EnvData env)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT EXISTS (SELECT id FROM obj_").append(KID.GROUP_RECORD_SHARING_PREFIX);
		sql.append(" WHERE usergroup = '").append(groupId).append("' AND recordid = '").append(recordId).append("' AND read = true limit 1)");
		
		return (Boolean)env.getJdbcTemplate().queryForObject(sql.toString(), Boolean.class);
	}
}