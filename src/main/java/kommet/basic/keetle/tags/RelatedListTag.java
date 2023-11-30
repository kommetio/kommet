/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.keetle.StandardObjectController;
import kommet.basic.keetle.tags.buttons.ButtonPanel;
import kommet.basic.keetle.tags.datatable.DataSourceType;
import kommet.basic.keetle.tags.datatable.DataTable;
import kommet.basic.keetle.tags.datatable.DataTableColumn;
import kommet.basic.keetle.tags.objectlist.ListColumn;
import kommet.basic.keetle.tags.objectlist.ObjectListConfig;
import kommet.basic.keetle.tags.objectlist.ObjectListItemType;
import kommet.basic.keetle.tags.objectlist.ObjectListSource;
import kommet.data.DataAccessUtil;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

public class RelatedListTag extends KommetTag implements ListDisplay
{
	private static final long serialVersionUID = 3510114994038248636L;
	
	// Fields of the referenced object to be displayed on the list
	private List<ListColumn> columns;
	
	// Field name identifying the relationship being displayed as a related list.
	// It can be either an inverse collection or an association.
	private String field;
	
	// Custom title, if different than the plural label of the referenced type
	private String title;
	
	/**
	 * Variable name under which the current record will be available to
	 * code rendering list columns.
	 */
	private String itemVar;
	
	private ButtonPanel buttonPanel;
	
	private Type recordType;
	
	private ObjectDetailsTag parent;
	private DataType fieldDataType;
	private Field collectionField;
	private ObjectListConfig config;
	
	// Number of records on a single page.
	private Integer pageSize;
	
	private String sortProperty;
	private String sortOrder;
	
	private boolean isDisplayWithJavascript = true;
	private Map<String, String> newRecordPassedParams = new HashMap<String, String>();
	
	public RelatedListTag() throws KommetException
	{
		super();
	}

	@Override
    public int doStartTag() throws JspException
    {	
		this.buttonPanel = new ButtonPanel(false);
		
		if (!StringUtils.hasText(this.field))
		{
			throw new JspException("List field not set on related list tag");
		}
		
		parent = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		if (parent == null)
		{
			throw new JspException("RelatedList tag needs to be placed inside an ObjectDetails tag");
		}
		
		// if parent object details tag is rendered in edit mode, we do not display the related list
		if (!TagMode.VIEW.stringValue().equals(parent.getMode()))
		{
			return SKIP_BODY;
		}
		
		Type type = parent.getType();
		
		try
		{
			collectionField = type.getField(this.field);
		}
		catch (KommetException e1)
		{
			throw new JspException("Error getting field for name " + this.field);
		}
		
		if (collectionField == null)
		{
			parent.addErrorMsgs("Collection field " + this.field + " not found on type " + type.getQualifiedName());
			return SKIP_BODY;
		}
		
		this.fieldDataType = collectionField.getDataType();
		this.config = new ObjectListConfig(ObjectListSource.QUERY, ObjectListItemType.RECORD);
		
		if (fieldDataType.getId().equals(DataType.INVERSE_COLLECTION))
		{
			try
			{
				InverseCollectionDataType dt = (InverseCollectionDataType)collectionField.getDataType();
				// the type is refetched from env because when some type fields are updated, it is not
				// reflected in the inverse type property
				this.recordType = getEnv().getType(dt.getInverseTypeId());
				
				if (this.recordType == null)
				{
					throw new KommetException("Type not found with ID " + dt.getInverseTypeId());
				}
				
				this.newRecordPassedParams = getNewRecordPassedParams(collectionField, this.parent.getRecord(), this.parent.getAuthData());
				
				// When objects of the related type are created by the "New" button, we want the newly-created
				// record to have the reference to the parent object already set. This is why we pass information
				// about the parent object to the related list tag.
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				parent.addErrorMsgs("Error getting related list: " + e.getMessage());
				return SKIP_BODY;
			}
		}
		else if (fieldDataType.getId().equals(DataType.ASSOCIATION))
		{
			try
			{	
				// the type is refetched from env because when some type fields are updated, it is not
				// reflected in the associated type property
				this.recordType = getEnv().getType(((AssociationDataType)collectionField.getDataType()).getAssociatedTypeId());
				
				if (this.recordType == null)
				{
					throw new KommetException("Type not found with ID " + ((AssociationDataType)collectionField.getDataType()).getAssociatedTypeId());
				}
				
				this.newRecordPassedParams = getNewRecordPassedParams(collectionField, this.parent.getRecord(), this.parent.getAuthData());
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				parent.addErrorMsgs("Error getting related list: " + e.getMessage());
				return SKIP_BODY;
			}
		}
		else
		{
			throw new JspException("List field " + this.field + " is not an inverse collection or association on type " + type.getQualifiedName());
		}
		
		if (!this.newRecordPassedParams.isEmpty() && !isDisplayWithJavascript)
		{
			for (String param : this.newRecordPassedParams.keySet())
			{
				this.config.addNewObjectPassedParam(param, this.newRecordPassedParams.get(param));
			}
		}
		
		return EVAL_BODY_INCLUDE;
    }
	
