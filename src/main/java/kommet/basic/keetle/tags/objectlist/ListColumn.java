/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.objectlist;

import java.lang.reflect.Method;
import java.util.Date;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.keetle.tags.TagException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetClassLoader;
import kommet.utils.MiscUtils;
import kommet.utils.VarInterpreter;

/**
 * Represents a single column displayed by an object list.
 * @author Radek Krawiec
 * @created 15-03-2014
 */
public class ListColumn
{
	private String field;
	private String formula;
	private boolean isLink;
	private boolean isSortable;
	private String idField;
	private String nameField;
	private String label;
	private String javaCallback;
	private Method javaCallbackMethod;
	private ListColumnType type;
	private String onClick;
	private String url;
	
	public String getCode (Object item, String itemVar, String onSelectHandler, AuthData authData, String contextPath) throws KommetException
	{	
		if (item instanceof Record)
		{
			return getCodeForRecordItem((Record)item, itemVar, onSelectHandler, authData, contextPath);
		}
		else
		{
			return getCodeForBeanItem(item, itemVar, onSelectHandler, authData, contextPath);
		}
	}
	
	private String getCodeForRecordItem(Record record, String itemVar, String onSelectHandler, AuthData authData, String contextPath) throws KommetException
	{
		// check if the columns definition is complete and consistent
		validate();
		
		StringBuilder code = new StringBuilder();
		
		if (isLink)
		{
			code.append("<a href=\"");
			
			String itemId = record.getKID().getId();
			String itemName = record.getDefaultFieldValue(authData.getLocale());
			
			if (!StringUtils.hasText(onSelectHandler))
			{
				code.append(contextPath).append("/").append(itemId).append("\"");
			}
			else
			{
				code.append("javascript:;\"");
				// substitute parameter names for record's actual ID and name
				String interpretedSelectHandler = onSelectHandler.replaceAll("\\$id", (record).getKID().getId()).replaceAll("\\$displayField", itemName);
				code.append(" onclick=\"").append(interpretedSelectHandler).append("\"");
			}
			
			// close link tag
			code.append(">");
		}
		
		if (StringUtils.hasText(javaCallback))
		{
			if (StringUtils.hasText(this.field))
			{
				try
				{
					code.append(this.javaCallbackMethod.invoke(null, record.getField(this.field)));
				}
				catch (Exception e)
				{
					throw new KommetException("Could not render column value. Executing Java callback method " + this.javaCallback + " on field value failed. Nested: " + e.getMessage());
				}
			}
			else
			{
				try
				{
					code.append(this.javaCallbackMethod.invoke(null, record));
				}
				catch (Exception e)
				{
					throw new KommetException("Could not render column value. Executing Java callback method " + this.javaCallback + " on record failed. Nested: " + e.getMessage());
				}
			}
		}
		else if (StringUtils.hasText(this.field))
		{
			code.append(getRecordValue(record, this.field, authData));
		}
		else
		{
			code.append(VarInterpreter.interprete(record, this.formula, itemVar, authData.getLocale()));
		}
		
		if (isLink)
		{
			// close link tag
			code.append("</a>");
		}
		
		return code.toString();
	}
	
	private String getCodeForBeanItem(Object object, String itemVar, String onSelectHandler, AuthData authData, String contextPath) throws KommetException
	{
		// check if the columns definition is complete and consistent
		validate();
		validateBeanColumn();
		
		StringBuilder code = new StringBuilder();
		
		if (isLink)
		{
			code.append("<a href=\"");
			
			String itemId;
			String itemName;
			
			try
			{
				itemId = MiscUtils.nullAsBlank(PropertyUtils.getProperty(object, this.idField)).toString();
				itemName = MiscUtils.nullAsBlank(PropertyUtils.getProperty(object, this.nameField)).toString();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new TagException("Error reading property value from bean: " + e.getMessage(), ListColumnTag.class.getName());
			}
			
			if (!StringUtils.hasText(onSelectHandler))
			{
				code.append(contextPath).append("/").append(itemId).append("\"");
			}
			else
			{
				code.append("javascript:;\"");
				// substitute parameter names for record's actual ID and name
				String interpretedSelectHandler = onSelectHandler.replaceAll("\\$id", itemId).replaceAll("\\$displayField", itemName);
				code.append(" onclick=\"").append(interpretedSelectHandler).append("\"");
			}
			
			// close link tag
			code.append(">");
		}
		
		if (StringUtils.hasText(javaCallback))
		{
			if (StringUtils.hasText(this.field))
			{
				try
				{
					code.append(this.javaCallbackMethod.invoke(null, PropertyUtils.getProperty(object, this.field)));
				}
				catch (Exception e)
				{
					throw new KommetException("Could not render column value. Executing Java callback method " + this.javaCallback + " on field value failed. Nested: " + e.getMessage());
				}
			}
			else
			{
				try
				{
					code.append(this.javaCallbackMethod.invoke(null, object));
				}
				catch (Exception e)
				{
					throw new KommetException("Could not render column value. Executing Java callback method " + this.javaCallback + " on record failed. Nested: " + e.getMessage());
				}
			}
		}
		else if (StringUtils.hasText(this.field))
		{
			code.append(getBeanValue(object, this.field, authData));
		}
		else
		{
			code.append(VarInterpreter.interprete(object, this.formula, itemVar, authData.getLocale()));
		}
		
		if (isLink)
		{
			// close link tag
			code.append("</a>");
		}
		
		return code.toString();
	}
	
