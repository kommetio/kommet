/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.Record;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.WEB_RESOURCE_API_NAME)
public class WebResource extends StandardTypeRecordProxy implements Deployable
{	
	private File file;
	private String name;
	private String mimeType;
	private String diskFilePath;
	private Boolean isPublic;
	
	public WebResource() throws RecordProxyException
	{
		super(null, true, null);
	}
	
	public WebResource(Record record, EnvData env) throws RecordProxyException
	{
		super(record, true, env);
	}

	@Property(field = "file")
	public File getFile()
	{
		return file;
	}

	public void setFile(File file)
	{
		this.file = file;
		setInitialized();
	}

	@Property(field = "mimeType")
	public String getMimeType()
	{
		return mimeType;
	}

	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
		setInitialized();
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Transient
	public String getDiskFilePath()
	{
		return diskFilePath;
	}

	public void setDiskFilePath(String diskFilePath)
	{
		// note: no call to setInitialized() here because this is a transient property
		this.diskFilePath = diskFilePath;
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.WEB_RESOURCE;
	}

	@Transient
	public Boolean getIsPublic()
	{
		return isPublic;
	}

	public void setPublic(Boolean isPublic)
	{
		this.isPublic = isPublic;
	}
}