/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dataimport.csv;

import kommet.data.KommetException;

public class CSVParserException extends KommetException
{
	private static final long serialVersionUID = -727644003638443875L;

	public CSVParserException (String msg)
	{
		super(msg);
	}
}