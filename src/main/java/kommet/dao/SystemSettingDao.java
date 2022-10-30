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
import kommet.basic.SystemSetting;
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
public class SystemSettingDao extends GenericDaoImpl<SystemSetting>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public SystemSettingDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * The default save method from GenericDao is overridden to pass skipTriggers and skipSharing parameters
	 * to the save method - we don't want triggers to be called or sharing added to SystemSettings.
	 */
	@Override
	public SystemSetting save(SystemSetting obj, AuthData authData, EnvData env) throws KommetException
	{
		return (SystemSetting)getEnvCommunication().save(obj, true, true, authData, env);
	}
	
	public List<SystemSetting> find (SystemSettingFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new SystemSettingFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.SYSTEM_SETTING_API_NAME)).getKID());
		c.addProperty("key, value");
		c.addStandardSelectProperties();
		
		if (filter.getKeys() != null && !filter.getKeys().isEmpty())
		{
			c.add(Restriction.in("key", filter.getKeys()));
		}
		
		return getObjectStubList(c.list(), env);
	}
	
	private static List<SystemSetting> getObjectStubList(List<Record> records, EnvData env) throws KommetException
	{
		List<SystemSetting> settings = new ArrayList<SystemSetting>();
		
		for (Record r : records)
		{
			settings.add(new SystemSetting(r, env));
		}
		
		return settings;
	}
}