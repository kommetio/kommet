/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.ide;

import java.util.HashMap;
import java.util.Map;

import kommet.data.KID;

public class IDEInfo
{
	// list of currently open files, mapped by their KID
	private Map<KID, IDEFile> files;
	private KID currentFileId;
	
	public IDEInfo()
	{
		this.files = new HashMap<KID, IDEFile>();
	}

	public IDEFile getFile(KID id)
	{
		return files.get(id);
	}

	public void addFile(IDEFile file)
	{
		if (!files.containsKey(file.getId()))
		{
			this.files.put(file.getId(), file);
		}
	}

	public Map<KID, IDEFile> getFiles()
	{
		return this.files;
	}

	public void reAddFile(IDEFile file)
	{
		this.files.put(file.getId(), file);
	}

	public void setCurrentFileId(KID currentFileId)
	{
		this.currentFileId = currentFileId;
	}

	public IDEFile getCurrentFile()
	{
		return this.currentFileId != null ? this.files.get(this.currentFileId) : null;
	}

	public void removeFile(KID itemId)
	{
		if (this.files != null)
		{
			this.files.remove(itemId);
		}
	}
}