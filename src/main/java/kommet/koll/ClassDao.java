/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.integration.PropertySelection;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ClassDao extends GenericDaoImpl<Class>
{	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public ClassDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * The default save method from GenericDao is overridden to pass skipTriggers and skipSharing parameters
	 * to the save method - we don't want triggers to be called or sharing added to SystemSettings.
	 */
	public Class save(Class obj, boolean skipTriggers, boolean skipSharing, AuthData authData, EnvData env) throws KommetException
	{
		return (Class)getEnvCommunication().save(obj, skipTriggers, skipSharing, authData, env);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public Class get (KID id, EnvData env) throws KommetException
	{
		return get(id, PropertySelection.ALL_SIMPLE_PROPERTIES, null, env);
	}
	
	public List<Class> find (ClassFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ClassFilter();
		}
		
		if (StringUtils.hasText(filter.getSimpleName()) && StringUtils.hasText(filter.getQualifiedName()))
		{
			throw new KommetException("Cannot search KOLL files by both simple and qualified name");
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.CLASS_API_NAME)).getKID());
		c.addProperty("name, packageName, isSystem, javaCode, kollCode, isDraft");
		c.addStandardSelectProperties();
		
		if (StringUtils.hasText(filter.getSimpleName()))
		{
			c.add(Restriction.eq("name", filter.getSimpleName()));
		}
		
		if (StringUtils.hasText(filter.getNameLike()))
		{
			c.add(Restriction.ilike("name", filter.getNameLike().replaceAll("\\*", "%")));
		}
		
		if (StringUtils.hasText(filter.getContentLike()))
		{
			c.add(Restriction.ilike("kollCode", "%" + filter.getContentLike() + "%"));
		}
		
		if (filter.getSystemFile() != null)
		{
			c.add(Restriction.eq("isSystem", filter.getSystemFile()));
		}
		
		if (StringUtils.hasText(filter.getQualifiedName()))
		{
			// split qualified name into package and simple name
			c.add(Restriction.eq("name", filter.getQualifiedName().substring(filter.getQualifiedName().lastIndexOf('.') + 1)));
			c.add(Restriction.eq("packageName", filter.getQualifiedName().substring(0, filter.getQualifiedName().lastIndexOf('.'))));
		}
		
		if (filter.getAccessType() != null)
		{
			c.add(Restriction.eq(Field.ACCESS_TYPE_FIELD_NAME, filter.getAccessType().getId()));
		}
		
		if (filter.getKIDs() != null && !filter.getKIDs().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getKIDs()));
		}
		
		c = filter.applySortAndLimit(c);
		
		List<Record> records = c.list();
		List<Class> files = new ArrayList<Class>();
		for (Record r : records)
		{
			files.add(new Class(r, env));
		}
		return files;
	}
}