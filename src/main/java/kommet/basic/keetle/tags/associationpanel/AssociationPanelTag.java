/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.associationpanel;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.tags.ObjectDetailsTag;
import kommet.basic.keetle.tags.KommetTag;
import kommet.basic.keetle.tags.MisplacedTagException;
import kommet.basic.keetle.tags.TagException;
import kommet.basic.keetle.tags.TagMode;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.utils.MiscUtils;

public class AssociationPanelTag extends KommetTag
{
	private static final long serialVersionUID = 3403990690153016500L;

	/**
	 * API name of the field that represents the association.
	 */
	private String field;
	
	/**
	 * ID of the record for which the association is displayed.
	 * This ID has priority over the ID of the record retrieved from the parent object details tag.
	 */
	private String sRecordId;
	
	private String displayedFields;
	private String addAction;
	private String viewAction;
	private String addButtonLabel;
	private TagMode mode;
	private List<PanelButton> buttons;

	public AssociationPanelTag() throws KommetException
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
		ObjectDetailsTag parentDetails = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		KID recordId = null;
		
		if (parentDetails == null)
		{
			// parent details is not set, but perhaps record ID is specified
			if (!StringUtils.hasText(this.sRecordId))
			{
				return exitWithTagError("Association panel tag is not placed within objectDetails tag and record ID is not defined.");
			}
		}
		else
		{
			try
			{
				recordId = parentDetails.getRecord().getKID();
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error retrieving parent record ID. Nested: " + e.getMessage());
			}
		}
		
		if (StringUtils.hasText(this.sRecordId))
		{
			try
			{
				recordId = KID.get(this.sRecordId);
			}
			catch (KIDException e)
			{
				return exitWithTagError("Invalid record ID value '" + this.sRecordId + "'");
			}
		}
		
		AssociationPanel panel = null;
		
		try
		{
			panel = new AssociationPanel(getEnv());
			panel.setAddActionURL(this.addAction);
			panel.setViewActionURL(this.viewAction);
			panel.setDisplayFields(this.displayedFields != null ? MiscUtils.splitAndTrim(this.displayedFields, ",") : null);
			panel.setRecordId(recordId);
			panel.setType(getEnv().getTypeByRecordId(recordId).getQualifiedName());
			panel.setAssociationField(this.field);
			panel.setAddButtonLabel(StringUtils.hasText(this.addButtonLabel) ? this.addButtonLabel : getParentView().getAuthData().getI18n().get("btn.new"));
			
			if (this.buttons != null && !this.buttons.isEmpty())
			{
				panel.setButtons(this.buttons);
			}
		}
		catch (KommetException e)
		{
			return exitWithTagError(MiscUtils.getExceptionDesc(e));
		}
		
		if (this.mode == null)
		{
			this.mode = TagMode.VIEW;
		}
		
		try
		{
			writeToPage(panel.getCode(null, this.mode, getHost()));
		}
		catch (TagException e)
		{
			return exitWithTagError(MiscUtils.getExceptionDesc(e));
		}
		catch (MisplacedTagException e)
		{
			return exitWithTagError(MiscUtils.getExceptionDesc(e));
		}
		catch (KommetException e)
		{
			return exitWithTagError(MiscUtils.getExceptionDesc(e));
		}
		
		cleanUp();
		return EVAL_PAGE;
    }
	
	@Override
	protected void cleanUp()
	{
		this.field = null;
		this.displayedFields = null;
		this.addAction = null;
		this.mode = null;
		this.buttons = null;
		this.sRecordId = null;
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

	public void setDisplayedFields(String displayedFields)
	{
		this.displayedFields = displayedFields;
	}

	public String getDisplayedFields()
	{
		return displayedFields;
	}

	public void setAddAction(String addAction)
	{
		this.addAction = addAction;
	}

	public String getAddAction()
	{
		return addAction;
	}

	public void setViewAction(String viewAction)
	{
		this.viewAction = viewAction;
	}

	public String getViewAction()
	{
		return viewAction;
	}

	public void setAddButtonLabel(String addButtonLabel)
	{
		this.addButtonLabel = addButtonLabel;
	}

	public String getAddButtonLabel()
	{
		return addButtonLabel;
	}

	public void setMode(String mode) throws KommetException
	{
		this.mode = TagMode.fromString(mode);
	}

	public String getMode()
	{
		return this.mode.stringValue();
	}
	
	public void addButton(PanelButton button)
	{
		if (this.buttons == null)
		{
			this.buttons = new ArrayList<PanelButton>();
		}
		this.buttons.add(button);
	}

	public List<PanelButton> getButtons()
	{
		return buttons;
	}

	public String getRecordId()
	{
		return sRecordId;
	}

	public void setRecordId(String recordId)
	{
		this.sRecordId = recordId;
	}
}
