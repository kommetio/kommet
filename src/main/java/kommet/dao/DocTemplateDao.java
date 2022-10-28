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

import kommet.basic.DocTemplate;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.docs.DocTemplateFilter;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class DocTemplateDao extends GenericDaoImpl<DocTemplate>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public DocTemplateDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<DocTemplate> find (DocTemplateFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new DocTemplateFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.DOC_TEMPLATE_API_NAME)).getKID());
		c.addProperty("name, content");
		c.addStandardSelectProperties();
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (filter.getIds() != null && !filter.getIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getIds()));
		}
		
		// convert and return results
		List<Record> records = c.list();
		List<DocTemplate> templates = new ArrayList<DocTemplate>();
		for (Record r : records)
		{
			templates.add(new DocTemplate(r, env));
		}
		return templates;
	}
}