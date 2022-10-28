/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.datatable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.keetle.tags.FilterField;
import kommet.basic.keetle.tags.MisplacedTagException;
import kommet.basic.keetle.tags.KommetTag;
import kommet.basic.keetle.tags.ViewWrapperTag;
import kommet.basic.keetle.tags.buttons.Button;
import kommet.basic.keetle.tags.buttons.ButtonPanel;
import kommet.basic.keetle.tags.buttons.ButtonPrototype;
import kommet.basic.keetle.tags.buttons.ButtonType;
import kommet.data.PIR;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.exceptions.NotImplementedException;
import kommet.i18n.InternationalizationService;
import kommet.js.jsti.JSTI;
import kommet.utils.BeanUtils;
import kommet.utils.MiscUtils;

public class DataTableTag extends KommetTag
{
	private static final long serialVersionUID = -774800129270223487L;
	private String query;
	private String id;
	private DataSourceType dataSourceType;
	private List<DataTableColumn> columns;
	private Boolean paginationActive;
	private Integer pageSize;
	private DataTableSearchTag tableSearchTag;
	private InternationalizationService i18n;
	private AuthData authData;
	private EnvService envService;
	private ButtonPanel buttonPanel;
	private Map<String, String> genericOptions;
	private String title;
	
	// The name of the javascript variable that will hold the table instance
	private String var;

	public DataTableTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		ViewWrapperTag viewWrapper = null;
		
		try
		{
			viewWrapper = this.getViewWrapper();
		}
		catch (MisplacedTagException e)
		{
			// this exception is thrown is view wrapper tag is not found
			// but in this case we can ignore it
		}
		
		try
		{
			if (viewWrapper == null)
			{
				envService = (EnvService)BeanUtils.getBean(EnvService.class, this.pageContext.getServletContext());
				i18n = (InternationalizationService)BeanUtils.getBean(InternationalizationService.class, this.pageContext.getServletContext());
			}
			else
			{
				envService = this.getViewWrapper().getEnvService();
				i18n = this.getViewWrapper().getI18n();
			}
		}
		catch (Exception e)
		{
			return exitWithTagError("Error initializing data table tag: " + e.getMessage());
		}
		
		this.authData = AuthUtil.getAuthData(this.pageContext.getSession());
		
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		if (this.dataSourceType == null)
		{
			// set default data source type
			this.dataSourceType = DataSourceType.DATABASE;
		}
		
		EnvData env = null;
		
		try
		{
			env = envService.getCurrentEnv(this.pageContext.getSession());
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error initializing data table tag: " + e.getMessage());
		}
		
		DataTable dt = new DataTable(this.query, DataSourceType.DATABASE, env);
		dt.setPageSize(this.pageSize);
		dt.setVar(this.var);
		dt.setGenericOptions(this.genericOptions);
		dt.setPaginationActive(Boolean.TRUE.equals(this.paginationActive));
		dt.setTitle(this.title);
		
		if (this.columns != null && !this.columns.isEmpty())
		{
			dt.setProperties(this.columns);
		}
		
