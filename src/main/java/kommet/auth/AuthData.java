/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import kommet.basic.ActionPermission;
import kommet.basic.BasicSetupService;
import kommet.basic.FieldPermission;
import kommet.basic.Profile;
import kommet.basic.TypePermission;
import kommet.basic.User;
import kommet.basic.UserSettings;
import kommet.config.UIConfig;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.i18n.I18nDictionary;
import kommet.i18n.Locale;
import kommet.koll.compiler.KommetCompiler;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

/**
 * User authentication data.
 * @author Radek Krawiec
 *
 */
public class AuthData
{
	private String envName;
	private KID envId;
	private Map<String, Record> permissionSetsByName;
	private User user;
	private Map<KID, TypePermission> typePermissionsByTypeId;
	private Map<KID, FieldPermission> fieldPermissionsByFieldId;
	private Map<String, ActionPermission> actionPermissions;
	private Profile profile;
	private boolean isGuest;
	private UIConfig UIConfig;
	private Map<String, String> userCascadeSettings = new HashMap<String, String>();
	
	protected PermissionService permissionService;
	
	/**
	 * Time when type permissions where last updated for this user/auth data;
	 */
	private long lastTypePermissionsUpdate;
	
	/**
	 * Time when field permissions where last updated for this user/auth data;
	 */
	private long lastFieldPermissionsUpdate;
	
	/**
	 * Time when page permissions where last updated for this user/auth data;
	 */
	private long lastActionPermissionsUpdate;
	
	private I18nDictionary i18n;
	
	private UserSettings userSettings;
	
	private static Map<KID, AuthData> rootAuthDataByEnvId;
	private static Map<KID, AuthData> guestAuthDataByEnvId;
	
	private String sessionId;
	
	public AuthData()
	{
		// generate random session ID
		this.sessionId = MiscUtils.getHash(50);
	}
	
	public static AuthData getRootAuthData (EnvData env) throws KommetException
	{
		if (rootAuthDataByEnvId == null)
		{
			rootAuthDataByEnvId = new HashMap<KID, AuthData>();
		}
		
		if (!rootAuthDataByEnvId.containsKey(env.getId()))
		{
			AuthData rootAuthData = new AuthData();
			rootAuthData.setEnvId(env.getId());
			rootAuthData.setEnvName(env.getName());
			
			Profile sysAdminProfile = new Profile();
			sysAdminProfile.setId(KID.get(Profile.ROOT_ID));
			sysAdminProfile.setName(Profile.ROOT_NAME);
			rootAuthData.setProfile(sysAdminProfile);
			
			User root = new User();
			root.setId(AppConfig.getRootUserId());
			root.setEmail(BasicSetupService.ROOT_USER_EMAIL);
			root.setUserName(BasicSetupService.ROOT_USERNAME);
			root.setId(AppConfig.getRootUserId());
			root.setLocale(Locale.EN_US.name());
			root.setProfile(sysAdminProfile);
			rootAuthData.setUser(root);
			
			rootAuthDataByEnvId.put(env.getId(), rootAuthData);
		}
		
		return rootAuthDataByEnvId.get(env.getId());
	}
	
	public AuthData (User user, EnvData env, PermissionService permissionService, KommetCompiler compiler) throws KommetException
	{
		// generate random session ID
		this.sessionId = MiscUtils.getHash(50);
		
		this.permissionService = permissionService;
		
		Profile profile = user.getProfile(); 
		if (profile == null)
		{
			throw new KommetException("User's profile not set");
		}
		
		this.profile = profile;
		this.user = user;
		this.envId = env.getId();
	}
	
