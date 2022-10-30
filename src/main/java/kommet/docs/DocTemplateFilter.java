/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.docs;

import java.util.ArrayList;
import java.util.List;

import kommet.data.KID;

public class DocTemplateFilter
{
	private String name;
	private List<KID> ids;
	
	public void addId (KID id)
	{
		if (this.ids == null)
		{
			this.ids = new ArrayList<KID>();
		}
		this.ids.add(id);
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setIds(List<KID> ids)
	{
		this.ids = ids;
	}

	public List<KID> getIds()
	{
		return ids;
	}
}