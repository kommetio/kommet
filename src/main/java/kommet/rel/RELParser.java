/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

public class RELParser
{
	private static Map<String, String> functionNames;
	private static Set<String> unalteredTokens;
	private static Set<Character> singleCharOperators;
	private static Map<String, String> operatorsToFunctions;
	
	static
	{
		functionNames = new HashMap<String, String>();
		functionNames.put("isEmpty", RELFunctions.class.getName() + ".isEmpty");
		functionNames.put("length",  RELFunctions.class.getName() + ".getLength");
		functionNames.put("isNull", RELFunctions.class.getName() + ".isNull");
		functionNames.put("isNotNull",  RELFunctions.class.getName() + ".isNotNull");
		functionNames.put("!",  RELFunctions.class.getName() + ".not");
		
		unalteredTokens = new HashSet<String>();
		unalteredTokens.add("+");
		unalteredTokens.add("-");
		unalteredTokens.add("true");
		unalteredTokens.add("false");
		
		// this collection only contains single characters from which operators
		// consist, even though there are also multi-character operators such as '>='
		singleCharOperators = new HashSet<Character>();
		singleCharOperators.add('-');
		singleCharOperators.add('=');
		singleCharOperators.add('+');
		singleCharOperators.add('>');
		singleCharOperators.add('<');
		
		// list of operators that are translated to Java functions
		operatorsToFunctions = new HashMap<String, String>();
		operatorsToFunctions.put(">", RELFunctions.class.getName() + ".gt");
		operatorsToFunctions.put(">=", RELFunctions.class.getName() + ".ge");
		operatorsToFunctions.put("<", RELFunctions.class.getName() + ".lt");
		operatorsToFunctions.put("<=", RELFunctions.class.getName() + ".le");
		operatorsToFunctions.put("==", RELFunctions.class.getName() + ".eq");
		operatorsToFunctions.put("<>", "!" + RELFunctions.class.getName() + ".eq");
	}
	
	public static String relToJava (String relCode, String recordVar, Type type, boolean isCheckFields, boolean isTranslateOperators, EnvData env) throws KommetException
	{
		return relToJava(relCode, recordVar, type, true, isCheckFields, isTranslateOperators, env);
	}
	
	public static Set<String> getRecordFields (String relCode, EnvData env) throws KommetException
	{
		try
		{
			return translateRELEntity(MiscUtils.tokenize(relCode, '\'', null, singleCharOperators), 0, ParseExitCondition.ANY, null, null, true, false, true, new StandardRelVarTranslator(), env).getRecordFields();
		}
		catch (RELSyntaxException e)
		{
			throw new RELSyntaxException(e.getMessage() + ": " + relCode, e);
		}
	}
	
	public static String relToJava (String relCode, String recordVar, Type type, boolean isTranslateVars, boolean isCheckFields, boolean isTranslateOperators, EnvData env) throws KommetException
	{
		try
		{
			return relToJava(MiscUtils.tokenize(relCode, '\'', null, singleCharOperators), recordVar, type, isTranslateVars, isCheckFields, isTranslateOperators, env);
		}
		catch (RELSyntaxException e)
		{
			throw new RELSyntaxException(e.getMessage() + ": " + relCode, e);
		}
	}
	
	public static String relToJava (String relCode, String recordVar, Type type, boolean isTranslateVars, boolean isCheckFields, boolean isTranslateOperators, RelVarTranslator varTranslator, EnvData env) throws KommetException
	{
		try
		{
			return relToJava(MiscUtils.tokenize(relCode, '\'', null, singleCharOperators), recordVar, type, isTranslateVars, isCheckFields, isTranslateOperators, varTranslator, env);
		}
		catch (RELSyntaxException e)
		{
			throw new RELSyntaxException(e.getMessage() + ": " + relCode, e);
		}
	}

	/**
	 * Parses a REL condition into a Java condition.
	 * @param tokens List of tokens to be translated.
	 * @param recordVar The name of the variable containing the current record. If null, it will be ignored.
	 * @return
	 * @throws KommetException 
	 */
	public static String relToJava (List<String> tokens, String recordVar, Type type, boolean isTranslateVars, boolean isCheckFields, boolean isTranslateOperators, EnvData env) throws KommetException
	{
		return translateRELEntity(tokens, 0, ParseExitCondition.ANY, recordVar, type, isTranslateVars, isCheckFields, isTranslateOperators, new StandardRelVarTranslator(), env).getCode();
	}
	
	private static String relToJava (List<String> tokens, String recordVar, Type type, boolean isTranslateVars, boolean isCheckFields, boolean isTranslateOperators, RelVarTranslator varTranslator, EnvData env) throws KommetException
	{
		return translateRELEntity(tokens, 0, ParseExitCondition.ANY, recordVar, type, isTranslateVars, isCheckFields, isTranslateOperators, varTranslator, env).getCode();
	}

