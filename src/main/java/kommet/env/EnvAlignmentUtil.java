/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.env;

import org.slf4j.Logger;

import kommet.auth.AuthData;
import kommet.basic.types.ProfileKType;
import kommet.basic.types.UserGroupAssignmentKType;
import kommet.basic.types.UserGroupKType;
import kommet.basic.types.UserKType;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.TextDataType;

public class EnvAlignmentUtil
{
	public static void alignRememberMe (DataService dataService, Logger log, EnvData env) throws KommetException
	{
		Integer rmFieldCount = env.getJdbcTemplate().queryForObject("select count(id) from fields where apiname = 'rememberMeToken' and typeid in (select id from types where apiname = 'User' and package = 'kommet.basic')", Integer.class);
		
		if (rmFieldCount == 0)
		{
			log.debug("Adding field User.rememberMeToken");
			
			// add remember me field to the User type
			Field rmField = new Field();
			rmField.setApiName("rememberMeToken");
			rmField.setLabel("Remember Me Token");
			rmField.setDataType(new TextDataType(1000));
			rmField.setDbColumn("remembermetoken");
			rmField.setRequired(false);
			
			UserKType userType = new UserKType(new ProfileKType());
			userType.setDbTable("obj_" + KID.USER_PREFIX);
			
			Long userTypeId = env.getJdbcTemplate().queryForObject("select id from types where apiname = 'User' and package = 'kommet.basic'", Long.class);
			userType.setId(userTypeId);
			rmField.setType(userType);
			
			dataService.createField(rmField, AuthData.getRootAuthData(env), true, false, false, true, env);
		}
		else
		{
			log.debug("Field User.rememberMeToken alread exists");
		}
	}

	public static void alignUgaIsPendingApply(DataService dataService, Logger log, EnvData env) throws KommetException
	{
		Integer fieldCount = env.getJdbcTemplate().queryForObject("select count(id) from fields where apiname = 'isApplyPending' and typeid in (select id from types where apiname = 'UserGroupAssignment' and package = 'kommet.basic')", Integer.class);
		
		if (fieldCount == 0)
		{
			log.debug("Adding field UserGroupAssignment.isApplyPending");
			
			// add field
			Field rmField = new Field();
			rmField.setApiName("isApplyPending");
			rmField.setLabel("Is Apply Pending");
			rmField.setDataType(new BooleanDataType());
			rmField.setDbColumn("isapplypending");
			rmField.setRequired(false);
			
			UserGroupAssignmentKType type = new UserGroupAssignmentKType(new UserKType(), new UserGroupKType());
			type.setDbTable("obj_" + KID.USER_GROUP_ASSIGNMENT_PREFIX);
			
			Long typeId = env.getJdbcTemplate().queryForObject("select id from types where apiname = 'UserGroupAssignment' and package = 'kommet.basic'", Long.class);
			type.setId(typeId);
			rmField.setType(type);
			
			dataService.createField(rmField, AuthData.getRootAuthData(env), true, false, false, true, env);
		}
		else
		{
			log.debug("Field User.rememberMeToken alread exists");
		}
	}
	
	public static void alignUgaIsPendingRemove(DataService dataService, Logger log, EnvData env) throws KommetException
	{
		Integer fieldCount = env.getJdbcTemplate().queryForObject("select count(id) from fields where apiname = 'isRemovePending' and typeid in (select id from types where apiname = 'UserGroupAssignment' and package = 'kommet.basic')", Integer.class);
		
		if (fieldCount == 0)
		{
			log.debug("Adding field UserGroupAssignment.isRemovePending");
			
			// add field
			Field rmField = new Field();
			rmField.setApiName("isRemovePending");
			rmField.setLabel("Is Remove Pending");
			rmField.setDataType(new BooleanDataType());
			rmField.setDbColumn("isremovepending");
			rmField.setRequired(false);
			
			UserGroupAssignmentKType type = new UserGroupAssignmentKType(new UserKType(), new UserGroupKType());
			type.setDbTable("obj_" + KID.USER_GROUP_ASSIGNMENT_PREFIX);
			
			Long typeId = env.getJdbcTemplate().queryForObject("select id from types where apiname = 'UserGroupAssignment' and package = 'kommet.basic'", Long.class);
			type.setId(typeId);
			rmField.setType(type);
			
			dataService.createField(rmField, AuthData.getRootAuthData(env), true, false, false, true, env);
		}
		else
		{
			log.debug("Field User.rememberMeToken alread exists");
		}
	}
}