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
import kommet.basic.WebResource;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.WebResourceFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class WebResourceDao extends GenericDaoImpl<WebResource>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public WebResourceDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * The default save method from GenericDao is overridden to pass skipTriggers and skipSharing parameters
	 * to the save method - we don't want triggers to be called or sharing added to SystemSettings.
	 */
	@Override
	public WebResource save(WebResource obj, AuthData authData, EnvData env) throws KommetException
	{
		return (WebResource)getEnvCommunication().save(obj, true, true, authData, env);
	}
	
	public List<WebResource> find (WebResourceFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new WebResourceFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.WEB_RESOURCE_API_NAME)).getKID());
		c.addProperty("file.id, file.name, mimeType, name");
		c.addStandardSelectProperties();
		c.createAlias("file", "file");
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (StringUtils.hasText(filter.getMimeType()))
		{
			c.add(Restriction.eq("mimeType", filter.getMimeType()));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<WebResource> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<WebResource> resources = new ArrayList<WebResource>();
		
		for (Record r : records)
		{
			resources.add(new WebResource(r, env));
		}
		
		return resources;
	}
}