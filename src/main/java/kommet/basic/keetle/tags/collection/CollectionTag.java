/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.collection;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.tags.ObjectDetailsTag;
import kommet.basic.keetle.tags.KommetTag;
import kommet.basic.keetle.tags.TagMode;
import kommet.basic.keetle.tags.collection.Collection.CollectionCode;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;

public class CollectionTag extends KommetTag
{
	private String field;
	private String parentId;
	private TagMode mode;
	
	private static final long serialVersionUID = 3315329528080352721L;

	public CollectionTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		Collection coll = new Collection();
		
		if (StringUtils.hasText(this.parentId))
		{	
			try
			{
				coll.setParentId(KID.get(this.parentId));
			}
			catch (KIDException e)
			{
				return exitWithTagError("Invalid parent ID " + this.parentId);
			}
		}
		else
		{
			ObjectDetailsTag parentDetails = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
			
			try
			{
				if (parentDetails == null)
				{
					return exitWithTagError("Collection tag is not placed within objectDetails tag and parent ID not specified");
				}
				
				if (parentDetails.getRecord().getKID() == null)
				{
					return exitWithTagError("Collection tag cannot be used with unsaved records");
				}
				
				coll.setParentId(parentDetails.getRecord().getKID());
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error reading record from object list: " + e.getMessage());
			}
		}
		
		Type type = null;
		Field relationField = null;
		
		try
		{
			type = getEnv().getTypeByRecordId(coll.getParentId());
			relationField = type.getField(this.field);
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error reading type or field: " + e.getMessage());
		}
		
		if (relationField == null)
		{
			return exitWithTagError("Field with name " + this.field + " not found on type " + type.getQualifiedName());
		}
		
		coll.setRelationField(relationField);
		coll.setType(type);
		
		if (this.mode == null)
		{
			this.mode = TagMode.VIEW;
		}
		
		try
		{
			CollectionCode code = coll.getCode(this.mode, getViewWrapper().getAuthData(), getEnv());
			
			// include JS libraries
			//getViewWrapper().addPreViewCode(KeetleUtil.includeJSLibraries(getContextPath()));
			
			// include initialization code at the end of the view
			getViewWrapper().addPostViewCode(code.getInitializationCode());
			
			// render the element code, which is essentially just a placeholder
			// to be filled by the initialization code
			writeToPage(code.getElementCode());
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error rendering collection tag: " + e.getMessage());
		}
		
		cleanUp();
		return EVAL_PAGE;
    }
	
	@Override
	protected void cleanUp()
	{
		this.field = null;
		this.parentId = null;
		this.mode = null;
		super.cleanUp();
	}

	public void setField(String field)
	{
		this.field = field;
	}

	public String getField()
	{
		return field;
	}

	public void setParentId(String parentId)
	{
		this.parentId = parentId;
	}

	public String getParentId()
	{
		return parentId;
	}

	public void setMode(String mode) throws KommetException
	{
		this.mode = TagMode.fromString(mode);
	}

	public String getMode()
	{
		return mode.stringValue();
	}
}
