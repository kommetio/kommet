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
public @interface ActionConfig
{
	/**
	 * Tells whether empty string values should be treated as nulls
	 * when casting to parameters of different types than string.
	 * @return
	 */
	public boolean emptyParamAsNull() default true;
	
	/**
	 * If set to true, parameter cast errors will be injected into the action method.
	 * Otherwise, for any failing parameter cast an exception will be thrown.
	 */
	public boolean processParamCastErrors() default false;
}