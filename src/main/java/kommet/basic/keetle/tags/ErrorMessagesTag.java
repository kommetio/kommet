/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.PageData;
import kommet.data.KommetException;
import kommet.i18n.I18nDictionary;

public class ErrorMessagesTag extends KommetTag
{
	private static final long serialVersionUID = 573899296901397363L;

	public ErrorMessagesTag() throws KommetException
	{
		super();
	}
	
	public static String getCode(List<String> msgs, String contextPath, int maxMessages, I18nDictionary i18n)
	{	
		if (!msgs.isEmpty())
		{
			StringBuilder html = new StringBuilder();
			html.append("<div class=\"msg-tag action-errors\"><table><tbody><tr><td>");
			html.append("<img src=\"" + contextPath + "/resources/images/erricon.png" + "\" /></td><td>");
			html.append("<ul>");
			
			int i = 0;
			
			for (String msg : msgs)
			{
				html.append("<li");
				
				if (++i > maxMessages)
				{
					// hide this message
					html.append(" class=\"hidden\"");
				}
				
				html.append(">" + msg + "</li>");
			}
			
			// if there are more messages than allowed, show "More" button
			if (i > maxMessages)
			{
				html.append("<li style=\"padding-top:10px\"><a href=\"javascript:;\" onclick=\"$('div.action-errors li.hidden').toggle();\">" + i18n.get("msgs.more") + " (" + (i - maxMessages) + ")</a></li>");
			}
			
			html.append("</ul></td></tr></tbody></table></div>");
			return html.toString();
		}
		else
		{
			return "";
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {	
		PageData pageData = getPageData();
		if (pageData == null)
		{
			throw new JspException("Page data not available in action messages tag");
		}
		
		List<String> msgs = (ArrayList<String>)pageData.getValue(PageData.ERROR_MSGS_KEY);
			
		try
		{
			this.pageContext.getOut().write(getCode(msgs, pageContext.getServletContext().getContextPath(), getViewWrapper().getAppConfig().getMaxMessagesDisplayed(), getViewWrapper().getAuthData().getI18n()));
		}
		catch (IOException e)
		{
			throw new JspException("Error writing to JSP page: " + e.getMessage());
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error rendering messages tag: " + e.getMessage());
		}
		
		return EVAL_PAGE;
    }
}
