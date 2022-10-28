/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.tags;

import java.util.List;

import javax.servlet.jsp.PageContext;


public class ActionErrors extends MessagesTag
{	
	private static final long serialVersionUID = 7714280159043643511L;

	@Override
	protected String getTagSpecificCssClass()
	{
		return "action-errors";
	}
	
	public static String getCode(List<String> msgs, String cssClass, PageContext pageContext)
	{
		return getCode(msgs, "action-errors", cssClass, MessageTagType.ERROR, pageContext);
	}

	@Override
	protected MessageTagType getTagType()
	{
		return MessageTagType.ERROR;
	}
}
