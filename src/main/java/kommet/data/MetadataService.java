/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import org.springframework.stereotype.Service;

import kommet.env.EnvData;

@Service
public class MetadataService
{
	/**
	 * Creates a type on a given environment
	 * @param type
	 * @param envData
	 * @return
	 */
	public Type insert (Type type, EnvData envData)
	{
		//StringBuilder sql = new StringBuilder();
		//sql.append("INSERT INTO objects (apiname, label, plurallabel, packageName, created, dbtable, kid, keyprefix, kidsequence")
		
		//envData.getJdbcTemplate().update(sql.toString());
		return null;
	}
}