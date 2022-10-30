/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import kommet.data.NullifiedRecord;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.i18n.Locale;

public class TypeReference extends DataType
{
	private Type type;
	/**
	 * Tells whether a delete operation is cascaded through this relation. Cascading means that when
	 * the referenced record is deleted (i.e. the one denoted by the type property), this object is deleted as well.
	 */
	private boolean cascadeDelete = false;
	
	/**
	 * Kommet ID of the type. Needed when the whole referenced object cannot be accessed, e.g. when
	 * reading data type definition from database and the definition of the referenced object does
	 * not exist yet.
	 */
	private KID typeId;
	
	public TypeReference()
	{
		super(TYPE_REFERENCE);
	}
	
	public TypeReference(Type type) throws KommetException
	{
		super(TYPE_REFERENCE);
		
		if (type == null)
		{
			throw new KommetException("Cannot create object reference type with null type reference");
		}
		
		this.type = type;
		this.typeId = type.getKID();
	}
	
	@Override
	public Object getJavaValue (String value) throws KommetException
	{
		return getJavaValue((Object)value);
	}

	@Override
	public Object getJavaValue (Object value) throws KommetException
	{
		if (value instanceof Record || value == null)
		{
			return (Record)value;
		}
		else if (isSpecialValueNull(value))
		{
			return SpecialValue.NULL;
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " ('" + value + "') cannot be converted to an object reference");
		}
	}

	@Override
	public String getPostgresValue (Object value) throws KommetException
	{
		if (isSpecialValueNull(value))
		{
			// nullify the property
			return "null";
		}
		else if (value instanceof Record || value == null)
		{
			// the field value is stored as a whole type Record, but only the KID is saved
			// as the column value in the DB
			return value != null ? "'" + ((Record)value).getKID().getId() + "'" : "null";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " ('" + value + "') cannot be cast to object reference");
		}
	}
	
	@Override
	public String getStringValue (Object value, Locale locale) throws KommetException
	{
		if ((value instanceof Record && !(value instanceof NullifiedRecord)) || value == null)
		{
			// the field value is stored as a whole type Record, but only the KID is saved
			// as the column value in the DB
			return value != null ? (((Record)value).attemptGetKID() != null ? ((Record)value).getKID().getId() : "") : "";
		}
		else if (value instanceof NullifiedRecord)
		{
			return "";
		}
		else
		{
			throw new KommetException("Value of type " + value.getClass().getName() + " ('" + value + "') cannot be cast to object reference");
		}
	}

	public Type getType()
	{
		return type;
	}
	
	@Override
	public boolean isTransient()
	{
		return false;
	}

	public void setCascadeDelete(boolean cascadeDelete)
	{
		this.cascadeDelete = cascadeDelete;
	}

	public boolean isCascadeDelete()
	{
		return cascadeDelete;
	}

	public void setTypeId(KID typeId) throws KommetException
	{
		if (this.type != null)
		{
			throw new KommetException("Type ID cannot be set for an object reference when the type property is already set.");
		}
		this.typeId = typeId;
	}

	public KID getTypeId()
	{
		return typeId;
	}
	
	public void setType (Type type)
	{
		this.type = type;
		this.typeId = type != null ? type.getKID() : null;
	}
	
	@Override
	public String getName()
	{
		return "Type Reference";
	}

	/**
	 * Returns the qualified name of the Java type that can store properties of this data type.
	 * 
	 * This property can be used e.g. while generating object stubs for fields of this data type.
	 */
	@Override
	public String getJavaType()
	{
		return this.type.getQualifiedName();
	}
	
	@Override
	public boolean isPrimitive()
	{
		return false;
	}
	
	@Override
	public boolean isCollection()
	{
		return false;
	}
}