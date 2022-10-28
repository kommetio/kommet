/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility methods for operating on dates.
 * @author Radek Krawiec
 * @since 26/04/2015
 */
public class DateTimeUtil
{
	/**
	 * Get the hour from the date.
	 * @param date The date
	 * @param timezone The timezone in which the given date is.
	 * @return
	 */
	public static int getHours (Date date, String timezone)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone(timezone));
		return cal.get(Calendar.HOUR_OF_DAY);
	}
	
	public static int getMinutes (Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.MINUTE);
	}
	
	public static int getSeconds (Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.SECOND);
	}
	
	public static int getMilliseconds (Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.MILLISECOND);
	}
}