/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.datatable;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;

public class DataTableOptionTag extends KommetTag
{
	private static final long serialVersionUID = -4017185955197332419L;
	private String name;
	private String value;

	public DataTableOptionTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		@SuppressWarnings("unchecked")
		DataTableTag dt = (DataTableTag)checkParentTag(DataTableTag.class);
		dt.setDataTableOption(this.name, this.value);
		return SKIP_BODY;
    }

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}
