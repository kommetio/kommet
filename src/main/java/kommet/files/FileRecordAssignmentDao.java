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

import kommet.auth.AuthData;
import kommet.basic.FileRecordAssignment;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class FileRecordAssignmentDao extends GenericDaoImpl<FileRecordAssignment>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public FileRecordAssignmentDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<FileRecordAssignment> find(FileRecordAssignmentFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new FileRecordAssignmentFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.FILE_RECORD_ASSIGNMENT_API_NAME)).getKID(), authData);
		c.createAlias("file", "file");
		c.createAlias("file.createdBy", "fileCreatedBy");
		c.addProperty("file.id, recordId, comment");
		if (filter.isInitFiles())
		{
			// initialize whole file record
			c.addProperty("file.name, file.sealed, file.access, file." + Field.CREATEDDATE_FIELD_NAME + ", file." + Field.CREATEDBY_FIELD_NAME + "." + Field.ID_FIELD_NAME);
		}
		c.addStandardSelectProperties();
		
		if (filter.getFileIds() != null && !filter.getFileIds().isEmpty())
		{
			c.add(Restriction.in("file.id", filter.getFileIds()));
		}
		
		if (filter.getRecordIds() != null && !filter.getRecordIds().isEmpty())
		{
			c.add(Restriction.in("recordId", filter.getRecordIds()));
		}
		
		if (filter.getIds() != null && !filter.getIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getIds()));
		}
		
		// convert and return results
		List<Record> records = c.list();
		List<FileRecordAssignment> fileObjAssignments = new ArrayList<FileRecordAssignment>();
		for (Record r : records)
		{
			fileObjAssignments.add(new FileRecordAssignment(r, env));
		}
		return fileObjAssignments;
	}
}