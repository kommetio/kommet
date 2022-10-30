/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.data.KommetException;

public class PropertyRowTag extends KommetTag
{
	private static final long serialVersionUID = 7959236655069773120L;
	private String cssClass;
	private String cssStyle;
	private String id;

	public PropertyRowTag() throws KommetException
	{
		super();
	}
	
	public static String getStartTagCode (String id, String cssClass, String cssStyle) throws KommetException
	{
		StringBuilder code = new StringBuilder("<div class=\"km-rd-row km-grid-section km-grid-group");
		if (StringUtils.hasText(cssClass))
		{
			code.append(" ").append(cssClass);
		}
		code.append("\"");
		
		if (StringUtils.hasText(cssClass))
		{
			code.append(" style=\"").append(cssStyle).append("\"");
		}
		
		if (StringUtils.hasText(id))
		{
			code.append(" id=\"").append(id).append("\"");
		}
		
		code.append(">");
		return code.toString();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		if (!StringUtils.hasText(id))
		{
			// get next component ID for the page
			try
			{
				this.id = getParentView().nextComponentId();
			}
			catch (KommetException e)
			{
				return exitWithTagError(e.getMessage());
			}
		}
		
		try
		{
			writeToPage(getStartTagCode(id, cssClass, cssStyle));
		}
		catch (KommetException e)
		{
			return exitWithTagError(e.getMessage());
		}
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		writeToPage(getEndTagCode());
		this.id = null;
		this.cssClass = null;
		this.cssStyle = null;
		return EVAL_PAGE;
    }

	public void setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
	}

	public String getCssClass()
	{
		return cssClass;
	}

	public void setCssStyle(String cssStyle)
	{
		this.cssStyle = cssStyle;
	}

	public String getCssStyle()
	{
		return cssStyle;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public static String getEndTagCode()
	{
		return "</div>";
	}
}
