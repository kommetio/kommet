/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.permissions;

import java.util.HashSet;
import java.util.Set;

import kommet.data.KID;
import kommet.filters.BasicFilter;

public class PermissionFilter<T> extends BasicFilter<T>
{
	private Set<KID> profileIds;
	
	public void addProfileId(KID profileId)
	{
		if (this.profileIds == null)
		{
			this.profileIds = new HashSet<KID>();
		}
		this.profileIds.add(profileId);
	}

	public void setProfileIds(Set<KID> profileIds)
	{
		this.profileIds = profileIds;
	}

	public Set<KID> getProfileIds()
	{
		return profileIds;
	}
}