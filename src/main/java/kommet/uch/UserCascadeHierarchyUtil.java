/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.uch;

import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;

public class UserCascadeHierarchyUtil
{
	public static Field getUCHField (Type type, EnvData env) throws KeyPrefixException, KommetException
	{
		Type uchType = env.getType(KeyPrefix.get(KID.USER_CASCADE_HIERARCHY_PREFIX));
		Field uchField = null;
		for (Field field : type.getFields())
		{
			if (DataType.TYPE_REFERENCE == field.getDataTypeId() && uchType.getKID().equals(((TypeReference)field.getDataType()).getType().getKID()))
			{
				if (uchField == null)
				{
					uchField = field;
				}
				else
				{
					throw new KommetException("More than one User Cascade Sharing reference on type " + type.getQualifiedName());
				}
			}
		}
		
		return uchField;
	}
}