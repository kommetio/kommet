/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.Layout;
import kommet.basic.RecordProxy;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.data.BasicModel;
import kommet.data.Env;
import kommet.data.Field;
import kommet.data.NoSuchFieldException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;

public class MiscUtils
{	
	public static final String DATE_TIME_FORMAT_FULL = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String DATE_TIME_FORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_FORMAT_YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
	public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
	
	private static SecureRandom random = new SecureRandom();
	
	public static String newLinesToBr (String s)
	{
		return s != null ? s.replaceAll("\\r?\\n", "<br/>") : null;
	}
	
	public static String toNonQuoteRegex(String regex)
	{
		String quotedToken = "(?:\"((\\\\.)|[^\"])*\")";
		return "(" + regex + "|" + quotedToken + ")";
	}
	
	public static int collectionSize (Collection<?> c)
	{
		return c != null ? c.size() : 0;
	}
	
	public static String replaceNotInQuotes(String regex, String source, String replacement)
	{
		Pattern pattern = Pattern.compile(regex + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
		Matcher matcher = pattern.matcher(source);
		
		StringBuffer result = new StringBuffer();
		while (matcher.find())
		{
			matcher.appendReplacement(result, replacement);
		}
		matcher.appendTail(result);
		return result.toString();
	}
	
	public static String[] splitFileName (String fileName)
	{
		String[] bits = new String[2];
		bits[1] = fileName.substring(fileName.lastIndexOf('.') + 1);
		bits[0] = fileName.substring(0, fileName.lastIndexOf('.'));
		return bits;
	}
	
	/*public static ArrayList<String> getClassNamesFromPackage(String packageName) throws KommetException
	{
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    URL packageURL;
	    ArrayList<String> names = new ArrayList<String>();;

	    packageName = packageName.replace(".", "/");
	    packageURL = classLoader.getResource(packageName);

	    if (packageURL.getProtocol().equals("jar"))
	    {
	        String jarFileName;
	        Enumeration<JarEntry> jarEntries;
	        String entryName;

	        // build jar file name, then loop through zipped entries
	        try
			{
				jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				throw new KommetException("Unsupported URL encoding for JAR file name " + packageURL);
			}
	        
	        jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
	        System.out.println(">"+jarFileName);
	        
	        JarFile jf = null;
	        try
			{
				jf = new JarFile(jarFileName);
			}
			catch (IOException e)
			{
				throw new KommetException("Error reading JAR file " + jarFileName + ": " + e.getMessage());
			}
	        
	        jarEntries = jf.entries();
	        
	        while(jarEntries.hasMoreElements())
	        {
	            entryName = jarEntries.nextElement().getName();
	            if(entryName.startsWith(packageName) && entryName.length()>packageName.length()+5)
	            {
	                entryName = entryName.substring(packageName.length(),entryName.lastIndexOf('.'));
	                names.add(entryName);
	            }
	        }
	        
	        try
			{
				jf.close();
			}
			catch (IOException e)
			{
				throw new KommetException("Error closing JAR file " + jarFileName + ": " + e.getMessage());
			}

	    // loop through files in classpath
	    }
	    else
	    {
	    	URI uri = null;
	    	
			try
			{
				uri = new URI(packageURL.toString());
			}
			catch (URISyntaxException e)
			{
				throw new KommetException("URI syntax error " + packageURL.toString());
			}
			
	    	File folder = new File(uri.getPath());
	    
	        // won't work with path which contains blank (%20)
	        // File folder = new File(packageURL.getFile()); 
	        String entryName;
	        
	        for (File actual: folder.listFiles())
	        {
	            entryName = actual.getName();
	            entryName = entryName.substring(0, entryName.lastIndexOf('.'));
	            names.add(entryName);
	        }
	    }
	    return names;
	}*/
	
	public static List<String> getFilesFromDir (File dir) throws KommetException
	{
		List<String> files = new ArrayList<String>();
		if (!dir.isDirectory())
		{
			throw new KommetException(dir.getName() + " is not a directory");
		}
		
		for (File f : dir.listFiles())
        {
			if (f.isDirectory())
			{
				files.addAll(getFilesFromDir(f));
			}
			else
			{
				files.add(f.getName());
			}
        }
		
		return files;
	}
	
	/*public static Field getAccessLevelFieldTemplate() throws KommetException
	{
		Field accessField = new Field();
		accessField.setApiName("accessLevel");
		accessField.setLabel("Access Level");
		
		EnumerationDataType enumDT = new EnumerationDataType("Editable\nRead-only\nRead-only methods\nClosed");
		enumDT.setValidateValues(true);
		
		accessField.setDataType(enumDT);
		accessField.setDbColumn("accesslevel");
		accessField.setRequired(true);
		accessField.setDefaultValue("Editable"); 
		return accessField;
	}*/
	
	/**
	 * Adds days to date.
	 * @param date
	 * @param days
	 * @return
	 */
	public static Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }
	
