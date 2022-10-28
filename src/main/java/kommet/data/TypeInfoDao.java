/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.auth.AuthData;
import kommet.basic.RecordProxyType;
import kommet.basic.TypeInfo;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class TypeInfoDao extends GenericDaoImpl<TypeInfo>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public TypeInfoDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	/**
	 * The default save method from GenericDao is overridden to pass skipTriggers and skipSharing parameters
	 * to the save method - we don't want triggers to be called or sharing added to SystemSettings.
	 */
	@Override
	public TypeInfo save(TypeInfo obj, AuthData authData, EnvData env) throws KommetException
	{
		return (TypeInfo)getEnvCommunication().save(obj, true, true, authData, env);
	}
	
	public List<TypeInfo> find (TypeInfoFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new TypeInfoFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.TYPE_INFO_API_NAME)).getKID());
		c.addProperty("typeId, standardController.id");
		
		List<String> actionFieldsToRetrieve = new ArrayList<String>();
		actionFieldsToRetrieve.add("url");
		actionFieldsToRetrieve.add("id");
		actionFieldsToRetrieve.add("view.id");
		actionFieldsToRetrieve.add("typeId");
		actionFieldsToRetrieve.add("name");
		actionFieldsToRetrieve.add("view.name");
		actionFieldsToRetrieve.add("view.typeId");
		
		for (String actionField : actionFieldsToRetrieve)
		{
			c.addProperty("defaultCreateAction." + actionField);
			c.addProperty("defaultEditAction." + actionField);
			c.addProperty("defaultDetailsAction." + actionField);
			c.addProperty("defaultListAction." + actionField);
			c.addProperty("defaultSaveAction." + actionField);
		}
		
		c.addStandardSelectProperties();
		c.createAlias("defaultListAction", "defaultListAction");
		c.createAlias("defaultEditAction", "defaultEditAction");
		c.createAlias("defaultCreateAction", "defaultCreateAction");
		c.createAlias("defaultDetailsAction", "defaultDetailsAction");
		c.createAlias("defaultSaveAction", "defaultSaveAction");
		// add aliases for views
		c.createAlias("defaultListAction.view", "defaultListActionView");
		c.createAlias("defaultEditAction.view", "defaultEditActionView");
		c.createAlias("defaultCreateAction.view", "defaultCreateActionView");
		c.createAlias("defaultDetailsAction.view", "defaultDetailsPageView");
		c.createAlias("defaultSaveAction.view", "defaultSaveActionView");
		c.createAlias("standardController", "standardController");
		
		if (filter.getTypeIds() != null && !filter.getTypeIds().isEmpty())
		{
			c.add(Restriction.in("typeId", filter.getTypeIds()));
		}
		
		List<Record> records = c.list();
		List<TypeInfo> typeInfos = new ArrayList<TypeInfo>();
		for (Record r : records)
		{
			typeInfos.add(new TypeInfo(r, env));
		}
		return typeInfos;
	}
}