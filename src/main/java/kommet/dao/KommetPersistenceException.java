/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import kommet.data.KommetException;

public class KommetPersistenceException extends KommetException
{
	private static final long serialVersionUID = -1132405994567706925L;

	public KommetPersistenceException(String msg)
	{
		super(msg);
	}

	public KommetPersistenceException(String msg, Exception e)
	{
		super(msg, e);
	}
}