	public static List<String> tokenize (String str, char quoteChar, Set<Character> predefinedTokenChars, Set<Character> predefinedTokenSpecialBreaks)
	{
		Set<Character> quoteChars = new HashSet<Character>();
		quoteChars.add(quoteChar);
		return tokenize(str, quoteChars, predefinedTokenChars, predefinedTokenSpecialBreaks);
	}
	
	/**
	 * Splits the given string into tokens. Words, brackets and quote characters are treated as separate tokens.
	 * @param str
	 * @param quoteChar
	 * @return
	 */
	public static List<String> tokenize (String str, char quoteChar)
	{
		Set<Character> quoteChars = new HashSet<Character>();
		quoteChars.add(quoteChar);
		return tokenize(str, quoteChars, null, null);
	}
	
	/*public static String queryToIdFormat (String dalQuery, EnvData env) throws KommetException
	{
		Criteria c = env.getSelectCriteriaFromDAL(dalQuery);
		
		StringBuilder resultQuery = new StringBuilder();
		resultQuery.append("SELECT ");
		
		List<String> propertyIds = new ArrayList<String>();
		
		// add IDs of simple properties
		for (String prop : c.getProperties())
		{
			propertyIds.add(c.getType().getField(prop).getKID().getId());
		}
		
		// add IDs of nested properties
		for (String nestedProp : c.getNestedProperties())
		{
			propertyIds.add(getPropertyIdFormat(nestedProp, c.getType(), env));
		}
		
		// add aggregate functions
		for (AggregateFunctionCall aggr : c.getAggregateFunctions())
		{
			propertyIds.add(aggr.getFunction() + "(" + getPropertyIdFormat(aggr.getProperty(), c.getType(), env) + ")")
		}
		
		resultQuery.append(MiscUtils.implode(propertyIds, ","));
		resultQuery.append(" FROM " + c.getType().getKID());
	}*/

	/**
	 * Splits the given string into tokens. Words, brackets and quote characters are treated as separate tokens.
	 * @param str
	 * @param quoteChar
	 * @param predefinedTokenChars Collection of characters that are always treated as separate tokens if located outside of a quoted string.
	 * @param predefinedTokenSpecialBreaks Collection of characters that are always separated from other characters, but not from each other.
	 * I.e. it is guaranteed that every character from this collection will not be a part of a token together with a character from outside of this collection.
	 * @return
	 */
	public static List<String> tokenize (String str, Set<Character> quoteChars, Set<Character> predefinedTokenChars, Set<Character> predefinedTokenSpecialBreaks)
	{
		if (predefinedTokenChars == null)
		{
			predefinedTokenChars = new HashSet<Character>();
		}
		
		if (predefinedTokenSpecialBreaks == null)
		{
			predefinedTokenSpecialBreaks = new HashSet<Character>();
		}
		
		List<String> tokens = new ArrayList<String>();
		Character prevChar = null;
		Character currChar = null;
		String currWord = new String();
		boolean inQuotes = false;
		
		for (int i = 0; i < str.length(); i++)
		{
			prevChar = currChar;
			currChar = str.charAt(i);
			
			if (quoteChars.contains(currChar))
			{
				if (prevChar != null && prevChar == '\\')
				{
					currWord += currChar;
				}
				else
				{
					// encountered a non-escaped single quote
					
					if (inQuotes)
					{
						// append the quote to the current word
						currWord += currChar;
						// if in quotes, close the quote and the word
						tokens.add(currWord);
						currWord = new String();
						inQuotes = false;
					}
					else
					{
						// end previous word
						if (currWord.length() > 0)
						{
							tokens.add(currWord);
						}
						currWord = String.valueOf(currChar);
						inQuotes = true;
					}
				}
			}
			else if (org.datanucleus.util.StringUtils.isWhitespace(String.valueOf(currChar)))
			{
				if (inQuotes)
				{
					currWord += currChar;
				}
				else
				{
					// end current word
					if (currWord.length() > 0)
					{
						tokens.add(currWord);
						currWord = new String();
					}
				}
			}
			else if (!inQuotes && (currChar == ')' || currChar == '(' || currChar == ',' || predefinedTokenChars.contains(currChar)))
			{
				// brackets will be treated as separated words
				// if a bracket is encountered, end the previous word, insert the bracket as a new word
				// and start another empty word
				if (currWord.length() > 0)
				{
					tokens.add(currWord);
				}
				tokens.add(String.valueOf(currChar));
				currWord = new String();
			}
			else if (!inQuotes && predefinedTokenSpecialBreaks.contains(currChar) && prevChar != null && !predefinedTokenSpecialBreaks.contains(prevChar))
			{
				if (currWord.length() > 0)
				{
					tokens.add(currWord);
				}
				currWord = String.valueOf(currChar);
			}
			else if (!inQuotes && !predefinedTokenSpecialBreaks.contains(currChar) && prevChar != null && predefinedTokenSpecialBreaks.contains(prevChar))
			{
				if (currWord.length() > 0)
				{
					tokens.add(currWord);
				}
				currWord = String.valueOf(currChar);
			}
			else
			{
				currWord += currChar;
			}
		}
		
		// add last word if not empty
		if (currWord.length() > 0)
		{
			tokens.add(currWord);
		}
		
		return tokens;
	}
	
