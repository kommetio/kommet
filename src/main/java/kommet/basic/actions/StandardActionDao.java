/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.actions;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.basic.RecordProxyType;
import kommet.basic.StandardAction;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class StandardActionDao extends GenericDaoImpl<StandardAction>
{	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public static final String STANDARD_ACTION_SELECT_FIELDS = "id, type, profile.id, profile.name, typeId, action.id, action.url, action.name, action.controller.id, action.controller.isSystem, action.view.id, action.typeId, action.view.name, action.view.typeId, action.view.isSystem";
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public StandardActionDao()
	{
		super(RecordProxyType.STANDARD);
	}

	public List<StandardAction> find(StandardActionFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new StandardActionFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.STANDARD_ACTION_API_NAME)).getKID());
		c.addProperty(STANDARD_ACTION_SELECT_FIELDS);
		c.addStandardSelectProperties();
		c.createAlias("action", "action");
		c.createAlias("profile", "profile");
		c.createAlias("action.controller", "action.controller");
		c.createAlias("action.view", "action.view");
		
		if (filter.getIds() != null && !filter.getIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getIds()));
		}
		
		if (filter.getPageIds() != null && !filter.getPageIds().isEmpty())
		{
			c.add(Restriction.in("action.id", filter.getPageIds()));
		}
		
		if (filter.getPageType() != null)
		{
			c.add(Restriction.eq("type", filter.getPageType().getStringValue()));
		}
		
		if (filter.getProfileId() != null)
		{
			c.createAlias("profile", "profile");
			c.add(Restriction.eq("profile.id", filter.getProfileId()));
		}
		
		if (filter.getTypeId() != null)
		{
			c.add(Restriction.eq("typeId", filter.getTypeId()));
		}
		
		List<Record> records = c.list();
		List<StandardAction> pages = new ArrayList<StandardAction>();
		for (Record r : records)
		{
			pages.add(new StandardAction(r, env));
		}
		return pages;
		
	}
}
