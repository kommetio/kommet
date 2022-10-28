/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.RecordAccessType;
import kommet.basic.SystemSetting;
import kommet.dao.SystemSettingDao;
import kommet.dao.SystemSettingFilter;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.systemsettings.SystemSettingKey;

@Service
public class SystemSettingService
{
	@Inject
	SystemSettingDao settingDao;

	// This number is added to linking types generated automatically to association field.
	// It is not guaranteed to reflect the actual number of associations, because it may be increased
	// during a failed attempt to create and association and this change is not rolled back.
	//public static final String ASSOCIATION_COUNT = "ASSOCIATION_COUNT";
	
	@Transactional(readOnly = true)
	public List<SystemSetting> find (SystemSettingFilter filter, EnvData env) throws KommetException
	{
		return settingDao.find(filter, env);
	}

	@Transactional
	public SystemSetting save(SystemSetting setting, AuthData authData, EnvData env) throws KommetException
	{
		return settingDao.save(setting, authData, env);
	}
	
	@Transactional
	public SystemSetting setSetting(SystemSettingKey key, String value, AuthData authData, EnvData env) throws KommetException
	{
		return setSetting(key.toString(), value, RecordAccessType.PUBLIC, authData, env);
	}
	
	@Transactional
	public SystemSetting setSetting(SystemSettingKey key, String value, RecordAccessType accessType, AuthData authData, EnvData env) throws KommetException
	{
		return setSetting(key.toString(), value, accessType, authData, env);
	}
	
	@Transactional
	public SystemSetting setSetting(String key, String value, AuthData authData, EnvData env) throws KommetException
	{
		return setSetting(key, value, RecordAccessType.PUBLIC, authData, env);
	}

	@Transactional
	public SystemSetting setSetting(String key, String value, RecordAccessType accessType, AuthData authData, EnvData env) throws KommetException
	{
		SystemSettingFilter filter = new SystemSettingFilter();
		filter.addKey(key);
		List<SystemSetting> settings = settingDao.find(filter, env);
		
		SystemSetting setting = null;
		
		if (!settings.isEmpty())
		{
			setting = settings.get(0);
		}
		else
		{
			setting = new SystemSetting();
			setting.setKey(key);
			setting.setAccessType(accessType.getId());
		}
		
		setting.setValue(value);
		if (value == null)
		{
			setting.nullify("value");
		}
		
		setting = settingDao.save(setting, authData, env);
		
		reloadSystemSettings(env);
		
		return setting;
	}
	
	@Transactional(readOnly = true)
	public void reloadSystemSettings (EnvData env) throws KommetException
	{
		// init system settings
		List<SystemSetting> settings = find(null, env);
		for (SystemSetting setting : settings)
		{
			env.addSetting(setting);
		}
	}
	
	@Transactional(readOnly = true)
	public Locale getDefaultLocale(EnvData env) throws KommetException
	{
		String sLocale = getSettingValue(SystemSettingKey.DEFAULT_ENV_LOCALE, env);
		return sLocale != null ? Locale.valueOf(sLocale) : null;
	}
	
	@Transactional(readOnly = true)
	public List<SystemSetting> getSettings(SystemSettingKey key, EnvData env) throws KommetException
	{
		return getSettings(key.toString(), env);
	}

	@Transactional(readOnly = true)
	public List<SystemSetting> getSettings(String key, EnvData env) throws KommetException
	{
		SystemSettingFilter filter = new SystemSettingFilter();
		filter.addKey(key);
		return settingDao.find(filter, env);
	}
	
	@Transactional(readOnly = true)
	public String getSettingValue(SystemSettingKey key, EnvData env) throws KommetException
	{
		return getSettingValue(key.toString(), env);
	}

	@Transactional(readOnly = true)
	public String getSettingValue(String key, EnvData env) throws KommetException
	{
		List<SystemSetting> settings = getSettings(key, env);
		if (settings.size() == 1)
		{
			return settings.get(0).getValue();
		}
		else if (settings.size() == 0)
		{
			return null;
		}
		else
		{
			throw new KommetException("More than one setting with key " + key + " found for single-value setting");
		}
	}

	@Transactional(readOnly = true)
	public List<SystemSetting> getSettings(EnvData env) throws KommetException
	{
		return settingDao.find(null, env);
	}

	public Integer getSettingIntValue(SystemSettingKey key, EnvData env) throws KommetException
	{
		String val = getSettingValue(key, env);
		return val != null ? Integer.valueOf(val) : null;
	}
}