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
import kommet.basic.ViewResource;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.ViewResourceFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ViewResourceDao extends GenericDaoImpl<ViewResource>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public ViewResourceDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * The default save method from GenericDao is overridden to pass skipTriggers and skipSharing parameters
	 * to the save method - we don't want triggers to be called or sharing added to SystemSettings.
	 */
	@Override
	public ViewResource save(ViewResource obj, AuthData authData, EnvData env) throws KommetException
	{
		return (ViewResource)getEnvCommunication().save(obj, true, true, authData, env);
	}
	
	public List<ViewResource> find (ViewResourceFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ViewResourceFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.VIEW_RESOURCE_API_NAME)).getKID());
		c.addProperty("path, mimeType, name");
		
		if (filter.isFetchContent())
		{
			c.addProperty("content");
		}
		
		c.addStandardSelectProperties();
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (StringUtils.hasText(filter.getMimeType()))
		{
			c.add(Restriction.eq("mimeType", filter.getMimeType()));
		}
		
		if (StringUtils.hasText(filter.getContentLike()))
		{
			c.add(Restriction.ilike("content", "%" + filter.getContentLike() + "%"));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<ViewResource> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<ViewResource> resources = new ArrayList<ViewResource>();
		
		for (Record r : records)
		{
			resources.add(new ViewResource(r, env));
		}
		
		return resources;
	}
}