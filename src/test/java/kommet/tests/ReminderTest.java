/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.Profile;
import kommet.basic.Reminder;
import kommet.basic.Task;
import kommet.basic.User;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.services.ReminderService;
import kommet.services.TaskService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class ReminderTest extends BaseUnitTest
{
	@Inject
	AppConfig config;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	ReminderService reminderService;
	
	@Inject
	TaskService taskService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UserService userService;
	
	@Test
	public void testButtons() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Date dueDate = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(dueDate);
		c.add(Calendar.MINUTE, -2);
		dueDate = c.getTime();
		
		Task task1 = new Task();
		task1.setTitle("Fix the perch");
		task1.setContent("Some text");
		task1.setDueDate(dueDate);
		task1.setPriority(1);
		task1.setStatus("Open");
		task1.setAssignedUser(authData.getUser());
		task1 = taskService.save(task1, authData, env);
		assertNotNull(task1.getId());
		
		// add reminder to task
		Reminder reminder = new Reminder();
		reminder.setTitle("Task reminder: " + task1.getTitle());
		reminder.setContent("Task is due");
		reminder.setRecordId(task1.getId());
		reminder.setReferencedField(env.getType(KeyPrefix.get(KID.TASK_PREFIX)).getField("dueDate").getKID());
		reminder.setIntervalUnit("minute");
		reminder.setIntervalValue(1);
		reminder.setMedia("notification");
		reminder.setAssignedUser(task1.getAssignedUser());
		reminder = reminderService.save(reminder, authData, env);
		
		assertNotNull(reminder.getId());
		
		// fetch reminder with status
		reminder = reminderService.get(reminder.getId(), authData, env);
		
		assertEquals("not sent", reminder.getStatus());
		
		Record taskRec = reminderService.isReminderToSend(reminder, new Date(), authData, env);
		assertNotNull(taskRec);
		assertEquals(task1.getId(), taskRec.getField(Field.ID_FIELD_NAME));
		
		reminder.setStatus("sent");
		reminderService.save(reminder, authData, env);
		assertNull(reminderService.isReminderToSend(reminder, new Date(), authData, env));
		
		// change task assignee
		Profile profile = new Profile();
		profile.setName("TestProfile");
		profile.setLabel("TestProfile");
		profile.setSystemProfile(false);
		profile = profileService.save(profile, dataHelper.getRootAuthData(env), env);
		assertNotNull(profile.getId());
		
		// create user
		User user = new User();
		user.setProfile(profile);
		user.setUserName("test");
		user.setEmail("test@kolmu.com");
		user.setPassword(MiscUtils.getSHA1Password("test"));
		user.setTimezone("GMT");
		user.setLocale("EN_US");
		user.setIsActive(true);
		
		user = userService.save(user, dataHelper.getRootAuthData(env), env);
		assertNotNull(user.getId());
		
		// update task assignee
		task1.setAssignedUser(user);
		taskService.save(task1, authData, env);
		
		reminder = reminderService.get(reminder.getId(), authData, env);
		
		// make sure assignee has been updated on reminder when task was updated
		assertEquals(user.getId(), reminder.getAssignedUser().getId());
	
	}
}
