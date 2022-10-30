/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.koll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.Profile;
import kommet.basic.SharingRule;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.KIDDataType;
import kommet.data.sharing.SharingService;
import kommet.data.sharing.UserRecordSharingFilter;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.SharingRuleFilter;
import kommet.koll.ClassService;
import kommet.koll.annotations.QueriedTypes;
import kommet.koll.annotations.SharedWith;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.SharingRuleService;
import kommet.services.UserGroupService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class SharingRuleTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	ClassService classService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	SharingRuleService srService;
	
	@Inject
	DataService dataService;
	
	@Inject
	UserService userService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	UserGroupService ugService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	EnvService envService;
	
	@Test
	public void testSharingRuleDeclarations() throws KommetException, ClassNotFoundException, MalformedURLException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create pigeon type
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		testSimpleRuleCreation(pigeonType, authData, env);
		
		// create profile
		Profile secretaryProfile = new Profile();
		secretaryProfile.setName("Secretary");
		secretaryProfile.setLabel("Secretary");
		secretaryProfile.setSystemProfile(false);
		secretaryProfile = profileService.save(secretaryProfile, AuthData.getRootAuthData(env), env);
		
		// create another user with this profile
		User user1 = dataHelper.getTestUser("reader1@kommet.io", "reader1@kommet.io", secretaryProfile, env);
		user1 = userService.save(user1, authData, env);
		
		User user2 = dataHelper.getTestUser("reader2@kommet.io", "reader2@kommet.io", secretaryProfile, env);
		user2 = userService.save(user2, authData, env);
		
		permissionService.setTypePermissionForProfile(secretaryProfile.getId(), pigeonType.getKID(), true, false, false, true, false, false, false, authData, env);
		
		// create two pigeons
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Jan");
		pigeon1.setField("age", 2);
		pigeon1 = dataService.save(pigeon1, env);
		
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Marek");
		pigeon2.setField("age", 1);
		pigeon2 = dataService.save(pigeon2, env);
		
		testRulesWithSharing(pigeonType, pigeon1, pigeon2, user1, user2, authData, env);
		testUserSharingRule(authData, env);
	}
	
	/**
	 * Tests a sharing rule on a user object that uses some queries with placeholders.
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	private void testUserSharingRule(AuthData authData, EnvData env) throws KommetException
	{
		String sharingRule1 = "@SharingRule\n";
		sharingRule1 += "public static List<User> shareUser (User u) throws KommetException {\n";
		sharingRule1 += "if (u == null) throw new KommetException(\"User passed to method is null\");\n";
		sharingRule1 += "System.out.println(\"User id = \" + u.getId());\n";
		sharingRule1 += "u = { select id, profile.name from User where id = '#u.getId()' };\n";
		
		// add another mock query
		sharingRule1 += "u = { select id, profile.name from User where id = '#u.getId()' and isActive = true };\n";
		
		sharingRule1 += "if (u == null) throw new KommetException(\"Queried user is null\");\n";
		sharingRule1 += "if (\"Lawyer\".equals(u.getProfile().getName())) { return { select id from User where profile.name = 'Lawyer' };	} else { return null; } }";
		
		Class cls = getSharingRuleClass("com.sharing.UserSharingRules", env.getType(KeyPrefix.get(KID.USER_PREFIX)), Arrays.asList(sharingRule1), authData, env);
		cls = classService.fullSave(cls, dataService, authData, env);
	}

	private void testRulesWithSharing(Type pigeonType, Record pigeon1, Record pigeon2, User user1, User user2, AuthData authData, EnvData env) throws KommetException, ClassNotFoundException, MalformedURLException
	{
		int initialCount = srService.get(new SharingRuleFilter(), authData, env).size();
		assertEquals(2, initialCount);
		
		sharingService.shareRecord(pigeon1.getKID(), user1.getId(), false, false, authData, "none", true, env);
		assertFalse(sharingService.canEditRecord(pigeon1.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon1.getKID(), user1.getId(), env));
		
		AuthData user1AuthData = dataHelper.getAuthData(user1, env);
		AuthData user2AuthData = dataHelper.getAuthData(user2, env);
		
		String pigeonQuery = "select id, name, age from " + pigeonType.getQualifiedName();
		assertEquals(1, env.getSelectCriteriaFromDAL(pigeonQuery, user1AuthData).list().size());
		assertEquals(0, env.getSelectCriteriaFromDAL(pigeonQuery, user2AuthData).list().size());
		
		// now create a sharing rule that shares pigeons with all users - but only for viewing
		String sharingRule1 = "@SharingRule\n";
		sharingRule1 += "public static List<User> sharePigeonWithGroups(Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id from User }; }\n";
		
		Class cls = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls = classService.fullSave(cls, dataService, authData, env);
		
		assertEquals(initialCount + 1, srService.get(new SharingRuleFilter(), authData, env).size());
		assertEquals(2, env.getSelectCriteriaFromDAL(pigeonQuery, user1AuthData).list().size());
		assertEquals(2, env.getSelectCriteriaFromDAL(pigeonQuery, user2AuthData).list().size());
		
		assertFalse(sharingService.canEditRecord(pigeon1.getKID(), user1.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon2.getKID(), user1.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon1.getKID(), user2.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon2.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon1.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon2.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon1.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon2.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user2.getId(), env));
		
		// now add sharing for editing as well
		sharingRule1 = "@SharingRule(edit = true)\n";
		sharingRule1 += "public static List<User> sharePigeonWithGroups (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id from User }; }\n";
		
		Class newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		assertEquals(initialCount + 1, srService.get(new SharingRuleFilter(), authData, env).size());
		assertEquals(2, env.getSelectCriteriaFromDAL(pigeonQuery, user1AuthData).list().size());
		assertEquals(2, env.getSelectCriteriaFromDAL(pigeonQuery, user2AuthData).list().size());
		
		assertTrue(sharingService.canEditRecord(pigeon1.getKID(), user1.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon2.getKID(), user1.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon1.getKID(), user2.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon2.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon1.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon2.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon1.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon2.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user2.getId(), env));
		
		// now add sharing for deleting as well
		sharingRule1 = "@SharingRule(edit = true, delete = true)\n";
		sharingRule1 += "public static List<User> sharePigeonWithGroups (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id from User }; }\n";
		
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		// make sure exactly one rule has been created for this class
		SharingRuleFilter srFilter = new SharingRuleFilter();
		srFilter.addFileId(cls.getId());
		List<SharingRule> rulesForClass = srService.get(srFilter, AuthData.getRootAuthData(env), env);
		assertEquals(1, rulesForClass.size());
		
		UserRecordSharingFilter ursFilter = new UserRecordSharingFilter();
		ursFilter.addSharingRuleId(rulesForClass.get(0).getId());
		assertFalse(sharingService.find(ursFilter, env).isEmpty());
		
		assertEquals(initialCount + 1, srService.get(new SharingRuleFilter(), authData, env).size());
		assertEquals(2, env.getSelectCriteriaFromDAL(pigeonQuery, user1AuthData).list().size());
		assertEquals(2, env.getSelectCriteriaFromDAL(pigeonQuery, user2AuthData).list().size());
		
		assertTrue(sharingService.canEditRecord(pigeon1.getKID(), user1.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon2.getKID(), user1.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon1.getKID(), user2.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon2.getKID(), user2.getId(), env));
		assertTrue(sharingService.canDeleteRecord(pigeon1.getKID(), user1.getId(), env));
		assertTrue(sharingService.canDeleteRecord(pigeon2.getKID(), user1.getId(), env));
		assertTrue(sharingService.canDeleteRecord(pigeon1.getKID(), user2.getId(), env));
		assertTrue(sharingService.canDeleteRecord(pigeon2.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user2.getId(), env));
		
		// revoke edit and delete permissions
		sharingRule1 = "@SharingRule(edit = false)\n";
		sharingRule1 += "public static List<User> sharePigeonWithGroups (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id from User }; }\n";
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		assertFalse(sharingService.canEditRecord(pigeon1.getKID(), user1.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon2.getKID(), user1.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon1.getKID(), user2.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon2.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon1.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon2.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon1.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon2.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user2.getId(), env));
		
		// create a new pigeon
		Record pigeon3 = new Record(pigeonType);
		pigeon3.setField("name", "Lei");
		pigeon3.setField("age", 1);
		pigeon3 = dataService.save(pigeon3, env);
		
		// make sure there are two URS records for pigeon3 and user2
		UserRecordSharingFilter user2Pigeon3Filter = new UserRecordSharingFilter();
		user2Pigeon3Filter.addUserId(user2.getId());
		user2Pigeon3Filter.addRecordId(pigeon3.getKID());
		assertEquals(1, sharingService.find(user2Pigeon3Filter, env).size());
		
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user1.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user2.getId(), env));
		
		Record pigeon4 = new Record(pigeonType);
		pigeon4.setField("name", "Leja");
		pigeon4.setField("age", 1);
		pigeon4 = dataService.save(pigeon4, dataHelper.getAuthData(user1, env), env);
		
		// make sure inserting a new pigeon did not affect the visibility of existing ones
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user1.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user2.getId(), env));
		
		// additionally share pigeon 3 manually
		sharingService.shareRecord(pigeon3.getKID(), user2.getId(), true, true, "any", true, null, null, authData, env);
		assertEquals(2, sharingService.find(user2Pigeon3Filter, env).size());
		
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user1.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon3.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user1.getId(), env));
		assertTrue(sharingService.canDeleteRecord(pigeon3.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user2.getId(), env));
		
		// revoke edit and delete permissions
		sharingService.shareRecord(pigeon3.getKID(), user2.getId(), false, false, "any", true, null, null, authData, env);
		assertEquals(2, sharingService.find(user2Pigeon3Filter, env).size());
		
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user1.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user2.getId(), env));
		
		// add edit permission using sharing rule declaration
		sharingRule1 = "@SharingRule(edit = true)\n";
		sharingRule1 += "public static List<User> sharePigeonWithGroups (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id from User }; }\n";
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		assertTrue(sharingService.canEditRecord(pigeon3.getKID(), user1.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon3.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user2.getId(), env));
		
		// make sure there are two URS records for pigeon3 and user2
		assertEquals(2, sharingService.find(user2Pigeon3Filter, env).size());
		
		// remove the sharing rule altogether
		sharingRule1 = "public static List<User> anyMethod (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id from User }; }\n";
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		assertEquals(initialCount, env.getSharingRulesByType().get(pigeonType.getKID()).size());
		
		// make sure now there is one URS record for pigeon3 and user2
		assertEquals(1, sharingService.find(user2Pigeon3Filter, env).size());
		
		// make sure user2 can still access pigeon3 because of generic sharing on this record
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user1.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user2.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user2.getId(), env));
		
		// now create some user group
		UserGroup group1 = new UserGroup();
		group1.setName("PeopleWithAccessToOldPigeons");
		group1 = ugService.save(group1, authData, env);
		
		User user3 = dataHelper.getTestUser("reader3@kommet.io", "reader3@kommet.io", user1.getProfile(), env);
		user3 = userService.save(user3, authData, env);
		
		ugService.assignUserToGroup(user3.getId(), group1.getId(), authData, env, true);
		assertFalse(sharingService.canViewRecord(pigeon1.getKID(), user3.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon2.getKID(), user3.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user3.getId(), env));
		
		// add a sharing rule that shares pigeons with members of this group
		sharingRule1 = "@SharingRule(edit = false)\n";
		sharingRule1 += "public static List<UserGroup> sharePigeonsWithGroup (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id from UserGroup where name = '" + group1.getName() + "' }; }\n";
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user3.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user3.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user3.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon1.getKID(), user3.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon2.getKID(), user3.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon3.getKID(), user3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon1.getKID(), user3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon2.getKID(), user3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user3.getId(), env));
		
		// also allow members of this group to edit these records
		sharingRule1 = "@SharingRule(edit = true)\n";
		sharingRule1 += "public static List<UserGroup> sharePigeonsWithGroup (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id from UserGroup where name = '" + group1.getName() + "' }; }\n";
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		assertTrue(sharingService.canViewRecord(pigeon1.getKID(), user3.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon2.getKID(), user3.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user3.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon1.getKID(), user3.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon2.getKID(), user3.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon3.getKID(), user3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon1.getKID(), user3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon2.getKID(), user3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user3.getId(), env));
		
		sharingService.unshareRecord(pigeon1.getKID(), user1.getId(), authData, env);
		
		// now add a criteria to share only some pigeons (older than 10)
		// but the updateForTypes parameter is empty
		sharingRule1 = "@SharingRule(edit = true, updateForTypes = {})\n";
		sharingRule1 += "public static List<UserGroup> sharePigeonsWithGroup (Pigeon p) throws KommetException {\n";
		sharingRule1 += "if (p.getAge() > 10) { return { select id from UserGroup where name = '" + group1.getName() + "' }; } else { return null; } }\n";
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		// no users can see any pigeons
		assertFalse(sharingService.canViewRecord(pigeon1.getKID(), user3.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon2.getKID(), user3.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user3.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon1.getKID(), user1.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon2.getKID(), user1.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		
		// now change the age of pigeon3 to fulfill the criteria of the sharing rule
		pigeon3.setField("age", 11);
		pigeon3 = dataService.save(pigeon3, env);
		
		// users still shouldn't be able to see the record because the sharing rule was not annotated with dependsOnRecord
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user3.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		
		// now add a criteria to share only some pigeons (older than 20)
		sharingRule1 = "@SharingRule(edit = true)\n";
		sharingRule1 += "public static List<UserGroup> sharePigeonsWithGroup (Pigeon p) throws KommetException {\n";
		sharingRule1 += "if (p.getAge() > 20) { return { select id from UserGroup where name = '" + group1.getName() + "' }; } else { return null; } }\n";
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		boolean isSharingRuleRegistered = false;
		for (SharingRule rule : env.getSharingRulesByType().get(pigeonType.getKID()))
		{
			if (rule.getName().equals("com.sharing.PigeonRules.sharePigeonsWithGroup"))
			{
				isSharingRuleRegistered = true;
				break;
			}
		}
		assertTrue(isSharingRuleRegistered);
		
		// now change the age of pigeon3 to fulfill the criteria of the sharing rule
		pigeon3.setField("age", 21);
		pigeon3 = dataService.save(pigeon3, env);
		
		// user should not be able to see the pigeon because the rule was not annotated with updateForTypes = { "com...Pigeon" }
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user3.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		
		// now set the rule to recalculate for pigeon type changes
		sharingRule1 = "@SharingRule(edit = true, updateForTypes = { \"" + pigeonType.getQualifiedName() + "\"})\n";
		sharingRule1 += "public static List<UserGroup> sharePigeonsWithGroup (Pigeon p) throws KommetException {\n";
		sharingRule1 += "if (p.getAge() > 30) { return { select id from UserGroup where name = '" + group1.getName() + "' }; } else { return null; } }\n";
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		// now change the age of pigeon3 to fulfill the criteria of the sharing rule
		pigeon3.setField("age", 31);
		pigeon3 = dataService.save(pigeon3, env);
		
		// now the user should be able to see the pigeon because the pigeon fulfill sharing criteria (age > 30) and the rule was set to be recalculated
		// when the pigeon type changes
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user3.getId(), env));
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		
		// now add a new member to the group and make sure the also can see the pigeon
		ugService.assignUserToGroup(user1.getId(), group1.getId(), authData, env, true);
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user3.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		
		// remove a member from the group and make sure they lose visibility of the records
		ugService.unassignUserFromGroup(user3.getId(), group1.getId(), authData, env);
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user3.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user1.getId(), env));
		
		// now create a new user and a group
		// now create some user group
		UserGroup group2 = new UserGroup();
		group2.setName("PeopleWithSubAccess");
		group2 = ugService.save(group2, authData, env);
		
		User user4 = dataHelper.getTestUser("reader4@kommet.io", "reader4@kommet.io", user1.getProfile(), env);
		user4 = userService.save(user4, authData, env);
		ugService.assignGroupToGroup(group2.getId(), group1.getId(), authData, env);
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user4.getId(), env));
		ugService.assignUserToGroup(user4.getId(), group2.getId(), authData, env, true);
		
		// the user should have access to pigeon3, because it is shared with group1, and the user belongs to group2 which is a subgroup of group1
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user4.getId(), env));
		assertTrue(sharingService.canEditRecord(pigeon3.getKID(), user4.getId(), env));
		assertFalse(sharingService.canDeleteRecord(pigeon3.getKID(), user4.getId(), env));
		
		// now unassign group2 from group1
		ugService.unassignUserGroupFromGroup(group2.getId(), group1.getId(), false, authData, env);
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user4.getId(), env));
		
		Type drawerType = new Type();
		drawerType.setApiName("Drawer");
		drawerType.setPackage("com.test");
		drawerType.setBasic(false);
		drawerType.setLabel("Drawer");
		drawerType.setPluralLabel("Drawers");
		
		Field userField = new Field();
		userField.setApiName("userId");
		userField.setLabel("User");
		userField.setDataType(new KIDDataType());
		userField.setRequired(true);
		drawerType.addField(userField);
		
		drawerType = dataService.createType(drawerType, env);
		
		// test creation of new group and new user
		sharingRule1 = "@SharingRule\n";
		sharingRule1 += "public static List<UserGroup> sharePigeonsWithGroup (Pigeon p) throws KommetException {\n";
		sharingRule1 += "if (p.getAge() > 30) { return { select id from UserGroup }; } else { return null; } }\n";
		
		String sharingRule2 = "@SharingRule\n";
		sharingRule2 += "public static List<User> sharePigeonsWithUsers (Pigeon p) throws KommetException {\n";
		sharingRule2 += "if (p.getAge() > 30) { java.util.List<User> list = new java.util.ArrayList<User>(); ";
		sharingRule2 += "java.util.List<com.test.Drawer> records = { select userId from com.test.Drawer }; ";
		sharingRule2 += "for (com.test.Drawer r : records) {";
		sharingRule2 += "User u = new User(); u.setId(r.getUserId()); list.add(u); } return list;";
		sharingRule2 += "} else { return null; } }";
		newClass = getSharingRuleClass("com.sharing.PigeonRules", pigeonType, Arrays.asList(sharingRule1, sharingRule2), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		
		assertTrue(isRuleRegisteredAsDependantForType(drawerType, "com.sharing.PigeonRules.sharePigeonsWithUsers", env));
		assertTrue(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.USER_PREFIX)), "com.sharing.PigeonRules.sharePigeonsWithUsers", env));
		assertTrue(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)), "com.sharing.PigeonRules.sharePigeonsWithGroup", env));
		
		User user5 = dataHelper.getTestUser("reader5@kommet.io", "reader5@kommet.io", user1.getProfile(), env);
		user5 = userService.save(user5, authData, env);
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user5.getId(), env));
		
		// create new drawer record
		Record drawer1 = new Record(drawerType);
		drawer1.setField("userId", user5.getId());
		drawer1 = dataService.save(drawer1, env);
		
		// user should now see the record because sharing was recalculated as a result of inserting a new Drawer record
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user5.getId(), env));
		
		UserGroup group3 = new UserGroup();
		group3.setName("NewGroup");
		group3 = ugService.save(group3, authData, env);
		
		java.lang.Class<?> compiledClass = compiler.getClass(cls, false, env);
		Method method = MiscUtils.getMethodByName(compiledClass, "sharePigeonsWithUsers");
		QueriedTypes queriedTypesAnnot = method.getAnnotation(QueriedTypes.class);
		assertNotNull(queriedTypesAnnot);
		assertEquals("Incorrect values: " + MiscUtils.implode(Arrays.asList(queriedTypesAnnot.value()), ","), 1, queriedTypesAnnot.value().length);
		
		assertTrue(sharingService.canGroupViewRecord(pigeon3.getKID(), group3.getId(), env));
		assertTrue(sharingService.canGroupViewRecord(pigeon3.getKID(), group2.getId(), env));
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user5.getId(), env));
		
		testRefreshEnv(pigeonType, env, authData);
		
		assertTrue(sharingService.canViewRecord(pigeon3.getKID(), user5.getId(), env));
		assertTrue(sharingService.canGroupViewRecord(pigeon3.getKID(), group3.getId(), env));
		assertTrue(sharingService.canGroupViewRecord(pigeon3.getKID(), group2.getId(), env));
		assertTrue(isRuleRegisteredAsDependantForType(drawerType, "com.sharing.PigeonRules.sharePigeonsWithUsers", env));
		assertTrue(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.USER_PREFIX)), "com.sharing.PigeonRules.sharePigeonsWithUsers", env));
		assertTrue(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)), "com.sharing.PigeonRules.sharePigeonsWithGroup", env));
		assertEquals(3, env.getDependentSharingRulesByType().get(env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)).getKID()).size());
		
		// delete the declaring class
		classService.delete(cls, dataService, authData, env);
		
		assertFalse(isRuleRegisteredAsDependantForType(drawerType, "com.sharing.PigeonRules.sharePigeonsWithUsers", env));
		assertFalse(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.USER_PREFIX)), "com.sharing.PigeonRules.sharePigeonsWithUsers", env));
		assertFalse(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)), "com.sharing.PigeonRules.sharePigeonsWithGroup", env));
		assertEquals(1, env.getDependentSharingRulesByType().get(env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)).getKID()).size());
		assertFalse(sharingService.canViewRecord(pigeon3.getKID(), user5.getId(), env));
		
		assertFalse(sharingService.canGroupViewRecord(pigeon3.getKID(), group2.getId(), env));
		assertFalse(sharingService.canGroupViewRecord(pigeon3.getKID(), group3.getId(), env));
	}
	
	private void testRefreshEnv(Type type, EnvData env, AuthData authData) throws KommetException
	{
		int sharingRulesForPigeon = env.getSharingRulesByType().get(type.getKID()).size();
		assertTrue(sharingRulesForPigeon > 0);
		
		KID userTypeId = env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID();
		
		int dependentSharingRulesForUser = env.getDependentSharingRulesByType().get(userTypeId).size();
		assertTrue(dependentSharingRulesForUser > 0);
		
		envService.resetEnv(env.getId());
		
		EnvData refreshedEnv = envService.get(env.getId());
		
		assertNotNull(refreshedEnv.getSharingRulesByType().get(type.getKID()));
		assertNotNull(refreshedEnv.getDependentSharingRulesByType().get(userTypeId));
		
		assertEquals(sharingRulesForPigeon, refreshedEnv.getSharingRulesByType().get(type.getKID()).size());
		assertEquals(dependentSharingRulesForUser, refreshedEnv.getDependentSharingRulesByType().get(userTypeId).size());
	}

	private void testSimpleRuleCreation (Type pigeonType, AuthData authData, EnvData env) throws KommetException, ClassNotFoundException, MalformedURLException
	{
		int initialCount = srService.get(new SharingRuleFilter(), authData, env).size();
		assertEquals(0, initialCount);
		
		String sharingRule1 = "@SharingRule\n";
		sharingRule1 += "public static List<User> sharePigeon (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id, profile.name from User }; }\n";
		
		// create class with sharing rules
		String sharingRule2 = "@SharingRule(edit = true)\n";
		sharingRule2 += "public static List<UserGroup> sharePigeonWithGroups (Pigeon p) throws KommetException {\n";
		sharingRule2 += "return null; }\n";
		
		Class cls = getSharingRuleClass("com.sharing.Rules", pigeonType, Arrays.asList(sharingRule1, sharingRule2), authData, env);
		cls = classService.fullSave(cls, dataService, authData, env);
		
		// make sure the @QueriedTypes annotation has been added to the sharePigeon method
		Method sharePigeonMethod = MiscUtils.getMethodByName(compiler.getClass(cls, false, env), "sharePigeon");
		assertNotNull(sharePigeonMethod);
		assertTrue(sharePigeonMethod.isAnnotationPresent(QueriedTypes.class));
		
		QueriedTypes queriedTypes = sharePigeonMethod.getAnnotation(QueriedTypes.class);
		assertEquals("Incorrect @QueriedTypes values " + queriedTypes.value(), 2, queriedTypes.value().length);
		
		// verify @SharedWith annotation for the first method
		SharedWith sharedWithAnnot = sharePigeonMethod.getAnnotation(SharedWith.class);
		assertNotNull(sharedWithAnnot);
		assertEquals("Incorrect @SharedWith values " + queriedTypes.value(), "User", sharedWithAnnot.value());
		
		// verify @SharedWith annotation for the second method
		Method sharePigeonGroupMethod = MiscUtils.getMethodByName(compiler.getClass(cls, false, env), "sharePigeonWithGroups");
		sharedWithAnnot = sharePigeonGroupMethod.getAnnotation(SharedWith.class);
		assertNotNull(sharedWithAnnot);
		assertEquals("Incorrect @SharedWith values " + queriedTypes.value(), "UserGroup", sharedWithAnnot.value());
		
		boolean containsUserType = false;
		boolean containsProfileType = false;
		
		Type profileType = env.getType(KeyPrefix.get(KID.PROFILE_PREFIX));
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		Type userGroupType = env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX));
		
		for (String type : queriedTypes.value())
		{
			if (type.equals(userType.getQualifiedName()))
			{
				containsUserType = true;
			}
			else if (type.equals(profileType.getQualifiedName()))
			{
				containsProfileType = true;
			}
		}
		
		assertTrue(containsUserType);
		assertTrue(containsProfileType);
		
		// make sure the sharing rule is registered on the env
		assertNotNull(env.getDependentSharingRulesByType().get(profileType.getKID()));
		assertNotNull(env.getDependentSharingRulesByType().get(userType.getKID()));
		
		assertTrue(isRuleRegisteredAsDependantForType(profileType, "com.sharing.Rules.sharePigeon", env));
		assertTrue(isRuleRegisteredAsDependantForType(userType, "com.sharing.Rules.sharePigeon", env));
		assertFalse(isRuleRegisteredAsDependantForType(userGroupType, "com.sharing.Rules.sharePigeon", env));
		assertFalse(isRuleRegisteredAsDependantForType(userType, "com.sharing.Rules.sharePigeonWithGroups", env));
		assertTrue(isRuleRegisteredAsDependantForType(userGroupType, "com.sharing.Rules.sharePigeonWithGroups", env));
		
		List<SharingRule> rules = srService.get(new SharingRuleFilter(), authData, env);
		assertEquals(2, rules.size());
		
		assertEquals(2, env.getSharingRulesByType().get(pigeonType.getKID()).size());
		
		for (SharingRule rule : env.getSharingRulesByType().get(pigeonType.getKID()))
		{
			if (rule.getName().endsWith("sharePigeon"))
			{
				assertEquals("User", rule.getSharedWith());
			}
			else if (rule.getName().endsWith("sharePigeonWithGroups"))
			{
				assertEquals("UserGroup", rule.getSharedWith());
			}
		}
		
		// find the first rule
		SharingRuleFilter filter = new SharingRuleFilter();
		filter.setName("com.sharing.Rules.sharePigeon");
		rules = srService.get(filter, authData, env);
		assertEquals(1, rules.size());
		
		// make sure the fetched rule has all the properties defined in code
		assertEquals(pigeonType.getKID(), rules.get(0).getReferencedType());
		assertFalse(rules.get(0).getIsEdit());
		assertFalse(rules.get(0).getIsDelete());
		assertEquals(cls.getId(), rules.get(0).getFile().getId());
		assertEquals("sharePigeon", rules.get(0).getMethod());
		
		assertEquals(2, srService.get(new SharingRuleFilter(), authData, env).size());
		
		// redefine the rule, removing one of the queried types
		sharingRule1 = "@SharingRule\n";
		sharingRule1 += "public static List<User> sharePigeon (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return { select id, userName from User }; }\n";
		
		// create class with sharing rules
		sharingRule2 = "@SharingRule(edit = true)\n";
		sharingRule2 += "public static List<UserGroup> sharePigeonWithGroups (Pigeon p) throws KommetException {\n";
		sharingRule2 += "return null; }\n";
		
		Class newClass = getSharingRuleClass("com.sharing.Rules", pigeonType, Arrays.asList(sharingRule1, sharingRule2), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		assertFalse(isRuleRegisteredAsDependantForType(profileType, "com.sharing.Rules.sharePigeon", env));
		assertTrue(isRuleRegisteredAsDependantForType(userType, "com.sharing.Rules.sharePigeon", env));
		assertFalse(isRuleRegisteredAsDependantForType(userGroupType, "com.sharing.Rules.sharePigeon", env));
		assertFalse(isRuleRegisteredAsDependantForType(userType, "com.sharing.Rules.sharePigeonWithGroups", env));
		assertTrue(isRuleRegisteredAsDependantForType(userGroupType, "com.sharing.Rules.sharePigeonWithGroups", env));
		
		// redefine the rule, removing all queried types
		sharingRule1 = "@SharingRule\n";
		sharingRule1 += "public static List<User> sharePigeon (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return null; }\n";
		
		// create class with sharing rules
		sharingRule2 = "@SharingRule(edit = true)\n";
		sharingRule2 += "public static List<UserGroup> sharePigeonWithGroups (Pigeon p) throws KommetException {\n";
		sharingRule2 += "return null; }\n";
		
		newClass = getSharingRuleClass("com.sharing.Rules", pigeonType, Arrays.asList(sharingRule1, sharingRule2), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		assertFalse(isRuleRegisteredAsDependantForType(profileType, "com.sharing.Rules.sharePigeon", env));
		assertTrue(isRuleRegisteredAsDependantForType(userType, "com.sharing.Rules.sharePigeon", env));
		assertFalse(isRuleRegisteredAsDependantForType(profileType, "com.sharing.Rules.sharePigeonWithGroups", env));
		assertFalse(isRuleRegisteredAsDependantForType(userType, "com.sharing.Rules.sharePigeonWithGroups", env));
		assertTrue(isRuleRegisteredAsDependantForType(userGroupType, "com.sharing.Rules.sharePigeonWithGroups", env));
		
		// redefine the rule, adding queried types manually
		sharingRule1 = "@SharingRule(updateForTypes = { \"kommet.basic.Task\", \"kommet.basic.Event\" })\n";
		sharingRule1 += "public static List<User> sharePigeon (Pigeon p) throws KommetException {\n";
		sharingRule1 += "return null; }\n";
		
		// create class with sharing rules
		sharingRule2 = "@SharingRule(edit = true)\n";
		sharingRule2 += "public static List<UserGroup> sharePigeonWithGroups (Pigeon p) throws KommetException {\n";
		sharingRule2 += "return null; }\n";
		
		newClass = getSharingRuleClass("com.sharing.Rules", pigeonType, Arrays.asList(sharingRule1, sharingRule2), authData, env);
		cls.setKollCode(newClass.getKollCode());
		cls = classService.fullSave(cls, dataService, authData, env);
		assertFalse(isRuleRegisteredAsDependantForType(profileType, "com.sharing.Rules.sharePigeon", env));
		assertTrue(isRuleRegisteredAsDependantForType(userType, "com.sharing.Rules.sharePigeon", env));
		assertFalse(isRuleRegisteredAsDependantForType(profileType, "com.sharing.Rules.sharePigeonWithGroups", env));
		assertFalse(isRuleRegisteredAsDependantForType(userType, "com.sharing.Rules.sharePigeonWithGroups", env));
		assertTrue(isRuleRegisteredAsDependantForType(userGroupType, "com.sharing.Rules.sharePigeonWithGroups", env));
		assertTrue(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.EVENT_PREFIX)), "com.sharing.Rules.sharePigeon", env));
		assertTrue(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.TASK_PREFIX)), "com.sharing.Rules.sharePigeon", env));
		
		// now delete the declaring class altogether
		classService.delete(cls, dataService, authData, env);
		assertNull(srService.get("com.sharing.Rules.sharePigeon", authData, env));
		assertTrue(env.getSharingRulesByType().get(pigeonType.getKID()).isEmpty());
		assertFalse(isRuleRegisteredAsDependantForType(userGroupType, "com.sharing.Rules.sharePigeonWithGroups", env));
		assertFalse(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.EVENT_PREFIX)), "com.sharing.Rules.sharePigeon", env));
		assertFalse(isRuleRegisteredAsDependantForType(env.getType(KeyPrefix.get(KID.TASK_PREFIX)), "com.sharing.Rules.sharePigeon", env));
		
		// now save the class again so that subsequent tests can use its data
		cls.uninitializeId();
		classService.fullSave(cls, dataService, authData, env);
		assertNotNull(srService.get("com.sharing.Rules.sharePigeon", authData, env));
	}
	
	private boolean isRuleRegisteredAsDependantForType(Type type, String ruleName, EnvData env)
	{
		if (!env.getDependentSharingRulesByType().containsKey(type.getKID()))
		{
			return false;
		}
		
		boolean ruleRegistered = false;
		for (SharingRule registeredRule : env.getDependentSharingRulesByType().get(type.getKID()))
		{
			if (registeredRule.getName().equals(ruleName))
			{
				ruleRegistered = true;
				break;
			}
		}
		
		return ruleRegistered;
	}

	private Class getSharingRuleClass(String name, Type pigeonType, List<String> methods, AuthData authData, EnvData env) throws KommetException
	{
		List<String> nameParts = MiscUtils.splitByLastDot(name);
		
		Class file = new Class();
		file.setName(nameParts.get(1));
		file.setPackageName(nameParts.get(0));
		
		StringBuilder code = new StringBuilder("package " + nameParts.get(0) + ";\n\n");
		
		code.append("import ").append(kommet.koll.annotations.SharingRule.class.getName()).append(";\n");
		code.append("import ").append(User.class.getName()).append(";\n");
		code.append("import ").append(UserGroup.class.getName()).append(";\n");
		code.append("import ").append(pigeonType.getQualifiedName()).append(";\n");
		code.append("import ").append(List.class.getName()).append(";\n");
		code.append("import ").append(KommetException.class.getName()).append(";\n");
		
		// add class body
		code.append("public class " + nameParts.get(1)).append(" {");
		
		for (String m : methods)
		{
			code.append(m);
		}
		
		// end class body
		code.append("\n}");
		
		file.setKollCode(code.toString());
		file.setIsSystem(false);
		
		return file;
	}
}	
