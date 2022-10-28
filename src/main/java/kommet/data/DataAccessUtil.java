/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.utils.NestedContextField;

public class DataAccessUtil
{	
	/**
	 * Returns fields from a type that the given user (identified by authData) can edit.
	 * @param type
	 * @param authData
	 * @return
	 * @throws KommetException 
	 */
	public static List<Field> getEditableFields (Type type, AuthData authData, EnvData env) throws KommetException
	{
		List<Field> fields = new ArrayList<Field>();
		
		for (Field field : type.getFields())
		{
			if (authData.canEditField(field, true, env))
			{
				fields.add(field);
			}
		}
		
		return fields;
	}
	
	/**
	 * Returns API names of fields from a type that the given user (identified by authData) can read.
	 * @param type
	 * @param authData
	 * @param initDefaultFieldsOnRelationships - if set to true, not only the ID field, but also the default field for inverse collections and type references will be fetched
	 * @return
	 * @throws KommetException 
	 */
	public static List<String> getReadableFieldApiNamesForQuery (Type type, AuthData authData, EnvData env, boolean initDefaultFieldsOnRelationships) throws KommetException
	{
		List<String> fields = new ArrayList<String>();
		
		for (Field field : type.getFields())
		{
			if (authData.canReadField(field, true, env))
			{
				if (field.getDataType().isPrimitive())
				{
					fields.add(field.getApiName());
				}
				else if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					fields.add(field.getApiName() + "." + Field.ID_FIELD_NAME);
					
					// always query default fields
					if (initDefaultFieldsOnRelationships)
					{
						String defaultField = env.getType(((TypeReference)field.getDataType()).getTypeId()).getDefaultFieldApiName();
						if (!Field.ID_FIELD_NAME.equals(defaultField))
						{
							fields.add(field.getApiName() + "." + defaultField);
						}
					}
				}
				else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
				{
					fields.add(field.getApiName() + "." + Field.ID_FIELD_NAME);
					
					if (initDefaultFieldsOnRelationships)
					{
						// always query default fields
						String defaultField = env.getType(((InverseCollectionDataType)field.getDataType()).getInverseTypeId()).getDefaultFieldApiName();
						if (!Field.ID_FIELD_NAME.equals(defaultField))
						{
							fields.add(field.getApiName() + "." + defaultField);
						}
					}
				}
				else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
				{
					fields.add(field.getApiName() + "." + Field.ID_FIELD_NAME);
					
					if (initDefaultFieldsOnRelationships)
					{
						// always query default fields
						String defaultField = env.getType(((AssociationDataType)field.getDataType()).getAssociatedTypeId()).getDefaultFieldApiName();
						if (!Field.ID_FIELD_NAME.equals(defaultField))
						{
							fields.add(field.getApiName() + "." + defaultField);
						}
					}
				}
			}
		}
		
		return fields;
	}
	
	/**
	 * Returns API names of fields from a type that the given user (identified by authData) can edit.
	 * @param type
	 * @param authData
	 * @return
	 * @throws KommetException 
	 */
	public static List<String> getEditableFieldApiNames (Type type, AuthData authData, EnvData env) throws KommetException
	{
		List<String> fields = new ArrayList<String>();
		
		for (Field field : type.getFields())
		{
			if (authData.canEditField(field, true, env))
			{
				fields.add(field.getApiName());
			}
		}
		
		return fields;
	}
	
	/**
	 * Returns fields from a type that the given user (identified by authData) can read.
	 */
	public static List<NestedContextField> getReadableFields (Type type, AuthData authData, EnvData env) throws KommetException
	{
		return getReadableFields(type, authData, null, env);
	}

	public static List<NestedContextField> getReadableFields (Type type, AuthData authData, Collection<String> fieldApiNames, EnvData env) throws KommetException
	{
		Collection<NestedContextField> fields = new ArrayList<NestedContextField>();
		
		if (fieldApiNames != null)
		{
			for (String fieldName : fieldApiNames)
			{
				Field field = type.getField(fieldName, env);
				if (field != null)
				{
					fields.add(new NestedContextField(type, fieldName, env));
				}
				else
				{
					throw new KommetException("Field " + fieldName + " not found on type " + type.getQualifiedName());
				}
			}
		}
		else
		{
			for (Field field : type.getFields())
			{
				fields.add(new NestedContextField(type, field.getApiName(), env));
			}
		}
		
		List<NestedContextField> returnedFields = new ArrayList<NestedContextField>();
		// store system fields in a separate list to put them at the end of the returned list
		List<NestedContextField> systemFields = new ArrayList<NestedContextField>();
		
		// update permissions if necessary, and do this only once here so that later
		// in this method we can use canRead* methods that do not perform the update
		// for root updating is not necessary, because the root permissions are always the same
		if (!AuthUtil.isRoot(authData))
		{
			authData.updateFieldPermissions(true, env);
		}
		
		for (NestedContextField field : fields)
		{
			if (authData.canReadField(field.getField(), false, env))
			{
				if (Field.isSystemField(field.getField().getApiName()))
				{
					systemFields.add(field);
				}
				else
				{
					returnedFields.add(field);
				}
			}
		}
		
		returnedFields.addAll(systemFields);
		return returnedFields;
	}

	public static List<String> getFieldsNamesForDisplay(Type type, AuthData authData, Collection<String> fields, EnvData env) throws KommetException
	{
		List<NestedContextField> fieldsForDisplay = getReadableFields(type, authData, fields, env);
		List<String> apiNames = new ArrayList<String>();
		for (NestedContextField field : fieldsForDisplay)
		{
			apiNames.add(field.getNestedName());
		}
		
		return apiNames;
	}
}