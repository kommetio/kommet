/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import kommet.basic.LoginHistory;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class LoginHistoryFilter extends BasicFilter<LoginHistory>
{
	private Set<KID> userIds;
	private Set<KID> loginHistoryIds;
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
	
	public void addLoginHistoryId(KID id)
	{
		if (this.loginHistoryIds == null)
		{
			this.loginHistoryIds = new HashSet<KID>();
		}
		this.loginHistoryIds.add(id);
	}

	public void setLoginHistoryIds(Set<KID> loginHistoryIds)
	{
		this.loginHistoryIds = loginHistoryIds;
	}

	public Set<KID> getLoginHistoryIds()
	{
		return loginHistoryIds;
	}

	public void setUserIds(Set<KID> userIds)
	{
		this.userIds = userIds;
	}

	public Set<KID> getUserIds()
	{
		return userIds;
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
