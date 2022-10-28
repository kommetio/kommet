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

import kommet.basic.ActionPermission;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.permissions.ActionPermissionFilter;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ActionPermissionDao extends GenericDaoImpl<ActionPermission>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public ActionPermissionDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * Returns page permissions for a given profile.
	 * @param profileId
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public List<ActionPermission> getForProfile(KID profileId, EnvData env) throws KommetException
	{
		ActionPermissionFilter filter = new ActionPermissionFilter();
		filter.addProfileId(profileId);
		return get(filter, env);
	}
	
	public List<ActionPermission> get(ActionPermissionFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ActionPermissionFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_PERMISSION_API_NAME)).getKID());
		c.addProperty("read, action.id, profile.id, profile.name");
		c.addStandardSelectProperties();
		c.createAlias("profile", "profile");
		
		if (filter.getActionIds() != null && !filter.getActionIds().isEmpty())
		{
			c.add(Restriction.in("action.id", filter.getActionIds()));
		}
		
		if (filter.getProfileIds() != null && !filter.getProfileIds().isEmpty())
		{
			c.add(Restriction.in("profile.id", filter.getProfileIds()));
		}
		
		List<Record> records = c.list();
		List<ActionPermission> permissions = new ArrayList<ActionPermission>();
		for (Record r : records)
		{
			permissions.add(new ActionPermission(r, env));
		}
		
		return permissions;
	}
}