/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import kommet.utils.ValidationUtil;


public class ValidationUtilTest
{
	@Test
	public void testValidPackage()
	{
		Map<String, List<String>> phoneNumbers = new HashMap<String, List<String>>();
		
		phoneNumbers.put("John Lawson", Arrays.asList("3232312323", "8933555472"));
		phoneNumbers.put("Mary Jane", Arrays.asList("12323344", "492648333"));
		phoneNumbers.put("Mary Lou", Arrays.asList("77323344", "938448333"));
		
		Map<String, List<String>> filteredNumbers = phoneNumbers.entrySet().stream()
			.filter(x -> x.getKey().contains("Mary"))
			.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
		
		filteredNumbers.forEach((key, value) -> {
			System.out.println("Name: " + key + ": ");
			value.forEach(System.out::println);
		});
		
		assertTrue(ValidationUtil.isValidPackageName("com"));
		assertTrue(ValidationUtil.isValidPackageName("kommet"));
		assertTrue(ValidationUtil.isValidPackageName("com.rai_mme"));
		assertTrue(ValidationUtil.isValidPackageName("kommet.test"));
		assertTrue(ValidationUtil.isValidPackageName("kommet.test09.aaa"));
		assertFalse(ValidationUtil.isValidPackageName("kommet.test09.0aaa"));
		assertFalse(ValidationUtil.isValidPackageName("Aom.kommet"));
		assertFalse(ValidationUtil.isValidPackageName("Com"));
	}
	
	@Test
	public void testValidEmail()
	{
		assertTrue(ValidationUtil.isValidEmail("radek@domain.com"));
		assertTrue(ValidationUtil.isValidEmail("radek3432@domain.com"));
		assertTrue(ValidationUtil.isValidEmail("radek.kc@domain.com"));
		assertTrue(ValidationUtil.isValidEmail("radek-kc@domain.com"));
		assertTrue(ValidationUtil.isValidEmail("radek_kc@domain.com"));
		assertTrue(ValidationUtil.isValidEmail("radek_kc@domain-dash.com"));
		assertTrue(ValidationUtil.isValidEmail("radek_kc@domain-with-many-dashes.com"));
		assertFalse(ValidationUtil.isValidEmail("@domain.com"));
		assertFalse(ValidationUtil.isValidEmail("kamila@-domainstartingwithdash.com"));
		assertFalse(ValidationUtil.isValidEmail("@domain.com"));
		assertFalse(ValidationUtil.isValidEmail("radek.domain.com"));
		assertFalse(ValidationUtil.isValidEmail("-namestartingwithdash@domain.com"));
		assertFalse(ValidationUtil.isValidEmail("blaa@"));
	}
	
	@Test
	public void testValidQualifiedName()
	{
		assertTrue(ValidationUtil.isValidOptionallyQualifiedResourceName("My_Rule"));
		assertTrue(ValidationUtil.isValidOptionallyQualifiedResourceName("MyRule"));
		assertFalse(ValidationUtil.isValidOptionallyQualifiedResourceName("my_Rule"));
		assertTrue(ValidationUtil.isValidOptionallyQualifiedResourceName("My_Rule"));
		assertTrue(ValidationUtil.isValidOptionallyQualifiedResourceName("com.My_Rule"));
		assertTrue(ValidationUtil.isValidOptionallyQualifiedResourceName("kommet.My_Rule"));
		assertFalse(ValidationUtil.isValidOptionallyQualifiedResourceName("UpperCase.My_Rule"));
		assertFalse(ValidationUtil.isValidOptionallyQualifiedResourceName("UpperCase_.My_Rule"));
		assertFalse(ValidationUtil.isValidOptionallyQualifiedResourceName("Upper_Case.My_Rule"));
		assertFalse(ValidationUtil.isValidOptionallyQualifiedResourceName("com.rai_mme.My_Rule"));
		assertFalse(ValidationUtil.isValidOptionallyQualifiedResourceName("name"));
		assertTrue(ValidationUtil.isValidOptionallyQualifiedResourceName("Name0"));
		assertTrue(ValidationUtil.isValidOptionallyQualifiedResourceName("Name"));
	}
	
