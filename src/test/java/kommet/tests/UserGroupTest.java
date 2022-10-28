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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.basic.Profile;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.basic.UserGroupAssignmentException;
import kommet.basic.types.SystemTypes;
import kommet.dao.UserGroupFilter;
import kommet.data.DataService;
import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.services.UserGroupService;

public class UserGroupTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Inject
	UserGroupService userGroupService;
	
	@Inject
	UserService userService;
	
	@Test
	public void testUserGroupCRUD() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create user group
		UserGroup group1 = new UserGroup();
		group1.setName("Group1");
		group1 = userGroupService.save(group1, authData, env);
		assertNotNull(group1.getId());
		
		// try creating another group with the same name
		UserGroup group2 = new UserGroup();
		group2.setName("Group2");
		group2.setDescription("Some description");
		group2 = userGroupService.save(group2, authData, env);
		assertNotNull(group2.getId());
		
		UserGroup retrievedGroup = userGroupService.getByName("Group1", authData, env);
		assertNotNull(retrievedGroup);
		assertEquals(group1.getId(), retrievedGroup.getId());
		
		assertEquals(2, userGroupService.get(new UserGroupFilter(), authData, env).size());
		
		// delete group
		userGroupService.delete(group2.getId(), authData, env);
		assertEquals(1, userGroupService.get(new UserGroupFilter(), authData, env).size());
		
		testGroupAssignments(group1, group2, authData, env);
		testNestedGroupAssignment(authData, env);
		
		UserGroup group3 = new UserGroup();
		group3.setName("Group1");
		
		try
		{
			group3 = userGroupService.save(group3, authData, env);
			fail("Inserting two user groups with the same name should fail");
		}
		catch (UniqueCheckViolationException e)
		{
			// expected
		}
	}

	/**
	 * Makes sure we can correctly check if a user belongs to a group, directly or indirectly
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	private void testNestedGroupAssignment(AuthData authData, EnvData env) throws KommetException
	{
		// create two groups
		UserGroup group1 = new UserGroup();
		group1.setName("Group11");
		group1.setDescription("Some description");
		group1 = userGroupService.save(group1, authData, env);
		assertNotNull(group1.getId());
		
		UserGroup group2 = new UserGroup();
		group2.setName("Group2");
		group2.setDescription("Some description");
		group2 = userGroupService.save(group2, authData, env);
		assertNotNull(group2.getId());
		
		UserGroup group3 = new UserGroup();
		group3.setName("Group3");
		group3.setDescription("Some description");
		group3 = userGroupService.save(group3, authData, env);
		assertNotNull(group3.getId());
		
		UserGroup group12 = new UserGroup();
		group12.setName("Group10");
		group12.setDescription("Some description");
		group12 = userGroupService.save(group12, authData, env);
		assertNotNull(group12.getId());
		
		Profile profile = dataHelper.getTestProfileObject("TestProfile1", env);
		User user = dataHelper.getTestUser("u1@kommet.io", "u1@kommet.io", profile, env);
		
		userGroupService.assignGroupToGroup(group12.getId(), group1.getId(), authData, env);
		userGroupService.assignUserToGroup(user.getId(), group12.getId(), authData, env);
		userGroupService.assignUserToGroup(user.getId(), group2.getId(), authData, env);
		
		assertTrue(userGroupService.isUserInGroup(user.getId(), group2.getId(), true, env));
		assertTrue(userGroupService.isUserInGroup(user.getId(), group12.getId(), true, env));
		assertFalse(userGroupService.isUserInGroup(user.getId(), group3.getId(), true, env));
		
		// now create a subgroup of group 1.2
		UserGroup group121 = new UserGroup();
		group121.setName("Group121");
		group121.setDescription("Some description");
		group121 = userGroupService.save(group121, authData, env);
		assertNotNull(group121.getId());
		
		userGroupService.assignGroupToGroup(group121.getId(), group12.getId(), authData, env);
		assertTrue(userGroupService.isGroupInGroup(group12.getId(), group1.getId(), true, env));
		assertTrue(userGroupService.isGroupInGroup(group121.getId(), group12.getId(), true, env));
		assertTrue(userGroupService.isGroupInGroup(group121.getId(), group1.getId(), false, env));
		assertFalse(userGroupService.isGroupInGroup(group121.getId(), group1.getId(), true, env));
	}

	@SuppressWarnings("unchecked")
	private void testGroupAssignments(UserGroup group1, UserGroup group2, AuthData authData, EnvData env) throws KommetException
	{
		// create some group
		UserGroup parentGroup = new UserGroup();
		parentGroup.setName("ParentGroup");
		userGroupService.save(parentGroup, authData, env);

		Record testProfile = dataService.save(dataHelper.getTestProfile("TestProfile", env), env);
		
		// create a user
		dataService.save(dataHelper.getTestUser("testuser", "testuser@kommet.io", testProfile, env), env);
		User testUser = userService.get("testuser", env);
		assertNotNull(testUser);
		
		assertFalse(userGroupService.isUserInGroup(testUser.getId(), parentGroup.getId(), true, env));
		assertFalse(userGroupService.isUserInGroup(testUser.getId(), parentGroup.getId(), false, env));
		
		// assign user to group
		userGroupService.assignUserToGroup(testUser.getId(), parentGroup.getId(), authData, env);
		
		// make sure the same user cannot be assigned to the group twice
		try
		{
			userGroupService.assignUserToGroup(testUser.getId(), parentGroup.getId(), authData, env);
			fail("The same user cannot be assigned to the group twice");
		}
		catch (UserGroupAssignmentException e)
		{
			assertEquals("User is already assigned to group", e.getMessage());
		}
		
		assertTrue(userGroupService.isUserInGroup(testUser.getId(), parentGroup.getId(), true, env));
		assertTrue(userGroupService.isUserInGroup(testUser.getId(), parentGroup.getId(), false, env));
		
		// now try getting group with users
		UserGroupFilter filter = new UserGroupFilter();
		filter.setInitUsers(true);
		filter.addUserGroupId(group1.getId());
		List<UserGroup> foundGroups = userGroupService.get(filter, authData, env);
		assertEquals(1, foundGroups.size());
		
		
		// query group with users
		Record group = env.getSelectCriteriaFromDAL("select id, users.id, users.userName from " + SystemTypes.USER_GROUP_API_NAME + " where id = '" + parentGroup.getId() + "'").list().get(0);
		assertEquals(1, ((List<Record>)group.getField("users")).size());
		Record subuser = ((List<Record>)group.getField("users")).get(0);
		assertEquals(testUser.getId(), subuser.getKID());
		
		assertFalse(userGroupService.isGroupInGroup(group1.getId(), parentGroup.getId(), true, env));
		
		// assign group to group
		userGroupService.assignGroupToGroup(group1.getId(), parentGroup.getId(), authData, env);
		
		try
		{
			userGroupService.assignGroupToGroup(group1.getId(), parentGroup.getId(), authData, env);
			fail("The same subgroup cannot be assigned to the group twice");
		}
		catch (UserGroupAssignmentException e)
		{
			assertEquals("Group is already assigned to group", e.getMessage());
		}
		
		assertTrue(userGroupService.isGroupInGroup(group1.getId(), parentGroup.getId(), true, env));
		assertFalse(userGroupService.isGroupInGroup(group2.getId(), parentGroup.getId(), true, env));
		
		// query group with subgroups
		group = env.getSelectCriteriaFromDAL("select id, users.id, users.userName, subgroups.id, subgroups.name from " + SystemTypes.USER_GROUP_API_NAME + " where id = '" + parentGroup.getId() + "'").list().get(0);
		assertEquals(1, ((List<Record>)group.getField("subgroups")).size());
		Record subgroup = ((List<Record>)group.getField("subgroups")).get(0);
		assertEquals(group1.getId(), subgroup.getKID());
		
		// make sure it is not possible to assign a group to itself
		try
		{
			userGroupService.assignGroupToGroup(parentGroup.getId(), parentGroup.getId(), authData, env);
			fail("Assigning user group to itself should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue(e.getMessage().startsWith("Child group must be different from parent group"));
		}
		
		// now unassign user from group
		userGroupService.unassignUserFromGroup(testUser.getId(), parentGroup.getId(), authData, env);
		// also try unassigning user from a group to which they don't actually belong
		userGroupService.unassignUserFromGroup(testUser.getId(), group1.getId(), authData, env);
		assertFalse(userGroupService.isUserInGroup(testUser.getId(), parentGroup.getId(), true, env));
		
		userGroupService.unassignUserGroupFromGroup(group1.getId(), parentGroup.getId(), false, authData, env);
		assertFalse(userGroupService.isGroupInGroup(group1.getId(), parentGroup.getId(), true, env));
	}
}
