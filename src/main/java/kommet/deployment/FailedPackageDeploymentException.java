/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;


import kommet.data.KommetException;

public class FailedPackageDeploymentException extends KommetException
{
	private static final long serialVersionUID = 6658015063121011550L;
	private PackageDeploymentStatus status;
	
	public FailedPackageDeploymentException(PackageDeploymentStatus status)
	{
		super("Deployment failed");
		this.status = status;
	}

	public PackageDeploymentStatus getStatus()
	{
		return status;
	}
}