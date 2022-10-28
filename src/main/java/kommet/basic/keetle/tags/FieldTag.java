/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.apache.commons.beanutils.BeanUtils;

import kommet.basic.RecordProxy;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.UninitializedFieldException;

/**
 * Universal field tag that can represent either an input or an output field of any data type.
 * @author Radek Krawiec
 * @created 2013
 */
public class FieldTag extends KommetTag
{
	private static final long serialVersionUID = 4563935637549043198L;
	
	// field API name
	private String fieldApiName;
	
	// ID of the input HTML element that will be rendered
	private String id;
	
	// CSS class of the input HTML element that will be rendered
	private String cssClass;
	
	// CSS style of the input HTML element that will be rendered
	private String cssStyle;
	
	protected RecordContext recordContext;
	
	protected TagMode mode;
	
	/**
	 * A record on which the value of the field is displayed. This variable is only used when
	 * the field tag is placed outside of the object details tag. If placed within the object details tag,
	 * the record instance is inherited from the parent tag. 
	 */
	protected RecordProxy record;
	
	/**
	 * Arbitrary value of the field. If set, it overrides the actual value of the field retrieved from the record.
	 */
	private Object value;

	public FieldTag() throws KommetException
	{
		super();
		
		if (this instanceof OutputFieldTag)
		{
			this.mode = TagMode.VIEW;
		}
		else if (this instanceof InputFieldTag)
		{
			this.mode = TagMode.EDIT;
		}
		else if (this instanceof HiddenFieldTag)
		{
			this.mode = TagMode.HIDDEN;
		}
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		ViewWrapperTag viewWrapper = null;
		
		try
		{
			viewWrapper = getViewWrapper();
		}
		catch (MisplacedTagException e)
		{
			return exitWithTagError("Field tag is not placed within a view wrapper tag");
		}
		
		// record context can be already set if it is represented by a record context proxy
		// or if it has been set on a previous call to this field tag
		if (this.recordContext == null)
		{
			// get parent object details tag
			ObjectDetailsTag parentDetails = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
			
			this.recordContext = parentDetails;
			if (this.recordContext == null)
			{
				throw new JspException("OutputField tag needs to be places inside an ObjectDetails tag");
			}
		}
		
		if (this.recordContext instanceof ObjectDetailsTag)
		{
			// retrieve the field value from the parent record
			Record record = ((ObjectDetailsTag)this.recordContext).getRecord();
			
			if (record == null)
			{
				return exitWithTagError("Record not set on the object details tag");
			}
			
			try
			{
				this.value = record.getField(this.fieldApiName, ((ObjectDetailsTag)this.recordContext).getFailOnUninitializedFields());
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error reading value of field " + this.fieldApiName + " from record. Nested: " + e.getMessage());
			}
			
			// inherit mode from parent object details tag
			if (this.mode == null)
			{
				try
				{
					setMode(((ObjectDetailsTag)this.recordContext).getMode());
				}
				catch (KommetException e)
				{
					return exitWithTagError("Error inheriting view/edit mode from parent object details tag. Nested: " + e.getMessage());
				}
			}
		}
		else if (this.recordContext instanceof RecordContextProxy)
		{
			// we are outside the object details tag, so the field value could either be passed in the
			// "value" attribute, or is empty and should be read from the record
			if (this.value == null)
			{
				try
				{
					this.value = this.record.getField(this.fieldApiName);
				}
				catch (KommetException e)
				{
					return exitWithTagError("Error reading value of field " + this.fieldApiName + "from record. Nested: " + e.getMessage());
				}
			}
		}
		
		if (this.mode == null)
		{
			return exitWithTagError("Tag view/edit mode not determined");
		}
		
		// get the type of the record
		Type type = this.recordContext.getType();
		
		try
		{
			String qualifiedFieldName = getName();
			Field inputField = null;
			
			try
			{
				inputField = type.getField(qualifiedFieldName, getEnv());
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
			}
			
			if (inputField == null)
			{
				this.recordContext.addErrorMsgs("Field " + qualifiedFieldName + " does not exist on type " + type.getQualifiedName());
				return EVAL_PAGE;
			}
			
			// make sure the field is not an inverse collection
			/*if (inputField.getDataTypeId().equals(DataType.INVERSE_COLLECTION) || inputField.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				this.recordContext.addErrorMsgs("Cannot use tag outputField to display related list/association " + inputField.getApiName() + ". Use tag relatedList or association instead.");
				return EVAL_PAGE;
			}*/
			
			// if user cannot edit this field, do not render it
			// TODO refreshIfUpdated option is used in the canEditField method below, but it may
			// slow it down - refresh privileges once and then call the method with this option
			// turned off
			if (!viewWrapper.getAuthData().canReadField(inputField, true, viewWrapper.getEnv()))
			{
				return EVAL_PAGE;
			}
			
			// OutputField can be placed:
			// 1. Directly under objectDetails
			// 2. Directly under propertyTable
			// 3. Within propertyValue
			Tag parent = getParent();
			if (parent instanceof ObjectDetailsTag)
			{
				// if the tag is placed directly under objectDetails, we pass it to the parent tag and
				// it will be responsible to render it properly
				((ObjectDetailsTag)parent).addField((FieldTag)BeanUtils.cloneBean(this));
				return EVAL_PAGE;
			}
			else if (parent instanceof PropertyTableTag)
			{
				((PropertyTableTag)parent).addField((FieldTag)BeanUtils.cloneBean(this));
				((PropertyTableTag)parent).addChild(this);
				
				// make sure tags propertyLabel/propertyValue are not mixed with tag outputField placed directly under
				// propertyTable
				if (((PropertyTableTag)parent).hasChildWithType(PropertyLabelTag.class) || ((PropertyTableTag)parent).hasChildWithType(PropertyValueTag.class))
				{
					return exitWithTagError("Tags propertyLabel/propertyValue cannot be mixed with tag outputField placed directly under propertyTable");
				}
				
				return EVAL_PAGE;
			}
			// the last possibility is for the tab to be placed within a property value tag,
			// or to be placed completely outside any parent tag (in which case the record context will be
			// an instance of RecordContextProxy)
			else if (parent instanceof PropertyValueTag || this.recordContext instanceof RecordContextProxy)
			{
				// just render the field (without label)
				if (this.mode == TagMode.VIEW)
				{
					writeToPage(OutputFieldTag.getCode(this.value, inputField, qualifiedFieldName, this.recordContext.getRecordId(), getId(), getCssClass(), getCssStyle(), true, this.pageContext, getEnv(), getParentView().getUserService(), viewWrapper.getAuthData(), viewWrapper));
				}
				else if (this.mode == TagMode.EDIT)
				{
					writeToPage(InputFieldTag.getCode(this.value, inputField, qualifiedFieldName, this.recordContext.getRecordId(), null, this.recordContext.getFieldNamePrefix(), getId(), getCssClass(), getCssStyle(), this.recordContext.getRecordId() != null, getPageData().getRmParams(), this.pageContext, getEnv(), viewWrapper.getAuthData(), getParentView().getI18n(), getParentView()));
				}
				else
				{
					return exitWithTagError("Unsupported tag mode " + this.mode);
				}
			}
			else
			{
				return exitWithTagError("Misplaced field tag for property " + this.fieldApiName);
			}
		}
		catch (UninitializedFieldException e)
		{
			e.printStackTrace();
			throw new JspException("Uninitialized field " + getName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new JspException("Error rendering page: " + e.getMessage());
		}
		
		return EVAL_PAGE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		cleanUp();
		return EVAL_PAGE;
    }
	
	@Override
	protected void cleanUp()
	{
		this.recordContext = null;
		this.value = null;
		this.fieldApiName = null;
		this.record = null;
		this.id = null;
		this.value = null;
		this.mode = null;
		super.cleanUp();
	}

	public void setName(String field)
	{
		this.fieldApiName = field;
	}

	public String getName()
	{
		return fieldApiName;
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
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public Object getValue()
	{
		return value;
	}

	public void setValue(Object value)
	{
		this.value = value;
	}
	
	public RecordProxy getRecord()
	{
		return record;
	}

	public void setRecord(RecordProxy record)
	{
		this.record = record;
	}
	
	public void setMode(String mode) throws KommetException
	{
		this.mode = TagMode.fromString(mode);
	}

	public String getMode()
	{
		return this.mode.stringValue();
	}
}
