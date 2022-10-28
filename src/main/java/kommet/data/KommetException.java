/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

public class KommetException extends Exception
{
	private static final long serialVersionUID = -4489165628619732852L;
	private ExceptionErrorType errorType;

	public KommetException (String msg)
	{
		super(msg);
	}
	
	public KommetException (String msg, ExceptionErrorType errorType)
	{
		super(msg);
		this.errorType = errorType;
	}
	
	public KommetException (String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public ExceptionErrorType getErrorType()
	{
		return errorType;
	}

	public void setErrorType(ExceptionErrorType errorType)
	{
		this.errorType = errorType;
	}
}