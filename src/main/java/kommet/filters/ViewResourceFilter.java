/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import kommet.basic.WebResource;

public class ViewResourceFilter extends BasicFilter<WebResource>
{
	private String name;
	private String mimeType;
	private String contentLike;
	private boolean fetchContent;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	public String getContentLike()
	{
		return contentLike;
	}

	public void setContentLike(String contentLike)
	{
		this.contentLike = contentLike;
	}

	public boolean isFetchContent()
	{
		return fetchContent;
	}

	public void setFetchContent(boolean fetchContent)
	{
		this.fetchContent = fetchContent;
	}
}