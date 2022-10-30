/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import kommet.data.KommetException;
import kommet.i18n.Locale;

public class TextDataType extends DataType
{
	private Integer length;
	private boolean isLong;
	private boolean isFormatted;
	
	public TextDataType(int length)
	{
		this(length, false, false);
	}
	
	public TextDataType(int length, boolean isLong, boolean isFormatted)
	{
		super(TEXT);
		this.length = length;
		this.isFormatted = isFormatted;
		this.isLong = isLong;
	}
	
	public TextDataType()
	{
		super(TEXT);
	}

	public void setLength(Integer length)
	{
		this.length = length;
	}

	public Integer getLength()
	{
		return length;
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
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to text");
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
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to text");
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
		return "Text";
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

	public boolean isLong()
	{
		return isLong;
	}

	public void setLong(boolean isLong)
	{
		this.isLong = isLong;
	}

	public boolean isFormatted()
	{
		return isFormatted;
	}

	public void setFormatted(boolean isFormatted)
	{
		this.isFormatted = isFormatted;
	}
}