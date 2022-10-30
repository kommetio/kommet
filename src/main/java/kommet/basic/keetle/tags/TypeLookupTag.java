/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;

public class TypeLookupTag extends KommetTag
{	
	private static final long serialVersionUID = -4155486845032822123L;
	
	// the prefix of the selected type
	private String value;

	// Name of the field where the found object is stored
	private String name;
	
	// If not all types but only selected ones should be displayed by this lookup,
	// this field contains their list
	private List<Type> types;
	
	private String id;
	protected DataService dataService;
	protected EnvData env;
	protected EnvService envService;
	
	// onclick javascript callback function
	private String onSelect;
	
	public TypeLookupTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		initBean();
		try
		{
			this.env = this.envService.getCurrentEnv(this.pageContext.getSession());
			
			if (this.types == null)
			{
				// rewrite types from Collection into a list
				this.types = new ArrayList<Type>();
				this.types.addAll(env.getCustomTypes());
			}
		}
		catch (KommetException e)
		{
			throw new JspException("Could not initialized environment in view tag: " + e.getMessage(), e);
		}
		
		KeyPrefix typePrefix = null;
		try
		{
			if (StringUtils.hasText(this.value))
			{
				typePrefix = KeyPrefix.get(this.value);
			}
		}
		catch (KeyPrefixException e)
		{
			return exitWithTagError("Value " + value + " is not a valid type key prefix");
		}
		
		try
		{	
			this.pageContext.getOut().write(getCode(typePrefix, name, id, this.onSelect, this.types, this.env));
		}
		catch (IOException e)
		{
			throw new JspException("Error writing to JSP page: " + e.getMessage());
		}
		catch (KeyPrefixException e)
		{
			return exitWithTagError(e.getMessage());
		}
		catch (KommetException e)
		{
			return exitWithTagError(e.getMessage());
		}
		
		cleanUp();
		return EVAL_PAGE;
    }
	
	private String getCode(KeyPrefix keyPrefix, String name, String id, String onSelect, List<Type> types, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		
		if (!StringUtils.hasText(id))
		{
			id = name + "_lookup";
		}
		
		String popupListId = id + "_list";
		
		String visibleFieldId = name + "_visible_lookup";
		String hiddenFieldId = name + "_hidden_lookup";
		String dialogCall = "$('#" + popupListId + "').show(); $('#" + popupListId + "').dialog({ modal: true, minWidth: 650 });";
		String selectJs = "$('#" + hiddenFieldId + "').val('$id'); $('#" + visibleFieldId + "').val('$displayName');$('#" + popupListId + "').dialog('close'); event.preventDefault();";
		
		if (StringUtils.hasText(onSelect))
		{
			selectJs += onSelect + "({ 'selectedKeyPrefix': '$id', 'selectedName': '$displayName' });";
		}
		
		code.append("<input type=\"text\" id=\"" + visibleFieldId + "\" readonly=\"true\" onclick=\"" + dialogCall + "\"");
		if (keyPrefix != null)
		{
			code.append(" value=\"").append(env.getType(keyPrefix).getQualifiedName()).append("\"");
		}
		code.append("></input>");
		
		// add hidden field for the lookup
		code.append("<input type=\"hidden\" id=\"" + hiddenFieldId + "\" name=\"" + name + "\"");
		if (keyPrefix != null)
		{
			code.append(" value=\"" + keyPrefix.getPrefix() + "\"");
		}
		code.append(">");	
		
		// start type list
		code.append("<div id=\"" + popupListId + "\" style=\"display: none; width: 400px\">");
		
		code.append("<table class=\"std-table\" style=\"width: 600px\">");
		code.append("<thead><tr>");
		code.append("<td>Name</td><td>API name</td>");
		code.append("</tr></thead>");
		code.append("<tbody>");
		
		for (Type type : types)
		{
			code.append("<tr><td><a href=\"javascript:;\" onclick=\"" + selectJs.replaceAll("\\$id", type.getKeyPrefix().getPrefix()).replaceAll("\\$displayName", type.getLabel()) + "\">").append(type.getLabel()).append("</a></td>");
			code.append("<td>").append(type.getQualifiedName()).append("</td>");
			code.append("</tr>");
		}
		
		code.append("</tbody>");
		code.append("</table>");
		code.append("</div>");
		
		return code.toString();
	}

	@Override
	protected void cleanUp()
	{
		this.id = null;
		this.types = null;
		this.name = null;
		this.value = null;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}
	
	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
	
	protected void initBean() throws JspException
	{
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(this.pageContext.getServletContext());
        AutowireCapableBeanFactory factory = wac.getAutowireCapableBeanFactory();
        
        if (factory == null)
        {
        	throw new JspException("Bean factory is null");
        }
        
        this.envService = factory.getBean(EnvService.class);
        this.dataService = factory.getBean(DataService.class);
	}

	public void setTypes(List<Type> types)
	{
		this.types = types;
	}

	public List<Type> getTypes()
	{
		return types;
	}

	public void setOnSelect(String onSelect)
	{
		this.onSelect = onSelect;
	}

	public String getOnSelect()
	{
		return onSelect;
	}
}
