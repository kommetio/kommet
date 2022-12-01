/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.fieldhistory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.FieldHistory;
import kommet.basic.FieldHistoryOperation;
import kommet.basic.keetle.tags.MisplacedTagException;
import kommet.basic.keetle.tags.ObjectDetailsTag;
import kommet.basic.keetle.tags.KommetTag;
import kommet.basic.keetle.tags.TagMode;
import kommet.basic.keetle.tags.UserLinkTag;
import kommet.basic.keetle.tags.ViewTag;
import kommet.basic.keetle.tags.objectlist.ObjectListConfig;
import kommet.basic.keetle.tags.objectlist.ObjectListTag;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.filters.FieldHistoryFilter;
import kommet.filters.QueryResultOrder;
import kommet.i18n.I18nDictionary;
import kommet.i18n.Locale;
import kommet.utils.MiscUtils;

public class FieldHistoryTag extends KommetTag
{
	private static final long serialVersionUID = -7074364434701318363L;
	private String title;
	private List<FieldHistoryField> fields;
	
	/**
	 * ID of the record for which the association is displayed.
	 * This ID has priority over the ID of the record retrieved from the parent object details tag.
	 */
	private String sRecordId;

	public FieldHistoryTag() throws KommetException
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
				return exitWithTagError("Field history tag is not placed within objectDetails tag and record ID is not defined.");
			}
		}
		else
		{
			if (!TagMode.VIEW.stringValue().equals(parentDetails.getMode()))
			{
				// skip field history tag in non-view mode
				return EVAL_PAGE;
			}
			
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
		
		I18nDictionary i18n = null;
		ViewTag parentView = null;
		AuthData authData = null;
		try
		{
			parentView = getParentView();
			authData = parentView.getAuthData();
			i18n = parentView.getI18n().getDictionary(authData.getUser().getLocaleSetting());
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error reading record ID from object details tag: " + e.getMessage());
		}
		
		Type type;
		try
		{
			type = getEnv().getTypeByRecordId(recordId);
		}
		catch (KommetException e1)
		{
			return exitWithTagError("Failed to deduce type from record ID " + recordId);
		}
		
		Map<String, List<FieldHistory>> fieldHistoryListsByFieldLabel = new HashMap<String, List<FieldHistory>>();
		
		// keep track of the order in which sections will be displayed
		List<String> orderedSectionLabels = new ArrayList<String>();
		
		try
		{
			// add the type label as the first one because immediate properties
			// of the record will be displayed first
			orderedSectionLabels.add(type.getInterpretedLabel(authData));
			
			// first retrieve field history of the main record
			fieldHistoryListsByFieldLabel.put(type.getInterpretedLabel(authData), getFieldHistory(recordId));
			getAdditionalFields(fieldHistoryListsByFieldLabel, orderedSectionLabels, type, recordId, authData);
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			return exitWithTagError("Error retrieving field history: " + e.getMessage());
		}
		
		Map<KID, Record> recordsById = null;
		
		try
		{
			// IDs of records referenced by field history logs that need to be fetched separately
			List<KID> missingRecordIds = new ArrayList<KID>();
			for (List<FieldHistory> fhs : fieldHistoryListsByFieldLabel.values())
			{
				for (FieldHistory fh : fhs)
				{
					// if this was an operation on collections
					if (FieldHistoryOperation.ADD.toString().equals(fh.getOperation()))
					{
						missingRecordIds.add(KID.get(fh.getNewValue()));
					}
					if (FieldHistoryOperation.REMOVE.toString().equals(fh.getOperation()))
					{
						missingRecordIds.add(KID.get(fh.getOldValue()));
					}
				}
			}
			
			recordsById = getViewWrapper().getDataService().getRecordMap(missingRecordIds, getParentView().getAuthData(), getEnv());
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			return exitWithTagError("Error fetching additional records for field history tag: " + e.getMessage());
		}
		
		String listId = parentView.nextComponentId();
		StringBuilder code = new StringBuilder();
		code.append("<div ");
		
		if (parentDetails != null)
		{
			code.append(" class=\"").append(parentDetails.getRelatedListCssClass()).append("\" ");
		}
		
		code.append("id=\"").append(listId).append("\">");
		
		String actualTitle = StringUtils.hasText(this.title) ? this.title : i18n.get("fieldhistory.title");
		
		try
		{
			code.append(ObjectListConfig.detachedBtnPanel(null, actualTitle, null, null, null, getEnv()));
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			return exitWithTagError("Error rendering buttons for field history tag: " + e.getMessage());
		}
		
		// warning container
		code.append("<div id=\"warn-").append(listId).append("\"></div>");
		// start record table
		code.append("<table class=\"std-table fh\">");
		List<String> columnNames = MiscUtils.toList(i18n.get("fieldhistory.fieldname"), i18n.get("fieldhistory.oldvalue"), i18n.get("fieldhistory.newvalue"), i18n.get("fieldhistory.date"), i18n.get("fieldhistory.user"));
		code.append(ObjectListTag.getTableHeader(columnNames, null, null));
		
		try
		{
			if (!fieldHistoryListsByFieldLabel.isEmpty())
			{
				for (String fieldLabel : orderedSectionLabels)
				{
					List<FieldHistory> fieldHistoryForField = fieldHistoryListsByFieldLabel.get(fieldLabel);
					
					if (fieldHistoryForField.isEmpty())
					{
						// if there is not field history for the given field, skip the whole field section
						continue;
					}
					
					Type recordType = getEnv().getTypeByRecordId(fieldHistoryForField.iterator().next().getRecordId());
					
					code.append("<tbody>");
					
					// If there is only one field label on the list, it will be the label of the
					// parent record itself. If no other fields are displayed, there is no need to
					// add the label in the header.
					if (orderedSectionLabels.size() > 1)
					{
						code.append("<tr class=\"section\"><td colspan=\"100\">").append(fieldLabel).append("</td></tr>");
					}
					
					// render information about each field change for the current object
					for (FieldHistory fh : fieldHistoryForField)
					{
						code.append("<tr id=\"file-row-").append(fh.getId()).append("\">");
						Field field = recordType.getField(fh.getFieldId());
						code.append("<td>").append(field != null ? field.getInterpretedLabel(authData) : i18n.get("fieldhistory.deletedfield")).append("</td>");
						code.append("<td>").append(interpreteValue(fh.getOldValue(), i18n, recordsById, fh.getOperation(), authData.getLocale())).append("</td>");
						code.append("<td>").append(interpreteValue(fh.getNewValue(), i18n, recordsById, fh.getOperation(), authData.getLocale())).append("</td>");
						// render date according to user's locale
						code.append("<td>").append(MiscUtils.formatDateTimeByUserLocale(fh.getCreatedDate(), parentView.getAuthData())).append("</td>");
						code.append("<td>").append(UserLinkTag.getCode(fh.getCreatedBy().getId(), getEnv(), getHost(), parentView.getUserService())).append("</td>");
						code.append("</tr>");
					}
					code.append("</tbody>");
				}
			}
			else
			{
				code.append(ObjectListConfig.getNoResultsMsg(columnNames.size(), parentView.getI18n().get(parentView.getAuthData().getUser().getLocaleSetting(), "msg.noresults")));
			}
		}
		catch (KommetException e)
		{
			parentView.addErrorMsgs("Error rendering files list: " + e.getMessage());
			return EVAL_PAGE;
		}
		
		code.append("</table>");
		code.append("</div>");
		
		// create anchor so that users can be taken directly to the list
		String anchor = "<a name=\"anchor-" + listId + "\"></a>";
		
		if (parentDetails != null)
		{
			// if tab is embedded in object details, add it to the parent details tag
			parentDetails.addTab(actualTitle, anchor + code.toString(), listId, null);
		}
		else
		{
			// write tag code to the page
			writeToPage(code.toString());
		}
		
		cleanUp();
		return EVAL_PAGE;
    }
	
	@Override
	public void cleanUp()
	{
		this.title = null;
		this.fields = null;
		super.cleanUp();
	}
	
	/**
	 * Get field history of all fields of the given record.
	 * @param recordId Record ID
	 * @return
	 * @throws MisplacedTagException
	 * @throws KommetException
	 */
	private List<FieldHistory> getFieldHistory (KID recordId) throws MisplacedTagException, KommetException
	{
		FieldHistoryFilter filter = new FieldHistoryFilter();
		filter.addRecordId(recordId);
		filter.setOrder(QueryResultOrder.DESC);
		filter.setOrderBy(Field.CREATEDDATE_FIELD_NAME);
		return getParentView().getFieldHistoryService().get(filter, getEnv());
	}

	/**
	 * Get IDs of additional fields for which field history will be retrieved.
	 * @param fieldHistoryListsByFieldLabel 
	 * @param orderedSectionLabels 
	 * @return
	 * @throws KommetException 
	 */
	private void getAdditionalFields(Map<String, List<FieldHistory>> fieldHistoryListsByFieldLabel, List<String> orderedSectionLabels, Type type, KID recordId, AuthData authData) throws KommetException
	{	
		if (this.fields == null || this.fields.isEmpty())
		{
			return;
		}
		
		List<String> queriedFields = new ArrayList<String>();
		for (FieldHistoryField field : this.fields)
		{
			// query ID field name of each associated object
			queriedFields.add(field.getField() + "." + Field.ID_FIELD_NAME);
		}
		
		List<Record> records = getEnv().select(type, queriedFields, Field.ID_FIELD_NAME + " = '" + recordId.getId() + "'", authData);
		
		if (records.size() != 1)
		{
			throw new KommetException("Expected exactly one result when record is retrieved by ID " + recordId + ", instead got " + records.size() + ". Authenticated user KID " + authData.getUser().getId());
		}
		
		Record record = records.get(0);
		
		for (FieldHistoryField field : this.fields)
		{
			String label = type.getField(field.getField()).getInterpretedLabel(authData);
			orderedSectionLabels.add(label);
			fieldHistoryListsByFieldLabel.put(label, getFieldHistory((KID)record.getField(field.getField() + "." + Field.ID_FIELD_NAME)));
		}
	}

	private String interpreteValue(String val, I18nDictionary i18n, Map<KID, Record> recordsById, String operation, Locale locale) throws KommetException
	{
		if (val == null)
		{
			return "&lt;" + i18n.get("fieldhistory.valuenotexisted") + "&gt;";
		}
		else if ("".equals(val))
		{
			return "&lt;" + i18n.get("fieldhistory.emptyvalue") + "&gt;";
		}
		else if (FieldHistoryOperation.ADD.toString().equals(operation) || FieldHistoryOperation.REMOVE.toString().equals(operation))
		{
			return recordsById.get(KID.get(val)).getDefaultFieldValue(locale);
		}
		else
		{
			return val;
		}
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public void setFields(List<FieldHistoryField> fields)
	{
		this.fields = fields;
	}

	public List<FieldHistoryField> getFields()
	{
		return fields;
	}

	public void addField(FieldHistoryField field)
	{
		if (this.fields == null)
		{
			this.fields = new ArrayList<FieldHistoryField>();
		}
		this.fields.add(field);
	}

	public String getRecordId()
	{
		return sRecordId;
	}

	public void setRecordId(String sRecordId)
	{
		this.sRecordId = sRecordId;
	}
}
