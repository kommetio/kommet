/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.basic.LoginHistory;
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
public class LoginHistoryDao extends GenericDaoImpl<LoginHistory>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public LoginHistoryDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public List<LoginHistory> get (LoginHistoryFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new LoginHistoryFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.LOGIN_HISTORY_API_NAME)).getKID());
		c.addProperty("loginUser, ip4Address, ip6Address, method, result");
		c.addStandardSelectProperties();
		c.createAlias("loginUser", "loginUser");
		
		if (filter.getLoginHistoryIds() != null && !filter.getLoginHistoryIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getLoginHistoryIds()));
		}
		
		if (filter.getUserIds() != null && !filter.getUserIds().isEmpty())
		{
			c.add(Restriction.in("loginUser.id", filter.getUserIds()));
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
		List<LoginHistory> loginHistoryItems = new ArrayList<LoginHistory>();
		for (Record r : records)
		{
			loginHistoryItems.add(new LoginHistory(r, env));
		}
		
		return loginHistoryItems;
	}
}
