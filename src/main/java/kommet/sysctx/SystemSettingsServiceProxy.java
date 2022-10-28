/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.sysctx;

import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.CurrentAuthDataAware;
import kommet.services.SystemSettingService;
import kommet.systemsettings.SystemSettingKey;

public class SystemSettingsServiceProxy extends ServiceProxy
{
	private SystemSettingService systemSettingService;
	
	public SystemSettingsServiceProxy(SystemSettingService ssService, CurrentAuthDataAware authDataProvider, EnvData env)
	{
		super(authDataProvider, env);
		this.systemSettingService = ssService;
	}
	
	public String getSettingValue(SystemSettingKey key, EnvData env) throws KommetException
	{
		return systemSettingService.getSettingValue(key.toString(), env);
	}
}