/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import org.springframework.web.multipart.MultipartFile;

public class UploadItem
{
	private String filename;
	private MultipartFile fileData;

	public String getFilename()
	{
		return filename;
	}

	public void setFilename(String filename)
	{
		this.filename = filename;
	}

	public MultipartFile getFileData()
	{
		return fileData;
	}

	public void setFileData(MultipartFile fileData)
	{
		this.fileData = fileData;
	}
}