/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.util.Date;

import kommet.auth.AuthData;
import kommet.basic.Profile;
import kommet.data.Field;
import kommet.data.NoSuchFieldException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.TypeReference;

public class DataUtil
{
	/**
	 * Tells whether the given user can edit permissions on the field, basing on the field properties and user's
	 * profile.
	 * @param field
	 * @param authData
	 * @return
	 * @throws KIDException
	 */
	public static boolean canEditFieldPermissions (Field field, AuthData authData) throws KIDException
	{
		if (Field.isSystemField(field.getApiName()))
		{
			return false;
		}
		
		// only root can edit field permissions
		return authData.getProfile().getId().equals(KID.get(Profile.ROOT_ID));
	}
	
	/**
	 * Tells whether the given nested or simple property is a collection. If it is a nested
	 * property "one.two.three", the method returns true is any of the properties "one", "two"
	 * or "three" are nested.
	 * @param property
	 * @param type
	 * @return
	 * @throws KommetException 
	 */
	public static boolean isCollection (String property, Type type) throws KommetException
	{
		if (!property.contains("."))
		{
			Field field = type.getField(property);
			if (field == null)
			{
				throw new NoSuchFieldException("Field " + property + " not found on type " + type.getQualifiedName());
			}
			return field.getDataType().isCollection();
		}
		else
		{
			String firstProperty = property.substring(0, property.indexOf('.'));
			Field field = type.getField(firstProperty);
			if (field == null)
			{
				throw new NoSuchFieldException("Field " + firstProperty + " not found on type " + type.getQualifiedName());
			}
			
			DataType fieldDT = field.getDataType();
			if (fieldDT.isCollection())
			{
				return true;
			}
			else if (fieldDT.getId().equals(DataType.TYPE_REFERENCE))
			{
				return isCollection(property.substring(property.indexOf('.') + 1), ((TypeReference)fieldDT).getType());
			}
			else
			{
				throw new KommetException("Qualified property " + property + " contains a field that is neither a collection nor a reference");
			}
		}
	}
	
	public static Object formatValue (Object val, int dataTypeId, AuthData authData) throws KommetException
	{
		if (dataTypeId == DataType.DATETIME || dataTypeId == DataType.DATE)
		{
			return MiscUtils.formatDateTimeByUserLocale((Date)val, authData);
		}
		else 
		{
			return val;
		}
	}
}