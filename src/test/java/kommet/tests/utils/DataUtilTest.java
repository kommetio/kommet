/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.NoSuchFieldException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.DataUtil;

public class DataUtilTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	UserService userService;
	
	@Test
	public void testIsCollectionCheck() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		assertFalse(DataUtil.isCollection("id", pigeonType));
		assertFalse(DataUtil.isCollection("father", pigeonType));
		assertFalse(DataUtil.isCollection("father.id", pigeonType));
		
		Field childrenField = new Field();
		childrenField.setApiName("children");
		childrenField.setDataType(new InverseCollectionDataType(pigeonType, "father"));
		childrenField.setLabel("Children");
		pigeonType.addField(childrenField);
		dataService.createField(childrenField, env);
		
		assertTrue(DataUtil.isCollection("children", pigeonType));
		assertTrue(DataUtil.isCollection("children.age", pigeonType));
		assertTrue(DataUtil.isCollection("father.children", pigeonType));
		assertTrue(DataUtil.isCollection("father.children.id", pigeonType));
		
		try
		{
			DataUtil.isCollection("father.childrens.id", pigeonType);
			fail("Checking collection for a non-existing field should fail");
		}
		catch (NoSuchFieldException e)
		{
			// expected
		}
		
		testCanEditFieldPermissions(env);
	}
	
	private void testCanEditFieldPermissions(EnvData env) throws KeyPrefixException, KommetException
	{
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		AuthData authData = dataHelper.getRootAuthData(env);
		assertTrue(DataUtil.canEditFieldPermissions(userType.getField("userName"), authData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.ID_FIELD_NAME), authData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.CREATEDBY_FIELD_NAME), authData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.CREATEDDATE_FIELD_NAME), authData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.LAST_MODIFIED_BY_FIELD_NAME), authData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.LAST_MODIFIED_DATE_FIELD_NAME), authData));
		
		// create another user
		Record profile = dataService.save(dataHelper.getTestProfile("TestProfile", env), env);
		Record testUser = dataService.save(dataHelper.getTestUser("test@kommet.io", "test@kommet.io", profile, env), env);
		assertNotNull(testUser.getKID());
		
		AuthData testUserAuthData = dataHelper.getAuthData(userService.getUser(testUser.getKID(), env), env);
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField("userName"), testUserAuthData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.ID_FIELD_NAME), testUserAuthData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.CREATEDBY_FIELD_NAME), testUserAuthData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.CREATEDDATE_FIELD_NAME), testUserAuthData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.LAST_MODIFIED_BY_FIELD_NAME), testUserAuthData));
		assertFalse(DataUtil.canEditFieldPermissions(userType.getField(Field.LAST_MODIFIED_DATE_FIELD_NAME), testUserAuthData));
	}
}
