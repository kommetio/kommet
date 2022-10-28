/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.tabs.TabsTag;
import kommet.data.KommetException;
import kommet.utils.StringUtils;

/**
 * Tab configuration for the object details tag.
 * @author Radek Krawiec
 * @since 21/03/2015
 */
public class TabConfigTag extends KommetTag
{
	private static final long serialVersionUID = 3954147111456236242L;
	
	private ObjectDetailsTag parentObjectDetails;
	private TabsTag parentTabs;
	
	private String afterRender;
	
	/**
	 * Tells if this config was added automatically by some other feature (not by user themselves).
	 */
	private boolean isAutoAdded;
	
	// js variable name that will store the km.js.tabs object
	private String var;

	public TabConfigTag() throws KommetException
	{
		super();
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {
		KommetTag parent = checkParentTag(ObjectDetailsTag.class, TabsTag.class);
		
		if (parent instanceof ObjectDetailsTag)
		{
			this.parentObjectDetails = (ObjectDetailsTag)parent;
			
			// check if tab config has not already been auto-added
			if (this.parentObjectDetails.getTabsConfig() != null)
			{
				if (this.parentObjectDetails.getTabsConfig().isAutoAdded())
				{
					// merge auto-added config with this one
					if (!StringUtils.isEmpty(this.afterRender))
					{
						// add new script to the existing value
						this.afterRender += ";" + this.parentObjectDetails.getTabsConfig().getAfterRender();
					}
					else
					{
						this.afterRender = this.parentObjectDetails.getTabsConfig().getAfterRender();
					}
				}
				else
				{
					return exitWithTagError("TabConfig tag has been added twice");
				}
			}
			
			// create a clone of this tag and assign it to the parent
			// a clone is needed, because the contents of this tag will be cleaned in the doEndTag method which will be called
			// before the parent reads the properties from this TabsConfig tag
			
			TabConfigTag clone;
			try
			{
				clone = new TabConfigTag();
				clone.setAfterRender(this.afterRender);
				clone.setVar(this.var);
				this.parentObjectDetails.setTabsConfig(clone);
			}
			catch (KommetException e)
			{
				throw new JspException("Could not clone tag TabConfigTag");
			}
		}
		else if (parent instanceof TabsTag)
		{
			this.parentTabs = (TabsTag)parent;
			this.parentTabs.setTabConfig(this);
		}
		
		return EVAL_PAGE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		this.afterRender = null;
		this.var = null;
		return EVAL_PAGE;
    }

	public String getVar()
	{
		return var;
	}

	public void setVar(String var)
	{
		this.var = var;
	}

	public String getAfterRender()
	{
		return afterRender;
	}

	public void setAfterRender(String afterRender)
	{
		this.afterRender = afterRender;
	}

	public boolean isAutoAdded()
	{
		return isAutoAdded;
	}

	public void setAutoAdded(boolean isAutoAdded)
	{
		this.isAutoAdded = isAutoAdded;
	}
}
