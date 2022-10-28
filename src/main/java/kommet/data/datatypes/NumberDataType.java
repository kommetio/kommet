/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import java.math.BigDecimal;

import kommet.data.InvalidFieldValueException;
import kommet.data.KommetException;
import kommet.exceptions.NotImplementedException;
import kommet.i18n.Locale;
import kommet.utils.NumberFormatUtil;


public class NumberDataType extends DataType
{
	private Integer decimalPlaces;
	private char decimalSeparator;
	private String javaType;
	
	public NumberDataType(int decimalPlaces, Class<?> javaType)
	{
		super(NUMBER);
		this.decimalPlaces = decimalPlaces;
		this.decimalSeparator = '.';
		this.javaType = javaType.getName();
	}
	
	public NumberDataType()
	{
		super(NUMBER);
	}

	public void setDecimalPlaces(Integer decimalPlaces)
	{
		this.decimalPlaces = decimalPlaces;
	}

	public Integer getDecimalPlaces()
	{
		return decimalPlaces;
	}

	@Override
	public String getPostgresValue (Object value) throws KommetException
	{
		if (value == null)
		{
			return "null";
		}
		else if (value instanceof BigDecimal)
		{
			return value != null ? ((BigDecimal)value).toPlainString() : "null";
		}
		else if (value instanceof Integer)
		{
			return value != null ? String.valueOf((Integer)value) : "null";
		}
		else if (value instanceof Double)
		{
			return value != null ? String.valueOf((Double)value) : "null";
		}
		else if (value instanceof Long)
		{
			return value != null ? String.valueOf((Long)value) : "null";
		}
		else if (value instanceof String)
		{
			// convert to big decimal to make sure it is a valid number, then back to string
			return String.valueOf(new BigDecimal((String)value));
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return "null";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to number");
		}
	}
	
	@Override
	public String getStringValue (Object value, Locale locale) throws KommetException
	{
		if (value == null)
		{
			return "";
		}
		else if (value instanceof BigDecimal)
		{
			return NumberFormatUtil.format((BigDecimal)value, decimalPlaces, locale);
		}
		else if (value instanceof Integer)
		{
			return String.valueOf((Integer)value).replace('.', decimalSeparator);
		}
		else if (value instanceof Double)
		{
			return String.valueOf((Double)value).replace('.', decimalSeparator);
		}
		else if (value instanceof Long)
		{
			return String.valueOf((Long)value).replace('.', decimalSeparator);
		}
		else if (value instanceof String)
		{
			// convert to int to make sure it is a valid integer, then back to string
			return String.valueOf(Integer.parseInt((String)value)).replace('.', decimalSeparator);
		}
		else if (SpecialValue.isNull(value))
		{
			return "";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " cannot be cast to number");
		}
	}
	
	@Override
	public Object getJavaValue (String value) throws KommetException
	{
		return getJavaValue((Object)value);
	}
	
	public static boolean isValidJavaType (String javaType)
	{
		return BigDecimal.class.getName().equals(javaType) || Double.class.getName().equals(javaType) || Integer.class.getName().equals(javaType) || Long.class.getName().equals(javaType);
	}

	@Override
	public Object getJavaValue(Object value) throws KommetException
	{
		if (value instanceof BigDecimal)
		{
			if (this.javaType.equals(BigDecimal.class.getName()))
			{
				return (BigDecimal)value;
			}
			else if (this.javaType.equals(Double.class.getName()))
			{
				return ((BigDecimal)value).doubleValue();
			}
			else if (this.javaType.equals(Integer.class.getName()))
			{
				return ((BigDecimal)value).intValue();
			}
			else if (this.javaType.equals(Long.class.getName()))
			{
				return ((BigDecimal)value).longValue();
			}
			else
			{
				throw new KommetException("Unsupported Java return type " + this.javaType);
			}
		}
		else if (value instanceof Integer)
		{
			if (this.javaType.equals(BigDecimal.class.getName()))
			{
				return new BigDecimal((Integer)value);
			}
			else if (this.javaType.equals(Double.class.getName()))
			{
				return ((Integer)value).doubleValue();
			}
			else if (this.javaType.equals(Integer.class.getName()))
			{
				return (Integer)value;
			}
			else if (this.javaType.equals(Long.class.getName()))
			{
				return ((Integer)value).longValue();
			}
			else
			{
				throw new KommetException("Unsupported Java return type " + this.javaType);
			}
		}
		else if (value instanceof Double)
		{
			if (this.javaType.equals(BigDecimal.class.getName()))
			{
				return new BigDecimal((Double)value);
			}
			else if (this.javaType.equals(Double.class.getName()))
			{
				return ((Double)value).doubleValue();
			}
			else if (this.javaType.equals(Integer.class.getName()))
			{
				return ((Double)value).intValue();
			}
			else if (this.javaType.equals(Long.class.getName()))
			{
				return ((Double)value).longValue();
			}
			else
			{
				throw new KommetException("Unsupported Java return type " + this.javaType);
			}
		}
		else if (value instanceof Long)
		{
			if (this.javaType.equals(BigDecimal.class.getName()))
			{
				return new BigDecimal((Long)value);
			}
			else if (this.javaType.equals(Double.class.getName()))
			{
				return ((Long)value).doubleValue();
			}
			else if (this.javaType.equals(Integer.class.getName()))
			{
				return ((Long)value).intValue();
			}
			else if (this.javaType.equals(Long.class.getName()))
			{
				return (Long)value;
			}
			else
			{
				throw new KommetException("Unsupported Java return type " + this.javaType);
			}
		}
		else if (value instanceof String)
		{
			Object val = null;
			try
			{
				val = new BigDecimal(((String)value).replace(decimalSeparator, '.'));
			}
			catch (NumberFormatException e)
			{
				throw new InvalidFieldValueException("Invalid field value for type numeric: " + value);
			}
			
			if (this.javaType.equals(BigDecimal.class.getName()))
			{
				return (BigDecimal)val;
			}
			else if (this.javaType.equals(Double.class.getName()))
			{
				return ((BigDecimal)val).doubleValue();
			}
			else if (this.javaType.equals(Integer.class.getName()))
			{
				return ((BigDecimal)val).intValue();
			}
			else
			{
				throw new KommetException("Unsupported Java return type " + this.javaType);
			}
		}
		else if (value == null)
		{
			return null;
		}
		else if (isSpecialValueNull(value))
		{
			// nullify the property
			return SpecialValue.NULL;
		}
		else
		{
			throw new NotImplementedException("Numeric value type not supported: " + value.getClass().getName());
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
		return "Number";
	}
	
	/**
	 * Returns the qualified name of the Java type that can store properties of this data type.
	 * 
	 * This property can be used e.g. while generating object stubs for fields of this data type.
	 */
	@Override
	public String getJavaType()
	{
		return this.javaType;
	}
	
	public void setJavaType(String javaType)
	{
		this.javaType = javaType;
	}
	
	@Override
	public boolean isPrimitive()
	{
		return true;
	}

	public void setDecimalSeparator(char decimalSeparator)
	{
		this.decimalSeparator = decimalSeparator;
	}

	public char getDecimalSeparator()
	{
		return decimalSeparator;
	}
	
	@Override
	public boolean isCollection()
	{
		return false;
	}
}