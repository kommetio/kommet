/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.keetle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.RecordProxy;
import kommet.basic.keetle.ActionParamCastException;
import kommet.basic.keetle.FieldLabelNotFoundException;
import kommet.basic.keetle.StandardObjectController;
import kommet.basic.keetle.ViewUtil;
import kommet.basic.keetle.tagdata.Namespace;
import kommet.basic.keetle.tagdata.Tag;
import kommet.basic.keetle.tagdata.TagData;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.SystemSettingService;
import kommet.systemsettings.SystemSettingKey;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.DateTimeUtil;
import kommet.web.RequestAttributes;
import kommet.web.actions.ActionUtil;

public class ViewUtilTest extends BaseUnitTest
{
	@Inject
	AppConfig config;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	SystemSettingService settingService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	DataService dataService;
	
	private void testTagData(EnvData env) throws KommetException
	{
		TagData tagData = TagData.get(env, config);
		assertNotNull(tagData);
		assertEquals(1, tagData.getNamespaces().size());
		
		Namespace rmNamespace = tagData.getNamespaces().get(0);
		assertNotNull(rmNamespace.getTagByName("dataTable"));
		
		Tag dataTable = rmNamespace.getTagByName("dataTable");
		assertEquals(8, dataTable.getAttributes().size());
		assertNotNull(dataTable.getAttribute("query"));
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testParamCasting() throws KommetException, ClassNotFoundException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		assertEquals(23, ActionUtil.castParam("23", Integer.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals("Leonard", ActionUtil.castParam("Leonard", String.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals(null, ActionUtil.castParam(null, String.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals(null, ActionUtil.castParam("", String.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals("", ActionUtil.castParam("", String.class, "param-name", false, Locale.EN_US, compiler, env));
		assertEquals(Boolean.TRUE, ActionUtil.castParam("true", Boolean.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals(Boolean.FALSE, ActionUtil.castParam("false", Boolean.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals(null, ActionUtil.castParam(null, Boolean.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals(KID.get("0040000000231"), ActionUtil.castParam("0040000000231", KID.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals(null, ActionUtil.castParam(null, KID.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals(null, ActionUtil.castParam(null, Date.class, "param-name", true, Locale.EN_US, compiler, env));
		assertEquals(null, ActionUtil.castParam("", Date.class, "param-name", true, Locale.EN_US, compiler, env));
		
		String serializedPigeon = "{ \"name\": \"mark\", \"age\": 11 }";
		
		// test deserialized pigeon record
		Object pigeon = ActionUtil.castParam(serializedPigeon, compiler.getClass(pigeonType.getQualifiedName(), true, env), "param-name", true, Locale.EN_US, compiler, env);
		assertNotNull(pigeon);
		assertTrue(pigeon instanceof RecordProxy);
		assertEquals("mark", ((RecordProxy)pigeon).getField("name"));
		assertEquals(11, ((RecordProxy)pigeon).getField("age"));

		Date parsedDate = (Date)ActionUtil.castParam("2015-04-11", Date.class, "param-name", true, Locale.EN_US, compiler, env);
		assertEquals(3, parsedDate.getMonth());
		assertEquals(11, parsedDate.getDate());
		
		parsedDate = (Date)ActionUtil.castParam("2015-04-11 10:23:11", Date.class, "param-name", true, Locale.EN_US, compiler, env);
		assertEquals(3, parsedDate.getMonth());
		assertEquals(11, parsedDate.getDate());
		
		System.out.println(parsedDate);
		
		assertEquals(10, DateTimeUtil.getHours(parsedDate, "GMT"));
		assertEquals(23, DateTimeUtil.getMinutes(parsedDate));
		assertEquals(11, DateTimeUtil.getSeconds(parsedDate));
		
		parsedDate = (Date)ActionUtil.castParam("2015-04-11 10:23:11.328", Date.class, "param-name", true, Locale.EN_US, compiler, env);
		assertEquals(3, parsedDate.getMonth());
		assertEquals(11, parsedDate.getDate());
		assertEquals(10, DateTimeUtil.getHours(parsedDate, "GMT"));
		assertEquals(23, DateTimeUtil.getMinutes(parsedDate));
		assertEquals(11, DateTimeUtil.getSeconds(parsedDate));
		assertEquals(328, DateTimeUtil.getMilliseconds(parsedDate));
		
		ActionUtil.castParam(new ArrayList<String>(), String.class, "param-name", true, Locale.EN_US, compiler, env);
		
		try
		{
			ActionUtil.castParam(new ArrayList<String>(), KID.class, "param-name", true, Locale.EN_US, compiler, env);
			fail("Using non-string param to cast should fail");
		}
		catch (KommetException e)
		{
			// expected
			assertEquals("Value of type " + ArrayList.class.getName() + " cannot be cast to integer", e.getMessage());
		}
		
		try
		{
			ActionUtil.castParam("0040000000231s", KID.class, "param-name", true, Locale.EN_US, compiler, env);
			fail("Casting invalid string to KID should throw an exception");
		}
		catch (ActionParamCastException e)
		{
			// expected exception
			assertEquals("Argument value 0040000000231s cannot be cast to KID", e.getMessage());
		}
	}
	
	@Test
	public void testConvertKeetleVars() throws KommetException, SecurityException, NoSuchMethodException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		String input = "some ${var.me} and \\${another.escaped }";
		String result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("some \\${var.me} and \\\\${another.escaped }"));
		
		input = "${var.me} and \\${another.escaped }";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("\\${var.me} and \\\\${another.escaped }"));
		
		input = "quoted \"${var.me}\" and \\${another.escaped }";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("quoted \"\\${var.me}\" and \\\\${another.escaped }"));
		
		// now test translating KTL vars
		input = "#{ktl.var}and ${var.me} and \\${another.escaped }";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("${ktl.var}and \\${var.me} and \\\\${another.escaped }"));
		
		input = "\"be#{ktl.var}\"and ${var.me} and \\${another.escaped }";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("\"be${ktl.var}\"and \\${var.me} and \\\\${another.escaped }"));
		
		testTextLabels(env);
		testFieldLabels(env);
		testPageDataVars(env);
		testRecordFields(env);
		testTagData(env);
	}

	private void testPageDataVars(EnvData env) throws KommetException
	{
		String ktlCode = "Some <div>code{{parent.child.name}} and</div>";
		String jsp = ViewUtil.keetleToJSP(ktlCode, config, env);
		assertTrue("Incorrect JSP code:\n" + jsp, jsp.contains("Some <div>code${pageData.getValue('parent').getChild().getName()} and</div>"));
		
		ktlCode = "Some <div>code{{parent.child.name11 >= child.age}} and</div>";
		jsp = ViewUtil.keetleToJSP(ktlCode, config, env);
		assertTrue("Incorrect JSP code:\n" + jsp, jsp.contains("Some <div>code${pageData.getValue('parent').getChild().getName11() >= pageData.getValue('child').getAge()} and</div>"));
		
		ktlCode = "Some <div>code{{parent.child.name - child.age}} and</div>";
		jsp = ViewUtil.keetleToJSP(ktlCode, config, env);
		assertTrue("Incorrect JSP code:\n" + jsp, jsp.contains("Some <div>code${pageData.getValue('parent').getChild().getName() - pageData.getValue('child').getAge()} and</div>"));
		
		ktlCode = "<c:if test=\"{{any.var}}\">";
		jsp = ViewUtil.keetleToJSP(ktlCode, config, env);
		assertTrue("Incorrect JSP code:\n" + jsp, jsp.contains("<c:if test=\"${pageData.getValue('any').getVar()}\">"));
		
		// test incorrect VREL expression - with only one opening curly bracket
		ktlCode = "<c:if test=\"{any.var}}\">";
		jsp = ViewUtil.keetleToJSP(ktlCode, config, env);
		assertTrue("Incorrect JSP code:\n" + jsp, jsp.contains("<c:if test=\"{any.var}}\">"));
		
		ktlCode = "<c:if test=\"{{single_Var01}}\">";
		jsp = ViewUtil.keetleToJSP(ktlCode, config, env);
		assertTrue("Incorrect JSP code:\n" + jsp, jsp.contains("<c:if test=\"${pageData.getValue('single_Var01')}\">"));
	}

	private void testFieldLabels(EnvData env) throws SecurityException, NoSuchMethodException, KommetException
	{
		AuthData authData = dataHelper.getRootAuthData(env);
		
		settingService.setSetting(SystemSettingKey.IGNORE_NON_EXISTING_FIELD_LABELS, "true", authData, env);
		assertNull(env.getEnvSpecificFieldLabel("com.User.name", authData));
		
		settingService.setSetting(SystemSettingKey.IGNORE_NON_EXISTING_FIELD_LABELS, "false", authData, env);
		
		try
		{
			env.getEnvSpecificFieldLabel("com.User.name", authData);
			fail("Referencing a non-existing field label should throw an exception");
		}
		catch (FieldLabelNotFoundException e)
		{
			// expected
		}
		
		assertEquals("Email", env.getEnvSpecificFieldLabel("kommet.basic.User.email", authData));
		
		
		
		String envSpecificTypeGetter = EnvData.class.getMethod("getEnvSpecificFieldLabel", String.class, AuthData.class).getName();
		
		String input = "\"be#{ktl.var}\"and ${var.me} and \\${another.escaped } and email:{{$fieldLabel.User.email}}";
		String result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("\"be${ktl.var}\"and \\${var.me} and \\\\${another.escaped } and email:${" + RequestAttributes.ENV_ATTR_NAME + "." + envSpecificTypeGetter + "('User.email', " + RequestAttributes.AUTH_DATA_ATTR_NAME + ")}"));
		
		input = "\"be#{ktl.var}\"and ${var.me} and \\${another.escaped } and email:{{$fieldLabel.kommet.Company.email}}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("\"be${ktl.var}\"and \\${var.me} and \\\\${another.escaped } and email:${" + RequestAttributes.ENV_ATTR_NAME + "." + envSpecificTypeGetter + "('kommet.Company.email', " + RequestAttributes.AUTH_DATA_ATTR_NAME +")}"));
	}

	private void testTextLabels(EnvData env) throws KommetException
	{
		String dictArg = ", " + RequestAttributes.AUTH_DATA_ATTR_NAME + ".getLocale()";
		
		String input = "\"be#{ktl.var}\"and ${var.me} and \\${another.escaped } and {{$Label.mike}}";
		String result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("\"be${ktl.var}\"and \\${var.me} and \\\\${another.escaped } and ${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('mike'" + dictArg + ")}"));
		
		input = "and {{Label.like}}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse(result.contains("and ${textLabels.get('like')}"));
		
		input = "and{{$Label.like.me}}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("and${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('like.me'" + dictArg + ")}"));
		
		input = "and{{$Label.like.me}}tail";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("and${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('like.me'" + dictArg + ")}tail"));
		
		input = "and {{$Label.like.me}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse("Incorrect JSP result:\n" + result, result.contains("and ${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('like.me'" + dictArg + ")}"));
		
		// check lowercase label prefix
		input = "and {{$label.like.me}}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("and ${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('like.me'" + dictArg + ")}"));
		
		// check whitespace before $label prefix
		input = "and {{ $label.like.me}}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse("Incorrect JSP result:\n" + result, result.contains("and ${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('like.me'" + dictArg + ")}"));
		
		// check whitespace after $label prefix
		input = "and {{$label.like.me }}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse("Incorrect JSP result:\n" + result, result.contains("and ${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('like.me'" + dictArg + ")}"));
		
		// check double white space
		input = "and{{ $Label.like.me }}tail";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse("Incorrect JSP result:\n" + result, result.contains("and${ " + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('like.me'" + dictArg + ") }tail"));
	}
	
	private void testRecordFields(EnvData env) throws KommetException
	{	
		String recordRef = "pageData.getValue('" + StandardObjectController.RECORD_VAR_PARAM + "')";
		
		String input = "\"be#{ktl.var}\"and ${var.me} and \\${another.escaped } and {{$record.mike}}";
		String result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("\"be${ktl.var}\"and \\${var.me} and \\\\${another.escaped } and ${" + recordRef + ".getField('mike')}"));
		
		input = "and {{Record.like}}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse(result.contains("and ${" + StandardObjectController.RECORD_VAR_PARAM + ".get('like')}"));
		
		input = "and{{$record.like.me}}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("and${" + recordRef + ".getField('like.me')}"));
		
		input = "and{{$record.like.me}}tail";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertTrue("Incorrect JSP result:\n" + result, result.contains("and${" + recordRef + ".getField('like.me')}tail"));
		
		input = "and {{$Record.like.me.at}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse("Incorrect JSP result:\n" + result, result.contains("and ${" + recordRef + ".getField('like.me.at')}"));
		
		// check whitespace before $record prefix
		input = "and {{ $record.like.me}}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse("Incorrect JSP result:\n" + result, result.contains("and ${" + recordRef + ".getField('like.me')}"));
		
		// check whitespace after $label prefix
		input = "and {{$label.like.me }}";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse("Incorrect JSP result:\n" + result, result.contains("and ${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".getField('like.me')}"));
		
		// check double white space
		input = "and{{ $Record.like.me }}tail";
		result = ViewUtil.keetleToJSP(input, config, env);
		assertFalse("Incorrect JSP result:\n" + result, result.contains("and${ " + recordRef + ".getField('like.me') }tail"));
	}
}
