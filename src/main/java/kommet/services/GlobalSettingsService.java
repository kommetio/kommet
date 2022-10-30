/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.data.KommetException;
import kommet.env.EnvData;

@Service
public class GlobalSettingsService
{	
	@Transactional(readOnly = true)
	public String getSetting (String name, EnvData envData) throws KommetException
	{
		String sql = "SELECT value FROM settings WHERE name = '" + name + "'";
		SqlRowSet rows = envData.getJdbcTemplate().queryForRowSet(sql);
		while (rows.next())
		{
			return rows.getString(1);
		}
		
		return null;
	}
	
	@Transactional(readOnly = true)
	public Integer getSettingAsInt (String name, EnvData envData) throws KommetException
	{
		String val = getSetting(name, envData);
		return val != null ? Integer.valueOf(val) : null;
	}
	
	@Transactional(readOnly = true)
	public Long getSettingAsLong (String name, EnvData envData) throws KommetException
	{
		String val = getSetting(name, envData);
		return val != null ? Long.valueOf(val) : null;
	}
	
	@Transactional(readOnly = true)
	public boolean settingExists (String name, EnvData envData) throws KommetException
	{
		return envData.getJdbcTemplate().queryForObject("SELECT count(id) FROM settings WHERE name = '" + name + "'", Integer.class) > 0;
	}
	
	@Transactional
	public void setSetting (String name, String value, EnvData envData) throws KommetException
	{
		String sql = null;
		if (settingExists(name, envData))
		{
			sql = "UPDATE settings SET value = '" + value + "' WHERE name = '" + name + "'";
		}
		else
		{
			sql = "INSERT INTO settings (name, value) VALUES ('" + name + "', '" + value + "')";
		}
		
		int result = envData.getJdbcTemplate().update(sql);
		if (result == 0)
		{
			throw new KommetException("Unknown error setting value for global setting '" + name + "'");
		}
	}
}