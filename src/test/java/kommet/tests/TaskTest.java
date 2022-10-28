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

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kommet.auth.AuthData;
import kommet.basic.FieldHistory;
import kommet.basic.Profile;
import kommet.basic.Task;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.basic.UserRecordSharing;
import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.data.sharing.UserRecordSharingFilter;
import kommet.env.EnvData;
import kommet.filters.FieldHistoryFilter;
import kommet.filters.TaskFilter;
import kommet.services.FieldHistoryService;
import kommet.services.TaskService;
import kommet.services.UserGroupService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class TaskTest extends BaseUnitTest
{
	@Inject
	TaskService taskService;
	
	@Inject
	AppConfig config;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	FieldHistoryService fhService;
	
	@Inject
	UserGroupService ugService;
	
	@Inject
	SharingService sharingService;
	
	private static final Logger log = LoggerFactory.getLogger(TaskTest.class);
	
	@Test
	public void testCreateTasks() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Task task1 = new Task();
		task1.setTitle("Fix the perch");
		
		try
		{
			taskService.save(task1, authData, env);
			fail("Saving task without content and due date should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
		}
		
		task1.setContent("Some text");
		try
		{
			taskService.save(task1, authData, env);
			fail("Saving task without due date should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
		}
		
		task1.setDueDate(new Date());
		task1.setPriority(1);
		task1.setStatus("Open");
		task1.setAssignedUser(authData.getUser());
		task1 = taskService.save(task1, authData, env);
		assertNotNull(task1.getId());
		
		// create another user
		User testUser = dataHelper.getTestUser("test@kommet.io", "test@kommet.io", authData.getProfile(), env);
		assertNotNull(testUser.getId());
		
		// create another task
		Task task2 = new Task();
		task2.setTitle("Fix the car");
		task2.setContent("This is my automobile");
		task2.setDueDate(MiscUtils.addDays(new Date(), 1));
		task2.setPriority(2);
		task2.setStatus("Resolved");
		task2.setAssignedUser(testUser);
		task2 = taskService.save(task2, authData, env);
		assertNotNull(task2);
		
		// find tasks for user
		TaskFilter filter = new TaskFilter();
		filter.addAssignedUserId(testUser.getId());
		List<Task> tasksForUser = taskService.get(filter, authData, env);
		assertFalse(tasksForUser.isEmpty());
		assertEquals(1, tasksForUser.size());
		assertEquals(task2.getId(), tasksForUser.get(0).getId());
		
		// find tasks by title
		filter = new TaskFilter();
		filter.setTitleLike("erch");
		tasksForUser = taskService.get(filter, authData, env);
		assertFalse(tasksForUser.isEmpty());
		assertEquals(1, tasksForUser.size());
		assertEquals(task1.getId(), tasksForUser.get(0).getId());
		
		// find tasks by content
		filter = new TaskFilter();
		filter.setContentLike("AutoMobile");
		tasksForUser = taskService.get(filter, authData, env);
		assertFalse(tasksForUser.isEmpty());
		assertEquals(1, tasksForUser.size());
		assertEquals(task2.getId(), tasksForUser.get(0).getId());
		
		// delete task
		taskService.delete(task1.getId(), authData, env);
		
		testTaskPercentage(task2, authData, env);
		testTaskStatusHistory(task2, authData, env);
		testUserAndGroupAssignee(task2, authData, env);
		testAssigneeSharing(authData, env);
	}

	private void testAssigneeSharing(AuthData authData, EnvData env) throws KommetException
	{
		// create a test user
		Profile testProfile = dataHelper.getTestProfileObject("TestProfile", env);
		User student1 = dataHelper.getTestUser("student1@kommet.io", "student1@kommet.io", testProfile, env);
		User student2 = dataHelper.getTestUser("student2@kommet.io", "student2@kommet.io", testProfile, env);
		User student3 = dataHelper.getTestUser("student3@kommet.io", "student3@kommet.io", testProfile, env);
		User student4 = dataHelper.getTestUser("student4@kommet.io", "student4@kommet.io", testProfile, env);
		
		// create a task and assign it to the first user
		Task task1 = new Task();
		task1.setTitle("Do your homework");
		task1.setContent("Do it!");
		task1.setDueDate(new Date());
		task1.setPriority(1);
		task1.setStatus("Open");
		task1.setAssignedUser(student1);
		task1.setRecordId(student2.getId());
		task1 = taskService.save(task1, authData, env);
		
		Task task2 = new Task();
		task2.setTitle("Do your homework 2");
		task2.setContent("Do it!");
		task2.setDueDate(new Date());
		task2.setPriority(1);
		task2.setStatus("In progress");
		task2.setAssignedUser(student1);
		task2 = taskService.save(task2, authData, env);
		
		TaskFilter filter = new TaskFilter();
		filter.addRecordId(student2.getId());
		List<Task> tasksForStudent = taskService.get(filter, authData, env);
		assertEquals(1, tasksForStudent.size());
		
		// make sure the assigned user can read and edit the task
		assertTrue(sharingService.canViewRecord(task1.getId(), student1.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student1.getId(), env));
		
		// make sure the other student cannot see the task
		assertFalse(sharingService.canViewRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canEditRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student2.getId(), env));
		
		// change assignee
		task1.setAssignedUser(student2);
		task1 = taskService.save(task1, authData, env);
		
		// make sure the assigned user can read and edit the task
		assertTrue(sharingService.canViewRecord(task1.getId(), student2.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canViewRecord(task1.getId(), student1.getId(), env));
		assertFalse(sharingService.canEditRecord(task1.getId(), student1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student1.getId(), env));
		
		// now add a manual sharing for the task and the second student (not the assignee)
		sharingService.shareRecord(task1.getId(), student1.getId(), true, true, authData, "Some reason", true, env);
		assertTrue(sharingService.canViewRecord(task1.getId(), student1.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student1.getId(), env));
		assertTrue(sharingService.canDeleteRecord(task1.getId(), student1.getId(), env));
		
		Integer sharingCount = sharingService.find(null, env).size();
		
		// now add a manual sharing for the assignee
		sharingService.shareRecord(task1.getId(), student2.getId(), true, true, authData, "Some reason", true, env);
		
		assertEquals(sharingCount + 1, sharingService.find(null, env).size());
		
		assertTrue(sharingService.canViewRecord(task1.getId(), student2.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student2.getId(), env));
		assertTrue(sharingService.canDeleteRecord(task1.getId(), student2.getId(), env));
		
		// now reassign the task again
		task1.setAssignedUser(student3);
		task1 = taskService.save(task1, authData, env);
		
		// reassigning assignee should remove one sharing and add another
		// so the total number of sharings should not change
		assertEquals(sharingCount + 1, sharingService.find(null, env).size());
		
		// make sure the manual sharing still has effect
		assertTrue(sharingService.canViewRecord(task1.getId(), student2.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student2.getId(), env));
		assertTrue(sharingService.canDeleteRecord(task1.getId(), student2.getId(), env));
		
		// find all sharings
		sharingCount = sharingService.find(null, env).size();
		
		sharingService.unshareRecord(task1.getId(), student2.getId(), authData, env);
		
		assertEquals(sharingCount - 1, sharingService.find(null, env).size());
		
		assertFalse(sharingService.canViewRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canEditRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student2.getId(), env));
		
		// now create a user group for marine biology
		UserGroup marineBiologyGroup = new UserGroup();
		marineBiologyGroup.setName("MarineBiologyStudents");
		marineBiologyGroup = ugService.save(marineBiologyGroup, authData, env);
		assertNotNull(marineBiologyGroup.getId());
		
		// add some students to the group
		ugService.assignUserToGroup(student1.getId(), marineBiologyGroup.getId(), authData, env, true);
		ugService.assignUserToGroup(student2.getId(), marineBiologyGroup.getId(), authData, env, true);
		
		// now reassign the task again
		task1.setAssignedUser(student4);
		task1 = taskService.save(task1, authData, env);
		
		// make sure the students don't have access to the task
		assertTrue(sharingService.canViewRecord(task1.getId(), student1.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student1.getId(), env));
		assertTrue(sharingService.canDeleteRecord(task1.getId(), student1.getId(), env));
		assertFalse(sharingService.canViewRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canEditRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canViewRecord(task1.getId(), student3.getId(), env));
		assertFalse(sharingService.canEditRecord(task1.getId(), student3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student3.getId(), env));
		
		// assign the task to the marine biology students group
		task1.setAssignedUser(null);
		task1.nullify("assignedUser");
		task1.setAssignedGroup(marineBiologyGroup);
		
		sharingCount = sharingService.find(null, env).size();
		taskService.save(task1, authData, env);
		
		Task refetchedTask = taskService.get(task1.getId(), authData, env);
		assertNull(refetchedTask.getAssignedUser());
		assertNotNull(refetchedTask.getAssignedGroup());
		
		// sharing the task with a group that has two members should result
		// in two new URS records, minus one that was deleted for the previous assignee
		assertEquals(sharingCount + 1, sharingService.find(null, env).size());
		
		// make sure students have been correctly assigned to the group
		assertTrue(ugService.isUserInGroup(student1.getId(), marineBiologyGroup.getId(), true, env));
		assertTrue(ugService.isUserInGroup(student2.getId(), marineBiologyGroup.getId(), true, env));
		assertFalse(ugService.isUserInGroup(student3.getId(), marineBiologyGroup.getId(), true, env));
		
		// find sharings for student1 and task1
		UserRecordSharingFilter ursFilter = new UserRecordSharingFilter();
		ursFilter.addRecordId(task1.getId());
		ursFilter.addUserId(student1.getId());
		List<UserRecordSharing> student1Sharings = sharingService.find(ursFilter, env);
		assertEquals(2, student1Sharings.size());
		
		boolean genericSharingFound = false;
		boolean nonGenericSharingFound = false;
		
		for (UserRecordSharing urs : student1Sharings)
		{
			if (urs.getIsGeneric())
			{
				genericSharingFound = true;
			}
			else if (!urs.getIsGeneric())
			{
				nonGenericSharingFound = true;
			}
		}
		
		assertTrue(genericSharingFound);
		assertTrue(nonGenericSharingFound);
		
		assertTrue(sharingService.canViewRecord(task1.getId(), student1.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student1.getId(), env));
		assertTrue(sharingService.canDeleteRecord(task1.getId(), student1.getId(), env));
		assertTrue(sharingService.canViewRecord(task1.getId(), student2.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student2.getId(), env));
		assertFalse(sharingService.canViewRecord(task1.getId(), student3.getId(), env));
		assertFalse(sharingService.canEditRecord(task1.getId(), student3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student3.getId(), env));
		
		// remove the manual sharing for the task and student1
		sharingService.unshareRecord(task1.getId(), student1.getId(), authData, env);
		
		student1Sharings = sharingService.find(ursFilter, env);
		assertEquals(1, student1Sharings.size());
		assertTrue(student1Sharings.get(0).getRead());
		assertTrue(student1Sharings.get(0).getEdit());
		assertFalse(student1Sharings.get(0).getDelete());
		
		assertTrue(sharingService.canViewRecord(task1.getId(), student1.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student1.getId(), env));
		
		// add the third student to the assigned group
		ugService.assignUserToGroup(student3.getId(), marineBiologyGroup.getId(), authData, env, true);
		assertTrue(sharingService.canViewRecord(task1.getId(), student3.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student3.getId(), env));
		
		// remove the first student from the group
		ugService.unassignUserFromGroup(student1.getId(), marineBiologyGroup.getId(), authData, env);
		assertFalse(sharingService.canViewRecord(task1.getId(), student1.getId(), env));
		assertFalse(sharingService.canEditRecord(task1.getId(), student1.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student1.getId(), env));
		
		// create another group
		UserGroup mathStudentGroup = new UserGroup();
		mathStudentGroup.setName("MathStudents");
		mathStudentGroup = ugService.save(mathStudentGroup, authData, env);
		assertNotNull(mathStudentGroup.getId());
		
		ugService.assignUserToGroup(student3.getId(), mathStudentGroup.getId(), authData, env, true);
		
		// share the task with the new group
		sharingService.shareRecordWithGroup(task1.getId(), mathStudentGroup.getId(), authData, "Bla bla", true, env);
		assertTrue(sharingService.canViewRecord(task1.getId(), student3.getId(), env));
		assertTrue(sharingService.canEditRecord(task1.getId(), student3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student3.getId(), env));
		
		// now unset the assignee for the task
		task1.setAssignedGroup(null);
		task1.nullify("assignedGroup");
		task1.setAssignedUser(null);
		task1.nullify("assignedUser");
		
		taskService.save(task1, authData, env);
		
		// refetch task
		refetchedTask = taskService.get(task1.getId(), authData, env);
		assertNull(refetchedTask.getAssignedGroup());
		assertNull(refetchedTask.getAssignedUser());
		
		assertFalse(sharingService.canGroupViewRecord(task1.getId(), marineBiologyGroup.getId(), env));
		assertFalse(sharingService.canGroupEditRecord(task1.getId(), marineBiologyGroup.getId(), env));
		assertTrue(sharingService.canViewRecord(task1.getId(), student3.getId(), env));
		assertFalse(sharingService.canEditRecord(task1.getId(), student3.getId(), env));
		assertFalse(sharingService.canDeleteRecord(task1.getId(), student3.getId(), env));
	}

	private void testUserAndGroupAssignee(Task task, AuthData authData, EnvData env) throws KommetException
	{
		// create user group
		UserGroup ug = new UserGroup();
		ug.setName("TaskAssignees");
		ug = ugService.save(ug, authData, env);
		assertNotNull(ug.getId());
		
		// assign task to group
		task.setAssignedGroup(ug);
		
		try
		{
			// make sure it is not possible to save task with both user and group assigned
			taskService.save(task, authData, env);
			fail("Saving task with both user and user group assigned should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
			assertTrue(e.getMessage().startsWith("Both assigned user and assigned group cannot be set on a task"));
		}
	}

	private void testTaskStatusHistory(Task task2, AuthData authData, EnvData env) throws KommetException
	{
		Date dateFrom = new Date();
		// change task status to In Progress
		task2.setStatus("In progress");
		taskService.save(task2, authData, env);
		
		FieldHistoryFilter filter = new FieldHistoryFilter();
		filter.addRecordId(task2.getId());
		filter.setDateFrom(dateFrom);
		
		List<FieldHistory> fhs = fhService.get(filter, env);
		assertFalse(fhs.isEmpty());
		assertEquals(1, fhs.size());
		
		FieldHistory fh = fhs.get(0);
		assertEquals("Resolved", fh.getOldValue());
		assertEquals("In progress", fh.getNewValue());
	}

	private void testTaskPercentage(Task task, AuthData authData, EnvData env) throws KommetException
	{
		// try to save a task with incorrect percentage
		task.setProgress(101);
		
		try
		{
			taskService.save(task, authData, env);
			fail("Saving task with incorrect progress value should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue(e.getMessage().startsWith("Progress must be expressed"));
		}
		
		// now set the progress to a correct value
		task.setProgress(97);
		taskService.save(task, authData, env);
	}
}
