/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.validationrules;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.ValidationRule;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class ValidationRuleFilter extends BasicFilter<ValidationRule>
{
	private Set<KID> ruleIds;
	private Set<KID> typeIds;
	private Boolean isActive;
	private Set<String> errorMessageLabels;
	private String name;
	
	public void addRuleId(KID id)
	{
		if (this.ruleIds == null)
		{
			this.ruleIds = new HashSet<KID>();
		}
		this.ruleIds.add(id);
	}
	
	public void addErrorMessageLabel(String label)
	{
		if (this.errorMessageLabels == null)
		{
			this.errorMessageLabels = new HashSet<String>();
		}
		this.errorMessageLabels.add(label);
	}
	
	public void addTypeId(KID id)
	{
		if (this.typeIds == null)
		{
			this.typeIds = new HashSet<KID>();
		}
		this.typeIds.add(id);
	}

	public Set<KID> getTypeIds()
	{
		return typeIds;
	}

	public void setTypeIds(Set<KID> typeIds)
	{
		this.typeIds = typeIds;
	}

	public Set<KID> getRuleIds()
	{
		return ruleIds;
	}

	public void setIds(Set<KID> ruleIds)
	{
		this.ruleIds = ruleIds;
	}

	public void setIsActive(Boolean isActive)
	{
		this.isActive = isActive;
	}

	public Boolean getIsActive()
	{
		return isActive;
	}

	public void setErrorMessageLabels(Set<String> errorMessageLabels)
	{
		this.errorMessageLabels = errorMessageLabels;
	}

	public Set<String> getErrorMessageLabels()
	{
		return errorMessageLabels;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
}