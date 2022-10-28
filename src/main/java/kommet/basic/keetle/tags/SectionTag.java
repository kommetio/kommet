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

public class SectionTag extends KommetTag
{
	private static final long serialVersionUID = -2397018589133508163L;
	private String title;
	private String id;
	private String cssClass;
	private String cssStyle;

	public SectionTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		writeToPage("<div class=\"km-rd-body km-property-section km-grid-col km-grid-span-2-of-2");
		
		if (StringUtils.hasText(this.cssClass))
		{
			writeToPage(" " + this.cssClass);
		}
		
		// close class attribute
		writeToPage("\"");
		
		if (StringUtils.hasText(this.id))
		{
			writeToPage(" id=\"" + this.id + "\"");
		}
		
		if (StringUtils.hasText(this.cssStyle))
		{
			writeToPage(" style=\"" + this.cssStyle + "\"");
		}
		
		writeToPage(">");
		writeToPage("<div class=\"km-rd-row km-section-title\"><div class=\"km-rd-cell\">" + this.title + "</div></div>");
		
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {	
		// end section
		writeToPage("</div>");
		
		// add space under the section
		writeToPage("<div class=\"km-rd-row km-section-space km-grid-col km-grid-span-2-of-2\"><div class=\"km-rd-cell\">&nbsp;</div></div>");
		
		return EVAL_PAGE;
    }

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
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
}
