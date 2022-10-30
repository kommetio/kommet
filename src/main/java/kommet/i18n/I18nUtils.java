/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.i18n;

public class I18nUtils
{
	private static final String SYSTEM_LABEL_PREFIX = "label.";
	
	public static String getSystemFieldLabel (String fieldApiName, I18nDictionary i18n)
	{
		return i18n.get(SYSTEM_LABEL_PREFIX + fieldApiName);
	}
}