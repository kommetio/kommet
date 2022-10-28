/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import kommet.data.KommetException;
import kommet.i18n.Locale;
import kommet.utils.MiscUtils;

public class DateTimeDataType extends DataType
{
	public DateTimeDataType()
	{
		super(DATETIME);
	}

	@Override
	public String getPostgresValue (Object value) throws KommetException
	{
		if (value != null && value instanceof Date)
		{
			return "'" + MiscUtils.formatPostgresDateTime((Date)value) + "'";
		}
		else if (value == null)
		{
			return "null";
		}
		else if (value instanceof String)
		{
			boolean isTimestamp = false;
			
			// check if the string value is a number
			if (StringUtils.isNumeric((String)value))
			{
				value = new Date(Long.parseLong((String)value));
				isTimestamp = true;
			}
			
			// Try to parse the string to date. Use a substring of the format pattern, because the pattern
			// includes minutes and seconds, and the input date may be just e.g. '2013-05-24'.
			String format = "yyyy-MM-dd HH:mm:ss.SSS";
			SimpleDateFormat sdf = new SimpleDateFormat(!isTimestamp ? format.substring(0, ((String)value).length()) : format);
			try
			{
				return "'" + sdf.format(isTimestamp ? (Date)value : sdf.parse((String)value)) + "'";
			}
			catch (ParseException e)
			{
				throw new KommetException("Cannot convert value " + value + " to datetime");
			}
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return "null";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to datetime");
		}
	}
	
	@Override
	public String getStringValue (Object value, Locale locale) throws KommetException
	{
		if (value != null && value instanceof Date)
		{
			return MiscUtils.formatPostgresDateTime((Date)value);
		}
		else if (value == null)
		{
			return "";
		}
		else if (value instanceof String)
		{
			// Try to parse the string to date. Use a substring of the format pattern, because the pattern
			// includes minutes and seconds, and the input date may be just e.g. '2013-05-24'.
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS".substring(0, ((String)value).length()));
			try
			{
				return sdf.format(sdf.parse((String)value));
			}
			catch (ParseException e)
			{
				throw new KommetException("Cannot convert value " + value + " to datetime");
			}
		}
		else if (SpecialValue.isNull(value))
		{
			return "";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to datetime");
		}
	}
	
	@Override
	public Object getJavaValue (String value) throws KommetException
	{
		return getJavaValue((Object)value);
	}

	@Override
	public Object getJavaValue (Object value) throws KommetException
	{
		if (isSpecialValueNull(value))
		{
			// nullify the property
			return SpecialValue.NULL;
		}
		else if (value instanceof Long)
		{
			return new Date((Long)value);
		}
		else if (value instanceof Integer)
		{
			return new Date((Integer)value);
		}
		else if (value instanceof String)
		{
			try
			{
				return MiscUtils.parseDateTime((String)value, true);
			}
			catch (ParseException e)
			{
				throw new KommetException("Date cannot be parsed from string '" + value + "'. Nested: " + e.getMessage());
			}
		}
		else
		{
			return (Date)value;
		}
	}
	
	@Override
	public boolean isTransient()
	{
		return false;
	}
	
	public String getName()
	{
		return "Date/Time";
	}
	
	/**
	 * Returns the qualified name of the Java type that can store properties of this data type.
	 * 
	 * This property can be used e.g. while generating object stubs for fields of this data type.
	 */
	@Override
	public String getJavaType()
	{
		return Date.class.getName();
	}
	
	@Override
	public boolean isPrimitive()
	{
		return true;
	}
	
	@Override
	public boolean isCollection()
	{
		return false;
	}
}