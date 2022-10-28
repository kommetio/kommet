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
import kommet.basic.Library;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.LibraryFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class LibraryDao extends GenericDaoImpl<Library>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public LibraryDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<Library> find (LibraryFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new LibraryFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.LIBRARY_API_NAME)).getKID(), authData);
		c.addProperty("name, isEnabled, provider, version, description, status, accessLevel, source");
		c.addStandardSelectProperties();
		
		if (filter.isInitItems())
		{
			c.createAlias("items", "items");
			c.addProperty("items.id, items.apiName, items.recordId, items.componentType, items.definition, items.accessLevel");
		}
		
		if (filter.getLibIds() != null && !filter.getLibIds().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getLibIds()));
		}
		
		if (StringUtils.hasText(filter.getNameLike()))
		{
			c.add(Restriction.ilike("name", "%" + filter.getNameLike() + "%"));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (filter.getIsEnabled() != null)
		{
			c.add(Restriction.eq("isEnabled", filter.getIsEnabled()));
		}
			
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<Library> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<Library> libs = new ArrayList<Library>();
		
		for (Record r : records)
		{
			libs.add(new Library(r, env));
		}
		
		return libs;
	}
}