/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth.tags;


import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.util.StringUtils;

import kommet.auth.AuthUtil;
import kommet.data.KommetException;

public class CheckAccessTag implements Tag
{
	private Tag parent;
	private PageContext pageContext;
	private String profile;
	
	@Override
	public int doEndTag() throws JspException
	{
		return EVAL_PAGE;
	}

	@Override
	public int doStartTag() throws JspException
	{
		if (StringUtils.hasText(this.profile))
		{
			String[] profileNames = this.profile.split(",");
			boolean profileFound = false;
			
			try
			{
				for (String profileName : profileNames)
				{
					if (AuthUtil.hasProfile(profileName.trim(), pageContext.getSession()))
					{
						profileFound = true;
						break;
					}
				}
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new JspException("Error checking if user has profile. Nested: " + e.getMessage());
			}
			
			if (profileFound)
			{
				return EVAL_BODY_INCLUDE;
			}
		}
		
		return SKIP_BODY;
	}

	@Override
	public Tag getParent()
	{
		return this.parent;
	}

	@Override
	public void release()
	{
		//
	}

	@Override
	public void setPageContext(PageContext pageContext)
	{
		this.pageContext = pageContext;
	}

	@Override
	public void setParent(Tag parent)
	{
		this.parent = parent;
	}

	public void setProfile(String profile)
	{
		this.profile = profile;
	}

	public String getProfile()
	{
		return profile;
	}
}

