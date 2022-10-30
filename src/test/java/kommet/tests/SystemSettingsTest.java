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

import kommet.basic.BasicSetupService;
import kommet.basic.SystemSetting;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.QueryResult;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.services.SystemSettingService;

public class SystemSettingsTest extends BaseUnitTest
{
	@Inject
	SystemSettingService settingService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Test
	public void testSystemSettingsCRUD() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Integer initialSettingCount = settingService.getSettings(env).size();
		
		SystemSetting setting = new SystemSetting();
		setting.setKey("test");
		setting.setValue("value");
		setting = settingService.save(setting, dataHelper.getRootAuthData(env), env);
		assertNotNull(setting.getId());
		
		List<Record> records = env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.SYSTEM_SETTING_API_NAME).list();
		assertEquals(initialSettingCount + Long.valueOf(1), ((QueryResult)records.get(0)).getAggregateValue("count(id)"));
		
		// set another setting using setSettingMethod
		settingService.setSetting("she", "Kamila", dataHelper.getRootAuthData(env), env);
		
		assertEquals(Long.valueOf(initialSettingCount + 2), env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.SYSTEM_SETTING_API_NAME).count());
		
		List<SystemSetting> sheSettings = settingService.getSettings("she", env);
		assertNotNull(sheSettings);
		assertEquals(1, sheSettings.size());
		assertEquals("Kamila", sheSettings.get(0).getValue());
		
		// update existing setting
		settingService.setSetting("test", "value2", dataHelper.getRootAuthData(env), env);
		assertEquals(Long.valueOf(initialSettingCount + 2), env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.SYSTEM_SETTING_API_NAME).count());	
	}
}
