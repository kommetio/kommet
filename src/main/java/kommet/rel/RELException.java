/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rel;

import kommet.data.KommetException;

public class RELException extends KommetException
{
	private static final long serialVersionUID = 3737414994827819509L;

	public RELException(String msg)
	{
		super(msg);
	}

	public RELException(String msg, RELException e)
	{
		super(msg, e);
	}
}