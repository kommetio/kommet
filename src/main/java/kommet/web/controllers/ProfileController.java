/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.Profile;
import kommet.basic.TypePermission;
import kommet.basic.UserSettings;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.security.RestrictedAccess;
import kommet.utils.AppConfig;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class ProfileController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	UserService userService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	AppConfig appConfig;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profiles/new", method = RequestMethod.GET)
	public ModelAndView newProfile() throws KommetException
	{
		ModelAndView mv = new ModelAndView("profiles/edit");
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profile/clonepermissions", method = RequestMethod.POST)
	public ModelAndView cloneProfilePermissions (@RequestParam(value = "sourceProfileId", required = false) String sSourceProfileId,
									@RequestParam(value = "destProfileId", required = false) String sDestProfileId,
									HttpSession session) throws KommetException
	{
		clearMessages();
		KID sourceProfileId = null;
		KID destProfileId = null;
		
		try
		{
			sourceProfileId = KID.get(sSourceProfileId);
		}
		catch (KIDException e)
		{
			addError("Invalid source profile ID " + sSourceProfileId);
		}
		
		try
		{
			destProfileId = KID.get(sDestProfileId);
		}
		catch (KIDException e)
		{
			addError("Invalid destination profile ID " + sDestProfileId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("profiles/typepermissions");
		
		if (hasErrorMessages())
		{
			mv = prepareProfileTypePermissions(mv, destProfileId, null, null, env);
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		// clone permissions from one profile to another
		profileService.clonePermissions(sourceProfileId, destProfileId, false, AuthUtil.getAuthData(session), env);
		
		mv = prepareProfileTypePermissions(mv, destProfileId, null, null, env);
		mv.addObject("actionMsgs", getMessage("Permissions successfully cloned"));
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profile/save", method = RequestMethod.POST)
	public ModelAndView saveProfile (@RequestParam(value = "profileId", required = false) String id,
									@RequestParam(value = "name", required = false) String name,
									@RequestParam(value = "label", required = false) String label,
									@RequestParam(value = "landingURL", required = false) String landingURL,
									HttpSession session)
									throws KommetException
	{
		clearMessages();
		
		Profile profile = null;
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (StringUtils.hasText(id))
		{
			KID profileId = null;
			try
			{
				profileId = KID.get(id);
			}
			catch (KIDException e)
			{
				return getErrorPage("Invalid profile ID " + id);
			}
			
			profile = profileService.getProfile(profileId, env);
			if (profile == null)
			{
				getErrorPage("No profile found with ID " + id);
			}
			
			if (profile.getSystemProfile() == true)
			{
				return getErrorPage("Profile " + profile.getLabel() + " is a system profile and cannot be edited");
			}
		}
		else
		{
			profile = new Profile();
			profile.setSystemProfile(false);
		}
		
		ModelAndView mv = new ModelAndView("profiles/edit");
		
		if (!StringUtils.hasText(name))
		{
			addError("Name cannot be empty");
		}
		else
		{
			profile.setName(name);
			
			if (profile.getId() == null && profileService.getProfileByName(name, env) != null)
			{
				// make sure a profile with this name does not already exist
				// this is also checked by a unique check
				addError("A profile with name '" + name + "' already exists");
			}
			else if (!ValidationUtil.isValidOptionallyQualifiedResourceName(name))
			{
				addError("Invalid profile name");
			}
		}
		
		if (!StringUtils.hasText(label))
		{
			addError("Label cannot be empty");
		}
		else
		{
			profile.setLabel(label);
			
			if (profile.getId() == null && profileService.getProfileByLabel(label, authData, env) != null)
			{
				addError("A profile with label '" + label + "' already exists");
			}
		}
		
		if (hasErrorMessages())
		{
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("profile", profile);
			return mv;
		}
		
		// save the record
		profile = profileService.save(profile, authData, env);
		
		// if any user settings for this profile are set, update user settings
		if (landingURL != null)
		{
			UserSettings profileSettings = userService.getProfileSettings(profile.getId(), env);
			
			// if profile settings for this profile don't exist, create them
			if (profileSettings == null)
			{
				profileSettings = new UserSettings();
				profileSettings.setProfile(profile);
			}
			
			profileSettings.setLandingURL(landingURL);
			userService.save(profileSettings, AuthUtil.getAuthData(session), env);
		}
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profile/" + profile.getId());	
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profiles/typepermissions/{id}", method = RequestMethod.GET)
	public ModelAndView showProfileTypePermissions(@PathVariable("id") String sProfileId, HttpSession session) throws KommetException
	{
		KID profileId = null;
		try
		{
			profileId = KID.get(sProfileId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid profile id " + sProfileId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		ModelAndView mv = new ModelAndView("profiles/typepermissions");
		return prepareProfileTypePermissions(mv, profileId, null, null, env);
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profiles/fieldpermissions", method = RequestMethod.GET)
	public ModelAndView showProfileFieldPermissions(@RequestParam(value = "typeId", required = false) String sTypeId,
													@RequestParam(value = "profileId", required = false) String sProfileId,
													HttpSession session) throws KommetException
	{
		clearMessages();
		KID profileId = null;
		KID typeId = null;
		
		try
		{
			profileId = KID.get(sProfileId);
		}
		catch (KIDException e)
		{
			addError("Invalid profile id " + sProfileId);
		}
		
		try
		{
			typeId = KID.get(sTypeId);
		}
		catch (KIDException e)
		{
			addError("Invalid type id " + sProfileId);
		}
		
		if (hasErrorMessages())
		{
			return getErrorPage(getErrorMsgs());
		}
		
		EnvData env = envService.getCurrentEnv(session);
		ModelAndView mv = new ModelAndView("profiles/fieldpermissions");
		return prepareProfileFieldPermissions(mv, profileId, typeId, env);
	}
	
	private ModelAndView prepareProfileFieldPermissions(ModelAndView mv, KID profileId, KID typeId, EnvData env) throws KommetException
	{
		mv.addObject("profile", profileService.getProfile(profileId, env));
		mv.addObject("type", env.getType(typeId));
		
		return mv;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profiles/edittypepermissions/{id}", method = RequestMethod.GET)
	public ModelAndView editProfileTypePermissions(@PathVariable("id") String sProfileId, HttpSession session) throws KommetException
	{
		KID profileId = null;
		try
		{
			profileId = KID.get(sProfileId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid profile id " + sProfileId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		ModelAndView mv = new ModelAndView("profiles/edittypepermissions");
		return prepareProfileTypePermissions(mv, profileId, null, null, env);
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profiles/typepermissions/save", method = RequestMethod.POST)
	public ModelAndView saveProfileTypePermissions(@RequestParam("profileId") String sProfileId, HttpServletRequest req, HttpSession session) throws KommetException
	{
		clearMessages();
		KID profileId = null;
		try
		{
			profileId = KID.get(sProfileId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid profile id " + sProfileId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		Profile profile = profileService.getProfile(profileId, env);
		
		Map<KID, TypePermission> typePermissionsByType = getEmptyTypePermissions(profile, env);
		
		for (TypePermission permission : permissionService.getTypePermissionForProfile(profileId, env))
		{
			// Since checkbox values for granted permission access are only passed when the checkbox is
			// selected, unselected checkboxes are not passed at all and we would not know
			// that the given permission should be revoked.
			// This is why at the beginning we set all permissions to false.
			permission.setRead(false);
			permission.setEdit(false);
			permission.setDelete(false);
			permission.setCreate(false);
			permission.setReadAll(false);
			permission.setEditAll(false);
			permission.setDeleteAll(false);
			typePermissionsByType.put((KID)permission.getTypeId(), permission);
		}
		
		// now rewrite assigned permissions from request
		Enumeration<String> paramNames = req.getParameterNames();
		while (paramNames.hasMoreElements())
		{
			String param = paramNames.nextElement();
			if (param.startsWith("perm_"))
			{
				String[] val = req.getParameterValues(param);
				
				if (val != null && val.length > 0)
				{
					String fieldName = param.replaceFirst("perm_", ""); 
					if (val.length > 1)
					{
						throw new KommetException("Field " + fieldName + " appears more than once on the page");
					}
					
					String[] objPermissionIndex = fieldName.split("_");
					KID typeId = KID.get(objPermissionIndex[0]);
					
					// check if the type permission for this type and profile already exists
					TypePermission permission = typePermissionsByType.get(typeId);
					if (permission == null)
					{
						throw new KommetException("Empty type permission not created before save was executed");
					}
					
					String accessType = objPermissionIndex[1];
					if ("read".equals(accessType))
					{
						permission.setRead(Boolean.valueOf(val[0]));
					}
					else if ("edit".equals(accessType))
					{
						permission.setEdit(Boolean.valueOf(val[0]));
					}
					else if ("delete".equals(accessType))
					{
						permission.setDelete(Boolean.valueOf(val[0]));
					}
					else if ("create".equals(accessType))
					{
						permission.setCreate(Boolean.valueOf(val[0]));
					}
					else if ("readAll".equals(accessType))
					{
						permission.setReadAll(Boolean.valueOf(val[0]));
					}
					else if ("editAll".equals(accessType))
					{
						permission.setEditAll(Boolean.valueOf(val[0]));
					}
					else if ("deleteAll".equals(accessType))
					{
						permission.setDeleteAll(Boolean.valueOf(val[0]));
					}
					else
					{
						throw new KommetException("Unknown access type '" + accessType + "'");
					}
					
					typePermissionsByType.put(typeId, permission);
				}
			}
		}
		
		// save updated type permissions
		for (TypePermission permission : typePermissionsByType.values())
		{
			permissionService.save(permission, AuthUtil.getAuthData(session), env);
		}
		
		env.setLastTypePermissionsUpdate((new Date()).getTime());
		
		// show type permission details
		ModelAndView mv = new ModelAndView("profiles/typepermissions");
		List<TypePermission> permissionList = new ArrayList<TypePermission>();
		permissionList.addAll(typePermissionsByType.values());
		return prepareProfileTypePermissions(mv, profileId, profile, permissionList, env);
	}
	
	private ModelAndView prepareProfileTypePermissions(ModelAndView mv, KID profileId, Profile profile, List<TypePermission> typePermissions, EnvData env) throws KommetException
	{
		if (profile == null)
		{
			profile = profileService.getProfile(profileId, env);
		}
		
		if (typePermissions == null)
		{
			Map<KID, TypePermission> typePermissionsByType = getEmptyTypePermissions(profile, env);
			for (TypePermission perm : permissionService.getTypePermissionForProfile(profileId, env))
			{
				if (env.getType(perm.getTypeId()).isAccessible())
				{
					typePermissionsByType.put(perm.getTypeId(), perm);
				}
			}
			
			typePermissions = new ArrayList<TypePermission>();
			typePermissions.addAll(typePermissionsByType.values());
		}
		
		mv.addObject("profile", profile);
		
		// display only non-system profiles as option for cloning, so that users cannot clone system profiles
		mv.addObject("profiles", getProfilesForDisplay(env));
		mv.addObject("typePermissions", wrapTypePermissions(typePermissions, env));
		return mv;
	}
	
	private Map<KID, TypePermission> getEmptyTypePermissions(Profile profile, EnvData env) throws KommetException
	{
		Map<KID, TypePermission> typePermissionsByType = new HashMap<KID, TypePermission>();
		Collection<Type> typesForPermissions = env.getAccessibleTypes();
		
		// allow for editing permissions for type "Report Type", "Comment" and "Notification"
		typesForPermissions.add(env.getType(KeyPrefix.get(KID.REPORT_TYPE_PREFIX)));
		typesForPermissions.add(env.getType(KeyPrefix.get(KID.COMMENT_PREFIX)));
		typesForPermissions.add(env.getType(KeyPrefix.get(KID.NOTIFICATION_PREFIX)));
		typesForPermissions.add(env.getType(KeyPrefix.get(KID.TASK_PREFIX)));
		typesForPermissions.add(env.getType(KeyPrefix.get(KID.EVENT_PREFIX)));
		typesForPermissions.add(env.getType(KeyPrefix.get(KID.EVENT_GUEST_PREFIX)));
		typesForPermissions.add(env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)));
		
		for (Type type : typesForPermissions)
		{
			TypePermission emptyPermission = new TypePermission();
			emptyPermission.setRead(false);
			emptyPermission.setEdit(false);
			emptyPermission.setDelete(false);
			emptyPermission.setCreate(false);
			emptyPermission.setReadAll(false);
			emptyPermission.setEditAll(false);
			emptyPermission.setDeleteAll(false);
			emptyPermission.setTypeId(type.getKID());
			emptyPermission.setProfile(profile);
			typePermissionsByType.put(type.getKID(), emptyPermission);
		}
		
		// add permissions for user and file types
		TypePermission emptyFilePermission = new TypePermission();
		emptyFilePermission.setRead(false);
		emptyFilePermission.setEdit(false);
		emptyFilePermission.setDelete(false);
		emptyFilePermission.setCreate(false);
		emptyFilePermission.setReadAll(false);
		emptyFilePermission.setEditAll(false);
		emptyFilePermission.setDeleteAll(false);
		KID fileTypeId = env.getType(KeyPrefix.get(KID.FILE_PREFIX)).getKID();
		emptyFilePermission.setTypeId(fileTypeId);
		emptyFilePermission.setProfile(profile);
		typePermissionsByType.put(fileTypeId, emptyFilePermission);
		
		TypePermission emptyUserPermission = new TypePermission();
		emptyUserPermission.setRead(false);
		emptyUserPermission.setEdit(false);
		emptyUserPermission.setDelete(false);
		emptyUserPermission.setCreate(false);
		emptyUserPermission.setReadAll(false);
		emptyUserPermission.setEditAll(false);
		emptyUserPermission.setDeleteAll(false);
		KID userTypeId = env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID();
		emptyUserPermission.setTypeId(userTypeId);
		emptyUserPermission.setProfile(profile);
		typePermissionsByType.put(userTypeId, emptyUserPermission);
		
		return typePermissionsByType;
	}

	private List<TypePermissionWrapper> wrapTypePermissions(List<TypePermission> permissions, EnvData env) throws KommetException
	{
		List<TypePermissionWrapper> wrappers = new ArrayList<ProfileController.TypePermissionWrapper>();
		
		for (TypePermission permission : permissions)
		{
			wrappers.add(new TypePermissionWrapper(permission, env.getType(permission.getTypeId())));
		}
		
		return wrappers;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profile/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("id") String id, HttpSession session) throws KommetException
	{
		KID profileId = null;
		try
		{
			profileId = KID.get(id);
		}
		catch (KIDException e)
		{
			e.printStackTrace();
			return getErrorPage("Invalid object id " + id);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("profiles/details");
		mv = prepareProfileDetails(profileId, false, mv, env);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profiles/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("id") String id, HttpSession session) throws KommetException
	{
		KID profileId = null;
		try
		{
			profileId = KID.get(id);
		}
		catch (KIDException e)
		{
			e.printStackTrace();
			return getErrorPage("Invalid object id " + id);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("profiles/edit");
		mv = prepareProfileDetails(profileId, true, mv, env);
		mv.addObject("layouts", layoutService.find(null, env));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profiles/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session, HttpServletRequest req) throws KommetException
	{ 	
		ModelAndView mv = new ModelAndView("profiles/list");
		EnvData env = envService.getCurrentEnv(session);
		
		mv.addObject("profiles", getProfilesForDisplay(env));
		
		Breadcrumbs.add(req.getRequestURL().toString(), "Profiles", appConfig.getBreadcrumbMax(), session);
		
		return mv;
	}
	
	private List<Profile> getProfilesForDisplay(EnvData env) throws KommetException
	{
		List<Profile> nonSystemProfiles = new ArrayList<Profile>();
		
		for (Profile p : profileService.getProfiles(env))
		{
			if (!p.getName().equals(Profile.ROOT_NAME) && !p.getName().equals(Profile.UNAUTHENTICATED_NAME))
			{
				nonSystemProfiles.add(p);
			}
		}
		
		return nonSystemProfiles;
	}

	public class TypePermissionWrapper
	{
		private Type type;
		private TypePermission permission;

		public TypePermissionWrapper (TypePermission permission, Type type) throws KommetException
		{
			this.permission = permission;
			this.type = type;
		}

		public Type getType()
		{
			return type;
		}

		public TypePermission getPermission()
		{
			return permission;
		}	
	}
	
	private ModelAndView prepareProfileDetails (KID id, boolean isEditMode, ModelAndView mv, EnvData env) throws KommetException
	{
		Profile profile = profileService.getProfile(id, env);
		
		if (profile == null)
		{
			return getErrorPage("No profile found with ID " + id);
		}
		
		if (profile.getSystemProfile() == true && !profile.getName().equals(Profile.SYSTEM_ADMINISTRATOR_NAME))
		{
			return getErrorPage("Profile " + profile.getLabel() + " is a system profile and cannot be edited");
		}
		
		mv.addObject("profile", profile);
		mv.addObject("profileSettings", userService.getProfileSettings(id, env));
		return mv;
	}
}