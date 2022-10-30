/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.env.EnvData;

@Service
public class EnvDataService
{
	private Map<String, EnvData> envsByName;
	
	@Transactional
	public EnvData getByName (String name)
	{
		if (!envsByName.containsKey(name.toLowerCase()))
		{
			// get env
		}
		
		return envsByName.get(name.toLowerCase());
	}
}