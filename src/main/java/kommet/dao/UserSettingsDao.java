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

import kommet.basic.RecordProxyType;
import kommet.basic.UserSettings;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.UserSettingsFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class UserSettingsDao extends GenericDaoImpl<UserSettings>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public UserSettingsDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<UserSettings> get (UserSettingsFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new UserSettingsFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_SETTINGS_API_NAME)).getKID());
		c.addProperty("user.id, profile.id, layout.id, layout.name, landingURL");
		c.addStandardSelectProperties();
		c.createAlias("profile", "profile");
		c.createAlias("layout", "layout");
		c.createAlias("user", "user");
		
		if (filter.isUserOrProfile())
		{
			c.add(Restriction.or(Restriction.in("user.id", filter.getUserIds()), Restriction.in("profile.id", filter.getProfileIds())));
		}
		else
		{
			if (filter.getUserIds() != null)
			{
				c.add(Restriction.in("user.id", filter.getUserIds()));
			}
			
			if (filter.getProfileIds() != null)
			{
				c.add(Restriction.in("profile.id", filter.getProfileIds()));
			}
		}
		
		List<Record> records = c.list();
		List<UserSettings> settings = new ArrayList<UserSettings>();
		for (Record r : records)
		{
			settings.add(new UserSettings(r, env));
		}
		return settings;
	}
}