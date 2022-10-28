/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.actions;

import kommet.basic.keetle.ActionParamCastException;

/**
 * Error returned when an action parameter cannot be parsed to the requested
 * type.
 * 
 * @author Radek Krawiec
 * @since 09/04/2015
 */
public class ParamCastError
{
	private String paramName;
	private Class<?> destinationClass;
	private Class<?> actualClass;
	
	public ParamCastError (ActionParamCastException e)
	{
		this.paramName = e.getParamName();
		this.destinationClass = e.getDestinationClass();
		this.actualClass = e.getActualClass();
	}

	public String getParamName()
	{
		return paramName;
	}

	public Class<?> getDestinationClass()
	{
		return destinationClass;
	}


	public Class<?> getActualClass()
	{
		return actualClass;
	}
}