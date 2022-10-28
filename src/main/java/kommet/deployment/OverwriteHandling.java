/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

/**
 * Defines how duplicates are handled during deployment
 * @author Radek Krawiec
 * @since 08/04/2016
 */
public enum OverwriteHandling
{
	ALWAYS_OVERWRITE,
	ALWAYS_REJECT
}