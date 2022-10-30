/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.auth.AuthData;
import kommet.basic.RecordProxyType;
import kommet.basic.UserGroupAssignment;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class UserGroupAssignmentDao extends GenericDaoImpl<UserGroupAssignment>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public UserGroupAssignmentDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<UserGroupAssignment> get (UserGroupAssignmentFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new UserGroupAssignmentFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_GROUP_ASSIGNMENT_API_NAME)).getKID(), authData);
		c.addProperty("parentGroup.id, childGroup.id, childUser.id");
		c.createAlias("parentGroup", "parentGroup");
		c.createAlias("childUser", "childUser");
		c.createAlias("childGroup", "childGroup");
		c.addStandardSelectProperties();
		
		if (filter.isFetchExtendedData())
		{
			// fetch additional fields
			c.addProperty("childUser.userName, childUser.email, childGroup.name");
		}
		
		if (filter.isApplyPending() != null)
		{
			c.add(Restriction.eq("isApplyPending", filter.isApplyPending()));
		}
		
		if (filter.isRemovePending() != null)
		{
			c.add(Restriction.eq("isRemovePending", filter.isRemovePending()));
		}
		
		if (filter.getParentGroupIds() != null && !filter.getParentGroupIds().isEmpty())
		{
			c.add(Restriction.in("parentGroup.id", filter.getParentGroupIds()));
		}
		
		if (filter.getChildGroupIds() != null && !filter.getChildGroupIds().isEmpty())
		{
			c.add(Restriction.in("childGroup.id", filter.getChildGroupIds()));
		}
		
		if (filter.getChildUserIds() != null && !filter.getChildUserIds().isEmpty())
		{
			c.add(Restriction.in("childUser.id", filter.getChildUserIds()));
		}
		
		// convert and return results
		List<Record> records = c.list();
		List<UserGroupAssignment> templates = new ArrayList<UserGroupAssignment>();
		for (Record r : records)
		{
			templates.add(new UserGroupAssignment(r, env));
		}
		return templates;
	}

	public boolean isUserInGroup(KID userId, KID groupId, EnvData env)
	{
		return env.getJdbcTemplate().queryForObject("select case when exists (select groupid from get_user_groups('" + userId + "') where groupid = '" + groupId + "') then true else false end;", Boolean.class);
	}
	
	public boolean isGroupInGroup(KID childGroupId, KID parentGroupId, EnvData env)
	{
		return env.getJdbcTemplate().queryForObject("select case when exists (select groupid from get_parent_groups('" + childGroupId + "') where groupid = '" + parentGroupId + "') then true else false end;", Boolean.class);
	}
}