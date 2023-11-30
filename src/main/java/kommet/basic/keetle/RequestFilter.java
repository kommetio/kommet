/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import kommet.auth.AuthData;
import kommet.auth.AuthHandler;
import kommet.auth.AuthUtil;
import kommet.auth.PermissionService;
import kommet.auth.RememberMeService;
import kommet.auth.UserService;
import kommet.auth.AuthUtil.PrepareAuthDataResult;
import kommet.auth.oauth2.AccessToken;
import kommet.auth.oauth2.TokenStore;
import kommet.basic.Action;
import kommet.basic.App;
import kommet.basic.AppUrl;
import kommet.basic.BasicSetupService;
import kommet.basic.RecordProxy;
import kommet.basic.StandardAction;
import kommet.basic.TypeInfo;
import kommet.basic.User;
import kommet.basic.View;
import kommet.basic.actions.ActionService;
import kommet.config.UserSettingKeys;
import kommet.data.DataService;
import kommet.data.DomainMapping;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.TypeInfoService;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.env.GenericAction;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.filters.AppUrlFilter;
import kommet.i18n.InternationalizationService;
import kommet.i18n.Locale;
import kommet.json.JSON;
import kommet.koll.SystemContextFactory;
import kommet.koll.annotations.ResponseBody;
import kommet.koll.annotations.Rest;
import kommet.koll.annotations.ReturnsFile;
import kommet.koll.compiler.KommetCompiler;
import kommet.rest.RestUtil;
import kommet.services.AppService;
import kommet.services.SystemSettingService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;
import kommet.utils.UrlUtil;
import kommet.web.RequestAttributes;
import kommet.web.actions.ActionUtil;
import kommet.web.actions.ParsedURL;
import kommet.web.kmparams.KmParamException;
import kommet.web.kmparams.KmParamUtils;
import kommet.web.kmparams.actions.OverrideLayout;

/**
 * This filter is the main point where action requests are intercepted and executed.
 * @author Radek Krawiec
 * @since 2013
 */
public class RequestFilter extends OncePerRequestFilter 
{
	@Inject
	ActionService actionService;
	
	@Inject
	EnvService envService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	TypeInfoService typeInfoService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	PlatformTransactionManager transactionManager;
	
	@Inject
	SystemContextFactory sysContextFactory;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	TokenStore tokenStore;
	
	@Inject
	UserService userService;
	
	@Inject
	InternationalizationService i18nService;
	
	@Inject
	SystemSettingService sysSettingService;

	@Inject
	ErrorLogService errorLog;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	BasicSetupService setupService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	AppService appService;
	
	@Inject
	RememberMeService rememberMeService;
	
	private static final Logger log = LoggerFactory.getLogger(RequestFilter.class);
	
	private static final String EDIT_URL_SUFFIX = "e";
	private static final String NEW_URL_SUFFIX = "n";
	
	//private static final Pattern URL_PATTERN = Pattern.compile("{\\{([A-z0-9\\-]+)\\}");
	
	/**
	 * List of URLs accessible to everyone without logging in.
	 */
	private static Set<String> openURLs;
	
	static
	{
		openURLs = new HashSet<String>();
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/initenv");
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/login");
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/auth/dologin");
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/restorepassword");
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/dorestorepassword");
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/forgottenpassword");
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/sendpasswordlink");
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/activate");
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/doactivate");
		
		// query REST service is open, and access to data is controlled at the type/record permission level
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_QUERY_DS_URL);
		
		// NOTE: can we safely expose config JS data to everyone? guess there's nothing confidential there, but while generating the file
		// we need to make sure we apply sharings
		// remove the starting slash from the URL
		openURLs.add(UrlUtil.CONFIG_JS_URL.substring(1));
		
