/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.utils.UrlUtil;

public class MenuTag extends KommetTag
{
	private static final long serialVersionUID = -7336490263710459729L;
	
	public MenuTag() throws KommetException
	{
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {
		checkParentTag(ViewTag.class);
		
		AuthData authData = AuthUtil.getAuthData(this.pageContext.getSession());
		
		StringBuilder code = new StringBuilder();
		
		try
		{
			code.append("<div class=\"ibox\">");
			code.append("<ul class=\"left-menu\">");
			for (Type type : getEnv().getCustomTypes())
			{
				if (authData.canReadType(type.getKID(), false, getEnv()))
				{
					code.append("<li><a href=\"").append(this.pageContext.getServletContext().getContextPath()).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("/list/").append(type.getKeyPrefix().getPrefix()).append("\">").append(type.getInterpretedPluralLabel(authData)).append("</a></li>");
				}
			}
			
			code.append("</ul></div>");
			writeToPage(code.toString());
		}
		catch (Exception e)
		{
			throw new JspException("Error rendering menu tag: " + e.getMessage());
		}
		
		return EVAL_PAGE;
    }
}
