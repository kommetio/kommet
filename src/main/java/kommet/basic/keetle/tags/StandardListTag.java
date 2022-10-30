/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import kommet.data.Record;

public class StandardListTag implements Tag
{
	private Tag parent;
	private PageContext pageContext;
	private String object;
	
	/**
	 * Comma separated list of fields to display
	 */
	private String fields;
	
	private List<Record> records;
	
	@Override
	public int doEndTag() throws JspException
	{
		return EVAL_PAGE;
	}

	@Override
	public int doStartTag() throws JspException
	{
		//
		//this.pageContext.getSession()
		return SKIP_BODY;
	}

	@Override
	public Tag getParent()
	{
		return this.parent;
	}

	@Override
	public void release()
	{
		//
	}

	@Override
	public void setPageContext(PageContext pageContext)
	{
		this.pageContext = pageContext;
	}

	@Override
	public void setParent(Tag parent)
	{
		this.parent = parent;
	}

	public void setObject(String object)
	{
		this.object = object;
	}

	public String getObject()
	{
		return object;
	}

	public void setFields(String fields)
	{
		this.fields = fields;
	}

	public String getFields()
	{
		return fields;
	}

	public void setRecords(List<Record> records)
	{
		this.records = records;
	}

	public List<Record> getRecords()
	{
		return records;
	}
}


