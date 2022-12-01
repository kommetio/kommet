/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.objectlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.tags.TagErrorMessageException;
import kommet.basic.keetle.tags.buttons.Button;
import kommet.basic.keetle.tags.buttons.ButtonFactory;
import kommet.basic.keetle.tags.buttons.ButtonPanel;
import kommet.basic.keetle.tags.buttons.ButtonPrototype;
import kommet.basic.keetle.tags.buttons.ButtonType;
import kommet.dao.queries.QueryResult;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KommetException;
import kommet.data.NoSuchFieldException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.i18n.I18nDictionary;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.utils.VarInterpreter;
import kommet.utils.XMLUtil;
import kommet.web.kmparams.KmParamException;
import kommet.web.kmparams.KmParamNode;
import kommet.web.kmparams.actions.Action;
import kommet.web.kmparams.actions.ExecuteCode;
import kommet.web.kmparams.actions.SetField;
import kommet.web.kmparams.actions.SetParentField;
import kommet.web.kmparams.actions.ShowLookup;

/**
 * Configuration of an object list. Stores all information necessary to render an object list.
 * @author Radek Krawiec
 */
public class ObjectListConfig
{
	public static final Integer DEFAULT_PAGE_SIZE = 15;
	public static final String DEFAULT_RECORD_VAR = "record";
	
	private Type type;
	private List<?> items;
	private List<ListColumn> columns;
	private String id;
	private String cssClass;
	private String cssStyle;
	private EnvData env;
	private String dalFilter;
	private PageContext pageContext;
	private String sortBy;
	private Integer pageNo;
	private Integer pageSize;
	private String listFilterCode;
	private I18nDictionary i18n;
	private boolean showNewBtn = true;
	private String idField;
	private ObjectListSource recordSource;
	private ObjectListItemType itemType;
	private String servletHost;

	/**
	 * Variable name under which the current record will be available to
	 * code displaying columns.
	 */
	private String itemVar = DEFAULT_RECORD_VAR;
	
	// Javascript to be executed when list item is clicked.
	// This is interpreted code, i.e. $parameters are substituted.
	private List<String> onListItemSelect;
	
	private KmParamNode rmParams;
	
	// title displayed in the header of the list
	private String title;
	
	private String lookupId;
	
	/**
	 * This set stores indices from property "columns" that refer to columns that should
	 * be rendered as sortable.
	 */
	private Set<Integer> sortableColumnIndices;
	
	private Map<String, String> newObjectPassedParams;
	
	public ObjectListConfig (ObjectListSource recordSource, ObjectListItemType itemType)
	{
		this.recordSource = recordSource;
		this.itemType = itemType;
	}
	
