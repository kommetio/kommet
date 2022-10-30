/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.tabs;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.tags.MisplacedTagException;
import kommet.basic.keetle.tags.KommetTag;
import kommet.basic.keetle.tags.TabConfig;
import kommet.basic.keetle.tags.TabConfigTag;
import kommet.basic.keetle.tags.ViewTag;
import kommet.data.KommetException;

/**
 * Tabbed view.
 * @author Radek Krawiec
 * @since 26/03/2015
 */
public class TabsTag extends KommetTag
{
	private static final String TAB_CONTAINER_CSS_CLASS = "km-tabs-container km-tabs-std";
	private static final String TAB_MENU_CSS_CLASS = "km-tabs-head";
	private static final String TAB_PANELS_CSS_CLASS = "km-tabs-panels";
	private static final String TAB_PANEL_CSS_CLASS = "km-tabs-panel";
	
	private static final long serialVersionUID = -2817895710388150458L;
	private List<Tab> tabs;
	private String id;
	private String cssClass;
	private TabConfig tabConfig;
	
	public TabsTag() throws KommetException
	{
		super();
	}
	
	public int doEndTag() throws JspException
	{
		ViewTag parentView = null;
		
		try
		{
			parentView = getParentView();
		}
		catch (MisplacedTagException e)
		{
			return exitWithTagError("Tag tabs is not placed within a view tag");
		}
		
		// generate tab component ID
		String tabComponentId = StringUtils.hasText(this.id) ? this.id : parentView.nextComponentId();
		
		StringBuilder tabContainerStart = new StringBuilder();
		tabContainerStart.append("<div id=\"").append(tabComponentId).append("\" class=\"").append(TAB_CONTAINER_CSS_CLASS);
		
		if (StringUtils.hasText(this.cssClass))
		{
			// append user-defined css class for the tab container
			tabContainerStart.append(" ").append(this.cssClass);
		}
		
		tabContainerStart.append("\">");
		writeToPage(tabContainerStart.toString());
		
		// create tab menu
		writeToPage(getTabMenu(this.tabs));
		
		// start tab container
		StringBuilder tabStart = new StringBuilder();
		tabStart.append("<div class=\"").append(TAB_PANELS_CSS_CLASS).append("\">");
		writeToPage(tabStart.toString());
		
		int tabIndex = 0;
		for (Tab tab : this.tabs)
		{
			writeToPage(getTabCode(tab, tabIndex++));
		}
		
		// end tab container
		writeToPage("</div>");
		
		// end component container
		writeToPage("</div>");
		
		// apply tab behaviour
		parentView.appendScript("km.js.ui.applyTabs($(\"#" + tabComponentId + "\"));\n");
		
		if (this.tabConfig != null)
		{
			if (StringUtils.hasText(this.tabConfig.getVar()))
			{
				return exitWithTagError("Attribute 'var' cannot be set on a tabConfig tag inside km:tabs, because km:tabs does not create any variable");
			}
			
			if (StringUtils.hasText(this.tabConfig.getAfterRender()))
			{
				parentView.appendScript("// tabconfig afterrender action\n" + this.tabConfig.getAfterRender() + ";\n");
			}
		}
		
		cleanUp();
		
		return EVAL_PAGE;
	}
	
	private String getTabCode (Tab tab, int tabIndex)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<div ");
		
		if (tabIndex == 0)
		{
			// show first tab by default - do this using style attribute, do not wait for the
			// km.js.ui.applyTabs function to do this because it executes only after the page been loaded
			sb.append("style=\"display:block\" ");
		}
		
		sb.append("class=\"").append(TAB_PANEL_CSS_CLASS).append(" ").append(TAB_PANEL_CSS_CLASS).append("-").append(tabIndex).append("\">");
		
		if (tab.getContent() != null)
		{
			sb.append(tab.getContent());
		}
		sb.append("</div>");
		return sb.toString();
	}
	
	@Override
	protected void cleanUp()
	{
		super.cleanUp();
		this.tabs = null;
		this.cssClass = null;
	}
	
	private String getTabMenu (List<Tab> tabs)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<ul class=\"").append(TAB_MENU_CSS_CLASS).append("\">");
		
		int tabIndex = 0;
		for (Tab tab : tabs)
		{
			sb.append("<li class=\"").append(TAB_MENU_CSS_CLASS).append("-").append(tabIndex).append("\">").append(tab.getName()).append("</li>");
			tabIndex++;
		}
		
		sb.append("</ul>");
		
		return sb.toString();
	}
	
	public void addTab (Tab tab)
	{
		if (this.tabs == null)
		{
			this.tabs = new ArrayList<Tab>();
		}
		this.tabs.add(tab);
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getCssClass()
	{
		return cssClass;
	}

	public void setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
	}

	public void setTabConfig (TabConfigTag tag)
	{
		// clone the tag's settings into a separate class to avoid the problem with a common instance of this TabConfigTag shared between km:tabs and km:objectDetailsTab
		this.tabConfig = new TabConfig(tag);
	}
}
