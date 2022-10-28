/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kommet.auth.UserService;
import kommet.auth.oauth2.TokenStore;
import kommet.basic.User;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class OAuth2Controller extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	UserService userService;
	
	@Inject
	TokenStore tokenStore;
	
	/**
	 * Obtain access token in one of the supported grant types.
	 * 
	 * Note that OAuth2 requires this to be a POST request.
	 * @param grantType
	 * @param username
	 * @param password
	 * @param clientId
	 * @param clientSecret
	 * @param envId
	 * @param resp
	 * @throws KommetException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.OAUTH_GET_TOKEN_URL, method = RequestMethod.POST)
	@ResponseBody
	public void getToken(@RequestParam(value = "grant_type", required = false) String grantType,
						@RequestParam(value = "username", required = false) String username,
						@RequestParam(value = "password", required = false) String password,
						@RequestParam(value = "client_id", required = false) String clientId,
						@RequestParam(value = "client_secret", required = false) String clientSecret,
						@RequestParam(value = "env", required = false) String envId,
						HttpServletResponse resp) throws KommetException
	{
		clearMessages();
		PrintWriter out = null;
		
		try
		{
			out = resp.getWriter();
		}
		catch (IOException e)
		{
			throw new KommetException("Could not get output writer for response");
		}
		
		if (!StringUtils.hasText(grantType))
		{
			out.write(getErrorJSON("Grant type not specified"));
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if ("password".equals(grantType.toLowerCase()))
		{
			// handle password grant type scenario
			
			if (!StringUtils.hasText(username))
			{
				addError("Username not specified");
			}
			if (!StringUtils.hasText(password))
			{
				addError("Password not specified");
			}
			if (!StringUtils.hasText(clientId))
			{
				addError("Client ID not specified");
			}
			if (!StringUtils.hasText(clientSecret))
			{
				addError("Client secret not specified");
			}
			if (!StringUtils.hasText(envId))
			{
				addError("Env not specified");
			}
			
			// TODO for now, client id and client secret are not verified at all - change this
			
			if (hasErrorMessages())
			{
				out.write(getErrorJSON(MiscUtils.implode(getErrorMsgs(), ". ")));
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			EnvData env = null;
			
			try
			{
				env = envService.get(KID.get(envId));
			}
			catch (KIDException e)
			{
				out.write(getErrorJSON("Invalid environment ID " + envId));
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			User user = userService.authenticate(username, password, env);
			
			if (user != null)
			{
				// generate and return access token
				String accessToken = MiscUtils.getHash(30);
				String refreshToken = MiscUtils.getHash(30);
				
				// store token in token store
				tokenStore.store(accessToken, refreshToken, 3600, user.getId());
				
				out.write(getAccessTokenJSON(accessToken, refreshToken, 3600));
				return;
			}
			else
			{
				// return error
				out.write(getInvalidAccessTokenJSON("access_denied"));
				resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
		}
		else
		{
			out.write(getErrorJSON("Grant type '" + grantType + "' not supported"));
			return;
		}
	}

	private String getAccessTokenJSON(String accessToken, String refreshToken, int expiresIn)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{ \"access_token\": \"").append(accessToken).append("\", \"expires_in\": ").append(expiresIn);
		sb.append(", \"refresh_token\": \"").append(refreshToken).append("\" }");
		return sb.toString();
	}

	private String getInvalidAccessTokenJSON(String token)
	{
		return "{ \"access_token\": \"" + token + "\" }";
	}
}