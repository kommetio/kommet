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

import kommet.basic.FieldHistory;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.FieldHistoryFilter;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class FieldHistoryDao extends GenericDaoImpl<FieldHistory>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public FieldHistoryDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public List<FieldHistory> find (FieldHistoryFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new FieldHistoryFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.FIELD_HISTORY_API_NAME)).getKID());
		c.addProperty("fieldId, recordId, oldValue, newValue, operation");
		c.addStandardSelectProperties();
		
		if (filter.getRecordIds() != null && !filter.getRecordIds().isEmpty())
		{
			c.add(Restriction.in("recordId", filter.getRecordIds()));
		}
		
		if (filter.getFieldIds() != null && !filter.getFieldIds().isEmpty())
		{
			c.add(Restriction.in("fieldId", filter.getFieldIds()));
		}
		
		if (filter.getUserIds() != null && !filter.getUserIds().isEmpty())
		{
			c.add(Restriction.in(Field.CREATEDBY_FIELD_NAME, filter.getRecordIds()));
		}
		
		if (filter.getDateFrom() != null)
		{
			c.add(Restriction.ge(Field.CREATEDDATE_FIELD_NAME, filter.getDateFrom()));
		}
		
		if (filter.getDateTo() != null)
		{
			c.add(Restriction.le(Field.CREATEDDATE_FIELD_NAME, filter.getDateTo()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<FieldHistory> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<FieldHistory> fhs = new ArrayList<FieldHistory>();
		
		for (Record r : records)
		{
			fhs.add(new FieldHistory(r, env));
		}
		
		return fhs;
	}
}