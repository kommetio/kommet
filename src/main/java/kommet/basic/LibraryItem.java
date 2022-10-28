/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.LIBRARY_ITEM_API_NAME)
public class LibraryItem extends StandardTypeRecordProxy
{
	private KID recordId;
	private Library library;
	private String apiName;
	private String definition;
	private String accessLevel;
	private Integer componentType;
	
	public LibraryItem() throws KommetException
	{
		this(null, null);
	}
	
	public LibraryItem(Record item, EnvData env) throws KommetException
	{
		super(item, true, env);
	}

	@Property(field = "recordId")
	public KID getRecordId()
	{
		return recordId;
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
		setInitialized();
	}

	@Property(field = "library")
	public Library getLibrary()
	{
		return library;
	}

	public void setLibrary(Library library)
	{
		this.library = library;
		setInitialized();
	}

	@Property(field = "apiName")
	public String getApiName()
	{
		return apiName;
	}

	public void setApiName(String apiName) throws LibraryException
	{
		if (!apiName.contains("."))
		{
			throw new LibraryException("API name of a library item must be qualified. The name " + apiName + " must therefore contain at least one dot");
		}
		
		this.apiName = apiName;
		setInitialized();
	}

	@Property(field = "definition")
	public String getDefinition()
	{
		return definition;
	}

	public void setDefinition(String definition)
	{
		this.definition = definition;
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

	@Property(field = "componentType")
	public Integer getComponentType()
	{
		return componentType;
	}

	public void setComponentType(Integer componentType)
	{
		this.componentType = componentType;
		setInitialized();
	}
}