/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.data.KommetException;
import kommet.data.Record;

public class HiddenFieldTag extends FieldTag
{
	private static final long serialVersionUID = -7213777930697765722L;
	
	private String field;

	public HiddenFieldTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		Record record = null;
		KommetTag parent = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		if (parent == null)
		{
			parent = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
			if (parent == null)
			{
				return exitWithTagError("Tag hiddenField should be embedded either in objectEdit or objectDetails");
			}
			else
			{
				record = ((ObjectDetailsTag)parent).getRecord();
			}
		}
		else
		{
			record = ((ObjectDetailsTag)parent).getRecord();
		}
		
		if (!StringUtils.hasText(this.field))
		{
			throw new JspException("Field attribute is required for hidden field tag");
		}
		
		try
		{
			writeToPage(HiddenFieldTag.getCode(record.getField(field), this.field, ((ObjectDetailsTag)parent).getFieldNamePrefix(), getId()));
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error creating hiddenField tag: " + e.getMessage());
		}
		
		return EVAL_PAGE;
    }
	
	public static String getCode (Object fieldValue, String field, String prefix, String id) throws KommetException
	{
		StringBuilder code = new StringBuilder("<input type=\"hidden\"");
		if (StringUtils.hasText(id))
		{
			code.append(" id=\"").append(id).append("\"");
		}
		
		code.append(" name=\"");
		
		if (StringUtils.hasText(prefix))
		{
			code.append(prefix);
		}
		
		code.append(field).append("\" ");
		code.append("value=\"").append(fieldValue).append("\" />");
		return code.toString();
	}

	public void setName(String field)
	{
		this.field = field;
	}

	public String getName()
	{
		return field;
	}
}