		// unauthenticated users can of course request an OAuth token
		openURLs.add(UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.OAUTH_GET_TOKEN_URL);
	}
	
	@Override
	protected void doFilterInternal (HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException
	{
		String requestID = "req-" + (new Random()).nextInt(10000);
		long reqStartTime = System.currentTimeMillis();
		
		String requestedUrl = request.getRequestURI();
		String contextPath = request.getSession().getServletContext().getContextPath();
		
		// if servlet context is not empty, remove it from the URL
		if (StringUtils.hasText(contextPath) && requestedUrl.startsWith(contextPath))
		{
			requestedUrl = requestedUrl.substring(contextPath.length());
		}
		
		boolean isDebug = false;
		
		try
		{
			isDebug = appConfig.isRequestDebug();
		}
		catch (PropertyUtilException e)
		{
			throw new ServletException("Cannot read property 'kommet.request.debug'");
		}
		
		// remove the starting slash from the request URL
		requestedUrl = MiscUtils.trim(requestedUrl, '/');
		
		if (requestedUrl.equals("createmissingtypes"))
		{
			try
			{
				setupService.createMissingTypes(envService.get(KID.get("001hqc2lv4ex3"), true, true, true, false, false, false, false, false, false, false, false, false, false, false));
			}
			catch (KeyPrefixException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (KIDException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (KommetException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// filter is skipped for resources such as images, CSS and Javascript files
		if (requestedUrl.startsWith("resources/"))
		{	
			// skip filter and continue
			chain.doFilter(request, response);
			return;
		}
		
		try
		{
			if (StringUtils.hasText(appConfig.getServerMaintenanceMessage()))
			{
				// redirect to the maintenance page should be done after the check if the
				// requested URL starts with "resource", because resource can be retrieved
				// regardless of this message
				RequestDispatcher maintenanceDispatcher = request.getRequestDispatcher("/maintenancebreak");
				maintenanceDispatcher.forward(request, response);
				return;
			}
		}
		catch (PropertyUtilException e)
		{
			throw new ServletException("Cannot read property 'server maintenance message'");
		}
		
		if (isDebug)
		{
			log.debug("Request [" + requestedUrl + "] [" + requestID + "] [" + (System.currentTimeMillis() - reqStartTime) + "]");
		}
		
		EnvData env = null;
		String sEnvId = null;
		
		String domain = stripServerName(request.getServerName());
		
		if (request.getParameter(RequestAttributes.ENV_ID_ATTR_NAME) != null)
		{
			sEnvId = request.getParameter(RequestAttributes.ENV_ID_ATTR_NAME);
		}
		else
		{
			DomainMapping domainMapping = null;
			// look for domain mappings in the master env
			try
			{
				domainMapping = appService.getDomainMapping(stripServerName(request.getServerName()), envService.getMasterEnv());
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new ServletException("Error getting domain mapping: " + e.getMessage());
			}
			
			if (domainMapping != null)
			{
				log.debug("Domain mapping exists");
				sEnvId = domainMapping.getEnv().getKID().getId();
			}
			else
			{
				log.debug("Domain mapping does not exist");
				KID defaultEnvId;
				try
				{
					defaultEnvId = appConfig.getDefaultEnvId();
				}
				catch (PropertyUtilException e)
				{
					e.printStackTrace();
					throw new ServletException("Error getting default env ID: " + e.getMessage());
				}
				
				log.debug("Using default env " + defaultEnvId);
				
				if (defaultEnvId != null)
				{
					sEnvId = defaultEnvId.getId();
				}
			}
		}
		
		if (sEnvId != null)
		{
			KID envId = null;
			try
			{
				envId = KID.get(sEnvId);
			}
			catch (KIDException e)
			{
				throw new ServletException("Invalid environment ID '" + sEnvId + "'");
			}
			
			try
			{
				env = envService.get(envId);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new ServletException("Error initializing environment: " + e.getMessage());
			}
			
			request.setAttribute(RequestAttributes.ENV_ATTR_NAME, env);
		}
		
		AuthData authData = AuthUtil.getAuthData(request.getSession());
		
		if (authData == null)
		{
			try
			{
				authData = getRestAuthData(request);
			}
			catch (KommetException e)
			{
				// TODO instead of throwing an exception, perhaps return a JSON response
				throw new ServletException("Error authenticating REST user: " + e.getMessage());
			}
		}
		
		// if authdata is null at this point, it means that the user is not authenticated
		// so we will check if he shouldn't be automatically authenticated using the remember-me feature
		if (authData == null || authData.isGuest())
		{
			AuthData newAuthData = handleRememberMe(request, userService, env);
			if (newAuthData != null)
			{
				authData = newAuthData;
			}
		}
		
		if (env != null)
		{
			GenericAction authGenericAction = env.getGenericAction(requestedUrl);
			
			// if AuthHandler has been defined for this generic action, use it to authenticate the user
			if (authGenericAction != null)
			{
				AuthHandler authHandler = null;
				try
				{
					authHandler = authGenericAction.getAuthHandler(compiler, env);
				}
				catch (KommetException e)
				{
					e.printStackTrace();
					throw new ServletException("Error getting auth handler for action " + authGenericAction.getUrl());
				}
				
				if (authHandler != null)
				{
					// Note that the authData returned by the custom authenticator overrides the previous auth data.
					// This means that is the user is logged in using standard auth, but custom auth fails, the overall auth fails
					AuthData customAuthData = handleCustomAuth(authGenericAction, compiler, env, request);
					
					// if user was not logged in with other auth method, or if custom auth overrides other methods,
					// perform the override and use the custom auth identity
					if (authData == null || authGenericAction.getAuthHandlerConfig().isOverrideOtherAuth())
					{
						authData = customAuthData;
					}
				}
			}
		}
		
		if (authData == null)
		{
			// if auth data is null at this stage, this means that the user is not authenticated
			// so we will created a guest auth data object for them
			try
			{
				authData = AuthData.getGuestAuthData(permissionService, uchService, env);
			}
			catch (KommetException e1)
			{
				try
				{
					errorLog.logException(e1, ErrorLogSeverity.ERROR, RequestFilter.class.getName(), 267, null, null, env);
				}
				catch (KommetException e)
				{
					e.printStackTrace();
					throw new ServletException("Error logging error: " + e.getMessage());
				}
				throw new ServletException("Could not obtain guest credentials");
			}

			if (env != null)
			{
				authData.setEnvId(env.getId());
			
				try
				{
					Locale defaultLocale = sysSettingService.getDefaultLocale(env);
					
					// in case locale is not defined in settings, use EN_US
					if (defaultLocale == null)
					{
						defaultLocale = Locale.EN_US;
					}
					
					authData.setI18n(i18nService.getDictionary(defaultLocale));
				}
				catch (KommetException e)
				{
					throw new ServletException("Could not retrieve default locale setting from environment");
				}
				
				// store auth data in the session
				AuthUtil.storePrimaryAuthData(authData, request.getSession());
			}
		}
		else
		{
			// get env from auth data
			try
			{
				env = envService.get(authData.getEnvId());
			}
			catch (KommetException e)
			{
				throw new ServletException("Error getting env with ID " + authData.getEnvId() + ". Nested: " + e.getMessage());
			}
			
			request.setAttribute(RequestAttributes.ENV_ATTR_NAME, env);
		}
		
		App app = null;
		
		if (env != null)
		{
			// store auth data for this thread
			env.addAuthData(authData);
			
			app = getApp(env, domain);
			
			if (app != null)
			{
				request.setAttribute(RequestAttributes.APP, app);
			}
		}
		
		// If URL is empty, redirect to default URL.
		// We did not do it earlier, because only at this stage do we have env and authData information, which we use to determine the default URL.
		if (!StringUtils.hasText(requestedUrl))
		{
			try
			{
				String homeURL = getHomeURL(authData, app, env);
				
				if (StringUtils.hasText(homeURL) && !"/".equals(homeURL))
				{
					// redirect to default URL only if this URL is different than empty, otherwise we'd get an infinite loop
					response.sendRedirect(request.getContextPath() + "/" + homeURL);
				}
			}
			catch (PropertyUtilException e)
			{
				// fail silently and just redirect to user's profile page
				response.sendRedirect(request.getContextPath() + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/me");
			}
			catch (KommetException e)
			{
				env.clearAuthData();
				throw new ServletException("Error getting home URL: " + e.getMessage(), e);
			}
			return;
		}
		
		Action action = null;
		
		if (StringUtils.hasText(requestedUrl) && env != null)
		{
			try
			{
				action = env.getActionForUrl(requestedUrl);
			}
			catch (KommetException e)
			{
				env.clearAuthData();
				throw new ServletException("Error retrieving action for URL " + requestedUrl + ". Nested exception: " + e.getMessage());
			}
		}
		
		// if env was not retrieved from domain name mapping or URL, try to retrieve it from auth data
		if (env == null)
		{
			try
			{
				// log which user invoked the request
				if (isDebug)
				{
					log.debug(requestID + ":" + (authData.isGuest() ? "guest" : authData.getUserId().getId()));
				}
				
				if (authData.getEnvId() != null)
				{
					// retrieve environment
					env = envService.get(authData.getEnvId());
				}
			}
			catch (KommetException e)
			{
				throw new ServletException("Error getting env data. Nested: " + e.getMessage());
			}
		}
		
		// add auth data to the request
		request.setAttribute(RequestAttributes.AUTH_DATA_ATTR_NAME, authData);
		request.setAttribute(RequestAttributes.APP_CONFIG_ATTR_NAME, appConfig);
		
		GenericAction genericAction = null;
		
		// at this point we know that if the user is authenticated, the env will not be null
		// if they are a guest, env will be not null if it was specified in the URL (with the "env" parameter)
		if (env != null)
		{
			genericAction = env.getGenericAction(requestedUrl);
		}
		
		boolean isStandardAction = false;
		
		// check if the user is not logged in and if the requested action is public
		if (authData.isGuest() && !(action != null && Boolean.TRUE.equals(action.getIsPublic())) && !(genericAction != null && genericAction.isPublic()))
		{
			// if auth data is not available, no further action is possible because we don't
			// have information about the environment
			
			if (!isOpenURL(requestedUrl))
			{
				// at this point we know that the user is accessing a resource for which they don't have access
				// whether we redirect them to the app login page or return a REST authentication error will
				// depend on whether this was a REST request, which in turn will be determined solely basing
				// on the accept header. We could also check the URL of the request against the env's
				// generic action URLs, but if the user did not pass env ID in the request, we don't even know
				// the env
				if (request.getHeader("Content-Type") != null && request.getHeader("Content-Type").equals("application/json"))
				{
					// return REST error
					try
					{
						response.getWriter().write(RestUtil.getRestErrorResponse("Permission denied"));
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						return;
					}
					catch (KommetException e)
					{
						throw new ServletException("Error generating error response: " + e.getMessage());
					}
				}
				else if (action == null && genericAction == null && !requestedUrl.startsWith(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/"))
				{
					String notFoundURL = null;
					try
					{
						if (env != null)
						{
							notFoundURL = uchService.getUserSettingAsString(UserSettingKeys.KM_SYS_404_URL, authData, AuthData.getRootAuthData(env), env);
						}
						
						if (StringUtils.hasText(notFoundURL))
						{
							response.sendRedirect(request.getContextPath() + "/" + notFoundURL);
							return;
						}
					}
					catch (KommetException e)
					{
						e.printStackTrace();
						throw new ServletException("Error reading default 404 page: " + e.getMessage());
					}
					
					if (!StringUtils.hasText(notFoundURL))
					{
						// use system default login page
						notFoundURL = "/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/404";
					}
					
					// redirect to the 404 page
					// forward the request to the KTL-JSP file
					RequestDispatcher loginDispatcher = request.getRequestDispatcher("/" + notFoundURL);
					loginDispatcher.forward(request, response);
				}
				else
				{
					String loginURL = null;
					try
					{
						if (env != null)
						{
							loginURL = uchService.getUserSettingAsString(UserSettingKeys.KM_SYS_LOGIN_URL, authData, AuthData.getRootAuthData(env), env);
						}
						
						if (StringUtils.hasText(loginURL))
						{
							response.sendRedirect(request.getContextPath() + "/" + loginURL + "?url=" + URLEncoder.encode(requestedUrl, "UTF-8"));
							return;
						}
					}
					catch (KommetException e)
					{
						e.printStackTrace();
						throw new ServletException("Error reading default login page: " + e.getMessage());
					}
					
					if (!StringUtils.hasText(loginURL))
					{
						// use system default login page
						loginURL = "/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/login";
					}
					
					// redirect to the login page
					// forward the request to the KTL-JSP file
					RequestDispatcher loginDispatcher = request.getRequestDispatcher("/" + loginURL + "?url=" + URLEncoder.encode(requestedUrl, "UTF-8"));
					loginDispatcher.forward(request, response);
				}
			}
			else
			{	
				// user is accessing a resource for which no permissions are needed, so we simply continue
				chain.doFilter(request, response);
			}
			return;
		}
		
		try
		{
			// if action for this URL has not been found, it means that it can be a standard URL or a generic action
			if (action == null && genericAction == null)
			{	
				// if the requested URL was not a custom action, perhaps it is a record ID
				if (requestedUrl.length() == KID.LENGTH)
				{
					Type type = env.getType(KeyPrefix.get(requestedUrl.substring(0, 3)));
					
					// If type is found for the given ID, it means that it was a valid record ID.
					// If type is not found, it may have been some other request with length equals
					// to the length of the ID (e.g. "comments/save"), so we will just continue and
					// let it be interpreted this way.
					if (type != null)
					{
						try
						{
							// TODO action is queried each time it is referenced, which is suboptimal
							StandardAction stdAction = actionService.getStandardDetailsAction(type, authData.getProfile(), env);
							
							if (stdAction == null)
							{
								// use default action for this type and action type
								// TODO note that below are two time consuming operations - two queries
								// should we optimize it as this will be called very often?
								TypeInfo typeInfo = typeInfoService.getForType(type.getKID(), env);
								action = actionService.getAction(typeInfo.getDefaultDetailsAction().getId(), env);
							}
							else
							{
								// get action from env because there they are stored with their controller data initialized
								action = env.getActionForUrl(stdAction.getAction().getUrl());
							}
							
							isStandardAction = true;
							applyOverriddenView(action, type, UserSettingKeys.KM_SYS_DEFAULT_TYPE_DETAILS_VIEW, authData, env);
						}
						catch (KommetException e)
						{
							throw new ServletException("Error getting standard details action: " + e.getMessage(), e);
						}
						
						// add the ID of the requested object to the request
						request.setAttribute("id", requestedUrl);
					}
				}
				// if it's an edit URL, e.g. "01200000024W/e"
				else if ((requestedUrl.length() == (KID.LENGTH) + EDIT_URL_SUFFIX.length() + 1) && requestedUrl.endsWith("/" + EDIT_URL_SUFFIX))
				{
					Type type = env.getType(KeyPrefix.get(requestedUrl.substring(0, 3)));
					
					if (type != null)
					{
						try
						{
							StandardAction stdAction = actionService.getStandardEditAction(type, authData.getProfile(), env);
							if (stdAction == null)
							{
								// use default page for this type and action type
								// TODO note that below are two time consuming operations - two queries
								// should we optimize it as this will be called very often?
								TypeInfo typeInfo = typeInfoService.getForType(type.getKID(), env);
								
								if (typeInfo == null)
								{
									throw new ServletException("Type information object not found for type " + type.getQualifiedName());
								}
								
								action = actionService.getAction(typeInfo.getDefaultEditAction().getId(), env);
							}
							else
							{
								// get page from env because there they are stored with their controller data initialized
								action = env.getActionForUrl(stdAction.getAction().getUrl());
							}
							
							isStandardAction = true;
							applyOverriddenView(action, type, UserSettingKeys.KM_SYS_DEFAULT_TYPE_EDIT_VIEW, authData, env);
						}
						catch (KommetException e)
						{
							throw new ServletException("Error getting standard edit page: " + e.getMessage(), e);
						}
						
						// extract the record's ID from the URL
						String recordId = requestedUrl.substring(0, KID.LENGTH);
						
						// add the ID of the requested object to the request
						request.setAttribute("id", recordId);
					}
				}
				// if it's a create URL, e.g. "012/n"
				else if ((requestedUrl.length() == (KeyPrefix.LENGTH) + NEW_URL_SUFFIX.length() + 1) && requestedUrl.endsWith("/" + NEW_URL_SUFFIX))
				{
					Type type = env.getType(KeyPrefix.get(requestedUrl.substring(0, 3)));
					
					if (type != null)
					{
						try
						{
							StandardAction stdAction = actionService.getStandardCreateAction(type, authData.getProfile(), env);
							if (stdAction == null)
							{
								// use default page for this type and action type
								// TODO note that below are two time consuming operations - two queries
								// should we optimize it as this will be called very often?
								TypeInfo typeInfo = typeInfoService.getForType(type.getKID(), env);
								
								if (typeInfo == null)
								{
									throw new ServletException("Type information object not found for type " + type.getQualifiedName());
								}
								
								action = actionService.getAction(typeInfo.getDefaultCreateAction().getId(), env);
							}
							else
							{
								// get page from env because there they are stored with their controller data initialized
								action = env.getActionForUrl(stdAction.getAction().getUrl());
							}
							
							isStandardAction = true;
							applyOverriddenView(action, type, UserSettingKeys.KM_SYS_DEFAULT_TYPE_CREATE_VIEW, authData, env);
						}
						catch (KommetException e)
						{
							throw new ServletException("Error getting standard edit action: " + e.getMessage(), e);
						}
					}
				}
				// if the URL is a key prefix, e.g. "1cq"
				else if (requestedUrl.length() == KeyPrefix.LENGTH)
				{
					Type type = env.getType(KeyPrefix.get(requestedUrl));
					if (type != null)
					{
						try
						{
							StandardAction stdAction = actionService.getStandardListAction(type, authData.getProfile(), env);
							if (stdAction == null)
							{
								// use default action for this type and action type
								// TODO note that below are two time consuming operations - two queries
								// should we optimize it as this will be called very often?
								TypeInfo typeInfo = typeInfoService.getForType(type.getKID(), env);
								
								if (typeInfo == null)
								{
									throw new ServletException("Type information object not found for type " + type.getQualifiedName());
								}
								
								action = actionService.getAction(typeInfo.getDefaultListAction().getId(), env);
							}
							else
							{
								// get action from env because there they are stored with their controller data initialized
								action = env.getActionForUrl(stdAction.getAction().getUrl());
							}

							isStandardAction = true;
							applyOverriddenView(action, type, UserSettingKeys.KM_SYS_DEFAULT_TYPE_LIST_VIEW, authData, env);
						}
						catch (KommetException e)
						{
							throw new ServletException("Error getting standard edit action: " + e.getMessage(), e);
						}
					}
				}
			}
		}
		catch (KommetException e)
		{
			throw new ServletException("Error retrieving action for URL " + requestedUrl + ". Nested exception: " + e.getMessage());
		}
		
		PageData pageData = new PageData(env);
		pageData.setRequestURL(request.getRequestURI());
		pageData.setHttpRequest(request);
		
		View view = null;
		
		// get the view from the environment, not directly from the action object, because there it can be out-of-date
		if (action != null)
		{
			view = env.getView(action.getView().getId());
			
			if (view == null)
			{
				throw new ServletException("View " + action.getView().getId() + " not found on env");
			}
		}
		else if (genericAction != null && genericAction.getViewId() != null)
		{
			view = env.getView(genericAction.getViewId());
			
			if (view == null)
			{
				throw new ServletException("View " + genericAction.getViewId() + " not found on env");
			}
		}
		
		try
		{
			pageData = KmParamUtils.initRmParams(request, pageData);
		}
		catch (KmParamException e)
		{
			e.printStackTrace();
			throw new ServletException("Error processing rm parameters: " + e.getMessage(), e);
		}
		
		// handle layout only if this is not a generic action, because we don't need
		// any layout for REST/WS services
		
		if (genericAction == null || !genericAction.isRest())
		{
			try
			{
				kommet.web.kmparams.actions.Action layoutRmParam = pageData.getRmParams() != null ? pageData.getRmParams().getSingleActionNode("layout") : null;
				KID layoutId = null;
				if (layoutRmParam != null)
				{
					// if layout is specified in the RM parameters, it can still be null,
					// which means that the page should be rendered with no layout
					layoutId = ((OverrideLayout)layoutRmParam).getLayoutId();
					if (layoutId == null)
					{
						layoutId = KID.get("00f0000000004");
					}
				}
				else
				{
					// check if layout has been overridden by a request parameter
					if (layoutId == null && view != null)
					{
						layoutId = determineLayout(view, env, authData);
					}
				}
				
				request.setAttribute(RequestAttributes.LAYOUT_ATTR_NAME, layoutId);
			}
			catch (KmParamException e)
			{
				e.printStackTrace();
				throw new ServletException("Error processing rm parameters: " + MiscUtils.getExceptionDesc(e), e);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new ServletException("Error processing preparing layout: " + MiscUtils.getExceptionDesc(e), e);
			}
		}
		
		if (action == null && genericAction == null)
		{	
			// no auth data is needed for built-in requests
			env.clearAuthData();
			
			// there is no action for this URL, but perhaps it's a regular URL,
			// so we just continue with processing the request
			chain.doFilter(request, response);
			return;
		}
		
		String controllerName = null;
		String actionMethod = null;
		Map<String, String> urlParams = null;
		
		if (action != null)
		{
			// pass view ID to page data
			pageData.setViewId(action.getView().getId());
			
			controllerName = action.getController().getQualifiedName();
			actionMethod = action.getControllerMethod();
			
			if (!Boolean.TRUE.equals(action.getIsSystem()))
			{
				// read URL params from action URL
				try
				{
					urlParams = new ParsedURL(action.getUrl()).getParamValues(requestedUrl, !isStandardAction);
				}
				catch (KommetException e)
				{
					e.printStackTrace();
					throw new ServletException("Error calling action method: " + MiscUtils.getExceptionDesc(e), e);
				}
			}
		}
		else if (genericAction != null)
		{
			response.setContentType("application/json; charset=UTF-8");
			
			controllerName = genericAction.getControllerName();
			actionMethod = genericAction.getActionMethod();
			
			// read URL params from action URL
			try
			{
				urlParams = new ParsedURL(genericAction.getUrl()).getParamValues(requestedUrl, true);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new ServletException("Error calling action method: " + MiscUtils.getExceptionDesc(e), e);
			}
		}
		else
		{
			env.clearAuthData();
			throw new ServletException("At this stage either action or genericAction must be not null");
		}
		
		// create a new transaction savepoint so that we can roll back
		// if anything goes wrong in the transaction
		TransactionDefinition def = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		TransactionStatus txStatus = transactionManager.getTransaction(def);
		Object actionResult = null;
		
		try
		{
			if (appConfig.isDebugKollCode())
			{
				log.debug("User KOLL start [" + requestID + "] [" + (System.currentTimeMillis() - reqStartTime) + "]");
			}
			
			actionResult = ActionUtil.callAction(controllerName, actionMethod, urlParams, sysContextFactory.get(authData, env), request, response, pageData, compiler, dataService, sharingService, authData, env, appConfig);
			
			if (appConfig.isDebugKollCode())
			{
				log.debug("User KOLL end [" + requestID + "] [" + (System.currentTimeMillis() - reqStartTime) + "]");
			}
			
			transactionManager.commit(txStatus);
			
			if (appConfig.isDebugKollCode())
			{
				log.debug("User tx committed [" + requestID + "] [" + (System.currentTimeMillis() - reqStartTime) + "]");
			}
		}
		catch (Exception e)
		{
			env.clearAuthData();
			
			transactionManager.rollback(txStatus);
			
			Exception nestedException = e;
			
			if (e.getCause() != null)
			{
				e.getCause().printStackTrace();
				nestedException = e;
			}
			
			// an error has occurred
			// if its a regular action, just forward to an error page
			if (action != null)
			{
				throw new ServletException("Error calling action method: " + MiscUtils.getExceptionDesc(nestedException), nestedException);
			}
			else if (genericAction != null)
			{
				// convert exception to JSON response
				returnRestActionError("Error calling action. Nested exception is: " + e.getMessage(), null, response);
				return;
			}
			else
			{	
				// this will never happen, but we want to make it clear that this place cannot be reached
				throw new ServletException("After action is performed, action or genericAction must be not null");
			}
		}
		
		if (genericAction == null)
		{
			// it is not a generic action, so it must be a registered action
			
			// actionResult will always be an object of type PageData or byte[] if the action was a regular action,
			// not a REST/Web service
			if (actionResult instanceof PageData)
			{
				pageData = (PageData)actionResult;
			}
			else
			{
				Method method = getMethodByName(controllerName, actionMethod, compiler, env);
				ReturnsFile returnsFileAnnotation = method.getAnnotation(ReturnsFile.class);
				
				// check if this action does not have @BinaryResponse annotation
				if (returnsFileAnnotation != null)
				{
					throw new ServletException("Controller method " + controllerName + "." + actionMethod + " does not return PageData, so it must be annotated with @" + ReturnsFile.class.getSimpleName());
				}
				
				try
				{
					ActionUtil.returnFile(actionResult, returnsFileAnnotation, response);
				}
				catch (KommetException e)
				{
					env.clearAuthData();
					throw new ServletException(e.getMessage());
				}
				return;
			}
		}
		else
		{
			// generic actions can return:
			// 1) JSON (if annotated with @Rest)
			// 2) files (if annotated with @ReturnsFile)
			// 3) regular pageData
			
			Method method = getMethodByName(controllerName, actionMethod, compiler, env);
			ReturnsFile returnsFileAnnotation = method.getAnnotation(ReturnsFile.class);
			
			if (returnsFileAnnotation != null)
			{
				// make sure that actions annotated with @ReturnsFile are not also annotated
				// with @Rest
				if (method.isAnnotationPresent(Rest.class))
				{
					throw new ServletException("Action method is annotated both with @" + Rest.class.getSimpleName() + " and @" + ReturnsFile.class.getSimpleName() + ". Only one of these annotations is allowed at a time.");
				}
				
				try
				{
					ActionUtil.returnFile(actionResult, returnsFileAnnotation, response);
				}
				catch (KommetException e)
				{
					throw new ServletException(e.getMessage());
				}
				return;
			}
			else if (method.isAnnotationPresent(Rest.class))
			{
				// this is a REST method
				// convert action response to JSON and write the JSON to the response body
				returnRestActionResult(genericAction, actionResult, response, authData);
				return;
			}
			else
			{
				// if is not annotated with @Rest or @ReturnsFile, then it must return page data
				if (!method.getReturnType().equals(PageData.class))
				{
					throw new ServletException("Generic action method " + controllerName + "." + method.getName() + " is not annotated with must either be annotated with @Rest, @ReturnsFile or must return PageData object");
				} 
					
				// let the processing of this action go on
			}
		}
		
		// if layout has been overridden in the action, update it in the request
		if (pageData.getLayoutId() != null)
		{
			request.setAttribute(RequestAttributes.LAYOUT_ATTR_NAME, pageData.getLayoutId());
		}
		
		// add text label collection to the request
		request.setAttribute(RequestAttributes.TEXT_LABELS_ATTR_NAME, env.getTextLabelDictionary());
		
		try
		{
			RequestDispatcher dispatcher = null;
			
			// Check if a redirect was set in the controller.
			// Only check for null, because an empty string as redirect URL would still be valid.
			if (pageData.getRedirectURL() == null)
			{	
				request.setAttribute(RequestAttributes.PAGE_DATA_ATTR_NAME, pageData);
				
				// if response body has been set by user, use this instead of the defined view
				// TODO this is a more rare case than standard actions returning views so move this condition to be checked as second
				if (pageData.getHttpResponse() != null && StringUtils.hasText(pageData.getHttpResponse().getBody()))
				{
					//log.debug("Writing response body [" + requestID + "]");
					
					// write response body directly
					response.getWriter().write(pageData.getHttpResponse().getBody());
					
					// modify content type
					if (pageData.getHttpResponse().getContentType() != null)
					{
						response.setContentType(pageData.getHttpResponse().getContentType());
					}
					
					if (pageData.getHttpResponse().getStatusCode() != null)
					{
						response.setStatus(pageData.getHttpResponse().getStatusCode());
					}
				}
				else
				{
					// Earlier pageData.viewId was set to the view of the current action.
					// In the meantime it could have been altered by the controller method, but it can never be empty.
					if (pageData.getViewId() == null)
					{	
						if (genericAction != null && genericAction.getViewId() != null)
						{
							// if view not explicitly set by user on the PageData object
							// use the view defined by user in the @View annotation
							pageData.setViewId(genericAction.getViewId());
						}
						else
						{
							throw new KommetException("View set to null. Probably a controller method changed the view to be displayed to an illegal empty value.");
						}
					}
					
					if (appConfig.isDebugKollCode())
					{
						log.debug("Dispatching view [" + requestID + "] [" + (System.currentTimeMillis() - reqStartTime) + "]");
					}
					
					if (pageData.getHttpResponse() != null && pageData.getHttpResponse().getStatusCode() != null)
					{
						response.setStatus(pageData.getHttpResponse().getStatusCode());
					}
					
					// forward the request to the KTL-JSP file - use the view ID from page data
					dispatcher = request.getRequestDispatcher("/" + env.getKeetleDir(appConfig.getRelativeKeetleDir()) + "/" + pageData.getViewId() + ".jsp");
					dispatcher.forward(request, response);
					
					if (appConfig.isDebugKollCode())
					{
						log.debug("View dispatched [" + requestID + "] [" + (System.currentTimeMillis() - reqStartTime) + "]");
					}
				}
			}
			else
			{
				// since this is a redirect, no page data is passed from the controller
				// TODO you can change this to allow for more flexibility during redirects (passing
				// params in redirects)
				
				// We cannot simply redirect to partial URL, instead, we need to explicitly redirect to HTTPS.
				// This is because Tomcat is behind a proxy and does not know that it uses HTTPS.
				// Redirecting to HTTP will work in all situations except when this action is called
				// within an iframe. In such case the browser will not allow for redirecting from HTTPS to HTTP
				// within an iframe, because users would not know that they are not using HTTPS any more, which is a
				// security risk.
				
				if (appConfig.isRedirectToHttps())
				{
					//response.sendRedirect("https://" + request.getServerName() + request.getContextPath() + pageData.getRedirectURL());
					response.sendRedirect("https://" + request.getServerName() + pageData.getRedirectURL());
				}
				else
				{
					// do not convert URL to HTTPS
					//response.sendRedirect(request.getServerName() + (request.getServerPort() != 80 ? (":" + request.getServerPort()) : "") + request.getContextPath() + pageData.getRedirectURL());
					response.sendRedirect(request.getContextPath() + pageData.getRedirectURL());
				}
				
				if (appConfig.isDebugKollCode())
				{
					log.debug("Redirecting [" + requestID + "] [" + (System.currentTimeMillis() - reqStartTime) + "]");
				}
			}
			
			env.clearAuthData();
			
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			//response.sendRedirect(request.getContextPath() + "/km/handleerror?msg=test" + URLEncoder.encode(e.getMessage()));
			
			throw new ServletException("Error redirecting to result view: " + e.getMessage(), e);
		}
	}
	
	private AuthData handleCustomAuth(GenericAction genericAction, KommetCompiler compiler, EnvData env, HttpServletRequest req) throws ServletException
	{
		KID userId = null;
		try
		{
			AuthHandler handler = genericAction.getAuthHandler(compiler, env); 
			userId = handler.check(req.getHeader(genericAction.getAuthHandlerConfig().getTokenHeader()));
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			throw new ServletException("Error running AuthHandler: " + e.getMessage());
		}
		if (userId != null)
		{
			// authentication successful
			User user = null;
			try
			{
				user = userService.getUser(userId, env);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new ServletException("Error finding user by ID " + userId + ": " + e.getMessage());
			}
			
			PrepareAuthDataResult result = null;
			try
			{
				result = AuthUtil.prepareAuthData(user, req.getSession(), false, userService, i18nService, uchService, appConfig, env);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new ServletException("Error initializing auth data for user " + userId + ": " + e.getMessage());
			}
			return result.getAuthData();
		}
		
		return null;
	}

	private AuthData handleRememberMe (HttpServletRequest req, UserService userService, EnvData env) throws ServletException
	{
		if (env == null)
		{
			return null;
		}
		
		try
		{
			return rememberMeService.read(req, userService, env);
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			throw new ServletException("Error checking remember-me: " + e.getMessage());
		}
	}

	private App getApp(EnvData env, String domain) throws ServletException
	{
		// at this point we are sure env is not null
		// try to find app by URL
		AppUrlFilter appUrlFilter = new AppUrlFilter();
		appUrlFilter.addUrl(domain);
		
		try
		{
			List<AppUrl> apps = appService.find(appUrlFilter, env);
			
			if (!apps.isEmpty())
			{
				return apps.get(0).getApp();
			}
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			throw new ServletException("Error find app for URL: " + e.getMessage());
		}
		
		return null;
	}

	/**
	 * If the used action is a default action, this method checks if the default view for this action type has been overridden in user settings. If yes,
	 * this overridden view is applied to the action.
	 * @param action
	 * @param type
	 * @param viewTypeSettingKey
	 * @param authData
	 * @param env
	 * @throws ServletException
	 * @throws KommetException
	 */
	private void applyOverriddenView(Action action, Type type, String viewTypeSettingKey, AuthData authData, EnvData env) throws ServletException, KommetException
	{
		if (action == null)
		{
			throw new KommetException("Cannot determine overridden view for null action");
		}
		
		// check if this is a default action
		if (!Boolean.TRUE.equals(action.getIsSystem()))
		{
			return;
		}
		
		KID overridenDefaultTypeViewId = uchService.getUserSettingAsKID(viewTypeSettingKey + "." + type.getKID(), authData, AuthData.getRootAuthData(env), env);
		if (overridenDefaultTypeViewId != null)
		{
			View overriddenView = viewService.getView(overridenDefaultTypeViewId, env);
			if (overriddenView == null)
			{
				// TODO instead of throwing an ugly error redirect to error page? or just catch all ServletExceptions
				throw new ServletException("Overridden view " + overridenDefaultTypeViewId + " does not exist");
			}
			
			action.setView(overriddenView);
		}
	}

	private boolean isOpenURL(String requestedUrl)
	{
		return openURLs.contains(requestedUrl.toLowerCase()) || requestedUrl.startsWith("resources/")
				|| requestedUrl.startsWith(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/download/")
				|| requestedUrl.startsWith(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/downloadresource");
	}

	private String getHomeURL(AuthData authData, App app, EnvData env) throws KommetException
	{
		String homeURL = null;
		
		if (env != null)
		{
			Type systemValueType = env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX));
			
			// check if a setting exist for the default URL
			RecordProxy homeURLSetting = uchService.getSetting(systemValueType, Arrays.asList("value"), "key", UserSettingKeys.KM_SYS_HOME_PAGE, authData, AuthData.getRootAuthData(env), env);
			//SettingValue homeURLSetting = settingValueService.get(Constants.SYSTEM_SETTING_KEY_HOME_PAGE, false, AuthData.getRootAuthData(env), env);
			if (homeURLSetting != null)
			{
				homeURL = (String)homeURLSetting.getField("value");
			}
		}
		
		String defaultURL = authData.getProfile() != null && AuthUtil.isSysAdminOrRoot(authData) ? appConfig.getAdminHomeURL() : appConfig.getHomeURL();
		
		return homeURL != null ? homeURL : defaultURL;
	}

	/**
	 * Returns an action method by controller name and method name.
	 * @param controllerName
	 * @param methodName
	 * @param compiler
	 * @param env
	 * @return
	 * @throws ServletException
	 */
	private Method getMethodByName (String controllerName, String methodName, KommetCompiler compiler, EnvData env) throws ServletException
	{
		Class<?> controllerClass;
		try
		{
			controllerClass = compiler.getClass(controllerName, true, env);
		}
		catch (ClassNotFoundException e)
		{
			throw new ServletException("Controller class with name " + controllerName + " not found");
		}
		catch (KommetException e)
		{
			throw new ServletException("Error getting controller class " + controllerName + ". Nested: " + e.getMessage());
		}
		
		return MiscUtils.getMethodByName(controllerClass, methodName);
	}
	
	private void returnRestActionError (String errorMsg, Integer responseCode, HttpServletResponse response) throws ServletException
	{
		StringBuilder respBody = new StringBuilder();
		respBody.append("{ \"success\": false, \"message\": \"");
		
		try
		{
			respBody.append(JSON.escape(errorMsg)).append("\" }");
		}
		catch (KommetException e1)
		{
			respBody.append("An error occurred, but the error message could not be returned").append("\" }");
		}
		
		if (responseCode == null)
		{
			responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
		
		try
		{
			response.setStatus(responseCode);
			response.getWriter().write(respBody.toString());
		}
		catch (IOException e)
		{
			// this is an extreme case when a response cannot be written so an error must be thrown
			// in all other cases a REST method will return JSON data containing action response
			// or error information in case of failure
			throw new ServletException("Error writing REST response: " + e.getMessage());
		}
	}

	private void returnRestActionResult(GenericAction action, Object actionResult, HttpServletResponse response, AuthData authData) throws ServletException
	{
		StringBuilder respBody = new StringBuilder();
		int respCode = HttpServletResponse.SC_OK;
		
		// convert action result into JSON
		if (action.isReturnsResponseBody())
		{
			if (actionResult instanceof String)
			{
				// treat action result as response body
				respBody.append((String)actionResult);
			}
			else
			{
				try
				{
					respBody.append(RestUtil.getRestErrorResponse("Action method annotated with @" + ResponseBody.class.getSimpleName() + " must return string, but instead returns " + actionResult.getClass().getName()));
				}
				catch (KommetException e)
				{
					throw new ServletException("Error generating error response: " + e.getMessage());
				}
				respCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			}
		}
		else
		{	
			long reqStartTime = System.currentTimeMillis();
			try
			{
				// serialize the action result to JSON and append it to the response body
				respBody.append(JSON.serialize(actionResult, authData));
			}
			catch (KommetException e)
			{
				try
				{
					respBody.append(RestUtil.getRestErrorResponse("Error parsing action result to JSON. Nested: " + e.getMessage()));
				}
				catch (KommetException e1)
				{
					throw new ServletException("Error generating error response: " + e.getMessage());
				}
				respCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			}
			
			log.debug("REST response serialization took " + (System.currentTimeMillis() - reqStartTime) + "ms");;
		}
		
		try
		{
			response.setStatus(respCode);
			//response.setCharacterEncoding("UTF-8");
			response.getWriter().write(respBody.toString());
		}
		catch (IOException e)
		{
			// this is an extreme case when a response cannot be written so an error must be thrown
			// in all other cases a REST method will return JSON data containing action response
			// or error information in case of failure
			throw new ServletException("Error writing REST response: " + e.getMessage());
		}
	}

	/**
	 * Removes the "www" prefix from the server name, if it has one.
	 * @param serverName
	 * @return
	 */
	private static String stripServerName(String serverName)
	{
		return serverName != null && serverName.startsWith("www.") ? serverName.substring(4) : serverName;
	}
	
	/**
	 * Attemps to get REST user's auth data from the request. Auth data can be retrieved if user passed
	 * <tt>access_token</tt> and <tt>env</tt> parameters. In such case, if the access token is valid,
	 * this method returns the <tt>AuthData</tt> object. Otherwise it returns null. 
	 * @param request
	 * @return
	 * @throws KommetException
	 */
	private AuthData getRestAuthData(HttpServletRequest request) throws KommetException
	{
		if (request.getParameter("access_token") != null && request.getParameter("env") != null)
		{
			// try to get auth data from access_token
			AccessToken apiToken = tokenStore.getToken(request.getParameter("access_token"));
			KID envId = KID.get(request.getParameter("env"));
			
			if (apiToken != null)
			{	
				EnvData env = envService.get(envId);
				
				// TODO is it not too time consuming to fetch user auth data each time they make an API call?
				return userService.getAuthData(userService.getUser(apiToken.getUserId(), env), env);
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

	/**
	 * Returns the ID of the layout to be used. The layout is determined basing on view, profile and environment
	 * settings.
	 * @param view
	 * @param env
	 * @param authData
	 * @return
	 * @throws KommetException
	 */
	private KID determineLayout(View view, EnvData env, AuthData authData) throws KommetException
	{
		if (view.getLayout() != null)
		{
			// if layout is defined on the view, use this layout
			return view.getLayout().getId();
		}
		else if (authData.getUserSettings() != null && authData.getUserSettings().getLayout() != null)
		{
			// get layout from user-specific settings
			return authData.getUserSettings().getLayout().getId();
		}
		else
		{
			// use environment's default layout
			return uchService.getUserSettingAsKID(UserSettingKeys.KM_SYS_DEFAULT_LAYOUT_ID, authData, AuthData.getRootAuthData(env), env);
		}
	}
}
