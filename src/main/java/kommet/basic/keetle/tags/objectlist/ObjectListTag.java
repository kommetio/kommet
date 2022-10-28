/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.objectlist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.keetle.tags.ListDisplay;
import kommet.basic.keetle.tags.KommetTag;
import kommet.basic.keetle.tags.TagErrorMessageException;
import kommet.basic.keetle.tags.ViewTag;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.basic.keetle.tags.buttons.ButtonPanel;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;

/**
 * Object list tag represents a list of objects.
 * It can be either a list of records of some type, or a list of custom objects.
 * 
 * @author Radek Krawiec
 *
 */
public class ObjectListTag extends KommetTag implements ListDisplay
{
	private static final long serialVersionUID = -7583166756157418418L;

	/**
	 * List of items to display
	 * This can be either a list of records of type {@link kommet.data.Record}, or of any objects.
	 */
	private List<?> items;
	private String type;
	private List<ListColumn> columns;
	
	/**
	 * This set stores indices from property "columns" that refer to columns that should
	 * be rendered as sortable.
	 */
	private Set<Integer> sortableColumnIndices;
	
	private String dalFilter;
	private String sortBy;
	private String id;
	
	// Number of the currently displayed page.
	// The first page has number 1.
	private Integer pageNo;
	
	// Number of records on a single page.
	private Integer pageSize;
	
	private ViewTag parentView;
	private String listFilterCode;
	private String recordListContainerId;
	private ObjectListConfig config;
	private AuthData authData;
	
	// List title. If not specified, defaults to type's plural label.
	private String title;
	
	/**
	 * ID property of the object. This property is used only when list has preset objects.
	 * It determines the ID property of the objects in the list.
	 */
	private String idField;
	private ButtonPanel buttonPanel;
	private boolean isError = false;
	
	/**
	 * Variable under which the current record is available for columns.
	 * Defaults to "record".
	 */
	private String itemVar;
	
	private ObjectListSource recordSource;
	private ObjectListItemType listItemType;
	
	private String itemType;
	
	private Type recordType;

