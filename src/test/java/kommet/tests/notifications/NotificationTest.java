/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.notifications;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.Notification;
import kommet.basic.User;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.notifications.NotificationException;
import kommet.notifications.NotificationService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class NotificationTest extends BaseUnitTest
{
	@Inject
	NotificationService notificationService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	UserService userService;
	
	@Test
	public void testNotificationCRUD() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Record testProfile = dataHelper.getTestProfile("TestProfile", env);
		dataService.save(testProfile, env);
		
		permissionService.setTypePermissionForProfile(testProfile.getKID(), env.getType(KeyPrefix.get(KID.NOTIFICATION_PREFIX)).getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		permissionService.setTypePermissionForProfile(testProfile.getKID(), env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		
		Record user1 = dataService.save(dataHelper.getTestUser("user-one", "user-one@kommet.io", testProfile, env), env);
		Record user2 = dataService.save(dataHelper.getTestUser("user-two", "user-two@kommet.io", testProfile, env), env);
		
		User user1Proxy = new User();
		user1Proxy.setId(user1.getKID());
		
		User user2Proxy = new User();
		user2Proxy.setId(user2.getKID());
		
		AuthData authData1 = dataHelper.getAuthData(userService.getUser(user1.getKID(), env), env);
		AuthData authData2 = dataHelper.getAuthData(userService.getUser(user2.getKID(), env), env);
		
		Notification n1 = new Notification();
		n1.setAssignee(user1Proxy);
		n1.setTitle("First notification");
		n1.setText("Content 1");
		n1 = notificationService.save(n1, dataHelper.getRootAuthData(env), env);
		assertNotNull(n1.getId());
		assertNull("Notification should not be set as viewed by default", n1.getViewedDate());
		
		// make sure the notification is shared with its assignee
		assertNotNull("Notification should by default be shared with its assignee", notificationService.get(n1.getId(), authData1, env));
		assertNull("Notification should not by default be shared with other users", notificationService.get(n1.getId(), authData2, env));
		
		try
		{
			notificationService.setViewedDate(n1.getId(), new Date(), authData2, env);
			fail("It should not be possible to set notification as viewed by user who does not have access to it");
		}
		catch (NotificationException e)
		{
			assertTrue(e.getMessage().contains("No notification with ID "));
		}
		
		notificationService.setViewedDate(n1.getId(), new Date(), authData1, env);
		n1 = notificationService.get(n1.getId(), dataHelper.getRootAuthData(env), env);
		assertNotNull(n1.getViewedDate());
		
		// create another notification for second user
		Notification n2 = new Notification();
		n2.setAssignee(user2Proxy);
		n2.setTitle("Second notification");
		n2.setText("Content 2");
		n2 = notificationService.save(n2, dataHelper.getRootAuthData(env), env);
		
		assertNotNull("Notification should by default be shared with its assignee", notificationService.get(n2.getId(), authData2, env));
		assertNull("Notification should not by default be shared with other users", notificationService.get(n2.getId(), authData1, env));
		
		// delete notification
		notificationService.delete(n1, authData1, env);
		assertNull("Notification not deleted", notificationService.get(n1.getId(), dataHelper.getRootAuthData(env), env));
		
		// test querying notification - such simple query (single field, no IDs in select clause) used to return errors, this is why we are checking this
		env.getSelectCriteriaFromDAL("select assignee.userName from " + env.getType(KeyPrefix.get(KID.NOTIFICATION_PREFIX)).getQualifiedName()).list();
	}
}
