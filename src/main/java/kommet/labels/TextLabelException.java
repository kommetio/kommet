/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.labels;

import kommet.data.KommetException;

public class TextLabelException extends KommetException
{
	private static final long serialVersionUID = -8197463330733168863L;

	public TextLabelException(String msg)
	{
		super(msg);
	}
}