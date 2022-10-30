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

import kommet.basic.RecordProxyType;
import kommet.basic.User;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.UserFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class UserDao extends GenericDaoImpl<User>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public UserDao()
	{
		super(RecordProxyType.STANDARD);
	}

	public List<User> get (UserFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new UserFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_API_NAME)).getKID());
		c.addProperty("userName, email, firstName, lastName, middleName, title, timezone, activationHash, forgottenPasswordHash, isActive, profile.id, profile.name, locale");
		c.addStandardSelectProperties();
		c.createAlias("profile", "profile");
		
		if (StringUtils.hasText(filter.getUsername()))
		{
			c.add(Restriction.eq("userName", filter.getUsername()));
		}
		
		if (StringUtils.hasText(filter.getRememberMeToken()))
		{
			c.add(Restriction.eq("rememberMeToken", filter.getRememberMeToken()));
		}
		
		if (StringUtils.hasText(filter.getEncryptedPassword()))
		{
			c.add(Restriction.eq("password", filter.getEncryptedPassword()));
		}
		
		if (StringUtils.hasText(filter.getEmail()))
		{
			c.add(Restriction.eq("email", filter.getEmail()));
		}
		
		if (StringUtils.hasText(filter.getActivationHash()))
		{
			c.add(Restriction.eq("activationHash", filter.getActivationHash()));
		}
		
		if (StringUtils.hasText(filter.getForgottenPasswordHash()))
		{
			c.add(Restriction.eq("forgottenPasswordHash", filter.getForgottenPasswordHash()));
		}
		
		if (filter.getUserIds() != null && !filter.getUserIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getUserIds()));
		}
		
		if (filter.getProfileIds() != null && !filter.getProfileIds().isEmpty())
		{
			c.add(Restriction.in("profile.id", filter.getProfileIds()));
		}
		
		if (filter.getIsActive() != null)
		{
			c.add(Restriction.eq("isActive", filter.getIsActive()));
		}
		
		List<Record> records = c.list();
		List<User> users = new ArrayList<User>();
		for (Record r : records)
		{
			users.add(new User(r, env));
		}
		return users;
	}
}