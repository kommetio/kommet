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

public class FileRecordAssignmentFilter
{
	private List<KID> fileIds;
	private List<KID> recordIds;
	private List<KID> ids;
	private boolean initFiles = false;

	public void setFileIds(List<KID> fileIds)
	{
		this.fileIds = fileIds;
	}

	public List<KID> getFileIds()
	{
		return fileIds;
	}
	
	public void addFileId (KID id)
	{
		if (this.fileIds == null)
		{
			this.fileIds = new ArrayList<KID>();
		}
		this.fileIds.add(id);
	}

	public void setRecordIds(List<KID> recordIds)
	{
		this.recordIds = recordIds;
	}

	public List<KID> getRecordIds()
	{
		return recordIds;
	}
	
	public void addRecordId (KID id)
	{
		if (this.recordIds == null)
		{
			this.recordIds = new ArrayList<KID>();
		}
		this.recordIds.add(id);
	}

	public void setInitFiles(boolean initFiles)
	{
		this.initFiles = initFiles;
	}

	public boolean isInitFiles()
	{
		return initFiles;
	}

	public void addId (KID id)
	{
		if (this.ids == null)
		{
			this.ids = new ArrayList<KID>();
		}
		this.ids.add(id);
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