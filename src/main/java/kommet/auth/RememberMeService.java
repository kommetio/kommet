/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import kommet.basic.User;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewService;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.UserFilter;
import kommet.i18n.InternationalizationService;
import kommet.services.ViewResourceService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.web.controllers.LoginController;

@Service
public class RememberMeService
{
	private static final String COOKIE_NAME = "kommet-remember-me-cookie";
	
	@Inject
	EnvService envService;

	@Inject
	PermissionService permissionService;

	@Inject
	ViewService viewService;

	@Inject
	AppConfig appConfig;

	@Inject
	LayoutService layoutService;

	@Inject
	UserService userService;

	@Inject
	LoginHistoryService loginHistoryService;

	@Inject
	InternationalizationService i18n;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	public AuthData read (HttpServletRequest req, UserService userService, EnvData env) throws KommetException
	{
		Cookie cookie = WebUtils.getCookie(req, COOKIE_NAME);
		
		if (cookie != null)
		{
			String accessToken = cookie.getValue();
			
			// find user with the access token
			UserFilter filter = new UserFilter();
			filter.setRememberMeToken(accessToken);
			List<User> users = userService.get(filter, env);
			
			if (users.size() == 1)
			{
				// automatically log in this user
				LoginController.setLogInState(users.get(0), null, req, i18n, users.get(0).getLocale(), loginHistoryService, uchService, userService, appConfig, viewService, layoutService, viewResourceService, env);
				
				AuthData authData = AuthUtil.getAuthData(req.getSession());
				
				if (authData == null)
				{
					throw new KommetException("AuthData for the logged in user should have been stored in the session after calling setLogInState()");
				}
				
				return authData;
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	public Cookie createCookie (User user, int days, EnvData env) throws KommetException
	{
		String value = MiscUtils.getHash(100);
		
		// save token in db
		user.setRememberMeToken(value);
		userService.save(user, AuthData.getRootAuthData(env), env);
		
		Cookie cookie = new Cookie(COOKIE_NAME, value);
		cookie.setPath("/");
		cookie.setMaxAge(days * 24 * 60 * 60); // set expiry time in seconds
		return cookie;
	}

	/**
	 * Forget the remembered user by removing their cookie.
	 * @param req
	 * @param resp
	 */
	public void forget(HttpServletRequest req, HttpServletResponse resp)
	{
		Cookie cookie = WebUtils.getCookie(req, COOKIE_NAME);
		
		if (cookie != null)
		{
			// cookies are removed by setting their expiry dates to now
			cookie.setMaxAge(0);
			cookie.setPath("/");
			resp.addCookie(cookie);
		}
	}
}
