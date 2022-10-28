/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

/**
 * Represents a nullified record, i.e. a record value of an type reference field that has been explicitly
 * set to null.
 * @author Radek Krawiec
 */
public class NullifiedRecord extends Record
{
	public NullifiedRecord(Type type) throws KommetException
	{
		super(type);
	}
}