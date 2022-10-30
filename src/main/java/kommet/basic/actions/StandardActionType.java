/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.actions;

import kommet.data.KommetException;

public enum StandardActionType
{
	VIEW,
	LIST,
	EDIT,
	CREATE;

	public static StandardActionType fromString(String val) throws KommetException
	{
		if ("view".equals(val.toLowerCase()))
		{
			return StandardActionType.VIEW;
		}
		else if ("list".equals(val.toLowerCase()))
		{
			return StandardActionType.LIST;
		}
		else if ("edit".equals(val.toLowerCase()))
		{
			return StandardActionType.EDIT;
		}
		else if ("create".equals(val.toLowerCase()))
		{
			return StandardActionType.CREATE;
		}
		else
		{
			throw new KommetException("Value '" + val + "' cannot be converted to StandardActionType");
		}
	}

	public String getStringValue()
	{
		switch (this)
		{
			case VIEW: return "View";
			case LIST: return "List";
			case EDIT: return "Edit"; 
			case CREATE: return "Create"; 
		}
		
		return null;
	}
}
