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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.LoginHistoryService;
import kommet.auth.LoginState;
import kommet.auth.PermissionService;
import kommet.auth.RememberMeService;
import kommet.auth.UserService;
import kommet.auth.AuthUtil.PrepareAuthDataResult;
import kommet.basic.Profile;
import kommet.basic.User;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewService;
import kommet.config.UserSettingKeys;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.i18n.I18nDictionary;
import kommet.i18n.InternationalizationService;
import kommet.i18n.Locale;
import kommet.koll.compiler.KommetCompiler;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.SystemSettingService;
import kommet.services.ViewResourceService;
import kommet.systemsettings.SystemSettingKey;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.UrlUtil;
import kommet.web.RequestAttributes;

@Controller
public class LoginController extends CommonKommetController
{
	private static final Logger log = LoggerFactory.getLogger(LoginController.class);

	@Inject
	EnvService envService;

	@Inject
	PermissionService permissionService;

	@Inject
	ViewService viewService;

	@Inject
	DataService dataService;

	@Inject
	KommetCompiler compiler;

	@Inject
	AppConfig appConfig;

	@Inject
	LayoutService layoutService;

	@Inject
	UserService userService;

	@Inject
	ErrorLogService errorLogService;

	@Inject
	LoginHistoryService loginHistoryService;

	@Inject
	SystemSettingService settingService;

