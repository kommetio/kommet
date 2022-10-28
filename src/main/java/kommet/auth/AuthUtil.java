/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.util.StringUtils;

import kommet.basic.Profile;
import kommet.basic.User;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.i18n.InternationalizationService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;

public class AuthUtil
{
	private static final String AUTH_DATA_SESSION_KEY = "authData";
	private static final String LOGIN_AS_AUTH_DATA_SESSION_KEY = "loginAsAuthData";
	
	/**
	 * Stores primary auth data in the session.
	 * @param authData
	 * @param session
	 */
	public static void storePrimaryAuthData (AuthData authData, HttpSession session)
	{
		session.setAttribute(AUTH_DATA_SESSION_KEY, authData);
	}
	
	/**
	 * Prepares user's auth data after they have been logged in and puts it into
	 * the session.
	 * 
	 * @param user
	 * @param session
	 * @param env
	 * @return URL to open after user is logged in
	 * @throws KommetException
	 */
	public static PrepareAuthDataResult prepareAuthData(User user, HttpSession session, boolean isSecondaryLogin, UserService userService, InternationalizationService i18n, UserCascadeHierarchyService uchService, AppConfig appConfig, EnvData env) throws KommetException
	{
		// URL to open after user is logged in
		String landingURL = null;

		// put user data into the session
		AuthData authData = userService.getAuthData(user, env);

		if (!StringUtils.hasText(landingURL))
		{
			// go to user's landing URL
			landingURL = authData.getUserSettings().getLandingURL();
		}

		// init all user permissions
		authData.initUserPermissions(env);
		authData.initUserCascadeSettings(uchService, env);
		authData.setI18n(i18n.getDictionary(authData.getUser().getLocaleSetting()));

		if (!isSecondaryLogin)
		{
			AuthUtil.storePrimaryAuthData(authData, session);
		}
		else
		{
			AuthUtil.storeSecondaryAuthData(authData, appConfig, session);
		}

		return new PrepareAuthDataResult(authData, landingURL);
	}
	
