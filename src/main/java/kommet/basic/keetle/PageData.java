/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import kommet.data.KID;
import kommet.env.EnvData;
import kommet.web.rmparams.KmParamNode;

/**
 * This class represents all data returned by a controller method.
 * @author Radek Krawiec
 */
public class PageData
{
	private Map<String, Object> values = new HashMap<String, Object>();
	
	public static final String ACTION_MSGS_KEY = "actionMsgs";
	public static final String ERROR_MSGS_KEY = "errorMsgs";
	public static final String PAGE_DATA_ASSOC_FIELD = "assocField";
	public static final String PAGE_DATA_ASSOC_PARENT = "assocParent";
	
	/**
	 * If set, the action will redirect to this URL instead of its regular view.
	 */
	private String redirectURL;
	
	private String requestURL;
	
	private KmParamNode rmParams;
	
	// overridden view
	private KID viewId;
	
	private HttpResponse httpResponse;
	
	private HttpServletRequest httpRequest;
	
	/**
	 * Overridden layout.
	 */
	private KID layoutId;
	
	/**
	 * The current env. It is used by views to dynamically determine layout disk path.
	 */
	private EnvData env;
	
	public PageData (EnvData env)
	{
		this.env = env;
	}
	
	public Map<String, Object> getValues()
	{
		return values;
	}

	/**
	 * Do not remove this method - it is used by reflection in method "fromActionResult"
	 * @param values
	 */
	public void setValues(Map<String, Object> values)
	{
		this.values = values;
	}

	public void setValue (String key, Object value)
	{
		this.values.put(key, value);
	}
	
	public Object getValue (String key)
	{
		return this.values.get(key);
	}

	public void setRedirectURL(String redirectURL)
	{
		this.redirectURL = redirectURL;
	}

	public String getRedirectURL()
	{
		return redirectURL;
	}

	public void setRmParams(KmParamNode rmParams)
	{
		this.rmParams = rmParams;
	}

	public KmParamNode getRmParams()
	{
		return rmParams;
	}

	public void setLayoutId(KID layoutId)
	{
		this.layoutId = layoutId;
	}

	public KID getLayoutId()
	{
		return layoutId;
	}

	public void setViewId(KID viewId)
	{
		this.viewId = viewId;
	}

	public KID getViewId()
	{
		return viewId;
	}

	public void setRequestURL(String requestURL)
	{
		this.requestURL = requestURL;
	}

	public String getRequestURL()
	{
		return requestURL;
	}

	public void setHttpResponse(HttpResponse httpResponse)
	{
		this.httpResponse = httpResponse;
	}

	public HttpResponse getHttpResponse()
	{
		return httpResponse;
	}

	public EnvData getEnv()
	{
		return env;
	}

	public HttpServletRequest getHttpRequest()
	{
		return httpRequest;
	}

	public void setHttpRequest(HttpServletRequest httpRequest)
	{
		this.httpRequest = httpRequest;
	}
}
