/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.security;

import kommet.data.KommetException;

/**
 * Exception thrown when access to a method is stopped by the {@link RestrictedAccessAspect}.
 * @author Radek Krawiec
 * @since 24/01/2015
 */
public class RestrictedAccessException extends KommetException
{
	private static final long serialVersionUID = 1788983531713214716L;

	public RestrictedAccessException(String msg)
	{
		super(msg);
	}
}