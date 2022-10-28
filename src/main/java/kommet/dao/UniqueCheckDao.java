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
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.RecordProxyType;
import kommet.basic.RecordProxyUtil;
import kommet.basic.UniqueCheck;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class UniqueCheckDao extends GenericDaoImpl<UniqueCheck>
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
	
	public UniqueCheckDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public UniqueCheck save (UniqueCheck check, EnvData env, AuthData authData) throws KommetException
	{	
		Record rec = RecordProxyUtil.generateRecord(check, env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.UNIQUE_CHECK_API_NAME)), 2, env); 
		rec = dataService.save(rec, true, true, authData, env);
		check.setId(rec.getKID());
		return check;
	}
	
	/**
	 * Finds unique checks for the given filter criteria.
	 * 
	 * DataService cannot be injected into this bean because the DAO is itself used in the TypeService bean.
	 * 
	 * @param filter
	 * @param env
	 * @param dataService
	 * @return
	 * @throws KommetException 
	 */
	public List<UniqueCheck> find (UniqueCheckFilter filter, EnvData env, DataService typeService) throws KommetException
	{
		if (filter == null)
		{
			filter = new UniqueCheckFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.UNIQUE_CHECK_API_NAME)).getKID());
		c.addProperty("name, dbName, fieldIds, typeId, isSystem");
		
		// add system fields
		c.addProperty(Field.ID_FIELD_NAME);
		c.addProperty(Field.CREATEDDATE_FIELD_NAME);
		c.addProperty(Field.LAST_MODIFIED_DATE_FIELD_NAME);
		
		if (filter.isInitUserReferenceFields())
		{
			c.addProperty(Field.CREATEDBY_FIELD_NAME);
			c.addProperty(Field.LAST_MODIFIED_BY_FIELD_NAME);
		}
		
		if (filter.getTypeIds() != null)
		{
			c.add(Restriction.in("typeId", filter.getTypeIds()));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		List<Record> records = c.list();
		List<UniqueCheck> checks = new ArrayList<UniqueCheck>();
		for (Record r : records)
		{
			checks.add(new UniqueCheck(r, env));
		}
		return checks;
	}
	
	public List<UniqueCheck> findForField(Field field, EnvData env, DataService typeService) throws KommetException
	{
		UniqueCheckFilter filter = new UniqueCheckFilter();
		filter.addTypeId(field.getType().getKID());
		List<UniqueCheck> checks = find(filter, env, typeService);
		
		if (!checks.isEmpty())
		{
			List<UniqueCheck> checksForFields = new ArrayList<UniqueCheck>();
			for (UniqueCheck check : checks)
			{
				if (check.hasField(field.getKID()))
				{
					checksForFields.add(check);
				}
			}
			
			return checksForFields;
		}
		else
		{
			return checks;
		}
	}
}