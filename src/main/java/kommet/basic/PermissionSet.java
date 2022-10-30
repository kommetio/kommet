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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.PERMISSION_SET_API_NAME)
public class PermissionSet extends StandardTypeRecordProxy
{
	private String name;
	private boolean system;
	
	public PermissionSet(Record record, EnvData env) throws KommetException
	{
		super(record, true, env);
		if (record != null)
		{
			this.name = (String)record.getField("name");
			this.system = (Boolean)record.getField("systemPermissionSet");
			this.id = record.attemptGetKID();
		}
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setSystem(boolean system)
	{
		this.system = system;
	}

	@Property(field = "systemPermissionSet")
	public boolean isSystem()
	{
		return system;
	}
}