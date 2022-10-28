/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.VIEW_RESOURCE_API_NAME)
public class ViewResource extends StandardTypeRecordProxy implements Deployable
{
	private String name;
	private String content;
	private String path;
	private String mimeType;
	
	public ViewResource() throws KommetException
	{
		super(null, true, null);
	}
	
	public ViewResource(Record view, EnvData env) throws KommetException
	{
		super(view, false, env);
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

	@Property(field = "content")
	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
		setInitialized();
	}

	@Property(field = "path")
	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
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
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.VIEW_RESOURCE;
	}
}