	/**
	 * Initializes permissions for the given user.
	 * 
	 * This method takes a user as a parameter, not only their profile, because in the future we might want
	 * to introduce permissions granted per user, not per profile.
	 * 
	 * @param user
	 * @param env
	 * @param permissionService
	 * @throws KommetException
	 */
	public void initUserPermissions(EnvData env) throws KommetException
	{
		this.typePermissionsByTypeId = new ConcurrentHashMap<KID, TypePermission>();
		List<TypePermission> typePermissionsForProfile = permissionService.getTypePermissionForProfile(this.profile.getId(), env);
		for (TypePermission permission : typePermissionsForProfile)
		{
			this.typePermissionsByTypeId.put(permission.getTypeId(), permission);
		}
		
		this.fieldPermissionsByFieldId = new ConcurrentHashMap<KID, FieldPermission>();
		List<FieldPermission> fieldPermissionsForProfile = permissionService.getFieldPermissionForProfile(this.profile.getId(), env);
		for (FieldPermission permission : fieldPermissionsForProfile)
		{
			this.fieldPermissionsByFieldId.put(permission.getFieldId(), permission);
		}
		
		this.actionPermissions = new ConcurrentHashMap<String, ActionPermission>();
		List<ActionPermission> actionPermissionsForProfile = permissionService.getActionPermissionForProfile(this.profile.getId(), env);
		for (ActionPermission permission : actionPermissionsForProfile)
		{
			this.actionPermissions.put(permission.getAction().getUrl(), permission);
		}
		
		long lastPermissionUpdate = (new Date()).getTime();
		this.lastFieldPermissionsUpdate = lastPermissionUpdate;
		this.lastTypePermissionsUpdate = lastPermissionUpdate;
		this.lastActionPermissionsUpdate = lastPermissionUpdate;
	}
	
	public static AuthData current()
	{
		RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
		
		// get current auth data
		return AuthUtil.getAuthData((HttpSession)requestAttributes.resolveReference(RequestAttributes.REFERENCE_SESSION));
	}
	
	public void addUserPermissionSet (Record permissionSet) throws KommetException
	{
		if (this.permissionSetsByName == null)
		{
			this.permissionSetsByName = new HashMap<String, Record>();
		}
		this.permissionSetsByName.put((String)permissionSet.getField("name"), permissionSet);
	}

	public void setEnvName(String envName)
	{
		this.envName = envName;
	}

	public String getEnvName()
	{
		return envName;
	}

	public void setEnvId(KID envId)
	{
		this.envId = envId;
	}

	public KID getEnvId()
	{
		return envId;
	}

	public boolean getUserPermissionSet (String permissionSet)
	{
		return this.permissionSetsByName != null && this.permissionSetsByName.containsKey(permissionSet);
	}

	public void setUser(User user)
	{
		this.user = user;
	}

	public User getUser()
	{
		return user;
	}
	
	public KID getUserId() throws KommetException
	{
		return this.user != null ? this.user.getId() : null;
	}
	
	public boolean canAccessAction(String pageUrl) throws KommetException
	{
		return canAccessAction(pageUrl, false, null);
	}
	
	public synchronized void updateTypePermissions(boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		// if permissions stored in this auth data object have been synced before last permission
		// update on the env, sync them again
		if (refreshIfOutdated && env.getLastTypePermissionsUpdate() > this.lastTypePermissionsUpdate)
		{
			initUserPermissions(env);
		}
	}
	
	public boolean isTypePermissionsOutOfDate(EnvData env)
	{
		return env.getLastTypePermissionsUpdate() > this.lastTypePermissionsUpdate;
	}
	
	public synchronized void updateFieldPermissions(boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		// if permissions stored in this auth data object have been synced before last permission
		// update on the env, sync them again
		if (refreshIfOutdated && env.getLastFieldPermissionsUpdate() > this.lastFieldPermissionsUpdate)
		{
			initUserPermissions(env);
		}
	}
	
	public synchronized void updateActionPermissions(boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		// if permissions stored in this auth data object have been synced before last permission
		// update on the env, sync them again
		if (refreshIfOutdated && env.getLastActionPermissionsUpdate() > this.lastActionPermissionsUpdate)
		{
			initUserPermissions(env);
		}
	}
	
	private boolean isAdminBackdoor(EnvData env) throws KommetException
	{
		return profile.getId().equals(env.getRootUser().getField("profile.id"));
	}
	
	private boolean isFieldBackdoor(Field field) throws KommetException
	{
		return Field.isSystemField(field.getApiName());
	}

	public boolean canReadType(KID typeId, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		if (isAdminBackdoor(env) || isSysAdminBackdoor(typeId, env))
		{
			return true;
		}
		updateTypePermissions(refreshIfOutdated, env);
		return this.typePermissionsByTypeId.containsKey(typeId) && this.typePermissionsByTypeId.get(typeId).getRead();
	}
	
	private boolean isSysAdminBackdoor(KID typeId, EnvData env) throws KommetException
	{
		return this.getProfile().getId().getId().equals(Profile.SYSTEM_ADMINISTRATOR_ID) && env.getType(typeId).isAccessible();
	}

