/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import kommet.data.KommetException;
import kommet.i18n.Locale;

public class EmailDataType extends DataType
{
	public EmailDataType()
	{
		super(EMAIL);
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
	public String getPostgresValue(Object value) throws KommetException
	{
		if (value instanceof String || value == null)
		{
			return value != null ? "'" + (String)value + "'" : "null";
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return "null";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to email");
		}
	}
	
	@Override
	public String getStringValue(Object value, Locale locale) throws KommetException
	{
		if (value instanceof String || value == null)
		{
			return value != null ? (String)value : "";
		}
		else if (SpecialValue.isNull(value))
		{
			return "";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to email");
		}
	}
	
	@Override
	public boolean isTransient()
	{
		return false;
	}
	
	public String getName()
	{
		return "Email";
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
}