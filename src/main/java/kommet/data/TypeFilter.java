/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.HashSet;
import java.util.Set;

import kommet.filters.BasicFilter;

public class TypeFilter extends BasicFilter<Type>
{
	private String apiName;
	private String qualifiedName;
	private Set<KID> rids;
	private Set<Long> ids; 
	private Boolean isBasic;

	public void setApiName(String apiName)
	{
		this.apiName = apiName;
	}

	public String getApiName()
	{
		return apiName;
	}
	
	public void addKID(KID id)
	{
		if (this.rids == null)
		{
			this.rids = new HashSet<KID>();
		}
		this.rids.add(id);
	}
	
	public void addId(Long id)
	{
		if (this.ids == null)
		{
			this.ids = new HashSet<Long>();
		}
		this.ids.add(id);
	}

	public void setKIDs(Set<KID> ids)
	{
		this.rids = ids;
	}

	public Set<KID> getKIDs()
	{
		return rids;
	}

	public void setIds(Set<Long> ids)
	{
		this.ids = ids;
	}

	public Set<Long> getIds()
	{
		return ids;
	}

	public String getQualifiedName()
	{
		return qualifiedName;
	}

	public void setQualifiedName(String envQualifiedName)
	{
		this.qualifiedName = envQualifiedName;
	}

	public Boolean getIsBasic()
	{
		return isBasic;
	}

	public void setIsBasic(Boolean isBasic)
	{
		this.isBasic = isBasic;
	}

}