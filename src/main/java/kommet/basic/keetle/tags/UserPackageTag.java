/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kommet.data.KommetException;
import kommet.env.EnvService;
import kommet.utils.MiscUtils;

public class UserPackageTag extends KommetTag
{
	private static final long serialVersionUID = 6908070954443282238L;
	private String packageName;

	public UserPackageTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		if (StringUtils.hasText(this.packageName))
		{
			WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(this.pageContext.getServletContext());
	        AutowireCapableBeanFactory factory = wac.getAutowireCapableBeanFactory();
	        
	        if (factory == null)
	        {
	        	throw new JspException("Bean factory is null");
	        }
	        
	        EnvService envService = factory.getBean(EnvService.class);
	        try
			{
	        	// TODO this is not a very efficient method because the envService variable is initialized
	        	// every time this tag is called
				writeToPage(MiscUtils.envToUserPackage(this.packageName, envService.getCurrentEnv(this.pageContext.getSession())));
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error converting package name: " + e.getMessage());
			}
		}
		
		return EVAL_PAGE;
    }

	public void setPackage(String packageName)
	{
		this.packageName = packageName;
	}

	public String getPackage()
	{
		return packageName;
	}

}
