/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.actions;

import kommet.basic.Action;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class ActionFilter extends BasicFilter<Action>
{
	private String url;
	private KID id;
	private KID controllerId;
	private Boolean isSystem;
	private String nameLike;
	private String nameOrUrl;
	private String name;
	
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getUrl()
	{
		return url;
	}
	public void setKID(KID id)
	{
		this.id = id;
	}
	public KID getKID()
	{
		return id;
	}
	public void setControllerId(KID controllerId)
	{
		this.controllerId = controllerId;
	}
	public KID getControllerId()
	{
		return controllerId;
	}
	public void setIsSystem(Boolean isSystem)
	{
		this.isSystem = isSystem;
	}
	public Boolean getIsSystem()
	{
		return isSystem;
	}
	public String getNameLike()
	{
		return nameLike;
	}
	public void setNameLike(String nameLike)
	{
		this.nameLike = nameLike;
	}
	public String getNameOrUrl()
	{
		return nameOrUrl;
	}
	public void setNameOrUrl(String nameOrUrl)
	{
		this.nameOrUrl = nameOrUrl;
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
