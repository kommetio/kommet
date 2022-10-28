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
import kommet.basic.SettingValue;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.uch.UserCascadeHierarchyDao;

@Repository
public class SettingValueDao extends GenericDaoImpl<SettingValue>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public SettingValueDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * The default save method from GenericDao is overridden to pass skipTriggers and skipSharing parameters
	 * to the save method - we don't want triggers to be called or sharing added to SystemSettings.
	 */
	@Override
	public SettingValue save(SettingValue obj, AuthData authData, EnvData env) throws KommetException
	{
		return (SettingValue)getEnvCommunication().save(obj, true, true, authData, env);
	}
	
	public List<SettingValue> get (SettingValueFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new SettingValueFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.SETTING_VALUE_API_NAME)).getKID(), authData);
		c.addProperty("key, value");
		c.createAlias("hierarchy", "hierarchy");
		c.createAlias("hierarchy.contextUser", "hierarchy_contextuser");
		c.createAlias("hierarchy.profile", "hierarchy_profile");
		c.createAlias("hierarchy.userGroup", "hierarchy_usergroup");
		
		// query all fields from the related UCH
		for (String hierarchyField : UserCascadeHierarchyDao.basicUchProperties)
		{
			c.addProperty("hierarchy." + hierarchyField);
		}
		
		if (filter.isFetchAllUchFields())
		{
			for (String hierarchyField : UserCascadeHierarchyDao.additionalUchProperties)
			{
				c.addProperty("hierarchy." + hierarchyField);
			}
		}
		
		c.addStandardSelectProperties();
		
		if (filter.getKeys() != null && !filter.getKeys().isEmpty())
		{
			c.add(Restriction.in("key", filter.getKeys()));
		}
		
		if (filter.getValues() != null && !filter.getValues().isEmpty())
		{
			c.add(Restriction.in("value", filter.getValues()));
		}
		
		if (filter.getContext() != null)
		{
			c.add(Restriction.eq("hierarchy.activeContextName", filter.getContext().toString()));
		}
		
		if (filter.getContextValue() != null)
		{
			setContextCondition(c, filter);
		}
		
		if (filter.getKIDs() != null && !filter.getKIDs().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getKIDs()));
		}
		
		return getObjectStubList(c.list(), env);
	}
	
	private void setContextCondition(Criteria c, SettingValueFilter filter) throws KommetException
	{
		if (filter.getContext() == null)
		{
			throw new KommetException("Cannot search by context value when active context is not set on the filter");
		}
		
		if (filter.getContext().equals(UserCascadeHierarchyContext.ENVIRONMENT))
		{
			c.add(Restriction.eq("hierarchy.env", ((Boolean)filter.getContextValue())));
		}
		else if (filter.getContext().equals(UserCascadeHierarchyContext.PROFILE))
		{
			c.add(Restriction.eq("hierarchy.profile.id", ((KID)filter.getContextValue())));
		}
		else if (filter.getContext().equals(UserCascadeHierarchyContext.USER))
		{
			c.add(Restriction.eq("hierarchy.contextUser.id", ((KID)filter.getContextValue())));
		}
		else if (filter.getContext().equals(UserCascadeHierarchyContext.USER_GROUP))
		{
			c.add(Restriction.eq("hierarchy.userGroup.id", ((KID)filter.getContextValue())));
		}
		else if (filter.getContext().equals(UserCascadeHierarchyContext.LOCALE))
		{
			c.add(Restriction.eq("hierarchy.localeName", ((Locale)filter.getContextValue()).name()));
		}
		else
		{
			throw new KommetException("Unsupported context " + filter.getContext().name());
		}
	}

	private static List<SettingValue> getObjectStubList(List<Record> records, EnvData env) throws KommetException
	{
		List<SettingValue> settings = new ArrayList<SettingValue>();
		
		for (Record r : records)
		{
			settings.add(new SettingValue(r, env));
		}
		
		return settings;
	}
}