		try
		{	
			StringBuilder btnPanelCode = new StringBuilder();
			
			StringBuilder jsCode = new StringBuilder();
			jsCode.append("<script>\n$(document).ready(function() {");
			
			boolean hasSearch = this.tableSearchTag != null && !this.tableSearchTag.getFilterFields().isEmpty();
			boolean hasVisibleButtons = hasSearch;
			
			// we want to add a button panel to the table if there are user-defined buttons, or if there is a search panel
			// that requires a button panel to add a search button to it
			if ((this.buttonPanel != null && this.buttonPanel.hasButtons()) || hasSearch)
			{
				// create button panel
				btnPanelCode.append("var btnPanel = km.js.buttonpanel.create({ id: \"").append(this.id).append("-btn-panel\"");
			
				btnPanelCode.append(", cssClass: \"km-record-list-button-panel\"");
				
				/*if (StringUtils.hasText(this.title))
				{
					// let button panel handle the buttons
					code.append(", title: { text: \"").append(this.title).append("\", cssClass: \"km-record-list-title\", placement: \"buttons-left\" }");
				}*/
				
				btnPanelCode.append(" });\n");
			}
			
			if (this.buttonPanel != null && this.buttonPanel.hasButtons())
			{
				for (ButtonPrototype btn : this.buttonPanel.getButtons())
				{
					if (btn instanceof Button)
					{
						Button customBtn = (Button)btn;
						btnPanelCode.append("btnPanel.addButton({");
						btnPanelCode.append("label: \"").append(customBtn.getLabel()).append("\"");
						
						if (StringUtils.hasText(customBtn.getUrl()))
						{
							btnPanelCode.append(", url: \"").append(customBtn.getUrl()).append("\"");
						}
						
						if (StringUtils.hasText(customBtn.getOnClick()))
						{
							btnPanelCode.append(", onClick: ").append(customBtn.getOnClick());
						}
						
						btnPanelCode.append("});\n");
						
						hasVisibleButtons = true;
					}
					else if (btn instanceof ButtonPrototype && ((ButtonPrototype)btn).getType().equals(ButtonType.NEW))
					{
						// TODO this is not optimal - it parses the whole query just to get the type from it
						Type type = getEnv().getSelectCriteriaFromDAL(this.query).getType();
						
						if (authData.canCreateType(type.getKID(), false, getEnv()))
						{
							btnPanelCode.append("btnPanel.addButton({");
							btnPanelCode.append("label: km.js.config.i18n['btn.new']");
							btnPanelCode.append(", url: km.js.config.contextPath + \"/" + type.getKeyPrefix() + "/n\"");
							
							btnPanelCode.append("});\n");
							
							hasVisibleButtons = true;
						}
					}
					else
					{
						throw new NotImplementedException("Standard buttons not implemented");
					}
				}
				
				dt.setTitleHandledByButtonPanel(hasVisibleButtons);
			}
			
			DataTableGeneratedCode generatedCode = dt.getCode();
			jsCode.append(generatedCode.getInitCode());
			this.pageContext.getOut().write(generatedCode.getHtmlCode());
			
			if (hasVisibleButtons)
			{
				jsCode.append(btnPanelCode);
				
				// add button panel to table
				jsCode.append(dt.getDataTableVarName()).append(".setButtonPanel(btnPanel);");
			}
			
			String tableSearchVarName = "tableSearch_" + dt.getDataTableVarName().replaceAll("\\.", "_");
			
			// if table search tag has been defined for this data table
			if (this.tableSearchTag != null && !this.tableSearchTag.getFilterFields().isEmpty())
			{
				// instantiate a table search object
				jsCode.append("var ").append(tableSearchVarName).append(" = km.js.tablesearch.create({\n");
				jsCode.append("table: ").append(dt.getDataTableVarName()).append(", ");
				jsCode.append("columns: ").append(this.tableSearchTag.getColumns()).append(", ");
				
				// define internationalized search label
				String searchLabel = authData.getI18n().get("btn.search");
				if (StringUtils.isEmpty(searchLabel))
				{
					searchLabel = "Search";
				}
				jsCode.append("searchLabel: \"").append(searchLabel).append("\", ");
				
				// create JSTI and pass it to the km.js.tablesearch object
				JSTI jsti = new JSTI();
				// TODO here we are retrieving JSTI for all nested types - perhaps we could
				// retrieve only types used in the query?
				jsti.addType(dt.getBaseType(), env, true, true, authData);
				jsCode.append("jsti: ").append(JSTI.serialize(jsti)).append(", ");
				
				jsCode.append("properties: [");
				
				List<String> propertyCode = new ArrayList<String>();
				for (FilterField field : this.tableSearchTag.getFilterFields())
				{
					String prop = "{ id: \"" + PIR.get(field.getField(), dt.getBaseType(), env) + "\", ";
					prop += "operator: \"" + field.getComparison() + "\"";
					
					if (StringUtils.hasText(field.getLabel()))
					{
						prop += ", label: \"" + field.getLabel() + "\"";
					}
					prop += "}";
					
					propertyCode.add(prop);
				}
				
				jsCode.append(MiscUtils.implode(propertyCode, ", "));
				jsCode.append("]");
				jsCode.append("});\n");
				
				jsCode.append(tableSearchVarName).append(".render(function(code) { ").append(generatedCode.getTarget()).append(".prepend(code); });");
			}
			
			jsCode.append("});\n</script>\n");
			this.pageContext.getOut().write(jsCode.toString());
		}
		catch (IOException e)
		{
			return exitWithTagError("Error writing HTML to standard output");
		}
		catch (KommetException e)
		{
			return exitWithTagError(e.getMessage());
		}
		
		cleanUp();
		return EVAL_PAGE;
    }
	
	@Override
	protected void cleanUp()
	{
		this.query = null;
		this.dataSourceType = null;
		this.id = null;
		this.columns = null;
		this.i18n = null;
		super.cleanUp();
	}

	public String getQuery()
	{
		return query;
	}

	public void setQuery(String query)
	{
		this.query = query;
	}

	public String getDataSourceType()
	{
		return dataSourceType.name();
	}

	public void setDataSourceType(String dataSourceType)
	{
		this.dataSourceType = DataSourceType.valueOf(dataSourceType.toUpperCase());
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}
	
	public void addColumn(DataTableColumn col)
	{
		if (this.columns == null)
		{
			this.columns = new ArrayList<DataTableColumn>();
		}
		this.columns.add(col);
	}

	public Boolean getPaginationActive()
	{
		return paginationActive;
	}

	public void setPaginationActive(Boolean paginationActive)
	{
		this.paginationActive = paginationActive;
	}

	public Integer getPageSize()
	{
		return pageSize;
	}

	public void setPageSize(Integer pageSize)
	{
		this.pageSize = pageSize;
	}

	public DataTableSearchTag getTableSearchTag()
	{
		return tableSearchTag;
	}

	public void setTableSearchTag(DataTableSearchTag tableSearchTag)
	{
		this.tableSearchTag = tableSearchTag;
	}

	public InternationalizationService getI18n()
	{
		return i18n;
	}

	public AuthData getAuthData()
	{
		return this.authData;
	}

	public ButtonPanel getButtonPanel()
	{
		return buttonPanel;
	}

	public void setButtonPanel(ButtonPanel buttonPanel)
	{
		this.buttonPanel = buttonPanel;
	}

	public String getVar()
	{
		return var;
	}

	public void setVar(String var)
	{
		this.var = var;
	}

	public void setDataTableOption(String name, String value)
	{
		if (this.genericOptions == null)
		{
			this.genericOptions = new HashMap<String, String>();
		}
		this.genericOptions.put(name, value);
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
}
