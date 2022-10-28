/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.associationpanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.tags.TagException;
import kommet.basic.keetle.tags.TagMode;
import kommet.data.Field;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;
import kommet.utils.XMLUtil;

/**
 * Configuration of an association panel.
 * @author Radek Krawiec
 * @created 08/03/2014
 */
public class AssociationPanel
{
	// CSS class of the panel DIV (apc - association panel container)
	private static final String PANEL_CSS_CLASS = "apc";
	
	// fields whose value will be displayed for selected objects
	private List<String> displayFieldNames;
	
	// User-defined name of the type of the field to which this association belongs.
	// E.g. if we are displaying association company.employees, then type
	// will be the company type.
	private String typeName;
	
	// The name of the association field
	private String associationFieldName;
	
	// Environment for which the tag is displayed.
	// We need this to interpret type and field names.
	private EnvData env;
	
	// URL of the action called when user adds a new object to the list.
	// The URL can contain interpreted parameter $field, which will be replaced with the ID
	// of the association field.
	private String addActionURL;
	
	// URL of the action to be called when user views details of the item. If not specified, the standard
	// details action of the type will be used.
	private String viewActionURL;
	
	// Type represented by the type name property
	private Type type;
	
	// ID of the parent record.
	// Since association cannot be displayed for unsaved records, this ID must not be null.
	private KID recordId;
	
	// Fields to be displayed
	private List<Field> displayFields;
	
	private Field associationField;
	
	private Type associatedType;
	
	private String addButtonLabel;
	
	private String refreshedPanelId;
	
	private List<PanelButton> buttons;

	public AssociationPanel (EnvData env)
	{
		this.env = env;
		this.viewActionURL = "$record.id";
	}
	
	/**
	 * Returns HTML code of an association panel.
	 * @param items - Optional list of records to be displayed when users open the select dialog.
	 * @param mode - View or edit mode
	 * @param contextPath
	 * @return
	 * @throws KommetException 
	 */
	public String getCode(List<Record> items, TagMode mode, String contextPath) throws TagException
	{
		try
		{
			prepare();
		}
		catch (KommetException e)
		{
			throw new TagException("Error preparing code " + e.getMessage(), this.getClass().getName());
		}
		
		int randomId = (new Random()).nextInt(1000);
		
		if (refreshedPanelId != null)
		{
			randomId = Integer.valueOf(refreshedPanelId);
		}
		
		// if items have not been passed to the method, retrieve them from the database
		if (items == null)
		{
			try
			{
				items = fetchItems();
			}
			catch (TagException e)
			{
				throw e;
			}
			catch (KommetException e)
			{
				throw new TagException("Error fetching records for association: " + MiscUtils.getExceptionDesc(e), this.getClass().getName());
			}
		}
		
		String addActionContainer = this.associationFieldName + "_add_" + randomId;
		String panelContainer = this.associationFieldName + "_panel_" + randomId;
		
		StringBuilder code = new StringBuilder();
		
		if (refreshedPanelId == null)
		{
			code.append("<div class=\"").append(PANEL_CSS_CLASS).append("\" id=\"").append(panelContainer).append("\">");
		}
		
		code.append("<ul>");
		
		// if in edit mode, show the "add" button
		if (TagMode.EDIT.equals(mode))
		{
			String fullAddActionURL = contextPath + "/";
			try
			{
				fullAddActionURL += this.addActionURL + "&km.layout=" + env.getBlankLayoutId();
			}
			catch (KommetException e1)
			{
				throw new TagException("Error getting blank layout for environment", this.getClass().getName());
			}
			
			fullAddActionURL += "&assocFieldIds=" + this.associationField.getKID();
			fullAddActionURL += "&widgetId=" + randomId;
			
			// add button for adding new object
			code.append("<li class=\"add\" onclick=\"");
			code.append("$('").append(addActionContainer).append("').rialog({url:'").append(fullAddActionURL).append("'})");
			code.append("\"><img src=\"").append(contextPath).append("/resources/images/attachment.png\">");
			code.append("</li>");
		}
		
		for (Record item : items)
		{
			try
			{
				addItemCode(code, item, mode, contextPath);
			}
			catch (KommetException e)
			{
				throw new TagException("Error rendering associated item: " + MiscUtils.getExceptionDesc(e), this.getClass().getName());
			}
		}
		
		code.append("</ul></div>");
		
		if (refreshedPanelId == null)
		{
			code.append("<div id=\"").append(addActionContainer).append("\"></div>");
			
			// add script for refreshing the whole association panel
			code.append("<script type=\"text/javascript\">");
			
			code.append("function refreshAssociationPanel_").append(randomId).append("() {");
			code.append("associationPanel('").append(this.associationFieldName + "_panel_" + randomId).append("', '");
			code.append(associationField.getKID()).append("', '").append(recordId).append("', '");
			code.append(MiscUtils.implode(displayFieldNames, ",")).append("','").append(addActionURL);
			code.append("','").append(viewActionURL).append("','").append(contextPath).append("', ").append(randomId).append(");");
			code.append("}");
			
			code.append("</script>");
		}
		
		return code.toString();
	}

