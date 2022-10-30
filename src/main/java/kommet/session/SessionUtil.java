/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.session;

import javax.servlet.http.HttpSession;

import kommet.data.Env;

public class SessionUtil
{
	private static final String USER_ENV_SESSION_KEY = "user-env";
	
	public static Env getUserEnv (HttpSession session)
	{
		return (Env)session.getAttribute(USER_ENV_SESSION_KEY);
	}
	
	public static void setUserEnv (Env env, HttpSession session)
	{
		session.setAttribute(USER_ENV_SESSION_KEY, env);
	}
}