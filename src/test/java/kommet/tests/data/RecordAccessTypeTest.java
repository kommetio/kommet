/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.RecordAccessType;
import kommet.basic.User;
import kommet.dao.dal.CannotModifyAccessTypeException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class RecordAccessTypeTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	UserService userService;
	
	@Inject
	DataService dataService;
	
	@Test
	public void testChangeRecordAccessLevel() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type type = dataHelper.getFullPigeonType(env);
		type = dataService.createType(type, env);
		
		Record youngPigeon = dataService.instantiate(type.getKID(), env);
		youngPigeon.setField("name", "Zenek");
		youngPigeon.setField("age", 2);
		youngPigeon = dataService.save(youngPigeon, env);
		
		youngPigeon = env.getSelectCriteriaFromDAL("select id, " + Field.ACCESS_TYPE_FIELD_NAME + ", name from " + type.getQualifiedName() + " where id ='" + youngPigeon.getKID() + "'").singleRecord();
		
		assertEquals((Integer)RecordAccessType.PUBLIC.getId(), youngPigeon.getAccessType());
		
		// make sure access type cannot be modified
		youngPigeon.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		try
		{
			dataService.save(youngPigeon, env);
			fail("Changing record access type should fail");
		}
		catch (CannotModifyAccessTypeException e)
		{
			assertEquals(CannotModifyAccessTypeException.CANNOT_MODIFY_ACCESS_TYPE_MSG, e.getMessage());
		}
		
		// the only situation in which we can modify a system immutable record is when updating root password
		testUpdateRootPwd(env);
	}

	private void testUpdateRootPwd(EnvData env) throws KommetException
	{
		String newPwd = "mockpwd";
		try
		{
			userService.updateRootPassword(MiscUtils.getSHA1Password(newPwd), env);
		}
		catch (Exception e)
		{
			fail("Updating root password did not success, error: " + e.getMessage());
		}
		
		User u = userService.authenticate(BasicSetupService.ROOT_USERNAME, newPwd, env);
		assertNotNull("Root not authenticated with new password", u);
	}
}