	@Inject
	InternationalizationService i18n;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	RememberMeService rememberMeService;

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/login", method = RequestMethod.GET)
	public ModelAndView login(
			@RequestParam(value = "error", required = false) Boolean isErrorLogin,
			@RequestParam(value = "showEnv", required = false) String showEnv,
			@RequestParam(value = "url", required = false) String redirectUrl,
			HttpServletRequest req) throws KommetException
	{
		clearMessages();
		ModelAndView mv = new ModelAndView("auth/login");

		if (isErrorLogin != null && isErrorLogin == true)
		{
			addError("Incorrect login or password");
			mv.addObject("errorMsgs", getErrorMsgs());
		}

		// env ID can be either passed as request parameter (when appended to
		// the URL)
		// or as request attribute (when deduced from domain in the request
		// filter)
		String envId = req.getParameter(RequestAttributes.ENV_ID_ATTR_NAME);
		if (!StringUtils.hasText(envId))
		{
			envId = (String)req.getAttribute(RequestAttributes.ENV_ID_ATTR_NAME);
		}

		Locale locale = appConfig.getDefaultLocale();
		
		EnvData env = null;

		if (StringUtils.hasText(envId))
		{
			env = envService.get(KID.get(envId));
			if (env == null)
			{
				mv.addObject("errorMsgs", getMessage("Environment with ID " + envId + " does not exist"));
				return mv;
			}
			else
			{
				mv.addObject("envId", envId);
			}

			locale = getDefaultLocale(env);
		}
		else if (req.getAttribute(RequestAttributes.ENV_ATTR_NAME) != null)
		{
			env = (EnvData)req.getAttribute(RequestAttributes.ENV_ATTR_NAME);
			envId = env.getId().getId();
			mv.addObject("envId", envId);
		}

		// show environment input if explicitly requested, or if environment ID
		// is empty
		mv.addObject("showEnv", "1".equals(showEnv) || !StringUtils.hasText(envId));
		mv.addObject("url", redirectUrl);
		mv.addObject("i18n", i18n.getDictionary(locale));
		mv.addObject("browserMsgTitle", i18n.get(locale, "msg.browser.not.supported.title"));
		mv.addObject("browserMsgText", i18n.get(locale, "msg.browser.not.supported.text"));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/404", method = RequestMethod.GET)
	public ModelAndView pageNotFound(HttpServletRequest req, HttpServletResponse res) throws KommetException
	{
		clearMessages();
		ModelAndView mv = new ModelAndView("auth/404");
		addError("Page does not exist");
		mv.addObject("errorMsgs", getErrorMsgs());
		res.setStatus(HttpServletResponse.SC_NOT_FOUND);
		return mv;
	}

	private Locale getDefaultLocale(EnvData env) throws KommetException
	{
		String setLocale = settingService.getSettingValue(SystemSettingKey.DEFAULT_ENV_LOCALE, env);
		return StringUtils.hasText(setLocale) ? Locale.valueOf(setLocale) : Locale.EN_US;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/auth/logout", method = RequestMethod.POST)
	@ResponseBody
	public void logOut (HttpSession session, HttpServletResponse resp, HttpServletRequest req) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		String loginURL = null;
		
		if (env != null)
		{
			try
			{
				loginURL = uchService.getUserSettingAsString(UserSettingKeys.KM_SYS_LOGIN_URL, AuthUtil.getAuthData(session), AuthData.getRootAuthData(env), env);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new KommetException("Error reading default login page: " + e.getMessage());
			}
		}
		
		AuthUtil.getAuthData(session);
		
		// remove the remember-me cookie
		rememberMeService.forget(req, resp);
		
		// clear auth data - primary or secondary
		AuthUtil.clearAuthData(session);
		
		// if user was logged in using secondary login, they will still be logged in after logging out from the secondary login
		boolean isStillLoggedIn = AuthUtil.getAuthData(session) != null;
		
		try
		{
			PrintWriter out = resp.getWriter();
			resp.setContentType("text/json; charset=UTF-8");
			String respText = "{ \"success\": true, \"url\": \"" + loginURL + "\", \"wasSecondaryLogin\": " + (isStillLoggedIn ? "true" : "false") + " }";
			out.write(respText);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new KommetException(e.getMessage());
		}
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/auth/dologin", method = RequestMethod.POST)
	@ResponseBody
	public void doLogin(
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "password", required = false) String password,
			@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "url", required = false) String url,
			@RequestParam(value = "locale", required = false) String locale,
			@RequestParam(value = "rememberMeDays", required = false) Integer rememberMeDays,
			HttpSession session, HttpServletRequest req, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		resp.setContentType("text/json; charset=UTF-8");
		
		clearMessages();
		
		if (!StringUtils.hasText(locale))
		{
			locale = "EN_US";
		}

		locale = locale.toUpperCase();
		
		I18nDictionary i18nDict = i18n.getDictionary(Locale.valueOf(locale));

		if (!StringUtils.hasText(username))
		{
			addError(i18nDict.get("auth.username.empty"));
		}
		if (!StringUtils.hasText(password))
		{
			addError(i18nDict.get("auth.pwd.empty"));
		}
		if (!StringUtils.hasText(envId))
		{
			addError(i18nDict.get("auth.env.empty"));
		}

		if (hasErrorMessages())
		{
			out.write(RestUtil.getRestErrorResponse(getErrorMsgs()));
			return;
		}

		try
		{
			log.debug("Started getting env " + envId);
			EnvData env = envService.get(KID.get(envId));
			log.debug("Finished getting env " + envId);
			
			User user = userService.authenticate(username.trim(), password, env);
			if (user == null)
			{
				log.debug("[login] Auth error for user \"" + username.trim() + "\"");
				List<String> errMsgs = getMessage(i18n.get(Locale.valueOf(locale), "auth.incorrect.username.or.password"));
				out.write(RestUtil.getRestErrorResponse(errMsgs));
				return;
			}
			else if (!Boolean.TRUE.equals(user.getIsActive()))
			{
				log.debug("[login] Inactive user login attempt \"" + username.trim() + "\"");
				List<String> errMsgs = getMessage(i18n.get(Locale.valueOf(locale), "auth.incorrect.username.or.password"));
				out.write(RestUtil.getRestErrorResponse(errMsgs));
				return;
			}

			log.debug("[login] Auth OK for \"" + username.trim() + "\"");

			LoginState loginState = setLogInState(user, url, req, i18n, locale, loginHistoryService, uchService, userService, appConfig, viewService, layoutService, viewResourceService, env);
			// layoutService.initDefaultLayout(appConfig, env);
			
			if (!loginState.isSuccess())
			{
				out.write(RestUtil.getRestErrorResponse(loginState.getError()));
				return;
			}
			
			if (rememberMeDays != null)
			{
				// add cookie to response
				resp.addCookie(rememberMeService.createCookie(user, rememberMeDays, env));
			}
			
			// return success
			out.write(RestUtil.getRestSuccessDataResponse("{ \"url\": \"" + (loginState.getUrl() != null ? loginState.getUrl() : "") + "\" }"));
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			String errMsg = "[login] Error logging in: " + e.getMessage();
			
			out.write(RestUtil.getRestErrorResponse(errMsg));
			return;
		}
	}

