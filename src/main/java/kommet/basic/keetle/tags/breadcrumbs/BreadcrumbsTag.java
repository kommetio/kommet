/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.breadcrumbs;

import javax.servlet.jsp.JspException;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.keetle.tags.MisplacedTagException;
import kommet.basic.keetle.tags.KommetTag;
import kommet.basic.keetle.tags.ViewWrapperTag;
import kommet.data.KommetException;

public class BreadcrumbsTag extends KommetTag
{
	private static final long serialVersionUID = -2232854605844314574L;
	private Boolean isAlwaysVisible;

	public BreadcrumbsTag() throws KommetException
	{
		super();
	}

	@Override
    public int doStartTag() throws JspException
    {
		AuthData authData = null;
		ViewWrapperTag viewWrapper = null;
		try
		{
			viewWrapper = getViewWrapper();
			authData = viewWrapper.getAuthData();
		}
		catch (MisplacedTagException e)
		{
			authData = AuthUtil.getAuthData(this.pageContext.getSession());
		}
		
		try
		{
			writeToPage(Breadcrumbs.getCode(authData.getI18n(), Boolean.TRUE.equals(this.isAlwaysVisible), this.pageContext.getSession()));
		}
		catch (Exception e)
		{
			if (viewWrapper != null)
			{
				viewWrapper.addErrorMsgs("Error rendering breadcrumbs: " + e.getMessage());
			}
			else
			{
				e.printStackTrace();
				throw new JspException("Error rendering breadcrumbs: " + e.getMessage());
			}
		}
		
		return EVAL_PAGE;
    }
	
	@Override
	protected void cleanUp()
	{
		super.cleanUp();
		this.isAlwaysVisible = null;
	}

	public Boolean getIsAlwaysVisible()
	{
		return isAlwaysVisible;
	}

	public void setIsAlwaysVisible(Boolean isAlwaysVisible)
	{
		this.isAlwaysVisible = isAlwaysVisible;
	}
}