	private void addItemCode (StringBuilder code, Record item, TagMode mode, String contextPath) throws KommetException
	{
		code.append("<li class=\"item\"><a href=\"").append(contextPath).append("/").append(this.viewActionURL.replaceAll("\\$record.id", item.getKID().getId())).append("\">");
		for (String displayField : this.displayFieldNames)
		{
			code.append(item.getField(displayField)).append(" ");
		}
		
		code.append("</a>");
		
		if (this.buttons != null && !this.buttons.isEmpty())
		{
			for (PanelButton btn : this.buttons)
			{
				addPanelButtonCode(btn, code, item, contextPath);
			}
		}
		
		// if in edit mode, add the "remove" button
		if (TagMode.EDIT.equals(mode))
		{
			code.append("<img src=\"").append(contextPath).append("/resources/images/ex.png\" ");
			
			// add unassociate call
			code.append(" onclick=\"unassociate('").append(associationField.getKID()).append("', '");
			code.append(this.recordId).append("', '").append(item.getKID()).append("', '");
			code.append(contextPath).append("'); $(this).closest('li').remove();\">");
		}
		
		code.append("</li>");
	}

	private void addPanelButtonCode(PanelButton btn, StringBuilder code, Record item, String contextPath) throws KommetException
	{
		String actualURL = contextPath + "/" + btn.getUrl();
		actualURL = actualURL.replaceAll("\\$record.id", item.getKID().getId());
		if (btn.getType() == PanelButton.BUTTON_TYPE_BUTTON)
		{
			code.append("<input type=\"button\" value=\"").append(btn.getLinkText()).append("\" ");
			code.append(" onclick=\"openUrl('").append(actualURL).append("')\" ");
			XMLUtil.addStandardTagAttributes(code, null, null, btn.getCssClass(), btn.getCssStyle());
			code.append("></input>");
		}
		else if (btn.getType() == PanelButton.BUTTON_TYPE_TEXT_LINK)
		{
			code.append("<a href=\"").append(actualURL).append("\" ");
			XMLUtil.addStandardTagAttributes(code, null, null, btn.getCssClass(), btn.getCssStyle());
			code.append(">").append(btn.getLinkText()).append("</a>");
		}
		else
		{
			throw new KommetException("Unsupported association panel button type: " + btn.getType());
		}
	}

	/**
	 * Retrieves items associated with this record by the association for which the panel is displayed.
	 * @throws KommetException 
	 */
	@SuppressWarnings("unchecked")
	private List<Record> fetchItems() throws KommetException
	{
		List<String> associationFields = new ArrayList<String>();
		associationFields.add(this.associationField.getApiName() + "." + Field.ID_FIELD_NAME);
		
		for (String field : this.displayFieldNames)
		{
			associationFields.add(this.associationField.getApiName() + "." + field);
		}
		
		List<Record> baseRecords = this.env.select(this.type, associationFields, Field.ID_FIELD_NAME + " = '" + this.recordId.getId() + "'", null);
		
		if (baseRecords.isEmpty())
		{
			throw new TagException("Record of type " + this.typeName + " with ID " + this.recordId + " not found", this.getClass().getName());
		}
		
		return (List<Record>)baseRecords.get(0).getField(this.associationFieldName);
	}

