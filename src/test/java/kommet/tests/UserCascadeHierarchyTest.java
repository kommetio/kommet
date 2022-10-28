/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.basic.BasicSetupService;
import kommet.basic.Profile;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyUtil;
import kommet.basic.SettingValue;
import kommet.basic.User;
import kommet.basic.UserCascadeHierarchy;
import kommet.basic.UserGroup;
import kommet.dao.SettingValueFilter;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.SystemSettingService;
import kommet.services.UserGroupService;
import kommet.uch.AmbiguousUserCascadeHierarchySettingException;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.uch.UserCascadeHierarchyException;
import kommet.uch.UserCascadeHierarchyFilter;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.MiscUtils;

public class UserCascadeHierarchyTest extends BaseUnitTest
{
	@Inject
	SystemSettingService settingService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	UserGroupService userGroupService;
	
	@Test
	public void testCRUD() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		int initialUchCount = uchService.get(null, authData, env).size();
		
		assertEquals(1, initialUchCount);
		
		UserCascadeHierarchy uch1 = new UserCascadeHierarchy();
		
		try
		{
			uchService.save(uch1, authData, env);
			fail("Saving UCH without an active context should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
		}
		
		// make sure assigning the active context object changes the active context property
		uch1.setEnv(true);
		assertEquals(UserCascadeHierarchyContext.ENVIRONMENT, uch1.getActiveContext());
		assertNotNull(uch1.getActiveContextRank());
		uch1.setEnv(null);
		//assertEquals(UserCascadeHierarchyContext.ENVIRONMENT, uch1.getActiveContext());
		assertNull(uch1.getActiveContext());
		assertNull(uch1.getActiveContextRank());
		
		uch1.setProfile(authData.getProfile());
		assertEquals(UserCascadeHierarchyContext.PROFILE, uch1.getActiveContext());
		uch1.setProfile(null);
		//assertEquals(UserCascadeHierarchyContext.PROFILE, uch1.getActiveContext());
		assertNull(uch1.getActiveContext());
		assertNull(uch1.getActiveContextRank());
		
		uch1.setLocaleName(Locale.EN_US.name());
		assertEquals(UserCascadeHierarchyContext.LOCALE, uch1.getActiveContext());
		uch1.setLocaleName(null);
		//assertEquals(UserCascadeHierarchyContext.LOCALE, uch1.getActiveContext());
		assertNull(uch1.getActiveContext());
		assertNull(uch1.getActiveContextRank());
		
		uch1.setUser(authData.getUser());
		assertEquals(UserCascadeHierarchyContext.USER, uch1.getActiveContext());
		uch1.setUser(null);
		assertNull(uch1.getActiveContext());
		assertNull(uch1.getActiveContextRank());
		
		uch1.setUser(authData.getUser());
		
		// now save the UCH
		uch1 = uchService.save(uch1, authData, env);
		assertNotNull(uch1.getId());
		
		assertEquals(initialUchCount + 1, uchService.get(null, authData, env).size());
		
		Type incorrectSettingType = createBackgroundSettingType(env, false);
		
		// create setting record
		Record envSettingRecord = new Record(incorrectSettingType);
		envSettingRecord.setField("colour", "blue");
		envSettingRecord.setField("settingName", "backgroundColour");
		RecordProxy envSetting = RecordProxyUtil.generateCustomTypeProxy(envSettingRecord, env, compiler);
		
		try
		{
			uchService.saveSetting(envSetting, UserCascadeHierarchyContext.ENVIRONMENT, true, dataHelper.getRootAuthData(env), env);
			fail("Inserting setting should fail because its type has UCH reference with cascade delete = false");
		}
		catch (UserCascadeHierarchyException e)
		{
			assertTrue(e.getMessage().endsWith("referencing UCH object should have cascade delete set to true"));
		}
	
		// create a custom setting type that will store information about background colour
		Type settingType = createBackgroundSettingType(env, true);
		// create data for further tests
		Profile testProfile1 = dataHelper.getTestProfileObject("TestProfile1", env);
		Profile testProfile2 = dataHelper.getTestProfileObject("TestProfile2", env);
		User testUser = dataHelper.getTestUser("testuser@kommet.io", "testuser@kommet.io", testProfile1, env);
		// run tests regarding retrieving settings for the current context
		testRetrievingUCH(settingType, testProfile1, testProfile2, testUser, env, true);
		testRetrievingUCH(settingType, testProfile1, testProfile2, testUser, env, false);
		
		try
		{
			assertNull(uchService.getSetting(settingType, Arrays.asList("colour"), "settingName1", "incorrect-value", dataHelper.getRootAuthData(env), dataHelper.getRootAuthData(env), env));
			fail("Searching by a non-existing discriminator field should fail");
		}
		catch (KommetException e)
		{
			assertTrue(e.getMessage().startsWith("Discriminator field"));
		}

		// test retrieving non-existing setting
		assertNull(uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "incorrect-value", dataHelper.getRootAuthData(env), dataHelper.getRootAuthData(env), env));
		
