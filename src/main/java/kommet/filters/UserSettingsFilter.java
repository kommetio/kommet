/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.data.KID;

public class UserSettingsFilter
{
	private Set<KID> userIds;
	private Set<KID> profileIds;
	private boolean userOrProfile;
	
	public void addUserId (KID id)
	{
		if (this.userIds == null)
		{
			this.userIds = new HashSet<KID>();
		}
		this.userIds.add(id);
	}
	
	public void addProfileId (KID id)
	{
		if (this.profileIds == null)
		{
			this.profileIds = new HashSet<KID>();
		}
		this.profileIds.add(id);
	}

	public void setUserIds(Set<KID> userIds)
	{
		this.userIds = userIds;
	}

	public Set<KID> getUserIds()
	{
		return userIds;
	}

	public void setProfileIds(Set<KID> profileIds)
	{
		this.profileIds = profileIds;
	}

	public Set<KID> getProfileIds()
	{
		return profileIds;
	}

	public void setUserOrProfile(boolean userOrProfile)
	{
		this.userOrProfile = userOrProfile;
	}

	public boolean isUserOrProfile()
	{
		return userOrProfile;
	}
}