/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.ArrayList;
import java.util.List;


public class FieldValidationException extends KommetException
{
	private static final long serialVersionUID = 4833249505343567446L;
	private List<ValidationMessage> messages;
	
	public FieldValidationException()
	{
		super(null);
	}

	public FieldValidationException(String msg)
	{
		super(msg);
	}
	
	@Override
	public String getMessage()
	{
		StringBuilder msg = new StringBuilder(super.getMessage() != null ? super.getMessage() : "");
		if (this.messages != null && !this.messages.isEmpty())
		{
			for (ValidationMessage m : messages)
			{
				msg.append(m.getText()).append("\n");
			}
		}
		return msg.toString();
	}
	
	public FieldValidationException(String msg, String uiMessage, KID fieldId, String fieldLabel, ValidationErrorType errorType)
	{
		super(msg);
		addMessage(uiMessage, fieldId, fieldLabel, errorType);
	}

	public void addMessage(String msg, KID fieldId, String fieldLabel, ValidationErrorType errorType)
	{
		if (this.messages == null)
		{
			this.messages = new ArrayList<ValidationMessage>();
		}
		this.messages.add(new ValidationMessage(msg, fieldId, fieldLabel, errorType));
	}

	public List<ValidationMessage> getMessages()
	{
		return messages;
	}
}