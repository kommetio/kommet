/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.dal;

import kommet.data.KommetException;

public class CannotModifyAccessTypeException extends KommetException
{
	private static final long serialVersionUID = 5293764404842609133L;
	public static final String CANNOT_MODIFY_ACCESS_TYPE_MSG = "Cannot modify access type on record";

	public CannotModifyAccessTypeException(String msg)
	{
		super(msg);
	}
}