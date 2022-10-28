/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import kommet.dao.dal.DALSyntaxException;
import kommet.data.KommetException;

public class IfTag extends KommetTag
{
	private static final long serialVersionUID = 3138779504093335295L;
	private String exists;

	public IfTag() throws KommetException
	{
		super();
	}
	
	@Override
	public int doStartTag() throws JspException
	{
		ViewWrapperTag view = null;
		try
		{
			view = getViewWrapper();
			if (view == null)
			{
				return exitWithTagError("If tag should be placed within a view wrapper tag");
			}
		}
		catch (MisplacedTagException e)
		{
			return exitWithTagError("If tag should be placed within a view wrapper tag");
		}
		
		try
		{
			return view.getDataService().select(this.exists, getEnv()).isEmpty() ? SKIP_BODY : EVAL_PAGE;
		}
		catch (DALSyntaxException e)
		{
			e.printStackTrace();
			return exitWithTagError("Invalid syntax of DAL query in if tag: " + e.getMessage());
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			return exitWithTagError("Error executing DAL query in if tag: " + e.getMessage());
		}
	}

	public void setExists(String exists)
	{
		this.exists = exists;
	}

	public String getExists()
	{
		return exists;
	}

}
