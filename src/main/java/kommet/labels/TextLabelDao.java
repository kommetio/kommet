/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.labels;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.basic.RecordProxyType;
import kommet.basic.TextLabel;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class TextLabelDao extends GenericDaoImpl<TextLabel>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public TextLabelDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public List<TextLabel> get (TextLabelFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new TextLabelFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.TEXT_LABEL_API_NAME)).getKID());
		c.addProperty("id, key, value, locale");
		c.addStandardSelectProperties();
		
		if (filter.getKeys() != null && !filter.getKeys().isEmpty())
		{
			c.add(Restriction.in("key", filter.getKeys()));
		}
		
		if (filter.getLocale() != null)
		{
			c.add(Restriction.eq("locale", filter.getLocale().name()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		List<Record> records = c.list();
		List<TextLabel> labels = new ArrayList<TextLabel>();
		for (Record r : records)
		{
			labels.add(new TextLabel(r, env));
		}
		
		return labels;
	}
}