	/**
	 * Performs the actual log in action for a user, once the authentication is successful.
	 * @param user
	 * @param url
	 * @param session
	 * @param i18n
	 * @param locale
	 * @param loginHistoryService
	 * @param uchService
	 * @param userService
	 * @param appConfig
	 * @param viewService
	 * @param layoutService
	 * @param viewResourceService
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static LoginState setLogInState (User user, String url, HttpServletRequest req, InternationalizationService i18n, String locale, LoginHistoryService loginHistoryService, UserCascadeHierarchyService uchService, UserService userService, AppConfig appConfig, ViewService viewService, LayoutService layoutService, ViewResourceService viewResourceService, EnvData env) throws KommetException
	{
		// invalidate the old session and start a new one
		// if session has expired, we might get exception saying "Session already invalidated" below - check for this
		try
		{
			// previously "session.invalidate()"
			req.getSession().invalidate();
		}
		catch (IllegalStateException e)
		{
			if (e.getMessage() != null && e.getMessage().contains("Session already invalidated"))
			{
				// session already expired, so it's OK
			}
			else
			{
				throw new KommetException("Log out error: " + e.getMessage());
			}
		}

		// the old session has been invalidated, so calling HttpServletRequest.getSession() will create and return a new one
		HttpSession newSession = req.getSession();
		PrepareAuthDataResult result = AuthUtil.prepareAuthData(user, newSession, false, userService, i18n, uchService, appConfig, env);
		if (!StringUtils.hasText(url))
		{
			// only use user's URL if no URL has been passed in the request
			url = result.getLandingURL();
		}

		// store information about the login
		loginHistoryService.recordLogin(user.getId(), "Browser", "Success", "0.0.0.0", AuthData.getRootAuthData(env), env);
		
		// check if user has permissions to log in
		boolean canLogIn = uchService.getUserSettingAsBoolean(UserSettingKeys.KM_SYS_CAN_LOGIN, result.getAuthData(), AuthData.getRootAuthData(env), env);
		
		// block log in, unless it is a root user
		if (!canLogIn && !AuthUtil.isRoot(result.getAuthData()))
		{
			log.debug("[login] Log in blocked for \"" + user.getUserName() + "\"");
			
			LoginState loginState = new LoginState(false);
			loginState.setError(i18n.get(Locale.valueOf(locale), "auth.login.blocked"));
			return loginState;
		}

		// TODO we shouldn't init the keetle dir each time a user logs in,
		// only on server startup
		viewService.initKeetleDir(env, false);
		layoutService.initLayoutDir(env, false);
		viewResourceService.initViewResourcesOnDisk(env, false);
		
		LoginState loginState = new LoginState(true);
		loginState.setUrl(url);
		return loginState;
	}

	/**
	 * Method called when user tries to log in onto other user's account using
	 * secondary login.
	 * 
	 * @param sUserId
	 * @param session
	 * @return
	 * @throws KommetException
	 */
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/secondarylogin", method = RequestMethod.POST)
	public ModelAndView secondaryLogin(
			@RequestParam(value = "userId", required = false) String sUserId,
			HttpSession session) throws KommetException
	{
		if (!StringUtils.hasText(sUserId))
		{
			return getErrorPage("ID of the user to log in as is null or empty");
		}

		EnvData env = envService.getCurrentEnv(session);
		
		User user = userService.getUser(KID.get(sUserId), env);

		if (user == null)
		{
			return getErrorPage("User with ID " + sUserId + " not found");
		}

		AuthData currentAuthData = AuthUtil.getAuthData(session);

		// make sure this user has permission to use secondary login
		if (!AuthUtil.canLogin(currentAuthData, user.getId()))
		{
			return getErrorPage("Permission denied");
		}

		PrepareAuthDataResult result = AuthUtil.prepareAuthData(user, session, true, userService, i18n, uchService, appConfig, env);

		if (StringUtils.hasText(result.getLandingURL()))
		{
			// if redirect URL is specified, open it
			return new ModelAndView("redirect:/" + result.getLandingURL());
		}
		else
		{
			// open the base URL
			return new ModelAndView("redirect:/");
		}
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/logout", method = RequestMethod.GET)
	public ModelAndView logOut (@RequestParam(value = "env", required = false) String envId, HttpSession session, HttpServletRequest req, HttpServletResponse resp) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		String loginURL = null;
		
		if (env != null)
		{
			try
			{
				loginURL = uchService.getUserSettingAsString(UserSettingKeys.KM_SYS_LOGIN_URL, AuthUtil.getAuthData(session), AuthData.getRootAuthData(env), env);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new KommetException("Error reading default login page: " + e.getMessage());
			}
		}
		
		System.out.println("Login URL: " + loginURL);
		
		AuthData originalAuthData = AuthUtil.getAuthData(session);
		
		// remove the remember-me cookie
		rememberMeService.forget(req, resp);
		
		// clear auth data - primary or secondary
		AuthUtil.clearAuthData(session);

		// check if user is still logged in - if the user was logged in using secondary login,
		// auth data for primary login may still be present in the session
		AuthData currentAuthData = AuthUtil.getAuthData(session);

		if (currentAuthData == null)
		{
			if (envId == null && originalAuthData.getEnvId() != null)
			{
				envId = originalAuthData.getEnvId().getId();
			}
			
			session.invalidate();
			
			if (!StringUtils.hasText(loginURL))
			{
				// use system default login page
				loginURL = "/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/login";
			}
			else if (!loginURL.startsWith("/"))
			{
				loginURL = "/" + loginURL;
			}
			
			System.out.println("Redirect: " + loginURL);
			
			return new ModelAndView("redirect:" + loginURL +(envId != null ? ("?env=" + envId) : ""));
		}
		else
		{
			// go to profile page
			return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/me");
		}
	}
}