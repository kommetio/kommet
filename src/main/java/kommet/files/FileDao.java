/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.files;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.File;
import kommet.basic.RecordProxyType;
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
public class FileDao extends GenericDaoImpl<File>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public FileDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<File> find (FileFilter filter, boolean initRevisions, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new FileFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.FILE_API_NAME)).getKID(), authData);
		c.addProperty("name, access, sealed");
		if (initRevisions)
		{
			c.createAlias("revisions", "revisions");
			c.addProperty("revisions.id, revisions.name, revisions.revisionNumber, revisions.path, revisions.size");
		}
		c.addStandardSelectProperties();
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (filter.getFileIds() != null && !filter.getFileIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getFileIds()));
		}
		
		// convert and return results
		List<Record> records = c.list();
		List<File> files = new ArrayList<File>();
		for (Record r : records)
		{
			files.add(new File(r, env));
		}
		return files;
	}
}