		testUserSetting(authData, env);
	}

	private void testUserSetting(AuthData authData, EnvData env) throws KommetException
	{
		SettingValue val = uchService.saveUserSetting("test.key", "val", UserCascadeHierarchyContext.ENVIRONMENT, true, authData, env);
		
		SettingValueFilter filter = new SettingValueFilter();
		filter.addKey("test.key");
		filter.setContext(UserCascadeHierarchyContext.ENVIRONMENT);
		filter.setContextValue(true);
		List<SettingValue> retrievedValues = uchService.getSettings(filter, authData, authData, env);
		assertEquals(1, retrievedValues.size());
		assertEquals(val.getId(), retrievedValues.get(0).getId());
		
		try
		{
			uchService.saveUserSetting("test.key", "val", UserCascadeHierarchyContext.ENVIRONMENT, true, authData, env);
			fail("Saving another setting with the same key, context and context value should fail");
		}
		catch (KommetException e)
		{
			assertEquals(UserCascadeHierarchyService.SETTING_EXISTS_ERROR, e.getMessage());
		}
		
		SettingValue profileVal = uchService.saveUserSetting("test.key", "val", UserCascadeHierarchyContext.PROFILE, authData.getProfile().getId(), authData, env);
		
		filter = new SettingValueFilter();
		filter.addKey("test.key");
		filter.setContext(UserCascadeHierarchyContext.PROFILE);
		filter.setContextValue(authData.getProfile().getId());
		retrievedValues = uchService.getSettings(filter, authData, authData, env);
		assertEquals(1, retrievedValues.size());
		assertEquals(profileVal.getId(), retrievedValues.get(0).getId());
		assertTrue(profileVal.getId() != val.getId());
		
		SettingValue userVal = uchService.saveUserSetting("test.key", "val", UserCascadeHierarchyContext.USER, authData.getUser().getId(), authData, env);
		
		retrievedValues = uchService.getSettings(filter, authData, authData, env);
		assertEquals(1, retrievedValues.size());
		assertEquals(profileVal.getId(), retrievedValues.get(0).getId());
		
		filter = new SettingValueFilter();
		filter.addKey("test.key");
		filter.setContext(UserCascadeHierarchyContext.USER);
		filter.setContextValue(authData.getUser().getId());
		retrievedValues = uchService.getSettings(filter, authData, authData, env);
		assertEquals(1, retrievedValues.size());
		assertEquals(userVal.getId(), retrievedValues.get(0).getId());
	}

	/**
	 * Test inserting and reading of currently application setting.
	 * 
	 * If this method is run with parameter testDiscriminatorField == true, then for each inserted setting an
	 * additional setting is added with the same value but different discriminator field value. This is done by
	 * method duplicateSettingWithDifferentSettingType.
	 * @param settingType
	 * @param testProfile1
	 * @param testProfile2
	 * @param testUser
	 * @param env
	 * @param testDiscriminatorField whether to test settings with discriminator fields or without them
	 * @throws KommetException
	 */
	private void testRetrievingUCH(Type settingType, Profile testProfile1, Profile testProfile2, User testUser, EnvData env, boolean testDiscriminatorField) throws KommetException
	{	
		// clear all setting records if created in a previous run of this method
		List<Record> settingRecords = env.getSelectCriteriaFromDAL("select id from " + settingType.getQualifiedName()).list();
		if (!settingRecords.isEmpty())
		{
			dataService.deleteRecords(settingRecords, false, dataHelper.getRootAuthData(env), env);
		}
		
		// make sure there are not settings to start with
		assertEquals(Long.valueOf(0), env.getSelectCriteriaFromDAL("select count(id) from " + settingType.getQualifiedName() + " where cascadeHierarchy.env = true").count());
		
		// create a system-wide setting
		Record envSettingRecord = new Record(settingType);
		envSettingRecord.setField("colour", "blue");
		envSettingRecord.setField("settingName", "backgroundColour");
		
		RecordProxy envSetting = RecordProxyUtil.generateCustomTypeProxy(envSettingRecord, env, compiler);
		
		envSetting = uchService.saveSetting(envSetting, UserCascadeHierarchyContext.ENVIRONMENT, true, dataHelper.getRootAuthData(env), env);
		assertNotNull(envSetting.getId());
		
		AuthData testAuthData = dataHelper.getAuthData(testUser, env);
		
		permissionService.setTypePermissionForProfile(testProfile1.getId(), settingType.getKID(), true, false, false, true, false, false, false, dataHelper.getRootAuthData(env), env);
		RecordProxy currentSetting = null;
		
		// test current colour setting
		if (testDiscriminatorField)
		{
			// try to retrieve using different method
			Object colorValue = uchService.getSettingValue(settingType, "colour", "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
			assertNotNull(colorValue);
			assertTrue ("Color value is of incorrect type " + colorValue.getClass().getName(), colorValue instanceof String);
			assertEquals("blue", (String)colorValue);
			
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
		}
		else
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
		}
		assertNotNull(currentSetting);
		assertEquals(MiscUtils.userToEnvPackage(settingType.getQualifiedName(), env), currentSetting.getClass().getName());
		assertEquals("blue", currentSetting.getField("colour"));
		
		// create setting for a non-used profile - this setting is not applicable
		Record profileSettingRecord1 = new Record(settingType);
		profileSettingRecord1.setField("colour", "red");
		profileSettingRecord1.setField("settingName", "backgroundColour");
		RecordProxy profileSetting1 = RecordProxyUtil.generateCustomTypeProxy(profileSettingRecord1, env, compiler);
		profileSetting1 = uchService.saveSetting(profileSetting1, UserCascadeHierarchyContext.PROFILE, testProfile2.getId(), dataHelper.getRootAuthData(env), env);
		assertNotNull(profileSetting1.getId());
		
		if (testDiscriminatorField)
		{
			// also create some other setting that will not be applicable because of settingName <> 'backgroundColour'
			cloneSettingWithDifferentSettingType(profileSettingRecord1, profileSetting1.getId(), UserCascadeHierarchyContext.PROFILE, testProfile2.getId(), env);
		}
		
		// create another setting for this user's environment
		Record envSettingRecord2 = new Record(settingType);
		envSettingRecord2.setField("colour", "yellow");
		envSettingRecord2.setField("settingName", "backgroundColour");
		RecordProxy envSetting2 = RecordProxyUtil.generateCustomTypeProxy(envSettingRecord2, env, compiler);
		envSetting2 = uchService.saveSetting(envSetting2, UserCascadeHierarchyContext.ENVIRONMENT, true, dataHelper.getRootAuthData(env), env);
		assertNotNull(envSetting2.getId());
		
		if (testDiscriminatorField)
		{	
			try
			{
				currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
				fail("Extracting setting for this user group should fail because they are assigned to two groups, so the choice is ambiguous. Instead got setting value: " + currentSetting.getField("colour"));
			}
			catch (AmbiguousUserCascadeHierarchySettingException e)
			{
				// expected
				assertEquals(AmbiguousUserCascadeHierarchySettingException.ERROR_MESSAGE, e.getMessage());
			}
			
			Long initialSettingCount = env.getSelectCriteriaFromDAL("select count(id) from " + settingType.getQualifiedName() + " where cascadeHierarchy.env = true").count();
			assertEquals(Long.valueOf(2), initialSettingCount);
			
			envSettingRecord.setKID(envSetting.getId());
			
			// delete the duplicate environment setting
			dataService.deleteRecords(Arrays.asList(envSettingRecord), false, dataHelper.getRootAuthData(env), env);
			
			// make sure that after deleting a setting, the number of setting records decreased by one
			assertEquals((Long)(initialSettingCount - 1), env.getSelectCriteriaFromDAL("select count(id) from " + settingType.getQualifiedName() + " where cascadeHierarchy.env = true").count());
			
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
		}
		else
		{
			envSettingRecord.setKID(envSetting.getId());
			
			// delete the duplicate environment setting
			dataService.deleteRecords(Arrays.asList(envSettingRecord), false, dataHelper.getRootAuthData(env), env);
			
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
		}
		
		assertNotNull(currentSetting);
		assertEquals(MiscUtils.userToEnvPackage(settingType.getQualifiedName(), env), currentSetting.getClass().getName());
		assertEquals("yellow", currentSetting.getField("colour"));
		
		// create a setting for this user's profile
		Record profileSettingRecord2 = new Record(settingType);
		profileSettingRecord2.setField("colour", "green");
		profileSettingRecord2.setField("settingName", "backgroundColour");
		RecordProxy profileSetting2 = RecordProxyUtil.generateCustomTypeProxy(profileSettingRecord2, env, compiler);
		profileSetting2 = uchService.saveSetting(profileSetting2, UserCascadeHierarchyContext.PROFILE, testProfile1.getId(), dataHelper.getRootAuthData(env), env);
		assertNotNull(profileSetting2.getId());
		
		if (testDiscriminatorField)
		{
			// also create some other setting that will not be applicable because of settingName <> 'backgroundColour'
			cloneSettingWithDifferentSettingType(profileSettingRecord2, profileSetting2.getId(), UserCascadeHierarchyContext.PROFILE, testProfile1.getId(), env);
		}
		
		// test current colour setting
		if (testDiscriminatorField)
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
		}
		else
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
		}
		assertNotNull(currentSetting);
		assertEquals(MiscUtils.userToEnvPackage(settingType.getQualifiedName(), env), currentSetting.getClass().getName());
		assertEquals("green", currentSetting.getField("colour"));
		
		assertNotNull(testAuthData.getLocale());
		
		// create locale setting
		Record localeSettingRecord2 = new Record(settingType);
		localeSettingRecord2.setField("colour", "navy");
		localeSettingRecord2.setField("settingName", "backgroundColour");
		RecordProxy localeSetting2 = RecordProxyUtil.generateCustomTypeProxy(localeSettingRecord2, env, compiler);
		localeSetting2 = uchService.saveSetting(localeSetting2, UserCascadeHierarchyContext.LOCALE, testAuthData.getLocale(), dataHelper.getRootAuthData(env), env);
		assertNotNull(localeSetting2.getId());
		
		if (testDiscriminatorField)
		{
			// also create some other setting that will not be applicable because of settingName <> 'backgroundColour'
			cloneSettingWithDifferentSettingType(localeSettingRecord2, localeSetting2.getId(), UserCascadeHierarchyContext.LOCALE, testAuthData.getLocale(), env);
		}
		
		// test current colour setting
		if (testDiscriminatorField)
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
		}
		else
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
		}
		assertNotNull(currentSetting);
		assertEquals(MiscUtils.userToEnvPackage(settingType.getQualifiedName(), env), currentSetting.getClass().getName());
		assertEquals("navy", currentSetting.getField("colour"));
		
		// create some user group for this user
		UserGroup group1 = new UserGroup();
		group1.setName("Group1" + (new Random()).nextInt(1000));
		userGroupService.save(group1, dataHelper.getRootAuthData(env), env);
		
		// assign user to group
		userGroupService.assignUserToGroup(testUser.getId(), group1.getId(), dataHelper.getRootAuthData(env), env);
		
		// add setting for this group
		Record groupSettingRecord1 = new Record(settingType);
		groupSettingRecord1.setField("colour", "grey");
		groupSettingRecord1.setField("settingName", "backgroundColour");
		RecordProxy groupSetting1 = RecordProxyUtil.generateCustomTypeProxy(groupSettingRecord1, env, compiler);
		groupSetting1 = uchService.saveSetting(groupSetting1, UserCascadeHierarchyContext.USER_GROUP, group1.getId(), dataHelper.getRootAuthData(env), env);
		assertNotNull(groupSetting1.getId());
		
		if (testDiscriminatorField)
		{
			// also create some other setting that will not be applicable because of settingName <> 'backgroundColour'
			cloneSettingWithDifferentSettingType(groupSettingRecord1, groupSetting1.getId(), UserCascadeHierarchyContext.USER_GROUP, group1.getId(), env);
		}
		
		// test current colour setting
		if (testDiscriminatorField)
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
		}
		else
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
		}
		assertEquals("grey", currentSetting.getField("colour"));
		
		// add another group and setting for it, but do not assign the user to this group yet
		UserGroup group2 = new UserGroup();
		group2.setName("Group2" + (new Random()).nextInt(1000));
		userGroupService.save(group2, dataHelper.getRootAuthData(env), env);
		
		// add setting for this group
		Record groupSettingRecord2 = new Record(settingType);
		groupSettingRecord2.setField("colour", "black");
		groupSettingRecord2.setField("settingName", "backgroundColour");
		RecordProxy groupSetting2 = RecordProxyUtil.generateCustomTypeProxy(groupSettingRecord2, env, compiler);
		groupSetting2 = uchService.saveSetting(groupSetting2, UserCascadeHierarchyContext.USER_GROUP, group2.getId(), dataHelper.getRootAuthData(env), env);
		assertNotNull(groupSetting2.getId());
		
		if (testDiscriminatorField)
		{
			// also create some other setting that will not be applicable because of settingName <> 'backgroundColour'
			cloneSettingWithDifferentSettingType(groupSettingRecord2, groupSetting2.getId(), UserCascadeHierarchyContext.USER_GROUP, group2.getId(), env);
		}
		
		if (testDiscriminatorField)
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
		}
		else
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
		}
		assertEquals("grey", currentSetting.getField("colour"));
		
		// now assign the user to this second group
		userGroupService.assignUserToGroup(testUser.getId(), group2.getId(), dataHelper.getRootAuthData(env), env);
		assertTrue(userGroupService.isUserInGroup(testUser.getId(), group2.getId(), false, env));
		
		// extracting setting for this user group should fail because they are assigned to two groups, so the choice is ambiguous
		try
		{
			if (testDiscriminatorField)
			{
				currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
			}
			else
			{
				currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
			}
			fail("Extracting setting for this user group should fail because they are assigned to two groups, so the choice is ambiguous. Instead got setting value: " + currentSetting.getField("colour"));
		}
		catch (AmbiguousUserCascadeHierarchySettingException e)
		{
			// expected
			assertEquals(AmbiguousUserCascadeHierarchySettingException.ERROR_MESSAGE, e.getMessage());
		}
		
		// now delete a setting for one of the groups
		dataService.deleteRecord(groupSetting1.getId(), dataHelper.getRootAuthData(env), env);
		if (testDiscriminatorField)
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
		}
		else
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
		}
		assertEquals("black", currentSetting.getField("colour"));
		
		// now add the ambiguous setting again
		groupSettingRecord1 = new Record(settingType);
		groupSettingRecord1.setField("colour", "grey");
		groupSettingRecord1.setField("settingName", "backgroundColour");
		groupSetting1 = RecordProxyUtil.generateCustomTypeProxy(groupSettingRecord1, env, compiler);
		groupSetting1 = uchService.saveSetting(groupSetting1, UserCascadeHierarchyContext.USER_GROUP, group1.getId(), dataHelper.getRootAuthData(env), env);
		assertNotNull(groupSetting1.getId());
		
		if (testDiscriminatorField)
		{
			// also create some other setting that will not be applicable because of settingName <> 'backgroundColour'
			cloneSettingWithDifferentSettingType(groupSettingRecord1, groupSetting1.getId(), UserCascadeHierarchyContext.USER_GROUP, group1.getId(), env);
		}
		
		// extracting setting for this user group should fail because they are assigned to two groups, so the choice is ambiguous
		try
		{
			if (testDiscriminatorField)
			{
				currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
			}
			else
			{
				currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
			}
			fail("Extracting setting for this user group should fail because they are assigned to two groups, so the choice is ambiguous");
		}
		catch (AmbiguousUserCascadeHierarchySettingException e)
		{
			// expected
			assertEquals(AmbiguousUserCascadeHierarchySettingException.ERROR_MESSAGE, e.getMessage());
		}
		
		// add a per user setting and make sure this solves the ambiguity problem
		Record userRecord2 = new Record(settingType);
		userRecord2.setField("colour", "pink");
		userRecord2.setField("settingName", "backgroundColour");
		RecordProxy userSetting2 = RecordProxyUtil.generateCustomTypeProxy(userRecord2, env, compiler);
		userSetting2 = uchService.saveSetting(userSetting2, UserCascadeHierarchyContext.USER, testUser.getId(), dataHelper.getRootAuthData(env), env);
		assertNotNull(userSetting2.getId());
		
		if (testDiscriminatorField)
		{
			// also create some other setting that will not be applicable because of settingName <> 'backgroundColour'
			cloneSettingWithDifferentSettingType(userRecord2, userSetting2.getId(), UserCascadeHierarchyContext.USER, testUser.getId(), env);
		}
		
		userRecord2 = env.getSelectCriteriaFromDAL("select id, colour, cascadeHierarchy.id from " + settingType.getQualifiedName() + " where id = '" + userSetting2.getId() + "'").list().get(0);
		assertEquals(userSetting2.getId(), userRecord2.getKID());
		
		UserCascadeHierarchyFilter filter = new UserCascadeHierarchyFilter();
		filter.addUchId((KID)userRecord2.getField("cascadeHierarchy.id"));
		UserCascadeHierarchy userUCH = uchService.get(filter, dataHelper.getRootAuthData(env), env).get(0);
		assertEquals((Integer)100, userUCH.getActiveContextRank());
		assertEquals(testUser.getId(), userUCH.getUser().getId());
		
		if (testDiscriminatorField)
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), "settingName", "backgroundColour", testAuthData, dataHelper.getRootAuthData(env), env);
		}
		else
		{
			currentSetting = uchService.getSetting(settingType, Arrays.asList("colour"), testAuthData, dataHelper.getRootAuthData(env), env);
		}
		assertEquals("pink", currentSetting.getField("colour"));
	}
	
	/**
	 * Clones the given setting changing only its <tt>settingName</tt> field.
	 * @param setting
	 * @param originalId
	 * @param ctx
	 * @param ctxValue
	 * @param env
	 * @throws KommetException
	 */
	private void cloneSettingWithDifferentSettingType(Record setting, KID originalId, UserCascadeHierarchyContext ctx, Object ctxValue, EnvData env) throws KommetException
	{
		setting.setField("settingName", "fontSize");
		setting.uninitializeField(Field.ID_FIELD_NAME);
		
		RecordProxy settingProxy = RecordProxyUtil.generateCustomTypeProxy(setting, env, compiler);
		settingProxy = uchService.saveSetting(settingProxy, ctx, ctxValue, dataHelper.getRootAuthData(env), env);
		
		assertNotNull(settingProxy.getId());
		assertFalse(originalId.equals(settingProxy.getId()));
	}

	/**
	 * Create a type called <tt>CustomSetting</tt> for storing background colour information.
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private Type createBackgroundSettingType(EnvData env, boolean isCascadeDelete) throws KommetException
	{
		Type settingType = new Type();
		
		// create name, and append some random suffix if cascade delete is true because in that case we are likely
		// to create another setting with cascade delete = false, and this would cause duplicates
		settingType.setApiName("CustomSetting" + (!isCascadeDelete ? (new Random()).nextInt(1000) : ""));
		settingType.setPackage("com.test.pack");
		settingType.setLabel("Custom Setting");
		settingType.setPluralLabel("Custom Settings");
		settingType.setCreated(new Date());
		
		// add fields to type
		Field colourField = new Field();
		colourField.setApiName("colour");
		colourField.setDataType(new TextDataType(10));
		colourField.setLabel("Colour");
		colourField.setRequired(true);
		settingType.addField(colourField);
		
		Field settingNameField = new Field();
		settingNameField.setApiName("settingName");
		settingNameField.setDataType(new TextDataType(20));
		settingNameField.setLabel("Setting Name");
		settingNameField.setRequired(true);
		settingType.addField(settingNameField);
		
		// add fields to type
		Field rankField = new Field();
		rankField.setApiName("rank");
		rankField.setDataType(new NumberDataType(0, Integer.class));
		rankField.setLabel("Rank");
		rankField.setRequired(false);
		settingType.addField(rankField);
		
		Field uchField = new Field();
		uchField.setApiName("cascadeHierarchy");
		
		TypeReference objRef = new TypeReference(env.getType(KeyPrefix.get(KID.USER_CASCADE_HIERARCHY_PREFIX)));
		objRef.setCascadeDelete(isCascadeDelete);
		
		uchField.setDataType(objRef);
		uchField.setLabel("Cascade Hierarchy");
		uchField.setRequired(true);
		settingType.addField(uchField);
		
		// save type
		settingType = dataService.createType(settingType, env);
		assertNotNull(settingType.getKID());
		
		return settingType;
	}
}
