/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.util.StringUtils;

public class LoginPanel implements Tag
{
	private Tag parent;
	private PageContext pageContext;
	private String action;
	private String cssClass;

	private static final String DEFAULT_ACTION = "/auth/dologin";
	
	@Override
	public int doEndTag() throws JspException
	{
		return EVAL_PAGE;
	}

	@Override
	public int doStartTag() throws JspException
	{
		JspWriter page = pageContext.getOut();
		
		if (!StringUtils.hasText(this.action))
		{
			this.action = DEFAULT_ACTION;
		}
		
		try
		{
			page.write("<form method=\"post\" action=\"" + this.action + "\" class=\"" + (this.cssClass != null ? " " + this.cssClass : "") + "\">");
			page.write("<table>");
			page.write("<tr><td><input type=\"text\" name=\"j_username\" placeholder=\"Username\" /></td></tr>");
			page.write("<tr><td><input type=\"password\" name=\"j_password\" placeholder=\"Password\" /></td></tr>");
			page.write("<tr><td><input type=\"submit\" value=\"Sign in\" /></td></tr>");
			page.write("</table>");
			page.write("</form>");
		}
		catch (IOException e)
		{
			throw new JspException("Error rendering loginForm tag");
		}
		
		return SKIP_BODY;
	}

	@Override
	public Tag getParent()
	{
		return this.parent;
	}

	@Override
	public void release()
	{
		//
	}

	@Override
	public void setPageContext(PageContext pageContext)
	{
		this.pageContext = pageContext;
	}

	@Override
	public void setParent(Tag parent)
	{
		this.parent = parent;
	}

	public void setAction(String action)
	{
		this.action = action;
	}

	public String getAction()
	{
		return action;
	}
	
	public String getCssClass()
	{
		return cssClass;
	}

	public void setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
	}

}