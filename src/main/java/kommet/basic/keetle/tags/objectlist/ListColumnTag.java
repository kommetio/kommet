/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.objectlist;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.tags.ListDisplay;
import kommet.basic.keetle.tags.LookupTag;
import kommet.basic.keetle.tags.KommetTag;
import kommet.basic.keetle.tags.RelatedListTag;
import kommet.data.Field;
import kommet.data.KommetException;

public class ListColumnTag extends KommetTag
{
	private static final long serialVersionUID = 3051742721473434422L;
	private String field;
	private String isLink;
	private String isSortable;
	private String label;
	
	/**
	 * In case not just some field value, but a more complicate formula is displayed,
	 * this property stores the formula.
	 */
	private String fieldFormula;
	
	/**
	 * Java callback function name consisting of the complete class name and static method name
	 * e.g. "kommet.utils.formatDate" 
	 */
	private String javaCallback;
	
	private String idField;
	private String nameField;
	private String onClick;
	private String url;

	public ListColumnTag() throws KommetException
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
		KommetTag list = checkParentTag(ObjectListTag.class, RelatedListTag.class, LookupTag.class);
		
		ListColumn col = new ListColumn();
		col.setLink("true".equals(this.isLink));
		col.setSortable("true".equals(this.isSortable));
		col.setLabel(label);
		col.setIdField(idField);
		col.setNameField(nameField);
		col.setOnClick(this.onClick);
		col.setUrl(url);
		
		try
		{
			col.setJavaCallback(javaCallback, getViewWrapper().getCompiler().getClassLoader(getEnv()), getEnv());
		}
		catch (KommetException e)
		{
			return exitWithTagError(e.getMessage());
		}
		
		if (this.field != null)
		{
			col.setField(this.field);
			col.setType(ListColumnType.FIELD);
		}
		
		if (!(list instanceof ListDisplay))
		{
			return exitWithTagError("Tag listColumn must be placed in a tag implementing ListDisplay");
		}
		
		if (getBodyContent() != null && StringUtils.hasText(getBodyContent().getString()))
		{
			if (StringUtils.hasText(javaCallback))
			{
				return exitWithTagError("Both formula and javaCallback are set on the list column. Only one of these can be set.");
			}
			
			col.setFormula(getBodyContent().getString());
			col.setType(ListColumnType.FORMULA);
		}
		
		if (list instanceof ObjectListTag)
		{
			// label can be deduced only if list displays records, not beans, and only field value is
			// displayed, not a formula
			if (!StringUtils.hasText(label) && ObjectListItemType.RECORD.equals(((ObjectListTag)list).getListItemType()) && this.field != null)
			{
				try
				{
					col.setLabel(((ObjectListTag)list).getRecordType().getField(this.field, getEnv()).getInterpretedLabel(getEnv().currentAuthData()));
				}
				catch (KommetException e)
				{
					return exitWithTagError("Error getting label for field " + this.field + ": " + e.getMessage());
				}
			}
			
			if (!StringUtils.hasText(col.getIdField()))
			{
				col.setIdField(((ObjectListTag)list).getIdField());
			}
			((ObjectListTag)list).addColumn(col);
		}
		else if (list instanceof RelatedListTag)
		{
			if (!StringUtils.hasText(label) && this.field != null)
			{
				try
				{
					Field field = ((RelatedListTag)list).getRecordType().getField(this.field, getEnv());
					
					if (field == null)
					{
						return exitWithTagError("Field " + this.field + " not found on type " + ((RelatedListTag)list).getRecordType().getQualifiedName());
					}
					
					col.setLabel(field.getLabel());
				}
				catch (KommetException e)
				{
					return exitWithTagError("Error getting label for field " + this.field + ": " + e.getMessage());
				}
			}
			
			((RelatedListTag)list).addColumn(col);
		}
		else if (list instanceof LookupTag)
		{
			if (!StringUtils.hasText(label) && this.field != null)
			{
				try
				{
					col.setLabel(((LookupTag)list).getRecordType().getField(this.field, getEnv()).getLabel());
				}
				catch (KommetException e)
				{
					return exitWithTagError("Error getting label for field " + this.field + ": " + e.getMessage());
				}
			}
			((LookupTag)list).addColumn(col);
		}
		
		this.field = null;
		this.isLink = null;
		this.label = null;
		this.isSortable = null;
		this.fieldFormula = null;
		this.url = null;
		this.onClick = null;
		
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

	public void setIsLink(String link)
	{
		this.isLink = link;
	}

	public String getIsLink()
	{
		return isLink;
	}

	public void setSortable(String sortable)
	{
		this.isSortable = sortable;
	}

	public String getSortable()
	{
		return isSortable;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	public void setFieldFormula(String fieldFormula)
	{
		this.fieldFormula = fieldFormula;
	}

	public String getFieldFormula()
	{
		return fieldFormula;
	}

	public void setIdField(String idField)
	{
		this.idField = idField;
	}

	public String getIdField()
	{
		return idField;
	}

	public void setNameField(String nameField)
	{
		this.nameField = nameField;
	}

	public String getNameField()
	{
		return nameField;
	}

	public String getJavaCallback()
	{
		return javaCallback;
	}

	public void setJavaCallback(String javaCallback)
	{
		this.javaCallback = javaCallback;
	}

	public String getOnClick()
	{
		return onClick;
	}

	public void setOnClick(String onClick)
	{
		this.onClick = onClick;
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
