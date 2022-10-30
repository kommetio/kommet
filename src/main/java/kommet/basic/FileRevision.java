/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.FILE_REVISION_API_NAME)
public class FileRevision extends StandardTypeRecordProxy
{
	private String name;
	private Integer revisionNumber;
	private File file;
	private String path;
	private Integer size;
	
	public FileRevision() throws KommetException
	{
		this(null, null);
	}
	
	public FileRevision(Record view, EnvData env) throws KommetException
	{
		super(view, true, env);
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setRevisionNumber(Integer revisionNumber)
	{
		this.revisionNumber = revisionNumber;
		setInitialized();
	}

	@Property(field = "revisionNumber")
	public Integer getRevisionNumber()
	{
		return revisionNumber;
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

	public void setPath(String path)
	{
		this.path = path;
		setInitialized();
	}

	@Property(field = "path")
	public String getPath()
	{
		return path;
	}

	public void setSize(Integer size)
	{
		this.size = size;
		setInitialized();
	}

	@Property(field = "size")
	public Integer getSize()
	{
		return size;
	}
}