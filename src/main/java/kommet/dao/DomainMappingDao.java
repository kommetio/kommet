/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.Set;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import kommet.data.DomainMapping;
import kommet.data.Env;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

@Repository
public class DomainMappingDao
{
	/**
	 * Finds a domain mapping for the given URL.
	 * @param url
	 * @param sharedEnv
	 * @return
	 * @throws KommetException
	 */
	public DomainMapping getForURL (String url, EnvData sharedEnv) throws KommetException
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT id, url, envid FROM domainmappings WHERE url = '" + url + "'");
		
		SqlRowSet rowSet = sharedEnv.getJdbcTemplate().queryForRowSet(sql.toString());
		DomainMapping result = null;
		
		int resultCount = 0;
		while (rowSet.next())
		{
			if (resultCount > 0)
			{
				throw new KommetException("More than one domain mapping found for URL '" + url + "'");
			}
			result = getDomainMappingFromRowSet(rowSet);
			resultCount++;
		}
		
		return result;
	}
	
	/**
	 * Saves a domain mapping on a shared server.
	 * @param mapping
	 * @param sharedEnv
	 * @return
	 */
	public DomainMapping save (DomainMapping mapping, EnvData sharedEnv)
	{
		StringBuilder query = new StringBuilder("INSERT INTO domainmappings (url, envid) VALUES (");
		query.append("'").append(mapping.getUrl()).append("', '").append(mapping.getEnv().getKID().getId()).append("') RETURNING id");
		
		Long id = sharedEnv.getJdbcTemplate().queryForObject(query.toString(), Long.class);
		mapping.setId(id);
		
		return mapping;
	}
	
	private DomainMapping getDomainMappingFromRowSet(SqlRowSet rowSet) throws InvalidResultSetAccessException, KommetException
	{
		DomainMapping mapping = new DomainMapping();
		mapping.setUrl(rowSet.getString("url"));
		
		Env env = new Env();
		env.setKID(KID.get(rowSet.getString("envid")));
		mapping.setEnv(env);
	
		return mapping;
	}

	public void deleteForUrls(Set<String> urls, EnvData sharedEnv)
	{
		if (urls.isEmpty())
		{
			return;
		}
		
		sharedEnv.getJdbcTemplate().execute("delete from domainmappings where url in (" + MiscUtils.implode(urls, ",", "'") + ")");
	}
}