/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.i18n.Locale;

public class FormulaDataType extends DataType
{
	private FormulaReturnType returnType;
	
	/**
	 * Formula definition as declared by the user, e.g. "if (name == "Mike") { return "Michael" }"
	 */
	private String userDefinition;
	
	/**
	 * Formula definition parsed to SQL form, e.g. CASE WHEN field{00200000100cjk} = 'Mike' THEN 'Michael' END
	 */
	private String parsedDefinition;
	
	public FormulaDataType(FormulaReturnType returnType, String userDefinition, Type type, EnvData env) throws KommetException
	{	
		super(FORMULA);
		
		if (type.getKID() == null)
		{
			throw new KommetException("Formula fields can only be created for saved types");
		}
		
		this.returnType = returnType;
		this.userDefinition = userDefinition;
		
		// parse user definition
		this.parsedDefinition = FormulaParser.parseDefinition(returnType, userDefinition, type, env);
	}

	public FormulaDataType()
	{
		super(FORMULA);
	}

	@Override
	public boolean isTransient()
	{
		return true;
	}
	
	public String getName()
	{
		return "Formula";
	}
	
	@Override
	public boolean isPrimitive()
	{
		return true;
	}
	
	@Override
	public String getPostgresValue(Object value) throws KommetException
	{
		throw new KommetException("Method Postgresql value is not supported for data type formula");
	}

	@Override
	public String getJavaType()
	{
		return this.returnType.getJavaType();
	}

	@Override
	public Object getJavaValue(Object value) throws KommetException
	{
		switch (this.returnType)
		{
			case TEXT: return (new TextDataType(1)).getJavaValue(value);
			case NUMBER: return (new NumberDataType(1, Double.class)).getJavaValue(value);
			case DATETIME: return (new DateTimeDataType()).getJavaValue(value);
			default: throw new KommetException("Unsupported formula return type: " + this.returnType);
		}
	}

	@Override
	public Object getJavaValue(String value) throws KommetException
	{
		return getJavaValue((Object)value);
	}

	@Override
	public String getStringValue(Object value, Locale locale) throws KommetException
	{
		switch (this.returnType)
		{
			case TEXT: return (new TextDataType(1)).getStringValue(value, locale);
			case NUMBER: return (new NumberDataType(1, Double.class)).getStringValue(value, locale);
			case DATETIME: return (new DateTimeDataType()).getStringValue(value, locale);
			default: throw new KommetException("Unsupported formula return type: " + this.returnType);
		}
	}

	public void setReturnType(FormulaReturnType returnType)
	{
		this.returnType = returnType;
	}

	public FormulaReturnType getReturnType()
	{
		return returnType;
	}

	public void setUserDefinition(String definition)
	{
		this.userDefinition = definition;
	}

	public String getUserDefinition()
	{
		return userDefinition;
	}

	public void setParsedDefinition(String parsedDefinition)
	{
		this.parsedDefinition = parsedDefinition;
	}

	public String getParsedDefinition()
	{
		return parsedDefinition;
	}
	
	@Override
	public boolean isCollection()
	{
		return false;
	}
}