/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import kommet.data.NullifiedRecord;

/**
 * Contains special values for some data types.
 * @author Radek Krawiec
 * @date 18/05/2013
 */
public enum SpecialValue
{
	/**
	 * We need a special null value to explicitly state that we want a given field to be nullified.
	 * Otherwise on update if a field had Java-null value, we would not know if it was intentionally
	 * nullified or just uninitialized.
	 */
	NULL;

	public static boolean isNull (Object value)
	{
		return value != null && ((value instanceof SpecialValue && ((SpecialValue)value).equals(SpecialValue.NULL)) || value instanceof NullifiedRecord);
	}
}