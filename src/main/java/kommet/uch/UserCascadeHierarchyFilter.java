/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.uch;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.UserCascadeHierarchy;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class UserCascadeHierarchyFilter extends BasicFilter<UserCascadeHierarchy>
{
	private Set<KID> uchIds;
	
	public void addUchId (KID id)
	{
		if (this.uchIds == null)
		{
			this.uchIds = new HashSet<KID>();
		}
		this.uchIds.add(id);
	}

	public void setUchIds(Set<KID> uchIds)
	{
		this.uchIds = uchIds;
	}

	public Set<KID> getUchIds()
	{
		return uchIds;
	}
}