	/**
	 * Tells whether user can read all records of the given type.
	 * @param rid
	 * @param refreshIfOutdated
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public boolean canReadAllType(KID typeId, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		if (isAdminBackdoor(env) || isSysAdminBackdoor(typeId, env))
		{
			return true;
		}
		updateTypePermissions(refreshIfOutdated, env);
		return this.typePermissionsByTypeId.containsKey(typeId) && this.typePermissionsByTypeId.get(typeId).getReadAll();
	}
	
	public boolean canEditAllType(KID typeId, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		if (isAdminBackdoor(env) || isSysAdminBackdoor(typeId, env))
		{
			return true;
		}
		updateTypePermissions(refreshIfOutdated, env);
		return this.typePermissionsByTypeId.containsKey(typeId) && this.typePermissionsByTypeId.get(typeId).getEditAll();
	}
	
	public boolean canDeleteAllType(KID typeId, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		if (isAdminBackdoor(env) || isSysAdminBackdoor(typeId, env))
		{
			return true;
		}
		updateTypePermissions(refreshIfOutdated, env);
		return this.typePermissionsByTypeId.containsKey(typeId) && this.typePermissionsByTypeId.get(typeId).getDeleteAll();
	}
	
	public boolean canCreateType(KID typeId, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		if (isAdminBackdoor(env)  || isSysAdminBackdoor(typeId, env))
		{
			return true;
		}
		updateTypePermissions(refreshIfOutdated, env);
		return this.typePermissionsByTypeId.containsKey(typeId) && this.typePermissionsByTypeId.get(typeId).getCreate();
	}
	
	public boolean canEditType(KID typeId, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		if (isAdminBackdoor(env)  || isSysAdminBackdoor(typeId, env))
		{
			return true;
		}
		updateTypePermissions(refreshIfOutdated, env);
		return this.typePermissionsByTypeId.containsKey(typeId) && this.typePermissionsByTypeId.get(typeId).getEdit();
	}
	
	public boolean canDeleteType(KID typeId, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		if (isAdminBackdoor(env) || isSysAdminBackdoor(typeId, env))
		{
			return true;
		}
		updateTypePermissions(refreshIfOutdated, env);
		return this.typePermissionsByTypeId.containsKey(typeId) && this.typePermissionsByTypeId.get(typeId).getDelete();
	}
	
	/**
	 * Check if a user has permissions to read values of a given field.
	 * @param field
	 * @param refreshIfOutdated
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public boolean canReadField(Field field, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		if (field == null)
		{
			throw new KommetException("Field is null");
		}
		
		Type type = field.getType();
		if (type == null)
		{
			throw new KommetException("Cannot determine field read access because the type on field is null");
		}
		else if (type.getDefaultFieldId() == null)
		{
			throw new KommetException("Cannot determine field read access because the default field on type is null");
		}
		
		// if the user's profile has read access on the type, then they also need to be able to see the default field
		if (type.getDefaultFieldId().equals(field.getKID()) && canReadType(field.getType().getKID(), false, env))
		{
			return true;
		}
		
		if (isAdminBackdoor(env) || isSysAdminBackdoor(field.getType().getKID(), env))
		{
			return true;
		}
		
		updateFieldPermissions(refreshIfOutdated, env);
		return isFieldBackdoor(field) || (this.fieldPermissionsByFieldId.containsKey(field.getKID()) && this.fieldPermissionsByFieldId.get(field.getKID()).getRead());
	}
	
	public boolean canEditField(Field field, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		if (isAdminBackdoor(env) || isSysAdminBackdoor(field.getType().getKID(), env))
		{
			return true;
		}
		updateFieldPermissions(refreshIfOutdated, env);
		return isFieldBackdoor(field) || (this.fieldPermissionsByFieldId.containsKey(field.getKID()) && this.fieldPermissionsByFieldId.get(field.getKID()).getEdit());
	}

	public boolean canAccessAction(String actionUrl, boolean refreshIfOutdated, EnvData env) throws KommetException
	{
		updateActionPermissions(refreshIfOutdated, env);
		return this.actionPermissions.containsKey(actionUrl) && this.actionPermissions.get(actionUrl).getRead();
	}

	public boolean hasProfile(String profile) throws KommetException
	{
		// check if user is not null - it can be null is this is guest auth data
		return this.user != null && this.user.getProfile().getName().equals(profile);
	}
	
	public void setProfile(Profile profile)
	{
		this.profile = profile;
	}

	public Profile getProfile()
	{
		return profile;
	}

	public void setI18n(I18nDictionary i18n)
	{
		this.i18n = i18n;
	}

	public I18nDictionary getI18n()
	{
		return i18n;
	}

	public void setUserSettings(UserSettings userSettings)
	{
		this.userSettings = userSettings;
	}

	public UserSettings getUserSettings()
	{
		return userSettings;
	}

	public Locale getLocale()
	{
		return this.user != null ? this.user.getLocaleSetting() : null;
	}
	
	public Integer getTimeZoneOffset()
	{	
		if (this.user == null || this.user.getTimezone() == null)
		{
			return null;
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone(this.user.getTimezone()));
		return cal.getTimeZone().getOffset(new Date().getTime()) / 3600000;
	}

	public boolean isGuest()
	{
		return isGuest;
	}

	public void setGuest(boolean isGuest)
	{
		this.isGuest = isGuest;
	}

	public static AuthData getGuestAuthData (PermissionService permissionService, UserCascadeHierarchyService uchService, EnvData env) throws KommetException
	{
		if (guestAuthDataByEnvId == null)
		{
			guestAuthDataByEnvId = new HashMap<KID, AuthData>();
		}
		
		if (env == null)
		{
			AuthData authData = new AuthData();
			authData.setPermissionService(permissionService);
			authData.setGuest(true);
			User guestUser = new User();
			guestUser.setLocale(Locale.EN_US.name());
			
			// get unauthenticated profile manually, not from DB, since we don't know which environment
			// to connect to
			Profile unauthenticatedProfile = new Profile();
			unauthenticatedProfile.setId(KID.get(Profile.UNAUTHENTICATED_ID));
			unauthenticatedProfile.setName(Profile.UNAUTHENTICATED_NAME);
			authData.setProfile(unauthenticatedProfile);
			authData.setUser(guestUser);
			
			return authData;
		}
		
		AuthData authData = guestAuthDataByEnvId.get(env.getId());
		
		// if auth data is not set or it is out of date, initialize it
		if (authData == null || authData.isTypePermissionsOutOfDate(env))
		{
			authData = new AuthData();
			authData.setPermissionService(permissionService);
			authData.setGuest(true);
			
			User guestUser = env.getGuestUser();
			
			if (guestUser == null)
			{
				throw new KommetException("Guest user not found on env object for env " + env.getId());
			}
			
			authData.setProfile(guestUser.getProfile());
			authData.setUser(guestUser);
			
			// guest users have profile unauthenticate user
			authData.initUserPermissions(env);
			authData.initUserCascadeSettings(uchService, env);
			
			guestAuthDataByEnvId.put(env.getId(), authData);
		}
		
		return guestAuthDataByEnvId.get(env.getId());
	}
	
	public void setPermissionService (PermissionService permissionService)
	{
		this.permissionService = permissionService;
	}
	
	/**
	 * Tells if this user can configure the env. System admins and roots can do that.
	 * @return
	 * @throws KIDException
	 */
	public boolean hasConfigPermission() throws KIDException
	{
		return AuthUtil.isRoot(this) || KID.get(Profile.SYSTEM_ADMINISTRATOR_ID).equals(this.getProfile().getId());
	}

	public UIConfig getUIConfig()
	{
		return UIConfig;
	}

	public void setUIConfig(UIConfig uIConfig)
	{
		UIConfig = uIConfig;
	}

	public Map<String, String> getUserCascadeSettings()
	{
		return userCascadeSettings;
	}

	public void setUserCascadeSettings(Map<String, String> userCascadeSettings)
	{
		this.userCascadeSettings = userCascadeSettings;
	}

	/**
	 * Initializes the user cascade settings for the current user.
	 * @param uchService
	 * @param env
	 * @throws KommetException
	 */
	public void initUserCascadeSettings (UserCascadeHierarchyService uchService, EnvData env) throws KommetException
	{
		// find all user cascade settings
		// TODO make this method more optimal because it is called very often - every time a user logs in
		
		this.userCascadeSettings = uchService.getSettingsAsMap(null, this, AuthData.getRootAuthData(env), env);
	}

	public String getSessionId()
	{
		return sessionId;
	}

	public void setSessionId(String sessionId)
	{
		this.sessionId = sessionId;
	}
}
