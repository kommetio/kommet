/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.emailing;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.basic.Email;
import kommet.basic.RecordProxyType;
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
public class EmailDao extends GenericDaoImpl<Email>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public EmailDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public List<Email> get (EmailFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new EmailFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.EMAIL_API_NAME)).getKID());
		c.addProperty("messageId, subject, plainTextBody, htmlBody, sender, recipients, ccRecipients, bccRecipients, status, sendDate");
		c.addStandardSelectProperties();
		
		if (filter.getEmailIds() != null && !filter.getEmailIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getEmailIds()));
		}
		
		if (filter.getMessageIds() != null && !filter.getMessageIds().isEmpty())
		{
			c.add(Restriction.in("messageId", filter.getMessageIds()));
		}
		
		if (filter.getSenders() != null && !filter.getSenders().isEmpty())
		{
			c.add(Restriction.in("sender", filter.getSenders()));
		}
		
		if (StringUtils.hasText(filter.getSubjectLike()))
		{
			c.add(Restriction.ilike("subject", "%" + filter.getSubjectLike() + "%"));
		}
		
		if (StringUtils.hasText(filter.getPlainTextBodyLike()))
		{
			c.add(Restriction.ilike("plainTextBody", "%" + filter.getPlainTextBodyLike() + "%"));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		List<Record> records = c.list();
		List<Email> emails = new ArrayList<Email>();
		for (Record r : records)
		{
			emails.add(new Email(r, env));
		}
		
		return emails;
	}
}