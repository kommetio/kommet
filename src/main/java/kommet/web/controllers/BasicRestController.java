/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.UserService;
import kommet.auth.oauth2.AccessToken;
import kommet.auth.oauth2.TokenStore;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.rest.RestUtil;
import kommet.uch.UserCascadeHierarchyService;

public abstract class BasicRestController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	TokenStore tokenStore;
	
	@Inject
	UserService userService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	protected RestInitInfo prepareRest(String sEnvId, String accessToken, HttpSession session, HttpServletResponse resp) throws KommetException
	{
		return prepareRest(sEnvId, accessToken, session, resp, true);
	}
	
	/**
	 * Check access token validity. If it is valid, get auth data and env for this token.
	 * @param sEnvId
	 * @param accessToken
	 * @param session
	 * @param resp
	 * @return
	 * @throws KommetException
	 */
	protected RestInitInfo prepareRest(String sEnvId, String accessToken, HttpSession session, HttpServletResponse resp, boolean initOut) throws KommetException
	{	
		PrintWriter out = null;
		
		if (initOut)
		{
			try
			{
				out = resp.getWriter();
			}
			catch (IOException e)
			{
				throw new KommetException("Error writing to page: " + e.getMessage());
			}
		}
		
		EnvData env = null;
		
		if (StringUtils.hasText(sEnvId))
		{
			KID envKID = null;
			try
			{
				envKID = KID.get(sEnvId);
			}
			catch (KIDException e)
			{
				return new RestInitInfo(null, null, out, "Environment ID '" + sEnvId + "' is not a valid Kommet ID", HttpServletResponse.SC_BAD_REQUEST);
			}
			
			env = envService.get(envKID);
		}
		else
		{
			env = envService.getCurrentEnv(session);
		}
		
		if (env == null)
		{
			return new RestInitInfo(null, null, out, "Environment not specified. Environment ID is '" + sEnvId + "', and getting env from session failed as well", HttpServletResponse.SC_BAD_REQUEST);
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		// if no auth data was found in the session and access token was
		// passed with the request, try to get auth data for this token
		if (authData == null && StringUtils.hasText(accessToken))
		{
			// try to get auth data from access_token
			AccessToken apiToken = tokenStore.getToken(accessToken);
			if (apiToken != null)
			{	
				// get user
				// TODO is it not too time consuming to fetch user auth data each time they make an API call?
				authData = userService.getAuthData(userService.getUser(apiToken.getUserId(), env), env);
				authData.initUserPermissions(env);
				authData.initUserCascadeSettings(uchService, env);
			}
		}
		
		if (authData == null)
		{
			return new RestInitInfo(null, null, out, "Access Denied", HttpServletResponse.SC_FORBIDDEN);
		}
		
		return new RestInitInfo(authData, env, out, null, HttpServletResponse.SC_OK);
	}
	
	protected void returnRestError(List<String> errs, PrintWriter out)
	{
		out.write(RestUtil.getRestErrorResponse(errs));
	}
	
	protected void returnRestError(String err, PrintWriter out) throws KommetException
	{
		out.write(RestUtil.getRestErrorResponse(err));
	}
	
	class RestInitInfo
	{
		private AuthData authData;
		private EnvData env;
		private PrintWriter out;
		private String error;
		private int respCode;
		
		public RestInitInfo (AuthData authData, EnvData env, PrintWriter out, String error, int respCode)
		{
			this.authData = authData;
			this.env = env;
			this.out = out;
			this.error = error;
			this.respCode = respCode;
		}
		
		public boolean isSuccess()
		{
			return this.error == null;
		}
		
		public AuthData getAuthData()
		{
			return authData;
		}
		
		public EnvData getEnv()
		{
			return env;
		}
		
		public PrintWriter getOut()
		{
			return out;
		}

		public String getError()
		{
			return error;
		}

		public int getRespCode()
		{
			return respCode;
		}
	}
}