/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.ref;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;

public class AvailableItemsOptionsTag extends KommetTag
{
	private static final long serialVersionUID = 8079205012278511493L;
	private ReferenceTag parentReferenceTag;
	private List<AvailableItemsColumn> columns;
	private String title;

	public AvailableItemsOptionsTag() throws KommetException
	{
		super();
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {
		this.parentReferenceTag = (ReferenceTag)checkParentTag(ReferenceTag.class);
		this.columns = new ArrayList<AvailableItemsColumn>();
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		this.parentReferenceTag.setAvailableItemsOptions(this);
		return EVAL_PAGE;
    }
	
	@Override
	protected void cleanUp()
	{
		this.columns = null;
		this.parentReferenceTag = null;
		super.cleanUp();
	}

	public void addColumn(AvailableItemsColumn col)
	{
		this.columns.add(col);
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

}
