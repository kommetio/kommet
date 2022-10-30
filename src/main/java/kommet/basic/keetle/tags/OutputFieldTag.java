/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.Date;

import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.basic.DictionaryItem;
import kommet.basic.keetle.tags.collection.Collection;
import kommet.basic.keetle.tags.collection.Collection.CollectionCode;
import kommet.data.Field;
import kommet.data.NullifiedRecord;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.UninitializedFieldException;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TextDataType;
import kommet.env.EnvData;
import kommet.i18n.I18nDictionary;
import kommet.utils.MiscUtils;
import kommet.utils.XMLUtil;

public class OutputFieldTag extends FieldTag
{	
	private static final long serialVersionUID = 5070544087584051259L;
	
	public OutputFieldTag() throws KommetException
	{
		super();
	}

	/**
	 * Returns the code of the output tag.
	 * @param record
	 * @param nestedField
	 * @param qualifiedFieldName
	 * @param id
	 * @param cssClass
	 * @param cssStyle
	 * @param failOnUninitialized
	 * @param pageContext
	 * @param env
	 * @param userService
	 * @param authData
	 * @return
	 * @throws KommetException
	 */
	public static String getCode (Object fieldValue, Field nestedField, String qualifiedFieldName, KID recordId, String id, String cssClass, String cssStyle, boolean failOnUninitialized, PageContext pageContext, EnvData env, UserService userService, AuthData authData, ViewWrapperTag viewWrapper) throws KommetException
	{	
		if (nestedField == null)
		{
			throw new KommetException("Field is empty in tag InputField");
		}
		
		StringBuilder code = new StringBuilder();
		String strValue = null;
		Object value = null;
		
		try
		{
			if (nestedField.getDataTypeId().equals(DataType.DATETIME))
			{
				Date date = (Date)(fieldValue instanceof SpecialValue ? null : fieldValue);
				strValue = date != null ? MiscUtils.formatDateTimeByUserLocale(date, authData) : "";
			}
			else if (nestedField.getDataTypeId().equals(DataType.DATE))
			{
				Date date = (Date)fieldValue;
				strValue = date != null ? MiscUtils.formatDateByUserLocale(date, authData) : "";
			}
			else if (nestedField.getDataType().isCollection())
			{
				// ignore and do not read text value for collection
			}
			else
			{
				strValue = nestedField.getDataType().getStringValue(fieldValue, authData.getLocale());
			}
			
			value = fieldValue;
			
			if (value instanceof SpecialValue)
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
		
		if (nestedField.getDataTypeId().equals(DataType.TYPE_REFERENCE))
		{
			Record refRecord = (Record)fieldValue;
			
			// display field value if it is not null
			if (refRecord != null && !(refRecord instanceof NullifiedRecord))
			{
				// if it is a reference to a file, display it differently
				if (!refRecord.getType().getKeyPrefix().getPrefix().equals(KID.FILE_PREFIX))
				{
					strValue = refRecord.getDefaultFieldValue(authData.getLocale());
					code.append("<a href=\"").append(pageContext.getServletContext().getContextPath()).append("/").append(refRecord.getKID()).append("\">").append(strValue).append("</a>");
				}
				else
				{
					// if it is a reference to a file, display it differently
					code.append(FileFieldTag.getCode(refRecord.getKID(), refRecord.getDefaultFieldValue(authData.getLocale()), env, "file_" + id, pageContext.getServletContext().getContextPath()));
				}
			}
		}
		else if (nestedField.getDataType().isCollection())
		{
			Collection coll = new Collection();
			coll.setParentId(recordId);
			
			Type type = env.getTypeByRecordId(coll.getParentId());
			Field relationField = type.getField(qualifiedFieldName);
			coll.setRelationField(relationField);
			coll.setType(type);
			
			CollectionCode collCode = coll.getCode(TagMode.VIEW, authData, env); 
			
			code.append(collCode.getElementCode());
			viewWrapper.addPostViewCode(collCode.getInitializationCode());
		}
		else if (nestedField.getApiName().equals(Field.CREATEDBY_FIELD_NAME) || nestedField.getApiName().equals(Field.LAST_MODIFIED_BY_FIELD_NAME))
		{
				
			// render user fields as user links
			code.append(UserLinkTag.getCode(KID.get(strValue), env, pageContext.getServletContext().getContextPath(), userService));
		}
		else if (nestedField.getDataType().getId().equals(DataType.BOOLEAN))
		{
			code.append(getCheckboxOutput(nestedField, (Boolean)value, pageContext.getServletContext().getContextPath(), authData.getI18n()));
		}
		else if (nestedField.getDataTypeId().equals(DataType.TEXT) && ((TextDataType)nestedField.getDataType()).isLong())
		{
			code.append(getTextAreaOutput(nestedField, strValue, id, qualifiedFieldName, cssClass, cssStyle));
		}
		else if (nestedField.getDataTypeId().equals(DataType.ENUMERATION))
		{
			EnumerationDataType enumDT = (EnumerationDataType)nestedField.getDataType();
			if (enumDT.getDictionary() != null)
			{
				for (DictionaryItem item : env.getDictionaries().get(enumDT.getDictionary().getId()).getItems())
				{
					if (item.getName().equals(strValue))
					{
						String uchValue = authData.getUserCascadeSettings().get(item.getKey());
						if (StringUtils.hasText(uchValue))
						{
							strValue = uchValue;
						}
					}
				}
			}
			
			code.append(getTextOutput(nestedField, strValue, id, qualifiedFieldName, cssClass, cssStyle));
		}
		else
		{
			// render field HTML depending on the field type
			code.append(getTextOutput(nestedField, strValue, id, qualifiedFieldName, cssClass, cssStyle));
		}
		
		//code.append("</div>");
		
		return code.toString();
	}
	
	private static String getCheckboxOutput(Field field, Boolean value, String contextPath, I18nDictionary i18n)
	{	
		if (value == null)
		{
			return "";
		}
		else
		{
			String icon = Boolean.TRUE.equals(value) ? "check.gif" : "uncheck.png";
			StringBuilder sb = new StringBuilder("<img src=\"").append(contextPath).append("/resources/images/").append(icon).append("\"></img>");
			return sb.toString();
		}
		
		//return value != null ? (Boolean.TRUE.equals(value) ? i18n.get("bool.yes") : i18n.get("bool.no")) : "";
	}

	private static String getTextOutput(Field field, String value, String id, String name, String cssClass, String cssStyle)
	{
		StringBuilder code = new StringBuilder("<span");
		XMLUtil.addStandardTagAttributes(code, id, name, cssClass, cssStyle);
		code.append(">").append(value).append("</span>");
		return code.toString();
	}
	
	private static String getTextAreaOutput(Field field, String value, String id, String name, String cssClass, String cssStyle)
	{
		StringBuilder code = new StringBuilder("<span");
		XMLUtil.addStandardTagAttributes(code, id, name, cssClass, cssStyle);
		code.append(">").append(MiscUtils.newLinesToBr(value)).append("</span>");
		return code.toString();
	}
}
