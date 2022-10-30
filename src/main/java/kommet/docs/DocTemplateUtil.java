/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.docs;

import java.util.Map;

public class DocTemplateUtil
{
	public static String interprete (String rawContent, Map<String, Object> values)
	{
		String interpretedContent = new String(rawContent);
		
		for (String key : values.keySet())
		{
			Object val = values.get(key);
			interpretedContent = interpretedContent.replaceAll("\\{\\{" + key + "\\}\\}", val != null ? val.toString() : "");
		}
		
		return interpretedContent;
	}
}