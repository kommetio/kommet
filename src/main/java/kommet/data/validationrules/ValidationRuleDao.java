/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.validationrules;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.basic.RecordProxyType;
import kommet.basic.ValidationRule;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ValidationRuleDao extends GenericDaoImpl<ValidationRule>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public ValidationRuleDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public List<ValidationRule> get (ValidationRuleFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ValidationRuleFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.VALIDATION_RULE_API_NAME)).getKID());
		c.addProperty("id, name, active, typeId, code, errorMessage, errorMessageLabel, isSystem, referencedFields");
		c.addStandardSelectProperties();
		
		if (filter.getRuleIds() != null && !filter.getRuleIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getRuleIds()));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (filter.getErrorMessageLabels() != null && !filter.getErrorMessageLabels().isEmpty())
		{
			c.add(Restriction.in("errorMessageLabel", filter.getErrorMessageLabels()));
		}
		
		if (filter.getTypeIds() != null && !filter.getTypeIds().isEmpty())
		{
			c.add(Restriction.in("typeId", filter.getTypeIds()));
		}
		
		if (filter.getIsActive() != null)
		{
			c.add(Restriction.eq("active", filter.getIsActive()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		List<Record> records = c.list();
		List<ValidationRule> labels = new ArrayList<ValidationRule>();
		for (Record r : records)
		{
			labels.add(new ValidationRule(r, env));
		}
		
		return labels;
	}
}