	public ObjectListTag() throws KommetException
	{
		super();
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public int doStartTag() throws JspException
    {
		this.isError = false;
		this.parentView = (ViewTag)checkParentTag(ViewTag.class);
		
		if ("bean".equals(this.itemType))
		{
			this.listItemType = ObjectListItemType.BEAN;
		}
		else
		{
			this.listItemType = ObjectListItemType.RECORD;
		}
		
		// make sure dalFilter attribute and items attribute are not combined
		if (StringUtils.hasText(dalFilter) && items != null)
		{
			this.isError = true;
			return exitWithTagError("Attributes dalFilter and items cannot be combined on tag objectList");
		}
		
		// make sure sortBy attribute and items attribute are not combined
		if (StringUtils.hasText(sortBy) && items != null)
		{
			this.isError = true;
			return exitWithTagError("Attributes sortBy and items cannot be combined on tag objectList");
		}
		
		// need to set ID here because listFilter tag placed within the objectList tag references the ID
		if (!StringUtils.hasText(this.id))
		{
			this.id = this.parentView.nextComponentId();
		}
		
		this.authData = AuthUtil.getAuthData(this.pageContext.getSession());
		
		this.recordSource = this.items != null ? ObjectListSource.PRESET : ObjectListSource.QUERY;
		
		// check type validity only if list is not provided with preset records
		if (ObjectListSource.QUERY.equals(this.recordSource))
		{
			try
			{
				this.recordType = getEnv().getType(this.type);
			}
			catch (KommetException e)
			{
				this.isError = true;
				return exitWithTagError("Error getting object type: " + e.getMessage());
			}
			
			if (type == null)
			{
				addErrorMsgs("Type " + this.type + " does not exist");
				// return EVAL_PAGE, not EVAL_BODY_INCLUDE, because we don't want the tag to be processed further
				return EVAL_PAGE;
			}
		}
		else
		{
			if (StringUtils.hasText(this.type))
			{
				this.isError = true;
				return exitWithTagError("Type attribute should not be used when object list is generated with preset items");
			}
			
			if (!StringUtils.hasText(this.idField))
			{
				this.isError = true;
				return exitWithTagError("Item ID attribute should be specified when list is generated with preset objects.");
			}
		}
		
		try
		{
			// Object list config has to be defined in the doStartTag method because
			// its property recordListId...
			// However, some properties are determined by child elements such as linkFields and they need
			// to be set in the doEndTag method, only after child elements are declared.
			this.config = new ObjectListConfig(this.recordSource, this.listItemType);
			this.config.setType(this.recordType);
			this.config.setItems(this.items);
			this.config.setEnv(getEnv());
			this.config.setDalFilter(this.dalFilter);
			this.config.setPageContext(this.pageContext);
			this.config.setId(this.id);
			this.config.setIdField(this.idField);
			this.config.setPageNo(pageNo != null ? pageNo : 1);
			this.config.setPageSize(pageSize != null ? pageSize : ObjectListConfig.DEFAULT_PAGE_SIZE);
			this.config.setSortBy(this.sortBy != null ? this.sortBy : "");
			this.config.setTitle(this.title);
			this.config.setI18n(this.parentView.getAuthData().getI18n());
			this.config.setItemVar(this.itemVar != null ? this.itemVar : ObjectListConfig.DEFAULT_RECORD_VAR);
		}
		catch (KommetException e)
		{
			this.isError = true;
			return exitWithTagError("Error rendering object list tag: " + e.getMessage());
		}
		
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {	
		// check if there were any errors while processing the doStartTag method
		if (this.isError)
		{
			cleanUp();
			return EVAL_PAGE;
		}
		else if (!getErrorMsgs().isEmpty())
		{
			return exitWithTagError(getErrorMsgs());
		}
		
		// if list is provided with preset records, search, sorting and pagination are not allowed
		if (ObjectListSource.PRESET.equals(this.recordSource))
		{
			if (StringUtils.hasText(this.listFilterCode))
			{
				addErrorMsgs("List filter tag cannot be used in object list with preset items");
			}
			
			if (!StringUtils.hasText(idField))
			{
				addErrorMsgs("ID field has to be specified when object list is rendered with preset items");
			}
			
			// TODO add check that sorting and paging are not defined in list with preset records
		}
		
		this.config.setListFilterCode(this.listFilterCode);
		
		Type type = config.getType();
		try
		{
			// any checks on record type or fields are valid only if no preset records are provided,
			// because if they are, type is not used.
			if (ObjectListSource.QUERY.equals(this.recordSource))
			{
				if (!authData.canReadType(type.getKID(), true, getEnv()))
				{
					return exitWithTagError("Insufficient privileges to view records of type " + type.getQualifiedName());
				}
				
				if (this.columns == null || this.columns.isEmpty())
				{
					this.columns = new ArrayList<ListColumn>();
					
					ListColumn defaultColumn = new ListColumn();
					defaultColumn.setField(type.getDefaultFieldApiName());
					// make the default field column sortable
					defaultColumn.setSortable(true);
					defaultColumn.setLink(true);
					defaultColumn.setLabel(type.getDefaultField().getInterpretedLabel(authData));
					this.columns = new ArrayList<ListColumn>();
					this.columns.add(defaultColumn);
				}
			}
			else
			{
				if (this.columns == null || this.columns.isEmpty())
				{
					// if list if rendered with preset items, we need to have at least one field specified to display
					// otherwise we wouldn't know what field to display - we cannot deduce a default field because
					// with preset records, no record type is known
					return exitWithTagError("No list columns specified. At least one columns needs to be specified if object list is rendered with preset items.");
				}
			}
			
			this.config.setColumns(this.columns);
			this.config.addRmParams(getPageData());
			
			if (this.buttonPanel != null)
			{
				// if custom buttons are added, do not show the "New" button
				this.config.setShowNewBtn(false);
				
				// Button list is set if it's not null - even if it's empty.
				// This will allow us to create an empty button panel.
				this.config.setButtonPanel(buttonPanel);
			}
			
			// TODO enable paging for preset records. Right now it is disabled.
			if (ObjectListSource.QUERY.equals(this.recordSource))
			{
				if (this.config.isPagingOn() && !StringUtils.hasText(this.config.getSortBy()) && this.config.getItems() != null)
				{
					addErrorMsgs("When paging is on and records are passed to the object list, sorting also needs to be specified");
				}
			}
			
			this.pageContext.getOut().write(ObjectListConfig.getCode(config, getEnv()));
		}
		catch (IOException e)
		{
			throw new JspException("Error writing to JSP page: " + e.getMessage());
		}
		catch (TagErrorMessageException e)
		{
			return exitWithTagError(e.getMessage());
		}
		catch (KommetException e)
		{
			return exitWithTagError(e.getMessage());
		}
		
		// add breadcrumb to session
		try
		{
			Breadcrumbs.add(this.getPageData().getRequestURL(), config.getTitle(), getViewWrapper().getAppConfig().getBreadcrumbMax(), this.pageContext.getSession());
		}
		catch (KommetException e1)
		{
			return exitWithTagError("Could not render breadcrumbs due to an error in configuration");
		}
		
		// the next call of the tag is to the same instance, so we clear the properties
		// so that they are not set in the next call to this tag
		cleanUp();
		clearErrorMessages(); 
		
		return EVAL_PAGE;
    }

	@Override
	public void cleanUp()
	{
		this.items = null;
		this.columns = null;
		this.sortableColumnIndices = null;
		this.dalFilter = null;
		this.listFilterCode = null;
		this.buttonPanel = null;
		this.itemVar = null;
		this.recordSource = null;
		this.itemType = null;
		this.listItemType = null;
		this.recordType = null;
		super.cleanUp();
	}
	
	/**
	 * Renders title bar.
	 * @param colCount
	 * @param title
	 * @return
	 */
	public static String titleBar(int colCount, String title)
	{
		StringBuilder code = new StringBuilder();
		code.append("<tr class=\"title\"><th colspan=\"").append(colCount).append("\">");
		code.append(title).append("</th></tr>");
		return code.toString();
	}
	
	public static String btnPanel (String title, int colCount, String ... btns)
	{		
		StringBuilder code = new StringBuilder();
		if (StringUtils.hasText(title) || (btns != null && btns.length > 0))
		{
			code.append("<tr class=\"btns\"><th colspan=\"").append(colCount).append("\">");
			
			if (StringUtils.hasText(title))
			{
				code.append("<span class=\"title\">").append(title).append("</span>");
			}
			
			for (String btn : btns)
			{
				if (btn != null)
				{
					code.append(btn);
				}
			}
			
			code.append("</th></tr>");
		}
		return code.toString();
	}

	public void setItems(List<?> objects)
	{
		this.items = objects;
	}

	public List<?> getObjects()
	{
		return items;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getType()
	{
		return type;
	}

	public void addColumn(ListColumn col)
	{
		if (this.columns == null)
		{
			this.columns = new ArrayList<ListColumn>();
		}
		
		this.columns.add(col);
		
		if (col.isSortable())
		{
			if (this.sortableColumnIndices == null)
			{
				this.sortableColumnIndices = new HashSet<Integer>();
			}
			this.sortableColumnIndices.add(this.columns.size() - 1);
		}
	}

	public void setDalFilter(String dalFilter)
	{
		this.dalFilter = dalFilter;
	}

	public String getDalFilter()
	{
		return dalFilter;
	}

	public void setSortBy(String sortBy)
	{
		this.sortBy = sortBy;
	}

	public String getSortBy()
	{
		return sortBy;
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setPageNo(Integer pageNo)
	{
		this.pageNo = pageNo;
	}

	public Integer getPageNo()
	{
		return pageNo;
	}

	public void setPageSize(Integer pageSize)
	{
		this.pageSize = pageSize;
	}

	public Integer getPageSize()
	{
		return pageSize;
	}

	public void setListFilterCode(String listFilterCode)
	{
		this.listFilterCode = listFilterCode;
	}

	public String getListFilterCode()
	{
		return listFilterCode;
	}

	public String getRecordListContainerId()
	{
		return recordListContainerId;
	}

	public ObjectListConfig getConfig()
	{
		return config;
	}

	public static String getTableHeader(List<String> columnNames, String title, String newBtnCode)
	{
		StringBuilder code = new StringBuilder();
		code.append("<thead>");
		
		if (StringUtils.hasText(newBtnCode))
		{
			code.append(ObjectListTag.btnPanel(title, columnNames.size(), newBtnCode));
		}
		else
		{
			code.append(ObjectListTag.btnPanel(title, columnNames.size()));
		}
		code.append("<tr class=\"cols\">");
		for (String col : columnNames)
		{
			code.append("<th>").append(col).append("</th>");
		}
		code.append("</tr></thead>");
		return code.toString();
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public ButtonPanel getButtonPanel()
	{
		return buttonPanel;
	}
	
	public String getIdField()
	{
		return idField;
	}

	public void setIdField(String idField)
	{
		this.idField = idField;
	}

	public void setButtonPanel(ButtonPanel panel)
	{
		this.buttonPanel = panel;
	}
	
	public String getItemType()
	{
		return itemType;
	}

	public void setItemType(String itemType)
	{
		this.itemType = itemType;
	}
	
	public ObjectListItemType getListItemType()
	{
		return this.listItemType;
	}
	
	@Override
	public EnvData getEnv() throws KommetException
	{
		return super.getEnv();
	}

	public Type getRecordType()
	{
		return recordType;
	}
	
	public void setItemVar(String itemVar)
	{
		this.itemVar = itemVar;
	}

	public String getItemVar()
	{
		return itemVar;
	}
}
