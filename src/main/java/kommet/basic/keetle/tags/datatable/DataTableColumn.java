/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.datatable;

public class DataTableColumn
{
	private String fieldApiName;
	private String label;
	private boolean sortable;
	private boolean filterable;
	private boolean link;
	
	// tells whether this column should be style as a link
	private boolean linkStyle;
	
	private String url;
	
	// javascript function that will be applied to format the property value
	private String formatFunction;
	
	private String onClick;

	public String getFieldApiName()
	{
		return fieldApiName;
	}

	public void setFieldApiName(String fieldApiName)
	{
		this.fieldApiName = fieldApiName;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public boolean isSortable()
	{
		return sortable;
	}

	public void setSortable(boolean sortable)
	{
		this.sortable = sortable;
	}

	public boolean isFilterable()
	{
		return filterable;
	}

	public void setFilterable(boolean filterable)
	{
		this.filterable = filterable;
	}

	public boolean isLink()
	{
		return link;
	}

	public void setLink(boolean link)
	{
		this.link = link;
	}

	public boolean isLinkStyle()
	{
		return linkStyle;
	}

	public void setLinkStyle(boolean linkStyle)
	{
		this.linkStyle = linkStyle;
	}

	public String getFormatFunction()
	{
		return formatFunction;
	}

	public void setFormatFunction(String formatFunction)
	{
		this.formatFunction = formatFunction;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getOnClick()
	{
		return onClick;
	}

	public void setOnClick(String onClick)
	{
		this.onClick = onClick;
	}
}
