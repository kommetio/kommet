/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.buttons;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.tags.TagException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.Type;
import kommet.i18n.I18nDictionary;
import kommet.utils.UrlUtil;
import kommet.utils.XMLUtil;
import kommet.web.ParamInterpreter;

/**
 * Utility class for generating standard buttons (and their code). 
 * @author Radek Krawiec
 * @created 10-03-2014
 *
 */
public class ButtonFactory
{
	public static Button getEditButton(KID recordId, I18nDictionary i18n, String contextPath)
	{
		return getEditButton(recordId, i18n.get("btn.edit"), null, null, i18n, contextPath);
	}
	
	public static Button getEditButton(KID recordId, String label, String url, String onClick, I18nDictionary i18n, String contextPath)
	{
		String actualUrl = contextPath + "/";
		if (StringUtils.hasText(url))
		{
			actualUrl += ParamInterpreter.interpret(url, recordId, null);
		}
		else
		{
			actualUrl += recordId + "/e";
		}
		
		StringBuilder editBtn = new StringBuilder("<a");
		XMLUtil.attr("href", actualUrl, editBtn);
		XMLUtil.attr("onclick", onClick, editBtn);
		editBtn.append(" class=\"sbtn\">").append(label).append("</a>");
		return new Button(editBtn.toString(), ButtonType.EDIT, label);
	}
	
	public static Button getDeleteButton(KID recordId, I18nDictionary i18n, String contextPath)
	{
		return getDeleteButton(recordId, i18n.get("btn.delete"), i18n, contextPath);
	}
	
	public static Button getDeleteButton(KID recordId, String label, I18nDictionary i18n, String contextPath)
	{
		StringBuilder delBtn = new StringBuilder();
		delBtn.append("<a href=\"javascript:;\" onclick=\"ask('").append(i18n.get("msg.deleterecord.warning")).append("', 'warnPrompt', function() { deleteRecord('").append(recordId).append("', '").append(contextPath + "/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("', '").append(contextPath).append("'); }, null, '").append(i18n.get("btn.yes")).append("', '").append(i18n.get("btn.no")).append("')\" ");
		delBtn.append("class=\"sbtn\">").append(label).append("</a>");
		return new Button(delBtn.toString(), ButtonType.DELETE, label);
	}
	
	public static Button getListButton(Type type, I18nDictionary i18n, String contextPath)
	{
		StringBuilder btn = new StringBuilder();
		String label = type.getPluralLabel();
		btn.append("<a href=\"").append(contextPath + "/" + type.getKeyPrefix()).append("\" class=\"sbtn\">").append(label).append("</a>");
		return new Button(btn.toString(), ButtonType.LIST, label);
	}
	
	public static Button getNewButton(KeyPrefix keyPrefix, I18nDictionary i18n, String contextPath)
	{
		return getNewButton(keyPrefix, i18n.get("btn.new"), null, null, i18n, contextPath);
	}
	
	public static Button getNewButton(KeyPrefix keyPrefix, String label, String url, String onClick, I18nDictionary i18n, String contextPath)
	{
		String actualUrl = contextPath + "/";
		if (StringUtils.hasText(url))
		{
			actualUrl += ParamInterpreter.interpret(url, null, keyPrefix);
		}
		else
		{
			actualUrl += keyPrefix.getPrefix() + "/n";
		}
		
		StringBuilder btn = new StringBuilder();
		btn.append("<a href=\"").append(actualUrl).append("\"");
		XMLUtil.attr("onclick", onClick, btn);
		btn.append(" class=\"sbtn\">").append(label).append("</a>");
		return new Button(btn.toString(), ButtonType.NEW, label);
	}

	/**
	 * Get any type of button, depending on the passed type.
	 * @param type
	 * @param recordId
	 * @param prefix
	 * @param formId
	 * @param i18n
	 * @param contextPath
	 * @return
	 * @throws TagException
	 */
	public static Button getButton(ButtonType type, KID recordId, KeyPrefix prefix, String formId, I18nDictionary i18n, String contextPath) throws TagException
	{
		switch (type)
		{
			case DELETE: return getDeleteButton(recordId, i18n, contextPath);
			case EDIT: return getEditButton(recordId, i18n, contextPath);
			case NEW: return getNewButton(prefix, i18n, contextPath);
			case SAVE: return getSaveButton(prefix, formId, i18n, contextPath);
			case CANCEL: return getCancelButton(recordId, prefix, i18n, contextPath);
			default: throw new TagException("Cannot render prototype button of type " + type, ButtonTag.class.getName());
		}
	}
	
	public static Button getSaveButton(KeyPrefix prefix, String formId, I18nDictionary i18n, String contextPath)
	{
		return getSaveButton(prefix, formId, i18n.get("btn.delete"), i18n, contextPath);
	}
	
	public static Button getSaveButton(KeyPrefix prefix, String formId, String label, I18nDictionary i18n, String contextPath)
	{
		StringBuilder code = new StringBuilder();
		code.append("<a id=\"saveBtn\" href=\"javascript:;\" onclick=\"document.getElementById('" + formId + "').action = '" + contextPath + "/save/" + prefix.getPrefix() + "'; document.getElementById('" + formId + "').submit();\" class=\"sbtn\">").append(label).append("</a>");
		return new Button(code.toString(), ButtonType.SAVE, label);
	}
	 
	public static Button getCancelButton(KID recordId, KeyPrefix prefix, I18nDictionary i18n, String contextPath)
	{
		return getCancelButton(recordId, prefix, i18n.get("btn.cancel"), i18n, contextPath);
	}
	
	public static Button getCancelButton(KID recordId, KeyPrefix prefix, String label, I18nDictionary i18n, String contextPath) 
	{
		StringBuilder code = new StringBuilder("<a href=\"").append(contextPath);
		if (recordId == null)
		{
			// if new record was cancelled, go back to object list
			code.append("/").append(prefix).append("\" class=\"sbtn\">").append(label).append("</a>");
		}
		else
		{
			// if existing record edit was canceled, go back to this records details
			code.append("/").append(recordId).append("\" class=\"sbtn\">").append(label).append("</a>");
		}
		return new Button(code.toString(), ButtonType.CANCEL, label);
	}
}
