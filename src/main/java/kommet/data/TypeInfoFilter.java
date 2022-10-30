/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.HashSet;
import java.util.Set;

public class TypeInfoFilter
{
	private Set<KID> typeIds;
	
	public void addTypeId(KID id)
	{
		if (this.typeIds == null)
		{
			this.typeIds = new HashSet<KID>();
		}
		this.typeIds.add(id);
	}

	public void setTypeIds(Set<KID> typeIds)
	{
		this.typeIds = typeIds;
	}

	public Set<KID> getTypeIds()
	{
		return typeIds;
	}
}