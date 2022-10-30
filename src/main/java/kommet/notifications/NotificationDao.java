/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.notifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.auth.AuthData;
import kommet.basic.Notification;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class NotificationDao extends GenericDaoImpl<Notification>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public NotificationDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}

	public Notification get (KID id, Collection<String> additionalQueriedProperties, AuthData authData, EnvData env) throws KommetException
	{
		NotificationFilter filter = new NotificationFilter();
		filter.addNotificationId(id);
		List<Notification> notifications = get(filter, additionalQueriedProperties, authData, env);
		return notifications.isEmpty() ? null : notifications.get(0);
	}
	
	public List<Notification> get (NotificationFilter filter, Collection<String> additionalQueriedProperties, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new NotificationFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.NOTIFICATION_API_NAME)).getKID(), authData);
		c.addProperty("text, assignee.id, title, viewedDate");
		
		if (additionalQueriedProperties != null)
		{
			Iterator<String> iterator = additionalQueriedProperties.iterator();
			while (iterator.hasNext())
			{
				c.addProperty(iterator.next());
			}
		}
		
		c.addStandardSelectProperties();
		c.createAlias("assignee", "assignee");
		
		if (filter.getAssigneeIds() != null && !filter.getAssigneeIds().isEmpty())
		{
			c.add(Restriction.in("assignee.id", filter.getAssigneeIds()));
		}
		
		if (filter.getNotificationIds() != null && !filter.getNotificationIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getNotificationIds()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		List<Record> records = c.list();
		List<Notification> notifications = new ArrayList<Notification>();
		for (Record r : records)
		{
			notifications.add(new Notification(r, env));
		}
		
		return notifications;
	}
}