/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import kommet.data.KommetException;

public class PropertyValueTag extends KommetTag
{
	private static final long serialVersionUID = -3277428473533211546L;

	public PropertyValueTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		writeToPage("<div class=\"value km-rd-cell\">");
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		writeToPage("</div>");
		
		PropertyTableTag table = (PropertyTableTag)findAncestorWithClass(this, PropertyTableTag.class);
		if (table == null)
		{
			return exitWithTagError("propertyLabel tag must be placed within a propertyTable tag");
		}
		table.addChild(this);
		
		// make sure tags propertyLabel/propertyValue are not mixed with tag outputField placed directly under
		// propertyTable
		if (table.hasChildWithType(OutputFieldTag.class))
		{
			return exitWithTagError("Tags propertyLabel/propertyValue cannot be mixed with tag outputField placed directly under propertyTable");
		}
		
		PropertyRowTag row = (PropertyRowTag)findAncestorWithClass(this, PropertyRowTag.class);
		if (row == null)
		{
			// if the tag is not placed within a row but directly in a propertyTable, it has to take care of closing the row tag
			KommetTag label = table.getLastChildTag();
			if (label instanceof PropertyLabelTag)
			{
				if (((PropertyLabelTag)label).getFieldOrdinal() % table.getColumns() == 0)
				{
					// close the current row
					writeToPage(PropertyRowTag.getEndTagCode());
				}
			}
			else
			{
				try
				{
					getParentView().addErrorMsgs("propertyValue tag is not preceded by propertyLabel tag");
				}
				catch (KommetException e)
				{
					throw new JspException("Error getting parent view: " + e.getMessage());
				}
			}
		}
		
		return EVAL_PAGE;
    }

	public static Object getCode(String innerCode)
	{
		return "<div class=\"value km-rd-cell\">" + innerCode + "</div>";
	}

}