	public static Type cloneType (Type type) throws KommetException
	{
		Type newType = null;
		try
		{
			/*newType = type.getClass().newInstance();
			newType.setApiName(type.getApiName());
			newType.setBasic(type.isBasic());
			newType.setCreated(type.getCreated());
			newType.setDbTable(type.getDbTable());
			newType.setKeyPrefix(type.getKeyPrefix());
			newType.setKID(type.getKID());
			newType.setLabel(type.getLabel());
			newType.setPackage(type.getPackage());
			newType.setPluralLabel(type.getPluralLabel());
			newType.setUniqueChecks(type.getUniqueChecks());*/
			
			// not using beanutils because this caused a failure in method setDefaultFieldId
			// which assumes fields are already set
			newType = (Type)org.apache.commons.beanutils.BeanUtils.cloneBean(type);
			
			// id is defined in class BasicModel, so cloneBean won't copy it
			newType.setId(type.getId());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new KommetException("Cannot clone type " + type.getQualifiedName() + ": " + e.getMessage() + ". Make sure the type class has default constructor.");
		}
		
		if (type.getFields() != null)
		{
			// clone fields - shallow copy
			for (Field field : type.getFields())
			{
				newType.addField(MiscUtils.cloneField(field));
			}
		}
		
		// unique checks don't need to be copied separately because they are a simple list
		// and are copied by the BeanUtils method
		
		newType.setDefaultFieldId(type.getDefaultFieldId());
		
		return newType;
	}

	public static List<String> getFieldNamesFromList (String commaSepFieldNames, Type type) throws KommetException
	{
		List<String> fieldApiNames = new ArrayList<String>();
		if (StringUtils.hasText(commaSepFieldNames))
		{
			String[] fieldNames = commaSepFieldNames.split(",");
			for (String fieldName : fieldNames)
			{
				// check if field exists on object
				if (type.getField(fieldName.trim()) == null)
				{
					throw new NoSuchFieldException("Field " + fieldName + " not found on object " + type.getQualifiedName());
				}
				fieldApiNames.add(fieldName.trim());
			}
		}
		else
		{
			for (Field field : type.getFields())
			{
				fieldApiNames.add(field.getApiName());
			}
		}
		
		return fieldApiNames;
	}
	
	public static String getHash(int length)
	{
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < length; i++)
		{
			int rand = random.nextInt(3);
			switch (rand)
			{
				// a digit
				case 0: sb.append((char)(random.nextInt(10) + 48)); break;
				// capital letter
				case 1: sb.append((char)(random.nextInt(25) + 65)); break;
				// small letter
				case 2: sb.append((char)(random.nextInt(25) + 97)); break;
			}
		}
		
