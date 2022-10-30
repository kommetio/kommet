/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import com.fasterxml.jackson.core.JsonProcessingException;

public class JcrSerializationException extends JsonProcessingException
{
	private static final long serialVersionUID = -8705142821744725107L;

	protected JcrSerializationException(String msg)
	{
		super(msg);
	}
}