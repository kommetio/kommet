/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import kommet.data.KID;
import kommet.data.KommetException;
import kommet.i18n.Locale;

public class KIDDataType extends DataType
{
	public KIDDataType()
	{
		super(KOMMET_ID);
	}

	@Override
	public String getPostgresValue(Object value) throws KommetException
	{
		if (value instanceof KID || value == null)
		{
			return value != null ? "'" + ((KID)value).getId() + "'" : "null";
		}
		else if (value instanceof String)
		{
			// return as string, but first parse to KID to check the ID is valid
			return "'" + KID.get((String)value).getId() + "'";
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return "null";
		}
		else
		{
			throw new KommetException("Value " + value + " of type " + value.getClass().getName() + " cannot be converted to KID");
		}
	}
	
	@Override
	public String getStringValue(Object value, Locale locale) throws KommetException
	{
		if (value instanceof KID || value == null)
		{
			return value != null ? ((KID)value).getId() : "";
		}
		else if (value instanceof String)
		{
			// return as string, but first parse to KID to check the ID is valid
			return KID.get((String)value).getId();
		}
		else if (SpecialValue.isNull(value))
		{
			return "";
		}
		else
		{
			throw new KommetException("Value " + value + " of type " + value.getClass().getName() + " cannot be converted to KID");
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
		
		if (value instanceof KID)
		{
			return (KID)value;
		}
		else if (value instanceof String)
		{
			return KID.get((String)value);
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return SpecialValue.NULL;
		}
		else
		{
			throw new KommetException("Value " + value + " of type " + value.getClass().getName() + " cannot be converted to KID");
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
		return "ID";
	}
	
	/**
	 * Returns the qualified name of the Java type that can store properties of this data type.
	 * 
	 * This property can be used e.g. while generating object stubs for fields of this data type.
	 */
	@Override
	public String getJavaType()
	{
		return KID.class.getName();
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