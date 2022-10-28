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

import kommet.basic.RecordProxyType;
import kommet.basic.TypePermission;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.permissions.TypePermissionFilter;
import kommet.persistence.GenericDaoImpl;

@Repository
public class TypePermissionDao extends GenericDaoImpl<TypePermission>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public TypePermissionDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * Returns type permissions for a given profile.
	 * @param profileId
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public List<TypePermission> getForProfile(KID profileId, EnvData env) throws KommetException
	{
		TypePermissionFilter filter = new TypePermissionFilter();
		filter.addProfileId(profileId);
		return get(filter, env);
	}
	
	public List<TypePermission> get(TypePermissionFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new TypePermissionFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.TYPE_PERMISSION_API_NAME)).getKID());
		c.addProperty("read, edit, delete, create, readAll, editAll, deleteAll, typeId, profile.id, profile.name");
		c.addStandardSelectProperties();
		c.createAlias("profile", "profile");
		
		if (filter.getTypeIds() != null && !filter.getTypeIds().isEmpty())
		{
			c.add(Restriction.in("typeId", filter.getTypeIds()));
		}
		
		if (filter.getProfileIds() != null && !filter.getProfileIds().isEmpty())
		{
			c.add(Restriction.in("profile.id", filter.getProfileIds()));
		}
		
		List<Record> records = c.list();
		List<TypePermission> permissions = new ArrayList<TypePermission>();
		for (Record r : records)
		{
			permissions.add(new TypePermission(r, env));
		}
		
		return permissions;
	}
}