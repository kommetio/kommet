/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import org.springframework.stereotype.Service;

import kommet.utils.PropertySet;
import kommet.utils.PropertyUtilException;

@Service
public class QueryService extends PropertySet
{
	private static final String QUERY_PROPERTIES_FILE = "queries.properties";
	
	public String getCreateEnvDBQuery() throws PropertyUtilException
	{
		return getProperty(QUERY_PROPERTIES_FILE, "query.createnvdb");
	}
	
	public String getCreateObjectsTableQuery() throws PropertyUtilException
	{
		return getProperty(QUERY_PROPERTIES_FILE, "query.createobjectstable");
	}
	
	public String getCreateFieldsTableQuery() throws PropertyUtilException
	{
		return getProperty(QUERY_PROPERTIES_FILE, "query.createfieldstable");
	}
	
	public String getCreateSettingsTableQuery() throws PropertyUtilException
	{
		return getProperty(QUERY_PROPERTIES_FILE, "query.createsettingstable");
	}
}