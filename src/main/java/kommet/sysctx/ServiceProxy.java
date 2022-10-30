/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.sysctx;

import kommet.env.EnvData;
import kommet.koll.CurrentAuthDataAware;

public abstract class ServiceProxy
{
	protected EnvData env;
	protected CurrentAuthDataAware authDataProvider;
	
	public ServiceProxy (CurrentAuthDataAware authDataProvider, EnvData env)
	{
		this.authDataProvider = authDataProvider;
		this.env = env;
	}
}