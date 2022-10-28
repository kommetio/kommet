/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.buttons;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.auth.AuthUtil;
import kommet.basic.keetle.tags.FormTag;
import kommet.basic.keetle.tags.ObjectDetailsTag;
import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;
import kommet.utils.XMLUtil;

public class ButtonTag extends KommetTag
{
	private static final long serialVersionUID = -624529244380890061L;
	
	// literal label (not i18n)
	private String label;
	
	// i18n label key
	private String labelKey;
	private String url;
	private String onClick;
	private String id;

	// Button type. Allowed values are: "custom" (default), "submit", "new"
	private String type;

	public ButtonTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		String actualLabel = label;
		
		if (StringUtils.hasText(labelKey))
		{
			actualLabel = AuthUtil.getAuthData(pageContext.getSession()).getI18n().get(this.labelKey);
		}
		
		ButtonPrototype button = new Button(ButtonType.CUSTOM);
		
		boolean appendPageContext = true;
		
		// get button type
		if ("submit".equals(this.type))
		{	
			button = new Button(ButtonType.SUBMIT);
			
			// if button type is submit, it has to be placed within a form tag
			FormTag form = (FormTag)findAncestorWithClass(this, FormTag.class);
			if (form == null)
			{
				return exitWithTagError("Button with type 'submit' has to be placed within a form tag");
			}
			
			String saveOnClick = "document.getElementById('" + form.getId() + "').action = '" + this.pageContext.getServletContext().getContextPath() + "/" + this.url + "'; document.getElementById('" + form.getId() + "').submit();";
			this.onClick = StringUtils.hasText(this.onClick) ? (this.onClick + "; " + saveOnClick) : saveOnClick;
			this.url = "javascript:;";
			appendPageContext = false;
		}
		else if ("new".equals(this.type))
		{
			button = new ButtonPrototype(ButtonType.NEW);
		}
		else if ("cancel".equals(this.type))
		{
			button = new ButtonPrototype(ButtonType.CANCEL);
		}
		else if ("edit".equals(this.type))
		{
			button = new ButtonPrototype(ButtonType.EDIT);
		}
		else if ("delete".equals(this.type))
		{
			button = new ButtonPrototype(ButtonType.DELETE);
		}
		
		// if any attribute is defined for the button, this is no longer a prototype,
		// but a custom button
		if (StringUtils.hasText(this.url) || StringUtils.hasText(this.onClick))
		{
			button = Button.fromPrototype(button);
		
			StringBuilder code = new StringBuilder("<a class=\"sbtn\"");
			
			if (StringUtils.hasText(this.url))
			{
				String actualUrl = "";
				
				if (appendPageContext)
				{
					actualUrl = this.pageContext.getServletContext().getContextPath();
					if (!this.url.startsWith("/"))
					{
						actualUrl += "/";
					}
				}
				
				actualUrl += this.url;
			
				//((Button)button).setUrl(actualUrl);
				XMLUtil.attr("href", actualUrl, code);
			}
			
			XMLUtil.attr("onclick", this.onClick, code);
			code.append(">").append(actualLabel).append("</a>");
			
			((Button)button).setLabel(actualLabel);
			((Button)button).setOnClick(this.onClick);
			((Button)button).setUrl(this.url);
			((Button)button).setCode(code.toString());
			button.setId(this.id);
		}
		
		KommetTag parentTag = (KommetTag)findAncestorWithClass(this, KommetTag.class);
		if (parentTag != null)
		{
			if (parentTag instanceof ObjectDetailsTag)
			{
				((ObjectDetailsTag)parentTag).addButton(button);
			}
			else if (parentTag instanceof ButtonPanelTag)
			{
				((ButtonPanelTag)parentTag).addButton(button);
			}
		}
		else
		{
			if (button instanceof Button)
			{
				writeToPage(((Button)button).getCode());
			}
			else
			{
				return exitWithTagError("Standard button of type " + this.type + " cannot be rendered outside of an objectDetails or buttons tag");
			}
		}
		
		cleanUp();
		return EVAL_PAGE;
    }
	
	@Override
	public void cleanUp()
	{
		this.label = null;
		this.labelKey = null;
		this.url = null;
		this.onClick = null;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabelKey(String labelKey)
	{
		this.labelKey = labelKey;
	}

	public String getLabelKey()
	{
		return labelKey;
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

	public void setOnClick(String onClick)
	{
		this.onClick = onClick;
	}

	public String getOnClick()
	{
		return onClick;
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
