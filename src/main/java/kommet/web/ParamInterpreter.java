/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web;

import kommet.data.KeyPrefix;
import kommet.data.KID;

public class ParamInterpreter
{
	public static String interpret (String params, KID recordId, KeyPrefix prefix)
	{
		if (params == null)
		{
			return params;
		}
		
		params = params.replaceAll("\\$record.id", recordId.getId());
		params = params.replaceAll("\\$type.prefix", prefix.getPrefix());
		
		return params;
	}
}