/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import kommet.data.KommetException;

/**
 * Exception thrown when casting action parameters to their actual type fails.
 * @author Radek Krawiec
 * @since 28/11/2014
 */
public class ActionParamCastException extends KommetException
{
	private static final long serialVersionUID = 4712194116949423975L;
	private String paramName;
	private Class<?> destinationClass;
	private Class<?> actualClass;

	public ActionParamCastException(String msg, String paramName, Class<?> destinationClass, Class<?> actualClass)
	{
		super(msg);
		this.paramName = paramName;
		this.destinationClass = destinationClass;
		this.actualClass = actualClass;
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
