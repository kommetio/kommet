/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.Button;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.filters.ButtonFilter;
import kommet.services.ButtonService;
import kommet.utils.AppConfig;

public class ButtonTest extends BaseUnitTest
{
	@Inject
	AppConfig config;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	ButtonService buttonService;
	
	@Test
	public void testButtons() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Button button1 = new Button();
		button1.setName("com.rm.Btn");
		button1.setLabel("Click it");
		
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		button1.setTypeId(userType.getKID());
		
		try
		{
			buttonService.save(button1, authData, env);
			fail("Saving button without URL, action or onClick event should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue(e.getMessage().startsWith("Button URL, action or onClick event must be defined"));
		}
		
		button1.setOnClick("jsCallback()");
		
		assertTrue(env.getTypeCustomButtons(userType.getKID()).isEmpty());
		
		button1 = buttonService.save(button1, authData, env);
		
		assertEquals(1, env.getTypeCustomButtons(userType.getKID()).size());
		
		assertNotNull(button1.getId());
		
		ButtonFilter filter = new ButtonFilter();
		filter.setTypeId(userType.getKID());
		List<Button> buttons = buttonService.get(filter, authData, env);
		assertEquals(1, buttons.size());
		
		buttonService.delete(button1.getId(), authData, env);
		buttons = buttonService.get(filter, authData, env);
		assertEquals(0, buttons.size());
		
		assertTrue(env.getTypeCustomButtons(userType.getKID()).isEmpty());
	}
}
