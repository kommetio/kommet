/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.validationrules;

public class ValidationRuleError
{
	private String message;
	private String messageLabel;

	public void setMessage(String message)
	{
		this.message = message;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessageLabel(String messageLabel)
	{
		this.messageLabel = messageLabel;
	}

	public String getMessageLabel()
	{
		return messageLabel;
	}
}