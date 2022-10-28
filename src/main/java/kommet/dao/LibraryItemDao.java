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
import kommet.basic.LibraryItem;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.LibraryItemFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class LibraryItemDao extends GenericDaoImpl<LibraryItem>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public LibraryItemDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<LibraryItem> find (LibraryItemFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new LibraryItemFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.LIBRARY_ITEM_API_NAME)).getKID(), authData);
		c.addProperty("apiName, recordId, library.id, definition, accessLevel, componentType");
		c.addStandardSelectProperties();
		
		if (StringUtils.hasText(filter.getApiName()))
		{
			c.add(Restriction.eq("apiName", filter.getApiName()));
		}
		
		if (filter.getRecordId() != null)
		{
			c.add(Restriction.eq("recordId", filter.getRecordId()));
		}
		
		if (filter.getLibraryId() != null)
		{
			c.add(Restriction.eq("library.id", filter.getLibraryId()));
		}
			
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<LibraryItem> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<LibraryItem> items = new ArrayList<LibraryItem>();
		
		for (Record r : records)
		{
			items.add(new LibraryItem(r, env));
		}
		
		return items;
	}
}