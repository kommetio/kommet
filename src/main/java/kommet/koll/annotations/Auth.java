/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth
{
	/**
	 * The qualified name of the class in KOLL code that handles the authentication and returns boolean.
	 * E.g. "com.myapp.AuthHandler.check"
	 * The method must implement kommet.auth.IAuthHandler
	 * @return
	 */
	public String handler();
	
	/**
	 * The name of the header that contains the authentication token
	 * @return
	 */
	public String header();
	
	/**
	 * Whether the authentication identity resulting from this custom authenticator
	 * should override other sources of authentication
	 * @return
	 */
	public boolean isOverride() default false;
}