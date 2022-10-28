/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.associationpanel;

public class PanelButton
{
	public static final int BUTTON_TYPE_TEXT_LINK = 0;
	public static final int BUTTON_TYPE_BUTTON = 1;
	
	private String url;
	private int type;
	private String linkText;
	private String buttonImageURL;
	private String cssClass;
	private String cssStyle;

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getUrl()
	{
		return url;
	}

	public void setType(int type)
	{
		this.type = type;
	}

	public int getType()
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
