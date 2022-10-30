/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.DICTIONARY_ITEM_API_NAME)
public class DictionaryItem extends StandardTypeRecordProxy
{
	private String name;
	private String key;
	private Dictionary dictionary;
	private Integer index;
	
	public DictionaryItem() throws KommetException
	{
		this(null, null);
	}
	
	public DictionaryItem(Record record, EnvData env) throws KommetException
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

	@Property(field = "key")
	public String getKey()
	{
		return key;
	}

	public void setKey(String key)
	{
		this.key = key;
		setInitialized();
	}

	@Property(field = "dictionary")
	public Dictionary getDictionary()
	{
		return dictionary;
	}

	public void setDictionary(Dictionary dictionary)
	{
		this.dictionary = dictionary;
		setInitialized();
	}

	@Property(field = "index")
	public Integer getIndex()
	{
		return index;
	}

	public void setIndex(Integer index)
	{
		this.index = index;
		setInitialized();
	}
}