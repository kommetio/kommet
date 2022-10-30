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

public class DataTableColumnTag extends KommetTag
{
	private static final long serialVersionUID = 5508875692819540358L;
	private String name;
	private String label;
	private String labelKey;
	private Boolean sortable;
	private Boolean filterable;
	private Boolean link;
	
	// tells whether this column should be style as a link (meaning CSS, not rendering it as an <a> tag)
	private Boolean linkStyle;
	
	private String url;
	
	// Javascript function that will be applied to format the property value
	private String formatFunction;

	public DataTableColumnTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		return EVAL_BODY_BUFFERED;
    }
	
	@SuppressWarnings("unchecked")
	@Override
	public int doEndTag() throws JspException
	{
		DataTableTag dt = (DataTableTag)checkParentTag(DataTableTag.class);
		
		DataTableColumn col = new DataTableColumn();
		col.setSortable(Boolean.TRUE.equals(this.sortable));
		
		if (this.label != null || this.labelKey != null)
		{
			col.setLabel(this.label != null ? this.label : (dt.getI18n().getDictionary(dt.getAuthData().getLocale()).get(this.labelKey)));
		}
		
		col.setFilterable(Boolean.TRUE.equals(this.filterable));
		col.setLink(Boolean.TRUE.equals(this.link));
		col.setFormatFunction(this.formatFunction);
		col.setFieldApiName(this.name);
		col.setUrl(this.url);
		
		// if explicitly defined, tells whether this column will be styled as a link
		// if not explicitly defined, display it as a link if it acts as a link
		col.setLinkStyle(this.linkStyle != null ? Boolean.TRUE.equals(this.linkStyle) : col.isLink());
		
		dt.addColumn(col);
		
		this.label = null;
		this.name = null;
		this.filterable = null;
		this.sortable = null;
		this.link = null;
		this.labelKey = null;
		
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

	public Boolean getSortable()
	{
		return sortable;
	}

	public void setSortable(Boolean sortable)
	{
		this.sortable = sortable;
	}

	public Boolean getFilterable()
	{
		return filterable;
	}

	public void setFilterable(Boolean filterable)
	{
		this.filterable = filterable;
	}

	public Boolean getLink()
	{
		return link;
	}

	public void setLink(Boolean link)
	{
		this.link = link;
	}

	public Boolean getLinkStyle()
	{
		return linkStyle;
	}

	public void setLinkStyle(Boolean linkStyle)
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

	public String getLabelKey()
	{
		return labelKey;
	}

	public void setLabelKey(String labelKey)
	{
		this.labelKey = labelKey;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}
}
