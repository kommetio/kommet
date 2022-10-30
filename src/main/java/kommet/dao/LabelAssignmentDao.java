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
import kommet.basic.LabelAssignment;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.LabelAssignmentFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class LabelAssignmentDao extends GenericDaoImpl<LabelAssignment>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public LabelAssignmentDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<LabelAssignment> get (LabelAssignmentFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new LabelAssignmentFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.LABEL_ASSIGNMENT_API_NAME)).getKID(), authData);
		c.addProperty("id, label.id, recordId");
		c.createAlias("label", "label");
		c.addStandardSelectProperties();
		
		if (filter.getLabelIds() != null && !filter.getLabelIds().isEmpty())
		{
			c.add(Restriction.in("label.id", filter.getLabelIds()));
		}
		
		if (filter.getRecordIds() != null && !filter.getRecordIds().isEmpty())
		{
			c.add(Restriction.in("recordId", filter.getLabelIds()));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<LabelAssignment> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<LabelAssignment> assignments = new ArrayList<LabelAssignment>();
		
		for (Record r : records)
		{
			assignments.add(new LabelAssignment(r, env));
		}
		
		return assignments;
	}
}