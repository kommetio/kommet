/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kommet.dao.FieldDefinitionException;
import kommet.data.KommetException;
import kommet.i18n.Locale;

public class AutoNumber extends DataType
{
	public static final Pattern AUTONUMBER_PATTERN = Pattern.compile("([A-z\\-]+[A-z]\\-)(\\{[0]+\\})");
	
	private String format;
	
	public AutoNumber(String format) throws KommetException
	{
		super(AUTO_NUMBER);
		this.setFormat(format);
	}
	
	public AutoNumber()
	{
		super(AUTO_NUMBER);
	}

	@Override
	public String getPostgresValue(Object value) throws KommetException
	{
		if (value == null || value instanceof String)
		{
			return value != null ? "'" + escape((String)value) + "'" : "null";
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return "null";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to auto number");
		}
	}
	
	@Override
	public String getStringValue(Object value, Locale locale) throws KommetException
	{
		if (value == null || value instanceof String)
		{
			return value != null ? (String)value : "";
		}
		else if (SpecialValue.isNull(value))
		{
			return "";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to auto number");
		}
	}
	
	private String escape (String value)
	{
		return value.replaceAll("'", "''");
	}

	@Override
	public Object getJavaValue (String value) throws KommetException
	{
		return getJavaValue((Object)value);
	}

	@Override
	public Object getJavaValue(Object value) throws KommetException
	{
		if (isSpecialValueNull(value))
		{
			// nullify the property
			return SpecialValue.NULL;
		}
		else
		{
			return (String)value;
		}
	}

	@Override
	public boolean isTransient()
	{
		return false;
	}
	
	@Override
	public String getName()
	{
		return "AutoNumber";
	}
	
	/**
	 * Returns the qualified name of the Java type that can store properties of this data type.
	 * 
	 * This property can be used e.g. while generating object stubs for fields of this data type.
	 */
	@Override
	public String getJavaType()
	{
		return String.class.getName();
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

	public String getFormat()
	{
		return format;
	}

	public void setFormat(String format) throws FieldDefinitionException
	{
		// we must allow for setting null format, because sometimes we're cloning the data type, and the Apache BeanUtils method that does this calls all setters with null
		if (format != null)
		{
			Matcher m = AutoNumber.AUTONUMBER_PATTERN.matcher(format);
			
			if (!m.find())
			{
				throw new FieldDefinitionException("Invalid auto-number format " + format);
			}
			// although we expect groups 0, 1 and 2, matcher will say groupCount is 2, not 3 (it does not count to overall 0-group)
			else if (m.groupCount() < 2)
			{
				throw new FieldDefinitionException("Invalid auto-number format does not contain prefix or/and numeric value: " + format);
			}
		}
		
		this.format = format;
	}
}