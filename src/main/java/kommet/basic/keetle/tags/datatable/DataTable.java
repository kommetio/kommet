/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.datatable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.util.StringUtils;

import kommet.dao.queries.Criteria;
import kommet.dao.queries.jcr.JCR;
import kommet.dao.queries.jcr.JCRUtil;
import kommet.dao.queries.jcr.JcrSerializationException;
import kommet.data.Field;
import kommet.data.PIR;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.js.jsti.JSTI;
import kommet.utils.MiscUtils;

public class DataTable
{
	private String query;
	private DataSourceType dataSourceType;
	private EnvData env;
	private List<DataTableColumn> properties = new ArrayList<DataTableColumn>();
	private Integer pageSize;
	private boolean paginationActive;
	private String cssClass;
	private static final String DEFAULT_CSS_CLASS = "km-datatable-std";
	private String dataTableVarName;
	private Type baseType;
	private String title;
	private boolean titleHandledByButtonPanel = false;
	
	// generic options set by the user that will be passed directly to the
	// configuration of the underlying km.js.table object
	private Map<String, String> genericOptions;
	
	private String jqueryTarget;
	
	// The name of the javascript variable that will hold the table instance
	private String var;

	public DataTable (String query, DataSourceType dataSourceType, EnvData env)
	{
		this.query = query;
		this.dataSourceType = dataSourceType;
		this.env = env;
	}
	
	/**
	 * Generate code of the data table.
	 * @param id
	 * @return
	 * @throws KommetException
	 */
	public DataTableGeneratedCode getCode() throws KommetException
	{
		// get select criteria from DAL to find out what type is queried
		Criteria criteria = env.getSelectCriteriaFromDAL(this.query);
		
		this.baseType = criteria.getType();
		
		// generate a random ID of this component
		String componentId = "datatable" + (new Random()).nextInt(1000);
		
		// get JSTI for the queried type
		JSTI jsti = new JSTI();
		jsti.addType(criteria.getType(), env, true, false, env.currentAuthData());
		
		StringBuilder code = new StringBuilder();
		
		String configName = "tableConfig" + componentId;
		code.append("var ").append(configName).append(" = {};");
		
		// create datasource
		code.append(configName).append(".datasource = km.js.datasource.create({ type: \"database\" });\n");
				
		// add JSTI
		code.append(configName).append(".jsti = ").append(JSTI.serialize(jsti)).append(";\n");
		
		// if no properties are specified to be displayed, display the default field for the type
		if (this.properties.isEmpty())
		{
			DataTableColumn defaultColumn = new DataTableColumn();
			defaultColumn.setFieldApiName(criteria.getType().getDefaultFieldApiName());
			this.properties.add(defaultColumn);
		}
		
		List<String> propertyDisplaySettings = new ArrayList<String>();
		
		for (DataTableColumn prop : this.properties)
		{
			// set labels for all columns for which they haven't been set
			if (!StringUtils.hasText(prop.getLabel()))
			{
				prop.setLabel(criteria.getType().getField(prop.getFieldApiName(), env).getInterpretedLabel(env.currentAuthData()));
			}
			
			PIR propertyPIR = PIR.get(prop.getFieldApiName(), criteria.getType(), env);
			StringBuilder setting = new StringBuilder();
			setting.append("{");
			setting.append("id: \"" + propertyPIR.getValue() + "\", ");
			setting.append("label: \"" + prop.getLabel() + "\", ");
			setting.append("sortable: " + prop.isSortable() + ", ");
			
			String onClick = null;
			
			if (StringUtils.hasText(prop.getOnClick()))
			{
				onClick = "function(recordId) { " + prop.getOnClick() + " }";
			}
			else
			{
				onClick = "function(recordId) { km.js.utils.openURL(km.js.config.contextPath + '/' + recordId); }";
			}
			
			if (prop.isLink() || StringUtils.hasText(prop.getOnClick()))
			{
				setting.append("onClick: ").append(onClick).append(", ");
			}
			
			setting.append("linkStyle: " + prop.isLinkStyle() + ", ");
			
			if (StringUtils.hasText(prop.getUrl()))
			{
				setting.append("url: \"" + prop.getUrl() + "\", ");
			}
				
			if (StringUtils.hasText(prop.getFormatFunction()))
			{
				setting.append("format: " + prop.getFormatFunction() + ", ");
			}
			
			setting.append("filterable: " + prop.isFilterable() + " ");
			setting.append("}");
			propertyDisplaySettings.add(setting.toString());
		}
		
		// translate criteria to JCR
		JCR jcr = JCRUtil.getJCRFromDALCriteria(criteria, env);
		try
		{
			code.append(configName).append(".jcr = ").append(JCRUtil.serialize(jcr)).append(";\n");
		}
		catch (JcrSerializationException e)
		{
			throw new KommetException("Error serializing JCR criteria to JSON. Nested: " + e.getMessage());
		}
		
		// build table display options
		code.append(configName).append(".tableDisplay = {");
		code.append("properties: [ ").append(MiscUtils.implode(propertyDisplaySettings, ", ")).append(" ], ");
		code.append("idProperty: { id: \"").append(PIR.get(Field.ID_FIELD_NAME, criteria.getType(), env)).append("\"} };\n");
		
		// build table config
		code.append(configName).append(".tableOptions = {");
		code.append("id: \"datatable-").append(componentId).append("\", ");
		code.append("jsti: ").append(configName).append(".jsti, ");
		code.append("pageSize: ").append(this.pageSize).append(", ");
		code.append("paginationActive: ").append(this.paginationActive).append(", ");
		code.append("wrapperCssClass: \"").append(StringUtils.hasText(this.cssClass) ? this.cssClass : DEFAULT_CSS_CLASS).append("\", ");
		
		if (StringUtils.hasText(this.title))
		{
			String titlePlacement = this.titleHandledByButtonPanel ? "buttons-left" : "top";
			code.append("title: { text: \"").append(this.title).append("\", cssClass: \"km-record-list-title km-title\", placement: \"" + titlePlacement + "\" }, ");
		}
		
		code.append("cssClass: \"km-table-std\" };\n");
		
		if (this.genericOptions != null)
		{
			for (String optionName : this.genericOptions.keySet())
			{
				// append option
				code.append(configName).append(".tableOptions.").append(optionName).append(" = ").append(this.genericOptions.get(optionName)).append(";\n");
			}
		}
		
		// if the name of the data table var has been defined by the user, use this name,
		// otherwise generate a name from the component ID
		this.dataTableVarName = StringUtils.hasText(this.var) ? this.var : "dataTable" + componentId;
		
		if (!this.dataTableVarName.contains("."))
		{
			// if variable name does not contain a dot, it is not a qualified property of some object, so it needs to be declared
			code.append("var ");
		}
		
		// create table
		code.append(dataTableVarName).append(" = km.js.table.create(");
		code.append(configName).append(".datasource, ");
		code.append(configName).append(".jcr, ");
		code.append(configName).append(".tableDisplay, ");
		code.append(configName).append(".tableOptions);");
		
		String containerId = "datatable-container-" + componentId;
		String target = StringUtils.hasText(jqueryTarget) ? jqueryTarget : "$(\"#" + containerId + "\")";
		
		// render the table
		code.append(dataTableVarName).append(".render(null, function(code) {\n");
		
		// if container already contains a rendered table, remove it
		code.append(target).append(".find(\".km-table-container\").remove();\n");
		code.append(target + ".append(code); });\n");
		
		DataTableGeneratedCode generatedCode = new DataTableGeneratedCode();
		generatedCode.setInitCode(code.toString());
		
		// create a container for the list that will be filled by the javascript function
		generatedCode.setHtmlCode("<div id=\"" + containerId + "\" class=\"km-datatable-container-std\"></div>");
		generatedCode.setTarget(target);
		
		return generatedCode;
	}
	
