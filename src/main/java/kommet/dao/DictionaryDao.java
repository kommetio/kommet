/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.auth.AuthData;
import kommet.basic.Dictionary;
import kommet.basic.DictionaryItem;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.DictionaryFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class DictionaryDao extends GenericDaoImpl<Dictionary>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public DictionaryDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<Dictionary> get (DictionaryFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new DictionaryFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.DICTIONARY_API_NAME)).getKID(), authData);
		c.addProperty("name, items.id, items.name, items.key, items.index");
		c.addStandardSelectProperties();
		c.createAlias("items", "items");
		
		if (filter.getDictionaryIds() != null && !filter.getDictionaryIds().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getDictionaryIds()));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<Dictionary> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<Dictionary> dictionaries = new ArrayList<Dictionary>();
		
		for (Record r : records)
		{
			dictionaries.add(sortDictionaryItems(new Dictionary(r, env)));
		}
		
		return dictionaries;
	}

	private static Dictionary sortDictionaryItems(Dictionary dictionary) 
	{
		if (dictionary.getItems() != null)
		{
			Collections.sort(dictionary.getItems(), new Comparator<DictionaryItem>() {
			    @Override
			    public int compare(DictionaryItem o1, DictionaryItem o2) {
			        return o1.getIndex().compareTo(o2.getIndex());
			    }
			});
		}
		
		return dictionary;
	}

}