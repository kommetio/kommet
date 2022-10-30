/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;

import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.services.GlobalSettingsService;

public class GlobalSettingsTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	GlobalSettingsService globalSettings;
	
	@Test
	public void testGlobalSettings() throws KommetException
	{
		EnvData testEnv = dataHelper.getTestEnvData(false);
		assertNull(globalSettings.getSetting("setting.one", testEnv));
		
		globalSettings.setSetting("setting.one", "val.one", testEnv);
		globalSettings.setSetting("setting.two", "val.two", testEnv);
		
		assertEquals("val.one", globalSettings.getSetting("setting.one", testEnv));
	}
}
