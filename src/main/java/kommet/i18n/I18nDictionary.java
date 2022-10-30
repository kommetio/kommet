/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.i18n;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class I18nDictionary
{
	private Locale locale;
	private Map<String, String> keys = new HashMap<String, String>();
	
	public I18nDictionary (Locale locale)
	{
		this.locale = locale;
	}
	
	public Locale getLocale()
	{
		return this.locale;
	}
	
	public Set<String> getKeys()
	{
		return this.keys != null ? this.keys.keySet() : new HashSet<String>();
	}
	
	public String get(String key)
	{
		return this.keys.get(key);
	}
	
	public void addKey(String key, String value)
	{
		this.keys.put(key, value);
	}
}