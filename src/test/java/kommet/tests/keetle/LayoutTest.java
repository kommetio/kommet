/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.keetle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.Layout;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewUtil;
import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.services.SystemSettingService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class LayoutTest extends BaseUnitTest
{
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	EnvService envService;
	
	@Inject
	SystemSettingService settingService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	AppConfig config;
	
	@Test
	public void testGetContent() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Layout layout = new Layout();
		String code = "<km:layout name=\"Test\"><km:beforeContent><a href=\"aa\">some link</a></km:beforeContent></km:layout>";
		layout.setCode(code);
		layout.setName("TestLayout");
		
		try
		{
			layout = layoutService.save(layout, dataHelper.getRootAuthData(env), env);
			fail("Saving layout with not matching name in code should fail");
		}
		catch (FieldValidationException e)
		{
			assertEquals("Layout name and package in code is different than layout name on the layout object", e.getMessage());
		}
		
		// assign corrected code
		layout.setCode("<km:layout name=\"TestLayout\"><km:beforeContent><a href=\"aa\">some link</a></km:beforeContent></km:layout>");
		layout = layoutService.save(layout, dataHelper.getRootAuthData(env), env);
		
		assertEquals("<a href=\"aa\">some link</a>", layout.getBeforeContent());
		assertNull(layout.getAfterContent());
		
		// make sure files for before and after layout parts have been created on disk
		File beforeContentFile = new File(config.getLayoutDir() + "/" + env.getId() + "/" + layout.getId() + "_before.jsp");
		assertTrue(beforeContentFile.exists() && beforeContentFile.isFile());
		
		File afterContentFile = new File(config.getLayoutDir() + "/" + env.getId() + "/" + layout.getId() + "_after.jsp");
		assertTrue(afterContentFile.exists() && beforeContentFile.isFile());
		
		layoutService.delete(layout.getId(), dataHelper.getRootAuthData(env), env);
		assertNull(layoutService.getById(layout.getId(), env));
	}
	
	@Test
	public void testParseLayout() throws KommetException
	{
		String code = ViewUtil.wrapLayout("<km:layout name=\"Test\"><km:beforeContent><a href=\"aa\">some link</a></km:beforeContent><km:afterContent><h3>after elem</km:afterContent></km:layout>");
		String[] contentParts = LayoutService.getPreAndPostContent(code);
		assertEquals("<a href=\"aa\">some link</a>", contentParts[0]);
		assertEquals("<h3>after elem", contentParts[1]);
	}
	
	@Test
	public void testDefaultLayout() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		String code1 = "<km:layout name=\"Test1\"><km:beforeContent></km:beforeContent></km:layout>";
		
		Layout layout = new Layout();
		layout.setName("Test1");
		layout.setCode(code1);
		layout = layoutService.save(layout, dataHelper.getRootAuthData(env), env);
		
		String code2 = "<km:layout name=\"Test2\"><km:beforeContent></km:beforeContent></km:layout>";
		
		Layout layout2 = new Layout();
		layout2.setName("Test2");
		layout2.setCode(code2);
		layout2 = layoutService.save(layout2, dataHelper.getRootAuthData(env), env);
		
		AuthData rootAuthData = dataHelper.getRootAuthData(env);
		
		layoutService.setDefaultLayout(layout, dataHelper.getRootAuthData(env), env);
		assertEquals("Test1", layoutService.getDefaultLayoutName(rootAuthData, env));
		layoutService.setDefaultLayout(layout2, dataHelper.getRootAuthData(env), env);
		assertEquals("Test2", layoutService.getDefaultLayoutName(rootAuthData, env));
		layoutService.setDefaultLayout(null, rootAuthData, env);
		assertNull(layoutService.getDefaultLayoutId(rootAuthData, env));
		assertNull("Expected default layout to be null, but was " + layoutService.getDefaultLayoutName(rootAuthData, env), layoutService.getDefaultLayoutName(rootAuthData, env));
	}
}
