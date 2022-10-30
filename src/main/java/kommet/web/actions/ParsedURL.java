/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import kommet.data.KommetException;
import kommet.utils.MiscUtils;

public class ParsedURL
{
	private String url;
	private List<String> parts;
	private static final Pattern URL_PATTERN = Pattern.compile("\\{([A-z0-9_]+)\\}");
	
	public ParsedURL (String url)
	{
		this.url = MiscUtils.trim(url, '/').trim();
		
		if (StringUtils.hasText(this.url))
		{
			this.parts = MiscUtils.splitAndTrim(this.url, "/");
		}
		else
		{
			this.parts = new ArrayList<String>();
		}
		
		/*for (String part : urlParts)
		{
			if (part.startsWith("{") && part.endsWith("}"))
			{
				
			}
		}
		
		/*Matcher m = URL_PATTERN.matcher(url);
		
		// parse URL using regex
		while (m.find())
		{
			String param = m.group(1);
		}*/
	}
	
	public boolean matchesParameterized (String comparedURL)
	{
		String urlRegex = this.url.replaceAll("\\{([A-z0-9_]+)\\}", "([^/]+)");
		String comparedUrlRegex = MiscUtils.trim(comparedURL.replaceAll("\\{([A-z0-9_]+)\\}", "([^/]+)"), '/').trim();
		
		return urlRegex.equals(comparedUrlRegex) || Pattern.compile(urlRegex).matcher(comparedURL).matches() || Pattern.compile(comparedUrlRegex).matcher(this.url).matches(); 
	}
	
	public boolean matches (String comparedURL)
	{
		String regex = this.url.replaceAll("\\{([A-z0-9_]+)\\}", "([^/]+)");
		Pattern pattern = Pattern.compile(regex);
		
		Matcher m = pattern.matcher(MiscUtils.trim(comparedURL, '/').trim());
		return m.matches();
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public List<String> getParts()
	{
		return parts;
	}

	public void setParts(List<String> parts)
	{
		this.parts = parts;
	}

	public Map<String, String> getParamValues(String actualURL, boolean verifyLength) throws KommetException
	{
		List<String> templateParts = MiscUtils.splitAndTrim(this.url, "/");
		List<String> actualParts = MiscUtils.splitAndTrim(MiscUtils.trim(actualURL, '/'), "/");
		
		if (verifyLength && templateParts.size() != actualParts.size())
		{
			throw new KommetException("Incompatible length of URLs: template " + this.url + " vs. actual " + actualURL);
		}
		
		Map<String, String> paramValues = new HashMap<String, String>();
		
		int i = 0;
		
		for (String templatePart : templateParts)
		{
			if (templatePart.startsWith("{") && templatePart.endsWith("}"))
			{
				paramValues.put(templatePart.substring(1, templatePart.length() - 1), actualParts.get(i));
			}
			
			i++;
		}
		
		return paramValues;
	}
}