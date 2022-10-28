/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.basic.ActionPermission;
import kommet.basic.FieldPermission;
import kommet.basic.Profile;
import kommet.basic.TypePermission;
import kommet.basic.types.SystemTypes;
import kommet.dao.ActionPermissionDao;
import kommet.dao.FieldPermissionDao;
import kommet.dao.ProfileDao;
import kommet.dao.TypePermissionDao;
import kommet.dao.UserDao;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.exceptions.NotImplementedException;
import kommet.filters.ProfileFilter;
import kommet.filters.UserFilter;
import kommet.utils.ValidationUtil;

@Service
public class ProfileService
{
	@Inject
	ProfileDao profileDao;
	
	@Inject
	TypePermissionDao typePermissionDao;
	
	@Inject
	FieldPermissionDao fieldPermissionDao;
	
	@Inject
	ActionPermissionDao actionPermissionDao;
	
	@Inject
	DataService dataService;
	
	@Inject
	UserDao userDao;
	
	@Inject
	PermissionService permissionService;
	
	@Transactional(readOnly = true)
	public Record getProfileRecordByName (String profileName, EnvData env) throws KommetException
	{
		List<Record> profiles = env.getSelectCriteriaFromDAL("select id, name, label, systemProfile from " + SystemTypes.PROFILE_API_NAME + " where name = '" + profileName + "'").list();
		
		if (profiles.size() > 1)
		{
			throw new KommetException("More than one profile found with name " + profileName);
		}
		
		return profiles.isEmpty() ? null : profiles.get(0);
	}
	
	@Transactional(readOnly = true)
	public List<Profile> find (ProfileFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return profileDao.find(filter, authData, env);
	}
	
	private Record getProfileRecord (KID profileId, EnvData env) throws KommetException
	{
		List<Record> profiles = env.getSelectCriteriaFromDAL("select id, name, label, systemProfile, createdDate, createdBy.id from " + SystemTypes.PROFILE_API_NAME + " where " + Field.ID_FIELD_NAME + " = '" + profileId + "'").list();
		
		if (profiles.size() > 1)
		{
			throw new KommetException("More than one profile found with ID " + profileId);
		}
		
		return profiles.isEmpty() ? null : profiles.get(0);
	}
	
	public Profile getProfileByName (String profileName, EnvData env) throws KommetException
	{
		Record profileRecord = getProfileRecordByName(profileName, env);
		return profileRecord != null ? new Profile(profileRecord, env) : null;
	}
	
	public Profile getProfile (KID profileId, EnvData env) throws KommetException
	{
		Record profileRecord = getProfileRecord(profileId, env);
		return profileRecord != null ? new Profile(profileRecord, env) : null;
	}
	
	@Transactional(readOnly = true)
	public List<Profile> getProfiles(EnvData env) throws KommetException
	{
		List<Record> profileObjs = env.getSelectCriteriaFromDAL("select id, name, label, systemProfile, createdDate, createdBy.id from " + SystemTypes.PROFILE_API_NAME).list();
		List<Profile> profiles = new ArrayList<Profile>();
		
		for (Record r : profileObjs)
		{
			profiles.add(new Profile(r, env));
		}
		
		return profiles;
	}

	@Transactional
	public Profile save(Profile profile, AuthData authData, EnvData env) throws KommetException
	{
		boolean isNew = profile.getId() == null;
		
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(profile.getName()))
		{
			throw new KommetException("Invalid profile name " + profile.getName());
		}
	
		profile = profileDao.save(profile, authData, env);
		
		if (isNew)
		{
			setDefaultPermissions(profile, authData, env);
		}
		
