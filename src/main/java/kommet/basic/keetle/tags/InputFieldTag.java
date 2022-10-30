/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Dictionary;
import kommet.basic.DictionaryItem;
import kommet.basic.keetle.tags.buttons.ButtonPanel;
import kommet.basic.keetle.tags.buttons.ButtonPrototype;
import kommet.basic.keetle.tags.buttons.ButtonType;
import kommet.basic.keetle.tags.collection.Collection;
import kommet.basic.keetle.tags.collection.Collection.CollectionCode;
import kommet.basic.keetle.tags.objectlist.ListColumn;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.UninitializedFieldException;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.i18n.I18nDictionary;
import kommet.i18n.InternationalizationService;
import kommet.utils.MiscUtils;
import kommet.utils.XMLUtil;
import kommet.web.rmparams.KmParamNode;

public class InputFieldTag extends FieldTag
{
	private static final long serialVersionUID = 5298038975706840459L;
	
	public InputFieldTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		ObjectDetailsTag parent = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		KID recordId = null;
		
		if (parent == null)
		{
			// parent details is not set, but perhaps record ID is specified
			if (this.record == null)
			{
				return exitWithTagError("Input field tag is not placed within objectDetails tag and record ID is not defined.");
			}
		}
		else
		{
			this.recordContext = parent;
			
			try
			{
				recordId = parent.getRecord().attemptGetKID();
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error retrieving parent record ID. Nested: " + e.getMessage());
			}
		}
		
		if (this.record != null)
		{
			recordId = this.record.getId();		
			RecordContextProxy rcp = new RecordContextProxy();
			rcp.setRecordId(recordId);
			try
			{
				rcp.setType(getEnv().getTypeByRecordId(recordId));
			}
			catch (KommetException e)
			{
				return exitWithTagError("Could not deduce type from record ID " + recordId);
			}
			
			this.recordContext = rcp;
		}
		
		Type type = this.recordContext.getType();
		Field inputField;
		try
		{
			inputField = type.getField(getName());
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error getting information for field " + getName() + " on type " + type.getQualifiedName());
		}
		
		if (getName().equals(Field.ID_FIELD_NAME) && inputField.getDataType().getId().equals(DataType.KOMMET_ID) && parent != null)
		{
			parent.setIdFieldRendered(true);
		}
		
