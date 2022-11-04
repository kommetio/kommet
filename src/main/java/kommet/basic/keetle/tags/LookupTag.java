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
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.keetle.tags.buttons.Button;
import kommet.basic.keetle.tags.buttons.ButtonPanel;
import kommet.basic.keetle.tags.buttons.ButtonPrototype;
import kommet.basic.keetle.tags.buttons.ButtonType;
import kommet.basic.keetle.tags.objectlist.ListColumn;
import kommet.data.DataAccessUtil;
import kommet.data.Field;
import kommet.data.NullifiedRecord;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.i18n.I18nDictionary;
import kommet.i18n.InternationalizationService;
import kommet.utils.MiscUtils;
import kommet.utils.NestedContextField;
import kommet.web.kmparams.KmParamNode;

public class LookupTag extends FieldTag implements ListDisplay
{
	private static final long serialVersionUID = -299953238144411416L;
	
	/**
	 * User-defined type name.
	 */
	private String type;
	
	/**
	 * The actual record selected in this lookup tag.
	 */
	private Record value;

	/**
	 * Fields of the referenced type to be displayed on the list
	 */
	private List<ListColumn> columns;
	
	/**
	 * Fields to be displayed when an item is selected.
	 */
	private List<String> displayFields;
	
	/**
	 * The name of the hidden input field that will contain the actual value.
	 */
	private String inputName;
	
	/**
	 * Variable under which the current record is available for columns.
	 * Defaults to "record".
	 */
	private String itemVar;
	
	/**
	 * DAL filter containing additional conditions on the records.
	 * The filter has a form of DAL WHERE-clause, without the WHERE keyword at the beginning.
	 */
	private String filter;
	
	/**
	 * Custom button definitions specified by the user in the buttons tag.
	 */
	private ButtonPanel buttonPanel;
	
	/**
	 * Lookup title - literal value.
	 */
	private String title;
	
	/**
	 * Lookup title - i18n key.
	 */
	private String titleKey;
	
	private Type recordType;
	
	private String afterSelect;
	
	public LookupTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		// create a default button panel, which can later be overridden by user defined buttons
		this.setButtonPanel(new ButtonPanel(false));
		
		super.doStartTag();
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
	public int doEndTag() throws JspException
	{
		try
		{
			try
			{
				this.recordType = getEnv().getType(type);
			}
			catch (KommetException e)
			{
				throw new TagErrorMessageException("Error getting type: " + e.getMessage());
			}
			
			if (this.recordType == null)
			{
				return exitWithTagError("Type " + type + " not found");
			}
			
			if (this.columns == null || this.columns.isEmpty())
			{
				ListColumn defaultColumn = new ListColumn();
				defaultColumn.setField(this.recordType.getDefaultFieldApiName());
				defaultColumn.setLink(true);
				defaultColumn.setLabel(this.recordType.getDefaultField().getInterpretedLabel(getViewWrapper().getAuthData()));
				this.columns = new ArrayList<ListColumn>();
				this.columns.add(defaultColumn);
			}
			else
			{
				// check if any column is a link column, if not, make the first one a link column
				boolean linkColumnSet = false;
				for (ListColumn col : this.columns)
				{
					if (col.isLink())
					{
						linkColumnSet = true;
						break;
					}
				}
				
				if (!linkColumnSet)
				{
					this.columns.get(0).setLink(true);
				}
			}
			
			if (!StringUtils.hasText(this.inputName))
			{
				this.inputName = getName();
			}
			
			AuthData authData = getParentView().getAuthData();
			
			// check if default list title has been overridden
			String actualTitle = StringUtils.hasText(this.titleKey) ? authData.getI18n().get(this.titleKey) : this.title;
			
			this.pageContext.getOut().write(getCode(this.recordType, value, this.columns, getName(), this.inputName, displayFields, this.filter, actualTitle, this.afterSelect, this.buttonPanel, getPageData().getRmParams(), pageContext, getParentView().getI18n(), getParentView(), getEnv()));
		}
		catch (IOException e)
		{
			cleanUp();
			throw new JspException("Error writing to JSP page: " + e.getMessage());
		}
		catch (TagErrorMessageException e)
		{
			return exitWithTagError(e.getMessage());
		}
		catch (KommetException e)
		{
			cleanUp();
			throw new JspException("Error rendering lookup tag: " + e.getMessage());
		}
		
		cleanUp();
		return EVAL_PAGE;
	}
	