		return profile;
	}

	/**
	 * Sets default permissions any profile must have.
	 * @param profile
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	private void setDefaultPermissions(Profile profile, AuthData authData, EnvData env) throws KommetException
	{
		// set default permissions on the FileObjectAssociation type
		Type fileAssocType = env.getType(KeyPrefix.get(KID.FILE_RECORD_ASSIGNMENT_PREFIX));
		permissionService.setTypePermissionForProfile(profile.getId(), fileAssocType.getKID(), true, true, true, true, true, false, false, authData, env);
		
		Type fileType = env.getType(KeyPrefix.get(KID.FILE_PREFIX));
		permissionService.setTypePermissionForProfile(profile.getId(), fileType.getKID(), true, true, true, true, false, false, false, authData, env);
		
		Type notificationType = env.getType(KeyPrefix.get(KID.NOTIFICATION_PREFIX));
		permissionService.setTypePermissionForProfile(profile.getId(), notificationType.getKID(), true, false, false, false, false, false, false, authData, env);
		
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		permissionService.setTypePermissionForProfile(profile.getId(), userType.getKID(), true, false, false, false, false, false, false, authData, env);
		
		Type eventType = env.getType(KeyPrefix.get(KID.EVENT_PREFIX));
		permissionService.setTypePermissionForProfile(profile.getId(), eventType.getKID(), true, false, false, true, false, false, false, authData, env);
		
		Type eventGuestType = env.getType(KeyPrefix.get(KID.EVENT_GUEST_PREFIX));
		permissionService.setTypePermissionForProfile(profile.getId(), eventGuestType.getKID(), true, false, false, true, false, false, false, authData, env);
		
		Type taskType = env.getType(KeyPrefix.get(KID.TASK_PREFIX));
		permissionService.setTypePermissionForProfile(profile.getId(), taskType.getKID(), true, false, false, true, false, false, false, authData, env);
	}
	
	@Transactional
	public void setUnauthenticatedProfileDefaultPermissions(AuthData authData, EnvData env) throws KommetException
	{
		Profile profile = getProfileByName(Profile.UNAUTHENTICATED_NAME, env);
		
		Type fileType = env.getType(KeyPrefix.get(KID.FILE_PREFIX));
		permissionService.setTypePermissionForProfile(profile.getId(), fileType.getKID(), true, false, false, false, false, false, false, authData, env);
		
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		permissionService.setTypePermissionForProfile(profile.getId(), userType.getKID(), true, false, false, false, false, false, false, authData, env);
	}

	/**
	 * Clones type, field and page permissions from one profile to another.
	 * @param sourceProfileId
	 * @param destProfileId
	 * @param keepOriginalPermissions
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	@Transactional
	public void clonePermissions(KID sourceProfileId, KID destProfileId, boolean keepOriginalPermissions, AuthData authData, EnvData env) throws KommetException
	{	
		if (keepOriginalPermissions == true)
		{
			throw new NotImplementedException("Merging permissions is not yet supported");
		}
		
		if (sourceProfileId == null)
		{
			throw new KommetException("Source profile ID is empty");
		}
		
		if (destProfileId == null)
		{
			throw new KommetException("Target profile ID is empty");
		}
		
		if (sourceProfileId.equals(destProfileId))
		{
			throw new KommetException("Cannot clone profile permissions onto the same profile");
		}
		
		Profile destProfile = new Profile();
		destProfile.setId(destProfileId);
		
		// find all type permissions for source profile
		List<TypePermission> sourceTypePermissions = typePermissionDao.getForProfile(sourceProfileId, env);
		
		// find all field permissions for source profile
		List<FieldPermission> sourceFieldPermissions = fieldPermissionDao.getForProfile(sourceProfileId, env);
		
		// find all page permissions for source profile
		List<ActionPermission> sourceActionPermissions = actionPermissionDao.getForProfile(sourceProfileId, env);
		
		// if old permissions on the destination profile should not be kept
		// we need to find and delete them
		if (!keepOriginalPermissions)
		{
			deleteProfilePermissions(destProfileId, authData, env);
		}
		
		// clone type permissions
		for (TypePermission permission : sourceTypePermissions)
		{
			// clone each type permission
			TypePermission clonedPermission = new TypePermission();
			clonedPermission.setCreate(permission.getCreate());
			clonedPermission.setDelete(permission.getDelete());
			clonedPermission.setRead(permission.getRead());
			clonedPermission.setEdit(permission.getEdit());
			clonedPermission.setReadAll(permission.getReadAll());
			clonedPermission.setEditAll(permission.getEditAll());
			clonedPermission.setDeleteAll(permission.getDeleteAll());
			clonedPermission.setTypeId(permission.getTypeId());
			clonedPermission.setProfile(destProfile);
			
			// save new permission
			typePermissionDao.save(clonedPermission, authData, env);
		}
		
		// clone field permissions
		for (FieldPermission permission : sourceFieldPermissions)
		{
			// clone each type permission
			FieldPermission clonedPermission = new FieldPermission();
			clonedPermission.setRead(permission.getRead());
			clonedPermission.setEdit(permission.getEdit());
			clonedPermission.setFieldId(permission.getFieldId());
			clonedPermission.setProfile(destProfile);
			
			// save new permission
			fieldPermissionDao.save(clonedPermission, authData, env);
		}
		
		// clone action permissions
		for (ActionPermission permission : sourceActionPermissions)
		{
			// clone each page permission
			ActionPermission clonedPermission = new ActionPermission();
			clonedPermission.setRead(permission.getRead());
			clonedPermission.setAction(permission.getAction());
			clonedPermission.setProfile(destProfile);
			
			// save new permission
			actionPermissionDao.save(clonedPermission, authData, env);
		}
	}

	/**
	 * Deletes all permissions (type, field and page permissions) for a given profile.
	 * @param profileId
	 * @throws KommetException 
	 */
	private void deleteProfilePermissions(KID profileId, AuthData authData, EnvData env) throws KommetException
	{
		dataService.delete(typePermissionDao.getForProfile(profileId, env), authData, env);
		dataService.delete(fieldPermissionDao.getForProfile(profileId, env), authData, env);
		dataService.delete(actionPermissionDao.getForProfile(profileId, env), authData, env);
	}

	@Transactional(readOnly = true)
	public Profile getUnauthenticatedProfile(EnvData env) throws KIDException, KommetException
	{
		Profile profile = profileDao.get(KID.get(Profile.UNAUTHENTICATED_ID), env);
		if (profile == null)
		{
			throw new KommetException("Unauthenticated profile not found on env");
		}
		return profile;
	}

	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		// first make sure there are no users with this profile
		UserFilter filter = new UserFilter();
		filter.addProfileId(id);
		
		if (!userDao.get(filter, env).isEmpty())
		{
			throw new KommetException("Profile cannot be deleted because there are users assigned to it");
		}
		
		profileDao.delete(id, authData, env);
	}

	@Transactional(readOnly = true)
	public Profile getProfileByLabel(String label, AuthData authData, EnvData env) throws KommetException
	{
		ProfileFilter filter = new ProfileFilter();
		filter.setLabel(label);
		
		List<Profile> profiles = profileDao.find(filter, authData, env);
		
		return profiles.isEmpty() ? null : profiles.get(0);
	}
}
