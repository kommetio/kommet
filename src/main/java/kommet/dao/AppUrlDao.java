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

import kommet.basic.AppUrl;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.AppUrlFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class AppUrlDao extends GenericDaoImpl<AppUrl>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public AppUrlDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<AppUrl> find (AppUrlFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new AppUrlFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.APP_URL_API_NAME)).getKID());
		c.addProperty("url, app.id, app.name");
		c.addStandardSelectProperties();
		c.createAlias("app", "app");
		
		if (filter.getUrls() != null && !filter.getUrls().isEmpty())
		{
			c.add(Restriction.in("url", filter.getUrls()));
		}
		
		if (filter.getAppUrlIds() != null && !filter.getAppUrlIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getAppUrlIds()));
		}
		
		if (filter.getAppIds() != null && !filter.getAppIds().isEmpty())
		{
			c.add(Restriction.in("app.id", filter.getAppIds()));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<AppUrl> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<AppUrl> appUrls = new ArrayList<AppUrl>();
		
		for (Record r : records)
		{
			appUrls.add(new AppUrl(r, env));
		}
		
		return appUrls;
	}
}