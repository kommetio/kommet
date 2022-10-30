/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import kommet.auth.AuthData;
import kommet.basic.RecordProxy;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.DeleteQuery;
import kommet.dao.queries.InsertQuery;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SelectQuery;
import kommet.dao.queries.UpdateQuery;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;

@Service
public class DaoFacade
{
	@Inject
	TypePersistenceConfig persistence;
	
	public Record save (Record record, boolean forceAllowEdit, AuthData authData, EnvData env) throws KommetException
	{	
		if (record.attemptGetKID() == null)
		{	
			InsertQuery query = env.getTypeMapping(record.getType().getKID()).createInsertQuery(env);
			query.execute(record);
		}
		else
		{
			// in this special case we want to pass null auth data to the criteria object
			// because this criteria is not really for querying objects, it's used to construct conditions
			// for the update query
			Criteria criteria = new Criteria(record.getType(), null, env, false);
			criteria.add(Restriction.eq("id", record.getKID()));
			UpdateQuery query = env.getTypeMapping(record.getType().getKID()).createUpdateQuery(env);
			query.execute(record, criteria, authData, forceAllowEdit);
		}
		
		return record;
	}
	
	public void delete (Record record, AuthData authData, EnvData envData) throws KommetException
	{	
		DeleteQuery query = envData.getTypeMapping(record.getType().getKID()).createDeleteQuery(envData);
		query.execute(record, authData);
	}
	
	public void delete (KID recordId, AuthData authData, EnvData env) throws KommetException
	{	
		Type type = env.getTypeByRecordId(recordId);
		DeleteQuery query = env.getTypeMapping(type.getKID()).createDeleteQuery(env);
		query.execute(recordId, authData);
	}
	
	public void delete (Collection<Record> records, AuthData authData, EnvData env) throws KommetException
	{	
		if (records.isEmpty())
		{
			return;
		}
		DeleteQuery query = env.getTypeMapping(records.iterator().next().getType().getKID()).createDeleteQuery(env);
		query.execute(records, authData, env);
	}
	
	public List<Record> select (String dalQuery, EnvData env) throws KommetException
	{
		return env.getSelectCriteriaFromDAL(dalQuery).list();
	}

	public List<Record> find(Criteria criteria, Collection<String> nestedProperties, EnvData env) throws KommetException
	{
		SelectQuery query = SelectQuery.buildFromCriteria(criteria, nestedProperties, env);
		return query.execute();
	}

	public <T extends RecordProxy> void delete(List<T> objects, AuthData authData, EnvData env) throws KommetException
	{
		if (objects.isEmpty())
		{
			return;
		}
		
		// deduce object type
		KID firstObjectId = objects.iterator().next().getId();
		Type type = null;
		if (firstObjectId != null)
		{
			type = env.getTypeByRecordId(firstObjectId);
		}
		else
		{
			throw new KommetException("First object on list of objects to delete has null ID");
		}
		
		DeleteQuery query = env.getTypeMapping(type.getKID()).createDeleteQuery(env);
		query.execute(objects, authData, env);
	}
}