/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

public class BaseConverter
{
	private static final char[] baseElements = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
										'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
										'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
	
	public static String convertToKommetBase (long sequence)
	{
		String result = new String();
		
		if (sequence == 0)
		{
			return "" + baseElements[0];
		}
		
		while (sequence > 0)
		{
			result = baseElements[(int)(sequence % 26)] + result;
			sequence /= 26;
		}
		
		return result;
	}
}