	/**
	 * Gets the secondary auth data.
	 * @param session Current session
	 * @return Secondary auth data, or null if no user is logged in with secondary logging.
	 */
	@SuppressWarnings("unchecked")
	private static AuthData getSecondaryAuthData(HttpSession session)
	{
		if (session.getAttribute(LOGIN_AS_AUTH_DATA_SESSION_KEY) == null)
		{
			return null;
		}
		else
		{
			List<AuthData> loginAsAuthData = (List<AuthData>)session.getAttribute(LOGIN_AS_AUTH_DATA_SESSION_KEY);
			return loginAsAuthData.isEmpty() ? null : loginAsAuthData.get(0);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void storeSecondaryAuthData (AuthData authData, AppConfig config, HttpSession session) throws KommetException
	{
		List<AuthData> loginAsAuthData = null;
		
		if (session.getAttribute(LOGIN_AS_AUTH_DATA_SESSION_KEY) == null)
		{
			loginAsAuthData = new ArrayList<AuthData>();
		}
		else
		{
			loginAsAuthData = (List<AuthData>)session.getAttribute(LOGIN_AS_AUTH_DATA_SESSION_KEY);
		}
		
		if (loginAsAuthData.size() == config.getLoginAsMax())
		{
			throw new AuthException("Cannot log in as more than " + config.getLoginAsMax() + " users - this limit is defined in app config");
		}
		
		AuthData currentAuthData = getAuthData(session);
		if (currentAuthData == null)
		{
			throw new AuthException("Login as feature cannot be used by unauthenticated users");
		}
		
		// prevent logging in as the same user twice
		if (currentAuthData.getUserId().equals(authData.getUserId()))
		{
			throw new AuthException("Trying to log in as the same user as the primary authenticated one");
		}
		
		AuthData authDataToRemove = null;
		
		// make sure this user is not already logged in
		for (AuthData ad : loginAsAuthData)
		{
			if (ad.getUser().getId().equals(authData.getUser().getId()))
			{
				authDataToRemove = ad;
				break;
			}
		}
		
		if (authDataToRemove != null)
		{
			loginAsAuthData.remove(authDataToRemove);
		}
		
		// prepend login as credentials at the beginning of the list
		loginAsAuthData.add(0, authData);
		
		session.setAttribute(LOGIN_AS_AUTH_DATA_SESSION_KEY, loginAsAuthData);
	}
	
	/**
	 * Gets the current auth data.
	 * <ul>
	 * <li>If user is logged in using secondary logging, secondary auth data will be returned.</li>
	 * <li>If user is only logged in using primary logging, primary auth data will be returned.</li>
	 * <li>If user is not logged in at all, null will be returned.</li>
	 * </ul>
	 * @param session Current session
	 * @return
	 */
	public static AuthData getAuthData (HttpSession session)
	{
		// first try to get login as auth data
		AuthData authData = getSecondaryAuthData(session);
		
		// return either primary or "login as" auth data
		return authData != null ? authData : (AuthData)session.getAttribute(AUTH_DATA_SESSION_KEY);
	}

	public static boolean hasProfile (String profile, HttpSession session) throws KommetException
	{
		if (session == null)
		{
			return false;
		}
		AuthData authData = getAuthData(session);
		return authData != null && authData.hasProfile(profile);
	}

	/**
	 * Clears the latest (secondary if exists, otherwise primary) auth data. This effectively works
	 * as log out for the current user and going back to previous logging credentials, if any exist.
	 * @param session Current session
	 */
	public static void clearAuthData(HttpSession session)
	{
		if (getSecondaryAuthData(session) != null)
		{
			clearLatestLoginAsAuthData(session);
		}
		else
		{
			storePrimaryAuthData(null, session);
		}
	}

	@SuppressWarnings("unchecked")
	private static void clearLatestLoginAsAuthData(HttpSession session)
	{
		List<AuthData> loginAsAuthData = (List<AuthData>)session.getAttribute(LOGIN_AS_AUTH_DATA_SESSION_KEY);
		if (loginAsAuthData == null || loginAsAuthData.isEmpty())
		{
			return;
		}
		else
		{
			loginAsAuthData.remove(0);
			session.setAttribute(LOGIN_AS_AUTH_DATA_SESSION_KEY, loginAsAuthData);
		}
	}
	
	/**
	 * Tells whether user with primary can log in as user
	 * @param primaryUser
	 * @param secondaryUser
	 * @param config
	 * @return
	 * @throws AuthException 
	 */
	public static boolean canLogin (AuthData primaryAuthData, KID userId) throws AuthException
	{
		if (primaryAuthData == null)
		{
			throw new AuthException("AuthData is null");
		}
		
		// only system administrators can log in as other users
		return Profile.ROOT_NAME.equals(primaryAuthData.getProfile().getName());
	}

	public static boolean isRoot(AuthData authData)
	{
		return authData != null && authData.getProfile() != null && authData.getProfile().getName().equals(Profile.ROOT_NAME);
	}
	
	public static boolean isSysAdmin(AuthData authData)
	{
		return authData != null && authData.getProfile() != null && authData.getProfile().getName().equals(Profile.SYSTEM_ADMINISTRATOR_NAME);
	}

	/**
	 * Tells if the user can edit code (KTL views, KOLL files, layouts etc.)
	 * @param authData
	 * @return
	 */
	public static boolean canModifyCode(AuthData authData)
	{
		return isRoot(authData);
	}

	/**
	 * Checks if the current user can edit the given record. This is checked basing on the user's type permissions
	 * as well as on user record sharings.
	 * @param recordId
	 * @param authData
	 * @param sharingService
	 * @param env
	 * @return
	 * @throws KommetException 
	 */
	public static boolean canEditRecord(KID recordId, AuthData authData, SharingService sharingService, EnvData env) throws KommetException
	{
		KID typeId = env.getTypeByRecordId(recordId).getKID();
		if (AuthUtil.isRoot(authData) || authData.canEditAllType(typeId, false, env))
		{
			return true;
		}
		
		if (authData.canEditType(typeId, false, env) && sharingService.canEditRecord(recordId, authData.getUserId(), env))
		{
			return true;
		}
		
		return false;
	}

	public static boolean isSysAdminOrRoot(AuthData authData)
	{
		return AuthUtil.isRoot(authData) || AuthUtil.isRoot(authData);
	}
	
	public static class PrepareAuthDataResult
	{
		private AuthData authData;
		private String landingURL;

		public PrepareAuthDataResult(AuthData authData, String landingURL)
		{
			this.authData = authData;
			this.landingURL = landingURL;
		}

		public AuthData getAuthData()
		{
			return authData;
		}

		public String getLandingURL()
		{
			return landingURL;
		}
	}
}
