/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.datatable;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.FilterField;
import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;

public class DataTableSearchTag extends KommetTag
{
	private static final long serialVersionUID = -320028508830556211L;
	private DataTableTag dataTable;
	private List<FilterField> filterFields;
	
	// number of columns in the search panel
	private Integer columns;

	public DataTableSearchTag() throws KommetException
	{
		super();
		this.columns = 2;
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {
		this.dataTable = (DataTableTag)checkParentTag(DataTableTag.class);
		this.filterFields = new ArrayList<FilterField>();
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		// pass the table search tag to the parent data table tag
		// the parent will take care of rendering it
		this.dataTable.setTableSearchTag(this);
		return EVAL_PAGE;
    }
	
	@Override
	protected void cleanUp()
	{
		this.filterFields = null;
		this.dataTable = null;
		this.columns = 2;
	}
	
	public void addFilterField(FilterField field)
	{
		this.filterFields.add(field);
	}

	public List<FilterField> getFilterFields()
	{
		return filterFields;
	}

	public void setFilterFields(List<FilterField> filterFields)
	{
		this.filterFields = filterFields;
	}

	public Integer getColumns()
	{
		return columns;
	}

	public void setColumns(Integer columns)
	{
		this.columns = columns;
	}
}
