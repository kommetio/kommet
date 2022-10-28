/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class ValidationUtil
{
	private static final String EMAIL_PATTERN = "^[A-Za-z0-9][_A-Za-z0-9-]*(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9][-A-Za-z0-9]*(\\.[-A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	private static final String RESOURCE_NAME_PATTERN = "^[A-Za-z]+([_\\dA-Za-z0-9])*[\\dA-Za-z0-9]$";
	private static final String OPTIONALLY_QUALIFIED_RESOURCE_NAME_PATTERN = "^([a-z][a-z\\.]+[a-z]\\.)?[A-Z]+([_\\dA-Za-z])*[\\dA-Za-z]$";
	private static final String PACKAGE_NAME = "^([a-z][a-z_0-9]*)(\\.[a-z][a-z_0-9]*)*$";
	private static final String LIBRARY_NAME_PATTERN = "^([a-z][a-z\\.]*[a-z]\\.)+[A-Z]+([_\\dA-Za-z])*[\\dA-Za-z]$";
	private static final String API_NAME_PATTERN = "^[A-Z]+([_\\dA-Za-z0-9])*[\\dA-Za-z0-9]$";
	private static final String FIELD_API_NAME_PATTERN = "^[a-z][_\\dA-Za-z]*[\\dA-Za-z]$";
	public static final String INVALID_RESOURCE_ERROR_EXPLANATION = "It can contain only letter of the English alphabet, digit and an underscore, must start with a letter and must not end with an underscore";
	
	private static Set<String> reservedNames = null;
	
	public static boolean isValidPackageName (String email)
	{
		return Pattern.matches(PACKAGE_NAME, email);
	}
	
	public static Set<String> getReservedFieldNames()
	{
		return getReservedNames();
	}
	
	public static Set<String> getReservedTypeNames()
	{
		return getReservedNames();
	}
	
	public static Set<String> getReservedNames()
	{
		if (reservedNames == null)
		{
			reservedNames = new HashSet<String>();
			reservedNames.add("order");
			reservedNames.add("true");
			reservedNames.add("false");
			reservedNames.add("table");
			reservedNames.add("asc");
			reservedNames.add("desc");
			reservedNames.add("select");
			reservedNames.add("column");
			reservedNames.add("case");
		}
		
		return reservedNames;
	}
	
	public static boolean isValidEmail (String email)
	{
		return Pattern.matches(EMAIL_PATTERN, email);
	}
	
	public static boolean isValidResourceName (String name)
	{
		if (!StringUtils.hasText(name))
		{
			return false;
		}
		return Pattern.matches(RESOURCE_NAME_PATTERN, name);
	}
	
	public static boolean isValidOptionallyQualifiedResourceName (String name)
	{
		if (!StringUtils.hasText(name))
		{
			return false;
		}
		return Pattern.matches(OPTIONALLY_QUALIFIED_RESOURCE_NAME_PATTERN, name);
	}
	
	public static boolean isValidLibraryName (String name)
	{
		if (!StringUtils.hasText(name))
		{
			return false;
		}
		return Pattern.matches(LIBRARY_NAME_PATTERN, name);
	}
	
	public static boolean isValidTypeApiName (String name)
	{
		if (!StringUtils.hasText(name))
		{
			return false;
		}
		
		if (!Pattern.matches(API_NAME_PATTERN, name))
		{
			return false;
		}
		else
		{
			// check for double underscore
			return !name.contains("__");
		}
	}
	
	public static boolean isValidFieldApiName (String name)
	{
		if (!StringUtils.hasText(name))
		{
			return false;
		}
		return Pattern.matches(FIELD_API_NAME_PATTERN, name);
	}
}