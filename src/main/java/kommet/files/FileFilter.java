/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.files;

import java.util.ArrayList;
import java.util.List;

import kommet.data.KID;


public class FileFilter
{
	private String name;
	private List<KID> fileIds;
	
	public void addId (KID fileId)
	{
		if (this.fileIds == null)
		{
			this.fileIds = new ArrayList<KID>();
		}
		this.fileIds.add(fileId);
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setFileIds(List<KID> fileIds)
	{
		this.fileIds = fileIds;
	}

	public List<KID> getFileIds()
	{
		return fileIds;
	}
}