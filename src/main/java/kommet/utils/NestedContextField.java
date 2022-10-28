/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;

/**
 * This class represents a field whose name is interpreted in some context, e.g. when this field is used
 * as a nested field.
 * 
 * @author Radek Krawiec
 * @created 24-03-2014
 */
public class NestedContextField
{
	private String nestedName;
	private Field field;
	
	/**
	 * Constructor
	 * @param field
	 * @param originalName - original full name, e.g. "createdBy.userName"
	 * @throws KommetException
	 */
	public NestedContextField (Type type, String originalName, EnvData env) throws KommetException
	{
		this.field = type.getField(originalName, env);
		
		if (this.field == null)
		{
			throw new KommetException("Field " + type.getQualifiedName() + "." + originalName + " does not exist");
		}
		
		if (field.getDataType().isPrimitive())
		{
			this.nestedName = originalName;
		}
		else if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
		{
			Type nestedType = env.getType(((TypeReference)field.getDataType()).getType().getKID());
			this.nestedName = originalName + "." + nestedType.getDefaultFieldApiName();
		}
		else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
		{
			Type nestedType = env.getType(((InverseCollectionDataType)field.getDataType()).getInverseType().getKID());
			this.nestedName = originalName + "." + nestedType.getDefaultFieldApiName();
		}
		else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			Type nestedType = env.getType(((AssociationDataType)field.getDataType()).getAssociatedType().getKID());
			this.nestedName = originalName + "." + nestedType.getDefaultFieldApiName();
		}
	}

	/**
	 * Nested name of a field is its fully qualified name that should be queried.
	 * For primitive types this will be simply the API name.
	 * For type references and collections this will be the API name plus the default field of the nested type, e.g. "createdBy.userName", "projects.name"
	 * @return
	 */
	public String getNestedName()
	{
		return nestedName;
	}

	public Field getField()
	{
		return field;
	}
}