	private void validateBeanColumn() throws TagException
	{
		if (isLink)
		{
			if (!StringUtils.hasText(idField))
			{
				throw new TagException("ID field not specified for column displayed from bean value and rendered as a link", ListColumnTag.class.getName());
			}
			
			if (!StringUtils.hasText(nameField))
			{
				throw new TagException("Name field not specified for column displayed from bean value and rendered as a link", ListColumnTag.class.getName());
			}
		}
	}

	private void validate() throws TagException
	{
		if (this.field != null)
		{
			if (this.formula != null)
			{
				throw new TagException("Both field and formula attributes cannot be set for a list column", ListColumnTag.class.getName());
			}
		}
		else if (StringUtils.hasText(this.javaCallback))
		{
			if (StringUtils.hasText(this.formula))
			{
				throw new TagException("Both Java callback and formula attributes are set on list column tag", ListColumnTag.class.getName());
			}
		}
		else
		{
			if (!StringUtils.hasText(this.formula))
			{
				throw new TagException("Neither field nor formula attributes are set on list column tag", ListColumnTag.class.getName());
			}
			
			if (isLink)
			{
				throw new TagException("List columns displayed as formulas cannot be rendered as links", ListColumnTag.class.getName());
			}
		}
	}

	/*private FieldDisplayType getDisplayType()
	{
		if (this.field != null)
		{
			return FieldDisplayType.FIELD;
		}
		else
		{
			return FieldDisplayType.FORMULA;
		}
	}*/
	
	private Object getBeanValue(Object obj, String property, AuthData authData) throws KommetException
	{
		Object value = null;
		try
		{
			value = PropertyUtils.getProperty(obj, property);
		}
		catch (Exception e)
		{
			throw new TagException("Error reading property " + property + " from bean: " + e.getMessage(), ListColumn.class.getName());
		}
		
		if (value instanceof Date)
		{
			// format date
			value = MiscUtils.formatDateTimeByUserLocale((Date)value, authData);
		}
		
		return MiscUtils.nullAsBlank(value);
	}

	private Object getRecordValue(Record record, String field, AuthData authData) throws KommetException
	{
		Object value = record.getField(field);
		Type type = record.getType();
		
		if (value == null)
		{
			return "";
		}
			
		if (type.getField(field).getDataType().getId().equals(DataType.DATETIME))
		{
			// format date
			value = MiscUtils.formatDateTimeByUserLocale((Date)value, authData);
		}
		else if (type.getField(field).getDataType().getId().equals(DataType.DATE))
		{
			// format date
			value = MiscUtils.formatDateByUserLocale((Date)value, authData);
		}
		else if (type.getField(field).getDataType().getId().equals(DataType.BOOLEAN))
		{
			// display i18n value for boolean field
			value = (Boolean)value ? authData.getI18n().get("bool.yes") : authData.getI18n().get("bool.no");
		}
		
		return MiscUtils.nullAsBlank(value);
	}

	public void setField(String field)
	{
		this.field = field;
	}
	public String getField()
	{
		return field;
	}
	public void setLink(boolean isLink)
	{
		this.isLink = isLink;
	}
	public boolean isLink()
	{
		return isLink;
	}
	public void setSortable(boolean isSortable)
	{
		this.isSortable = isSortable;
	}
	public boolean isSortable()
	{
		return isSortable;
	}
	public void setLabel(String label)
	{
		this.label = label;
	}
	public String getLabel()
	{
		return label;
	}
	public void setFormula(String formula)
	{
		this.formula = formula;
	}
	public String getFormula()
	{
		return formula;
	}
	
	public void setNameField(String nameField)
	{
		this.nameField = nameField;
	}

	public String getNameField()
	{
		return nameField;
	}
	
	public String getIdField()
	{
		return idField;
	}

	public void setIdField(String idField)
	{
		this.idField = idField;
	}

	public void setType(ListColumnType type)
	{
		this.type = type;
	}

	public ListColumnType getType()
	{
		return type;
	}

	public String getJavaCallback()
	{
		return javaCallback;
	}

	public void setJavaCallback(String javaCallback, KommetClassLoader classLoader, EnvData env) throws KommetException
	{
		this.javaCallback = javaCallback;
		
		if (StringUtils.hasText(javaCallback))
		{
			if (MiscUtils.isEnvSpecific(javaCallback, env))
			{
				throw new KommetException("Java callback method must not be specified in env-specific format");
			}
			
			// The javaCallback method can take exactly one parameter, either of type Record or Object.
			// Also, the name of the type can be both env-specific and non-env specific, so we must try both.
			
			this.javaCallbackMethod = MiscUtils.getStaticMethod(javaCallback, classLoader, Record.class);
			
			if (this.javaCallbackMethod == null)
			{
				this.javaCallbackMethod = MiscUtils.getStaticMethod(javaCallback, classLoader, Object.class);
			}
			
			if (this.javaCallbackMethod == null)
			{	
				this.javaCallbackMethod = MiscUtils.getStaticMethod(javaCallback, classLoader, Record.class);
				
				if (this.javaCallbackMethod == null)
				{
					this.javaCallbackMethod = MiscUtils.getStaticMethod(javaCallback, classLoader, Object.class);
				}
				
				if (this.javaCallbackMethod == null)
				{
					throw new KommetException("Java callback static method " + javaCallback + " not found. Make sure the method is public and static.");
				}
			}
			
		}
		else
		{
			this.javaCallbackMethod = null;
		}
	}

	public String getOnClick()
	{
		return onClick;
	}

	public void setOnClick(String onClick)
	{
		this.onClick = onClick;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	enum FieldDisplayType
	{
		FIELD,
		FORMULA;
	}
}
