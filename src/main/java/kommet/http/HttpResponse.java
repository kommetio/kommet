/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.http;

public class HttpResponse
{
	private String body;
	private int statusCode;

	public String getBody()
	{
		return body;
	}

	public void setBody(String body)
	{
		this.body = body;
	}

	public int getStatusCode()
	{
		return statusCode;
	}

	public void setStatusCode(int code)
	{
		this.statusCode = code;
	}
}