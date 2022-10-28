/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

/**
 * Exception thrown when an error occurs during creation of a deployment package.
 * @author krawiecr
 * @since 11/03/2016
 */
public class PackageGenetionException extends DeploymentException
{
	private static final long serialVersionUID = 6335885913474883160L;

	public PackageGenetionException(String msg)
	{
		super(msg);
	}
}