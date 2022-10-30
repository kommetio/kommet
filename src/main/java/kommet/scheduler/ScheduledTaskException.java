/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.scheduler;

import kommet.data.ExceptionErrorType;
import kommet.data.KommetException;

public class ScheduledTaskException extends KommetException
{
	private static final long serialVersionUID = -1177913073734731034L;
	private int errorCode;
	
	public ScheduledTaskException(String msg, ExceptionErrorType errType)
	{
		super(msg, errType);
	}
	
	public ScheduledTaskException(String msg)
	{
		super(msg);
	}

	public void setErrorCode(int errorCode)
	{
		this.errorCode = errorCode;
	}

	public int getErrorCode()
	{
		return errorCode;
	}
}