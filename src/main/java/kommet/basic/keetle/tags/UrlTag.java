/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.data.KommetException;

public class UrlTag extends KommetTag
{
	private static final long serialVersionUID = -2785983008799886405L;
	private String url;

	public UrlTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		if (!StringUtils.hasText(this.url))
		{
			return exitWithTagError("URL parameter must not be empty");
		}
		
		try
		{
			this.pageContext.getOut().write("<a href=\"" + this.pageContext.getServletContext().getContextPath() + "/" + this.url + "\">");
		}
		catch (IOException e)
		{
			return exitWithTagError("Cannot render URL tag: " + e.getMessage());
		}
		
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		try
		{
			this.pageContext.getOut().write("</a>");
		}
		catch (IOException e)
		{
			return exitWithTagError("Cannot render URL tag: " + e.getMessage());
		}
		return EVAL_PAGE;
    }

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getUrl()
	{
		return url;
	}
}
