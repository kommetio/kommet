/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.i18n;

public enum Locale
{
	PL_PL(1),
	EN_US(0);
	
	private int id;
	
	private Locale (int id)
	{
		this.id = id;
	}
	
	public String getLanguage()
	{
		switch (this)
		{
			case EN_US: return "English";
			case PL_PL: return "Polski";
			default: return null;
		}
	}
	
	public int getId()
	{
		return this.id;
	}
}