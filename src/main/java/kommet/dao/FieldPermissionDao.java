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

import kommet.basic.FieldPermission;
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
import kommet.permissions.FieldPermissionFilter;
import kommet.persistence.GenericDaoImpl;

@Repository
public class FieldPermissionDao extends GenericDaoImpl<FieldPermission>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public FieldPermissionDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * Returns field permissions for a given profile.
	 * @param profileId
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public List<FieldPermission> getForProfile(KID profileId, EnvData env) throws KommetException
	{
		FieldPermissionFilter filter = new FieldPermissionFilter();
		filter.addProfileId(profileId);
		return get(filter, env);
	}
	
	public List<FieldPermission> get(FieldPermissionFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new FieldPermissionFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.FIELD_PERMISSION_API_NAME)).getKID());
		c.addProperty("read, edit, fieldId, profile.id, profile.name");
		c.addStandardSelectProperties();
		c.createAlias("profile", "profile");
		
		if (filter.getFieldIds() != null && !filter.getFieldIds().isEmpty())
		{
			c.add(Restriction.in("fieldId", filter.getFieldIds()));
		}
		
		if (filter.getProfileIds() != null && !filter.getProfileIds().isEmpty())
		{
			c.add(Restriction.in("profile.id", filter.getProfileIds()));
		}
		
		List<Record> records = c.list();
		List<FieldPermission> permissions = new ArrayList<FieldPermission>();
		for (Record r : records)
		{
			permissions.add(new FieldPermission(r, env));
		}
		
		return permissions;
	}
}