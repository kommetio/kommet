/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import kommet.data.KommetException;

/**
 * Exception thrown when a field has incorrect definition, e.g. incorrect API name, too long description etc.
 * @author Radek Krawiec
 * @since 25/04/2015
 */
public class FieldDefinitionException extends KommetException
{
	private static final long serialVersionUID = -5197563369412223212L;

	public FieldDefinitionException(String msg)
	{
		super(msg);
	}
}