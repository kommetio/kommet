/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.Class;
import kommet.basic.User;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.testdata.TestClassOne;
import kommet.testdata.TestClassTwo;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.DateTimeUtil;
import kommet.utils.MiscUtils;

public class MiscUtilsTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Test
	public void testPad()
	{
		assertEquals("1111abc", MiscUtils.padLeft("abc", 7, '1'));
		assertEquals("abc", MiscUtils.padLeft("abc", 3, '1'));
	}
	
	@Test
	public void testTrimExtension()
	{
		assertEquals("me", MiscUtils.trimExtension("me.txt"));
		assertEquals("me", MiscUtils.trimExtension("me"));
		assertEquals("Me_me", MiscUtils.trimExtension("Me_me.txt"));
		assertEquals("cone.Two.me", MiscUtils.trimExtension("cone.Two.me.t"));
	}
	
	@Test
	public void testReplaceNotInQuotes()
	{
		String src = "my string is \"my string\" and \"string\"";
		assertEquals(MiscUtils.replaceNotInQuotes("my", src, "one"), "one string is \"my string\" and \"string\"");
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testParseDate() throws ParseException
	{
		Date date = MiscUtils.parseDateTime("2015-03-23", true);
		assertNotNull(date);
		assertEquals(23, date.getDate());
		assertEquals(2, date.getMonth());
		
		date = MiscUtils.parseDateTime("2015-03-23 22:01:05", true);
		assertNotNull(date);
		assertEquals(23, date.getDate());
		assertEquals(2, date.getMonth());
		assertEquals(22, DateTimeUtil.getHours(date, "GMT"));
		assertEquals(1, DateTimeUtil.getMinutes(date));
		assertEquals(5, DateTimeUtil.getSeconds(date));
		
		date = MiscUtils.parseDateTime("2015-04-11 10:23:11", true);
		assertEquals(3, date.getMonth());
		assertEquals(11, date.getDate());
		assertEquals(10, DateTimeUtil.getHours(date, "GMT"));
		assertEquals(23, DateTimeUtil.getMinutes(date));
	}
	
	@Test
	public void testMapById() throws KommetException
	{
		List<User> users = new ArrayList<User>();
		
		User u1 = new User();
		u1.setId(KID.get("0040000000001"));
		users.add(u1);
		
		User u2 = new User();
		u2.setId(KID.get("0040000000002"));
		u2.setUserName("tester");
		users.add(u2);
		
		Map<KID, User> usersById = MiscUtils.mapById(users);
		assertEquals(2, usersById.size());
		assertNotNull(usersById.get(KID.get("0040000000002")));
		assertEquals("tester", usersById.get(KID.get("0040000000002")).getUserName());
	}
	
	@Test
	public void testFileDirectoryMap() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		List<Class> files = new ArrayList<Class>();
		
		Class file1 = new Class();
		file1.setName("File1");
		file1.setPackageName("kommet.test");
		files.add(file1);
		
		Class file2 = new Class();
		file2.setName("File2");
		file2.setPackageName("kommet.test");
		files.add(file2);
		
		Class file3 = new Class();
		file3.setName("File3");
		file3.setPackageName("kommet");
		files.add(file3);
		
		LinkedHashMap<String, Object> mappedFiles = MiscUtils.getFileDirectoryMap(files, null, null, null, env);
		assertNotNull(mappedFiles.get("kommet"));
		assertTrue(mappedFiles.get("kommet") instanceof LinkedHashMap<?, ?>);
		
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object> kommetPackage = (LinkedHashMap<String, Object>)mappedFiles.get("kommet");
		assertEquals(2, kommetPackage.size());
		assertNotNull(kommetPackage.get("File3"));
		assertNotNull(kommetPackage.get("test"));
	}
	
	@Test
	public void testTokenize()
	{
		String expr = "this is some+expr (and another)s \"ee\"";
		Set<Character> operators = new HashSet<Character>();
		operators.add('+');
		operators.add('-');
		
		List<String> tokens = MiscUtils.tokenize(expr, '"', operators, operators);
		
		assertEquals(11, tokens.size());
		assertEquals("some", tokens.get(2));
		assertEquals("+", tokens.get(3));
		assertEquals("\"ee\"", tokens.get(10));
		
		// test tokenization of string with commas in quotes
		expr = "Text \"word, another\" after, comma \"bracket(\\\"aa\"";
		tokens = MiscUtils.tokenize(expr, '"', null, operators);
		assertEquals(6, tokens.size());
		
		expr = "\"starting with\" + quote";
		tokens = MiscUtils.tokenize(expr, '"', null, operators);
		assertEquals(3, tokens.size());
	}
	
	@Test
	public void testTokenizeWithoutWhitespace()
	{
		String expr = "this is some>=expr (and another)s \"ee\"";
		Set<Character> operators = new HashSet<Character>();
		operators.add('+');
		operators.add('-');
		operators.add('>');
		operators.add('<');
		operators.add('=');
		
		List<String> tokens = MiscUtils.tokenize(expr, '"', null, operators);
		
		assertEquals(11, tokens.size());
		assertEquals("some", tokens.get(2));
		assertEquals(">=", tokens.get(3));
		assertEquals("\"ee\"", tokens.get(10));
		
		expr = "Text \"word, another\" after, comma \"bracket(\\\"aa\"";
		tokens = MiscUtils.tokenize(expr, '"', null, operators);
		assertEquals(6, tokens.size());
		
		// test tokenization of string with commas in quotes
		expr = "this is some>11 (and another)s \"ee\"";
		tokens = MiscUtils.tokenize(expr, '"', operators, operators);
		System.out.println("Tokens: " + MiscUtils.implode(tokens, ":"));
		assertEquals(11, tokens.size());
		assertEquals(">", tokens.get(3));
		assertEquals("11", tokens.get(4));
		
		expr = "\"starting with\" <=quote";
		tokens = MiscUtils.tokenize(expr, '"', null, operators);
		assertEquals(3, tokens.size());
		
		expr = "\"starting with\" <='quote'";
		tokens = MiscUtils.tokenize(expr, '"', null, operators);
		assertEquals(3, tokens.size());
		assertEquals("<=", tokens.get(1));
		assertEquals("'quote'", tokens.get(2));
		
		expr = "something>=\"quote\"";
		tokens = MiscUtils.tokenize(expr, '"', null, operators);
		assertEquals(3, tokens.size());
		assertEquals(">=", tokens.get(1));
		assertEquals("\"quote\"", tokens.get(2));
		
		expr = "something>='quote'";
		tokens = MiscUtils.tokenize(expr, '\'', null, operators);
		assertEquals(3, tokens.size());
		assertEquals(">=", tokens.get(1));
		assertEquals("'quote'", tokens.get(2));
	}
	
	@Test
	public void testImplode()
	{
		List<String> items = new ArrayList<String>();
		items.add("ab");
		items.add("ee");
		items.add("xyz");
		
		assertEquals("'ab', 'ee', 'xyz'", MiscUtils.implode(items, ", ", "'"));
		assertEquals("ab ee xyz", MiscUtils.implode(items, " ", null));
	}
	
	@Test
	public void trimRight()
	{
		assertEquals(",eta", MiscUtils.trimRight(",eta,,", ','));
	}
	
	@Test
	public void splitByWhitespaceOrQuote()
	{
		List<String> tokens = MiscUtils.splitByWhitespaceAndQuote("select 'maker \\'ble'		tee  'me	'");
		assertEquals("select", tokens.get(0));
		assertEquals("'maker \\'ble'", tokens.get(1));
		assertEquals("tee", tokens.get(2));
		assertEquals("'me	'", tokens.get(3));
	}
	
	@Test
	public void getPartialProperties()
	{
		List<String> parts = MiscUtils.getPartialProperties("one.two.three", true);
		assertEquals(2, parts.size());
		assertEquals("one", parts.get(0));
		assertEquals("one.two", parts.get(1));
	}
	
	@Test
	public void testCopyProperties() throws KommetException
	{
		TestClassOne one = new TestClassOne();
		one.setPropertyOne("one");
		one.setPropertyTwo("two");
		
		TestClassTwo two = new TestClassTwo();
		
		MiscUtils.copyProperties(one, two);
		assertEquals("one", two.getPropertyOne());
		assertEquals("two", two.getPropertyTwo());
	}
	
	@Test
	public void testGetSetter() throws KommetException, SecurityException, NoSuchMethodException
	{
		Method setterMethod = MiscUtils.getSetter(Class.class.getMethod("getId"));
		assertNotNull(setterMethod);
		assertEquals("setId", setterMethod.getName());
	}
	
	@Test
	public void testConvertPackageNames() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		
		String packageName = "one.two.three";
		String envSpecificPackage = MiscUtils.userToEnvPackage(packageName, env);
		assertEquals(env.getEnv().getBasePackage() + "." + packageName, envSpecificPackage);
		assertEquals(packageName, MiscUtils.envToUserPackage(envSpecificPackage, env));
	}
	
	@Test
	public void testGetStaticMethod()
	{
		Method method = MiscUtils.getStaticMethod(MiscUtils.class.getName() + ".newLinesToBr", null, String.class);
		assertNotNull(method);
		assertNull(MiscUtils.getStaticMethod(MiscUtils.class.getName() + ".newLinesToBr_aaa", null, String.class));
	}
}
