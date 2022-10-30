/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

public class FormulaParser
{
	private static final Map<String, String> operators;
	private static final Set<Character> operatorChars;
	public static final String FIELD_VAR_PREFIX = "field";
	
	static
	{
		operators = new HashMap<String, String>();
		operators.put("+", "||");
		
		operatorChars = new HashSet<Character>();
		operatorChars.add('+');
	}
	
	/**
	 * Parses formula definition created by the user to the field's inner representation.
	 * @param userDefinition
	 * @param type
	 * @return Formula definition's inner representation.
	 * @throws KommetException 
	 */
	public static String parseDefinition(FormulaReturnType returnType, String userDefinition, Type type, EnvData env) throws KommetException
	{
		if (!returnType.equals(FormulaReturnType.TEXT))
		{
			throw new KommetException("Unsupported formula return type " + returnType + " in formula parser");
		}
		
		List<String> tokens = MiscUtils.tokenize(userDefinition, '"', operatorChars, operatorChars);
		StringBuilder parsedDef = new StringBuilder();
		int i = 0;
		
		for (String token : tokens)
		{
			if (operators.containsKey(token))
			{
				// translate operator
				parsedDef.append(" ").append(operators.get(token)).append(" ");
			}
			else if ("(".equals(token))
			{
				checkVarTokenPlacement(token, tokens, i);
				
				// rewrite brackets
				parsedDef.append(" ").append(token);
			}
			else if (")".equals(token))
			{
				// rewrite brackets
				parsedDef.append(token).append(" ");
			}
			else if (token.startsWith("\"") && token.endsWith("\""))
			{
				checkVarTokenPlacement(token, tokens, i);
				
				// if token is a quoted string, rewrite it, but change quotes to Postgres (single) quotes
				parsedDef.append("'").append(token.substring(1, token.length() - 1)).append("'");
			}
			else if (token.startsWith("'"))
			{
				// if token is a quoted string, rewrite it, but change quotes to Postgres (single) quotes
				throw new FormulaSyntaxException("Single quotes are not allowed in formula definitions. Use double quotes instead.");
			}
			else
			{
				checkVarTokenPlacement(token, tokens, i);
				
				Field field = type.getField(token);
				if (field == null)
				{
					throw new FormulaSyntaxException("Unknown field " + token + " on type " + type.getQualifiedName());
				}
				
				if (!isFieldValidForFormulaReturnType(field, returnType))
				{
					throw new FormulaSyntaxException("Field " + field.getApiName() + " of type " + field.getDataType().getName() + " cannot be used in a formula with return type " + returnType.name().toLowerCase());
				}
				
				// if the token is not a bracket and not an operator, it must be a field name
				// so we will just translate the field name into its ID
				parsedDef.append(FIELD_VAR_PREFIX).append("{").append(field.getKID()).append("}");
			}
			
			i++;
		}
		
		return parsedDef.toString();
	}

	private static void checkVarTokenPlacement(String token, List<String> tokens, int i) throws FormulaSyntaxException
	{
		if (i == 0)
		{
			// if first token, do not check
			return;
		}
		
		String prevToken = tokens.get(i - 1);
		
		// brackets can only be placed in brackets or after operators
		if (!"(".equals(prevToken) && !operators.containsKey(prevToken))
		{
			throw new FormulaSyntaxException("Misplaced token " + token);
		}
	}

	/**
	 * Checks whether the given data type can be used in a formula with the given return type.
	 * @param field
	 * @param returnType
	 * @throws FormulaSyntaxException 
	 */
	private static boolean isFieldValidForFormulaReturnType(Field field, FormulaReturnType returnType) throws FormulaSyntaxException
	{
		DataType dt = field.getDataType();
		if (dt.getId().equals(DataType.FORMULA))
		{
			throw new FormulaSyntaxException("Field " + field.getApiName() + " cannot be used in formula field because it is a formula itself");
		}
		
		if (returnType.equals(FormulaReturnType.TEXT))
		{
			// only text fields are valid (for now) in a text formula
			return dt.getId().equals(DataType.TEXT);
		}
		else
		{
			throw new FormulaSyntaxException("Unsupported formula return type " + returnType);
		}
	}
	
	public static List<Field> getUsedFields(String parsedDefinition, Type type) throws KIDException
	{
		Pattern p = Pattern.compile(FIELD_VAR_PREFIX + "\\{([^\\}]+)\\}");
		Matcher m = p.matcher(parsedDefinition);
		
		List<Field> fields = new ArrayList<Field>();
		
		while (m.find())
		{
			fields.add(type.getField(KID.get(m.group(1))));
		}
	
		return fields;
	}

	/**
	 * Returns the SQL representation of the formula definition.
	 * @param parsedDefinition
	 * @return
	 * @throws KIDException 
	 */
	public static String getSQLFromParsedDefinition (String parsedDefinition, Type type, String alias) throws KIDException
	{
		//return parsedDefinition.replaceAll(FIELD_VAR_PREFIX + "\\{([^\\}]+)\\}", type.getField(KID.get("$1")).getDbColumn());
		Pattern p = Pattern.compile(FIELD_VAR_PREFIX + "\\{([^\\}]+)\\}");
		Matcher m = p.matcher(parsedDefinition);
		
		String actualAlias = alias != null ? alias + "." : "";
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			//parsedDefinition.
			m.appendReplacement(sb, "coalesce(" + actualAlias + type.getField(KID.get(m.group(1))).getDbColumn() + ",'')");
		}
		m.appendTail(sb);
		
		// We need to check the ID of the record as well. The reason is:
		// Consider a situation when we have type Employee with type reference to type Company.
		// There is a formula field on type Company called "address", defined as "street + ' ' + city".
		// Now consider an employee object with null reference to company (no company assigned).
		// The querying "select company.address from employee" would return ' ', because although
		// street and city are null, they are concatenated with a single space (' ').
		// This is why we need to make sure the whole object is not null, and this can be done by checking the ID
		StringBuilder clause = new StringBuilder("case when ");
		clause.append(actualAlias).append(Field.ID_FIELD_DB_COLUMN).append(" is null then null else ");
		clause.append(sb.toString()).append(" end");
		
		return clause.toString();
	}
	
	public static boolean isFormulaNonNullable(FormulaDataType formula, Type type) throws KIDException
	{
		List<Field> usedFields = FormulaParser.getUsedFields(formula.getParsedDefinition(), type);
		for (Field field : usedFields)
		{
			if (!field.isRequired())
			{
				return false;
			}
		}
		
		return true;
	}
}