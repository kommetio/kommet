/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;


/**
 * This exception is thrown when an uninitialized field is accessed (read).
 * @author Radek Krawiec
 *
 */
public class UninitializedFieldException extends KommetException
{
	private static final long serialVersionUID = 3245196797983565461L;

	public UninitializedFieldException(String msg)
	{
		super(msg);
	}
}