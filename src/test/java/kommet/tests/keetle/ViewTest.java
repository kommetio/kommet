/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.keetle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.BasicSetupService;
import kommet.basic.RecordProxyException;
import kommet.basic.RecordProxyUtil;
import kommet.basic.View;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.ViewSyntaxException;
import kommet.basic.keetle.ViewUtil;
import kommet.basic.types.SystemTypes;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class ViewTest extends BaseUnitTest
{
	@Inject
	ViewService viewService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	AppConfig config;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Test
	public void testViews() throws KommetException, InterruptedException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		testStoreViewOnDisk(env);
		testFullStoreView(env);
		testKTLParse(env);
		testValidateViewPackage(env);
	}
	
	private void testValidateViewPackage(EnvData env) throws KommetException
	{
		View testView = new View();
		String viewCode = ViewUtil.getEmptyViewCode("SampleView1", "any.package");
		testView.setIsSystem(false);
		testView.setPackageName("com.diff.package");
		
		// save view
		try
		{
			testView = viewService.fullSave(testView, viewCode, false, dataHelper.getRootAuthData(env), env);
			fail("Saving view with different package in code and on object should fail");
		}
		catch (KommetException e)
		{
			assertEquals("Package name in code is different than the one specified on the view object", e.getMessage());
		}
		
		// with the proper flag the update should succeed
		viewService.fullSave(testView, viewCode, true, dataHelper.getRootAuthData(env), env);
	}

	private void testKTLParse(EnvData env) throws KommetException
	{	
		View testView = new View();
		String viewCode = ViewUtil.getEmptyViewCode("SampleView", "any.package");
		testView.setIsSystem(false);
		
		// save view
		testView = viewService.fullSave(testView, viewCode, true, dataHelper.getRootAuthData(env), env);
		
		// add some javascript using double ampersands
		String code = "<script>\nvar isReady = var1 && (var2 || var3); for (var i = 0; i < 10; i++)</script>";
		//String code = "<script>\nvar isReady = var1 && (var2 || var3);</script>";
		viewCode = viewCode.replaceAll(ViewUtil.EMPTY_VIEW_CONTENT, code);
		testView = viewService.fullSave(testView, viewCode, true, dataHelper.getRootAuthData(env), env);
		assertTrue("Invalid KTL code:\n" + testView.getKeetleCode(), testView.getKeetleCode().contains(code));
		assertTrue(testView.getJspCode().contains("<script>//<![CDATA[\nvar isReady = var1 && (var2 || var3); for (var i = 0; i < 10; i++)//]]></script>"));
		
		code = "<script src=\"aaa\">//<![CDATA[\nvar isReady = var1 && (var2 || var3); for (var i = 0; i < 10; i++)//]]></script>";
		viewCode = ViewUtil.getEmptyViewCode("SampleView", "any.package");
		viewCode = viewCode.replaceAll(ViewUtil.EMPTY_VIEW_CONTENT, code);
		testView = viewService.fullSave(testView, viewCode, true, dataHelper.getRootAuthData(env), env);
		assertTrue("Invalid KTL code:\n" + testView.getKeetleCode(), testView.getKeetleCode().contains(code));
		assertTrue("Invalid jsp code:\n" + testView.getJspCode(), testView.getJspCode().contains("<script src=\"aaa\">//<![CDATA[\nvar isReady = var1 && (var2 || var3); for (var i = 0; i < 10; i++)//]]></script>"));
		
		code = "<script>\n//<![CDATA[\nvar isReady = var1 && (var2 || var3); for (var i = 0; i < 10; i++)\n//]]></script>";
		//String code = "<script>\nvar isReady = var1 && (var2 || var3);</script>";
		viewCode = ViewUtil.getEmptyViewCode("SampleView", "any.package");
		viewCode = viewCode.replaceAll(ViewUtil.EMPTY_VIEW_CONTENT, code);
		testView = viewService.fullSave(testView, viewCode, true, dataHelper.getRootAuthData(env), env);
		assertTrue("Invalid KTL code:\n" + testView.getKeetleCode(), testView.getKeetleCode().contains(code));
		assertTrue("Invalid jsp code:\n" + testView.getJspCode(), testView.getJspCode().contains("<script>//<![CDATA[\nvar isReady = var1 && (var2 || var3); for (var i = 0; i < 10; i++)\n//]]></script>"));
	}

	private void testFullStoreView(EnvData env) throws KommetException, InterruptedException
	{
		String ktlDirPath = env.getKeetleDir(config.getKeetleDir());
		
		// there may be files in the keetle directory from previous tests
		// so we clear it
		File ktlDir = new File(ktlDirPath);
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		
		// create the dir anew - the page create methods won't do this
		ktlDir.mkdir();
		
		View testView = new View();
		
		String ktlCode = ViewUtil.getEmptyViewCode("SampleView", "any.package");
		testView.setIsSystem(false);
		assertNull(testView.getName());
		assertNull(testView.getPackageName());
		
		// save view
		testView = viewService.fullSave(testView, ktlCode, true, dataHelper.getRootAuthData(env), env);
		
		assertEquals("SampleView", testView.getName());
		assertEquals("any.package", testView.getPackageName());
		
		String viewPath = ktlDirPath + "/" + testView.getId() + ".jsp";
		
		long testStartTime = new Date().getTime();
		
		// need to wait one second because unix systems round up the file.lastModified property to the closest full seconds
		Thread.sleep(1000);
		
		viewService.storeView(testView, env);
		
		// search view by name and package
		ViewFilter filter = new ViewFilter();
		filter.setName(testView.getName());
		filter.setPackage(testView.getPackageName());
		List<View> foundViews = viewService.getViews(filter, env);
		assertEquals(1, foundViews.size());
		View foundView = foundViews.get(0);
		assertEquals(testView.getId(), foundView.getId());
		
		// try searching view by qualified name
		assertNotNull(viewService.getView(testView.getQualifiedName(), env));
		
		// try searching by the same name by different package and make sure any such view is found
		filter.setPackage("another.pack");
		assertTrue("Finding view by package that does not exist should return no results", viewService.getViews(filter, env).isEmpty());
		
		// need to wait one second because unix systems round up the file.lastModified property to the closest full seconds
		Thread.sleep(1000);
		
		// make sure the view file has been persisted on disk
		File viewFile = new File(viewPath);
		assertTrue("JSP file for view not stored in the KTL directory", viewFile.exists());
		
		long viewFileDate = viewFile.lastModified();
		
		assertTrue("View file '" + viewPath + "' should have been updated on disk. The file path is: " + viewFile.getAbsolutePath(), viewFileDate > testStartTime);
		
		// sometimes the saving happens too fast and is not reflected in a change in the file's last modified date
		// so we need to hold off the thread for a while
		Thread.sleep(1000);
		
		// update the file on disk
		viewService.storeView(testView, env);
		
		// make sure the file's date has changed
		viewFile = new File(viewPath);
		long fileDateAfterSave = viewFile.lastModified();
		assertTrue("View file's (" + viewPath + ") last modified date has not changed after view has been updated. Probably the file has not been saved properly on disk.", viewFileDate != fileDateAfterSave);
		
		// update the file on disk changing its name
		testView.setName("newName");
		viewService.storeView(testView, env);
		
		// make sure the file's date has changed
		viewFile = new File(viewPath);
		long fileDateAfterNameChange = viewFile.lastModified();
		assertTrue("View file's last modified date has not changed after view has been updated with new name. Probably the file has not been saved properly on disk.", viewFileDate != fileDateAfterNameChange);
		
		testViewValidationWhileSaving(env);
		
		// now try to delete the view
		viewService.deleteView(testView, env);
		
		// make sure the view has been deleted from DB
		Long viewCount = env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.VIEW_API_NAME + " where " + Field.ID_FIELD_NAME + " = '" + testView.getId() + "'").count();
		assertEquals(Long.valueOf(0), viewCount);
		
		// make sure the view has been deleted from disk
		File deletedViewFile = new File(viewPath);
		assertFalse("Keetle view not deleted from disk storage", deletedViewFile.exists());
	}

	private void assertNotNull(View view)
	{
		// TODO Auto-generated method stub
		
	}

	private void testStoreViewOnDisk(EnvData env) throws KommetException, InterruptedException
	{	
		String ktlDirPath = env.getKeetleDir(config.getKeetleDir());
		
		// there may be files in the keetle directory from previous tests
		// so we clear it
		File ktlDir = new File(ktlDirPath);
		if (ktlDir.isDirectory() && ktlDir.exists())
		{
			ktlDir.delete();
		}
		
		// create the dir anew - the page create methods won't do this
		ktlDir.mkdir();
		
		View pigeonListView = new View(dataHelper.getPigeonListView(env), env);
		viewService.save(pigeonListView, config, dataHelper.getRootAuthData(env), env);
		String viewPath = ktlDirPath + "/" + pigeonListView.getId() + ".jsp";
		
		// make sure the file is not on disk
		//File viewFile = new File(viewPath);
		//assertTrue("View file should have been created together with the object", viewFile.exists());
		
		long testStartTime = System.currentTimeMillis();
		
		// need to wait one second because unix systems round up the file.lastModified property to the closest full seconds
		Thread.sleep(1000);
		
		viewService.storeView(pigeonListView, env);
		
		// search view by name and package
		ViewFilter filter = new ViewFilter();
		filter.setName(pigeonListView.getName());
		filter.setPackage(pigeonListView.getPackageName());
		List<View> foundViews = viewService.getViews(filter, env);
		assertEquals(1, foundViews.size());
		View foundView = foundViews.get(0);
		assertEquals(pigeonListView.getId(), foundView.getId());
		
		// try searching by the same name by different package and make sure any such view is found
		filter.setPackage("another.pack");
		assertTrue("Finding view by package that does not exist should return no results", viewService.getViews(filter, env).isEmpty());
		
		// need to wait one second because unix systems round up the file.lastModified property to the closest full seconds
		Thread.sleep(1000);
		
		// make sure the view file has been persisted on disk
		File viewFile = new File(viewPath);
		assertTrue("JSP file for view not stored in the KTL directory", viewFile.exists());
		
		long viewFileDate = viewFile.lastModified();
		
		assertTrue("View file '" + viewPath + "' should have been updated on disk. Test start time is " + testStartTime + ", file modification time " + viewFileDate, viewFileDate > testStartTime);
		
		// need to wait one second because unix systems round up the file.lastModified property to the closest full seconds
		Thread.sleep(1000);
		
		// update the file on disk
		viewService.storeView(pigeonListView, env);
		
		// make sure the file's date has changed
		viewFile = new File(viewPath);
		long fileDateAfterSave = viewFile.lastModified();
		assertTrue("View file's (" + viewPath + ") last modified date has not changed after view has been updated. Probably the file has not been saved properly on disk.", viewFileDate != fileDateAfterSave);
		
		// update the file on disk changing its name
		pigeonListView.setName("newName");
		viewService.storeView(pigeonListView, env);
		
		// make sure the file's date has changed
		viewFile = new File(viewPath);
		long fileDateAfterNameChange = viewFile.lastModified();
		assertTrue("View file's last modified date has not changed after view has been updated with new name. Probably the file has not been saved properly on disk.", viewFileDate != fileDateAfterNameChange);
		
		testViewValidationWhileSaving(env);
		
		// now try to delete the view
		viewService.deleteView(pigeonListView, env);
		
		// make sure the view has been deleted from DB
		Long viewCount = env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.VIEW_API_NAME + " where " + Field.ID_FIELD_NAME + " = '" + pigeonListView.getId() + "'").count();
		assertEquals(Long.valueOf(0), viewCount);
		
		// make sure the view has been deleted from disk
		File deletedViewFile = new File(viewPath);
		assertFalse("Keetle view not deleted from disk storage", deletedViewFile.exists());
	}
	
	@Test
	public void testGetViewNameAndPackageFromKeetle() throws KommetException
	{
		String code = ViewUtil.wrapKeetle("<km:list></km:list>");
		
		try
		{
			ViewUtil.getViewPropertiesFromCode(code);
			fail("Analyzing keetle code not starting with km:view should fail");
		}
		catch (ViewSyntaxException e)
		{
			// expected
		}
		
		code = ViewUtil.wrapKeetle("<km:view><km:innerTag /></km:view>");
		try
		{
			ViewUtil.getViewPropertiesFromCode(code);
			fail("Analyzing view tag with no name defined should fail");
		}
		catch (ViewSyntaxException e)
		{
			// expected
		}
		
		code = ViewUtil.wrapKeetle("<km:view name=\"test\"><km:innerTag /></km:vie");
		try
		{
			ViewUtil.getViewPropertiesFromCode(code);
			fail("Analyzing keetle code with syntax errors should fail");
		}
		catch (ViewSyntaxException e)
		{
			// expected
		}
		
		code = ViewUtil.wrapKeetle("<km:view name=\"test\"><km:innerTag /></km:view>");
		String[] name = ViewUtil.getViewPropertiesFromCode(code);
		assertEquals("test", name[0]);
		assertTrue("Even if view package is null, the length of the returned array should always be 4", name.length == 4);
		assertNull(name[1]);
		
		code = ViewUtil.wrapKeetle("<km:view name=\"test\" package=\"some.pack.test\"><km:innerTag /></km:view>");
		name = ViewUtil.getViewPropertiesFromCode(code);
		assertEquals("test", name[0]);
		assertEquals("some.pack.test", name[1]);
		
		code = ViewUtil.wrapKeetle("<km:view name=\"test.test1\" package=\"some.pack.test\"><km:innerTag /></km:view>");
		name = ViewUtil.getViewPropertiesFromCode(code);
		// even though view names with dots are invalid, this method does not check for this
		assertEquals("test.test1", name[0]);
		assertEquals("some.pack.test", name[1]);
		
		code = ViewUtil.wrapKeetle("<km:view name=\"test.test1\" package=\"some.pack.test\" layout=\"SomeLayout\"><km:innerTag /></km:view>");
		name = ViewUtil.getViewPropertiesFromCode(code);
		// even though view names with dots are invalid, this method does not check for this
		assertEquals("test.test1", name[0]);
		assertEquals("some.pack.test", name[1]);
		assertEquals("SomeLayout", name[2]);
		
		code = ViewUtil.wrapKeetle("<km:view name=\"test.test1\" package=\"some.pack.test\" layout=\"SomeLayout\" object=\"kommet.data.User\"><km:innerTag /></km:view>");
		name = ViewUtil.getViewPropertiesFromCode(code);
		// even though view names with dots are invalid, this method does not check for this
		assertEquals("test.test1", name[0]);
		assertEquals("some.pack.test", name[1]);
		assertEquals("SomeLayout", name[2]);
		assertEquals("kommet.data.User", name[3]);
	}
	
	private void testViewValidationWhileSaving(EnvData env) throws RecordProxyException, KommetException
	{
		View view = (View)RecordProxyUtil.generateStandardTypeProxy(View.class, dataHelper.getPigeonListView(env), true, env);
		view.setName("Name withSpace");
		
		try
		{
			viewService.save(view, config, dataHelper.getRootAuthData(env), env);
			fail("Saving view with space in name should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue(e.getMessage().startsWith("View name is not valid"));
		}
	}
}
