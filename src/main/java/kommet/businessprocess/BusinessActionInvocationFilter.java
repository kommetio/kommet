/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.BusinessActionInvocation;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class BusinessActionInvocationFilter extends BasicFilter<BusinessActionInvocation>
{
	private Set<KID> invokedActionIds;
	private Set<KID> invokedProcessIds;
	private Set<KID> parentProcessIds;
	
	public Set<KID> getParentProcessIds()
	{
		return parentProcessIds;
	}

	public void setParentProcessIds(Set<KID> businessProcessIds)
	{
		this.parentProcessIds = businessProcessIds;
	}
	
	public void addParentProcessId(KID id)
	{
		if (this.parentProcessIds == null)
		{
			this.parentProcessIds = new HashSet<KID>();
		}
		this.parentProcessIds.add(id);
	}
	
	public void addInvokedActionId (KID id)
	{
		if (this.invokedActionIds == null)
		{
			this.invokedActionIds = new HashSet<KID>();
		}
		this.invokedActionIds.add(id);
	}
	
	public void addInvokedProcessId (KID id)
	{
		if (this.invokedProcessIds == null)
		{
			this.invokedProcessIds = new HashSet<KID>();
		}
		this.invokedProcessIds.add(id);
	}

	public Set<KID> getInvokedActionIds()
	{
		return invokedActionIds;
	}

	public void setActionIds(Set<KID> actionIds)
	{
		this.invokedActionIds = actionIds;
	}

	public Set<KID> getInvokedProcessIds()
	{
		return invokedProcessIds;
	}

	public void setInvokedProcessIds(Set<KID> invokedProcessIds)
	{
		this.invokedProcessIds = invokedProcessIds;
	}
}