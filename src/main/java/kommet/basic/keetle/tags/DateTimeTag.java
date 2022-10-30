/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.IOException;
import java.util.Date;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import kommet.auth.AuthUtil;
import kommet.data.KommetException;
import kommet.utils.MiscUtils;

public class DateTimeTag extends TagSupport
{
	private static final long serialVersionUID = -829598886273901702L;
	
	private Date value;
	private String format;
	
	public DateTimeTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		try
		{	
			this.pageContext.getOut().write(this.value != null ? MiscUtils.formatDateTimeByUserLocale(this.value, AuthUtil.getAuthData(pageContext.getSession())) : "");
		}
		catch (IOException e)
		{
			throw new JspException("Error writing to JSP page: " + e.getMessage());
		}
		catch (KommetException e)
		{
			throw new JspException("Error rendering date time tag: " + e.getMessage());
		}
		
		return EVAL_PAGE;
    }

	public void setValue(Date value)
	{
		this.value = value;
	}

	public Date getValue()
	{
		return value;
	}

	public void setFormat(String format)
	{
		this.format = format;
	}

	public String getFormat()
	{
		return format;
	}

}
