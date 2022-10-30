/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import java.util.HashSet;
import java.util.Set;

import kommet.data.KommetException;
import kommet.exceptions.NotImplementedException;
import kommet.i18n.Locale;
import kommet.utils.MiscUtils;

/**
 * A data type that can hold a set of string values.
 * @author Radek Krawiec
 * @since 03/04/2015
 */
public class MultiEnumerationDataType extends DataType
{
	/**
	 * String values separated by a new line character.
	 */
	private Set<String> values;
	
	public MultiEnumerationDataType()
	{
		super(MULTI_ENUMERATION);
		values = new HashSet<String>();
	}
	
	public MultiEnumerationDataType (Set<String> values) throws KommetException
	{
		super(MULTI_ENUMERATION);
		
		if (values != null)
		{
			for (String val : values)
			{
				if (val.length() > 255)
				{
					throw new KommetException("Multi-enumeration value cannot be longer than 255 characters. Its actual value is " + val.length());
				}
			}
		}
		
		this.setValues(values);
	}

	/**
	 * In Postgres, the values are stored as an array of strings.
	 */
	@Override
	public String getPostgresValue (Object value) throws KommetException
	{
		if (value == null)
		{
			return "null";
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return "null";
		}
		else if (value instanceof Set)
		{
			return "'{" + MiscUtils.implode((Set<?>)value, ", ", "\"") + "}'";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to multi-enumeration");
		}
	}
	
	@Override
	public String getStringValue (Object value, Locale locale) throws KommetException
	{
		if (value == null)
		{
			return "";
		}
		if (value instanceof String)
		{
			return (String)value;
		}
		else if (SpecialValue.isNull(value))
		{
			return "";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to enumeration");
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
		if (value == null)
		{
			return null;
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return SpecialValue.NULL;
		}
		// make sure the enumeration value does not contain newline characters
		else if (value instanceof String)
		{
			// split values by new line
			String[] subvalues = ((String)value).split("\\r?\\n");
			
			Set<String> valueSet = new HashSet<String>();
			for (String subvalue : subvalues)
			{
				valueSet.add(subvalue);
			}
			
			return valueSet;
		}
		else if (value instanceof Set)
		{
			return value;
		}
		else
		{
			throw new NotImplementedException("Multi-enumeration value type not supported: " + value.getClass().getName());
		}
	}

	public void setValues(Set<String> values)
	{
		this.values = values;
	}

	public Set<String> getValues()
	{
		return values;
	}
	
	@Override
	public boolean isTransient()
	{
		return false;
	}
	
	@Override
	public String getName()
	{
		return "Multi-Enumeration";
	}
	
	/**
	 * Returns the qualified name of the Java type that can store properties of this data type.
	 * 
	 * This property can be used e.g. while generating object stubs for fields of this data type.
	 */
	@Override
	public String getJavaType()
	{
		return "java.util.ArrayList<" + String.class.getName() + ">";
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