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
import kommet.basic.App;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.AppFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class AppDao extends GenericDaoImpl<App>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public AppDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<App> get (AppFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new AppFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.APP_API_NAME)).getKID(), authData);
		c.addProperty("id, name, landingUrl, type, label");
		c.addStandardSelectProperties();
		
		if (filter.getAppIds() != null && !filter.getAppIds().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getAppIds()));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<App> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<App> tasks = new ArrayList<App>();
		
		for (Record r : records)
		{
			tasks.add(new App(r, env));
		}
		
		return tasks;
	}
}