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
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Action;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.RestrictionOperator;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.integration.PropertySelection;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ActionDao extends GenericDaoImpl<Action>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public ActionDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * The default save method from GenericDao is overridden to pass skipTriggers and skipSharing parameters
	 * to the save method - we don't want triggers to be called or sharing added to page.
	 */
	public Action save(Action obj, boolean skipTriggers, boolean skipSharing, AuthData authData, EnvData env) throws KommetException
	{
		return (Action)getEnvCommunication().save(obj, skipTriggers, skipSharing, authData, env);
	}
	
	public Action get (KID id, EnvData env) throws KommetException
	{
		Action action = get(id, PropertySelection.SPECIFIED_AND_BASIC, "isSystem, isPublic, url, name, controller.id, controller.name, controller.packageName, controllerMethod, view.name, view.path, view.id, view.typeId, view.name, view.packageName, view.layout.id, typeId", env);
		if (action != null && action.getTypeId() != null)
		{
			action.setType(env.getType(action.getTypeId()));
			if (action.getView() != null && action.getView().getTypeId() != null)
			{
				action.getView().setType(env.getType(action.getView().getTypeId()));
			}
		}
		return action;
	}
	
	public List<Action> find (ActionFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ActionFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME)).getKID());
		c.addProperty("id, createdDate, isSystem, isPublic, url, name, controller.id, controller.name, controller.packageName, controllerMethod, view.name, view.path, view.id, view.layout.id, typeId");
		c.createAlias("controller", "controller");
		c.createAlias("view", "view");
		c.createAlias("view.layout", "layout");
		
		if (StringUtils.hasText(filter.getUrl()))
		{
			c.add(Restriction.eq("url", filter.getUrl()));
		}
		
		if (filter.getKID() != null)
		{
			c.add(Restriction.eq("id", filter.getKID()));
		}
		
		if (filter.getControllerId() != null)
		{
			c.add(Restriction.eq("controller.id", filter.getControllerId()));
		}
		
		if (filter.getIsSystem() != null)
		{
			c.add(Restriction.eq("isSystem", filter.getIsSystem()));
		}
		
		if (StringUtils.hasText(filter.getNameLike()))
		{
			c.add(Restriction.ilike("name", "%" + filter.getNameLike() + "%"));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (StringUtils.hasText(filter.getNameOrUrl()))
		{
			Restriction nameOrUrlRestriction = new Restriction();
			nameOrUrlRestriction.setOperator(RestrictionOperator.OR);
			nameOrUrlRestriction.addSubrestriction(Restriction.ilike("name", "%" + filter.getNameOrUrl() + "%"));
			nameOrUrlRestriction.addSubrestriction(Restriction.ilike("url", "%" + filter.getNameOrUrl() + "%"));
			c.add(nameOrUrlRestriction);
		}
		
		c = filter.applySortAndLimit(c);
		
		List<Record> records = c.list();
		List<Action> actions = new ArrayList<Action>();
		for (Record r : records)
		{
			actions.add(new Action(r, env));
		}
		return actions;
	}
}