	private static Map<String, String> getNewRecordPassedParams(Field field, Record record, AuthData authData) throws KommetException
	{
		Map<String, String> params = new HashMap<String, String>();
		Type type = field.getType();
		DataType dt = field.getDataType();
		
		if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
		{
			// add ID reference to the parent object
			params.put("passed." + ((InverseCollectionDataType)dt).getInverseProperty() + "." + Field.ID_FIELD_NAME, record.getKID().getId());
			// add default column reference
			try
			{
				params.put("passed." + ((InverseCollectionDataType)dt).getInverseProperty() + "." + type.getDefaultFieldApiName(), URLEncoder.encode(record.getDefaultFieldValue(authData.getLocale()), "UTF-8"));
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
				throw new KommetException("Error encoding URL params: " + e.getMessage());
			}
		}
		else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			// if a new object in an association is created, we want to let the edit form know
			// that an association table item should be created as well to bind the new object
			// to the parent
			params.put(StandardObjectController.ASSOC_FIELD_PARAM, field.getKID().getId());
			// add default column reference
			params.put(StandardObjectController.ASSOC_PARENT_PARAM, record.getKID().getId());
		}
		else
		{
			throw new KommetException("Cannot get passed params for data type " + field.getDataType().getName());
		}
		
		return params;
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public int doEndTag() throws JspException
    {	
		// if parent object details tag is rendered in edit mode, we do not display the files tab
		if (!TagMode.VIEW.stringValue().equals(parent.getMode()))
		{
			return EVAL_PAGE;
		}
				
		if (parent.hasErrorMsgs())
		{
			cleanUp();
			return EVAL_PAGE;
		}
		
		// if no fields were specified for display, display the default field
		if (this.columns == null || this.columns.isEmpty())
		{	
			ListColumn defaultColumn = new ListColumn();
			try
			{
				defaultColumn.setField(this.recordType.getDefaultFieldApiName());
			}
			catch (KommetException e)
			{
				throw new JspException("Error getting default field name: " + e.getMessage());
			}
			
			defaultColumn.setLink(true);
			try
			{
				defaultColumn.setLabel(this.recordType.getDefaultField().getInterpretedLabel(getViewWrapper().getAuthData()));
			}
			catch (KommetException e)
			{
				try
				{
					return exitWithTagError("Error getting label for default field " + this.recordType.getDefaultFieldApiName() + ": " + e.getMessage());
				}
				catch (KommetException e1)
				{
					throw new JspException("Error getting default field name: " + e.getMessage());
				}
			}
			
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
		
		Record record = parent.getRecord();
		
		StringBuilder code = new StringBuilder();
		
		Set<String> queriedFields = ObjectListConfig.extractFields(columns, itemVar);
		
		// always query ID field of objects displayed on the related list, because it may be
		// needed for creating links etc.
		queriedFields.add(Field.ID_FIELD_NAME);
		
		String sortExpression = (StringUtils.hasText(this.sortProperty) ? this.sortProperty : Field.CREATEDDATE_FIELD_NAME) + (StringUtils.hasText(this.sortOrder) ? " " + this.sortOrder : " DESC");
		
		List<String> filteredFields = null;
		try
		{
			filteredFields = DataAccessUtil.getFieldsNamesForDisplay(this.recordType, parent.getAuthData(), queriedFields, parent.getEnv());
			queriedFields.clear();
			queriedFields.addAll(filteredFields);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new JspException("Error getting fields to be displayed by the related list");
		}
		
		ViewWrapperTag viewWrapper = null;
		try
		{
			viewWrapper = getViewWrapper();
		}
		catch (MisplacedTagException e2)
		{
			parent.addErrorMsgs("Tag not placed within a view wrapper tag");
			cleanUp();
			return EVAL_PAGE;
		}
		
		// render list title - if custom title is not defined, use the plural label of the referenced type
		String actualTitle = StringUtils.hasText(this.title) ? this.title : this.recordType.getInterpretedPluralLabel(viewWrapper.getAuthData());
		String listId;
		try
		{
			listId = getParentView().nextComponentId();
		}
		catch (MisplacedTagException e1)
		{
			parent.addErrorMsgs("Tag not placed within a view tag");
			cleanUp();
			return EVAL_PAGE;
		}
		
		if (!isDisplayWithJavascript)
		{
			try
			{
				List<Record> collectionRecords = null;
				
				if (fieldDataType.getId().equals(DataType.INVERSE_COLLECTION))
				{	
					// query the inverse type for records
					collectionRecords = parent.getEnv().getSelectCriteriaFromDAL("select " + MiscUtils.implode(queriedFields, ", ") + " FROM " + this.recordType.getQualifiedName() + " WHERE " + ((InverseCollectionDataType)collectionField.getDataType()).getInverseProperty() + "." + Field.ID_FIELD_NAME + " = '" + record.getKID().getId() + "' ORDER BY " + sortExpression, parent.getAuthData()).list();
				}
				else
				{
					// prepend the name of the association to every retrieved field
					Set<String> associationFields = new HashSet<String>();
					
					// always query the default field
					associationFields.add(collectionField.getApiName() + "." + this.recordType.getDefaultFieldApiName());
					
					for (String field : queriedFields)
					{
						associationFields.add(collectionField.getApiName() + "." + field);
					}
					
					List<Record> parentRecords = parent.getEnv().getSelectCriteriaFromDAL("SELECT " + Field.ID_FIELD_NAME + ", " + MiscUtils.implode(associationFields, ", ") + " FROM " + this.collectionField.getType().getQualifiedName() + " WHERE " + Field.ID_FIELD_NAME + " = '" + record.getKID().getId() + "'", parent.getAuthData()).list();
					if (parentRecords.size() == 1)
					{
						collectionRecords = (List<Record>)parentRecords.get(0).getField(this.field);
					}
					else if (parentRecords.isEmpty())
					{
						parent.addErrorMsgs("Record with ID " + record.getKID().getId() + " not found");
						return EVAL_PAGE;
					}
					else if (parentRecords.size() > 0)
					{
						parent.addErrorMsgs("More than 1 record with ID " + record.getKID().getId() + " found");
						return EVAL_PAGE;
					}
				}
				
				code.append("<div class=\"").append(parent.getRelatedListCssClass()).append("\" id=\"").append(listId).append("\">");
				
				if (this.buttonPanel != null && this.buttonPanel.isCustomButtons())
				{
					// if custom buttons are added, do not show the "New" button
					config.setShowNewBtn(false);
					
					// Button list is set if it's not null - even if it's empty.
					// This will allow us to create an empty button panel.
					config.setButtonPanel(buttonPanel);
				}
				
				config.setType(this.recordType);
				config.setItems(collectionRecords);
				config.setColumns(this.columns);
				config.setEnv(parent.getEnv());
				config.setPageContext(this.pageContext);
				config.setServletHost(getHost());
				config.setTitle(actualTitle);
				config.setPageSize(this.pageSize != null ? this.pageSize : 20);
				config.setPageNo(1);
				config.setId(listId);
				config.setI18n(parent.getAuthData().getI18n());
				config.setSortBy(sortExpression);
				config.setItemVar(this.itemVar != null ? this.itemVar : ObjectListConfig.DEFAULT_RECORD_VAR);
				
				code.append(ObjectListConfig.getCode(config, getEnv()));
				code.append("</div>");
				
				// create anchor so that users can be taken directly to the list
				parent.addTab(actualTitle, "<a name=\"anchor-" + listId + "\"></a>" + code.toString(), listId, null);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				parent.addErrorMsgs("Error generating related list code: " + e.getMessage());
				cleanUp();
				return EVAL_PAGE;
			}
		}
		else
		{
			try
			{
				parent.addTab(getRelatedListTab(this.collectionField, listId, this.columns, queriedFields, record, actualTitle, this.pageSize, this.parent, getEnv()));
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				parent.addErrorMsgs("Could not render related list");
				cleanUp();
				return EVAL_PAGE;
			}
		}
		
		cleanUp();
		
		return EVAL_PAGE;
    }
	
	/**
	 * Returns a definitions of a related list tab.
	 * @param tabId
	 * @param queriedFields
	 * @param record
	 * @param title
	 * @return
	 * @throws KommetException
	 */
	public static ObjectDetailsTag.Tab getRelatedListTab(Field field, String tabId, List<ListColumn> columns, Set<String> queriedFields, Record record, String title, Integer pageSize, ObjectDetailsTag parent, EnvData env) throws KommetException
	{
		String targetId = "tab-" + MiscUtils.getHash(20);
		String targetElement = "<div class=\"km-relatedlist-container\"><a name=\"anchor-" + tabId + "\"></a><div id=\"" + targetId + "\"></div></div>";
		String jqueryTarget = "$(\"#" + targetId + "\")";
		
		String tabRenderCallback = getTabJavascriptRenderFunction(field, columns, queriedFields, jqueryTarget, record.getType(), title, record, pageSize, parent.getPageContext(), parent.getAuthData(), parent.getViewWrapper(), env);
		ObjectDetailsTag.Tab tab = parent.new Tab(title, targetElement, jqueryTarget, tabRenderCallback, tabId, null);
		tab.setTabStyle(TabStyle.TOP);
		
		return tab;
	}
	
	private static String getTabJavascriptRenderFunction(Field field, List<ListColumn> columns, Set<String> queriedFields, String jqueryTarget, Type type, String actualTitle, Record record, Integer pageSize, PageContext pageContext, AuthData authData, ViewWrapperTag viewWrapper, EnvData env) throws KommetException
	{
		Type collectionType = null;
		
		DataType fieldDataType = field.getDataType();
		
		if (fieldDataType.getId().equals(DataType.INVERSE_COLLECTION))
		{	
			collectionType = ((InverseCollectionDataType)fieldDataType).getInverseType();
		}
		else if (fieldDataType.getId().equals(DataType.ASSOCIATION))
		{	
			collectionType = ((AssociationDataType)fieldDataType).getAssociatedType();
		}
		else
		{
			throw new KommetException("Invalid collection data type " + fieldDataType.getName());
		}
		
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(MiscUtils.implode(queriedFields, ", ")).append(" FROM ").append(collectionType.getQualifiedName());
		
		// add subquery that queries all items in the related collection
		query.append(" WHERE ").append(Field.ID_FIELD_NAME).append(" IN (SELECT ").append(field.getApiName()).append(".").append(Field.ID_FIELD_NAME).append(" FROM ").append(type.getQualifiedName()).append(" WHERE ").append(Field.ID_FIELD_NAME).append(" = '").append(record.getKID()).append("')");
		
		String varName = "rel_" + MiscUtils.getHash(20);
		
		DataTable dt = new DataTable(query.toString(), DataSourceType.DATABASE, env);
		dt.setPageSize(pageSize);
		dt.setVar(varName);
		//dt.setGenericOptions(this.genericOptions);
		dt.setPaginationActive(true);
		dt.setTitle(actualTitle);
		dt.setJqueryTarget(jqueryTarget);
		
		if (columns != null && !columns.isEmpty())
		{
			List<DataTableColumn> dtColumns = new ArrayList<DataTableColumn>();
			for (ListColumn col : columns)
			{
				DataTableColumn dtColumn = new DataTableColumn();
				dtColumn.setFieldApiName(col.getField());
				dtColumn.setSortable(true);
				dtColumn.setOnClick(col.getOnClick());
				
				if (StringUtils.hasText(col.getLabel()))
				{
					dtColumn.setLabel(col.getLabel());
				}
				
				// set this column as link
				dtColumn.setLinkStyle(true);
				
				if (StringUtils.hasText(col.getUrl()))
				{
					dtColumn.setUrl(col.getUrl());
				}
				else
				{
					dtColumn.setUrl(viewWrapper.getHost() + "/{id}");
				}
				
				dtColumns.add(dtColumn);
			}
				
			dt.setProperties(dtColumns);
		}
		
		StringBuilder btnPanelCode = new StringBuilder();
		
		// add new button
		if (authData.canCreateType(collectionType.getKID(), true, env))
		{
			String btnPanelVar = "btnPanel_" + MiscUtils.getHash(10);
			
			List<String> newRecordParams = new ArrayList<String>();
			Map<String, String> newRecordPassedParams = getNewRecordPassedParams(field, record, authData);
			for (String param : newRecordPassedParams.keySet())
			{
				newRecordParams.add(param + "=" + newRecordPassedParams.get(param));
			}
			
			// create button panel
			btnPanelCode.append("var ").append(btnPanelVar).append(" = km.js.buttonpanel.create({ id: \"btn-panel-").append(MiscUtils.getHash(5)).append("\"");
			btnPanelCode.append(", cssClass: \"km-record-list-button-panel\"").append(" });\n");
			
			btnPanelCode.append(btnPanelVar).append(".addButton({");
			btnPanelCode.append("label: km.js.config.i18n['btn.new']");
			btnPanelCode.append(", url: km.js.config.contextPath + \"/" + collectionType.getKeyPrefix() + "/n?" + MiscUtils.implode(newRecordParams, "&") + "\"");
			
			btnPanelCode.append("});\n");
			
			btnPanelCode.append(varName).append(".setButtonPanel(").append(btnPanelVar).append(");");
			
			dt.setTitleHandledByButtonPanel(true);
		}
		
		String initFunction = "initRelatedList" + MiscUtils.getHash(5);
		StringBuilder initCode = new StringBuilder();
		initCode.append("function ").append(initFunction).append("() {").append(dt.getCode().getInitCode());
		initCode.append("; ").append(btnPanelCode.toString()).append("};");
		initCode.append(initFunction).append("();");
		
		return initCode.toString();
	}

	@Override
	protected void cleanUp()
	{
		// clear values
		this.columns = null;
		this.field = null;
		this.title = null;
		this.itemVar = null;
		this.parent = null;
		this.fieldDataType = null;
		this.collectionField = null;
		this.config = null;
		this.pageSize = null;
		this.newRecordPassedParams = new HashMap<String, String>();
		
		super.cleanUp();
	}

	public void setField(String listField)
	{
		this.field = listField;
	}

	public String getField()
	{
		return field;
	}
	
	public void addColumn(ListColumn col)
	{
		if (this.columns == null)
		{
			this.columns = new ArrayList<ListColumn>();
		}
		this.columns.add(col);
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public void setButtonPanel(ButtonPanel buttonPanel)
	{
		this.buttonPanel = buttonPanel;
	}

	public ButtonPanel getButtonPanel()
	{
		return buttonPanel;
	}

	public void setItemVar(String itemVar)
	{
		this.itemVar = itemVar;
	}

	public String getItemVar()
	{
		return itemVar;
	}

	public void setColumns(List<ListColumn> columns)
	{
		this.columns = columns;
	}

	public List<ListColumn> getColumns()
	{
		return columns;
	}

	public Type getRecordType()
	{
		return recordType;
	}

	public void setPageSize(Integer pageSize)
	{
		this.pageSize = pageSize;
	}

	public Integer getPageSize()
	{
		return pageSize;
	}

	public void setSortProperty(String sortProperty)
	{
		this.sortProperty = sortProperty;
	}

	public String getSortProperty()
	{
		return sortProperty;
	}

	public void setSortOrder(String sortOrder)
	{
		this.sortOrder = sortOrder;
	}

	public String getSortOrder()
	{
		return sortOrder;
	}
}
