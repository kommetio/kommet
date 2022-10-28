/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;


public class ValidationError
{
	private Field field;
	private String message;
	private ValidationErrorType errorType;
	
	public ValidationError (Field field, String msg, ValidationErrorType errorType)
	{
		this.field = field;
		this.message = msg;
		this.errorType = errorType;
	}
	
	public void setField(Field field)
	{
		this.field = field;
	}
	public Field getField()
	{
		return field;
	}
	public void setMessage(String message)
	{
		this.message = message;
	}
	public String getMessage()
	{
		return message;
	}

	public void setErrorType(ValidationErrorType errorType)
	{
		this.errorType = errorType;
	}

	public ValidationErrorType getErrorType()
	{
		return errorType;
	}
}