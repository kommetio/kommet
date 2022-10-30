/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.objectlist;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.FilterField;
import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;
import kommet.utils.MiscUtils;

public class ListFilterFieldTag extends KommetTag
{
	private static final long serialVersionUID = 8921205690491467364L;
	private String field;
	private String comparison;
	private String label;
	private String hashId;
	
	private static final String COMPARISON_EQUALS = "equals";
	private static final String COMPARISON_CONTAINS = "contains";
	private static final String COMPARISON_STARTS_WITH = "startsWith";

	public ListFilterFieldTag() throws KommetException
	{
		super();
		
		// set default comparison type
		this.comparison = COMPARISON_CONTAINS;
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {
		// generate hash ID
		this.hashId = MiscUtils.getHash(10);
		ListFilterTag parent = (ListFilterTag)checkParentTag(ListFilterTag.class);
		parent.addFilterField(new FilterField(this.field, this.comparison, this.label, this.hashId));
		return EVAL_PAGE;
    }

	public void setField(String field)
	{
		this.field = field;
	}

	public String getField()
	{
		return field;
	}

	public void setComparison(String comparison)
	{
		this.comparison = comparison;
	}

	public String getComparison()
	{
		return comparison;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	public String getHashId()
	{
		return hashId;
	}

}
