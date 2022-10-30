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

/**
 * Represents a tab on an object details view.
 * @author Radek Krawiec
 *
 */
public class ObjectDetailsTabTag extends KommetTag
{
	private static final long serialVersionUID = 5613081603221978456L;
	private String title;
	private String titleKey;
	
	// tells whether this component should be rendered
	private Boolean render;

	public ObjectDetailsTabTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		if (Boolean.FALSE.equals(this.render))
		{
			return SKIP_BODY;
		}
		else
		{
			return EVAL_BODY_BUFFERED;
		}
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		if (Boolean.FALSE.equals(this.render))
		{
			return EVAL_PAGE;
		}
		
		ObjectDetailsTag parentDetails = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		if (parentDetails == null)
		{
			return exitWithTagError("Tag objectDetailsTab is not placed within objectDetails tag");
		}
		
		if (StringUtils.hasText(this.titleKey))
		{
			this.title = parentDetails.getAuthData().getI18n().get(this.titleKey);
		}
		else if (!StringUtils.hasText(this.title))
		{
			return exitWithTagError("Neither title not titleKey attribute is set on tag objectDetailsTab");
		}
		
		if (getBodyContent() != null && StringUtils.hasText(getBodyContent().getString()))
		{
			String id = null;
			try
			{
				id = parentDetails.getParentView().nextComponentId();
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				return exitWithTagError("Error rendering object details tab: " + e.getMessage());
			}
			StringBuilder code = new StringBuilder();
			code.append("<div class=\"").append(parentDetails.getRelatedListCssClass()).append("\" id=\"").append(id).append("\">");
			code.append(getBodyContent().getString()).append("</div>");
			parentDetails.addTab(title, code.toString(), id, null);
		}
		
		return EVAL_PAGE;
    }
	
	@Override
	public void cleanUp()
	{
		this.title = null;
		this.titleKey = null;
	}
	
	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public void setTitleKey(String titleKey)
	{
		this.titleKey = titleKey;
	}

	public String getTitleKey()
	{
		return titleKey;
	}
	
	public Boolean getRender()
	{
		return render;
	}

	public void setRender(Boolean render)
	{
		this.render = render;
	}
}
