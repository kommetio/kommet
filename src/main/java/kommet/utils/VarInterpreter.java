/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.util.StringUtils;

import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.i18n.Locale;

/**
 * Utility methods for interpreting variables in strings.
 * @author Radek Krawiec
 * @created 15-03-2014
 */
public class VarInterpreter
{
	/**
	 * Interprets record variable in the given formula string.
	 * @param record
	 * @param formula
	 * @param recordVar
	 * @return
	 * @throws KommetException
	 */
	public static String interprete (Object obj, String formula, String recordVar, Locale locale) throws KommetException
	{
		if (formula == null)
		{
			return null;
		}
		
		if (!StringUtils.hasText(recordVar))
		{
			throw new KommetException("Record variable name is empty");
		}
		
		// find anything that starts with a hash, followed by an opening curly bracket, some characters,
		// and a closing curly bracket
		Matcher varMatcher = Pattern.compile("\\$\\{" + recordVar + "\\.([^\\}]+)\\}").matcher(formula);
		
		String interpretedFormula = new String(formula);
		
		if (obj instanceof Record)
		{
			Type type = ((Record)obj).getType();
			
			while (varMatcher.find())
			{
				String nestedField = varMatcher.group(1);
				String fieldValue = type.getField(nestedField).getDataType().getStringValue(((Record)obj).getField(nestedField), locale);
				interpretedFormula = interpretedFormula.replaceAll("\\$\\{" + recordVar + "." + nestedField + "\\}", fieldValue.toString());
			}
		}
		else
		{
			while (varMatcher.find())
			{
				String nestedField = varMatcher.group(1);
				String fieldValue;
				try
				{
					fieldValue = MiscUtils.nullAsBlank(PropertyUtils.getProperty(obj, nestedField)).toString();
				}
				catch (Exception e)
				{
					throw new KommetException("Error reading property " + nestedField + " from bean of type " + obj.getClass().getName() + ": " + e.getMessage());
				}
				
				interpretedFormula = interpretedFormula.replaceAll("\\$\\{" + recordVar + "." + nestedField + "\\}", fieldValue.toString());
			}
		}
		
		return interpretedFormula;
	}

	/**
	 * Extracts name of properties used in the given formula
	 * @param formula
	 * @param itemVar
	 * @return
	 */
	public static Collection<String> extractProperties(String formula, String itemVar)
	{
		// find anything that starts with a hash, followed by an opening curly bracket, some characters,
		// and a closing curly bracket
		Matcher varMatcher = Pattern.compile("\\$\\{" + itemVar + "\\.([^\\}]+)\\}").matcher(formula);
		
		Set<String> properties = new HashSet<String>();
		
		while (varMatcher.find())
		{
			properties.add(varMatcher.group(1));
		}
		
		return properties;
	}
}