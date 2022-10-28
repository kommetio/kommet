/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.errorlog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.basic.ErrorLog;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ErrorLogDao extends GenericDaoImpl<ErrorLog>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public ErrorLogDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public List<ErrorLog> get (ErrorLogFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ErrorLogFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ERROR_LOG_API_NAME)).getKID());
		c.addProperty("message, details, affectedUser.id, affectedUser.userName, severity, codeLine, codeClass");
		c.addStandardSelectProperties();
		c.createAlias("affectedUser", "affectedUser");
		
		if (filter.getErrorLogIds() != null && !filter.getErrorLogIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getErrorLogIds()));
		}
		
		if (filter.getUserIds() != null && !filter.getUserIds().isEmpty())
		{
			c.add(Restriction.in("affectedUser.id", filter.getUserIds()));
		}
		
		if (filter.getSeverities() != null && !filter.getSeverities().isEmpty())
		{
			c.add(Restriction.in("severity", filter.getSeverities()));
		}
		
		if (filter.getDateFrom() != null)
		{
			c.add(Restriction.ge("createdDate", filter.getDateFrom()));
		}
		
		if (filter.getDateTo() != null)
		{
			c.add(Restriction.le("createdDate", filter.getDateTo()));
		}
		
		c = filter.applySortAndLimit(c);
		
		List<Record> records = c.list();
		List<ErrorLog> logs = new ArrayList<ErrorLog>();
		for (Record r : records)
		{
			logs.add(new ErrorLog(r, env));
		}
		
		return logs;
	}
}