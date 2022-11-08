/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.basic.StandardTypeRecordProxy;
import kommet.basic.User;
import kommet.basic.UserSettings;
import kommet.config.UIConfig;
import kommet.dao.ProfileDao;
import kommet.dao.UserDao;
import kommet.dao.UserSettingsDao;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.UserFilter;
import kommet.filters.UserSettingsFilter;
import kommet.integration.PropertySelection;
import kommet.koll.compiler.KommetCompiler;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.MiscUtils;

@Service
public class UserService
{
	@Inject
	UserDao userDao;
	
	@Inject
	ProfileDao profileDao;
	
	@Inject
	UserSettingsDao userSettingsDao;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	KommetCompiler compiler;
	
	private static final String UPDATE_ROOT_PWD_FLAG = "UPDATEROOTPWD";
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Transactional(readOnly = true)
	public User getUser (KID id, EnvData env) throws KommetException
	{
		return userDao.get(id, PropertySelection.SPECIFIED_AND_BASIC, "userName, email, password, locale, timezone, profile.id, profile.name", env);
	}
	
	@Transactional(readOnly = true)
	public List<User> get (UserFilter filter, EnvData env) throws KommetException
	{
		return userDao.get(filter, env);
	}
	
	/**
	 * Initialize users assigned to fields createdBy and lastModifiedBy on a list of records.
	 * @param objs
	 * @param env
	 * @return
	 * @throws KommetException 
	 */
	@Transactional(readOnly = true)
	public <T extends StandardTypeRecordProxy> Map<KID, User> getUsersForStandardFields (List<T> objs, EnvData env) throws KommetException
	{
		if (objs == null || objs.isEmpty())
		{
			// if no records were passed, just return empty map
			return new HashMap<KID, User>();
		}
		
		UserFilter filter = new UserFilter();
		
		for (T obj : objs)
		{
			filter.addUserId(obj.getCreatedBy().getId());
			filter.addUserId(obj.getLastModifiedBy().getId());
		}
		
		// find users with ids
		List<User> users = userDao.get(filter, env);
		
		// map users by ID
		return MiscUtils.mapById(users);
	}
	
	@Transactional(readOnly = true)
	public User get (String userName, EnvData env) throws KommetException
	{
		UserFilter filter = new UserFilter();
		filter.setUsername(userName);
		List<User> users = userDao.get(filter, env);
		
		if (users.size() > 1)
		{
			throw new KommetException("More than one user found with user name " + userName);
		}
		
		return users.isEmpty() ? null : users.get(0);
	}

	@Transactional(readOnly = true)
	public boolean updateRootPassword (String newEncryptedPwd, EnvData env) throws KommetException
	{
		env.getJdbcTemplate().execute("UPDATE obj_004 SET _triggerflag = '" + UPDATE_ROOT_PWD_FLAG + "', password = '" + newEncryptedPwd + "' WHERE username = 'root'");
		return true;
	}
	
	@Transactional(readOnly = true)
	public boolean activateRoot (String newEncryptedPwd, EnvData env) throws KommetException
	{
		env.getJdbcTemplate().execute("UPDATE obj_004 SET _triggerflag = '" + UPDATE_ROOT_PWD_FLAG + "', password = '" + newEncryptedPwd + "', isactive = true, activationhash = null WHERE username = 'root'");
		return true;
	}

	@Transactional
	public User save (User user, AuthData authData, EnvData env) throws KommetException
	{	
		return userDao.save(user, authData, env);
	}
	
	@Transactional(readOnly = true)
	public User authenticate (String userName, String plainTextPassword, EnvData env) throws KommetException
	{
		UserFilter filter = new UserFilter();
		filter.setUsername(userName);
		filter.setEncryptedPassword(MiscUtils.getSHA1Password(plainTextPassword));
		List<User> users = userDao.get(filter, env);
		return users.isEmpty() ? null : users.get(0);
	}

	@Transactional
	public UserSettings save (UserSettings settings, AuthData authData, EnvData env) throws KommetException
	{
		if (settings.getUser() != null && settings.getProfile() != null)
		{
			throw new KommetException("Both user and profile set on user settings object. Only one of these values is allowed");
		}
		
		if (settings.getUser() == null && settings.getProfile() == null)
		{
			throw new KommetException("Neither user nor profile set on user settings object. Exactly one of these properties needs to be set.");
		}
		
		return userSettingsDao.save(settings, authData, env);
	}

	@Transactional(readOnly = true)
	public UserSettings getMergedUserSettings (KID userId, KID profileId, EnvData env) throws KommetException
	{
		UserSettingsFilter filter = new UserSettingsFilter();
		filter.addUserId(userId);
		filter.addProfileId(profileId);
		filter.setUserOrProfile(true);
		List<UserSettings> settings = userSettingsDao.get(filter, env);
		
		UserSettings userSettings = null;
		UserSettings profileSettings = null;
		
		for (UserSettings us : settings)
		{
			if (us.getProfile() != null)
			{
				userSettings = us;
			}
			else
			{
				profileSettings = us;
			}
		}
		
		UserSettings mergedSettings = new UserSettings();
		
		if (userSettings != null)
		{
			fillSettings(userSettings, mergedSettings);
		}
		if (profileSettings != null)
		{
			fillSettings(profileSettings, mergedSettings);
		}
		
		return mergedSettings;
	}

	private void fillSettings(UserSettings source, UserSettings dest)
	{
		if (dest.getLandingURL() == null)
		{
			dest.setLandingURL(source.getLandingURL());
		}
		
		if (dest.getLayout() == null)
		{
			dest.setLayout(source.getLayout());
		}
	}

	@Transactional(readOnly = true)
	public UserSettings getProfileSettings(KID id, EnvData env) throws KommetException
	{
		UserSettingsFilter filter = new UserSettingsFilter();
		filter.addProfileId(id);
		
		List<UserSettings> settings = userSettingsDao.get(filter, env);
		
		if (settings.size() < 2)
		{
			return settings.isEmpty() ? null : settings.get(0);
		}
		else
		{
			throw new KommetException("More than one user settings found for profile " + id);
		}
	}
	
	@Transactional(readOnly = true)
	public UserSettings getUserSettings(KID id, EnvData env) throws KommetException
	{
		UserSettingsFilter filter = new UserSettingsFilter();
		filter.addUserId(id);
		
		List<UserSettings> settings = userSettingsDao.get(filter, env);
		
		if (settings.size() < 2)
		{
			return settings.isEmpty() ? null : settings.get(0);
		}
		else
		{
			throw new KommetException("More than one user settings found for user " + id);
		}
	}

	/**
	 * Create auth data for the given user.
	 * @param user
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional(readOnly = true)
	public AuthData getAuthData(User user, EnvData env) throws KommetException
	{	
		AuthData authData = new AuthData(user, env, permissionService, compiler);
		authData.setEnvName(env.getEnv().getName());
		authData.setEnvId(env.getEnv().getKID());
		authData.setUser(user);
		
		// get profile with all information
		if (user != null && user.getProfile() != null && user.getProfile().getId() != null)
		{
			authData.setProfile(profileDao.get(user.getProfile().getId(), env));
		}

		// get merged user settings for user and profile
		UserSettings mergedUserSettings = getMergedUserSettings(authData.getUserId(), authData.getProfile().getId(), env);
		authData.setUserSettings(mergedUserSettings);
		
		authData.setUIConfig(UIConfig.get(uchService, authData, env));
		
		return authData;
	}

	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		userDao.delete(id, authData, env);
	}
}
