/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import kommet.data.KommetException;

public enum TagMode
{
	EDIT,
	VIEW,
	HIDDEN;

	public static TagMode fromString(String mode) throws KommetException
	{
		if ("edit".equals(mode))
		{
			return TagMode.EDIT;
		}
		else if ("view".equals(mode))
		{
			return TagMode.VIEW;
		}
		else if ("hidden".equals(mode))
		{
			return TagMode.HIDDEN;
		}
		else
		{
			throw new KommetException("Unrecognized tag mode " + mode);
		}
	}

	public String stringValue()
	{
		return this.name().toLowerCase();
	}
}