	/**
	 * 
	 * @param tokens
	 * @param startIndex
	 * @param exitCondition Tells whether parse process should stop when all brackets are closed, or when a complete entity is parsed
	 * @param recordVar Variable name under which the current record is available
	 * @param isCheckFields 
	 * @return
	 * @throws KommetException 
	 */
	private static RELParseResult translateRELEntity (List<String> tokens, int startIndex, ParseExitCondition exitCondition, String recordVar, Type type, boolean isTranslateVars, boolean isCheckFields, boolean isTranslateOperators, RelVarTranslator varTranslator, EnvData env) throws KommetException
	{
		// tokens parsed so far by this method
		List<String> parsedTokens = new ArrayList<String>();
		Set<String> recordFields = new HashSet<String>();
		String incompleteOperatorFuncCall = null;
		int index = startIndex;
		String token = null;
		
		// tells whether a portion of tokens that constitute a complete entity has finished
		boolean isEntityEnd = false;
		
		for (; index < tokens.size(); index++)
		{
			// get current token
			token = tokens.get(index);
			
			if ("(".equals(token))
			{
				RELParseResult res = translateRELEntity(tokens, index + 1, ParseExitCondition.BRACKET_END, recordVar, type, isTranslateVars, isCheckFields, isTranslateOperators, varTranslator, env); 
				recordFields.addAll(res.getRecordFields());
				
				if (incompleteOperatorFuncCall != null)
				{
					// append the current token as a second parameter of a started function call
					parsedTokens.add(incompleteOperatorFuncCall + token + res.getCode() + ")");
					incompleteOperatorFuncCall = null;
				}
				else
				{
					parsedTokens.add(token + res.getCode());
				}
				
				// move the pointer to where the inner method finished
				index = res.getTokenIndex();
				isEntityEnd = true;
			}
			else if (")".equals(token))
			{
				// append bracket to last token
				String prevToken = parsedTokens.remove(parsedTokens.size() - 1);
				parsedTokens.add(prevToken + token);
				
				RELParseResult res = new RELParseResult();
				res.setCode(MiscUtils.implode(parsedTokens, " ", null, null));
				res.setTokenIndex(index);
				res.setRecordFields(recordFields);
				return res;
			}
			else if (incompleteOperatorFuncCall != null)
			{
				RELParseResult res = translateRELEntity(tokens, index, ParseExitCondition.ENTITY_END, recordVar, type, isTranslateVars, isCheckFields, isTranslateOperators, varTranslator, env);
				
				// append the next token as a second parameter of a started function call
				parsedTokens.add(incompleteOperatorFuncCall + res.getCode() + ")");
				recordFields.addAll(res.getRecordFields());
				incompleteOperatorFuncCall = null;
				index = res.getTokenIndex();
				isEntityEnd = true;
			}
			else if (isUnalteredToken(token) || (!isTranslateOperators && (singleCharOperators.contains(token) || operatorsToFunctions.keySet().contains(token))))
			{
				// rewrite some tokens literally
				parsedTokens.add(token);
				isEntityEnd = true;
			}
			else if (operatorsToFunctions.containsKey(token))
			{
				// turn the binary operator to a function call
				String prevToken = parsedTokens.remove(parsedTokens.size() - 1);
				incompleteOperatorFuncCall = operatorsToFunctions.get(token) + "(" + prevToken + ", ";
			}
			else if ("or".equals(token.toLowerCase()))
			{
				parsedTokens.add("||");
			}
			else if ("and".equals(token.toLowerCase()))
			{
				parsedTokens.add("&&");
			}
			else if (isFunction(token))
			{
				// make sure the next token after the function name is a bracket
				if (!"(".equals(tokens.get(index + 1)))
				{
					throw new RELSyntaxException("Function name " + token + " should be followed by a left bracket");
				}
				
				// get the bracket after the function call
				// since current index points to the token containing function name, the next token will
				// be a left bracket, so we want to start with index + 2, which is the first of the function args
				RELParseResult res = translateRELEntity(tokens, index + 2, ParseExitCondition.BRACKET_END, recordVar, type, isTranslateVars, isCheckFields, isTranslateOperators, varTranslator, env);
				parsedTokens.add(translateFunction(token) + "(" + res.getCode());
				recordFields.addAll(res.getRecordFields());
				index = res.getTokenIndex();
				isEntityEnd = true;
			}
			else
			{
				if (isTranslateVars)
				{
					TranslatedVar var = varTranslator.translateVar(token, recordVar, isCheckFields, type, env);
					parsedTokens.add(var.getVar());
					
					if (var.isField())
					{
						// the field name is the unaltered token
						recordFields.add(token);
					}
				}
				else
				{
					parsedTokens.add(token);
				}
			
				isEntityEnd = true;
			}
			
			if (exitCondition.equals(ParseExitCondition.ENTITY_END) && isEntityEnd)
			{
				RELParseResult res = new RELParseResult();
				res.setCode(MiscUtils.implode(parsedTokens, " ", null, null));
				res.setTokenIndex(index);
				res.setRecordFields(recordFields);
				return res;
			}
		}
		
		// if parsing was within brackets, it should have exited earlier,
		// when closing bracket was encountered
		if (exitCondition.equals(ParseExitCondition.BRACKET_END))
		{
			throw new RELSyntaxException("REL expression contains unterminated brackets");
		}
		
		RELParseResult res = new RELParseResult();
		res.setCode(MiscUtils.implode(parsedTokens, " ", null, null));
		res.setTokenIndex(index);
		res.setRecordFields(recordFields);
		return res;
	}

	/**
	 * Translates a REL function into its Java implementation.
	 * @param token
	 * @return
	 */
	private static String translateFunction(String token)
	{
		return functionNames.get(token);
	}

	/**
	 * Tells whether the given token represents a REL function.
	 * @param token
	 * @return
	 */
	private static boolean isFunction(String token)
	{
		return functionNames.containsKey(token);
	}

	/**
	 * Tells whether the given token does not need translation and can be directly rewritten to the Java code.
	 * @param token
	 * @return
	 */
	private static boolean isUnalteredToken(String token)
	{
		return unalteredTokens.contains(token) || isNumeric(token);// || (token.startsWith("'") && token.endsWith("'"));
	}

	/**
	 * Tells whether the given token is a numeric value (integer or double).
	 * @param token
	 * @return
	 */
	private static boolean isNumeric(String token)
	{
		try
		{
			Double.valueOf(token);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	enum ParseExitCondition
	{
		ANY,
		BRACKET_END,
		ENTITY_END
	}
}
