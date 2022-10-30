/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.triggers;

import kommet.data.KommetException;

public class TriggerException extends KommetException
{
	private static final long serialVersionUID = 2959908351722384559L;
	
	public static final int TRIGGER_ERROR_OTHER = 0;
	public static final int TRIGGER_ERROR_NO_TRIGGER_TO_UNREGISTER = 1;
	
	private int errorCode;
	
	public TriggerException(String msg, int errorCode)
	{
		super(msg);
		this.errorCode = errorCode;
	}

	public TriggerException(String msg)
	{
		super(msg);
	}

	public int getErrorCode()
	{
		return errorCode;
	}
}