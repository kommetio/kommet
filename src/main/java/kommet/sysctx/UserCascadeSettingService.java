/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.sysctx;

import org.springframework.stereotype.Service;

import kommet.auth.AuthData;
import kommet.env.EnvData;
import kommet.uch.UserCascadeHierarchyService;

public class UserCascadeSettingService
{
	private EnvData env;
	private AuthData authData;
	private UserCascadeHierarchyService uchService;
	
	public UserCascadeSettingService (UserCascadeHierarchyService uchService, AuthData authData, EnvData env)
	{
		this.uchService = uchService;
		this.env = env;
		this.authData = authData;
	}

	public EnvData getEnv()
	{
		return env;
	}

	public void setEnv(EnvData env)
	{
		this.env = env;
	}

	public AuthData getAuthData()
	{
		return authData;
	}

	public void setAuthData(AuthData authData)
	{
		this.authData = authData;
	}

	public UserCascadeHierarchyService getUchService()
	{
		return uchService;
	}

	public void setUchService(UserCascadeHierarchyService uchService)
	{
		this.uchService = uchService;
	}
}