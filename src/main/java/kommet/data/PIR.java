/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import org.springframework.util.StringUtils;

import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;

/**
 * PIR - Property ID Representation
 * @author Radek Krawiec
 * @created 02-09-2014
 */
public class PIR
{
	// the actual string value of the PIR, e.g. "00300000002dA.00300000345"
	private String value;
	
	public static PIR get (String nestedProperty, Type type, EnvData env) throws KommetException
	{
		if (nestedProperty.contains("."))
		{
			// split nested property
			String firstProperty = nestedProperty.substring(0, nestedProperty.indexOf('.'));
			String furtherProperties = nestedProperty.substring(nestedProperty.indexOf('.') + 1);
			
			Field field = type.getField(firstProperty);
			if (field == null)
			{
				throw new NoSuchFieldException("Field " + firstProperty + " not found on type " + type.getQualifiedName());
			}
			
			Type nestedType = null;
			
			if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				nestedType = ((TypeReference)field.getDataType()).getType();
			}
			else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
			{
				nestedType = ((InverseCollectionDataType)field.getDataType()).getInverseType();
			}
			else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				nestedType = ((AssociationDataType)field.getDataType()).getAssociatedType();
			}
			else
			{
				throw new PirException("Property " + firstProperty + " on type " + type.getQualifiedName() + " is not an type reference", nestedProperty);
			}
			
			return new PIR(type.getField(firstProperty).getKID().getId() + "." + get(furtherProperties, nestedType, env).getValue());
		}
		else
		{
			Field field = type.getField(nestedProperty);
			if (field == null)
			{
				throw new NoSuchFieldException("Field " + nestedProperty + " not found on type " + type.getQualifiedName());
			}
			return new PIR(field.getKID().getId());
		}
	}
	
	/**
	 * Takes a PIR sequence and returns the field object for the last element in the sequence.
	 * 
	 * Note: calling this method has the same result as calling:<p>
	 * <code>type.getField(JCRUtil.deserialize(type, prop.getId(), env))</code>
	 * </p>
	 * @param pir
	 * @param type
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static Field getField (PIR pir, Type type, EnvData env) throws KommetException
	{
		if (pir == null)
		{
			throw new PirException("Cannot deduce field from null PIR", null);
		}
		else if (!StringUtils.hasText(pir.getValue()))
		{
			throw new PirException("Cannot deduce field - PIR value is empty", null);
		}
		
		String sPIR = pir.getValue();
		
		if (sPIR.contains("."))
		{
			// split nested property
			String firstPropertyId = sPIR.substring(0, sPIR.indexOf('.'));
			String furtherPropertyIds = sPIR.substring(sPIR.indexOf('.') + 1);
			
			Field field = type.getField(KID.get(firstPropertyId));
			if (field == null)
			{
				throw new NoSuchFieldException("Field " + firstPropertyId + " not found on type " + type.getQualifiedName());
			}
			
			if (!field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				throw new PirException("Property " + firstPropertyId + " on type " + type.getQualifiedName() + " is not an type reference", sPIR);
			}
			
			Type nestedType = ((TypeReference)field.getDataType()).getType();
			return getField(new PIR(furtherPropertyIds), nestedType, env);
		}
		else
		{
			Field field = type.getField(KID.get(sPIR));
			if (field == null)
			{
				throw new NoSuchFieldException("Field " + sPIR + " not found on type " + type.getQualifiedName());
			}
			return field;
		}
	}
	
	public PIR (String value)
	{
		this.value = value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}
	
	@Override
	public String toString()
	{
		return this.value;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		
		PIR other = (PIR) obj;
		if (value == null)
		{
			if (other.value != null)
			{
				return false;
			}
		}
		else if (!value.equals(other.value))
		{
			return false;
		}
		return true;
	}
}