		return super.doStartTag();
    }
	
	public static String getCode (Object fieldValue, Field field, String qualifiedFieldName, KID recordId, String inputName, String fieldNamePrefix, String id, String cssClass, String cssStyle, boolean failOnUninitialized, KmParamNode rmParams, PageContext pageContext, EnvData env, AuthData authData, InternationalizationService i18n, ViewTag viewTag) throws KommetException
	{	
		if (field == null)
		{
			throw new KommetException("Field is empty in tag InputField");
		}
		else if (Field.isSystemField(field.getApiName()))
		{
			throw new KommetException("Input field tag cannot be used for system field " + field.getApiName());
		}
		
		StringBuilder code = new StringBuilder();
		String strValue = null;
		Object value = null;
		
		try
		{
			if (field.getDataTypeId().equals(DataType.DATETIME))
			{
				Object val = fieldValue;
				Date date = SpecialValue.NULL.equals(val) ? null : (Date)val;
				strValue = date != null ? MiscUtils.formatDateTimeByUserLocale(date, authData) : "";
			}
			else if (field.getDataTypeId().equals(DataType.DATE))
			{
				Object val = fieldValue;
				Date date = SpecialValue.NULL.equals(val) ? null : (Date)val;
				strValue = date != null ? MiscUtils.formatDateByUserLocale(date, authData) : "";
			}
			else if (field.getDataType().isCollection())
			{
				// ignore and do not read text value for collection
			}
			else
			{
				strValue = field.getDataType().getStringValue(fieldValue, authData.getLocale());
			}
			value = fieldValue;
			
			if (SpecialValue.NULL.equals(value))
			{
				value = null;
			}
		}
		catch (UninitializedFieldException e)
		{
			if (failOnUninitialized)
			{
				throw e;
			}
			else
			{
				strValue = "";
			}
		}
		
		if (inputName == null)
		{
			inputName = (StringUtils.hasText(fieldNamePrefix) ? fieldNamePrefix : "") + qualifiedFieldName;
		}
		
		// render field HTML depending on the field type
		if (field.getDataType().getId().equals(DataType.TYPE_REFERENCE))
		{
			// check the referenced type
			Type referencedType = ((TypeReference)field.getDataType()).getType();
			
			if (referencedType.getKeyPrefix().equals(KeyPrefix.get(KID.FILE_PREFIX)))
			{
				String fileLookupId = "file-" + MiscUtils.getHash(10);
				String fileLookup = "<div id=\"" + fileLookupId + "\"><img src=\"" + pageContext.getServletContext().getContextPath() + "/resources/images/attachicon.png\" class=\"km-wait\"></img></div>";
				code.append(fileLookup);
				
				//code.append("<input type=\"hidden\" name=\"").append(inputName + "." + Field.ID_FIELD_NAME).append("\" id=\"" + inputId + "\"></input>");
				
				StringBuilder fileInitScript = new StringBuilder();
				
				// start wrapper function
				fileInitScript.append("(function() {");
				
				// define file dialog
				fileInitScript.append("var upload = km.js.filelookup.show({");
				fileInitScript.append("fileId: ").append(value != null ? "\"" + ((Record)value).getKID() + "\"" : "null").append(", ");
				fileInitScript.append("target: $(\"#").append(fileLookupId).append("\"), ");
				fileInitScript.append("inputName: \"").append(inputName + "." + Field.ID_FIELD_NAME).append("\", ");
				fileInitScript.append("afterSave: function(fileId, revisionId, fileName, options) {");
				
				// after file is uploaded and saved, we set its ID to the hidden input field
				//fileInitScript.append("$(\"#").append(inputId).append("\").val(fileId);");
				fileInitScript.append("options.fileId = fileId;");
				
				// rerender file lookup
				fileInitScript.append("km.js.filelookup.show(options);");
				
				fileInitScript.append("}");
				
				fileInitScript.append("});");
				
				// end wrapper function
				fileInitScript.append("})();");
				
				// append script for loading file lookup
				viewTag.appendScript(fileInitScript.toString());
			}
			else
			{
				// display the default field
				List<String> displayFields = new ArrayList<String>();
				String defaultField = env.getType(((TypeReference)field.getDataType()).getType().getKID()).getDefaultFieldApiName();
				displayFields.add(defaultField);
				
				// create columns to be displayed
				List<ListColumn> columns = new ArrayList<ListColumn>();
				ListColumn col1 = new ListColumn();
				col1.setField(defaultField);
				col1.setLink(true);
				columns.add(col1);
				ListColumn col2 = new ListColumn();
				col2.setField("createdDate");
				columns.add(col2);
				
				ButtonPanel btnPanel = null;
				
				// if user can create type, add a "new" button to the lookup list by default
				if (authData.canCreateType(referencedType.getKID(), true, env))
				{
					btnPanel = new ButtonPanel(false);
					btnPanel.addButton(new ButtonPrototype(ButtonType.NEW));
				}
				
				code.append(LookupTag.getCode(referencedType, (Record)value, columns, qualifiedFieldName, inputName + "." + Field.ID_FIELD_NAME, displayFields, null, null, null, btnPanel, rmParams, pageContext, i18n, viewTag, env));
			}
		}
		else if (field.getDataType().isCollection())
		{
			Collection coll = new Collection();
			coll.setParentId(recordId);
			
			Type type = env.getTypeByRecordId(coll.getParentId());
			Field relationField = type.getField(qualifiedFieldName);
			coll.setRelationField(relationField);
			coll.setType(type);
			
			CollectionCode collCode = coll.getCode(TagMode.EDIT, authData, env); 
			
			code.append(collCode.getElementCode());
			viewTag.getViewWrapper().addPostViewCode(collCode.getInitializationCode());
		}
		else if (field.getDataType().getId().equals(DataType.ENUMERATION))
		{
			code.append(getSelectInput(field, strValue, id, inputName, cssClass, cssStyle, authData, env));
		}
		else if (field.getDataType().getId().equals(DataType.TEXT) && ((TextDataType)field.getDataType()).isLong())
		{
			code.append(getTextAreaInput(field, strValue, id, inputName, cssClass, cssStyle));
		}
		else if (field.getDataType().getId().equals(DataType.BOOLEAN))
		{
			code.append(getBooleanSelectInput(field, (Boolean)value, id, inputName, cssClass, cssStyle, authData.getI18n()));
			//code.append(getCheckboxInput(field, (Boolean)value, id, inputName, cssClass, cssStyle, authData.getI18n()));
		}
		else if (field.getDataType().getId().equals(DataType.DATETIME) || field.getDataType().getId().equals(DataType.DATE))
		{
			code.append(getDateTimeInput(field, strValue, id, inputName, cssClass, cssStyle));
		}
		else
		{
			code.append(getTextInput(field, strValue, id, inputName, cssClass, cssStyle));
		}
		
		return code.toString();
	}

	private static String getDateTimeInput(Field field, String value, String id, String name, String cssClass, String cssStyle) throws KommetException
	{
		if (!StringUtils.hasText(id))
		{
			// we need it for JQuery to be able to attach datepicker to the field
			id = "field" + MiscUtils.getHash(5);
		}
		String datePickerCode = MiscUtils.scriptTag("$(function() { $(\"#" + MiscUtils.escapeHtmlId(id) + "\").datepicker({ dateFormat: \"yy-mm-dd\" }); });");
		return getTextInput(field, value, id, name, cssClass != null ? cssClass + " dp" : "dp", cssStyle) + datePickerCode;
	}

	/*private static String getCheckboxInput(Field field, Boolean value, String id, String fieldName, String cssClass, String cssStyle, I18nDictionary i18n)
	{
		StringBuilder code = new StringBuilder();
		code.append("<input type=\"checkbox\" class=\"km-checkbox\" value=\"true\" ");
		
		if ("true".equals(value))
		{
			code.append("checked ");
		}
		
		XMLUtil.addStandardTagAttributes(code, id, fieldName, cssClass, cssStyle);
		
		code.append("></input>");
		return code.toString();
	}*/

	private static String getBooleanSelectInput(Field field, Boolean value, String id, String fieldName, String cssClass, String cssStyle, I18nDictionary i18n)
	{
		StringBuilder code = new StringBuilder();
		code.append("<select");
		XMLUtil.addStandardTagAttributes(code, id, fieldName, cssClass, cssStyle);
		
		// first add an empty option
		code.append("><option value=\"\"");
		if (value == null)
		{
			code.append(" selected");
		}
		code.append("></option>");
		code.append("<option value=\"true\"");
		if (Boolean.TRUE.equals(value))
		{
			code.append(" selected");
		}
		code.append("/>").append(i18n.get("bool.yes")).append("</option>");
		code.append("<option value=\"false\"");
		if (Boolean.FALSE.equals(value))
		{
			code.append(" selected");
		}
		code.append("/>").append(i18n.get("bool.no")).append("</option>");
		code.append("</select>");
		return code.toString();
	}
	
	private static Object getSelectInput(Field inputField, String strValue,	String id, String fieldName, String cssClass, String cssStyle, AuthData authData, EnvData env)
	{
		StringBuilder code = new StringBuilder();
		code.append("<select");
		XMLUtil.addStandardTagAttributes(code, id, fieldName, cssClass, cssStyle);
		code.append(">");
		
		// add empty option
		code.append("<option value=\"\"></option>");
		
		EnumerationDataType dt = ((EnumerationDataType)inputField.getDataType()); 
		
		List<String> values = dt.getValueList();
		
		if (values.isEmpty() && dt.getDictionary() != null)
		{
			Dictionary dict = env.getDictionaries().get(dt.getDictionary().getId());
			
			values = new ArrayList<String>();
			
			// items are already sorted
			for (DictionaryItem item : dict.getItems())
			{
				String displayValue = authData.getUserCascadeSettings().get(item.getKey());
				if (displayValue == null)
				{
					displayValue = item.getName();
				}
				
				code.append("<option value=\"").append(item.getName()).append("\"");
				
				if (item.getName().equals(strValue))
				{
					code.append(" selected");
				}
				
				code.append(">").append(displayValue).append("</option>");
			}
		}
		else
		{
			for (String value : values)
			{
				code.append("<option value=\"").append(value).append("\"");
				
				if (value.equals(strValue))
				{
					code.append(" selected");
				}
				
				code.append(">").append(value).append("</option>");
			}
		}
		
		code.append("</select>");
		
		return code.toString();
	}
	
	private static String getTextInput(Field field, String value, String id, String name, String cssClass, String cssStyle)
	{
		StringBuilder code = new StringBuilder();
		code.append("<input type=\"text\"");
		if (StringUtils.hasText(value))
		{
			code.append(" value=\"").append(value).append("\"");
		}
		XMLUtil.addStandardTagAttributes(code, id, name, cssClass, cssStyle);
		code.append("></input>");
		
		return code.toString();
	}
	
	private static String getTextAreaInput(Field field, String value, String id, String name, String cssClass, String cssStyle)
	{
		StringBuilder code = new StringBuilder();
		code.append("<textarea ");
		XMLUtil.addStandardTagAttributes(code, id, name, cssClass, cssStyle);
		code.append(">");
		if (StringUtils.hasText(value))
		{
			code.append(value);
		}
		code.append("</textarea>");
		
		return code.toString();
	}
}
