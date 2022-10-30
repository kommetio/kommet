/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.koll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.eclipse.jdt.core.dom.ASTParser;
import org.junit.Test;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.Class;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.ViewService;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.env.GenericAction;
import kommet.koll.ClassService;
import kommet.koll.annotations.Action;
import kommet.koll.annotations.Controller;
import kommet.koll.annotations.Public;
import kommet.koll.annotations.Rest;
import kommet.koll.annotations.View;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

@WebAppConfiguration
public class GenericActionTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	EnvService envService;
	
	@Inject
	ClassService classService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UserService userService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ViewService viewService;
	
	@Test
	public void testAST()
	{
		// the creation of ASTParser used to fail for Java 11, so I placed this check here
		ASTParser.newParser(org.eclipse.jdt.core.dom.AST.JLS11);
	}
	
	@Test
	public void testRestAction() throws Exception
	{
		EnvData env = dataHelper.configureFullTestEnv();
		assertNotNull(env.getGenericActions());
		
		String packageName = "a.b.c";
		
		Class file = new Class();
		file.setName("TestController");
		file.setPackageName(packageName);
		assertTrue(compiler.compile(file, env).isSuccess());
		
		String fullCode = getControllerCode(packageName, false, false, null);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		String javaCode = classService.getKollTranslator(env).kollToJava(fullCode, true, authData, env);
		assertTrue("Invalid Java code: " + javaCode, javaCode.contains("sys.queryUniqueResult(\"select id, userName from User where userName = '\""));
		
		file.setJavaCode(javaCode);
		file.setKollCode(fullCode);
		file.setIsSystem(false);
		
		CompilationResult res = compiler.compile(file, env);
		assertTrue(res.getDescription(), res.isSuccess());
		
		try
		{
			classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
			fail("Saving controller class that does not extend " + BaseController.class.getSimpleName() + " should fail");
		}
		catch (KommetException e)
		{
			// expected exception
			assertTrue(e.getMessage().startsWith("Controller class " + file.getName() + " does not extend " + BaseController.class.getSimpleName()));
		}
		
		// change file definition to extend basic controller
		file.setKollCode(getControllerCode(packageName, false, true, null));
		
		// this compilation should be successful
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertNotNull(file.getId());
		
		assertNotNull(env.getGenericActions());
		assertFalse(env.getGenericActions().isEmpty());
		assertNotNull(env.getGenericActions().get("get/name"));
		GenericAction action = env.getGenericAction("get/name");
		assertNotNull(action);
		assertEquals("getText", action.getActionMethod());
		assertEquals(file.getId(), action.getControllerClassId());
		
		envService.clear(env.getId());
		
		// read in env configuration anew, this time with the generic actions just created
		EnvData refreshedEnv = envService.get(env.getId());
		assertNotNull(refreshedEnv.getGenericActions());
		assertFalse(refreshedEnv.getGenericActions().isEmpty());
		assertNotNull(refreshedEnv.getGenericActions().get("get/name"));
		action = refreshedEnv.getGenericAction("get/name");
		assertNotNull(action);
		assertEquals("getText", action.getActionMethod());
		assertEquals(file.getId(), action.getControllerClassId());
		
		action = refreshedEnv.getGenericAction("action/url");
		assertNotNull(action);
		assertEquals("getActionUrl", action.getActionMethod());
		assertEquals(file.getId(), action.getControllerClassId());
		assertFalse(action.isPublic());
		
		// is this necessary?
		envService.add(refreshedEnv);
		
		// test public generic action
		file.setKollCode(getControllerCode(packageName, true, true, null));
		
		// this compilation should be successful
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertNotNull(env.getGenericActions());
		assertFalse(env.getGenericActions().isEmpty());
		action = env.getGenericAction("action/url");
		assertNotNull(action);
		assertEquals("getActionUrl", action.getActionMethod());
		assertEquals(file.getId(), action.getControllerClassId());
		assertTrue(action.isPublic());
		action = env.getGenericAction("get/name");
		assertNotNull(action);
		assertTrue(action.isPublic());
		
		testViewAnnotation(file, packageName, env);
		
		// add this generic action anew
		file.setKollCode(getControllerCode(packageName, true, true, null));
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		action = refreshedEnv.getGenericAction("action/url");
		assertNotNull(action);
		
		// now delete the class
		classService.delete(file, dataService, authData, refreshedEnv);
		
		// make sure the action has been deleted
		assertNull("Action not deleted together with the class file", refreshedEnv.getGenericAction("action/url"));
	}

	private void testViewAnnotation(Class file, String packageName, EnvData env) throws KommetException
	{
		// create some test view
		kommet.basic.View pigeonListView = new kommet.basic.View(dataHelper.getPigeonListView(env), env);
		viewService.save(pigeonListView, appConfig, dataHelper.getRootAuthData(env), env);
		
		// test public generic action
		file.setKollCode(getControllerCode(packageName, false, true, pigeonListView.getQualifiedName()));
		
		// this compilation should be successful
		file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
		assertNotNull(env.getGenericActions());
		assertFalse(env.getGenericActions().isEmpty());
		GenericAction action = env.getGenericAction("action/url");
		assertNotNull(action);
		assertEquals("getActionUrl", action.getActionMethod());
		assertEquals(file.getId(), action.getControllerClassId());
		assertFalse(action.isPublic());
		action = env.getGenericAction("get/name");
		assertNotNull(action);
		assertFalse(action.isPublic());
		
		// now try saving invalid controller (with non-existing view)
		file.setKollCode(getControllerCode(packageName, false, true, "com.test.InvalidViewName"));
		
		// this compilation should be successful
		try
		{
			file = classService.fullSave(file, dataService, dataHelper.getRootAuthData(env), env);
			fail("Saving @View annotation for a non-existing view should fail");
		}
		catch (KommetException e)
		{
			assertEquals("View com.test.InvalidViewName in @" + View.class.getSimpleName() + " annotation not found", e.getMessage());
		}
	}

	private static String getControllerCode(String packageName, boolean isActionPublic, boolean isExtendsBaseController, String returnedView)
	{
		String imports = "import " + Controller.class.getName() + ";\n";
		imports += "import " + Action.class.getName() + ";\n";
		imports += "import " + Rest.class.getName() + ";\n";
		imports += "import " + Public.class.getName() + ";\n";
		imports += "import " + PageData.class.getName() + ";\n";
		imports += "import " + View.class.getName() + ";\n";
		imports += "import " + BaseController.class.getName() + ";\n";
		
		String code = "package " + packageName + ";\n";
		String classCode = "@Controller\npublic class TestController ";
		
		if (isExtendsBaseController)
		{
			classCode += " extends " + BaseController.class.getSimpleName();
		}
			
		classCode += "{ ";
		classCode += "@Rest(url = \"get/name\")\n";
		
		if (isActionPublic)
		{
			classCode += "@" + Public.class.getSimpleName() + "\n";
		}
		
		classCode += "public String getText() { return \"kamila\"; }";
		
		// add generic action annotated with @Action
		classCode += "\n\n@Action(url = \"action/url\")\n";
		
		if (isActionPublic)
		{
			classCode += "@" + Public.class.getSimpleName() + "\n";
		}
		
		if (StringUtils.hasText(returnedView))
		{
			classCode += "@" + View.class.getSimpleName() + "(name = \"" + returnedView + "\")\n";
		}
		
		if (isExtendsBaseController)
		{
			classCode += "public PageData getActionUrl() { return getPageData(); }";
			
			// add method annotated with action, but not with the URL attribute
			classCode += "\n\n@Action\n";
			classCode += "public PageData someAction() { return getPageData(); }";
		}
		else
		{
			classCode += "public String getActionUrl() { return \"kamila\"; }";
			
			// add method annotated with action, but not with the URL attribute
			classCode += "\n\n@Action\n";
			classCode += "public String someAction() { return \"kamila\"; }";
		}
		
		// add method that retrieves users
		classCode += "@Rest(url = \"getusers\")\n";
		classCode += "public java.util.List<kommet.basic.User> getUsers() throws " + KommetException.class.getName() + " {";
		classCode += "java.util.List users = { select id, userName from User }; return users; }";
		
		// add method that retrieves root user
		classCode += "@Rest(url = \"getroot\")\n";
		classCode += "public java.util.List<kommet.basic.User> getRootUser() throws " + KommetException.class.getName() + "{";
		classCode += "java.lang.String rootName = \"root\";\n";
		classCode += "java.util.List users = { select id, userName from User where userName = '#rootName.trim()' }; return users; }";
		
		// close class
		classCode += "\n}";
		
		return code + imports + classCode;
	}
}
