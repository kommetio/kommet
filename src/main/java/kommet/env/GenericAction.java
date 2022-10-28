/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.env;

import org.apache.commons.lang3.StringUtils;

import kommet.auth.AuthHandler;
import kommet.auth.AuthHandlerConfig;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.koll.compiler.KommetCompiler;


/**
 * Represents an action that is read from annotations on system start-up and stored in env configuration.
 * The actions are called generic because they are not stored in the database.
 * @author Radek Krawiec
 * @since 2/12/2014
 */
public class GenericAction
{
	private String url;
	private String controllerName;
	private String actionMethod;
	private KID controllerClassId;
	private KID viewId;
	private boolean returnsResponseBody;
	private boolean isRest;
	private String authHandler;
	private AuthHandlerConfig authHandlerConfig;
	
	/**
	 * Public actions are accessible to everyone, even unauthenticated users.
	 * They are meant to be used e.g. in public websites.
	 */
	private boolean isPublic;

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	/**
	 * Returns the user-specific controller name
	 * @return
	 */
	public String getControllerName()
	{
		return controllerName;
	}

	public void setControllerName(String userSpecificCntroller)
	{
		this.controllerName = userSpecificCntroller;
	}

	public String getActionMethod()
	{
		return actionMethod;
	}

	public void setActionMethod(String actionMethod)
	{
		this.actionMethod = actionMethod;
	}

	public KID getControllerClassId()
	{
		return controllerClassId;
	}

	public void setControllerClassId(KID controllerClassId)
	{
		this.controllerClassId = controllerClassId;
	}

	public boolean isReturnsResponseBody()
	{
		return returnsResponseBody;
	}

	public void setReturnsResponseBody(boolean returnsResponseBody)
	{
		this.returnsResponseBody = returnsResponseBody;
	}

	public boolean isPublic()
	{
		return isPublic;
	}

	public void setPublic(boolean isPublic)
	{
		this.isPublic = isPublic;
	}

	public boolean isRest()
	{
		return isRest;
	}

	public void setRest(boolean isRest)
	{
		this.isRest = isRest;
	}

	public KID getViewId()
	{
		return viewId;
	}

	public void setViewId(KID viewId)
	{
		this.viewId = viewId;
	}

	public AuthHandler getAuthHandler (KommetCompiler compiler, EnvData env) throws KommetException
	{
		if (StringUtils.isEmpty(this.authHandler))
		{
			return null;
		}
		
		java.lang.Class<?> handlerCls = null;
		
		try
		{
			handlerCls = compiler.getClass(this.authHandler, true, env);
		}
		catch (ClassNotFoundException e)
		{
			throw new KommetException("AuthHandler class " + this.authHandler + " not found by compiler");
		}
		
		try
		{
			return (AuthHandler)handlerCls.newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new KommetException("Error instantiating auth handler");
		}
		
	}

	public void setAuthHandler(String authHandler)
	{
		this.authHandler = authHandler;
	}

	public AuthHandlerConfig getAuthHandlerConfig()
	{
		return authHandlerConfig;
	}

	public void setAuthHandlerConfig(AuthHandlerConfig authHandlerConfig)
	{
		this.authHandlerConfig = authHandlerConfig;
	}
}