/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.emailing;

public class Attachment
{
	private String name;
	private String filePath;
	
	public Attachment (String name, String filePath)
	{
		this.name = name;
		this.filePath = filePath;
	}

	public String getName()
	{
		return name;
	}

	public String getFilePath()
	{
		return this.filePath;
	}
}