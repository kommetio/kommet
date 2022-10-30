/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

/**
 * Exception thrown when a non-environment qualified name is encountered on some type. 
 * @author Radek Krawiec
 * @created 27-03-2014
 */
public class EnvSpecificTypeException extends KommetException
{
	private static final long serialVersionUID = 6410212644221212807L;
	private String typeName;

	public EnvSpecificTypeException(String msg, String typeName)
	{
		super(msg);
		this.typeName = typeName;
	}

	public String getTypeName()
	{
		return typeName;
	}

}