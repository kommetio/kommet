/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

public class ConstraintViolationException extends KommetPersistenceException
{
	private static final long serialVersionUID = -8029204835619495478L;

	public ConstraintViolationException(String msg)
	{
		super(msg);
	}
}