/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.ref;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;

public class AvailableItemsColumnTag extends KommetTag
{
	private static final long serialVersionUID = -6104251940893168303L;
	private String field;
	private String label;

	public AvailableItemsColumnTag() throws KommetException
	{
		super();
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {
		AvailableItemsOptionsTag parent = (AvailableItemsOptionsTag)checkParentTag(AvailableItemsOptionsTag.class);
		parent.addColumn(new AvailableItemsColumn(this.field, this.label));
		return EVAL_PAGE;
    }
	
	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

}
