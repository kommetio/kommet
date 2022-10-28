/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rel;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;
import kommet.web.RequestAttributes;

public class PageDataVarTranslator implements RelVarTranslator
{
	/**
	 * Translated a REL variable into an object proxy.
	 * @param variable
	 * @param recordVar Record variable to be prefixed to the variable.
	 * @return
	 * @throws KommetException 
	 */
	public TranslatedVar translateVar(String variable, String recordVar, boolean isCheckFields, Type type, EnvData env) throws KommetException
	{	
		// parse strings
		if (variable.startsWith("'") && variable.endsWith("'"))
		{
			return new TranslatedVar("\"" + variable.substring(1, variable.length() - 1) + "\"", false);
		}
		
		// parse null literals
		if ("null".equals(variable))
		{
			return new TranslatedVar("null", false);
		}
		
		if (isCheckFields)
		{
			if (type == null)
			{
				throw new KommetException("When REL parser checks field correctness, record type should be passed to the parser");
			}
			
			if (type.getField(variable, env) == null)
			{
				throw new RELSyntaxException("Field " + variable + " does not exist on type " + type.getQualifiedName());
			}
		}
		
		StringBuilder sb = new StringBuilder();
		if (StringUtils.hasText(recordVar))
		{
			throw new KommetException(this.getClass().getSimpleName() + " does not take recordVar as parameter - the argument should be null");
		}
		
		// split by dot
		String[] properties = variable.split("\\.");
		
		// translate the first property to pageData reference
		sb.append(RequestAttributes.PAGE_DATA_ATTR_NAME + ".getValue('" + properties[0] + "')");
		
		if (properties.length > 1)
		{
			sb.append(".");
			List<String> getters = new ArrayList<String>();
			
			// start iteration from the second property
			for (int i = 1; i < properties.length; i++)
			{
				// translate each property into a getter
				getters.add("get" + StringUtils.capitalize(properties[i]) + "()");
			}
			
			sb.append(MiscUtils.implode(getters, ".", null, null));
		}
		return new TranslatedVar(sb.toString(), true);
	}
}