	public void addProperty (DataTableColumn property)
	{
		this.properties.add(property);
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
	
	public Integer getPageSize()
	{
		return pageSize;
	}

	public void setPageSize(Integer pageSize)
	{
		this.pageSize = pageSize;
	}

	public boolean isPaginationActive()
	{
		return paginationActive;
	}

	public void setPaginationActive(boolean paginationActive)
	{
		this.paginationActive = paginationActive;
	}

	public void setProperties(List<DataTableColumn> properties)
	{
		this.properties = properties;
	}

	public String getCssClass()
	{
		return cssClass;
	}

	public void setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
	}

	public String getDataTableVarName()
	{
		return dataTableVarName;
	}

	public Type getBaseType()
	{
		return baseType;
	}
	
	public String getVar()
	{
		return var;
	}

	public void setVar(String var)
	{
		this.var = var;
	}

	public Map<String, String> getGenericOptions()
	{
		return genericOptions;
	}

	public void setGenericOptions(Map<String, String> genericOptions)
	{
		this.genericOptions = genericOptions;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public boolean isTitleHandledByButtonPanel()
	{
		return titleHandledByButtonPanel;
	}

	public void setTitleHandledByButtonPanel(boolean titleHandledByButtonPanel)
	{
		this.titleHandledByButtonPanel = titleHandledByButtonPanel;
	}
	
	public String getJqueryTarget()
	{
		return jqueryTarget;
	}

	public void setJqueryTarget(String jqueryTarget)
	{
		this.jqueryTarget = jqueryTarget;
	}
}