	public static String getCode (ObjectListConfig config, EnvData env) throws NoSuchFieldException, KommetException
	{	
		config.prepare();
		
		StringBuilder code = new StringBuilder("<div");
		XMLUtil.addStandardTagAttributes(code, config.getId(), null, "object-list-container" + (config.getCssClass() != null ? " " + config.getCssClass() : ""), config.getCssStyle());
		code.append(">");
		
		// render search panel if set
		if (StringUtils.hasText(config.getListFilterCode()))
		{
			code.append("<form id=\"searchform").append(config.getId()).append("\">");
			code.append(config.getListFilterCode());
			code.append("<input type=\"hidden\" name=\"mode\" value=\"search\">");
			code.append("</form>");
			
			code.append(addConfigJSON(config));
		}
		
		AuthData authData = AuthUtil.getAuthData(config.getPageContext().getSession());
		
		// total number of items to be displayed
		Long totalCount = null;
		
		// Tells whether the type's default field is displayed on the list.
		// We need to know it so that we can manually add it to the queried fields. 
		boolean defaultFieldDisplayed = false;
		
		// TODO enable paging for list with preset records
		if (ObjectListSource.QUERY.equals(config.getRecordSource()))
		{
			if (config.isPagingOn() && !StringUtils.hasText(config.getSortBy()))
			{
				if (config.getItems() == null)
				{
					// if paging is on, we need to have some sorting of records to be able
					// to correctly display next page
					config.setSortBy(Field.CREATEDDATE_FIELD_NAME + " desc");
				}
				else
				{
					throw new KommetException("When records are passed to object list and paging is on, sort order and sort field have to be specified");
				}
			}
		}
		
		if (config.getItems() == null)
		{	
			try
			{
				String queriedFields = MiscUtils.implode(ObjectListConfig.extractFields(config.getColumns(), config.getItemVar()), ", ");
				if (!defaultFieldDisplayed)
				{
					// always query the default field
					queriedFields += ", " + config.getType().getDefaultFieldApiName();
				}
				
				String dalQuery = "SELECT " + queriedFields + " from " + config.getType().getQualifiedName();
				
				// if additional DAL conditions were specified for the tag, apply them to the search
				if (StringUtils.hasText(config.getDalFilter()))
				{
					dalQuery += " WHERE " + config.getDalFilter();
				}
				
				// add order clause if sort conditions were specified
				if (StringUtils.hasText(config.getSortBy()))
				{
					dalQuery += " ORDER BY " + config.getSortBy();
				}
				
				if (config.isPagingOn())
				{
					String countQuery = "SELECT COUNT(id) FROM " + config.getType().getQualifiedName();
					// if additional DAL conditions were specified for the tag, apply them to the search
					if (StringUtils.hasText(config.getDalFilter()))
					{
						countQuery += " WHERE " + config.getDalFilter();
					}
					
					// count entities
					List<Record> countResult = config.getEnv().getSelectCriteriaFromDAL(countQuery, authData).list();
					totalCount = (Long)((QueryResult)countResult.get(0)).getAggregateValue("count(id)");
					dalQuery += " LIMIT " + config.getPageSize() + " OFFSET " + (config.getPageNo() - 1) * config.getPageSize();
				}
				
				// search records according to DAL criteria
				config.setItems(config.getEnv().getSelectCriteriaFromDAL(dalQuery, authData).list());
			}
			catch (KommetException e)
			{
				throw new TagErrorMessageException("Error retrieving objects for list: " + e.getMessage());
			}
		}
		else
		{
			totalCount = Long.valueOf(config.getItems().size());
		}
		
		code.append("<div");
		XMLUtil.addStandardTagAttributes(code, config.getRecordListId(), null, null, null);
		code.append(">");
		
		String sortField = "";
		String sortOrder = "";
		
		if (config.isSingleSort())
		{
			sortField = config.getSingleSortField();
			sortOrder = config.getSingleSortOrder();
		}
		
		// add fields for storing current search, sort and paging settings
		code.append("<input type=\"hidden\" id=\"sortfield-").append(config.getId()).append("\" value=\"").append(sortField).append("\">");
		code.append("<input type=\"hidden\" id=\"sortorder-").append(config.getId()).append("\" value=\"").append(sortOrder).append("\">");
		
		// if button panel not defined, create it
		if (config.getButtonPanel() == null)
		{
			// if no button panel has been defined by user, create standard button panel
			config.setButtonPanel(new ButtonPanel(false));
		}
		
		// if list is not displayed with preset records and new button should be displayed, display it
		if (!config.getButtonPanel().isCustomButtons() && ObjectListSource.QUERY.equals(config.getRecordSource()) && config.isShowNewBtn() && authData.canCreateType(config.getType().getKID(), false, config.getEnv()))
		{
			config.getButtonPanel().addButton(newObjectBtn(config.getType().getKeyPrefix(), config.getI18n().get("btn.new"), config.getNewObjectPassedParams(), config.getServletHost(), config.getRmParams(), config.getLookupId(), env));
		}
		
		code.append(detachedBtnPanel(config, config.getTitle(), config.getType() != null ? config.getType().getKeyPrefix() : null, config.getServletHost(), config.getI18n(), env, config.getButtonPanel() != null ? config.getButtonPanel().getButtons().toArray(new ButtonPrototype[0]) : new ButtonPrototype[0]));
		
		code.append("<table class=\"std-table\">");
		code.append("<thead>");
		
		// start rendering table header
		code.append("<tr class=\"cols\">");
		try
		{
			for (ListColumn col : config.getColumns())
			{
				code.append("<th>");
				
				// if field is sortable
				if (col.isSortable())
				{
					if (col.getField() == null)
					{
						throw new KommetException("Only field columns can be sortable, formula columns cannot.");
					}
					
					code.append("<a class=\"sortable-header\" href=\"javascript:;\"");
					code.append(" onclick=\"").append(getObjectListFunctionCall(config, sortOrder, col.getField())).append("\">");
					code.append(col.getLabel());
					code.append("</a>");
				}
				else
				{
					code.append(col.getLabel());
				}
				
				code.append("</th>");
			}
		}
		catch (KommetException e)
		{
			throw new TagErrorMessageException("Error reading field definition: " + e.getMessage());
		}
		
		code.append("</tr></thead>");
		code.append("<tbody>");
		
		try
		{
			if (!config.getItems().isEmpty())
			{
				String listSelectCustomHandler = getListSelectHandler(config);
				
				// render a row for each item on the list
				for (Object object : config.getItems())
				{
					code.append("<tr>");
					for (ListColumn col : config.getColumns())
					{	
						code.append("<td>").append(col.getCode(object, config.getItemVar(), listSelectCustomHandler, authData, config.getServletHost()));
						code.append("</td>");
					}
					code.append("</tr>");
				}
			}
			else
			{
				code.append(getNoResultsMsg(config.getColumns().size(), config.getI18n().get("msg.noresults")));
			}
		}
		catch (KommetException e)
		{
			throw new TagErrorMessageException("Error displaying records: " + e.getMessage());
		}
		
		code.append("</tbody>");
		code.append("</table>");
		
		if (config.isPagingOn())
		{
			code.append(getPagingPanel(config, totalCount.intValue(), sortField, sortOrder.toLowerCase().equals("asc") ? "desc" : "asc", config.getI18n(), authData));
		}
		
		// end DIV record-list-container
		code.append("</div>");
		// end DIV object-list-container
		code.append("</div>");
		
		return code.toString();
	}
	
