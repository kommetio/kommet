/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.associationpanel;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;

public class AssociationPanelButtonTag extends KommetTag
{
	private static final long serialVersionUID = 3671206702375226329L;
	
	private String url;
	private String type;
	private String buttonImageURL;
	private String linkText;
	private String cssClass;
	private String cssStyle;
	
	public AssociationPanelButtonTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		AssociationPanelTag assocTag = (AssociationPanelTag)findAncestorWithClass(this, AssociationPanelTag.class);
		
		if (assocTag == null)
		{
			return exitWithTagError("Association panel button tag is not placed within associationPanel tag");
		}
		
		PanelButton button = new PanelButton();
		button.setButtonImageURL(this.buttonImageURL);
		
		Integer type = null;
		if (!StringUtils.hasText(this.type))
		{
			return exitWithTagError("Type attribute not set on tag panelButton");
		}
		else if ("button".equals(this.type))
		{
			type = PanelButton.BUTTON_TYPE_BUTTON;
		}
		else if ("link".equals(this.type))
		{
			type = PanelButton.BUTTON_TYPE_TEXT_LINK;
			if (!StringUtils.hasText(this.linkText))
			{
				return exitWithTagError("Link text has to be set on association panel button with type 'link'");
			}
		}
		else
		{
			return exitWithTagError("Unsupported association panel button type " + this.type);
		}
		
		button.setType(type);
		button.setUrl(this.url);
		button.setLinkText(this.linkText);
		button.setCssClass(this.cssClass);
		button.setCssStyle(this.cssStyle);
		assocTag.addButton(button);
		
		return EVAL_BODY_INCLUDE;
    }

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getUrl()
	{
		return url;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getType()
	{
		return type;
	}

	public void setButtonImageURL(String buttonImageURL)
	{
		this.buttonImageURL = buttonImageURL;
	}

	public String getButtonImageURL()
	{
		return buttonImageURL;
	}
	
	public void setLinkText(String linkText)
	{
		this.linkText = linkText;
	}

	public String getLinkText()
	{
		return linkText;
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
