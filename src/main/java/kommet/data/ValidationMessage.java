/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

public class ValidationMessage
{	
	private String text;
	private String fieldLabel;
	private ValidationErrorType errorType;
	private KID fieldId;
	
	public ValidationMessage (String text, KID fieldId, String fieldLabel, ValidationErrorType errorType)
	{
		this.fieldLabel = fieldLabel;
		this.text = text;
		this.errorType = errorType;
		this.fieldId = fieldId;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public String getText()
	{
		return text;
	}

	public void setFieldLabel(String fieldLabel)
	{
		this.fieldLabel = fieldLabel;
	}

	public String getFieldLabel()
	{
		return fieldLabel;
	}

	public void setErrorType(ValidationErrorType errorType)
	{
		this.errorType = errorType;
	}

	public ValidationErrorType getErrorType()
	{
		return errorType;
	}

	public KID getFieldId()
	{
		return fieldId;
	}

	public void setFieldId(KID fieldId)
	{
		this.fieldId = fieldId;
	}
}