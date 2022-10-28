/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.tabs;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.ObjectDetailsTag;
import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;

/**
 * Represents a single tab within a tabbed view.
 * @author Radek Krawiec
 * @since 26/03/2015
 */
public class TabTag extends KommetTag
{
	private static final long serialVersionUID = -7699730100030263600L;
	
	// tab name
	private String name;
	
	// tabs tag in which this tab is placed
	private TabsTag parentTabs;
	
	// object details tag in which this tab is placed
	private ObjectDetailsTag parentObjectDetails;
	
	// tells whether this component should be rendered
	private Boolean render;
	
	public TabTag() throws KommetException
	{
		super();
	}
	
	@SuppressWarnings("unchecked")
	public int doStartTag() throws JspException
	{
		if (Boolean.FALSE.equals(this.render))
		{
			return SKIP_BODY;
		}
		else
		{	
			KommetTag parent = checkParentTag(TabsTag.class, ObjectDetailsTag.class);
			
			if (parent instanceof TabsTag)
			{
				this.parentTabs = (TabsTag)parent;
			}
			else if (parent instanceof ObjectDetailsTag)
			{
				this.parentObjectDetails = (ObjectDetailsTag)parent;
			}
			else
			{
				return exitWithTagError("Unsupported parent tag " + parent.getClass().getName() + " for tag km:tab");
			}
			
			return EVAL_BODY_BUFFERED;
		}
	}
	
	@Override
	protected void cleanUp()
	{
		super.cleanUp();
		this.name = null;
	}
	
	public int doEndTag() throws JspException
	{
		if (Boolean.FALSE.equals(this.render))
		{
			return EVAL_PAGE;
		}
		
		if (this.parentTabs != null)
		{
			this.parentTabs.addTab(new Tab(this));
		}
		else if (this.parentObjectDetails != null)
		{
			this.parentObjectDetails.addTab(new Tab(this));
		}
		
		return EVAL_PAGE;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
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

