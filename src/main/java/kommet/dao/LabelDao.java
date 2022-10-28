/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Label;
import kommet.basic.LabelAssignment;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.LabelFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class LabelDao extends GenericDaoImpl<Label>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public LabelDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<Label> get (LabelFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new LabelFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.LABEL_API_NAME)).getKID(), authData);
		c.addProperty("id, text");
		c.addStandardSelectProperties();
		
		if (filter.getLabelIds() != null && !filter.getLabelIds().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getLabelIds()));
		}
		
		if (filter.getAssignments() != null && !filter.getAssignments().isEmpty())
		{
			Set<KID> assignedLabelIds = new HashSet<KID>();
			for (LabelAssignment assignment : filter.getAssignments())
			{
				assignedLabelIds.add(assignment.getLabel().getId());
			}
			
			c.add(Restriction.in(Field.ID_FIELD_NAME, assignedLabelIds));
		}
		
		if (StringUtils.hasText(filter.getText()))
		{
			c.add(Restriction.eq("text", filter.getText()));
		}
		
		if (StringUtils.hasText(filter.getTextLike()))
		{
			c.add(Restriction.like("text", "%" + filter.getTextLike() + "%"));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<Label> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<Label> labels = new ArrayList<Label>();
		
		for (Record r : records)
		{
			labels.add(new Label(r, env));
		}
		
		return labels;
	}
}