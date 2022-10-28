/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.reports;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.ReportType;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class ReportTypeFilter extends BasicFilter<ReportType>
{
	private String name;
	private Set<KID> baseTypeIds;
	private Set<KID> reportTypeIds;

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
	
	public void addBaseTypeId (KID typeId)
	{
		if (this.baseTypeIds == null)
		{
			this.baseTypeIds = new HashSet<KID>();
		}
		this.baseTypeIds.add(typeId);
	}
	
	public void addReportTypeId (KID id)
	{
		if (this.reportTypeIds == null)
		{
			this.reportTypeIds = new HashSet<KID>();
		}
		this.reportTypeIds.add(id);
	}

	public void setBaseTypeIds(Set<KID> typeIds)
	{
		this.baseTypeIds = typeIds;
	}

	public Set<KID> getBaseTypeIds()
	{
		return baseTypeIds;
	}

	public void setReportTypeIds(Set<KID> reportTypeIds)
	{
		this.reportTypeIds = reportTypeIds;
	}

	public Set<KID> getReportTypeIds()
	{
		return reportTypeIds;
	}
}