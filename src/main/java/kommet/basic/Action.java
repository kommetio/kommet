/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.ACTION_API_NAME)
public class Action extends StandardTypeRecordProxy implements Deployable
{
	private Boolean isSystem;
	private String url;
	private View view;
	private Class controller;
	private String controllerMethod;
	private String name;
	private Boolean isPublic;
	private KID typeId;
	private Type type;
	
	public Action() throws KommetException
	{
		this(null, true, null);
	}
	
	public Action(Record action, boolean ignoreUnfetchedFields, EnvData env) throws KommetException
	{
		super(action, true, env);
	}

	public Action(Record r, EnvData env) throws KommetException
	{
		this(r, true, env);
		if (this.typeId != null)
		{
			this.type = env.getType(this.typeId);
		}
	}

	public void setUrl(String url) throws KommetException
	{
		if (!isReservedUrl(url))
		{
			this.url = url;
		}
		else
		{
			throw new KommetException("URL '" + url + "' is reserved and cannot be used");
		}
		setInitialized();
	}

	// TODO implement this method somewhere else in a better way, be sure to include more URLs
	private static boolean isReservedUrl(String url)
	{
		return "details".equals(url) || "edit".equals("url") || "list".equals(url);
	}

	@Property(field = "url")
	public String getUrl()
	{
		return url;
	}

	public void setView(View view)
	{
		this.view = view;
		setInitialized();
	}

	@Property(field = "view")
	public View getView()
	{
		return view;
	}

	public void setController(Class controller)
	{
		this.controller = controller;
		setInitialized();
	}

	@Property(field = "controller")
	public Class getController()
	{
		return controller;
	}

	public void setIsSystem(Boolean isSystem)
	{
		this.isSystem = isSystem;
		setInitialized();
	}

	/**
	 * System views are those created automatically by the platform when a type is created.
	 * @return
	 */
	@Property(field = "isSystem")
	public Boolean getIsSystem()
	{
		return isSystem;
	}

	public void setControllerMethod(String controllerMethod)
	{
		this.controllerMethod = controllerMethod;
		setInitialized();
	}

	@Property(field = "controllerMethod")
	public String getControllerMethod()
	{
		return controllerMethod;
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
			return this.name.substring(0, this.name.indexOf("For") + 3) + " " + this.type.getLabel();
		}
		else
		{
			throw new KommetException("Cannot get interpreted page name because type is not set on action.");
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
			return this.name;
		}
	}

	@Property(field = "isPublic")
	public Boolean getIsPublic()
	{
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic)
	{
		this.isPublic = isPublic;
		setInitialized();
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.ACTION;
	}
}
