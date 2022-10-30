/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Different string utility methods.
 * @author Radek Krawiec
 * @created 07-03-2014
 */
public class StringUtils
{
	/**
	 * Returns true is the given string is null or empty.
	 * @param s
	 * @return
	 */
	public static boolean isEmpty (String s)
	{
		return s == null || "".equals(s);
	}
	
	public static boolean hasText (String s)
	{
		return !isEmpty(s);
	}
	
	public static String escapeJava (String s)
	{
		return StringEscapeUtils.escapeJava(s);
	}
	
	public static String unescapeJava (String s)
	{
		return StringEscapeUtils.unescapeJava(s);
	}
}