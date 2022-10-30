/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.errorlog;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import kommet.basic.ErrorLog;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class ErrorLogFilter extends BasicFilter<ErrorLog>
{
	private Set<KID> userIds;
	private Set<KID> errorLogIds;
	private Set<String> severities;
	private Date dateFrom;
	private Date dateTo;
	
	public void addUserId(KID id)
	{
		if (this.userIds == null)
		{
			this.userIds = new HashSet<KID>();
		}
		this.userIds.add(id);
	}
	
	public void addErrorLogId(KID id)
	{
		if (this.errorLogIds == null)
		{
			this.errorLogIds = new HashSet<KID>();
		}
		this.errorLogIds.add(id);
	}

	public void setErrorLogIds(Set<KID> ids)
	{
		this.errorLogIds = ids;
	}

	public Set<KID> getErrorLogIds()
	{
		return errorLogIds;
	}

	public void setUserIds(Set<KID> userIds)
	{
		this.userIds = userIds;
	}

	public Set<KID> getUserIds()
	{
		return userIds;
	}

	public void addSeverity(String sev)
	{
		if (this.severities == null)
		{
			this.severities = new HashSet<String>();
		}
		this.severities.add(sev);
	}
	
	public void setSeverities(Set<String> severities)
	{
		this.severities = severities;
	}

	public Set<String> getSeverities()
	{
		return severities;
	}

	public void setDateFrom(Date dateFrom)
	{
		this.dateFrom = dateFrom;
	}

	public Date getDateFrom()
	{
		return dateFrom;
	}

	public void setDateTo(Date dateTo)
	{
		this.dateTo = dateTo;
	}

	public Date getDateTo()
	{
		return dateTo;
	}
}