/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import kommet.data.KommetException;
import kommet.i18n.Locale;

/**
 * Provides number formatting based on user's locale settings.
 * @author Radek Krawiec
 * @since 29/03/2015
 */
public class NumberFormatUtil
{
	/**
	 * Converts a string number in locale-specific format to universal representation.
	 * E.g. locale specific string "11,3" will be parsed to "11.3".
	 * @param number
	 * @param locale
	 * @return
	 * @throws KommetException 
	 */
	public static String parseLocaleSpecificNumber (String number, Locale locale) throws KommetException
	{
		if (number == null)
		{
			return null;
		}
		
		if ("".equals(number))
		{
			throw new KommetException("Invalid number string: empty string");
		}
		
		if (locale == Locale.PL_PL)
		{
			return number.replaceAll(",", ".");
		}
		else
		{
			// return unformatted, but remove any commas, e.g. in numbers like "11,000.31"
			return number.replaceAll(",", "");
		}
	}
	
	public static Character getLocaleSpecificNumberDelimer (Locale locale)
	{
		return Locale.PL_PL == locale ? ',' : '.';
	}
	
	public static String getLocaleSpecificNumberFormat (Locale locale)
	{
		String delimeterRegex = String.valueOf(getLocaleSpecificNumberDelimer(locale));
		if (".".equals(delimeterRegex))
		{
			// escape the dot
			delimeterRegex = "\\" + delimeterRegex;
		}
		
		return "^(([1-9]\\d*)|0)(" + delimeterRegex + "\\d+)?$";
	}
	
	public static String format (BigDecimal number, int decimalPlaces, Locale locale) throws KommetException
	{
		if (number == null)
		{
			return null;
		}
		
		char decimalSeparator = getLocaleSpecificNumberDelimer(locale);
		
		return ((BigDecimal)number).setScale(decimalPlaces, RoundingMode.HALF_UP).toPlainString().replace('.', decimalSeparator);
	}
}