/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.FILE_RECORD_ASSIGNMENT_API_NAME)
public class FileRecordAssignment extends StandardTypeRecordProxy
{
	private File file;
	private String comment;
	private KID recordId;
	
	public FileRecordAssignment(Record record, EnvData env) throws RecordProxyException
	{
		super(record, true, env);
	}
	
	public FileRecordAssignment() throws RecordProxyException
	{
		super(null, true, null);
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
		setInitialized();
	}

	@Property(field = "recordId")
	public KID getRecordId()
	{
		return recordId;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
		setInitialized();
	}

	@Property(field = "comment")
	public String getComment()
	{
		return comment;
	}

	public void setFile(File file)
	{
		this.file = file;
		setInitialized();
	}

	@Property(field = "file")
	public File getFile()
	{
		return file;
	}

}