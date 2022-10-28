/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kommet.auth.AuthData;
import kommet.auth.LoginHistoryFilter;
import kommet.auth.LoginHistoryService;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.basic.ActionPermission;
import kommet.basic.BasicSetupService;
import kommet.basic.FieldPermission;
import kommet.basic.LoginHistory;
import kommet.basic.Profile;
import kommet.basic.RecordProxyUtil;
import kommet.basic.TypePermission;
import kommet.basic.User;
import kommet.basic.types.SystemTypes;
import kommet.basic.types.UserKType;
import kommet.data.DataService;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.MiscUtils;
import kommet.utils.TestConfig;

public class AuthTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	TestConfig testConfig;
	
	@Inject
	LoginHistoryService loginHistoryService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	private static final Logger log = LoggerFactory.getLogger(AuthTest.class);
	
	@Test
	public void testClonePermissions() throws KommetException
	{
		// TODO finish this test
		EnvData env = dataHelper.configureFullTestEnv();
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create profile
		Profile secretaryProfile = new Profile();
		secretaryProfile.setName("Secretary");
		secretaryProfile.setLabel("Secretary");
		secretaryProfile.setSystemProfile(false);
		secretaryProfile = profileService.save(secretaryProfile, authData, env);
		assertNotNull(secretaryProfile.getId());
		
		// create profile
		Profile bossProfile = new Profile();
		bossProfile.setName("Boss");
		bossProfile.setSystemProfile(false);
		bossProfile.setLabel("Boss");
		bossProfile = profileService.save(bossProfile, authData, env);
		assertNotNull(bossProfile.getId());
		
		// create pigeon type
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		pigeonType = env.getType(pigeonType.getKID());
		assertNotNull(pigeonType);
		assertNotNull(pigeonType.getField("name"));
		
		// add type permission for boss profile
		permissionService.setTypePermissionForProfile(bossProfile.getId(), pigeonType.getKID(), true, true, false, true, false, false, false, authData, env);
		
		// add field permission for boss profile and field "name"
		permissionService.setFieldPermissionForProfile(bossProfile.getId(), pigeonType.getField("name").getKID(), true, false, authData, env);
		
		profileService.clonePermissions(bossProfile.getId(), secretaryProfile.getId(), false, dataHelper.getRootAuthData(env), env);
		
		// make sure field permissions have been copied to secretary profile
		List<FieldPermission> fieldPermissions = permissionService.getFieldPermissionForProfile(secretaryProfile.getId(), env);
		assertEquals(1, fieldPermissions.size());
	}
	
	@Test
	public void testInitPermissionSets() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		UserKType userType = (UserKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_API_NAME));
		
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		Profile nonAdminProfileObj = new Profile();
		nonAdminProfileObj.setName("TestProfile");
		nonAdminProfileObj.setLabel("TestProfile");
		nonAdminProfileObj.setSystemProfile(false);
		profileService.save(nonAdminProfileObj, dataHelper.getRootAuthData(env), env);
		Record nonAdminProfile = RecordProxyUtil.generateRecord(nonAdminProfileObj, env.getType(KeyPrefix.get(KID.PROFILE_PREFIX)), 1, env);
		
		// create test user
		Record user = new Record(userType);
		user.setField("userName", "kamila");
		user.setField("email", "kamila@kolmu.com");
		user.setField("password", MiscUtils.getSHA1Password("test"));
		user.setField("profile", nonAdminProfile);
		user.setField("timezone", "GMT");
		user.setField("locale", "EN_US");
		user.setField("isActive", true);
		dataService.save(user, env);
		
		assertNotNull(user.getKID());
		
		//===========================================================
		// Test type permissions
		//===========================================================
		// assign some rights to the profile
		TypePermission pigeonTypePermission = permissionService.setTypePermissionForProfile(nonAdminProfile.getKID(), pigeonType.getKID(), true, true, false, true, true, true, false, dataHelper.getRootAuthData(env), env);
		
		// make sure object permissions are assigned
		List<Record> typePermissions = env.getSelectCriteriaFromDAL("select id, read, edit, delete, create, readAll, editAll, deleteAll, profile.id, profile.name from " + SystemTypes.TYPE_PERMISSION_API_NAME + " where id = '" + pigeonTypePermission.getRecord().getKID() + "'").list();
		assertEquals(1, typePermissions.size());
		assertEquals(true, (Boolean)typePermissions.get(0).getField("read"));
		assertEquals(true, (Boolean)typePermissions.get(0).getField("edit"));
		assertEquals(false, (Boolean)typePermissions.get(0).getField("delete"));
		assertEquals(true, (Boolean)typePermissions.get(0).getField("create"));
		assertEquals(true, (Boolean)typePermissions.get(0).getField("readAll"));
		assertEquals(true, (Boolean)typePermissions.get(0).getField("editAll"));
		assertEquals(false, (Boolean)typePermissions.get(0).getField("deleteAll"));
		assertEquals(nonAdminProfile.getKID(), (KID)typePermissions.get(0).getField("profile.id"));
		
		// test mass retrieve
		List<TypePermission> massTypePermissions = permissionService.getTypePermissionForProfile(nonAdminProfile.getKID(), env);
		assertEquals(8, massTypePermissions.size());
		
		boolean containsPigeonPermissions = false;
		for (TypePermission permission : massTypePermissions)
		{
			if (permission.getTypeId().equals(pigeonType.getKID()))
			{
				containsPigeonPermissions = true;
			}
		}
		assertTrue(containsPigeonPermissions);
		
		//===========================================================
		// Test field permissions
		//===========================================================
		FieldPermission ageFieldPermission = permissionService.setFieldPermissionForProfile(nonAdminProfile.getKID(), pigeonType.getField("age").getKID(), true, false, dataHelper.getRootAuthData(env), env);
		
		// make sure field permissions are assigned
		List<Record> fieldPermissions = env.getSelectCriteriaFromDAL("select id, read, edit, profile.id from " + SystemTypes.FIELD_PERMISSION_API_NAME + " where id = '" + ageFieldPermission.getRecord().getKID() + "'").list();
		assertEquals(1, fieldPermissions.size());
		assertEquals(true, (Boolean)fieldPermissions.get(0).getField("read"));
		assertEquals(false, (Boolean)fieldPermissions.get(0).getField("edit"));
		assertEquals(nonAdminProfile.getKID(), (KID)fieldPermissions.get(0).getField("profile.id"));
		
		// test mass retrieve
		List<FieldPermission> massFieldPermissions = permissionService.getFieldPermissionForProfile(nonAdminProfile.getKID(), env);
		assertEquals(1, massFieldPermissions.size());
		assertEquals(pigeonType.getField("age").getKID(), massFieldPermissions.get(0).getFieldId());
		
		//===========================================================
		// Test page permission
		//===========================================================
		Record testAction = dataHelper.getSavedPigeonListAction(env, dataService);
		
		ActionPermission urlPermission = permissionService.setActionPermissionForProfile(nonAdminProfile.getKID(), testAction.getKID(), true, dataHelper.getRootAuthData(env), env);
		
		// make sure field permissions are assigned
		List<Record> actionPermissions = env.getSelectCriteriaFromDAL("select id, read, profile.id, action.id, action.url from " + SystemTypes.ACTION_PERMISSION_API_NAME + " where id = '" + urlPermission.getRecord().getKID() + "'").list();
		assertEquals(1, actionPermissions.size());
		assertEquals(true, (Boolean)actionPermissions.get(0).getField("read"));
		assertEquals(nonAdminProfile.getKID(), (KID)actionPermissions.get(0).getField("profile.id"));
		assertEquals(testAction.getKID(), (KID)actionPermissions.get(0).getField("action.id"));
		
		// test mass retrieve
		List<ActionPermission> massPagePermissions = permissionService.getActionPermissionForProfile(nonAdminProfile.getKID(), env);
		assertEquals(1, massPagePermissions.size());
		assertEquals(testAction.getField("url"), massPagePermissions.get(0).getAction().getUrl());
		
		//===========================================================
		// Test initializing all permissions for a user
		//===========================================================
		AuthData authData = new AuthData((User)RecordProxyUtil.generateStandardTypeProxy(user, true, env, compiler), env, permissionService, compiler);
		authData.setEnvName(env.getEnv().getName());
		authData.setEnvId(env.getEnv().getKID());
		authData.setUser((User)RecordProxyUtil.generateStandardTypeProxy(User.class, user, true, env));
		
		authData.initUserPermissions(env);
		assertTrue(authData.canReadType(pigeonType.getKID(), false, env));
		assertTrue(authData.canEditType(pigeonType.getKID(), false, env));
		assertFalse(authData.canDeleteType(pigeonType.getKID(), false, env));
		assertTrue(authData.canReadAllType(pigeonType.getKID(), false, env));
		assertTrue(authData.canEditAllType(pigeonType.getKID(), false, env));
		assertTrue(authData.canReadField(pigeonType.getField("age"), false, env));
		assertFalse(authData.canEditField(pigeonType.getField("age"), false, env));
		assertTrue(authData.canAccessAction((String)testAction.getField("url")));
		
		long oldPermissionUpdateDate = env.getLastActionPermissionsUpdate();
		
		// now update some permissions - revoke permissions on testAction
		permissionService.setActionPermissionForProfile(nonAdminProfile.getKID(), testAction.getKID(), false, dataHelper.getRootAuthData(env), env);
		
		// make sure the action update date has been updated on the env
		assertTrue(oldPermissionUpdateDate < env.getLastActionPermissionsUpdate());
		
		// make sure the regular canAccess method did not sync for the changes
		assertTrue(authData.canAccessAction((String)testAction.getField("url")));
		
		// make sure that checking permissions with the "update" options renders proper results
		assertFalse("Permissions not updated for URL " + testAction.getField("url"), authData.canAccessAction((String)testAction.getField("url"), true, env));
		
		// additionally, test query with properties nested two levels down, with refetched env
		// It's essential to fetch the env below using the envService, because we are testing
		// if such fetching reads in full metadata
		env = envService.get(testConfig.getTestEnvId());
		List<Record> users = env.getSelectCriteriaFromDAL("SELECT id, userName, email, profile.id, profile.name FROM " + SystemTypes.USER_API_NAME).list();
		assertTrue(!users.isEmpty());
		// make sure double-nested property can be referenced
		assertNotNull(users.get(0).getField("profile.name"));
		
		testLoginHistory(env, user);
		testUnauthenticatedUser(env);
	}

	private void testUnauthenticatedUser(EnvData env) throws KIDException, KommetException
	{
		assertNotNull(profileService.getUnauthenticatedProfile(env));
		AuthData authData = AuthData.getGuestAuthData(permissionService, uchService, env);
		assertNotNull(authData);
		assertEquals(KID.get(Profile.UNAUTHENTICATED_ID), authData.getProfile().getId());
	}

	private void testLoginHistory(EnvData env, Record user) throws KommetException
	{
		User loginUser = new User(user, env);
		
		LoginHistory lh = new LoginHistory();
		lh.setLoginUser(loginUser);
		lh.setIp4Address("127.0.0.1");
		lh.setMethod("Browser");
		
		try
		{
			loginHistoryService.save(lh, dataHelper.getRootAuthData(env), env);
			fail("Saving login history without result should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
		}
		
		lh.setResult("Success");
		assertNotNull(loginHistoryService.save(lh, dataHelper.getRootAuthData(env), env));
		
		// record login
		LoginHistory lh2 = loginHistoryService.recordLogin(user.getKID(), "Mobile", "Success", "0.0.0.0", dataHelper.getRootAuthData(env), env);
		assertNotNull(lh2.getId());
		
		// test searching login history
		assertEquals(2, loginHistoryService.get(new LoginHistoryFilter(), env).size());
		
		// test searching with limit but without any conditions or order by clause
		LoginHistoryFilter filter = new LoginHistoryFilter();
		filter.setLimit(1);
		assertEquals(1, loginHistoryService.get(filter, env).size());
		
		// add some condition to filter, but no order by clause
		filter.addUserId(user.getKID());
		assertEquals(1, loginHistoryService.get(filter, env).size());
	}
}
