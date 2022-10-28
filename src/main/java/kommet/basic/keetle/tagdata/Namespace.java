/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tagdata;

import java.util.ArrayList;
import java.util.List;

public class Namespace
{
	private String name;
	private List<Tag> tags = new ArrayList<Tag>();
	
	public Tag getTagByName(String name)
	{
		for (Tag tag : this.tags)
		{
			if (tag.getName().equals(name))
			{
				return tag;
			}
		}
		
		return null;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public List<Tag> getTags()
	{
		return tags;
	}

	public void setTags(List<Tag> tags)
	{
		this.tags = tags;
	}
}
