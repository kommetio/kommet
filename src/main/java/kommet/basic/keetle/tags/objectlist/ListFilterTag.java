/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.objectlist;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.tags.FilterField;
import kommet.basic.keetle.tags.InputFieldTag;
import kommet.basic.keetle.tags.MisplacedTagException;
import kommet.basic.keetle.tags.KommetTag;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.utils.UrlUtil;

public class ListFilterTag extends KommetTag
{
	private static final long serialVersionUID = -9157168781264360587L;
	private List<FilterField> filterFields;
	private ObjectListTag parentList;

	public ListFilterTag() throws KommetException
	{
		super();
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {
		this.parentList = (ObjectListTag)checkParentTag(ObjectListTag.class);
		this.filterFields = new ArrayList<FilterField>();
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		StringBuilder code = new StringBuilder();
		code.append("<table class=\"list-filter property-table\"><tbody><tr>");
		
		int currField = 0;
		int fieldCount = this.filterFields.size();
		
		for (FilterField field : this.filterFields)
		{
			if (currField % 2 == 0)
			{
				if (currField > 0)
				{
					code.append("</tr>");
				}
				if (currField < fieldCount)
				{
					code.append("<tr>");
				}
			}
				
			code.append("<td class=\"label\"><div class=\"property-label\">");
			
			// read label from field or used the specified one
			try
			{
				code.append(StringUtils.hasText(field.getLabel()) ? field.getLabel() : parentList.getEnv().getType(parentList.getType()).getField(field.getField()).getInterpretedLabel(getViewWrapper().getAuthData()));
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error rendering label for search field: " + e.getMessage());
			}
			code.append("</div></td>");
			code.append("<td class=\"value\"><div class=\"property-value\">");
			
			// add hidden field that describes the filter field
			code.append("<input type=\"hidden\" name=\"filter:").append(field.getHashId()).append("\" id=\"filter:").append(field.getHashId()).append("\" ");
			code.append("value=\"").append(field.getField()).append(":").append(field.getComparison()).append("\">");
			
			try
			{
				Type parentListType = getEnv().getType(parentList.getType());
				Field typeField = parentListType.getField(field.getField());
				
				if (typeField.getDataType().isCollection())
				{
					throw new KommetException("Collection field " + typeField.getApiName() + " cannot be used as list filter");
				}
				
				// TODO - is this an optimal way to render search filter fields?
				code.append(InputFieldTag.getCode(null, typeField, field.getField(), null, field.getHashId(), null, field.getHashId(), null, null, false, getPageData().getRmParams(), pageContext, getEnv(), getParentView().getAuthData(), getParentView().getI18n(), getParentView()));
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error rendering filter panel: " + e.getMessage());
			}
			
			// add input field for the search filter
			//code.append("<input type=\"text\" name=\"").append(field.getHashId()).append("\">");
			
			code.append("</div></td>");
			
			currField++;
			if (currField % 2 == 0)
			{
				code.append("<td class=\"sep\"></td>");
			}
		}
		
		if (currField % 2 == 1)
		{
			code.append("<td colspan=\"2\"></td></tr>");
		}
		
		code.append("<tr><td colspan=\"2\">");
		
		// Render the search button.
		// The search button is rendered as type="submit" to make the browser react to clicking enter on the form.
		// To prevent actually submitting the form, "return false" is added to the onclick attribute.
		
		try
		{
			code.append("<input type=\"submit\" value=\"").append(getParentView().getI18n().get(getParentView().getAuthData().getUser().getLocaleSetting(), "btn.search")).append("\" onclick=\"submitObjectListSearch(objectListConfigs['").append(parentList.getId()).append("'], 'searchform").append(parentList.getId()).append("', '").append(parentList.getConfig().getRecordListId()).append("', '").append(getHost() + "/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("');");
		}
		catch (MisplacedTagException e)
		{
			return exitWithTagError("Misplaced tag error: " + e.getMessage());
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error rendering tag: " + e.getMessage());
		}
		
		code.append(" return false;\" class=\"sbtn\">");
		code.append("</td></tr>");
		
		code.append("</tbody></table>");
		
		
		// code has to be buffered to be render later
		parentList.setListFilterCode(code.toString());
		
		return EVAL_PAGE;
    }
	
	public void addFilterField (FilterField filterField)
	{
		this.filterFields.add(filterField);
	}
}
