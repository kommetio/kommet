/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.SettingValue;
import kommet.data.KID;
import kommet.filters.BasicFilter;
import kommet.uch.UserCascadeHierarchyContext;

public class SettingValueFilter extends BasicFilter<SettingValue>
{
	private Set<String> keys;
	private Set<String> values;
	private Set<KID> rids;
	private boolean fetchAllUchFields;
	private UserCascadeHierarchyContext context;
	private Object contextValue;
	
	public void addKey(String key)
	{
		if (this.keys == null)
		{
			this.keys = new HashSet<String>();
		}
		this.keys.add(key);
	}
	
	public void addValue(String value)
	{
		if (this.values == null)
		{
			this.values = new HashSet<String>();
		}
		this.values.add(value);
	}
	
	public void addKID (KID id)
	{
		if (this.rids == null)
		{
			this.rids = new HashSet<KID>();
		}
		this.rids.add(id);
	}

	public Set<String> getKeys()
	{
		return keys;
	}

	public void setKeys(Set<String> keys)
	{
		this.keys = keys;
	}

	public Set<String> getValues()
	{
		return values;
	}

	public void setValues(Set<String> values)
	{
		this.values = values;
	}

	public Set<KID> getKIDs()
	{
		return rids;
	}

	public void setRids(Set<KID> rids)
	{
		this.rids = rids;
	}

	public boolean isFetchAllUchFields()
	{
		return fetchAllUchFields;
	}

	public void setFetchAllUchFields(boolean fetchAllUchFields)
	{
		this.fetchAllUchFields = fetchAllUchFields;
	}

	public UserCascadeHierarchyContext getContext()
	{
		return context;
	}

	public void setContext(UserCascadeHierarchyContext context)
	{
		this.context = context;
	}

	public Object getContextValue()
	{
		return contextValue;
	}

	public void setContextValue(Object contextValue)
	{
		this.contextValue = contextValue;
	}
}