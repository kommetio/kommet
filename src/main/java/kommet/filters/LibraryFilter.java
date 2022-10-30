/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.Library;
import kommet.data.KID;

public class LibraryFilter extends BasicFilter<Library>
{
	private String name;
	private String nameLike;
	private Boolean isEnabled;
	private boolean initItems;
	private Set<KID> libIds;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getNameLike()
	{
		return nameLike;
	}

	public void setNameLike(String nameLike)
	{
		this.nameLike = nameLike;
	}

	public Boolean getIsEnabled()
	{
		return isEnabled;
	}

	public void setIsEnabled(Boolean isEnabled)
	{
		this.isEnabled = isEnabled;
	}

	public boolean isInitItems()
	{
		return initItems;
	}

	public void setInitItems(boolean initItems)
	{
		this.initItems = initItems;
	}

	public Set<KID> getLibIds()
	{
		return libIds;
	}

	public void setLibIds(Set<KID> libIds)
	{
		this.libIds = libIds;
	}
	
	public void addLibId(KID id)
	{
		if (this.libIds == null)
		{
			this.libIds = new HashSet<KID>();
		}
		this.libIds.add(id);
	}
}