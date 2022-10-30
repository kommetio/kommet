/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.labels;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.TextLabel;
import kommet.filters.BasicFilter;
import kommet.i18n.Locale;

public class TextLabelFilter extends BasicFilter<TextLabel>
{
	private Locale locale;
	private Set<String> keys;
	
	public void addKey (String key)
	{
		if (this.keys == null)
		{
			this.keys = new HashSet<String>();
		}
		this.keys.add(key);
	}

	public void setLocale(Locale locale)
	{
		this.locale = locale;
	}

	public Locale getLocale()
	{
		return locale;
	}

	public void setKeys(Set<String> keys)
	{
		this.keys = keys;
	}

	public Set<String> getKeys()
	{
		return keys;
	}
}