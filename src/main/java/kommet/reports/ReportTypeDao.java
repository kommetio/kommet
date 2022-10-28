/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.reports;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.RecordProxyType;
import kommet.basic.ReportType;
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
public class ReportTypeDao extends GenericDaoImpl<ReportType>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public ReportTypeDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public List<ReportType> get (ReportTypeFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ReportTypeFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.REPORT_TYPE_API_NAME)).getKID(), authData);
		c.addProperty("id, serializedQuery, baseTypeId, name, description");
		c.addStandardSelectProperties();
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (filter.getBaseTypeIds() != null && !filter.getBaseTypeIds().isEmpty())
		{
			c.add(Restriction.in("baseTypeId", filter.getBaseTypeIds()));
		}
		
		if (filter.getReportTypeIds() != null && !filter.getReportTypeIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getReportTypeIds()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		List<Record> records = c.list();
		List<ReportType> rts = new ArrayList<ReportType>();
		for (Record r : records)
		{
			rts.add(new ReportType(r, env));
		}
		
		return rts;
	}
}