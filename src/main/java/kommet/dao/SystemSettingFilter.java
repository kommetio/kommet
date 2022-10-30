/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.HashSet;
import java.util.Set;

import kommet.systemsettings.SystemSettingKey;

public class SystemSettingFilter
{
	private Set<String> keys;

	public void addKey(String key)
	{
		if (this.keys == null)
		{
			this.keys = new HashSet<String>();
		}
		this.keys.add(key);
	}
	
	public void addKey (SystemSettingKey key)
	{
		addKey(key.toString());
	}

	public Set<String> getKeys()
	{
		return keys;
	}
}