	@Test
	public void testValidLibraryName()
	{
		assertFalse(ValidationUtil.isValidLibraryName("My_Rule"));
		assertFalse(ValidationUtil.isValidLibraryName("MyRule"));
		assertFalse(ValidationUtil.isValidLibraryName("my_Rule"));
		assertTrue(ValidationUtil.isValidLibraryName("com.My_Rule"));
		assertTrue(ValidationUtil.isValidLibraryName("kommet.My_Rule"));
		assertFalse(ValidationUtil.isValidLibraryName("UpperCase.My_Rule"));
		assertFalse(ValidationUtil.isValidLibraryName("UpperCase_.My_Rule"));
		assertFalse(ValidationUtil.isValidLibraryName("Upper_Case.My_Rule"));
		assertFalse(ValidationUtil.isValidLibraryName("com.rai_mme.My_Rule"));
		assertFalse(ValidationUtil.isValidLibraryName("name"));
		assertFalse(ValidationUtil.isValidLibraryName("Name0"));
		assertFalse(ValidationUtil.isValidLibraryName("Name"));
		assertFalse(ValidationUtil.isValidLibraryName("com.api"));
		assertFalse(ValidationUtil.isValidLibraryName("com.api.name"));
		assertTrue(ValidationUtil.isValidLibraryName("com.api.Name"));
		assertFalse(ValidationUtil.isValidLibraryName("Name"));
		assertFalse(ValidationUtil.isValidLibraryName("com.2om.Name"));
		assertFalse(ValidationUtil.isValidLibraryName("com.2Name"));
		assertTrue(ValidationUtil.isValidLibraryName("com.Name2"));
		assertFalse(ValidationUtil.isValidLibraryName("com.1ame"));
		assertFalse(ValidationUtil.isValidLibraryName("com.name965"));
		assertFalse(ValidationUtil.isValidLibraryName("com.bit0aname2"));
		assertFalse(ValidationUtil.isValidLibraryName("com.bit0a.name2"));
		assertFalse(ValidationUtil.isValidLibraryName("com.bit0.name"));
		assertTrue(ValidationUtil.isValidLibraryName("cm.bit.Name"));
		assertTrue(ValidationUtil.isValidLibraryName("rm.bit.Name"));
	}
	
	@Test
	public void testValidApiName()
	{
		assertTrue(ValidationUtil.isValidTypeApiName("SomeType"));
		assertTrue(ValidationUtil.isValidTypeApiName("SomeType2"));
		assertTrue(ValidationUtil.isValidTypeApiName("Some_Type"));
		assertTrue(ValidationUtil.isValidTypeApiName("Some_Type2"));
		assertTrue(ValidationUtil.isValidTypeApiName("Some_Type_232"));
		assertFalse(ValidationUtil.isValidTypeApiName("_Some_Type2"));
		assertFalse(ValidationUtil.isValidTypeApiName("_Some_Type"));
		assertFalse(ValidationUtil.isValidTypeApiName("_Some"));
		assertFalse(ValidationUtil.isValidTypeApiName("lowerCaseStart"));
		assertFalse(ValidationUtil.isValidTypeApiName("2digitStart"));
		assertFalse(ValidationUtil.isValidTypeApiName("UnderscoreEnd_"));
		assertFalse(ValidationUtil.isValidTypeApiName("Double__Underscore"));
		
		// one letter not allowed
		assertFalse(ValidationUtil.isValidTypeApiName("A"));
		
		assertTrue(ValidationUtil.isValidTypeApiName("AB"));
	}
	
	@Test
	public void testValidFieldApiName()
	{
		assertTrue(ValidationUtil.isValidFieldApiName("abc"));
		assertTrue(ValidationUtil.isValidFieldApiName("id"));
		assertFalse(ValidationUtil.isValidFieldApiName("Asdjskd"));
		assertFalse(ValidationUtil.isValidFieldApiName("abc_"));
		assertFalse(ValidationUtil.isValidFieldApiName("_abc"));
		assertFalse(ValidationUtil.isValidFieldApiName("aąbc"));
		assertFalse(ValidationUtil.isValidFieldApiName("AB_bc"));
		assertTrue(ValidationUtil.isValidFieldApiName("aB_bc"));
		assertFalse(ValidationUtil.isValidFieldApiName("łaa"));
	}
	
	@Test
	public void testIsValidResourceName()
	{
		assertTrue(ValidationUtil.isValidResourceName("aajsk"));
		assertTrue(ValidationUtil.isValidResourceName("aajsk44"));
		assertTrue(ValidationUtil.isValidResourceName("aajsk_AA"));
		assertTrue(ValidationUtil.isValidResourceName("aajsk_11"));
		assertTrue(!ValidationUtil.isValidResourceName("111"));
		assertTrue(!ValidationUtil.isValidResourceName("aajsk_"));
		assertTrue(!ValidationUtil.isValidResourceName("_aajsk"));
		assertTrue(!ValidationUtil.isValidResourceName("aa dsd"));
	}
}
