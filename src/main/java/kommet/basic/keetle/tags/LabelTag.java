/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.util.StringUtils;

import kommet.auth.AuthUtil;
import kommet.i18n.I18nDictionary;

public class LabelTag extends TagSupport
{
	private static final long serialVersionUID = -2909830337796564149L;
	
	private String key;
	private I18nDictionary i18n;

	@Override
    public int doStartTag() throws JspException
    {
		if (!StringUtils.hasText(key))
		{
			cleanUp();
			throw new JspException("Empty key passed to label tag");
		}
		
		if (i18n == null)
		{
			i18n = AuthUtil.getAuthData(pageContext.getSession()).getI18n(); 
		}
		
		try
		{	
			this.pageContext.getOut().write(i18n.get(key));
		}
		catch (IOException e)
		{
			throw new JspException("Error writing to JSP page: " + e.getMessage());
		}
		finally
		{
			cleanUp();
		}
		return EVAL_PAGE;
    }
	
	private void cleanUp()
	{
		this.key = null;
		this.i18n = null;
	}
	
	public String getKey()
	{
		return key;
	}

	public void setKey(String key)
	{
		this.key = key;
	}

	public void setI18n(I18nDictionary i18n)
	{
		this.i18n = i18n;
	}

	public I18nDictionary getI18n()
	{
		return i18n;
	}
}
