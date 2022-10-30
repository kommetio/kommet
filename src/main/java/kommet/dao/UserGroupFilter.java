/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.UserGroup;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class UserGroupFilter extends BasicFilter<UserGroup>
{
	private String name;
	private Set<KID> userGroupIds;
	private boolean initSubgroups;
	private boolean initUsers;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	public void addUserGroupId (KID id)
	{
		if (this.userGroupIds == null)
		{
			this.userGroupIds = new HashSet<KID>();
		}
		this.userGroupIds.add(id);
	}

	public Set<KID> getUserGroupIds()
	{
		return userGroupIds;
	}

	public void setUserGroupIds(Set<KID> userGroupIds)
	{
		this.userGroupIds = userGroupIds;
	}

	public boolean isInitSubgroups()
	{
		return initSubgroups;
	}

	public void setInitSubgroups(boolean initSubgroups)
	{
		this.initSubgroups = initSubgroups;
	}

	public boolean isInitUsers()
	{
		return initUsers;
	}

	public void setInitUsers(boolean initUsers)
	{
		this.initUsers = initUsers;
	}
}