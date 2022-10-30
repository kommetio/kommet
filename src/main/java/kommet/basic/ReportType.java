/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.REPORT_TYPE_API_NAME)
public class ReportType extends StandardTypeRecordProxy
{
	private String name;
	private String description;
	private String serializedQuery;
	private KID baseTypeId;
	
	public ReportType() throws KommetException
	{
		this(null, null);
	}
	
	public ReportType(Record rt, EnvData env) throws KommetException
	{
		super(rt, true, env);
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

	public void setSerializedQuery(String serializedQuery)
	{
		this.serializedQuery = serializedQuery;
		setInitialized();
	}

	@Property(field = "serializedQuery")
	public String getSerializedQuery()
	{
		return serializedQuery;
	}

	public void setBaseTypeId(KID baseTypeId)
	{
		this.baseTypeId = baseTypeId;
		setInitialized();
	}

	@Property(field = "baseTypeId")
	public KID getBaseTypeId()
	{
		return baseTypeId;
	}

	public void setDescription(String description)
	{
		this.description = description;
		setInitialized();
	}

	@Property(field = "description")
	public String getDescription()
	{
		return description;
	}
}