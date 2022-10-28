/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.Profile;
import kommet.basic.RecordAccessType;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.dao.dal.InsufficientPrivilegesException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.services.UserGroupService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class SystemAdministratorTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	AppConfig config;
	
	@Inject
	DataService dataService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UserService userService;
	
	@Inject
	UserGroupService ugService;
	
	@Inject
	ClassService classService;
	
	@Test
	public void testSystemAdministratorPermissions() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		assertNotNull(pigeonType.getKID());
		
		AuthData rootAuthData = dataHelper.getRootAuthData(env);
		
		Profile saProfile = profileService.getProfileByName(Profile.SYSTEM_ADMINISTRATOR_NAME, env);
		User saUser = userService.save(dataHelper.getTestUser("testsa@kommet.io", "testsa@kommet.io", "admin123", saProfile, env), dataHelper.getRootAuthData(env), env);
		AuthData saAuthData = dataHelper.getAuthData(saUser, env);
		
		for (Type type : env.getAllTypes())
		{
			// find all records of this type
			List<Record> records = env.getSelectCriteriaFromDAL("select " + Field.ID_FIELD_NAME + ", " + Field.ACCESS_TYPE_FIELD_NAME + " from " + type.getQualifiedName()).list();
			
			for (Record r : records)
			{	
				RecordAccessType expectedAccessType = null;
				
				if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.USER_RECORD_SHARING_PREFIX)) || r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.SYSTEM_SETTING_PREFIX)))
				{
					expectedAccessType = RecordAccessType.SYSTEM;
				}
				else if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.CLASS_PREFIX)) || r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.VIEW_PREFIX)))
				{
					/*if (r.getKID().equals(classService.getClass(Constants.BUSINESS_ACTIONS_PACKAGE + ".QueryUniqueAction", env).getId()))
					{
						expectedAccessType = RecordAccessType.SYSTEM;
					}
					else
					{*/
					expectedAccessType = RecordAccessType.SYSTEM;
				}
				else if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.TYPE_PERMISSION_PREFIX)))
				{
					Record permission = env.getSelectCriteriaFromDAL("select id, profile.id, typeId from " + r.getType().getQualifiedName() + " where " + Field.ID_FIELD_NAME + " = '" + r.getKID() + "'").singleRecord();
					if (permission.getField("profile.id").equals(saProfile.getId()))
					{
						if (permission.getField("typeId").equals(pigeonType.getKID()))
						{
							expectedAccessType = RecordAccessType.SYSTEM;
						}
						else
						{
							expectedAccessType = RecordAccessType.SYSTEM_IMMUTABLE;
						}
					}
					else
					{
						expectedAccessType = RecordAccessType.PUBLIC;
					}
				}
				else if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.BUSINESS_ACTION_PREFIX)))
				{
					continue;
				}
				else if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.BUSINESS_PROCESS_INPUT_PREFIX)))
				{
					continue;
				}
				else if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.BUSINESS_PROCESS_OUTPUT_PREFIX)))
				{
					continue;
				}
				else if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.SETTING_VALUE_PREFIX)))
				{
					continue;
				}
				else if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.USER_CASCADE_HIERARCHY_PREFIX)))
				{
					continue;
				}
				else if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.LAYOUT_PREFIX)))
				{
					expectedAccessType = RecordAccessType.SYSTEM;
				}
				else if (r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.STANDARD_ACTION_PREFIX)) || r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.ACTION_PREFIX)) || r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.UNIQUE_CHECK_PREFIX)) || r.getType().getKeyPrefix().equals(KeyPrefix.get(KID.TYPE_INFO_PREFIX)))
				{	
					Record fullRecord = env.getSelectCriteriaFromDAL("select typeId from " + r.getType().getQualifiedName() + " where id = '" + r.getKID() + "'").singleRecord();
					if (fullRecord.getField("typeId").equals(pigeonType.getKID()))
					{
						// unique checks, actions and other records  for custom types have access type "system"
						expectedAccessType = RecordAccessType.SYSTEM;
					}
					else
					{
						// unique checks for basic types have access type "system immutable"
						expectedAccessType = RecordAccessType.SYSTEM_IMMUTABLE;
					}
				}
				else if (r.getKID().equals(saUser.getId()))
				{
					expectedAccessType = RecordAccessType.PUBLIC;
				}
				else
				{
					expectedAccessType = RecordAccessType.SYSTEM_IMMUTABLE;
				}
				
				Integer accessType = (Integer)r.getField(Field.ACCESS_TYPE_FIELD_NAME);
				if (accessType == null)
				{
					fail("Field " + Field.ACCESS_TYPE_FIELD_NAME + " not set on record " + r.getKID());
				}
				
				assertEquals("Expected access type = " + expectedAccessType + ", but record " + r.getKID() + " has value " + accessType, (Integer)expectedAccessType.getId(), accessType);
				
				
				if (expectedAccessType != RecordAccessType.SYSTEM_IMMUTABLE)
				{
					// if the access type is not system-immutable, then editing and deletion is not blocked, so we don't check this
					continue;
				}
				
				// no user (even root and system administrator) should be able to edit or delete system records
				try
				{
					dataService.deleteRecord(r, saAuthData, env);
					fail("System administrator should not be able to delete record " + r.getKID() + " because it is a system record");
				}
				catch (InsufficientPrivilegesException e)
				{
					assertEquals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_SYSTEM_IMMUTABLE_RECORD_MSG, e.getMessage());
				}
				
				try
				{
					dataService.deleteRecord(r, rootAuthData, env);
					fail("Root should not be able to delete record " + r.getKID() + " because it is a system record");
				}
				catch (InsufficientPrivilegesException e)
				{
					assertEquals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_SYSTEM_IMMUTABLE_RECORD_MSG, e.getMessage());
				}
				
				try
				{
					dataService.save(r, saAuthData, env);
					fail("System administrator should not be able to edit record " + r.getKID() + " because it is a system record");
				}
				catch (InsufficientPrivilegesException e)
				{
					assertEquals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_SYSTEM_IMMUTABLE_RECORD_MSG, e.getMessage());
				}
				
				try
				{
					dataService.save(r, rootAuthData, env);
					fail("Root should not be able to edit record " + r.getKID() + " because it is a system record");
				}
				catch (InsufficientPrivilegesException e)
				{
					assertEquals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_SYSTEM_IMMUTABLE_RECORD_MSG, e.getMessage());
				}
			}
		}
		
		// create record and save it using root auth data
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Mikey");
		pigeon1.setField("age", 11);
		pigeon1 = dataService.save(pigeon1, rootAuthData, env);
		
		boolean isPigeonTypeFound = false;
		
		for (Type type : env.getAllTypes())
		{
			if (type.getKID().equals(pigeonType.getKID()))
			{
				isPigeonTypeFound = true;
			}
			
			assertTrue("System administrator cannot create records of type " + type.getQualifiedName(), saAuthData.canCreateType(type.getKID(), false, env));
			assertTrue("System administrator cannot edit records of type " + type.getQualifiedName(), saAuthData.canEditType(type.getKID(), false, env));
			assertTrue("System administrator cannot delete records of type " + type.getQualifiedName(), saAuthData.canDeleteType(type.getKID(), false, env));
			assertTrue("System administrator cannot edit records of type " + type.getQualifiedName(), saAuthData.canEditAllType(type.getKID(), false, env));
			assertTrue("System administrator cannot delete records of type " + type.getQualifiedName(), saAuthData.canDeleteAllType(type.getKID(), false, env));
		}
		
		assertTrue(isPigeonTypeFound);
		
		// make sure sysadmin can edit the record
		pigeon1.setField("name", "John");
		dataService.save(pigeon1, saAuthData, env);
		
		// make sure sysadmin can delete the record
		dataService.deleteRecord(pigeon1, saAuthData, env);
		
		// create the record again and delete by id
		pigeon1.setKID(null);
		dataService.save(pigeon1, saAuthData, env);
		dataService.deleteRecord(pigeon1.getKID(), saAuthData, env);
		
		// create a user group that has public access type, and make sure it can be modified and deleted by system administrator
		UserGroup ug = new UserGroup();
		ug.setName("com.group.Group1");
		ug = ugService.save(ug, saAuthData, env);
		ugService.save(ug, saAuthData, env);
		ugService.delete(ug.getId(), saAuthData, env);
		
		// delete the pigeon type
		dataService.deleteType(pigeonType, saAuthData, env);
	}
}
