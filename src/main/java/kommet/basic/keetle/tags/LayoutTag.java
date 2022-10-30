/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;

public class LayoutTag extends KommetTag
{
	private static final long serialVersionUID = 6184788291188223559L;
	protected EnvData env;
	private AuthData authData;
	protected EnvService envService;
	
	public LayoutTag() throws KommetException
	{
		super();
	}
	
	public int doStartTag() throws JspException
    {	
		initBean();
		try
		{
			this.env = this.envService.getCurrentEnv(this.pageContext.getSession());
		}
		catch (KommetException e)
		{
			throw new JspException("Could not initialized environment in view tag: " + e.getMessage(), e);
		}
		
		this.authData = AuthUtil.getAuthData(this.pageContext.getSession());
		
		return EVAL_BODY_INCLUDE;
    }
	
	protected void initBean() throws JspException
	{
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(this.pageContext.getServletContext());
        AutowireCapableBeanFactory factory = wac.getAutowireCapableBeanFactory();
        
        if (factory == null)
        {
        	throw new JspException("Bean factory is null");
        }
        
        this.envService = factory.getBean(EnvService.class);
	}

	public AuthData getAuthData()
	{
		return authData;
	}
	
	public EnvData getEnv()
	{
		return this.env;
	}
}
