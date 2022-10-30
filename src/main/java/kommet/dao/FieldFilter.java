/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import kommet.data.KID;
import kommet.data.datatypes.DataType;

public class FieldFilter
{
	private DataType dataType;
	private KID objectRefTypeId;
	private String typeQualifiedName;
	
	// type to which this field belongs
	private Integer typeId;
	
	private KID typeKID;
	
	// field name
	private String apiName;
	
	// If dataType is Association, this variable can specify the ID of the associated type
	private KID associatedTypeId;
	
	private KID formulaFieldId;
	private KID dictionaryId;

	public void setDataType(DataType dataType)
	{
		this.dataType = dataType;
	}

	public DataType getDataType()
	{
		return dataType;
	}

	public void setObjectRefTypeId(KID objectRefTypeId)
	{
		this.objectRefTypeId = objectRefTypeId;
	}

	public KID getObjectRefTypeId()
	{
		return objectRefTypeId;
	}

	public void setAssociatedTypeId(KID associatedTypeId)
	{
		this.associatedTypeId = associatedTypeId;
	}

	public KID getAssociatedTypeId()
	{
		return associatedTypeId;
	}

	public Integer getTypeId()
	{
		return typeId;
	}

	public void setTypeId(Integer typeId)
	{
		this.typeId = typeId;
	}

	public String getApiName()
	{
		return apiName;
	}

	public void setApiName(String apiName)
	{
		this.apiName = apiName;
	}

	public KID getTypeKID()
	{
		return typeKID;
	}

	public void setTypeKID(KID typeKID)
	{
		this.typeKID = typeKID;
	}

	public String getTypeQualifiedName()
	{
		return typeQualifiedName;
	}

	public void setTypeQualifiedName(String typeQualifiedEnvName)
	{
		this.typeQualifiedName = typeQualifiedEnvName;
	}

	public KID getFormulaFieldId()
	{
		return formulaFieldId;
	}

	public void setFormulaFieldId(KID formulaFieldId)
	{
		this.formulaFieldId = formulaFieldId;
	}

	public KID getDictionaryId()
	{
		return dictionaryId;
	}

	public void setDictionaryId(KID dictionaryId)
	{
		this.dictionaryId = dictionaryId;
	}
}