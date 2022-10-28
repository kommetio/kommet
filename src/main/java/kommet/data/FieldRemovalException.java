/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

/**
 * Exception thrown when field cannot be removed for some reason.
 * @author Radek Krawiec
 * @since 28/06/2016
 */
public class FieldRemovalException extends KommetException
{
	private static final long serialVersionUID = -6934781060713740336L;

	public FieldRemovalException(String msg)
	{
		super(msg);
	}
}