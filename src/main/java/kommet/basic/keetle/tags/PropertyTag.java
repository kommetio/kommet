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

public class PropertyTag extends KommetTag
{
	private static final long serialVersionUID = 4728595248070010575L;
	private String cssClass;
	private String cssStyle;
	private String id;

	public PropertyTag() throws KommetException
	{
		super();
	}
	
	public static String getStartCode(String id, String cssClass, String cssStyle)
	{
		StringBuilder sb = new StringBuilder("<div class=\"km-rd-property km-grid-col km-grid-span-1-of-2\"");
		
		if (StringUtils.hasText(id))
		{
			sb.append(" id=\"" + id + "\"");
		}
		
		if (StringUtils.hasText(cssClass))
		{
			sb.append(" class=\"" + cssClass + "\"");
		}
		
		if (StringUtils.hasText(cssStyle))
		{
			sb.append(" style=\"" + cssStyle + "\"");
		}
		
		sb.append(">");
		return sb.toString();
	}
	
	public static String getEndCode()
	{
		return "</div>";
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		writeToPage(getStartCode(id, cssClass, cssStyle));
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		writeToPage(getEndCode());
		return EVAL_PAGE;
    }

	public String getCssClass()
	{
		return cssClass;
	}

	public void setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
	}

	public String getCssStyle()
	{
		return cssStyle;
	}

	public void setCssStyle(String cssStyle)
	{
		this.cssStyle = cssStyle;
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}
}