	private void prepare()
	{
		if (ObjectListSource.QUERY.equals(this.recordSource))
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
	}

	private static String addConfigJSON(ObjectListConfig config) throws KommetException
	{
		StringBuilder code = new StringBuilder("<script language=\"Javascript\">");
		
		String serializedConfig = config.serializeJSON();
		
		// store list config as JSON
		code.append("objectListConfigs['").append(config.getId()).append("'] = ").append(serializedConfig).append(";");
		code.append("</script>");
		
		return code.toString();
	}

	public static Set<String> extractFields (Collection<ListColumn> columns, String itemVar)
	{
		Set<String> queriedFields = new HashSet<String>();
		for (ListColumn col : columns)
		{
			if (col.getField() != null)
			{
				queriedFields.add(col.getField());
			}
			else if (col.getJavaCallback() == null)
			{
				// extract properties from the formula
				queriedFields.addAll(VarInterpreter.extractProperties(col.getFormula(), itemVar));
			}
		}
		
		return queriedFields;
	}
	
	public static String detachedBtnPanel (ObjectListConfig config, String title, KeyPrefix prefix, String contextPath, I18nDictionary i18n, EnvData env, ButtonPrototype... btns) throws KommetException
	{
		if (config == null)
		{
			config = new ObjectListConfig(ObjectListSource.QUERY, ObjectListItemType.RECORD);
		}
		
		StringBuilder code = new StringBuilder("<div class=\"dbtns\">");
		
		// Render title.
		// Since we always want buttons to float to the right, we need to set the title to &nbsp; if it's
		// empty so that CSS property aligns the buttons.
		code.append("<span class=\"title\">").append(StringUtils.hasText(title) ? title : "&nbsp;").append("</span>");
		
		if (btns != null && btns.length > 0)
		{
			code.append("<div class=\"btn-list\">");
			for (ButtonPrototype btn : btns)
			{
				if (btn == null)
				{
					continue;
				}
				
				if (btn instanceof Button)
				{
					code.append(((Button)btn).getCode());
				}
				else if (btn.getType().equals(ButtonType.NEW))
				{	
					// add special handling of new button for list
					code.append(newObjectBtn(prefix, i18n.get("btn.new"), config.getNewObjectPassedParams(), contextPath, config.getRmParams(), config.getLookupId(), env).getCode());
				}
				else
				{
					code.append(ButtonFactory.getButton(btn.getType(), null, prefix, null, i18n, contextPath).getCode());
				}
			}
			code.append("</div>");
		}
		
		code.append("</div>");
		return code.toString();
	}

	private static Button newObjectBtn(KeyPrefix prefix, String label, Map<String, String> passedParams, String contextPath, KmParamNode rmParams, String lookupId, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		code.append("<a href=\"").append(contextPath).append("/").append(prefix).append("/n");
		
		List<String> paramSettings = new ArrayList<String>();
		
		// append passed parameters
		if (passedParams != null && !passedParams.isEmpty())
		{	
			for (String param : passedParams.keySet())
			{
				// TODO escape param values
				paramSettings.add(param + "=" + passedParams.get(param));
			}
		}
		
		// add lookup reference
		if (rmParams != null && rmParams.getSingleActionNode("lookup") != null)
		{
			paramSettings.add("km.lookup=" + ((ShowLookup)rmParams.getSingleActionNode("lookup")).getId());
			paramSettings.add("km.layout=" + env.getBlankLayoutId());
		}
		else if (lookupId != null)
		{
			paramSettings.add("km.lookup=" + lookupId);
			paramSettings.add("km.layout=" + env.getBlankLayoutId());
		}
		
		if (!paramSettings.isEmpty())
		{
			code.append("?");
			code.append(MiscUtils.implode(paramSettings, "&"));
		}
		
		code.append("\"");
		code.append(" class=\"sbtn\"");
		code.append(">").append(label).append("</a>");
		return new Button(code.toString(), ButtonType.NEW, label);
	}
	
