/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.AppUrl;
import kommet.data.KID;

public class AppUrlFilter extends BasicFilter<AppUrl>
{
	private Set<String> urls;
	private Set<KID> appIds;
	private Set<KID> appUrlIds;
	
	public void addUrl (String url)
	{
		if (this.urls == null)
		{
			this.urls = new HashSet<String>();
		}
		this.urls.add(url);
	}
	
	public void addAppId (KID id)
	{
		if (this.appIds == null)
		{
			this.appIds = new HashSet<KID>();
		}
		this.appIds.add(id);
	}

	public Set<String> getUrls()
	{
		return urls;
	}

	public void setUrls(Set<String> urls)
	{
		this.urls = urls;
	}

	public Set<KID> getAppIds()
	{
		return appIds;
	}

	public void setAppIds(Set<KID> appIds)
	{
		this.appIds = appIds;
	}
	
	public void addAppUrlId (KID id)
	{
		if (this.appUrlIds == null)
		{
			this.appUrlIds = new HashSet<KID>();
		}
		this.appUrlIds.add(id);
	}

	public Set<KID> getAppUrlIds()
	{
		return appUrlIds;
	}

	public void setAppUrlIds(Set<KID> appUrlIds)
	{
		this.appUrlIds = appUrlIds;
	}
}