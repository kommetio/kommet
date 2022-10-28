/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

public class KommetDataValidation
{
	public static boolean isFieldAutoCreated(String apiName)
	{
		if (Field.ID_FIELD_NAME.equals(apiName))
		{
			return true;
		}
		if (Field.CREATEDDATE_FIELD_NAME.equals(apiName))
		{
			return true;
		}
		if (Field.CREATEDBY_FIELD_NAME.equals(apiName))
		{
			return true;
		}
		if (Field.LAST_MODIFIED_BY_FIELD_NAME.equals(apiName))
		{
			return true;
		}
		if (Field.LAST_MODIFIED_DATE_FIELD_NAME.equals(apiName))
		{
			return true;
		}
		if (Field.ACCESS_TYPE_FIELD_NAME.equals(apiName))
		{
			return true;
		}

		return false;
	}
}