/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.ViewUtil;
import kommet.data.KommetException;

/**
 * Includes a view resource into a view.
 * @author Radek Krawiec
 * @since 27/03/2015
 */
public class ViewResourceTag extends KommetTag
{
	private static final long serialVersionUID = 799296328342577886L;
	
	// the name of the view resource
	private String name;

	public ViewResourceTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		try
		{
			writeToPage(ViewUtil.includeViewResource(name, getViewWrapper().getAppConfig(), this.pageContext.getServletContext().getContextPath(), getEnv()));
		}
		catch (MisplacedTagException e)
		{
			return exitWithTagError("View resource tag is not placed within a view wrapper tag");
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error including view resource" + (e.getMessage() != null ? (". " + e.getMessage()) : ""));
		}
		
		this.name = null;
		
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
}
