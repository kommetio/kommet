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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.SYSTEM_SETTING_API_NAME)
public class SystemSetting extends StandardTypeRecordProxy
{
	private String key;
	private String value;
	
	public SystemSetting() throws KommetException
	{
		this(null, null);
	}
	
	public SystemSetting(Record view, EnvData env) throws KommetException
	{
		super(view, true, env);
	}

	public void setKey(String key)
	{
		this.key = key;
		setInitialized();
	}

	@Property(field = "key")
	public String getKey()
	{
		return key;
	}

	public void setValue(String value)
	{
		this.value = value;
		setInitialized();
	}

	@Property(field = "value")
	public String getValue()
	{
		return value;
	}
}