/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.docs;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.DocTemplate;
import kommet.dao.DocTemplateDao;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;

@Service
public class DocTemplateService
{
	@Inject
	DocTemplateDao templateDao;
	
	@Transactional(readOnly = true)
	public List<DocTemplate> find (DocTemplateFilter filter, EnvData env) throws KommetException
	{
		return templateDao.find(filter, env);
	}
	
	@Transactional(readOnly = true)
	public DocTemplate get (KID id, EnvData env) throws KommetException
	{
		return templateDao.get(id, env);
	}
	
	@Transactional
	public void delete (KID id, EnvData env) throws KommetException
	{
		templateDao.delete(id, true, null, env);
	}
	
	@Transactional(readOnly = true)
	public DocTemplate getByName (String name, EnvData env) throws KommetException
	{
		if (!StringUtils.hasText(name))
		{
			throw new KommetException("Name by which template is retrieved is empty");
		}
		
		DocTemplateFilter filter = new DocTemplateFilter();
		filter.setName(name);
		List<DocTemplate> templates = templateDao.find(filter, env);
		
		if (templates.size() > 1)
		{
			throw new KommetException("More than one doc template found for name " + name);
		}
		else
		{
			return templates.isEmpty() ? null : templates.get(0);
		}
	}
	
	@Transactional
	public DocTemplate save (DocTemplate template, AuthData authData, EnvData env) throws KommetException
	{
		return templateDao.save(template, authData, env);
	}
}