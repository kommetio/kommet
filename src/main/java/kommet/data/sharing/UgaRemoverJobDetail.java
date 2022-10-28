/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.sharing;

import org.quartz.impl.JobDetailImpl;

import kommet.env.EnvData;
import kommet.services.UserGroupService;

public class UgaRemoverJobDetail extends JobDetailImpl
{	
	private static final long serialVersionUID = -8150617868831963728L;
	private EnvData env;
	private UserGroupService ugService;
	
	public UgaRemoverJobDetail(UserGroupService ugService, EnvData env)
	{
		super();
		this.env = env;
		this.ugService = ugService;
	}
	
	public UserGroupService getUserGroupService()
	{
		return ugService;
	}

	public EnvData getEnv()
	{
		return env;
	}
}