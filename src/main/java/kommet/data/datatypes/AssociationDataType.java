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

public class AssociationDataType extends DataType implements CollectionDataType
{
	private Type linkingType;
	private Type associatedType;
	private KID linkingTypeId;
	private KID associatedTypeId;
	private String selfLinkingField;
	private String foreignLinkingField;
	
	public AssociationDataType()
	{
		super(ASSOCIATION);
	}
	
	public AssociationDataType(Type linkingType, Type associatedType, String selfLinkingField, String foreignLinkingField)
	{
		super(ASSOCIATION);
		this.linkingType = linkingType;
		this.linkingTypeId = linkingType.getKID();
		this.associatedType = associatedType;
		this.associatedTypeId = associatedType.getKID();
		this.selfLinkingField = selfLinkingField;
		this.foreignLinkingField = foreignLinkingField;
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
		throw new KommetException("Association data type has no Postgres representation");
	}
	
	@Override
	public String getStringValue(Object value, Locale locale) throws KommetException
	{
		throw new KommetException("Association data type has no string representation");
	}
	
	@Override
	public String getJavaType()
	{
		return "java.util.ArrayList<" + this.associatedType.getQualifiedName() + ">";
	}
	
	@Override
	public Object getJavaValue (String value) throws KommetException
	{
		return getJavaValue((Object)value);
	}
	
	@Override
	public boolean isTransient()
	{
		return true;
	}
	
	@Override
	public boolean isPrimitive()
	{
		return false;
	}
	
	@Override
	public boolean isCollection()
	{
		return true;
	}
	
	public String getName()
	{
		return "Association";
	}

	public void setLinkingType(Type linkingType)
	{
		this.linkingType = linkingType;
		this.linkingTypeId = linkingType != null ? linkingType.getKID() : null;
	}


	public Type getLinkingType()
	{
		return linkingType;
	}


	public void setSelfLinkingField(String selfLinkingField)
	{
		this.selfLinkingField = selfLinkingField;
	}


	public String getSelfLinkingField()
	{
		return selfLinkingField;
	}


	public void setForeignLinkingField(String foreignLinkingField)
	{
		this.foreignLinkingField = foreignLinkingField;
	}


	public String getForeignLinkingField()
	{
		return foreignLinkingField;
	}


	public void setLinkingTypeId(KID linkingTypeId)
	{
		this.linkingTypeId = linkingTypeId;
	}


	public KID getLinkingTypeId()
	{
		return linkingTypeId;
	}

	public void setAssociatedType(Type associatedType)
	{
		this.associatedType = associatedType;
		this.associatedTypeId = associatedType != null ? associatedType.getKID() : null;
	}

	public Type getAssociatedType()
	{
		return associatedType;
	}

	public void setAssociatedTypeId(KID associatedTypeId)
	{
		this.associatedTypeId = associatedTypeId;
	}

	public KID getAssociatedTypeId()
	{
		return associatedTypeId;
	}

	@Override
	public Type getCollectionType()
	{
		return this.associatedType;
	}
}