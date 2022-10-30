/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.actions;

import kommet.data.KommetException;

/**
 * Exception thrown when an error occurs while saving action.
 * @author Radek Krawiec
 *
 */
public class ActionCreationException extends KommetException
{
	private static final long serialVersionUID = 8380463789928679791L;
	
	public static final int ERR_CODE_OTHER = 0;
	public static final int ERR_CODE_DUPLICATE_GENERIC_ACTION_URL = 1;
	public static final int ERR_CODE_DUPLICATE_REGISTERED_ACTION_URL = 2;
	public static final int ERR_CODE_RESERVED_URL = 3;
	
	private int errCode;

	public ActionCreationException(String msg, int errCode)
	{
		super(msg);
		this.errCode = errCode;
	}

	public int getErrCode()
	{
		return errCode;
	}
}
