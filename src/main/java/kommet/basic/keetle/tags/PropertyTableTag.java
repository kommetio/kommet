/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.i18n.I18nDictionary;

public class PropertyTableTag extends KommetTag
{
	private static final long serialVersionUID = -2635284783261075156L;
	private String cssClass;
	private String cssStyle;
	private Integer columns;
	private String id;
	private Integer currentProperty;
	
	/**
	 * Property storing the last visited child tag of this propertyRow tag, i.e. either propertyLabel or propertyValue.
	 * Needed to control whether propertyValue is always preceded by propertyLabel (this should be done by XSD) and for 
	 * propertyValue to be able to get the current field ordinal from propertyLabel.
	 */
	private KommetTag lastChildTag;
	private List<FieldTag> fields;
	private StringBuilder innerCode;
	private ViewTag parentView;
	private ObjectDetailsTag objectDetailsTag;

	public PropertyTableTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		this.objectDetailsTag = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		
		this.fields = new ArrayList<FieldTag>();
		this.innerCode = new StringBuilder();
		this.currentProperty = 0;
		
		if (getParent() instanceof ObjectDetailsTag)
		{
			((ObjectDetailsTag)getParent()).setInnerPropertyTable(this);
		}
		
		if (this.columns == null)
		{
			this.columns = 2;
		}
		
		// get next component ID for the page
		try
		{
			this.id = getParentView().nextComponentId();
		}
		catch (KommetException e)
		{
			return exitWithTagError(e.getMessage());
		}
		
		innerCode.append(getStartTagCode(this.id, this.cssClass + (this.objectDetailsTag.getMode().equals(TagMode.VIEW.stringValue()) ? " km-rd-table-mobile-inline-labels-view" : ""), this.cssStyle));
		
