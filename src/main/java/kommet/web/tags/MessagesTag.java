/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.tags;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract tag for displaying action messages (info or errors).
 * @author Radek Krawiec
 * @created 2013
 */
public abstract class MessagesTag implements Tag, Serializable
{
	private static final long serialVersionUID = -87399295385322316L;
	private Tag parent;
	private PageContext pageContext;
	private List<String> messages;
	private String cssClass;

	protected abstract String getTagSpecificCssClass();
	
	@Override
	public int doEndTag() throws JspException
	{
		return EVAL_PAGE;
	}
	
	public static String getCode (List<String> msgs, String tagSpecificCssClass, String cssClass, MessageTagType tagType, PageContext pageContext)
	{
		StringBuilder html = new StringBuilder();
		html.append("<table class=\"msg-tag ").append(tagSpecificCssClass);
		if (StringUtils.hasText(cssClass))
		{
			html.append(" ").append(cssClass);
		}
		html.append("\"><tr><td class=\"img-cell\">");
		
		if (tagType.equals(MessageTagType.INFO))
		{
			html.append("<img src=\"").append(pageContext.getServletContext().getContextPath()).append("/resources/images/infoicon.png\">");
		}
		else if (tagType.equals(MessageTagType.ERROR))
		{
			html.append("<img src=\"").append(pageContext.getServletContext().getContextPath()).append("/resources/images/erricon.png\">");
		}
		
		html.append("</td><td><ul>");
		for (String error : msgs)
		{
			html.append("<li>").append(error).append("</li>");
		}
		
		return html.append("</ul></td></tr></table>").toString();
	}
	
	protected abstract MessageTagType getTagType();

	@Override
	public int doStartTag() throws JspException
	{
		try
		{
			if (!CollectionUtils.isEmpty(this.messages))
			{
				JspWriter page = pageContext.getOut();
				page.write(getCode(this.messages, getTagSpecificCssClass(), this.cssClass, getTagType(), this.pageContext));
			}
		}
		catch (IOException e)
		{
			throw new JspTagException("An IOException occurred.");
		}
		return SKIP_BODY;
	}

	@Override
	public Tag getParent()
	{
		return parent;
	}

	@Override
	public void release()
	{
		this.pageContext = null;
		this.parent = null;
		this.messages = null;
	}

	@Override
	public void setPageContext(PageContext pc)
	{
		this.pageContext = pc;
	}

	@Override
	public void setParent(Tag parent)
	{
		this.parent = parent;
	}

	public void setMessages(List<String> messages)
	{
		this.messages = messages;
	}

	public List<String> getMessages()
	{
		return messages;
	}

	public void setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
	}

	public String getCssClass()
	{
		return cssClass;
	}
}