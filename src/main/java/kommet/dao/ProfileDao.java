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

import kommet.auth.AuthData;
import kommet.basic.Profile;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.ProfileFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ProfileDao extends GenericDaoImpl<Profile>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public ProfileDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<Profile> find (ProfileFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ProfileFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.PROFILE_API_NAME)).getKID(), authData);
		c.addProperty("id, name, label, systemProfile");
		c.addStandardSelectProperties();
		
		if (filter.getLabel() != null)
		{
			c.add(Restriction.eq("label", filter.getLabel()));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<Profile> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<Profile> profiles = new ArrayList<Profile>();
		
		for (Record r : records)
		{
			profiles.add(new Profile(r, env));
		}
		
		return profiles;
	}
}