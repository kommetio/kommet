/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.BusinessProcess;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class BusinessProcessFilter extends BasicFilter<BusinessProcess>
{
	private String name;
	private Set<KID> processIds;
	private Boolean isCallable;
	private boolean initSubprocesses;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Set<KID> getProcessIds()
	{
		return processIds;
	}

	public void setProcessIds(Set<KID> processIds)
	{
		this.processIds = processIds;
	}
	
	public void addProcessId(KID id)
	{
		if (this.processIds == null)
		{
			this.processIds = new HashSet<KID>();
		}
		this.processIds.add(id);
	}

	public Boolean getIsCallable()
	{
		return isCallable;
	}

	public void setIsCallable(Boolean isCallable)
	{
		this.isCallable = isCallable;
	}

	public boolean isInitSubprocesses()
	{
		return initSubprocesses;
	}

	public void setInitSubprocesses(boolean initSubprocesses)
	{
		this.initSubprocesses = initSubprocesses;
	}
}