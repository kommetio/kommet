/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.LIBRARY_API_NAME)
public class Library extends StandardTypeRecordProxy
{
	private String name;
	private Boolean isEnabled;
	private ArrayList<LibraryItem> items;
	private String provider;
	private String version;
	private String description;
	private String source;
	private String accessLevel;
	
	/**
	 * The status field can have the following values:
	 * - Installed - library successfully installed and activated
	 * - Installed-Deactivated - library was installed and then deactivated, but some of its components that are not uninstallable may still be active
	 * - Installation failed - an attempt was made to install the library but deployment failed
	 * - Not installed - library is downloaded but not installed 
	 */
	private String status;
	
	public Library() throws KommetException
	{
		this(null, null);
	}
	
	public Library(Record lib, EnvData env) throws KommetException
	{
		super(lib, true, env);
	}

	@Property(field = "name")
	public String getName()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "isEnabled")
	public Boolean getIsEnabled()
	{
		return isEnabled;
	}

	public void setIsEnabled(Boolean isEnabled)
	{
		this.isEnabled = isEnabled;
		setInitialized();
	}

	@Property(field = "items")
	public ArrayList<LibraryItem> getItems()
	{
		return items;
	}

	public void setItems(ArrayList<LibraryItem> items)
	{
		this.items = items;
		setInitialized();
	}

	@Property(field = "provider")
	public String getProvider()
	{
		return provider;
	}

	public void setProvider(String provider)
	{
		this.provider = provider;
		setInitialized();
	}

	@Property(field = "version")
	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
		setInitialized();
	}

	@Property(field = "description")
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
		setInitialized();
	}

	@Property(field = "source")
	public String getSource()
	{
		return source;
	}

	public void setSource(String source)
	{
		this.source = source;
		setInitialized();
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

	public void addItem(LibraryItem item)
	{
		if (this.items == null)
		{
			this.items = new ArrayList<LibraryItem>();
		}
		this.items.add(item);
		item.setLibrary(this);
	}

	@Property(field = "status")
	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
		setInitialized();
	}
}