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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.basic.FileRevision;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class FileRevisionDao extends GenericDaoImpl<FileRevision>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	private static final Logger log = LoggerFactory.getLogger(FileRevisionDao.class);
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public FileRevisionDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	private Criteria getFilterCriteria (FileRevisionFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new FileRevisionFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.FILE_REVISION_API_NAME)).getKID());
		c.addProperty("name, revisionNumber, path, size, file.id");
		c.createAlias("file", "file");
		c.addStandardSelectProperties();
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (filter.getRevisionNumber() != null)
		{
			c.add(Restriction.eq("revisionNumber", filter.getRevisionNumber()));
		}
		
		if (filter.getFileIds() != null && !filter.getFileIds().isEmpty())
		{
			c.add(Restriction.in("file.id", filter.getFileIds()));
		}
		
		if (filter.getId() != null)
		{
			c.add(Restriction.eq("id", filter.getId()));
		}
		
		return c;
	}
	
	public List<FileRevision> find (FileRevisionFilter filter, EnvData env) throws KommetException
	{	
		Criteria c = getFilterCriteria(filter, env);
		
		// convert and return results
		List<Record> records = c.list();
		List<FileRevision> revisions = new ArrayList<FileRevision>();
		for (Record r : records)
		{
			revisions.add(new FileRevision(r, env));
		}
		return revisions;
	}

	public List<FileRevision> findOrderedRevisions(FileRevisionFilter filter, EnvData env) throws KommetException
	{
		Criteria c = getFilterCriteria(filter, env);
		String fileAlias = c.getPropertyAlias("file");
		if (!StringUtils.hasText(fileAlias))
		{
			c.createAlias("file", "file");
			fileAlias = "file";
		}
		
		c.addOrderBy(SortDirection.ASC, "file.id");
		c.addOrderBy(SortDirection.DESC, "revisionNumber");
		
		// convert and return results
		List<Record> records = c.list();
		List<FileRevision> revisions = new ArrayList<FileRevision>();
		for (Record r : records)
		{
			revisions.add(new FileRevision(r, env));
		}
		return revisions;
	}
}