/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.permissions;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.TypePermission;
import kommet.data.KID;

public class TypePermissionFilter extends PermissionFilter<TypePermission>
{
	private Set<KID> typeIds;

	public void setTypeIds(Set<KID> typeIds)
	{
		this.typeIds = typeIds;
	}

	public Set<KID> getTypeIds()
	{
		return typeIds;
	}
	
	public void addTypeId (KID typeId)
	{
		if (this.typeIds == null)
		{
			this.typeIds = new HashSet<KID>();
		}
		this.typeIds.add(typeId);
	}
}