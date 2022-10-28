/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.RecordProxyType;
import kommet.basic.View;
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
public class ViewDao extends GenericDaoImpl<View>
{	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public ViewDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<View> find (ViewFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ViewFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.VIEW_API_NAME)).getKID());
		// do not retrieve the code field in the query because it's too large
		c.addProperty("name, path, packageName, isSystem, typeId, layout");
		c.addStandardSelectProperties();
		
		if (filter.isInitCode())
		{
			c.addProperty("keetleCode, jspCode");
		}
		
		if (filter.getSystemView() != null)
		{
			c.add(Restriction.eq("isSystem", filter.getSystemView()));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (StringUtils.hasText(filter.getPackage()))
		{
			c.add(Restriction.eq("packageName", filter.getPackage()));
		}
		
		if (StringUtils.hasText(filter.getQualifiedName()))
		{
			// split qualified name into package and simple name
			c.add(Restriction.eq("name", filter.getQualifiedName().substring(filter.getQualifiedName().lastIndexOf('.') + 1)));
			c.add(Restriction.eq("packageName", filter.getQualifiedName().substring(0, filter.getQualifiedName().lastIndexOf('.'))));
		}
		
		if (filter.getKID() != null)
		{
			c.add(Restriction.eq("id", filter.getKID()));
		}
		
		c = filter.applySortAndLimit(c);
		
		return getObjectStubList(c.list(), env);
	}

	private static List<View> getObjectStubList(List<Record> records, EnvData env) throws KommetException
	{
		List<View> views = new ArrayList<View>();
		
		for (Record r : records)
		{
			views.add(new View(r, true, env));
		}
		
		return views;
	}

	public View saveSystemView(View view, AuthData authData, EnvData env) throws KommetException
	{
		return (View)getEnvCommunication().save(view, true, true, authData, env);
	}
}
