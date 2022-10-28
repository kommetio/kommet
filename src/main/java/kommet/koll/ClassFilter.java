/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.RecordAccessType;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class ClassFilter extends BasicFilter<kommet.basic.Class>
{
	private Set<KID> ids;
	private String simpleName;
	private String nameLike;
	private String contentLike;
	private String qualifiedName;
	private Boolean systemFile;
	private RecordAccessType accessType;
	
	public void addId(KID id)
	{
		if (this.ids == null)
		{
			this.ids = new HashSet<KID>();
		}
		this.ids.add(id);
	}
	
	public void setKIDs(Set<KID> ids)
	{
		this.ids = ids;
	}
	public Set<KID> getKIDs()
	{
		return ids;
	}
	public void setSimpleName(String name)
	{
		this.simpleName = name;
	}
	public String getSimpleName()
	{
		return simpleName;
	}

	public void setQualifiedName(String qualifiedName)
	{
		this.qualifiedName = qualifiedName;
	}

	public String getQualifiedName()
	{
		return qualifiedName;
	}

	public void setSystemFile(Boolean systemFile)
	{
		this.systemFile = systemFile;
	}

	public Boolean getSystemFile()
	{
		return systemFile;
	}

	public void setNameLike(String nameLike)
	{
		this.nameLike = nameLike;
	}

	public String getNameLike()
	{
		return nameLike;
	}

	public void setContentLike(String contentLike)
	{
		this.contentLike = contentLike;
	}

	public String getContentLike()
	{
		return contentLike;
	}

	public RecordAccessType getAccessType()
	{
		return accessType;
	}

	public void setAccessType(RecordAccessType accessType)
	{
		this.accessType = accessType;
	}
}