/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import kommet.data.KID;
import kommet.data.KommetException;

public abstract class AuthHandler
{
	public static final String CHECK_METHOD_NAME = "check";

	/**
	 * If authentication successful, returns the ID of the authenticated user. Otherwise, returns null.
	 * @param token
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public abstract KID check (String token) throws KommetException;
	
}
