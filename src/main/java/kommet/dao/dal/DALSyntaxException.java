/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.dal;


public class DALSyntaxException extends DALException
{
	private static final long serialVersionUID = 4561241981427970490L;

	public DALSyntaxException(String msg)
	{
		super(msg);
	}
}