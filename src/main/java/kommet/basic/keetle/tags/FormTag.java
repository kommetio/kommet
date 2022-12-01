/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.Random;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.data.KommetException;
import kommet.utils.XMLUtil;

public class FormTag extends KommetTag
{
	private static final long serialVersionUID = 8014938310143359581L;
	
	private String name;
	private String id;
	private String action;
	private String method;
	
	public FormTag() throws KommetException
	{
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {	
		checkParentTag(ViewTag.class);
		if (this.method == null)
		{
			this.method = "POST";
		}
		else
		{
			if (!this.method.toLowerCase().equals("get") && !this.method.toLowerCase().equals("post"))
			{
				return exitWithTagError("Invalid method attribute on tag form: " + this.method + ". Only POST and GET are allowed.");
			}
		}
		
		if (!StringUtils.hasText(this.id))
		{
			// TODO keep a global count of item IDs in the top-most km:page tag and assign these IDs here
			this.id = "form_" + (new Random()).nextInt(100);
		}
		
		StringBuilder code = new StringBuilder();
		code.append("<form action=\"");
		
		try
		{
			code.append(getHost() + "/" + this.action);
		}
		catch (Exception e)
		{
			return exitWithTagError("Error rendering form tag: " + e.getMessage());
		}
		
		code.append("\"");
		code.append(" method=\"").append(this.method).append("\"");
		XMLUtil.addStandardTagAttributes(code, this.id, this.name, null, null);
		code.append(">");
		
		writeToPage(code.toString());
		
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		writeToPage("</form>");
		return EVAL_PAGE;
    }
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setAction(String action)
	{
		this.action = action;
	}

	public String getAction()
	{
		return action;
	}

	public void setMethod(String method)
	{
		this.method = method;
	}

	public String getMethod()
	{
		return method;
	}
}
