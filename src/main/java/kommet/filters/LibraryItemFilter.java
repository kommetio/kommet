/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import kommet.basic.LibraryItem;
import kommet.data.KID;

public class LibraryItemFilter extends BasicFilter<LibraryItem>
{
	private String apiName;
	private KID recordId;
	private KID libraryId;

	public String getApiName()
	{
		return apiName;
	}

	public void setApiName(String apiName)
	{
		this.apiName = apiName;
	}

	public KID getRecordId()
	{
		return recordId;
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
	}

	public KID getLibraryId()
	{
		return libraryId;
	}

	public void setLibraryId(KID libraryId)
	{
		this.libraryId = libraryId;
	}
}