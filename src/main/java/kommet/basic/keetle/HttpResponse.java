/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

public class HttpResponse
{
	private String body;
	private String contentType;
	
	public void appendBody(String s)
	{
		if (this.body == null)
		{
			body = s;
		}
		else
		{
			body += s;
		}
	}

	public void setBody(String body)
	{
		this.body = body;
	}

	public String getBody()
	{
		return body;
	}

	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public String getContentType()
	{
		return contentType;
	}
}
