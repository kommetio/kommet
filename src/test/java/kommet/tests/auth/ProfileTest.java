/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.Profile;
import kommet.basic.RecordAccessType;
import kommet.basic.TypePermission;
import kommet.basic.User;
import kommet.basic.types.SystemTypes;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class ProfileTest extends BaseUnitTest
{
	@Inject
	ProfileService profileService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	UserService userService;
	
	@Test
	public void testGetProfiles() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		List<Profile> profiles = profileService.getProfiles(env);
		
		// there are three system profiles - root, system administrator and unauthenticated
		assertEquals(3, profiles.size());
		
		Profile profile = profiles.get(0);
		assertNotNull(profile.getId());
		
		Type profileType = env.getType(KeyPrefix.get(KID.PROFILE_PREFIX));
		assertNotNull(profileType);
		assertEquals("'Name' should be defined as the default field on the profile type", "name", profileType.getDefaultFieldApiName());
		
		testDeletingTypePermission(env);
	}

	private void testDeletingTypePermission(EnvData env) throws KommetException
	{
		// create pigeon type
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		assertNotNull(pigeonType.getKID());
		
		// create profile
		Profile testProfile1 = dataHelper.getTestProfileObject("TestProfile2", env);
		
		Long initialTypePermissionCount = env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.TYPE_PERMISSION_API_NAME).count();
		Profile testProfile = dataHelper.getTestProfileObject("TestProfile", env);
		
		assertEquals(testProfile1.getId(), profileService.getProfileByLabel("TestProfile2", AuthData.getRootAuthData(env), env).getId());
		assertNull(profileService.getProfileByLabel("", AuthData.getRootAuthData(env), env));
		
		User testUser = dataHelper.getTestUser("test@kommet.io", "test@kommet.io", testProfile, env);
		assertNotNull(testUser.getId());
		
		// add some permission assignments to type and its fields
		permissionService.setTypePermissionForProfile(testProfile.getId(), pigeonType.getKID(), true, false, false, false, true, false, false, dataHelper.getRootAuthData(env), env);
		
		// make sure type permission has been added
		assertEquals((Long)(initialTypePermissionCount + 8), env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.TYPE_PERMISSION_API_NAME).count());
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		try
		{
			// now delete the profile and make sure it is not possible because there are users assigned to it
			profileService.delete(testProfile.getId(), authData, env);
			fail("Deleting profile should fail because there are users assigned to it");
		}
		catch (KommetException e)
		{
			// expected
			assertEquals("Profile cannot be deleted because there are users assigned to it", e.getMessage());
		}
		
		// delete the user
		userService.delete(testUser.getId(), authData, env);
		assertNull(userService.get("test@kommet.io", env));
		
		profileService.delete(testProfile.getId(), authData, env);
		
		assertEquals((Long)(initialTypePermissionCount), env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.TYPE_PERMISSION_API_NAME).count());
		
		testDefaultPermissions(env);
	}

	private void testDefaultPermissions(EnvData env) throws KommetException
	{
		Profile testProfile = dataHelper.getTestProfileObject("AnyProfile", env);
		
		// create user for profile
		User testUser = dataHelper.getTestUser("test1@kommet.io", "test1@kommet.io", testProfile, env);
		AuthData authData = userService.getAuthData(testUser, env);
		authData.initUserPermissions(env);
		
		Type fileAssocType = env.getType(KeyPrefix.get(KID.FILE_RECORD_ASSIGNMENT_PREFIX));
		assertTrue(authData.canReadType(fileAssocType.getKID(), false, env));
		assertTrue(authData.canReadAllType(fileAssocType.getKID(), false, env));
		assertTrue(authData.canEditType(fileAssocType.getKID(), false, env));
		assertTrue(authData.canCreateType(fileAssocType.getKID(), false, env));
		assertTrue(authData.canDeleteType(fileAssocType.getKID(), false, env));
		
		Type fileType = env.getType(KeyPrefix.get(KID.FILE_PREFIX));
		assertTrue(authData.canReadType(fileType.getKID(), false, env));
		assertTrue(authData.canEditType(fileType.getKID(), false, env));
		assertTrue(authData.canCreateType(fileType.getKID(), false, env));
		assertTrue(authData.canDeleteType(fileType.getKID(), false, env));
		assertFalse(authData.canReadAllType(fileType.getKID(), false, env));
		
		Type notificationType = env.getType(KeyPrefix.get(KID.NOTIFICATION_PREFIX));
		assertTrue(authData.canReadType(notificationType.getKID(), false, env));
		
		// all profiles must be able to read the user type, because it is used by the createdBy and lastModifiedBy fields
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		assertTrue(authData.canReadType(userType.getKID(), false, env));
		
		Type taskType = env.getType(KeyPrefix.get(KID.TASK_PREFIX));
		assertTrue(authData.canReadType(taskType.getKID(), false, env));
		assertTrue(authData.canCreateType(taskType.getKID(), false, env));
		
		Type eventType = env.getType(KeyPrefix.get(KID.EVENT_PREFIX));
		assertTrue(authData.canReadType(eventType.getKID(), false, env));
		
		Type eventGuestType = env.getType(KeyPrefix.get(KID.EVENT_GUEST_PREFIX));
		assertTrue(authData.canReadType(eventGuestType.getKID(), false, env));
		assertTrue(authData.canCreateType(eventGuestType.getKID(), false, env));
		
		// get all type permissions for guest profile
		for (TypePermission tp : permissionService.getTypePermissionForProfile(KID.get(Profile.UNAUTHENTICATED_ID), env))
		{
			assertEquals((Integer)RecordAccessType.PUBLIC.getId(), tp.getAccessType());
		}
	}
}
