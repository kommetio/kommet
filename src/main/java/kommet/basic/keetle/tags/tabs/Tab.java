/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.tabs;

import javax.servlet.jsp.tagext.BodyContent;

/**
 * Represents a single tab in a tabbed display
 * @author Radek Krawiec
 * @since 26/03/2015
 */
public class Tab
{
	private String name;
	private String content;
	
	public Tab(TabTag tabTag)
	{
		this.name = tabTag.getName();
		
		BodyContent bc = tabTag.getBodyContent();
		this.content = bc != null ? bc.getString() : null;
	}
	
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getContent()
	{
		return content;
	}
	public void setContent(String content)
	{
		this.content = content;
	}
}
