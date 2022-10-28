/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kommet.basic.RecordProxy;
import kommet.env.EnvService;

public class InputTag extends TagSupport
{
	private static final long serialVersionUID = -8607917030249656481L;

	@Autowired
	EnvService envService;
	
	/**
	 * The name of the input field
	 */
	private String name;
	
	/**
	 * Value of the variable
	 */
	private Object value;
	
	@Override
    public int doStartTag() throws JspException
    {	
        WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(this.pageContext.getServletContext());
        AutowireCapableBeanFactory factory = wac.getAutowireCapableBeanFactory();
        
        if (factory == null)
        {
        	throw new JspException("Bean factory is null");
        }
        
        factory.autowireBean(this);
        
        if (envService == null)
        {
        	throw new JspException("EnvService not injected");
        }
        
        JspWriter out = this.pageContext.getOut();
        try
		{
        	if (value == null)
        	{
        		throw new JspException("Value of a KTL tag must not be null. Use special object stub that wraps null values.");
        	}
        	
        	// TODO add support for scalar properties here - right now only object stub can be passed as values
        	if (!(value instanceof RecordProxy))
        	{
        		throw new JspException("Value of a KTL tag must be an object stub");
        	}
        	
        	RecordProxy objValue = (RecordProxy)value;
        	
			//out.write("<div>env = " + envService.getCurrentEnv(this.pageContext.getSession()) + "</div>");
        	out.write("<input type=\"text\" readonly=\"readonly\"");
        	if (!objValue.isNull())
        	{
        		out.write(" value=\"" + objValue.getId() + "\"");
        	}
        	out.write("></input>");
        	
        	// add search 
        	out.write("<input type=\"button\" value=\"Select\"></input>");
        	
        	//out.write("<div>env = </div>");
		}
		catch (Exception e)
		{
			throw new JspException(e.getMessage());
		}

        return EVAL_PAGE;
    }

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setValue(Object value)
	{
		this.value = value;
	}

	public Object getValue()
	{
		return value;
	}
}
