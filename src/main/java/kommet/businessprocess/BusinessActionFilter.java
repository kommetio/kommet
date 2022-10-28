/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.BusinessAction;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class BusinessActionFilter extends BasicFilter<BusinessAction>
{
	private String name;
	private String type;
	private Set<KID> fileIds;
	private Set<KID> actionIds;
	private boolean initParams;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	public Set<KID> getFileIds()
	{
		return fileIds;
	}

	public void setFileIds(Set<KID> fileIds)
	{
		this.fileIds = fileIds;
	}
	
	public void addFileId(KID id)
	{
		if (this.fileIds == null)
		{
			this.fileIds = new HashSet<KID>();
		}
		this.fileIds.add(id);
	}

	public boolean isInitParams()
	{
		return initParams;
	}

	public void setInitParams(boolean initParams)
	{
		this.initParams = initParams;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public Set<KID> getActionIds()
	{
		return actionIds;
	}

	public void setActionIds(Set<KID> actionIds)
	{
		this.actionIds = actionIds;
	}
	
	public void addActionId(KID id)
	{
		if (this.actionIds == null)
		{
			this.actionIds = new HashSet<KID>();
		}
		this.actionIds.add(id);
	}
}