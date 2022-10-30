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

public class ActionPermissionFilter extends PermissionFilter<TypePermission>
{
	private Set<KID> actionIds;

	public void setActionIds(Set<KID> actionIds)
	{
		this.actionIds = actionIds;
	}

	public Set<KID> getActionIds()
	{
		return actionIds;
	}
	
	public void addActionId (KID actionId)
	{
		if (this.actionIds == null)
		{
			this.actionIds = new HashSet<KID>();
		}
		this.actionIds.add(actionId);
	}
}