/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.http;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.junit.Test;

import kommet.http.HttpException;
import kommet.http.HttpRequest;
import kommet.http.HttpResponse;
import kommet.http.HttpService;
import kommet.tests.BaseUnitTest;

public class HttpTest extends BaseUnitTest
{
	@Inject
	HttpService http;
	
	@Test
	public void testSimplePost() throws HttpException
	{
		HttpRequest req = new HttpRequest();
		req.setBody("hello");
		req.addParam("user", "km-test");
		req.setUrl("http://kommet.io");
		
		HttpResponse resp = http.post(req);
		assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, resp.getStatusCode());
		
		req = new HttpRequest();
		req.setBody("hello");
		req.addParam("user", "km-test");
		req.setUrl("http://kommet.io");
		
		resp = http.get(req);
		assertEquals(HttpStatus.SC_OK, resp.getStatusCode());
	}
}
