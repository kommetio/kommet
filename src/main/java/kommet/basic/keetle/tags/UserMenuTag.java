/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Profile;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.utils.UrlUtil;

public class UserMenuTag extends KommetTag
{
	private static final long serialVersionUID = -3626466119779108046L;
	private List<String> items;
	private Boolean renderAllTypes;
	private Boolean renderMyProfile;
	
	/**
	 * name of the icon file located in the resources/images directory, or full URL (if contains slash)
	 */
	private String icon;

	public UserMenuTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		this.items = new ArrayList<String>();
		
		if (!StringUtils.hasText(this.icon))
		{
			this.icon = "extlinkwhite.png";
		}
		
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		ViewWrapperTag viewWrapper = null;
		try
		{
			viewWrapper = getViewWrapper();
		}
		catch (MisplacedTagException e)
		{
			throw new JspException("Error getting view wrapper tag: " + e.getMessage());
		}
		
		try
		{
			writeToPage(getCode(viewWrapper, this.items, this.icon, this.renderAllTypes, this.renderMyProfile, this.pageContext));
		}
		catch (KommetException e)
		{
			viewWrapper.addErrorMsgs("Error rendering user menu: " + e.getMessage());
		}
		
		cleanUp();
		
		return EVAL_PAGE;
    }
	
	@Override
	protected void cleanUp()
	{
		super.cleanUp();
		this.items = null;
		this.icon = null;
		this.renderAllTypes = null;
		this.renderMyProfile = null;
	}
	
	private String getCode (ViewWrapperTag viewWrapper, List<String> menuItems, String icon, Boolean renderAllTypes, Boolean renderMyProfile, PageContext pageContext) throws KommetException
	{
		return getCode(menuItems, icon, renderAllTypes, renderMyProfile, viewWrapper.getAuthData(), viewWrapper.getEnv(), pageContext);
	}
	
	private String getCode (List<String> menuItems, String iconFile, Boolean renderAllTypes, Boolean renderMyProfile, AuthData authData, EnvData env, PageContext pageContext) throws KommetException
	{	
		if (renderAllTypes == null)
		{
			renderAllTypes = true;
		}
		
		if (renderMyProfile == null)
		{
			renderMyProfile = true;
		}
		
		StringBuilder code = new StringBuilder();
		code.append("<div class=\"km-menu\"><ul>");
		
		// system administrators and root users can see the setup section
		if (authData.hasProfile(Profile.ROOT_NAME) || authData.hasProfile(Profile.SYSTEM_ADMINISTRATOR_NAME))
		{
			String setupIcon = "<span class=\"km-menu-icon\"><img src=\"" + getHost() + "/resources/images/setup-white.png\"></img></span>";
			code.append("<li>").append("<a href=\"").append(getHost()).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("/setup").append("\">").append(setupIcon).append("<span class=\"km-menu-item-title\">Setup</span></a><li>");
		}
		
		String defaultMenuIcon = "<span class=\"km-menu-icon\"><img src=\"" + getHost() + "/resources/images/" + iconFile + "\"></img></span>";
		
		// if links for all types are supposed to be rendered, render them
		if (renderAllTypes)
		{
			for (Type type : env.getCustomTypes())
			{
				if (!type.isAutoLinkingType() && authData.canReadType(type.getKID(), false, env))
				{
					code.append("<li>").append("<a href=\"").append(getHost()).append("/").append(type.getKeyPrefix()).append("\">");
					code.append(defaultMenuIcon).append("<span class=\"km-menu-item-title\">");
					code.append(type.getInterpretedPluralLabel(authData)).append("</span></a>");
					code.append("</li>");
				}
			}
		}
		
		if (menuItems != null && !menuItems.isEmpty())
		{
			for (String item : menuItems)
			{
				code.append(item);
			}
		}
		
		if (renderMyProfile)
		{
			// add my profile link
			code.append("<li><a href=\"").append(getHost()).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("/me\">").append(defaultMenuIcon).append("<span class=\"km-menu-item-title\">").append(authData.getI18n().get("menu.myprofile")).append("</span></a></li>");
		}
		
		// add log out link
		code.append("<li><a href=\"").append(getHost()).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("/logout?env=").append(env.getId()).append("\">").append(defaultMenuIcon).append("<span class=\"km-menu-item-title\">").append(authData.getI18n().get("menu.logout")).append("</span></a></li>");
		
		code.append("</ul></div>");
		return code.toString();
	}

	public void addItem(String itemHTML)
	{
		this.items.add(itemHTML);
	}

	public void setRenderAllTypes(Boolean renderAllTypes)
	{
		this.renderAllTypes = renderAllTypes;
	}

	public Boolean getRenderAllTypes()
	{
		return renderAllTypes;
	}

	public Boolean getRenderMyProfile()
	{
		return renderMyProfile;
	}

	public void setRenderMyProfile(Boolean renderMyProfile)
	{
		this.renderMyProfile = renderMyProfile;
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
