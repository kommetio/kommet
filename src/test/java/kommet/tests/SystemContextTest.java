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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.basic.BasicSetupService;
import kommet.basic.Comment;
import kommet.basic.RecordProxy;
import kommet.basic.UserGroup;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.SystemContext;
import kommet.koll.SystemContextException;
import kommet.koll.SystemContextFactory;
import kommet.notifications.NotificationFilter;
import kommet.notifications.NotificationService;
import kommet.services.UserGroupService;
import kommet.tests.harness.CompanyAppDataSet;
import kommet.utils.MiscUtils;

public class SystemContextTest extends BaseUnitTest
{
	@Inject
	SystemContextFactory sysContextFactory;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	NotificationService notificationService;
	
	@Inject
	UserGroupService userGroupService;
	
	@Test
	public void testSystemContext() throws KommetException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), dataHelper.getRootAuthData(env), env);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(pigeonType.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		dataService.save(oldPigeon, env);
		
		SystemContext sys = sysContextFactory.get(dataHelper.getRootAuthData(env), env);
		List<? extends RecordProxy> pigeons = sys.query("select id from " + pigeonType.getQualifiedName());
		assertNotNull(pigeons);
		assertEquals(1, pigeons.size());
		assertEquals(MiscUtils.userToEnvPackage(pigeonType.getQualifiedName(), env), pigeons.get(0).getClass().getName());
		
		RecordProxy pigeon = pigeons.get(0);
		assertTrue(pigeon.isSet("id"));
		assertFalse(pigeon.isSet("name"));
		assertFalse(pigeon.isSet("father"));
		
		// set one field on pigeon and save it
		PropertyUtils.setProperty(pigeon, "age", 5);
		sys.save(pigeon);
		
		// test querying unique result
		RecordProxy pigeonProxy = sys.queryUniqueResult("select id from " + pigeonType.getQualifiedName() + " where id = '" + pigeon.getId() + "'", false);
		assertNotNull(pigeonProxy);
		
		// query non-existing pigeon
		pigeonProxy = sys.queryUniqueResult("select id from " + pigeonType.getQualifiedName() + " where name = 'blablasomename'", false);
		assertNull(pigeonProxy);
		
		testQueryUserGroup(sys, env);
		testInjectSystemContext(sys, env);
		testCommentService(sys, oldPigeon, env);
	}
	
	private void testCommentService(SystemContext sys, Record someRecord, EnvData env) throws KommetException
	{
		String text = "This is some comment";
		Comment comment = sys.getCommentService().save(text, someRecord.getKID());
		assertNotNull(comment);
		assertNotNull(comment.getId());
	}

	private void testInjectSystemContext(SystemContext sys, EnvData env) throws SystemContextException, KommetException
	{
		SystemContextAwareClass obj = new SystemContextAwareClass();
		sysContextFactory.injectSystemContext(obj, dataHelper.getRootAuthData(env), env);
		assertNotNull(obj.getSys());
		
		sysContextFactory.injectSystemContext(new String(), dataHelper.getRootAuthData(env), env);
		
		try
		{
			sysContextFactory.injectSystemContext(new SystemContextAwareIncorrectClass(), dataHelper.getRootAuthData(env), env);
			fail("Injecting system context into a method with invalid setter should fail");
		}
		catch (SystemContextException e)
		{
			// expected
			assertTrue("Invalid error message: " + e.getMessage(), e.getMessage().contains("is annotated"));
		}
	}

	private void testQueryUserGroup(SystemContext sys, EnvData env) throws KommetException, NoSuchMethodException, SecurityException
	{
		AuthData authData = dataHelper.getRootAuthData(env);
		
		// create some user group
		UserGroup ug = new UserGroup();
		ug.setName("Test");
		userGroupService.save(ug, authData, env);
		assertNotNull(ug.getId());
		
		userGroupService.assignUserToGroup(env.getRootUser().getKID(), ug.getId(), authData, env);
		
		List<RecordProxy> groups = sys.query("select id, name, users.id, users.userName from UserGroup");
		assertEquals("kommet.envs.env" + env.getId() + ".kommet.basic.UserGroup", groups.get(0).getClass().getName());
		
		Method userGetter = groups.get(0).getClass().getMethod("getUsers");
		assertEquals("java.util.ArrayList", userGetter.getReturnType().getName());
	}

	@Test
	public void testSaveProxy() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		CompanyAppDataSet dataSet = CompanyAppDataSet.getInstance(dataService, env);
		
		// insert some companies
		Record company1 = dataService.save(dataSet.getTestCompany("company-1", null), env);
		Record company2 = dataService.save(dataSet.getTestCompany("company-2", null), env);
		
		// make company field on employee required
		Field field = dataService.getFieldForUpdate(dataSet.getEmployeeType().getField("company").getKID(), env);
		field.setRequired(true);
		dataService.updateField(field, dataHelper.getRootAuthData(env), env);
		
		// insert some employees
		Record employee1 = dataService.save(dataSet.getTestEmployee("first name 1", "last name 1", "middle name 1", company1, null), env);
		dataService.save(dataSet.getTestEmployee("first name 2", "last name 2", "middle name 2", company2, null), env);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		authData.initUserPermissions(env);
		
		SystemContext sys = sysContextFactory.get(authData, env);
		
		// get employee proxy without the company field
		RecordProxy employeeProxy1 = sys.get(employee1.getKID(), "id", "firstName");
		sys.save(employeeProxy1);
		
		testCreateNotification(env, sys);
	}

	private void testCreateNotification(EnvData env, SystemContext sys) throws KommetException
	{
		Integer initialCount = notificationService.get(new NotificationFilter(), dataHelper.getRootAuthData(env), env).size();
		assertEquals((Integer)0, initialCount);
		
		Record profile = dataHelper.getTestProfile("SomeProfile", env);
		profile = dataService.save(profile, env);
		
		Record testUser = dataHelper.getTestUser("test-user", "test-user@kommet.io", profile, env);
		testUser = dataService.save(testUser, env);
		assertNotNull(testUser.getKID());
		
		// create notification
		sys.createNotification("Title", "Some text", testUser.getKID());
		assertEquals(1, notificationService.get(new NotificationFilter(), dataHelper.getRootAuthData(env), env).size());
	}
	
	public class SystemContextAwareClass
	{
		private SystemContext sys;

		public SystemContext getSys()
		{
			return sys;
		}

		@kommet.koll.annotations.InjectSystemContext
		public void setSys(SystemContext sys)
		{
			this.sys = sys;
		}
	}
	
	public class SystemContextAwareIncorrectClass
	{
		private SystemContext sys;

		public SystemContext getSys()
		{
			return sys;
		}

		@kommet.koll.annotations.InjectSystemContext
		public void setSys(SystemContext sys, String stubParam)
		{
			this.sys = sys;
		}
	}
}