		return sb.toString();
	}
	
	public static boolean isValidPackageName (String packageName)
	{
		return !packageName.startsWith("package.") && !packageName.endsWith(".package") && !packageName.contains(".package.");
	}
	
	public static String getPropertyFromGetter (String method) throws KommetException
	{
		if (!method.startsWith("get"))
		{
			throw new KommetException("Getter method '" + method + "' does not start with 'get'");
		}
		
		return org.springframework.util.StringUtils.uncapitalize(method.substring(3));
	}
	
	public static String getPropertyFromSetter (String method) throws KommetException
	{
		if (!method.startsWith("set"))
		{
			throw new KommetException("Setter method '" + method + "' does not start with 'set'");
		}
		
		return org.springframework.util.StringUtils.uncapitalize(method.substring(3));
	}
	
	public static <T extends BasicModel<Long>> List<Long> getIDList(Collection<T> objs)
	{
		List<Long> ids = new ArrayList<Long>();
		for (T obj : objs)
		{
			ids.add(obj.getId());
		}
		return ids;
	}
	
	public static String getSHA1Password(String password)
	{
	    String sha1 = "";
	    try
	    {
	        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
	        crypt.reset();
	        crypt.update(password.getBytes("UTF-8"));
	        sha1 = byteToHex(crypt.digest());
	    }
	    catch(NoSuchAlgorithmException e)
	    {
	        e.printStackTrace();
	    }
	    catch(UnsupportedEncodingException e)
	    {
	        e.printStackTrace();
	    }
	    return sha1;
	}

	private static String byteToHex(final byte[] hash)
	{
	    Formatter formatter = new Formatter();
	    for (byte b : hash)
	    {
	        formatter.format("%02x", b);
	    }
	    String result = formatter.toString();
	    formatter.close();
	    return result;
	}

	public static String formatPostgresDateTime (Date date)
	{
		return formatPostgresDateTime(date, "GMT");
	}
	
	public static String formatPostgresDateTime (Date date, String timezone)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
		return sdf.format(date);
	}
	
	public static String formatPostgresDate (Date date)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}
	
	public static String formatDateTimeByUserLocale (Date date, AuthData authData) throws KommetException
	{
		return formatDateTimeByUserLocale(date, null, authData);
	}
	
	public static String formatDateByUserLocale (Date date, AuthData authData) throws KommetException
	{
		return formatDateByUserLocale(date, null, authData);
	}
	
	public static String formatDateByUserLocale (Date date, String format, AuthData authData) throws KommetException
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format != null ? format : DATE_FORMAT_YYYY_MM_DD);
		sdf.setTimeZone(TimeZone.getTimeZone((String)authData.getUser().getTimezone()));
		return sdf.format(date);
	}
	
	/**
	 * 
	 * @param date
	 * @param authData For now, this parameters is not used
	 * @return
	 * @throws KommetException 
	 */
	public static String formatDateTimeByUserLocale (Date date, String format, AuthData authData) throws KommetException
	{
		if (date == null)
		{
			return "";
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat(format != null ? format : DATE_TIME_FORMAT_DEFAULT);
		sdf.setTimeZone(TimeZone.getTimeZone((String)authData.getUser().getTimezone()));
		return sdf.format(date);
	}
	
	public static String formatGMTDate (Date date, String format)
	{
		if (date == null)
		{
			return "";
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat(format != null ? format : DATE_TIME_FORMAT_DEFAULT);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}

	public static String padLeft (String str, int desiredLength, char padChar)
	{
		String result = str;
		while (result.length() < desiredLength)
		{
			result = padChar + result;
		}
		
		return result;
	}
	
	public static <T> String implode(Collection<T> items, String delimeter)
	{
		return implode(items, delimeter, null, null);
	}
	
	public static <T> String implode(Set<T> items, String delimeter)
	{
		return implode(items, delimeter, null, null);
	}
	
	public static <T> String implode(Set<T> items, String delimeter, String wrapper)
	{
		ArrayList<T> list = new ArrayList<T>();
		list.addAll(items);
		return implode (list, delimeter, wrapper, null);
	}
	
	public static <T> String implode(Set<T> items, String delimeter, String wrapper, String pre)
	{
		ArrayList<T> list = new ArrayList<T>();
		list.addAll(items);
		return implode (list, delimeter, wrapper, pre);
	}
	
	public static <T> String implode(Collection<T> items, String delimeter, String wrapper)
	{
		return implode(items, delimeter, wrapper, null);
	}
	
	public static <T> String implode(Collection<T> items, String delimeter, String wrapper, String pre)
	{
		ArrayList<T> list = new ArrayList<T>();
		list.addAll(items);
		return implode (list, delimeter, wrapper, pre);
	}
	
	public static <T> String implode(List<T> items, String delimeter, String wrapper)
	{
		return implode(items, delimeter, wrapper, null);
	}
	
	public static <T> String implode(List<T> items, String delimeter, String wrapper, String pre)
	{
		StringBuilder sb = new StringBuilder();
		
		if (items != null && !items.isEmpty())
		{
			for (T item : items)
			{
				sb.append(wrapper != null ? wrapper : "");
				sb.append(pre != null ? pre : "");
				sb.append(item);
				sb.append(wrapper != null ? wrapper : "");
				sb.append(delimeter != null ? delimeter : "");
			}
		}
		else
		{
			return items == null ? null : "";
		}
		
		return delimeter != null ? sb.toString().substring(0, sb.length() - delimeter.length()) : sb.toString();
	}

	public static String trimRight (String str, char c)
	{
		String escapedChar = String.valueOf(c);
		if (escapedChar.equals("(") || escapedChar.equals(")") || escapedChar.equals("."))
		{
			escapedChar = "\\" + escapedChar;
		}
		return str.replaceAll(escapedChar + "+$", "");
	}
	
	public static String trimLeft (String str, char c)
	{
		String escapedChar = String.valueOf(c);
		if (escapedChar.equals("(") || escapedChar.equals(")") || escapedChar.equals("."))
		{
			escapedChar = "\\" + escapedChar;
		}
		return str.replaceAll("^" + escapedChar + "+", "");
	}
	
	public static List<String> splitByWhitespaceAndQuote (String str)
	{	
		List<String> tokens = new ArrayList<String>();
		Character prevChar = null;
		Character currChar = null;
		String currWord = new String();
		boolean inQuotes = false;
		
		for (int i = 0; i < str.length(); i++)
		{
			prevChar = currChar;
			currChar = str.charAt(i);
			
			if (currChar == '\'')
			{
				if (prevChar == '\\')
				{
					currWord += currChar;
				}
				else
				{
					// encountered a non-escaped single quote
					currWord += currChar;
					
					if (inQuotes)
					{
						// if in quotes, close the quote and the word
						tokens.add(currWord);
						currWord = new String();
						inQuotes = false;
					}
					else
					{
						inQuotes = true;
					}
				}
			}
			else if (org.datanucleus.util.StringUtils.isWhitespace(String.valueOf(currChar)))
			{
				if (inQuotes)
				{
					currWord += currChar;
				}
				else
				{
					// end current word
					if (currWord.length() > 0)
					{
						tokens.add(currWord);
						currWord = new String();
					}
				}
			}
			else
			{
				currWord += currChar;
			}
		}
		
		return tokens;
	}
	
	public static String normalizeWhiteSpace (String str)
	{
		return str != null ? str.replaceAll("\\s+", " ") : null;
	}

	public static List<String> getPartialProperties (String nestedProperty, boolean skipLastOne)
	{
		if (nestedProperty == null)
		{
			return null;
		}
		String[] parts = nestedProperty.split("\\.");
		List<String> partialProperties = new ArrayList<String>();
		String currentProperty = "";
		
		int length = skipLastOne ? parts.length - 1 : parts.length;
		
		for (int i = 0; i < length; i++)
		{
			currentProperty += (currentProperty.equals("") ? "" : ".") + parts[i];
			partialProperties.add(currentProperty);
		}
		return partialProperties;
	}
	
	public static Date parseDateTime(String value, String format) throws ParseException
	{
		return parseDateTime(value, format, "GMT");
	}
	
	public static Date parseDateTime(String value, String format, String timezone) throws ParseException
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
		return sdf.parse(value);
	}
	
	public static Date parseDateTime(String value) throws ParseException
	{
		return parseDateTime(value, false);
	}

	public static Date parseDateTime(String value, boolean anyFormat) throws ParseException
	{
		try
		{
			return parseDateTime(value, DATE_TIME_FORMAT_FULL);
		}
		catch (ParseException e)
		{
			if (anyFormat)
			{
				try
				{
					// try different format
					return parseDateTime(value, DATE_TIME_FORMAT_DEFAULT);
				}
				catch (ParseException e1)
				{
					try
					{
						// try different format
						return parseDateTime(value, DATE_FORMAT_YYYY_MM_DD_HH_MM);
					}
					catch (ParseException e2)
					{
						return parseDateTime(value, DATE_FORMAT_YYYY_MM_DD);
					}
				}
			}
			else
			{
				throw e;
			}
		}
	}

	public static boolean isGetter (Method method)
	{
		return method.getName().startsWith("get");
	}
	
	public static String getSetterNameFromGetter (Method method) throws KommetException
	{
		if (method.getName().startsWith("get"))
		{
			return method.getName().replaceFirst("get", "set");
		}
		else
		{
			throw new KommetException("Method name + " + method.getName() + " is not a getter");
		}
	}

	public static Method getSetter (Method getterMethod) throws KommetException
	{
		String setterName = getSetterNameFromGetter(getterMethod);
		try
		{
			return getterMethod.getDeclaringClass().getMethod(setterName, getterMethod.getReturnType());
		}
		catch (Exception e)
		{
			throw new KommetException("Cannot get setter for getter " + getterMethod.getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Copies properties from source object to dest. The objects does not have to be of the same type or even
	 * members of the same type hierarchy. The only requirement is that all properties of type "source" exist
	 * on type "dest", have the same type and are writable.
	 * 
	 * @param source
	 * @param dest
	 * @param ... properties 
	 * @throws KommetException
	 */
	public static void copyProperties(Object source, Object dest) throws KommetException
	{
		List<Method> getters = getGetters(source.getClass());
		java.lang.Class<?> destClass = dest.getClass();
		
		for (Method getter : getters)
		{
			Method destGetter = null;
			try
			{
				destGetter = destClass.getMethod(getter.getName());
			}
			catch (SecurityException e)
			{
				throw new KommetException("Getter " + getter.getName() + " is not accessible on type " + destClass.getName());
			}
			catch (NoSuchMethodException e)
			{
				throw new KommetException("Getter " + getter.getName() + " exists on type " + source.getClass().getName() + " but not on type " + destClass.getName());
			}
			
			if (!destGetter.getReturnType().equals(getter.getReturnType()))
			{
				throw new KommetException("Getters " + getter.getName() + " have different return types on types " + destClass.getName() + " and " + source.getClass().getName());
			}
			
			Method destSetter = getSetter(destGetter);
			
			// call the setter on the destination object
			try
			{
				destSetter.invoke(dest, getter.invoke(source));
			}
			catch (Exception e)
			{
				throw new KommetException("Error setting property " + getter.getName().substring(3) + ": " + e.getMessage(), e);
			}
		}
	}

	private static List<Method> getGetters(java.lang.Class<?> cls)
	{
		List<Method> getters = new ArrayList<Method>();
		
		Method[] methods = cls.getDeclaredMethods();
		
		for (int i = 0; i < methods.length; i++)
		{
			if (methods[i].getName().startsWith("get"))
			{
				getters.add(methods[i]);
			}
		}
		
		return getters;
	}
	
	/**
	 * Converts the package name seen and defined by the user to the full package used by Raimme.
	 * 
	 * I.e. if user defines a class's package as "one.two", then the full package is kommet.envs.<envId>.one.two
	 * 
	 * @param userPackage
	 * @param env
	 * @return
	 */
	public static String userToEnvPackage (String userPackage, EnvData env)
	{
		return env.getEnv().getBasePackage() + (StringUtils.hasText(userPackage) ? "." + userPackage : "");
	}
	
	public static boolean isEnvSpecific (String name, EnvData env)
	{
		return name.startsWith(env.getEnv().getBasePackage());
	}
	
	public static String envToUserPackage (String fullPackage, EnvData env) throws KommetException
	{
		String envPrefix = env.getEnv().getBasePackage();
		if (!fullPackage.startsWith(envPrefix))
		{
			throw new KommetException("Invalid full package name " + fullPackage + ". Environment-specific packages should start with the package prefix: " + envPrefix);
		}
		else if (fullPackage.equals(envPrefix))
		{
			return "";
		}
		else
		{
			return fullPackage.substring(envPrefix.length() + 1);
		}
	}
	
	public static <T extends Record> List<KID> getKIDList (Collection<T> objs) throws KommetException
	{
		List<KID> ids = new ArrayList<KID>();
		for (T obj : objs)
		{
			ids.add(obj.getKID());
		}
		return ids;
	}
	
	public static <T extends RecordProxy> List<KID> getKIDListForProxies (Collection<T> objs) throws KommetException
	{
		List<KID> ids = new ArrayList<KID>();
		for (T obj : objs)
		{
			ids.add(obj.getId());
		}
		return ids;
	}

	public static Method getMethodByName(java.lang.Class<?> cls, String methodName)
	{
		for (Method m : cls.getMethods())
		{
			if (m.getName().equals(methodName))
			{
				return m;
			}
		}
		
		return null;
	}

	public static Object nullAsBlank (Object val)
	{
		return val != null ? val : "";
	}
	
	public static String blankAsNull (String s)
	{
		return StringUtils.hasText(s) ? s : null;
	}

	public static boolean isSetter(Method method)
	{
		return method.getName().startsWith("set") && method.getReturnType().equals(Void.TYPE) && method.getParameterTypes().length == 1;
	}

	public static String getPropertyNameFromSetter(Method method)
	{
		return StringUtils.uncapitalize(method.getName().substring(3));
	}
	
	public static List<String> splitByNewLine(String s)
	{
		if (s == null)
		{
			return null;
		}
		
		return Arrays.asList(s.split("\\r?\\n"));
	}

	public static List<String> splitAndTrim(String items, String delimeter)
	{
		List<String> splitItems = new ArrayList<String>();
		String[] bits = items.split(delimeter);
		
		for (int i = 0; i < bits.length; i++)
		{
			splitItems.add(bits[i].trim());
		}
		
		return splitItems;
	}

	public static Field cloneField(Field field) throws KommetException
	{
		Field newField = null;
		try
		{	
			// not using beanutils because this caused a failure in method setDefaultFieldId
			// which assumes fields are already set
			newField = (Field)org.apache.commons.beanutils.BeanUtils.cloneBean(field);
			
			// id is defined in class BasicModel, so cloneBean won't copy it
			newField.setId(field.getId());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new KommetException("Cannot clone field: " + e.getMessage());
		}
		
		return newField;
	}
	
	/**
	 * Returns an ArrayList from an enumeration of items.
	 * @param <T>
	 * @param items
	 * @return
	 */
	@SafeVarargs
	public static <T> List<T> toList(T ... items)
	{
		List<T> list = new ArrayList<T>();
		for (T item : items)
		{
			list.add(item);
		}
		return list;
	}
	
	public static <T extends RecordProxy> Map<KID, T> mapById (Collection<T> objs) throws KommetException
	{
		Map<KID, T> objsById = new HashMap<KID, T>();
		for (T obj : objs)
		{
			if (obj.getId() == null)
			{
				throw new KommetException("Cannot map object by null ID");
			}
			objsById.put(obj.getId(), obj);
		}
		
		return objsById;
	}
	
	@SafeVarargs
	public static <T> Set<T> toSet(T ... items)
	{
		Set<T> list = new HashSet<T>();
		for (T item : items)
		{
			list.add(item);
		}
		return list;
	}

	public static String scriptTag(String s)
	{
		return "<script language=\"Javascript\">" + s + "</script>";
	}
	
	public static String escapeHtmlId (String s)
	{
		return s != null ? s.replaceAll("\\:", "\\\\\\\\:") : null;
	}

	public static String escapePostgresString(String str)
	{
		return str.replaceAll("\\'", "''");
	}

	/**
	 * Sets hours, minutes and seconds of a date to 0.
	 * @param date
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static Date truncTimeInfo(Date date)
	{
		if (date != null)
		{
			Date truncDate = new Date(date.getTime());
			truncDate.setHours(0);
			truncDate.setMinutes(0);
			truncDate.setSeconds(0);
			return truncDate;
		}
		else
		{
			return null;
		}
	}

	public static Record shallowCloneRecord(Record record) throws KommetException
	{
		Record clone = new Record(record.getType());
		clone.setId(record.getId());
		for (String fieldName : record.getFieldValues().keySet())
		{
			clone.setField(fieldName, record.getField(fieldName));
		}
		return clone;
	}

	public static String getExceptionDesc(Exception e)
	{
		StringBuilder sb = new StringBuilder();
		
		if (e.getMessage() != null)
		{
			sb.append(e.getMessage()).append("\n");
		}
		
		sb.append("Stack trace:\n").append(getStackTrace(e));
		return sb.toString();
	}
	
	/**
	 * Create a string stack trace of the exception. The printed stack trace does not contain the
	 * exception message itself.
	 * @param e Exception for which stack trace is prepared
	 * @return
	 */
	public static String getStackTrace(Exception e)
	{
		StringBuilder sb = new StringBuilder();
		
		for (StackTraceElement elem : e.getStackTrace())
		{
			sb.append(elem.getClassName()).append(".").append(elem.getMethodName()).append("(").append(elem.getFileName()).append(":").append(elem.getLineNumber()).append(")\n");
		}
		
		return sb.toString();
	}

	public static String implode(String[] itemArr, String delimeter)
	{
		List<String> items = new ArrayList<String>();
		CollectionUtils.addAll(items, itemArr);
		return implode(items, delimeter);
	}

	public static Object getProperty(Object bean, String property) throws KommetException
	{
		if (property.contains("."))
		{
			// get first property from the nested property name
			bean = getProperty(bean, property.substring(0, property.indexOf(".")));
			return getProperty(bean, property.substring(property.indexOf(".") + 1));
		}
		
		Method getter = getGetter(bean.getClass(), property);
		if (getter != null)
		{
			try
			{
				return getter.invoke(bean);
			}
			catch (Exception e)
			{
				throw new KommetException("Error calling getter for property " + property + ". Nested: " + e.getMessage());
			}
		}
		else
		{
			throw new KommetException("No getter found for property " + property + " on class " + bean.getClass().getName());
		}
	}

	private static Method getGetter(java.lang.Class<? extends Object> cls, String property) throws KommetException
	{
		try
		{
			return cls.getMethod("get" + StringUtils.capitalize(property));
		}
		catch (SecurityException e)
		{
			throw new KommetException("Property getter inaccessible");
		}
		catch (NoSuchMethodException e)
		{
			throw new KommetException("No getter found for property " + property + " on class " + cls.getName());
		}
	}

	public static String trim (String str, char c)
	{
		return trimRight(trimLeft(str, c), c);
	}

	public static List<String> idListToStringList(List<KID> rids)
	{
		List<String> stringIds = new ArrayList<String>();
		for (KID id : rids)
		{
			stringIds.add(id.getId());
		}
		return stringIds;
	}
	
	public static LinkedHashMap<String, Object> getFileDirectoryMap (Collection<Class> files, Collection<View> views, Collection<ViewResource> viewResources, Collection<Layout> layouts, EnvData env)
	{
		LinkedHashMap<String, Object> mappedFiles = new LinkedHashMap<String, Object>();
		
		if (files != null)
		{
			for (Class file : files)
			{
				addItemToFileDirectorMap(file.getQualifiedName(), file, mappedFiles, 0);
			}
		}
		
		if (viewResources != null)
		{
			for (ViewResource resource : viewResources)
			{
				// prepend environment ID to the view resource name so it is properly handled by the directory tree
				addItemToFileDirectorMap(resource.getName(), resource, mappedFiles, 1);
			}
		}
		
		if (views != null)
		{
			for (View view : views)
			{
				addItemToFileDirectorMap(view.getQualifiedName(), view, mappedFiles, 0);
			}
		}
		
		if (layouts != null)
		{
			for (Layout layout : layouts)
			{
				addItemToFileDirectorMap(layout.getName(), layout, mappedFiles, 0);
			}
		}
		
		return mappedFiles;
	}
	
	/**
	 * Add an item to a directory map.
	 * @param qualifiedName
	 * @param file
	 * @param mappedFiles
	 * @param ignoreLastNDots The number of last dots in the qualified name that will not be used for splitting the name.
	 * This is useful when the name of the file contains an extension, e.g. "views.styles.css". In this case "styles.css" should
	 * be treated as a full file name and not be slit. To achieve this, we have to set ignoreLastNDots to 1.
	 */
	@SuppressWarnings("unchecked")
	private static void addItemToFileDirectorMap(String qualifiedName, Object file, LinkedHashMap<String, Object> mappedFiles, int ignoreLastNDots)
	{
		String[] nameBits = qualifiedName.split("\\.");
		
		if (nameBits.length > (ignoreLastNDots + 1))
		{
			if (!mappedFiles.containsKey(nameBits[0]))
			{
				mappedFiles.put(nameBits[0], new LinkedHashMap<String, Object>());
			}
			addItemToFileDirectorMap(qualifiedName.substring(qualifiedName.indexOf('.') + 1), file, (LinkedHashMap<String, Object>)mappedFiles.get(nameBits[0]), ignoreLastNDots);
		}
		else
		{
			mappedFiles.put(qualifiedName, file);
		}
	}

	/**
	 * Returns a method by its class and method name.
	 * @param classAndMethodName E.g. for "kommet.MiscUtils.formatDate"
	 * @return Method object, or null if method/class not found
	 * @throws KommetException
	 */
	public static Method getStaticMethod (String classAndMethodName, ClassLoader classLoader, java.lang.Class<?> ... args)
	{
		String className = classAndMethodName.substring(0, classAndMethodName.lastIndexOf("."));
		String methodName = classAndMethodName.substring(classAndMethodName.lastIndexOf(".") + 1);
		
		java.lang.Class<?> cls = null;
		try
		{
			if (classLoader == null)
			{
				cls = java.lang.Class.forName(className);
			}
			else
			{
				cls = classLoader.loadClass(className);
			}
		}
		catch (ClassNotFoundException e)
		{
			return null;
		}
		
		try
		{
			Method m = cls.getDeclaredMethod(methodName, args);
			if (!Modifier.isStatic(m.getModifiers()))
			{
				return null;
			}
			return m;
		}
		catch (NoSuchMethodException e)
		{
			return null;
		}
		catch (SecurityException e)
		{
			return null;
		}
	}

	public static List<String> splitByLastDot(String val) throws KommetException
	{
		if (!val.contains("."))
		{
			throw new KommetException("Value string does not contain a dot");
		}
		
		List<String> parts = new ArrayList<String>();
		parts.add(val.substring(0, val.lastIndexOf('.')));
		parts.add(val.substring(val.lastIndexOf('.') + 1));
		return parts;
	}

	public static String trimExtension(String s)
	{
		if (!StringUtils.hasText(s) || !s.contains("."))
		{
			return s;
		}
		
		return s.substring(0, s.lastIndexOf('.'));
	}

	public static boolean isEnvSpecific(String name)
	{
		return name.startsWith(Env.ENV_PACKAGE_PREFIX + ".");
	}

	public static String coalesce(String s1, String s2)
	{
		return StringUtils.hasText(s1) ? s1 : s2;
	}

	public static String indent(int indent)
	{
		String indentString = "";
		for (int i = 0; i < indent; i++)
		{
			indentString += "\t";
		}
		return indentString;
	}
}
