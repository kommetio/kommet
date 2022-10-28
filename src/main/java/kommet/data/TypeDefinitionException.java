/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

/**
 * This exception is thrown when a create/update/delete operation on an object type or field fails.
 * @author Radek Krawiec
 * @created 9-08-2013
 */
public class TypeDefinitionException extends KommetException
{
	private static final long serialVersionUID = 5938353409789194310L;

	public TypeDefinitionException (String msg)
	{
		super(msg);
	}
}