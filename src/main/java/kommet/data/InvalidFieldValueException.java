/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

public class InvalidFieldValueException extends KommetException
{
	private static final long serialVersionUID = 539989651251364778L;

	public InvalidFieldValueException(String msg)
	{
		super(msg);
	}
}