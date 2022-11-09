/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.http;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

@Service
public class HttpService
{
	private final String USER_AGENT = "Kommet API";
	
	private boolean isDisableCookies = false;
	
	private HttpClient getHttpClient()
	{
		HttpClientBuilder builder = HttpClientBuilder.create();
		
		if (isDisableCookies)
		{
			builder.disableCookieManagement();
		}
		
		return builder.build();
	}
	
	/**
	 * Download a file from the given URL and store in the file
	 * @param url
	 * @param file
	 * @return
	 * @throws MalformedURLException
	 * @throws HttpException
	 */
	public File download (String url, File file) throws MalformedURLException, HttpException
	{
		try
		{	
			FileUtils.copyURLToFile(new URL(url), file);
			return file;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new HttpException("Error downloading file from URL " + url + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * Sends a GET HTTP request
	 * @param req
	 * @return
	 * @throws HttpException
	 */
	public HttpResponse get (HttpRequest req) throws HttpException
	{
		HttpClient client = getHttpClient();
		
		URL url;
		try
		{
			url = new URL(req.getUrl());
		}
		catch (MalformedURLException e)
		{
			throw new HttpException("Malformed URL " + req.getUrl(), e);
		}
		
		URIBuilder builder = new URIBuilder();
		builder.setScheme(url.getProtocol()).setHost(url.getHost()).setPath(url.getPath());
	
		for (String param : req.getParams().keySet())
		{
			builder.setParameter(param, req.getParams().get(param));
		}
		
		URI uri;
		try
		{
			uri = builder.build();
		}
		catch (URISyntaxException e)
		{
			throw new HttpException("URI syntax error for URL " + req.getUrl(), e);
		}
		
		HttpGet get = new HttpGet(uri);
		get.setHeader("User-Agent", USER_AGENT);
		get = addHeaders(get, req);

		return sendRequest(client, get);
	}
	
	public HttpResponse post (HttpRequest req) throws HttpException
	{
		return post(req, null);
	}
	
	/**
	 * Sends a POST HTTP request
	 * @param req
	 * @return
	 * @throws HttpException
	 */
	public HttpResponse post (HttpRequest req, String encoding) throws HttpException
	{
		HttpClient client = getHttpClient();
		HttpPost post = new HttpPost(req.getUrl());
		
		// add header
		post.setHeader("User-Agent", USER_AGENT);
		post = addHeaders(post, req);

		// add params
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		
		for (String paramName : req.getParams().keySet())
		{
			urlParameters.add(new BasicNameValuePair(paramName, req.getParams().get(paramName)));
		}
		
		try
		{
			if (encoding != null)
			{
				post.setEntity(new UrlEncodedFormEntity(urlParameters, encoding));
			}
			else
			{
				post.setEntity(new UrlEncodedFormEntity(urlParameters));
			}
		}
		catch (UnsupportedEncodingException e)
		{
			throw new HttpException("Error encoding HTTP parameters", e);
		}
		
		if (req.getBody() != null)
		{
	        try
			{
	        	if (encoding != null)
	        	{
	        		post.setEntity(new StringEntity(req.getBody(), encoding));
	        	}
	        	else
	        	{
	        		post.setEntity(new StringEntity(req.getBody()));
	        	}
			}
			catch (UnsupportedEncodingException e)
			{
				throw new HttpException("Unsupported encoding while setting POST body string entity", e);
			}
		}

		return sendRequest(client, post);
	}
	
	/**
	 * Sends a PUT HTTP request
	 * @param req
	 * @return
	 * @throws HttpException
	 */
	public HttpResponse put (HttpRequest req) throws HttpException
	{
		HttpClient client = getHttpClient();
		HttpPut put = new HttpPut(req.getUrl());
		
		// add header
		put.setHeader("User-Agent", USER_AGENT);
		put = addHeaders(put, req);

		// add params
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		
		for (String paramName : req.getParams().keySet())
		{
			urlParameters.add(new BasicNameValuePair(paramName, req.getParams().get(paramName)));
		}
		
		try
		{
			put.setEntity(new UrlEncodedFormEntity(urlParameters));
		}
		catch (UnsupportedEncodingException e)
		{
			throw new HttpException("Error encoding HTTP parameters", e);
		}
		
		if (req.getBody() != null)
		{
	        try
			{
				put.setEntity(new StringEntity(req.getBody()));
			}
			catch (UnsupportedEncodingException e)
			{
				throw new HttpException("Unsupported encoding while setting PUT body string entity", e);
			}
		}

		return sendRequest(client, put);
	}

	private HttpResponse sendRequest(HttpClient client, HttpUriRequest req) throws HttpException
	{
		org.apache.http.HttpResponse response = null;
		try
		{
			response = client.execute(req);
		}
		catch (ClientProtocolException e )
		{
			throw new HttpException("Error sending HTTP request", e);
		}
		catch (IOException e)
		{
			throw new HttpException("Error sending HTTP request", e);
		}
		
		HttpResponse resp = new HttpResponse();
		try
		{
			resp.setBody(EntityUtils.toString(response.getEntity(), "UTF-8"));
		}
		catch (ParseException e)
		{
			throw new HttpException("Error reading HTTP response body", e);
		}
		catch (IOException e)
		{
			throw new HttpException("Error reading HTTP response body", e);
		}
		
		resp.setStatusCode(response.getStatusLine().getStatusCode());
		return resp;
	}

	/**
	 * Rewrites headers from km.request to original apache request
	 * @param httpReq
	 * @param req
	 * @return
	 */
	private static <T extends AbstractHttpMessage> T addHeaders(T httpReq, HttpRequest req)
	{
		for (String header : req.getHeaders().keySet())
		{
			httpReq.setHeader(header, req.getHeaders().get(header));
		}
		
		return httpReq;
	}
	
	public void setDisableCookies (boolean val)
	{
		this.isDisableCookies = val;
	}
}