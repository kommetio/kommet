/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.files;

import kommet.data.KommetException;

/**
 * Exception thrown by file operations.
 * @author Radek Krawiec
 * @since 11/03/2015
 */
public class FileException extends KommetException
{
	private static final long serialVersionUID = 3151861279868256923L;

	public FileException(String msg)
	{
		super(msg);
	}
}