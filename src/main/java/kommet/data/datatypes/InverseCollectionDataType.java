/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import java.util.List;

import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.i18n.Locale;

public class InverseCollectionDataType extends DataType implements CollectionDataType
{
	private Type inverseType;
	private String inverseProperty;
	
	/**
	 * KID of the type. Needed when the whole referenced object cannot be accessed, e.g. when
	 * reading data type definition from database and the definition of the referenced object does
	 * not exist yet.
	 */
	private KID inverseTypeId;
	
	public InverseCollectionDataType()
	{
		super(INVERSE_COLLECTION);
	}
	
	public InverseCollectionDataType(Type inverseType, String inverseProperty)
	{
		super(INVERSE_COLLECTION);
		this.inverseType = inverseType;
		this.inverseTypeId = inverseType.getKID();
		this.inverseProperty = inverseProperty;
	}
	
	@Override
	public Object getJavaValue (String value) throws KommetException
	{
		return getJavaValue((Object)value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getJavaValue(Object value) throws KommetException
	{
		try
		{
			return (List<Record>)value;
		}
		catch (Exception e)
		{
			throw new KommetException("Object of type " + value.getClass().getName() + " cannot be cast to Record collection");
		}
	}

	@Override
	public String getPostgresValue(Object value) throws KommetException
	{
		throw new KommetException("Collection data type has no Postgres representation");
	}
	
	@Override
	public String getStringValue(Object value, Locale locale) throws KommetException
	{
		throw new KommetException("Collection data type has no string representation");
	}

	public void setInverseType(Type inverseType)
	{
		this.inverseType = inverseType;
		this.inverseTypeId = inverseType != null ? inverseType.getKID() : null;
	}

	public Type getInverseType()
	{
		return inverseType;
	}

	public void setInverseProperty(String inverseProperty)
	{
		this.inverseProperty = inverseProperty;
	}

	public String getInverseProperty()
	{
		return inverseProperty;
	}
	
	@Override
	public boolean isTransient()
	{
		return true;
	}
	
	public String getName()
	{
		return "Dependent Collection";
	}

	@Override
	public String getJavaType()
	{
		return "java.util.ArrayList<" + this.inverseType.getQualifiedName() + ">";
	}
	
	@Override
	public boolean isPrimitive()
	{
		return false;
	}

	public void setInverseTypeId(KID inverseTypeId) throws KommetException
	{
		if (this.inverseType != null)
		{
			throw new KommetException("Type ID cannot be set for an type reference when the type property is already set.");
		}
		this.inverseTypeId = inverseTypeId;
	}

	public KID getInverseTypeId()
	{
		return inverseTypeId;
	}
	
	@Override
	public boolean isCollection()
	{
		return true;
	}

	@Override
	public Type getCollectionType()
	{
		return this.inverseType;
	}
}