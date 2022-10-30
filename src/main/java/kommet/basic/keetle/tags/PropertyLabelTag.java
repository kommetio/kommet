/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.data.Field;
import kommet.data.KommetException;

public class PropertyLabelTag extends KommetTag
{
	private static final long serialVersionUID = -1969517646838671765L;
	
	private String field;
	private PropertyTableTag propertyTable;
	private String required;

	public PropertyLabelTag() throws KommetException
	{
		super();
	}
	
	private Integer fieldOrdinal;

	@Override
    public int doStartTag() throws JspException
    {
		propertyTable = (PropertyTableTag)findAncestorWithClass(this, PropertyTableTag.class);
		if (propertyTable == null)
		{
			return exitWithTagError("propertyLabel tag must be placed within a propertyTable tag");
		}
		propertyTable.setLastChildTag(this);
		propertyTable.addChild(this);
		
		// make sure tags propertyLabel/propertyValue are not mixed with tag outputField placed directly under
		// propertyTable
		if (propertyTable.hasChildWithType(OutputFieldTag.class))
		{
			return exitWithTagError("Tags propertyLabel/propertyValue cannot be mixed with tag outputField placed directly under propertyTable");
		}
		
		StringBuilder code = new StringBuilder();
		PropertyRowTag row = (PropertyRowTag)findAncestorWithClass(this, PropertyRowTag.class);
		if (row == null)
		{
			// if the tag is not placed within a row but directly in a propertyTable, it has to take care of
			// starting a new row according to the number of columns defined for the table
			this.fieldOrdinal = propertyTable.nextPropertyOrdinal();
			if ((this.fieldOrdinal + 1) % propertyTable.getColumns() == 1)
			{
				ViewTag parentView = null;
				try
				{
					parentView = getParentView();
				}
				catch (KommetException e)
				{
					return exitWithTagError("Error getting parent view: " + e.getMessage());
				}
				
				// start new row
				try
				{
					code.append(PropertyRowTag.getStartTagCode(parentView.nextComponentId(), null, null));
				}
				catch (KommetException e)
				{
					parentView.addErrorMsgs("Error rendering propertyRow: " + e.getMessage());
				}
			}
			else
			{
				// render separator cell between properties
				code.append("<div class=\"sep km-rd-cell\"></div>");
			}
		}
		
		code.append("<div class=\"label km-rd-cell\">");
		writeToPage(code.toString());
		
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		if (StringUtils.hasText(this.field))
		{
			try
			{
				if (StringUtils.hasText(this.field))
				{
					Field field = propertyTable.getObjectDetailsTag().getRecord().getType().getField(this.field, getEnv());
					
					if (field == null)
					{
						return exitWithTagError("Field " + this.field + " does not exist");
					}
					
					// display the red asterisk if the field is required, or if the label
					// has been explicitly specified to have this asterisk
					// but not when attribute "required" has been explicitly set to false
					if (!"false".equals(this.required) && (field.isRequired() || "true".equals(this.required)))
					{
						writeToPage("<span class=\"km-req\">*</span>");
					}
				}
			}
			catch (KommetException e)
			{
				return exitWithTagError(e.getMessage());
			}
			this.field = null;
		}
		writeToPage("</div>");
		
		this.propertyTable = null;
		
		return EVAL_PAGE;
    }

	public Integer getFieldOrdinal()
	{
		return fieldOrdinal;
	}

	public static String getCode(String label, boolean required)
	{
		StringBuilder code = new StringBuilder();
		code.append("<div class=\"label km-rd-cell\">");
		code.append(label);
		if (required)
		{
			code.append("<span style=\"color:red;padding-left:3px;font-weight:bold\">*</span>");
		}
		code.append("</div>");
		return code.toString();
	}

	public void setField(String forField)
	{
		this.field = forField;
	}

	public String getField()
	{
		return field;
	}

	public void setRequired(String required)
	{
		this.required = required;
	}

	public String getRequired()
	{
		return required;
	}
}
