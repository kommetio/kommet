/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.util.Properties;

public class PropertySet
{
	private Properties properties;

	public String getProperty (String propertiesFile, String name) throws PropertyUtilException
	{
		if (properties == null)
		{
			properties = new Properties();
			try
			{
				properties.load(PropertySet.class.getClassLoader().getResourceAsStream(propertiesFile));
			}
			catch (Exception e)
			{
				throw new PropertyUtilException("Could not load " + propertiesFile + " file: " + e.getMessage());
			}
		}
		
		return properties.getProperty(name);
	}
	
	public void clearCachedProperties()
	{
		this.properties = null;
	}
}