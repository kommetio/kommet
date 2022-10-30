/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.labels;

import java.util.HashMap;
import java.util.Map;

import kommet.basic.TextLabel;
import kommet.i18n.I18nDictionary;
import kommet.i18n.Locale;

public class TextLabelDictionary
{
	// locale-specific dictionaries
	private Map<Locale, I18nDictionary> dictionaries;
	
	// non locale-specific dictionary (common for all locales)
	private I18nDictionary commonDictionary;
	private long lastSynchronized;
	
	public TextLabelDictionary()
	{
		this.dictionaries = new HashMap<Locale, I18nDictionary>();
		this.commonDictionary = new I18nDictionary(null);
	}

	public void setDictionaries(Map<Locale, I18nDictionary> dictionaries)
	{
		this.dictionaries = dictionaries;
	}

	public Map<Locale, I18nDictionary> getDictionaries()
	{
		return dictionaries;
	}

	public void setLastSynchronized(long lastSynchronized)
	{
		this.lastSynchronized = lastSynchronized;
	}

	public long getLastSynchronized()
	{
		return lastSynchronized;
	}

	public void setCommonDictionary(I18nDictionary commonDictionary)
	{
		this.commonDictionary = commonDictionary;
	}

	public I18nDictionary getCommonDictionary()
	{
		return commonDictionary;
	}

	public void addLabel(TextLabel label) throws TextLabelException
	{
		if (label == null)
		{
			throw new TextLabelException("Label added to dictionary is null");
		}
		
		if (label.getLocale() != null)
		{
			if (!this.dictionaries.containsKey(label.getLocaleAsEnum()))
			{
				this.dictionaries.put(label.getLocaleAsEnum(), new I18nDictionary(label.getLocaleAsEnum()));
			}
			
			this.dictionaries.get(label.getLocaleAsEnum()).addKey(label.getKey(), label.getValue());
		}
		else
		{
			this.commonDictionary.addKey(label.getKey(), label.getValue());
		}
	}

	/**
	 * Return a text label value for the given locale. If locale is null, a label value with locale = ALL will be returned.
	 * @param key Label key
	 * @param locale Locale for which label is returned, or null if we want to get label value common for all locales
	 * @return label value, or null if no label exists for the given key
	 */
	public String get(String key, Locale locale)
	{
		if (locale != null)
		{
			String value = this.dictionaries.containsKey(locale) ? this.dictionaries.get(locale).get(key) : null;
			
			// if value not found in locale-specific dictionary, use common dictionary
			return value != null ? value : this.commonDictionary.get(key);
		}
		else
		{
			return this.commonDictionary.get(key);
		}
	}
}