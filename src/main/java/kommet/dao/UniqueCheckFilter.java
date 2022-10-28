/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.UniqueCheck;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class UniqueCheckFilter extends BasicFilter<UniqueCheck>
{
	private Set<KID> typeIds;
	private String name;
	private boolean initUserReferenceFields;
	
	public void addTypeId (KID typeId)
	{
		if (this.typeIds == null)
		{
			this.typeIds = new HashSet<KID>();
		}
		this.typeIds.add(typeId);
	}

	public void setTypeIds(Set<KID> typeIds)
	{
		this.typeIds = typeIds;
	}

	public Set<KID> getTypeIds()
	{
		return typeIds;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public boolean isInitUserReferenceFields()
	{
		return initUserReferenceFields;
	}

	public void setInitUserReferenceFields(boolean initUserReferenceFields)
	{
		this.initUserReferenceFields = initUserReferenceFields;
	}
}