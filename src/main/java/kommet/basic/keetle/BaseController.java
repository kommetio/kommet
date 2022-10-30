/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import kommet.auth.AuthData;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.koll.SystemContext;
import kommet.utils.AppConfig;
import kommet.web.actions.ParamCastError;

/**
 * Common ancestor class for all KOLL controllers defined by users.
 * @author Radek Krawiec
 */
public abstract class BaseController
{
	private List<String> errorMsgs = new ArrayList<String>();
	private List<String> actionMsgs = new ArrayList<String>();
	
	private EnvData env;
	private AuthData authData;
	private DataService dataService;
	private SharingService sharingService;
	private Map<String, Object> parameters;
	private AppConfig appConfig;
	private HttpServletRequest request;
	
	private Collection<ParamCastError> paramCastErrors;
	
	/**
  	 * Stores all information about the current action invocation.
  	 */
  	protected PageData pageData;
  	
  	private SystemContext sys;

	public void setEnv(EnvData env)
	{
		this.env = env;
	}

	public EnvData getEnv()
	{
		return env;
	}

	public void setAuthData(AuthData authData)
	{
		this.authData = authData;
	}

	public AuthData getAuthData()
	{
		return authData;
	}
	
	protected List<String> getMessage(String msg)
	{
		List<String> msgs = new ArrayList<String>();
		msgs.add(msg);
		return msgs;
	}
	
	@SuppressWarnings("unchecked")
	public void addError (PageData data, String msg)
	{
		List<String> msgs = (List<String>)data.getValue(PageData.ERROR_MSGS_KEY);
		if (msgs == null)
		{
			msgs = new ArrayList<String>();
		}
		msgs.add(msg);
		data.setValue(PageData.ERROR_MSGS_KEY, msgs);
		
		this.errorMsgs.add(msg);
	}
	
	protected boolean hasErrorMessages()
	{
		return !this.errorMsgs.isEmpty();
	}
	
	protected void clearMessages()
	{
		this.errorMsgs = new ArrayList<String>();
		this.actionMsgs = new ArrayList<String>();
	}
	
	@SuppressWarnings("unchecked")
	public void addActionMessage (PageData data, String msg)
	{
		List<String> msgs = (List<String>)data.getValue(PageData.ACTION_MSGS_KEY);
		if (msgs == null)
		{
			msgs = new ArrayList<String>();
		}
		msgs.add(msg);
		data.setValue(PageData.ACTION_MSGS_KEY, msgs);
		
		this.actionMsgs.add(msg);
	}
	
	public List<String> getActionMsgs()
	{
		return actionMsgs;
	}
	
	public List<String> getErrorMsgs()
	{
		return errorMsgs;
	}

	public void setDataService(DataService dataService)
	{
		this.dataService = dataService;
	}

	public DataService getDataService()
	{
		return dataService;
	}

	public void setParameters(Map<String, Object> parameters)
	{
		this.parameters = parameters;
	}

	protected Map<String, Object> getParameters()
	{
		return parameters;
	}
	
	/**
	 * Returns a parameter with the given name from the request.
	 * 
	 * If exactly one parameter with the given name exists and is of string type, it is returned. If more
	 * than one parameters exist with this name, an exception is thrown. If the parameter exists but is of
	 * different type than string, an exception is thrown as well.
	 * 
	 * @param name
	 * @return
	 * @throws KommetException
	 */
	protected String getParameter (String name) throws KommetException
	{
		String[] values = (String[])parameters.get(name);
		
		if (values == null)
		{
			return null;
		}
		else
		{
			if (values.length == 1)
			{
				return values[0];
			}
			else
			{
				throw new KommetException("More than one parameter found with name '" + name + "'");
			}
		}
	}

	public void setPageData(PageData pageData)
	{
		this.pageData = pageData;
	}

	public PageData getPageData()
	{
		return pageData;
	}
	
	public void setSystemContext(SystemContext sys)
	{
		this.sys = sys;
	}

	protected SystemContext getSys()
	{
		return sys;
	}

	public void setSharingService(SharingService sharingService)
	{
		this.sharingService = sharingService;
	}

	public SharingService getSharingService()
	{
		return sharingService;
	}

	/**
	 * Return errors that occurred while casting action parameters.
	 * @return
	 */
	protected Collection<ParamCastError> getParamCastErrors()
	{
		return paramCastErrors;
	}
	
	public void setParamCastErrors(Collection<ParamCastError> errors)
	{
		this.paramCastErrors = errors;
	}

	public AppConfig getAppConfig()
	{
		return appConfig;
	}

	public void setAppConfig(AppConfig appConfig)
	{
		this.appConfig = appConfig;
	}

	public HttpServletRequest getRequest()
	{
		return request;
	}

	public void setRequest(HttpServletRequest request)
	{
		this.request = request;
	}
	
	public String getContextPath()
	{
		return this.request.getContextPath();
	}
}
