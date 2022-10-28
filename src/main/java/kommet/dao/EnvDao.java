/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.data.Env;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.EnvFilter;
import kommet.utils.MiscUtils;

@Repository
public class EnvDao
{	
	public List<Env> find (EnvFilter filter, EnvData env) throws KommetException
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT id, name, adminid, kid FROM envs");
		
		if (filter == null)
		{
			filter = new EnvFilter();
		}
		
		List<String> conditions = new ArrayList<String>();
		
		if (filter.getId() != null)
		{
			conditions.add("id = " + filter.getId());
		}
		if (StringUtils.hasText(filter.getName()))
		{
			conditions.add("name = '" + filter.getName() + "'");
		}
		if (filter.getKID() != null)
		{
			conditions.add("kid = '" + filter.getKID().getId() + "'");
		}
		
		if (!conditions.isEmpty())
		{
			sql.append(" WHERE " + MiscUtils.implode(conditions, " AND "));
		}
		
		SqlRowSet rowSet = env.getJdbcTemplate().queryForRowSet(sql.toString());
		
		List<Env> envs = new ArrayList<Env>();
		while (rowSet.next())
		{
			try
			{
				envs.add(getEnvFromRowSet(rowSet));
			}
			catch (InvalidResultSetAccessException e)
			{
				throw new KommetException("Error creating env object from row set: " + e.getMessage());
			}
		}
		
		return envs;
	}

	private Env getEnvFromRowSet(SqlRowSet rowSet) throws InvalidResultSetAccessException, KommetException
	{
		Env env = new Env();
		env.setName(rowSet.getString("name"));
		env.setKID(KID.get(rowSet.getString("kid")));
		env.setId(rowSet.getLong("id"));
		env.setAdminId(KID.get(rowSet.getString("adminid")));
		return env;
	}
}