/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.SharingRule;
import kommet.data.KID;

public class SharingRuleFilter extends BasicFilter<SharingRule>
{
	private String name;
	private String method;
	private Set<KID> ruleIds;
	private Set<KID> fileIds;
	private Set<KID> referencedTypes;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getMethod()
	{
		return method;
	}

	public void setMethod(String method)
	{
		this.method = method;
	}

	public Set<KID> getRuleIds()
	{
		return ruleIds;
	}

	public void setRuleIds(Set<KID> ruleIds)
	{
		this.ruleIds = ruleIds;
	}
	
	public void addRuleId(KID id)
	{
		if (this.ruleIds == null)
		{
			this.ruleIds = new HashSet<KID>();
		}
		this.ruleIds.add(id);
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

	public Set<KID> getReferencedTypes()
	{
		return referencedTypes;
	}

	public void setReferencedTypes(Set<KID> referencedTypes)
	{
		this.referencedTypes = referencedTypes;
	}
	
	public void addReferencedType(KID id)
	{
		if (this.referencedTypes == null)
		{
			this.referencedTypes = new HashSet<KID>();
		}
		this.referencedTypes.add(id);
	}
}