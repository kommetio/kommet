/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tagdata;

import java.util.ArrayList;
import java.util.List;

public class Tag
{
	private String name;
	private String description;
	private List<Attribute> attributes = new ArrayList<Attribute>();
	private List<Tag> children = new ArrayList<Tag>();
	
	public Attribute getAttribute(String name)
	{
		for (Attribute a : this.attributes)
		{
			if (a.getName().equals(name))
			{
				return a;
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

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public List<Attribute> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes)
	{
		this.attributes = attributes;
	}
	
	public void addAttribute (Attribute attr)
	{
		this.attributes.add(attr);
	}

	public List<Tag> getChildren()
	{
		return children;
	}

	public void setChildren(List<Tag> children)
	{
		this.children = children;
	}
}
