/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.dal;

import kommet.data.KommetException;

public class DALException extends KommetException
{
	private static final long serialVersionUID = 8934218192922524490L;

	public DALException(String msg)
	{
		super(msg);
	}
}