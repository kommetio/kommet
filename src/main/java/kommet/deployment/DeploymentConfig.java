/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

public class DeploymentConfig
{
	private OverwriteHandling overwriteHandling;
	private String packagePrefix;
	
	public DeploymentConfig (OverwriteHandling oh)
	{
		this.overwriteHandling = oh;
	}

	public OverwriteHandling getOverwriteHandling()
	{
		return overwriteHandling;
	}

	public void setOverwriteHandling(OverwriteHandling overwriteHandling)
	{
		this.overwriteHandling = overwriteHandling;
	}

	public String getPackagePrefix()
	{
		return packagePrefix;
	}

	public void setPackagePrefix(String packagePrefix)
	{
		this.packagePrefix = packagePrefix;
	}
}