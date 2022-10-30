/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.basic.Layout;
import kommet.basic.RecordAccessType;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class LayoutDao extends GenericDaoImpl<Layout>
{
	@Inject
	DataService typeService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public LayoutDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<Layout> find (LayoutFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new LayoutFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.LAYOUT_API_NAME)).getKID());
		// do not retrieve the code field in the query because it's too large
		c.addProperty("name, code");
		c.addStandardSelectProperties();
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (filter.getIsSystem() != null)
		{
			c.add(Restriction.eq(Field.ACCESS_TYPE_FIELD_NAME, RecordAccessType.PUBLIC.getId()));
		}
		
		return getObjectStubList(c.list(), env);
	}
	
	public Layout getByName (String name, EnvData env) throws KommetException
	{
		if (!StringUtils.hasText(name))
		{
			throw new KommetException("Cannot search layout by empty name");
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.LAYOUT_API_NAME)).getKID());
		// do not retrieve the code field in the query because it's too large
		c.addProperty("name, code");
		c.addStandardSelectProperties();
		
		c.add(Restriction.eq("name", name));
		
		List<Record> layouts = c.list();
		if (layouts.size() > 1)
		{
			throw new KommetException("More than one layout found with name " + name);
		}
		else if (layouts.isEmpty())
		{
			return null;
		}
		else
		{
			return new Layout(layouts.get(0), env);
		}
	}
	
	private static List<Layout> getObjectStubList(List<Record> records, EnvData env) throws KommetException
	{
		List<Layout> views = new ArrayList<Layout>();
		
		for (Record r : records)
		{
			views.add(new Layout(r, env));
		}
		
		return views;
	}
}