		return EVAL_BODY_BUFFERED;
    }
	
	protected int exitWithTagError(String msg) throws JspException
	{
		try
		{	
			JspWriter out = this.pageContext.getOut();
			out.write("<div class=\"msg-tag tag-error\">");
			out.write(msg);
			out.write("</div>");
		}
		catch (IOException e)
		{
			throw new JspException("Error writing to JSP page: " + e.getMessage());
		}
		
		return EVAL_PAGE;
	}
	
	public ViewTag getParentView() throws MisplacedTagException
	{
		if (this.parentView == null)
		{
			this.parentView = (ViewTag)findAncestorWithClass(this, ViewTag.class);
			if (this.parentView == null)
			{
				throw new MisplacedTagException("Tag " + this.getClass().getSimpleName() + " is not placed within a view tag");
			}
		}
		return parentView;
	}
	
	@Override
    public int doEndTag() throws JspException
    {	
		// if there are any fields to render, do it
		if (!this.fields.isEmpty())
		{	
			try
			{
				innerCode.append(getInnerCodeForFields(fields, objectDetailsTag, columns, objectDetailsTag.getFailOnUninitializedFields(), getParentView(), pageContext));
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error rendering propertyTable: " + e.getMessage());
			}
		}
		else
		{
			innerCode.append(getBodyContent().getString());
		}
		
		innerCode.append(getEndTagCode());
		
		// do not render property table code right away, buffer it in the enclosing object details tag
		objectDetailsTag.setInnerPropertyTableCode(innerCode.toString());
		
		this.id = null;
		this.columns = null;
		this.cssClass = null;
		this.cssStyle = null;
		this.currentProperty = null;
		this.fields = null;
		this.innerCode = null;
		this.objectDetailsTag = null;
		return EVAL_PAGE;
    }
	
	public static String getInnerCodeForFields (List<FieldTag> fieldTags, ObjectDetailsTag objectDetailsTag, int columns, boolean failOnUninitializedFields, ViewTag parentView, PageContext pageContext) throws KommetException
	{
		int index = 0;
		StringBuilder code = new StringBuilder();
		Type type = objectDetailsTag.getType();
		
		AuthData authData = AuthUtil.getAuthData(pageContext.getSession());
		I18nDictionary i18n = parentView.getI18n().getDictionary(authData.getUser().getLocaleSetting());
	
		for (FieldTag fieldTag : fieldTags)
		{
			Field field = type.getField(fieldTag.getName(), parentView.getEnv());
			
			if (TagMode.VIEW.stringValue().equals(fieldTag.getMode()))
			{
				if (!authData.canReadField(field, false, parentView.getEnv()))
				{
					continue;
				}
			}
			// TODO - the code below allows for adding hidden fields only if user has edit access to them
			// which is not fully correct
			else if (TagMode.EDIT.stringValue().equals(fieldTag.getMode()) || (TagMode.HIDDEN.stringValue().equals(fieldTag.getMode())))
			{
				if (!authData.canEditField(field, false, parentView.getEnv()))
				{
					continue;
				}
			}
			else
			{
				// this will never happen since we only have two types inheriting from FieldTag:
				// InputFieldTag and OutputFieldTag
				throw new KommetException("Field tag is of invalid type " + fieldTag.getClass().getName());
			}
			
			if (index % columns == 0)
			{
				// start new row
				code.append(PropertyRowTag.getStartTagCode(null, null, null));
			}
			else
			{
				// if not first field in row, render field separator
				code.append("<div class=\"sep km-rd-cell\"></div>");
			}
			
			// start property tag
			code.append(PropertyTag.getStartCode(null, null, null));
			
			// Render label for the field, but render the "required" marker only in edit mode.
			// If this is a system field, its label should be translated according to the i18n dictionary.
			// System field i18n labels always have the form "label." + field API name.
			code.append(PropertyLabelTag.getCode(Field.isSystemField(field.getApiName()) ? i18n.get("label." + field.getApiName()) : field.getInterpretedLabel(authData), (field.isRequired() && TagMode.EDIT.stringValue().equals(fieldTag.getMode()) ? true : false)));
			
			Object fieldValue = objectDetailsTag.getRecord().getField(field.getApiName(), failOnUninitializedFields);
			
			// render field value
			if (TagMode.VIEW.stringValue().equals(fieldTag.getMode()))
			{
				code.append(PropertyValueTag.getCode(OutputFieldTag.getCode(fieldValue, field, field.getApiName(), objectDetailsTag.getRecordId(), StringUtils.hasText(fieldTag.getId()) ? fieldTag.getId() : field.getApiName(), fieldTag.getCssClass(), fieldTag.getCssStyle(), true, pageContext, parentView.getEnv(), parentView.getUserService(), authData, parentView.getViewWrapper())));
			}
			else if (TagMode.EDIT.stringValue().equals(fieldTag.getMode()))
			{
				code.append(PropertyValueTag.getCode(InputFieldTag.getCode(fieldValue, field, field.getApiName(), objectDetailsTag.getRecordId(), null, objectDetailsTag.getFieldNamePrefix(), StringUtils.hasText(fieldTag.getId()) ? fieldTag.getId() : field.getApiName(), fieldTag.getCssClass(), fieldTag.getCssStyle(), failOnUninitializedFields, parentView.getPageData().getRmParams(), pageContext, parentView.getEnv(), authData, parentView.getI18n(), parentView)));
			}
			else if (TagMode.HIDDEN.stringValue().equals(fieldTag.getMode()))
			{
				code.append(PropertyValueTag.getCode(HiddenFieldTag.getCode(fieldValue, field.getApiName(), objectDetailsTag.getFieldNamePrefix(), StringUtils.hasText(fieldTag.getId()) ? fieldTag.getId() : field.getApiName())));
			}
			
			// end property tag
			code.append(PropertyTag.getEndCode());
			
			if ((index + 1) % columns == 0)
			{
				// end row
				code.append(PropertyRowTag.getEndTagCode());
			}
			
			index++;
		}
		
		// fill up and end any open row tags
		if (index % columns != 0)
		{
			// fill up with cells to a full row
			for (int i = 0; i < (columns - (index % columns)); i++)
			{
				code.append("<div class=\"km-rd-cell sep\"></div>");
				
				code.append(PropertyTag.getStartCode(null, "km-rd-property-empty", null));
				code.append(PropertyLabelTag.getCode("", false)).append(PropertyValueTag.getCode(""));
				
				// end property tag
				code.append(PropertyTag.getEndCode());
			}
			code.append(PropertyRowTag.getEndTagCode());
		}
		
		return code.toString();
	}

	public void setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
	}

	public String getCssClass()
	{
		return cssClass;
	}

	public void setCssStyle(String cssStyle)
	{
		this.cssStyle = cssStyle;
	}

	public String getCssStyle()
	{
		return cssStyle;
	}

	public void setColumns(Integer columns)
	{
		this.columns = columns;
	}

	public Integer getColumns()
	{
		return columns;
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}
	
	public Integer getCurrentProperty()
	{
		return currentProperty;
	}
	
	public Integer nextPropertyOrdinal()
	{
		return ++currentProperty;
	}
	
	public void setLastChildTag(KommetTag lastChildTag)
	{
		this.lastChildTag = lastChildTag;
	}

	public KommetTag getLastChildTag()
	{
		return lastChildTag;
	}
	
	public void addField (FieldTag tag)
	{
		this.fields.add(tag);
	}

	public static String getStartTagCode(String id, String cssClass, String cssStyle)
	{
		StringBuilder code = new StringBuilder("<div class=\"km-rd-table km-grid-section km-grid-group");
		if (StringUtils.hasText(cssClass))
		{
			code.append(" ").append(cssClass);
		}
		code.append("\"");
		if (StringUtils.hasText(cssClass))
		{
			code.append(" style=\"").append(cssStyle).append("\"");
		}
		
		if (StringUtils.hasText(id))
		{
			code.append(" id=\"").append(id).append("\"");
		}
		
		code.append(">");
		return code.toString();
	}

	public static String getEndTagCode()
	{
		return "</div>";
	}
	
	public void appendInnerCode (String code)
	{
		this.innerCode.append(code);
	}

	public ObjectDetailsTag getObjectDetailsTag()
	{
		return objectDetailsTag;
	}
}