	private void prepare() throws KommetException
	{
		if (env == null)
		{
			throw new TagException("Env data not passed to tag", this.getClass().getName());
		}
		
		try
		{
			this.type = env.getType(this.typeName);
		}
		catch (KommetException e)
		{
			throw new TagException("Error getting type with name " + this.typeName + " from environment " + env.getName() + ". Probably type name is not user-defined.", this.getClass().getName());
		}
		
		// check if type exists
		if (type == null)
		{
			throw new TagException("Type " + this.typeName + " not found on environment " + env.getName(), this.getClass().getName());
		}
		
		// check if field exists on type
		if (!StringUtils.hasText(this.associationFieldName))
		{
			throw new TagException("Association field name is empty in association panel", this.getClass().getName());
		}
		
		try
		{
			this.associationField = this.type.getField(this.associationFieldName);
		}
		catch (KommetException e1)
		{
			throw new TagException("Error getting field " + this.associationFieldName + " from type " + this.typeName, this.getClass().getName());
		}
		
		if (this.associationField == null)
		{
			throw new TagException("Association field " + this.associationFieldName + " not found on type " + this.typeName, this.getClass().getName());
		}
		
		if (!this.associationField.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			throw new TagException("Field " + this.associationFieldName + " specified for association panel is not an association field", this.getClass().getName());
		}
		
		// get associated type
		try
		{
			this.associatedType = env.getType(((AssociationDataType)associationField.getDataType()).getAssociatedTypeId());
		}
		catch (KommetException e)
		{
			throw new TagException("Error getting associated type with ID " + ((AssociationDataType)associationField.getDataType()).getAssociatedTypeId(), this.getClass().getName());
		}
		
		// check if display fields have been specified
		if (this.displayFieldNames == null || this.displayFieldNames.isEmpty())
		{
			this.displayFieldNames = new ArrayList<String>();
			this.displayFieldNames.add(this.associatedType.getDefaultFieldApiName());
		}
		
		this.displayFields = new ArrayList<Field>();
		
		for (String fieldName : displayFieldNames)
		{
			Field field = null;
			try
			{
				field = this.associatedType.getField(fieldName);
			}
			catch (KommetException e)
			{
				throw new TagException("Error getting field " + fieldName + " from type " + this.typeName, this.getClass().getName());
			}
			
			if (field == null)
			{
				throw new TagException("Field " + fieldName + " not found on type " + this.associatedType.getQualifiedName(), this.getClass().getName());
			}
			
			this.displayFields.add(field);
		}
		
		// check if record ID is set
		if (this.recordId == null)
		{
			throw new TagException("Record ID not set for association panel", this.getClass().getName());
		}
		
		try
		{
			// check if record ID is valid for the type
			if (!this.recordId.getKeyPrefix().equals(this.type.getKeyPrefix()))
			{
				throw new TagException("Record ID " + this.recordId + " is not a valid ID for type " + this.typeName, this.getClass().getName());
			}
		}
		catch (KeyPrefixException e)
		{
			throw new TagException("Error checking ID consistency for record ID " + this.recordId + ": " + MiscUtils.getExceptionDesc(e), this.getClass().getName());
		}
	}

	public void setDisplayFields(List<String> displayFields)
	{
		this.displayFieldNames = displayFields;
	}

	public List<String> getDisplayFields()
	{
		return displayFieldNames;
	}

	public void setType(String type)
	{
		this.typeName = type;
	}

	public String getType()
	{
		return typeName;
	}

	public void setAssociationField(String field)
	{
		this.associationFieldName = field;
	}

	public String getAssociationField()
	{
		return associationFieldName;
	}

	public void setEnv(EnvData env)
	{
		this.env = env;
	}

	public EnvData getEnv()
	{
		return env;
	}

	public void setAddActionURL(String addActionURL)
	{
		this.addActionURL = addActionURL;
	}

	public String getAddActionURL()
	{
		return addActionURL;
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
	}

	public KID getRecordId()
	{
		return recordId;
	}

	public void setViewActionURL(String viewAction)
	{
		this.viewActionURL = viewAction;
	}

	public String getViewActionURL()
	{
		return viewActionURL;
	}
	
	public String getAddButtonLabel()
	{
		return addButtonLabel;
	}

	public void setAddButtonLabel(String addButtonLabel)
	{
		this.addButtonLabel = addButtonLabel;
	}

	public void setRefreshedPanelId(String refreshedPanelId)
	{
		this.refreshedPanelId = refreshedPanelId;
	}

	public String getRefreshedPanelId()
	{
		return refreshedPanelId;
	}

	public void addButton(PanelButton button)
	{
		if (this.buttons == null)
		{
			this.buttons = new ArrayList<PanelButton>();
		}
		this.buttons.add(button);
	}
	
	public void setButtons(List<PanelButton> buttons)
	{
		this.buttons = buttons;
	}

	public List<PanelButton> getButtons()
	{
		return buttons;
	}
}
