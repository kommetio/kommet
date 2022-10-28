/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rel;

import java.math.BigDecimal;
import java.util.Date;

import kommet.data.KID;

public class RELFunctions
{
	public static boolean isEmpty(String s)
	{
		return s == null || s.length() == 0;
	}
	
	public static boolean not(boolean val)
	{
		return !val;
	}
	
	public static boolean isNull(Object o)
	{
		return o == null;
	}
	
	public static boolean isNotNull(Object o)
	{
		return o != null;
	}
	
	public static int getLength(String s)
	{
		return s != null ? s.length() : 0;
	}
	
	public static boolean eq (Object obj1, Object obj2)
	{
		if (obj1 instanceof Date && obj2 instanceof Date)
		{
			return ((Date)obj1).compareTo((Date)obj2) == 0;
		}
		else if (obj1 instanceof String && obj2 instanceof KID)
		{
			// convert KID to string before comparing
			return ((KID)obj2).getId().equals(obj1);
		}
		else if (obj2 instanceof String && obj1 instanceof KID)
		{
			// convert KID to string before comparing
			return ((KID)obj1).getId().equals(obj2);
		}
		
		return obj1 != null && obj1.equals(obj2);
	}
	
	public static boolean gt (BigDecimal obj1, BigDecimal obj2)
	{
		return obj1 != null && obj1.compareTo(obj2) > 0;
	}
	
	public static boolean gt (Object obj1, Object obj2) throws RELException
	{
		if (obj1 == null || obj2 == null)
		{
			return false;
		}
		
		if (obj1 instanceof Date && obj2 instanceof Date)
		{
			return gt((Date)obj1, (Date)obj2);
		}
		
		// convert both to big decimal
		return getBigDecimal(obj1).compareTo(getBigDecimal(obj2)) > 0;
	}
	
	private static BigDecimal getBigDecimal (Object o) throws RELException
	{
		if (o instanceof BigDecimal)
		{
			return (BigDecimal)o;
		}
		else if (o instanceof Double)
		{
			return BigDecimal.valueOf((Double)o);
		}
		else if (o instanceof Integer)
		{
			return BigDecimal.valueOf((Integer)o);
		}
		else if (o instanceof Long)
		{
			return BigDecimal.valueOf((Long)o);
		}
		else
		{
			throw new RELException("Non-numeric value " + o + " cannot be converted to number");
		}
	}
	
	public static boolean ge (BigDecimal obj1, BigDecimal obj2)
	{
		return obj1 != null && obj1.compareTo(obj2) >= 0;
	}
	
	public static boolean ge (Object obj1, Object obj2) throws RELException
	{
		if (obj1 == null || obj2 == null)
		{
			return false;
		}
		
		if (obj1 instanceof Date && obj2 instanceof Date)
		{
			return ge((Date)obj1, (Date)obj2);
		}
		
		// convert both to big decimal
		return getBigDecimal(obj1).compareTo(getBigDecimal(obj2)) >= 0;
	}
	
	public static boolean lt (BigDecimal obj1, BigDecimal obj2)
	{
		return obj1 != null && obj1.compareTo(obj2) < 0;
	}
	
	public static boolean lt (Object obj1, Object obj2) throws RELException
	{
		if (obj1 == null || obj2 == null)
		{
			return false;
		}
		
		if (obj1 instanceof Date && obj2 instanceof Date)
		{
			return lt((Date)obj1, (Date)obj2);
		}
		
		// convert both to big decimal
		return getBigDecimal(obj1).compareTo(getBigDecimal(obj2)) < 0;
	}
	
	public static boolean le (BigDecimal obj1, BigDecimal obj2)
	{
		return obj1 != null && obj1.compareTo(obj2) <= 0;
	}
	
	public static boolean le (Date d1, Date d2)
	{
		return d1 != null && !d1.after(d2);
	}
	
	public static boolean lt (Date d1, Date d2)
	{
		return d1 != null && d1.before(d2);
	}
	
	public static boolean gt (Date d1, Date d2)
	{
		return d1 != null && d1.after(d2);
	}
	
	public static boolean ge (Date d1, Date d2)
	{
		return d1 != null && !d1.before(d2);
	}
	
	public static boolean le (Object obj1, Object obj2) throws RELException
	{
		if (obj1 == null || obj2 == null)
		{
			return false;
		}
		
		if (obj1 instanceof Date && obj2 instanceof Date)
		{
			return le((Date)obj1, (Date)obj2);
		}
		
		// convert both to big decimal
		return getBigDecimal(obj1).compareTo(getBigDecimal(obj2)) <= 0;
	}
	
	public static boolean ne (BigDecimal obj1, BigDecimal obj2)
	{
		return obj1 != null && obj1.compareTo(obj2) != 0;
	}
	
	public static boolean ne (Object obj1, Object obj2) throws RELException
	{
		if (obj1 == null || obj2 == null)
		{
			return false;
		}
		
		// convert both to big decimal
		return getBigDecimal(obj1).compareTo(getBigDecimal(obj2)) != 0;
	}
}