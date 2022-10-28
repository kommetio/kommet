/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rel;

import kommet.data.KommetException;

public class RELSyntaxException extends KommetException
{
	private static final long serialVersionUID = 5221259974979650732L;

	public RELSyntaxException(String msg)
	{
		super(msg);
	}

	public RELSyntaxException(String msg, RELSyntaxException e)
	{
		super(msg, e);
	}
}