/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.objectlist;

import javax.servlet.jsp.JspException;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;

public class ObjectListConfigTag extends KommetTag
{
	private static final long serialVersionUID = 8322887729749753702L;
	private ObjectListConfig config;

	public ObjectListConfigTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		AuthData authData = AuthUtil.getAuthData(this.pageContext.getSession());
		
		try
		{
			config.setEnv(getEnv());
			config.setI18n(authData.getI18n());
			config.setPageContext(this.pageContext);
			config.setServletHost(getHost());
			writeToPage(ObjectListConfig.getCode(config, getEnv()));
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error rendering object list tag: " + e.getMessage());
		}
		
		return EVAL_PAGE;
    }

	public void setConfig(ObjectListConfig config)
	{
		this.config = config;
	}

	public ObjectListConfig getConfig()
	{
		return config;
	}
}