	/**
	 * Returns HTML code of the look-up generated basing on multiple parameters.
	 * @param type
	 * @param value
	 * @param fields
	 * @param fieldName
	 * @param visibleFieldName
	 * @param lookupId
	 * @param displayFields
	 * @param linkFields
	 * @param customFieldLabels
	 * @param filter
	 * @param buttonPanel
	 * @param rmParams
	 * @param pageContext
	 * @param i18n
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static String getCode (Type type, Record value, List<ListColumn> columns, String fieldName, String hiddenFieldName, List<String> displayFields, String filter, String title, String afterSelect, ButtonPanel buttonPanel, KmParamNode rmParams, PageContext pageContext, InternationalizationService i18n, ViewTag viewTag, EnvData env) throws KommetException
	{	
		type = env.getType(type.getKeyPrefix());
		if (displayFields == null || displayFields.isEmpty())
		{
			throw new TagErrorMessageException("Display fields must be set");
		}
		else if (displayFields.size() > 1)
		{
			// TODO implement the possibility to add more than one display field
			throw new TagErrorMessageException("Lookup tag implementation does not currently allow more than one display field");
		}
		
		AuthData authData = AuthUtil.getAuthData(pageContext.getSession());
		List<NestedContextField> displayFieldList;
		try
		{
			displayFieldList = DataAccessUtil.getReadableFields(type, authData, displayFields, env);//MiscUtils.getFieldNamesFromList(displayFields, type);
		}
		catch (KommetException e)
		{
			throw new TagErrorMessageException("Error getting fields to display in lookup tag: " + e.getMessage());
		}
		
		StringBuilder code = new StringBuilder();
		
		// TODO dots in ID fields don't work well with dialogs, but shouldn't we handle it in
		// a different way than replacing them with underscores?
		String visibleInputFieldId = fieldName.replaceAll("\\.", "_") + "_lookup";
		
		// get field names used in lookup columns
		List<String> fieldNames = new ArrayList<String>();
		for (ListColumn col : columns)
		{
			if (!StringUtils.hasText(col.getField()))
			{
				throw new TagException("Lookup tag can only contain columns displaying fields, but one column displays a formula: " + col.getFormula(), LookupTag.class.getName());
			}
			
			fieldNames.add(col.getField());
		}
		
		// create an input for the lookup
		code.append("<input type=\"text\" id=\"" + visibleInputFieldId + "\" readonly=\"true\"").append("></input>");
		
		// turn this simple input into an km.js.ref field
		appendLookup(type, visibleInputFieldId, hiddenFieldName, hiddenFieldName, (value != null && !(value instanceof NullifiedRecord)) ? value.getKID() : null, title, columns, displayFieldList, afterSelect, buttonPanel, viewTag, authData, env);
		
		return code.toString();
	}
	
	private static void appendLookup (Type type, String visibleInputFieldId, String inputFieldId, String inputFieldName, KID selectedRecordId, String title, List<ListColumn> columns, List<NestedContextField> displayFieldList, String afterSelect, ButtonPanel buttonPanel, ViewTag viewTag, AuthData authData, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		
		List<String> queryProperties = new ArrayList<String>();
		List<String> displayProperties = new ArrayList<String>();
		
		for (ListColumn col : columns)
		{
			queryProperties.add("{ name: \"" + col.getField() + "\" }");
			
			String label = StringUtils.hasText(col.getLabel()) ? col.getLabel() : type.getField(col.getField()).getInterpretedLabel(authData);
			displayProperties.add("{ name: \"" + col.getField() + "\", \"label\": \"" + label + "\", linkStyle: " + col.isLink() + " }");
		}
		
		if (columns.isEmpty())
		{
			// by default query the ID and default field
			queryProperties.add("{ name: \"" + Field.ID_FIELD_NAME + "\" }");
			queryProperties.add("{ name: \"" + type.getDefaultFieldApiName() + "\" }");
			
			// by default display the default field - obviously
			displayProperties.add("{ name: \"" + type.getDefaultFieldApiName() + "\", \"label\": \"" + type.getDefaultFieldLabel(authData) + "\" }");
		}
		
		// create jcr to query type
		String jcrVar = "jcr";
		code.append("var ").append(jcrVar).append(" = {");
		code.append("baseTypeName: \"").append(type.getQualifiedName()).append("\",");
		code.append("properties: [");
		code.append(MiscUtils.implode(queryProperties, ", "));
		code.append("]");
		// end jcr
		code.append("};");
		
		// create display properties
		StringBuilder display = new StringBuilder();
		display.append("{ properties: [");
		display.append(MiscUtils.implode(displayProperties, ", "));
		display.append("], idProperty: { name: \"" + Field.ID_FIELD_NAME + "\" }");
		// end display properties
		display.append("}");
		
		// create available items options
		StringBuilder availableItemsOptions = new StringBuilder();
		availableItemsOptions.append("var availableItemsOptions = {");
		availableItemsOptions.append("display: ").append(display).append(", ");
		availableItemsOptions.append("options: { ");
		
		List<String> options = new ArrayList<String>();
		
		if (StringUtils.hasText(title))
		{
			options.add("title: \"" + title + "\"");
		}
		
		String lookupId = viewTag.nextComponentId();
		
		if (buttonPanel != null && !buttonPanel.getButtons().isEmpty())
		{
			// create a button panel definition with the new button defined
			options.add(getButtonPanelDefinition(type, buttonPanel, authData.getI18n(), lookupId, env));
		}
		
		availableItemsOptions.append(MiscUtils.implode(options, ", "));
		
		// end available items options
		availableItemsOptions.append("} };");
		
		code.append(availableItemsOptions);
		
		// create the lookup
		code.append("var lookup = km.js.ref.create({");
		code.append("id: \"").append(lookupId).append("\",");
		code.append("selectedRecordDisplayField: { name: \"").append(displayFieldList.get(0).getNestedName()).append("\" },");
		code.append("jcr: ").append(jcrVar).append(", ");
		code.append("availableItemsDialogOptions: {},");
		code.append("availableItemsOptions: availableItemsOptions,");
		code.append("inputName: \"").append(inputFieldName).append("\"");
		
		if (StringUtils.hasText(afterSelect))
		{
			code.append(", afterSelect: ").append(afterSelect);
		}
		
		if (selectedRecordId != null)
		{
			code.append(", selectedRecordId: \"").append(selectedRecordId).append("\"");
		}
		
		code.append("});");
		
		code.append("lookup.render($(\"#").append(visibleInputFieldId).append("\"));");
		
		viewTag.appendScript("(function() {" + code.toString() + "})();");
	}
	
	private static String getButtonPanelDefinition(Type type, ButtonPanel btnPanel, I18nDictionary i18n, String lookupId, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		code.append("buttonPanel: (function() {");
		code.append("var btnPanel = km.js.buttonpanel.create({ id: \"").append(lookupId + "-btns").append("\" });");
		
		for (ButtonPrototype btn : btnPanel.getButtons())
		{
			if (btn.getType().equals(ButtonType.NEW))
			{
				code.append("btnPanel.addButton({ label: \"").append(i18n.get("btn.new")).append("\", url: km.js.config.contextPath + \"/").append(type.getKeyPrefix()).append("/n?rm.lookup=").append(lookupId).append("&rm.layout=").append(env.getBlankLayoutId()).append("\"");
				
				if (StringUtils.hasText(btn.getId()))
				{
					code.append(" id=\"").append(btn.getId()).append("\"");
				}
				
				code.append(" });");
			}
			else if (btn.getType().equals(ButtonType.CUSTOM))
			{
				code.append("btnPanel.addButton({ label: \"").append(((Button)btn).getLabel()).append("\", url: \"").append(((Button)btn).getUrl()).append("\"");
				
				if (StringUtils.hasText((btn).getId()))
				{
					code.append(" id=\"").append((btn.getId())).append("\"");
				}
				
				code.append(" });");
			}
			else
			{
				throw new KommetException("Unsupported button type " + btn.getType() + " on lookup items list");
			}
		}
		
		code.append("return btnPanel;");
		
		// end function call
		code.append("})()");
		
		return code.toString();
	}

	@Override
	protected void cleanUp()
	{
		this.inputName = null;
		this.columns = null;
		this.title = null;
		this.titleKey = null;
		this.setButtonPanel(null);
	}
	
	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public void setValue(Record value)
	{
		this.value = value;
	}

	public Record getValue()
	{
		return value;
	}

	public void setFilter(String filter)
	{
		this.filter = filter;
	}

	public String getFilter()
	{
		return filter;
	}

	public void setButtonPanel(ButtonPanel buttonPanel)
	{
		this.buttonPanel = buttonPanel;
	}

	public ButtonPanel getButtonPanel()
	{
		return buttonPanel;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitleKey(String titleKey)
	{
		this.titleKey = titleKey;
	}

	public String getTitleKey()
	{
		return titleKey;
	}
	
	public void addColumn(ListColumn col)
	{
		if (this.columns == null)
		{
			this.columns = new ArrayList<ListColumn>();
		}
		this.columns.add(col);
	}
	
	public Type getRecordType()
	{
		return this.recordType;
	}

	public void setItemVar(String itemVar)
	{
		this.itemVar = itemVar;
	}

	public String getItemVar()
	{
		return itemVar;
	}

	/**
	 * Set fields to be displayed when an item is selected.
	 * @param displayFields Comma separated list of field names
	 */
	public void setDisplayFields(String displayFields)
	{
		if (StringUtils.hasText(displayFields))
		{
			this.displayFields = MiscUtils.splitAndTrim(displayFields, ",");
		}
		else
		{
			this.displayFields = new ArrayList<String>();
		}
	}

	public String getInputName()
	{
		return inputName;
	}

	public void setInputName(String inputName)
	{
		this.inputName = inputName;
	}

	public String getAfterSelect()
	{
		return afterSelect;
	}

	public void setAfterSelect(String afterSelect)
	{
		this.afterSelect = afterSelect;
	}
}
