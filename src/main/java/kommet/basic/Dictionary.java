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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.DICTIONARY_API_NAME)
public class Dictionary extends StandardTypeRecordProxy
{
	private String name;
	private ArrayList<DictionaryItem> items;
	
	public Dictionary() throws KommetException
	{
		this(null, null);
	}
	
	public Dictionary(Record record, EnvData env) throws KommetException
	{
		super(record, true, env);
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "items")
	public ArrayList<DictionaryItem> getItems()
	{
		return items;
	}

	public void setItems(ArrayList<DictionaryItem> items)
	{
		this.items = items;
		setInitialized();
	}

	public boolean hasValue(Object value)
	{
		if (this.items != null)
		{
			for (DictionaryItem item : this.items)
			{
				if (item.getName().equals(value))
				{
					return true;
				}
			}
		}
		
		return false;
	}

	public void addItem(DictionaryItem item)
	{
		if (this.items == null)
		{
			this.items = new ArrayList<DictionaryItem>();
		}
		this.items.add(item);
	}
}