/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.js.jsti;

import java.util.ArrayList;
import java.util.List;

import kommet.auth.AuthData;
import kommet.basic.DictionaryItem;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;

public class JSTIDataType
{
	private Integer id;
	private KID inverseTypeId;
	private KID linkingTypeId;
	private KID associatedTypeId;
	private KID inverseFieldId;
	private KID selfLinkingFieldId;
	private KID foreignLinkingFieldId;
	private KID typeId;
	private List<String> enumValues;
	
	public JSTIDataType()
	{
		// empty constructor needed for deserialization
	}

	public JSTIDataType(DataType dataType, AuthData authData, EnvData env) throws KommetException
	{
		this.id = dataType.getId();
		
		if (DataType.INVERSE_COLLECTION == dataType.getId())
		{
			Type inverseType = env.getType(((InverseCollectionDataType)dataType).getInverseTypeId());
			this.inverseTypeId = inverseType.getKID();
			this.inverseFieldId = inverseType.getField(((InverseCollectionDataType)dataType).getInverseProperty()).getKID();
		}
		else if (DataType.ASSOCIATION == dataType.getId())
		{
			Type linkingType = env.getType(((AssociationDataType)dataType).getLinkingTypeId());
			this.linkingTypeId = linkingType.getKID();
			this.associatedTypeId = ((AssociationDataType)dataType).getAssociatedTypeId();
			this.selfLinkingFieldId = linkingType.getField(((AssociationDataType)dataType).getSelfLinkingField()).getKID();
			this.foreignLinkingFieldId = linkingType.getField(((AssociationDataType)dataType).getForeignLinkingField()).getKID();
		}
		else if (DataType.TYPE_REFERENCE == dataType.getId())
		{
			this.typeId = ((TypeReference)dataType).getTypeId();
		}
		else if (DataType.ENUMERATION == dataType.getId())
		{
			this.enumValues = new ArrayList<String>();
			
			EnumerationDataType dt = (EnumerationDataType)dataType;
			
			if (dt.getDictionary() != null)
			{
				this.enumValues = new ArrayList<String>();
				
				for (DictionaryItem item : dt.getDictionary().getItems())
				{
					String displayValue = authData.getUserCascadeSettings().get(item.getKey());
					if (displayValue == null)
					{
						displayValue = item.getName();
					}
					
					this.enumValues.add(displayValue);
				}
			}
			else
			{
				this.enumValues.addAll(dt.getValueList());
			}
		}
		else if (DataType.MULTI_ENUMERATION == dataType.getId())
		{
			this.enumValues = new ArrayList<String>();
			this.enumValues.addAll(((MultiEnumerationDataType)dataType).getValues());
		}
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public Integer getId()
	{
		return id;
	}

	public void setInverseTypeId(KID inverseTypeId)
	{
		this.inverseTypeId = inverseTypeId;
	}

	public KID getInverseTypeId()
	{
		return inverseTypeId;
	}

	public void setLinkingTypeId(KID linkingTypeId)
	{
		this.linkingTypeId = linkingTypeId;
	}

	public KID getLinkingTypeId()
	{
		return linkingTypeId;
	}

	public void setAssociatedTypeId(KID associatedTypeId)
	{
		this.associatedTypeId = associatedTypeId;
	}

	public KID getAssociatedTypeId()
	{
		return associatedTypeId;
	}

	public void setInverseFieldId(KID inverseFieldId)
	{
		this.inverseFieldId = inverseFieldId;
	}

	public KID getInverseFieldId()
	{
		return inverseFieldId;
	}

	public void setSelfLinkingFieldId(KID selfLinkingFieldId)
	{
		this.selfLinkingFieldId = selfLinkingFieldId;
	}

	public KID getSelfLinkingFieldId()
	{
		return selfLinkingFieldId;
	}

	public void setForeignLinkingFieldId(KID foreignLinkingFieldId)
	{
		this.foreignLinkingFieldId = foreignLinkingFieldId;
	}

	public KID getForeignLinkingFieldId()
	{
		return foreignLinkingFieldId;
	}

	public KID getTypeId()
	{
		return typeId;
	}

	public void setTypeId(KID typeId)
	{
		this.typeId = typeId;
	}

	public List<String> getEnumValues()
	{
		return enumValues;
	}

	public void setEnumValues(List<String> enumValues)
	{
		this.enumValues = enumValues;
	}
}