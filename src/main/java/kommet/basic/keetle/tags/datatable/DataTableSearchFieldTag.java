/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.datatable;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.FilterField;
import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;

public class DataTableSearchFieldTag extends KommetTag
{
	private static final long serialVersionUID = -4123135875388104685L;
	private String name;
	private String label;
	private String comparison;

	public DataTableSearchFieldTag() throws KommetException
	{
		super();
		this.comparison = "ilike";
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		return EVAL_BODY_BUFFERED;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		@SuppressWarnings("unchecked")
		DataTableSearchTag parent = (DataTableSearchTag)checkParentTag(DataTableSearchTag.class);
		
		if (getBodyContent() == null)
		{
			parent.addFilterField(new FilterField(this.name, this.comparison, this.label, null));
		}
		else
		{
			// TODO in the future, we want to allow for custom search fields
			throw new JspException("Arbitrary search field content not implemented");
		}
		return EVAL_PAGE;
    }

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getComparison()
	{
		return comparison;
	}

	public void setComparison(String comparison)
	{
		this.comparison = comparison;
	}
}
