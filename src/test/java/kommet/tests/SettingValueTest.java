/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.SettingValue;
import kommet.dao.SettingValueFilter;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.services.SettingValueService;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.uch.UserCascadeHierarchyFilter;
import kommet.uch.UserCascadeHierarchyService;

public class SettingValueTest extends BaseUnitTest
{
	@Inject
	SettingValueService settingValueService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Test
	public void testCRUD() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		Integer initialCount = settingValueService.get(new SettingValueFilter(), dataHelper.getRootAuthData(env), env).size();
		Integer initialUchCount = uchService.get(new UserCascadeHierarchyFilter(), dataHelper.getRootAuthData(env), env).size();
		
		SettingValue val = new SettingValue();
		val.setKey("KEY 1");
		
		val = uchService.saveSetting(val, UserCascadeHierarchyContext.ENVIRONMENT, true, dataHelper.getRootAuthData(env), env);
		
		assertEquals(initialCount + 1, settingValueService.get(new SettingValueFilter(), dataHelper.getRootAuthData(env), env).size());
		assertEquals(initialUchCount + 1, uchService.get(new UserCascadeHierarchyFilter(), dataHelper.getRootAuthData(env), env).size());
		
		// find setting by key
		SettingValueFilter filter = new SettingValueFilter();
		filter.addKey("KEY 1");
		List<SettingValue> values = settingValueService.get(filter, dataHelper.getRootAuthData(env), env);
		assertEquals(1, values.size());
		SettingValue retrievedValue = values.get(0);
		assertNotNull(retrievedValue.getHierarchy());
		assertNotNull(retrievedValue.getHierarchy().getActiveContext());
		assertNotNull(retrievedValue.getHierarchy().getEnv());
		assertEquals(true, retrievedValue.getHierarchy().getActiveContextValue());
		
		// retrieve the value using different method
		SettingValue alternativeValue = settingValueService.get("KEY 1", true, dataHelper.getRootAuthData(env), env);
		assertNotNull(alternativeValue);
		assertEquals(retrievedValue.getId(), alternativeValue.getId());
		
		// delete setting
		settingValueService.delete(values.get(0).getId(), dataHelper.getRootAuthData(env), env);
		
		assertEquals((Integer)(initialUchCount + 1), (Integer)uchService.get(new UserCascadeHierarchyFilter(), dataHelper.getRootAuthData(env), env).size());
		assertEquals((Integer)initialCount, (Integer)settingValueService.get(new SettingValueFilter(), dataHelper.getRootAuthData(env), env).size());
		
		SettingValue val2 = new SettingValue();
		val2.setKey("KEY 2");
		
		val2 = uchService.saveSetting(val2, UserCascadeHierarchyContext.ENVIRONMENT, true, dataHelper.getRootAuthData(env), env);
		assertEquals((Integer)(initialUchCount + 2), (Integer)uchService.get(new UserCascadeHierarchyFilter(), dataHelper.getRootAuthData(env), env).size());
		assertEquals((Integer)(initialCount + 1), (Integer)settingValueService.get(new SettingValueFilter(), dataHelper.getRootAuthData(env), env).size());
		
		uchService.deleteSetting(val2, dataHelper.getRootAuthData(env), env);
		assertEquals((Integer)(initialUchCount + 1), (Integer)uchService.get(new UserCascadeHierarchyFilter(), dataHelper.getRootAuthData(env), env).size());
		assertEquals((Integer)initialCount, (Integer)settingValueService.get(new SettingValueFilter(), dataHelper.getRootAuthData(env), env).size());
	}
}
