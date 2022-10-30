/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.App;
import kommet.data.KID;

public class AppFilter extends BasicFilter<App>
{
	private String name;
	private String type;
	private Set<KID> appIds;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}
	
	public void addAppId(KID id)
	{
		if (this.appIds == null)
		{
			this.appIds = new HashSet<KID>();
		}
		this.appIds.add(id);
	}

	public Set<KID> getAppIds()
	{
		return appIds;
	}

	public void setAppIds(Set<KID> appIds)
	{
		this.appIds = appIds;
	}
}