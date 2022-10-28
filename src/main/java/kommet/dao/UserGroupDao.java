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
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.RecordProxyType;
import kommet.basic.UserGroup;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class UserGroupDao extends GenericDaoImpl<UserGroup>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public UserGroupDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<UserGroup> get (UserGroupFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new UserGroupFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_GROUP_API_NAME)).getKID(), authData);
		c.addProperty("name, description");
		c.addStandardSelectProperties();
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (filter.getUserGroupIds() != null && !filter.getUserGroupIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getUserGroupIds()));
		}
		
		if (filter.isInitSubgroups())
		{
			c.createAlias("subgroups", "subgroups");
			c.addProperty("subgroups.id, subgroups.name");
		}
		
		if (filter.isInitUsers())
		{
			c.createAlias("users", "users");
			c.addProperty("users.id, users.userName");
		}
		
		// convert and return results
		List<Record> records = c.list();
		List<UserGroup> templates = new ArrayList<UserGroup>();
		for (Record r : records)
		{
			templates.add(new UserGroup(r, env));
		}
		return templates;
	}
}