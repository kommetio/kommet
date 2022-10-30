/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import kommet.data.KommetException;

/**
 * Exception thrown when rendering of a tag fails.
 * @author Radek Krawiec
 * @created 8-03-2014
 */
public class TagException extends KommetException
{
	private static final long serialVersionUID = -5378147398511763226L;
	private String tagClass;
	
	public TagException(String msg, String tagClass)
	{
		super(msg);
		this.tagClass = tagClass;
	}
	
	public String getTagClass()
	{
		return tagClass;
	}
}
