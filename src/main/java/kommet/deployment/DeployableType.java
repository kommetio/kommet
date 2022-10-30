/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

import kommet.data.Type;

public class DeployableType extends Type
{
	private static final long serialVersionUID = -6090364552615507397L;
	
	private String defaultFieldApiName;

	@Override
	public String getDefaultFieldApiName()
	{
		return defaultFieldApiName;
	}

	public void setDefaultFieldApiName(String defaultFieldApiName)
	{
		this.defaultFieldApiName = defaultFieldApiName;
	}
}