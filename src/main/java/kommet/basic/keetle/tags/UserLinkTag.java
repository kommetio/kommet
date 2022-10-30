/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;

import kommet.auth.UserService;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.UrlUtil;

public class UserLinkTag extends KommetTag
{
	private static final long serialVersionUID = -3970138118676719764L;
	private String userId;

	public UserLinkTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		ViewTag parentView = null;
		try
		{
			parentView = getParentView();
		}
		catch (KommetException e1)
		{
			return exitWithTagError("Error getting parent view: " + e1.getMessage());
		}
		
		KID userKID = null;
		try
		{
			userKID = KID.get(userId);
		}
		catch (KIDException e)
		{
			parentView.addErrorMsgs("Invalid user ID " + userId);
			return EVAL_PAGE;
		}
		
		try
		{
			this.pageContext.getOut().write(getCode(userKID, parentView.getEnv(), this.pageContext.getServletContext().getContextPath(), parentView.getUserService()));
		}
		catch (IOException e)
		{
			parentView.addErrorMsgs("Cannot render user link tag: " + e.getMessage());
		}
		catch (KommetException e)
		{
			parentView.addErrorMsgs("Cannot render user link tag: " + e.getMessage());
		}
		return EVAL_PAGE;
    }

	public static String getCode(KID userId, EnvData env, String contextPath, UserService userService) throws KommetException
	{
		StringBuilder code = new StringBuilder("<a href=\"");
		code.append(contextPath).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("/user/").append(userId).append("\">");
		code.append(env.getUser(userId, userService).getUserName());
		code.append("</a>");
		return code.toString();
	}

	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	public String getUserId()
	{
		return userId;
	}
}
