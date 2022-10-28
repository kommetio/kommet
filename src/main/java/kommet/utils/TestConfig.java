/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import org.springframework.stereotype.Service;

import kommet.data.KID;
import kommet.data.KIDException;

@Service
public class TestConfig extends PropertySet
{
	private static final String TEST_PROPERTIES_FILE = "test.properties";
	
	public String getTestEnvDB() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.envdb");
	}
	
	public String getTestEnv2DB() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.env2db");
	}
	
	public String getTestEnv3DB() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.env3db");
	}

	public String getTestEnv() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.env");
	}
	
	public String getTestEnv2() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.env2");
	}
	
	public String getTestEnv3() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.env3");
	}
	
	public String getTestEnvDBUser() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.envdb.user");
	}
	
	public String getTestEnvDBPassword() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.envdb.password");
	}

	public String getTestEnvDBHost() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.envdb.host");
	}

	public String getTestEnvDBPort() throws PropertyUtilException
	{
		return getProperty(TEST_PROPERTIES_FILE, "test.envdb.port");
	}

	public KID getTestEnvId() throws KIDException, PropertyUtilException
	{
		return KID.get(getProperty(TEST_PROPERTIES_FILE, "test.env.id"));
	}
	
	public KID getTestEnv2Id() throws KIDException, PropertyUtilException
	{
		return KID.get(getProperty(TEST_PROPERTIES_FILE, "test.env2.id"));
	}
	
	public KID getTestEnv3Id() throws KIDException, PropertyUtilException
	{
		return KID.get(getProperty(TEST_PROPERTIES_FILE, "test.env3.id"));
	}
}