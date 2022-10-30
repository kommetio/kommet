/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.keetle.ViewUtil;
import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.VIEW_API_NAME)
public class View extends StandardTypeRecordProxy implements Deployable
{
	private Boolean isSystem;
	private String name;
	private String path;
	private String keelteCode;
	private String jspCode;
	
	/**
	 * This is a non-env-specific package name. View package names are not prefixed with the env prefix.
	 */
	private String packageName;
	
	private KID typeId;
	private Type type;
	
	/**
	 * Layout in which this view is displayed.
	 */
	private Layout layout;
	
	private String accessLevel;
	
	public View() throws KommetException
	{
		this(null, null);
	}
	
	public View(Record view, EnvData env) throws KommetException
	{
		this(view, false, env);
	}

	public View(Record record, boolean ignoreUnfetchedFields, EnvData env) throws KommetException
	{
		super(record, ignoreUnfetchedFields, env);
		if (this.typeId != null)
		{
			this.type = env.getType(this.typeId);
		}
	}

	public void setIsSystem(Boolean system)
	{
		this.isSystem = system;
		setInitialized();
	}

	@Property(field = "isSystem")
	public Boolean getIsSystem()
	{
		return isSystem;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setPath(String path)
	{
		this.path = path;
		setInitialized();
	}

	@Property(field = "path")
	public String getPath()
	{
		return path;
	}
	
	public void setKeetleCode(String code)
	{
		this.keelteCode = code;
		setInitialized();
	}

	public void initKeetleCode(String code, AppConfig config, EnvData env) throws KommetException
	{
		this.keelteCode = code;
		setInitialized("keetleCode");
		
		// initialize JSP code
		this.jspCode = ViewUtil.keetleToJSP(this.keelteCode, config, env);
		setInitialized("jspCode");
	}

	@Property(field = "keetleCode")
	public String getKeetleCode()
	{
		return keelteCode;
	}

	public void setPackageName(String packageName)
	{
		this.packageName = packageName;
		setInitialized();
	}

	@Property(field = "packageName")
	public String getPackageName()
	{
		return packageName;
	}

	public void setTypeId(KID typeId)
	{
		this.typeId = typeId;
		setInitialized();
	}

	@Property(field = "typeId")
	public KID getTypeId()
	{
		return typeId;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	@Transient
	public Type getType()
	{
		return type;
	}
	
	@Transient
	public String getTypeSpecificName() throws KommetException
	{
		if (this.type != null)
		{
			// the view name ends with a prefix, so we replace it by type label
			return this.name.substring(0, this.name.length() - KeyPrefix.LENGTH) + this.type.getLabel();
		}
		else
		{
			throw new KommetException("Cannot get interpreted view name because type is not set on action.");
		}
	}
	
	@Transient
	public String getInterpretedName() throws KommetException
	{
		if (this.type != null)
		{
			return getTypeSpecificName();
		}
		else
		{
			return (this.packageName != null ? (this.packageName + ".") : "") + this.name;
		}
	}

	public void setJspCode(String jspCode)
	{
		this.jspCode = jspCode;
		setInitialized();
	}

	@Property(field = "jspCode")
	public String getJspCode()
	{
		return jspCode;
	}

	public void setLayout(Layout layout)
	{
		this.layout = layout;
		setInitialized();
	}

	@Property(field = "layout")
	public Layout getLayout()
	{
		return layout;
	}
	
	@Transient
	public String getQualifiedName()
	{
		return (this.packageName != null ? this.packageName + "." : "") + this.name;
	}

	@Property(field = "accessLevel")
	public String getAccessLevel()
	{
		return accessLevel;
	}

	public void setAccessLevel(String accessLevel)
	{
		this.accessLevel = accessLevel;
		setInitialized();
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.VIEW;
	}
}