	private static String getListSelectHandler(ObjectListConfig config) throws KmParamException
	{
		StringBuilder selectCode = new StringBuilder();
		
		// check if custom events are defined for the onclick action on the list item
		if (config.getOnListItemSelect() != null && !config.getOnListItemSelect().isEmpty())
		{
			// TODO change js function to accept params
			selectCode.append(MiscUtils.implode(config.getOnListItemSelect(), ";"));
		}
		
		String lookupId = null;
		if (config.getRmParams() != null)
		{
			KmParamNode rmParams = config.getRmParams();
			if (rmParams.getSingleActionNode("lookup") != null)
			{
				lookupId = ((ShowLookup)rmParams.getSingleActionNode("lookup")).getId();
			}
		}
		
		if (lookupId == null)
		{
			lookupId = config.getLookupId();
		}
		
		
		// check if the list is displayed in lookup mode
		if (lookupId != null)
		{
			selectCode.append("selectLookupItem('").append(lookupId).append("','$id','$displayField', event)");
		}
		
		return selectCode.length() > 0 ? selectCode.toString() : null;
	}
	
	public static String getNoResultsMsg(int colCount, String msg)
	{
		StringBuilder code = new StringBuilder();
		code.append("<tr><td colspan=\"").append(colCount).append("\" style=\"font-style:italic\">");
		code.append(msg).append("</td></tr>");
		return code.toString();
	}
	
	private static String getPagingPanel(ObjectListConfig config, Integer totalCount, String sortField, String sortOrder, I18nDictionary i18n, AuthData authData) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		
		code.append("<div class=\"paging-panel\">");
		Integer initialPageNo = config.getPageNo();
		
		if (initialPageNo > 1)
		{
			config.setPageNo(1);
			code.append("<a class=\"page-no\" href=\"javascript:;\" onclick=\"").append(getObjectListFunctionCall(config, sortOrder, sortField)).append("\"><< ").append(i18n.get("list.first")).append("</a>");
			config.setPageNo(initialPageNo > 1 ? initialPageNo - 1 : 1);
			code.append("<a class=\"page-no\" href=\"javascript:;\" onclick=\"").append(getObjectListFunctionCall(config, sortOrder, sortField)).append("\">< ").append(i18n.get("list.prev")).append("</a>");
		}
		else
		{
			code.append("<a class=\"page-no page-no-inactive\"><< ").append(i18n.get("list.first")).append("</a>");
			code.append("<a class=\"page-no page-no-inactive\">< ").append(i18n.get("list.prev")).append("</a>");
		}
		
		Integer pageCount = (totalCount / config.getPageSize()) + (totalCount % config.getPageSize() == 0 ? 0 : 1);
		
		code.append("<span class=\"pages\">");
		
		for (int i = 0; i < pageCount; i++)
		{
			if ((i + 1) != initialPageNo)
			{
				config.setPageNo(i+1);
				code.append("<a class=\"page-no\" href=\"javascript:;\" onclick=\"").append(getObjectListFunctionCall(config, sortOrder, sortField)).append("\">").append(i + 1).append("</a>");
			}
			else
			{
				code.append(i + 1);
			}
		}
		
		// end "pages" span
		code.append("</span>");
		
		if (initialPageNo < pageCount)
		{
			config.setPageNo(initialPageNo < pageCount ? initialPageNo + 1 : pageCount);
			code.append("<a class=\"page-no\" href=\"javascript:;\" onclick=\"").append(getObjectListFunctionCall(config, sortOrder, sortField)).append("\">").append(i18n.get("list.next")).append(" ></a>");
			config.setPageNo(pageCount);
			code.append("<a class=\"page-no\" href=\"javascript:;\" onclick=\"").append(getObjectListFunctionCall(config, sortOrder, sortField)).append("\">").append(i18n.get("list.last")).append(" >></a>");
		}
		else
		{
			code.append("<a class=\"page-no page-no-inactive\">").append(i18n.get("list.next")).append(" ></a>");
			code.append("<a class=\"page-no page-no-inactive\">").append(i18n.get("list.last")).append(" >></a>");
		}
		
