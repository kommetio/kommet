/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import kommet.basic.Dictionary;
import kommet.data.KommetException;
import kommet.exceptions.NotImplementedException;
import kommet.i18n.Locale;
import kommet.utils.MiscUtils;

public class EnumerationDataType extends DataType
{
	private String values;
	private boolean validateValues;
	private Dictionary dictionary;
	
	public EnumerationDataType()
	{
		super(ENUMERATION);
	}
	
	public EnumerationDataType (String values) throws KommetException
	{
		super(ENUMERATION);
		if (values.length() > 1024)
		{
			throw new KommetException("Enumeration value list cannot be longer than 1024 characters. Its actual value is " + values.length());
		}
		this.setValues(values);
	}
	
	public EnumerationDataType (Dictionary dictionary) throws KommetException
	{
		super(ENUMERATION);
		this.dictionary = dictionary;
	}

	@Override
	public String getPostgresValue (Object value) throws KommetException
	{
		if (value == null)
		{
			return "null";
		}
		if (value instanceof String)
		{
			return "'" + value + "'";
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return "null";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to enumeration");
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
			if (((String)value).contains("\n"))
			{
				throw new FieldValueException("Enumeration value " + value + " cannot contain new line characters");
			}
			return (String)value;
		}
		else
		{
			throw new NotImplementedException("Enumeration value type not supported: " + value.getClass().getName());
		}
	}

	public void setValues(String values)
	{
		this.values = values;
	}
	
	public List<String> getValueList()
	{
		return StringUtils.hasText(this.values) ? MiscUtils.toList(values.split("\\r?\\n")) : new ArrayList<String>();
	}

	public String getValues()
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
		return "Enumeration";
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

	public boolean isValidateValues()
	{
		return validateValues;
	}

	public void setValidateValues(boolean validateValues)
	{
		this.validateValues = validateValues;
	}

	public Dictionary getDictionary()
	{
		return dictionary;
	}

	public void setDictionary(Dictionary dictionary)
	{
		this.dictionary = dictionary;
	}
}
