/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.utils.XMLUtil;

public class UserMenuItemTag extends KommetTag
{
	private static final long serialVersionUID = -7931923700821078158L;

	public UserMenuItemTag() throws KommetException
	{
		super();
	}

	private String type;
	private String label;
	private String labelKey;
	private String url;
	private String cssStyle;
	private String cssClass;
	
	/**
	 * name of the icon file located in the resources/images directory, or full URL (if contains slash)
	 */
	private String icon;
	
	@Override
    public int doStartTag() throws JspException
    {
		UserMenuTag menu = (UserMenuTag)findAncestorWithClass(this, UserMenuTag.class);
		if (menu == null)
		{
			return exitWithTagError("Tag " + this.getClass().getSimpleName() + " is not placed within a view wrapper tag");
		}
		
		if (!StringUtils.hasText(this.icon) && StringUtils.hasText(menu.getIcon()))
		{
			// inherit icon from menu
			this.icon = menu.getIcon();
		}
		
		String actualLabel = null;
		Type typeToDisplay = null;
		ViewWrapperTag viewWrapper = null;
		
		try
		{
			viewWrapper = menu.getViewWrapper();
		}
		catch (MisplacedTagException e1)
		{
			return exitWithTagError("Misplaced tag menuItem");
		}
		
		if (!StringUtils.hasText(this.type))
		{
			actualLabel = this.label;
			if (!StringUtils.hasText(label))
			{
				if (StringUtils.hasText(this.labelKey))
				{
					actualLabel = AuthUtil.getAuthData(this.pageContext.getSession()).getI18n().get(this.labelKey);
				}
				else
				{
					return exitWithTagError("Neither label nor labelKey specified on user menu item");
				}
			}
		}
		else
		{
			try
			{
				typeToDisplay = viewWrapper.getEnv().getType(this.type);
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error rendering menu item: " + e.getMessage());
			}
			
			if (typeToDisplay == null)
			{
				return exitWithTagError("Type " + this.type + " referenced in menu item tag does not exist");
			}
		}
		
		try
		{
			menu.addItem(getCode(typeToDisplay, this.icon, actualLabel, this.url, this.cssClass, this.cssStyle, getHost(), viewWrapper.getAuthData()));
		}
		catch (Exception e)
		{
			return exitWithTagError("Error rendering menu item: " + e.getMessage());
		}
		
		return EVAL_PAGE;
    }

	private static String getCode(Type type, String iconFile, String label, String url, String cssClass, String cssStyle, String contextPath, AuthData authData)
	{
		StringBuilder code = new StringBuilder("<li>");
		
		String iconLink = iconFile.contains("/") ? iconFile : (contextPath + "/resources/images/" + iconFile);
		
		String icon = "<span class=\"km-menu-icon\"><img src=\"" + iconLink + "\"></img></span>";
		
		if (type != null)
		{
			code.append("<a href=\"").append(contextPath).append("/").append(type.getKeyPrefix()).append("\"");
			
			XMLUtil.attr("class", cssClass, code);
			XMLUtil.attr("style", cssStyle, code);
			
			code.append(">").append(icon).append("<span class=\"km-menu-item-title\">").append(type.getInterpretedPluralLabel(authData)).append("</span></a>");
		}
		else
		{
			String actualURL = "";
			if (url != null)
			{
				if (url.startsWith("http://") || url.startsWith("https://"))
				{
					actualURL = url;
				}
				else
				{
					actualURL = contextPath + "/" + url; 
				}
			}
			
			code.append("<a href=\"").append(actualURL).append("\"");
			
			XMLUtil.attr("class", cssClass, code);
			XMLUtil.attr("style", cssStyle, code);
			
			code.append(">").append(icon).append("<span class=\"km-menu-item-title\">").append(label).append("</span></a>");
		}
		
		code.append("</li>");
		return code.toString();
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getType()
	{
		return type;
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

	public void setCssStyle(String cssStyle)
	{
		this.cssStyle = cssStyle;
	}

	public String getCssStyle()
	{
		return cssStyle;
	}

	public void setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
	}

	public String getCssClass()
	{
		return cssClass;
	}

	public String getIcon()
	{
		return icon;
	}

	public void setIcon(String icon)
	{
		this.icon = icon;
	}
}
