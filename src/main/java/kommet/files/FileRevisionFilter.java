/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.files;

import java.util.HashSet;
import java.util.Set;

import kommet.data.KID;

public class FileRevisionFilter
{
	private String name;
	private Integer revisionNumber;
	private Set<KID> fileIds;
	private KID id;

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setRevisionNumber(Integer revisionNumber)
	{
		this.revisionNumber = revisionNumber;
	}

	public Integer getRevisionNumber()
	{
		return revisionNumber;
	}

	public void addFileId (KID fileId)
	{
		if (this.fileIds == null)
		{
			this.fileIds = new HashSet<KID>();
		}
		this.fileIds.add(fileId);
	}

	public void setFileIds(Set<KID> fileIds)
	{
		this.fileIds = fileIds;
	}

	public Set<KID> getFileIds()
	{
		return fileIds;
	}

	public void setId(KID id)
	{
		this.id = id;
	}

	public KID getId()
	{
		return id;
	}
}