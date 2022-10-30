/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import kommet.basic.FieldHistory;
import kommet.data.KID;

public class FieldHistoryFilter extends BasicFilter<FieldHistory>
{
	private Set<KID> fieldIds;
	private Set<KID> recordIds;
	private Set<KID> userIds;
	private Date dateFrom;
	private Date dateTo;

	public void setFieldIds(Set<KID> fieldIds)
	{
		this.fieldIds = fieldIds;
	}

	public Set<KID> getFieldIds()
	{
		return fieldIds;
	}
	
	public void addFieldId(KID fieldId)
	{
		if (this.fieldIds == null)
		{
			this.fieldIds = new HashSet<KID>();
		}
		this.fieldIds.add(fieldId);
	}

	public void setRecordIds(Set<KID> recordIds)
	{
		this.recordIds = recordIds;
	}

	public Set<KID> getRecordIds()
	{
		return recordIds;
	}
	
	public void addRecordId(KID recordId)
	{
		if (this.recordIds == null)
		{
			this.recordIds = new HashSet<KID>();
		}
		this.recordIds.add(recordId);
	}

	public void setUserIds(Set<KID> userIds)
	{
		this.userIds = userIds;
	}

	public Set<KID> getUserIds()
	{
		return userIds;
	}
	
	public void addUserId(KID userId)
	{
		if (this.userIds == null)
		{
			this.userIds = new HashSet<KID>();
		}
		this.userIds.add(userId);
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