		code.append("</div>");
		config.setPageNo(initialPageNo);
		
		return code.toString();
	}
	
	private static String getObjectListFunctionCall (ObjectListConfig config, String sortOrder, String sortField) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		code.append("sortObjectList(").append(config.serializeJSON()).append(", \"sort\", \"").append(sortOrder).append("\", \"").append(sortField).append("\", \"searchform").append(config.getId()).append("\", \"").append(config.getRecordListId()).append("\")");
		return code.toString().replaceAll("\"", "&quot;");
	}
	
	/**
	 * Serializes the object list config to a string of parameters to used used in a HTTP request.
	 * @return
	 */
	/*public String serializeToParams()
	{
		List<String> params = new ArrayList<String>();
		
		params.add("title=\"" + this.title + "\"");
		params.add("pageNo=\"" + this.pageNo + "\"");
		params.add("pageSize=\"" + this.pageSize + "\"");
		params.add("dalFilter=\"" + this.dalFilter + "\"");
		params.add("recordListId=\"" + this.getRecordListId() + "\"");
		params.add("sortBy=\"" + this.sortBy + "\"");
		
		for (ListColumn col : this.columns)
		{
			String serializedCol = "column=" + col.get;
			
			params.add(serializedCol);
		}
		
		return MiscUtils.implode(params, "&");
	}*/
	
	/**
	 * Serializes the config object into JSON.
	 * @throws KommetException 
	 */
	public String serializeJSON() throws KommetException
	{
		StringBuilder json = new StringBuilder("{");
		
		// serialize object list configuration properties
		json.append("\"title\": \"").append(MiscUtils.nullAsBlank(this.title + "")).append("\", ");
		json.append("\"type\": \"").append(MiscUtils.nullAsBlank(this.getType().getQualifiedName())).append("\", ");
		json.append("\"pageNo\": \"").append(MiscUtils.nullAsBlank(this.pageNo)).append("\", ");
		json.append("\"pageSize\": \"").append(MiscUtils.nullAsBlank(this.pageSize)).append("\", ");
		json.append("\"dalFilter\": \"").append(MiscUtils.nullAsBlank(this.dalFilter)).append("\", ");
		json.append("\"id\": \"").append(this.getId()).append("\", ");
		json.append("\"lookupId\": \"").append(MiscUtils.nullAsBlank(this.lookupId)).append("\", ");
		json.append("\"sortBy\": \"").append(MiscUtils.nullAsBlank(this.getSortBy())).append("\", ");
		json.append("\"contextPath\": \"").append(getServletHost()).append("\", ");
		json.append("\"sysContextPath\": \"").append(getServletHost()).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("\", ");
		
		// each column will be serialized as a separate property, not as an array,
		// because array does not guarantee the order of items to be preserved
		
		int i = 0;
		
		for (ListColumn col : this.columns)
		{
			json.append("\"column").append(i).append("\": ");
			serializeColumn(col, json);
			
			if (i < this.columns.size() - 1)
			{
				json.append(", ");
			}
			
			i++;
		}
		
		// each button will be serialized as a separate property, not as an array,
		// because array does not guarantee the order of items to be preserved
		
		if (this.buttonPanel != null)
		{
			if (this.buttonPanel.getButtons() != null && !this.buttonPanel.getButtons().isEmpty())
			{
				json.append(", ");
				i = 0;
				
				for (ButtonPrototype btn : this.buttonPanel.getButtons())
				{
					json.append("\"button").append(i).append("\": ");
					serializeButton(btn, json);
					
					if (i < this.buttonPanel.getButtons().size() - 1)
					{
						json.append(", ");
					}
					
					i++;
				}
			}
			
			// even if there are not buttons in the panel, we need to leave some kind of mark
			// that the button panel was defined as empty, so that during deserialization
			// it is converted into an empty button panel, not a null.
			// TODO serialize buttons and columns as lists, then the problem will disappear
			json.append(", \"buttonPanel\": { \"custom\": \"").append(this.buttonPanel.isCustomButtons()).append("\" }");
		}
		
		// serialize RM params
		/*if (this.rmParams != null)
		{
			json.append("\"rmParams\": [");
			
			List<String> params = new ArrayList<String>();
			
			for (Action actionNode : this.rmParams.getActionNodes().values())
			{
				params.add("\"" + actionNode.getName() + "\": \"" + actionNode.getValue() + "\"");
			}
			
			json.append(MiscUtils.implode(params, ", "));
			
			json.append("]");
		}*/
		
		json.append("}");
		return json.toString();
	}
	
	private void serializeColumn(ListColumn col, StringBuilder json)
	{
		json.append("{");
		json.append("\"field\": \"").append(MiscUtils.nullAsBlank(col.getField())).append("\", ");
		json.append("\"formula\": \"").append(MiscUtils.nullAsBlank(col.getFormula())).append("\", ");
		json.append("\"idField\": \"").append(MiscUtils.nullAsBlank(col.getIdField())).append("\", ");
		json.append("\"nameField\": \"").append(MiscUtils.nullAsBlank(col.getNameField())).append("\", ");
		json.append("\"label\": \"").append(MiscUtils.nullAsBlank(col.getLabel())).append("\", ");
		json.append("\"isLink\": \"").append(col.isLink()).append("\", ");
		json.append("\"javaCallback\": \"").append(col.getJavaCallback() != null ? col.getJavaCallback() : "").append("\", ");
		json.append("\"isSortable\": \"").append(col.isSortable()).append("\"");
		
		json.append("}");
	}
	
	private void serializeButton(ButtonPrototype btn, StringBuilder json)
	{
		json.append("{");
		json.append("\"type\": \"").append(MiscUtils.nullAsBlank(btn.getType().name())).append("\", ");
		
		if (btn instanceof Button)
		{
			Button button = (Button)btn;
			json.append("\"url\": \"").append(MiscUtils.nullAsBlank(button.getUrl())).append("\", ");
			json.append("\"label\": \"").append(MiscUtils.nullAsBlank(button.getLabel())).append("\", ");
			json.append("\"onClick\": \"").append(MiscUtils.nullAsBlank(button.getOnClick())).append("\"");
		}
		
		json.append("}");
	}

	/**
	 * Custom buttons for this list. If defined, i.e. when list is not null, they will be rendered instead of the
	 * standard buttons.
	 */
	private ButtonPanel buttonPanel;

	public void setType(Type type)
	{
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}

	public void setItems(List<?> items)
	{
		this.items = items;
	}

	public List<?> getItems()
	{
		return items;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getId()
	{
		return id;
	}

	public void setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
	}

	public String getCssClass()
	{
		return cssClass;
	}

	public void setCssStyle(String cssStyle)
	{
		this.cssStyle = cssStyle;
	}

	public String getCssStyle()
	{
		return cssStyle;
	}

	public void setEnv(EnvData env)
	{
		this.env = env;
	}

	public EnvData getEnv()
	{
		return env;
	}

	public void setDalFilter(String dalFilter)
	{
		this.dalFilter = dalFilter;
	}

	public String getDalFilter()
	{
		return dalFilter;
	}

	public void setPageContext(PageContext pageContext)
	{
		this.pageContext = pageContext;
	}

	public PageContext getPageContext()
	{
		return pageContext;
	}
	
	public void setSortBy(String sortBy)
	{
		this.sortBy = sortBy;
	}

	public String getSortBy()
	{
		return sortBy;
	}

	public boolean isSingleSort()
	{
		return StringUtils.hasText(sortBy) && !sortBy.contains(",");
	}

	public String getSingleSortField() throws KommetException
	{
		if (isSingleSort())
		{
			return this.sortBy.split("\\s")[0];
		}
		else
		{
			throw new KommetException("Sort clause " + this.sortBy + " does not represent a sort by one property");
		}
	}
	
	public String getSingleSortOrder() throws KommetException
	{
		if (isSingleSort())
		{
			return this.sortBy.split("\\s")[1];
		}
		else
		{
			throw new KommetException("Sort clause " + this.sortBy + " does not represent a sort by one property");
		}
	}

	public void setPageNo(int pageNo) throws KommetException
	{
		if (pageNo < 1)
		{
			throw new KommetException("Invalid page number: the first page has number 1, so " + pageNo + " is not a valid number");
		}
		
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

	public String getRecordListId()
	{
		return "record-list-container-" + this.id;
	}
	
	public boolean isPagingOn()
	{
		return this.pageNo != null && this.pageSize != null;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public void setI18n(I18nDictionary i18n)
	{
		this.i18n = i18n;
	}

	public I18nDictionary getI18n()
	{
		return i18n;
	}

	public void addNewObjectPassedParam (String name, String value)
	{
		if (this.newObjectPassedParams == null)
		{
			this.newObjectPassedParams = new HashMap<String, String>();
		}
		this.newObjectPassedParams.put(name, value);
	}

	public Map<String, String> getNewObjectPassedParams()
	{
		return newObjectPassedParams;
	}
	
	public void setNewObjectPassedParams (String paramList)
	{
		String[] params = paramList.split("&");
		
		for (String param : params)
		{
			String[] paramParts = param.split("=");
			this.addNewObjectPassedParam(paramParts[0], paramParts[1]);
		}
	}

	public void setShowNewBtn(boolean showNewBtn)
	{
		this.showNewBtn = showNewBtn;
	}

	public boolean isShowNewBtn()
	{
		return showNewBtn;
	}

	public void setButtonPanel(ButtonPanel panel)
	{
		this.buttonPanel = panel;
	}

	public ButtonPanel getButtonPanel()
	{
		return buttonPanel;
	}

	public void setIdField(String idField)
	{
		this.idField = idField;
	}

	public String getIdField()
	{
		return idField;
	}

	public void setOnListItemSelect(List<String> onListItemSelect)
	{
		this.onListItemSelect = onListItemSelect;
	}
	
	public void addOnListItemSelect (String script)
	{
		if (this.onListItemSelect == null)
		{
			this.onListItemSelect = new ArrayList<String>();
		}
		this.onListItemSelect.add(script);
	}

	public List<String> getOnListItemSelect()
	{
		return onListItemSelect;
	}

	public void setRmParams(KmParamNode rmParams)
	{
		this.rmParams = rmParams;
	}

	public KmParamNode getRmParams()
	{
		return rmParams;
	}

	public void setItemVar(String itemVar)
	{
		this.itemVar = itemVar;
	}

	public String getItemVar()
	{
		return itemVar;
	}

	public ObjectListSource getRecordSource()
	{
		return recordSource;
	}

	public void setSortableColumnIndices(Set<Integer> sortableColumnIndices)
	{
		this.sortableColumnIndices = sortableColumnIndices;
	}

	public Set<Integer> getSortableColumnIndices()
	{
		return sortableColumnIndices;
	}
	
	public void setColumns (List<ListColumn> cols)
	{
		this.columns = cols;
	}
	
	public List<ListColumn> getColumns()
	{
		return this.columns;
	}

	public void setItemType(ObjectListItemType itemType)
	{
		this.itemType = itemType;
	}

	public void applyDefaults()
	{
		if (this.pageNo == null)
		{
			this.pageNo = 1;
		}
		if (this.pageSize == null)
		{
			this.pageSize = 15;
		}
		if (!StringUtils.hasText(this.sortBy))
		{
			this.sortBy = Field.CREATEDDATE_FIELD_NAME + " DESC";
		}
	}

	public void addColumn(ListColumn col)
	{
		if (this.columns == null)
		{
			this.columns = new ArrayList<ListColumn>();
		}
		this.columns.add(col);
	}

	/**
	 * Deserializes information about a config object stored as a LinkedHashMap.
	 * @param params
	 * @return
	 * @throws NumberFormatException
	 * @throws KommetException
	 */
	public static ObjectListConfig deserializeMap(LinkedHashMap<?,?> params, KommetCompiler compiler, EnvData env) throws NumberFormatException, KommetException
	{
		ObjectListConfig config = new ObjectListConfig(ObjectListSource.QUERY, ObjectListItemType.RECORD);
		
		String userDefinedType = (String)params.get("type");
		Type type = env.getType(userDefinedType);
		if (type == null)
		{
			throw new KommetException("Type with name " + userDefinedType + " not found on env " + env.getId());
		}
		
		config.setType(type);
		config.setEnv(env);
		config.setTitle((String)params.get("title"));
		config.setDalFilter((String)params.get("dalFilter"));
		config.setId((String)params.get("id"));
		config.setLookupId(MiscUtils.blankAsNull((String)params.get("lookupId")));
		config.setSortBy((String)params.get("sortBy"));
		
		String sPageNo = (String)params.get("pageNo");
		config.setPageNo(sPageNo != null ? Integer.valueOf(sPageNo) : null);
		
		String sPageSize = (String)params.get("pageSize");
		config.setPageSize(sPageSize != null ? Integer.valueOf(sPageSize) : null);
		
		Map<Integer, ListColumn> columnsByOrder = new HashMap<Integer, ListColumn>();
		Map<Integer, ButtonPrototype> buttonsByOrder = new HashMap<Integer, ButtonPrototype>();
		
		// find all columns, i.e. elements with key starting with "column"
		for (Object key : params.keySet())
		{
			if (((String)key).startsWith("column"))
			{
				Integer columnIndex = Integer.valueOf(((String)key).substring("column".length()));
				
				LinkedHashMap<?,?> columnDef = (LinkedHashMap<?,?>)params.get(key);
				ListColumn col = new ListColumn();
				
				// convert empty json strings to null
				col.setField(MiscUtils.blankAsNull((String)columnDef.get("field")));
				col.setFormula(MiscUtils.blankAsNull((String)columnDef.get("formula")));
				col.setJavaCallback(MiscUtils.blankAsNull((String)columnDef.get("javaCallback")), compiler.getClassLoader(env), env);
				col.setLink("true".equals(columnDef.get("isLink")));
				col.setSortable("true".equals(columnDef.get("isSortable")));
				col.setLabel(MiscUtils.blankAsNull((String)columnDef.get("label")));
				col.setNameField(MiscUtils.blankAsNull((String)columnDef.get("nameField")));
				col.setIdField(MiscUtils.blankAsNull((String)columnDef.get("idField")));
				
				columnsByOrder.put(columnIndex, col);
			}
			else if (((String)key).equals("buttonPanel"))
			{
				LinkedHashMap<?,?> buttonPanelDef = (LinkedHashMap<?,?>)params.get(key);
				config.setButtonPanel(new ButtonPanel("true".equals(buttonPanelDef.get("custom"))));
			}
			else if (((String)key).startsWith("button"))
			{
				/*Integer buttonIndex = Integer.valueOf(((String)key).substring("button".length()));
				
				LinkedHashMap<?,?> btnDef = (LinkedHashMap<?,?>)params.get(key);
				ButtonPrototype btn = new ButtonPrototype(type)
				col.setField((String)columnDef.get("field"));
				col.setFormula((String)columnDef.get("formula"));
				col.setLink("true".equals(columnDef.get("isLink")));
				col.setSortable("true".equals(columnDef.get("isSortable")));
				col.setLabel((String)columnDef.get("label"));
				col.setNameField((String)columnDef.get("nameField"));
				col.setIdField((String)columnDef.get("idField"));
				
				buttonsByOrder.put(buttonIndex, col);*/
			}
		}
		
		// add columns to config in proper order
		for (int i = 0; i < columnsByOrder.size(); i++)
		{
			config.addColumn(columnsByOrder.get(i));
		}
		
		config.applyDefaults();
		return config;
	}
	
	public void addRmParams (PageData pageData) throws KmParamException
	{
		if (pageData == null || pageData.getRmParams() == null)
		{
			return;
		}
		
		setRmParams(pageData.getRmParams());
		
		KmParamNode listSelectEvent = pageData.getRmParams().getEventNode("listselect");
		
		if (listSelectEvent == null)
		{
			return;
		}
		
		// translate rm parameters into Javascript code
		if (listSelectEvent.getActionNodes() != null)
		{
			for (Set<Action> listSelectActions : listSelectEvent.getActionNodes().values())
			{
				for (Action listSelectAction : listSelectActions)
				{
					if (listSelectAction instanceof Action)
					{
						if (listSelectAction instanceof SetField)
						{
							String setAction = "document.getElementById('" + ((SetField)listSelectAction).getField() + "').value = '" + ((SetField)listSelectAction).getValue() + "'";
							addOnListItemSelect(setAction);
						}
						else if (listSelectAction instanceof SetParentField)
						{
							String setAction = "window.parent.document.getElementById('" + ((SetParentField)listSelectAction).getField() + "').value = '" + ((SetParentField)listSelectAction).getValue() + "'";
							addOnListItemSelect(setAction);
						}
						else if (listSelectAction instanceof ExecuteCode)
						{
							addOnListItemSelect(((ExecuteCode)listSelectAction).getCode());
						}
						else
						{
							if (listSelectAction.getName().equals("close"))
							{
								addOnListItemSelect("open(location, '_self').close();");
							}
							else if (listSelectAction.getName().equals("closedialog"))
							{
								addOnListItemSelect("$.closeRialog()");
							}
							else
							{
								throw new KmParamException("Unsupported action " + listSelectAction.getName() + " defined as rm parameter");
							}
						}
					}
					else
					{
						throw new KmParamException("Unsupported node type in object list " + listSelectAction.getClass().getName());
					}
				}
			}
		}
	}

	public void setLookupId(String lookupId)
	{
		this.lookupId = lookupId;
	}

	public String getLookupId()
	{
		return lookupId;
	}

	public String getServletHost()
	{
		return servletHost;
	}

	public void setServletHost(String servletHost)
	{
		this.servletHost = servletHost;
	}
}
