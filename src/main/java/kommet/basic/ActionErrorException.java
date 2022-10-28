/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic;

import java.util.ArrayList;
import java.util.List;

import kommet.data.KommetException;

public class ActionErrorException extends KommetException
{
	private static final long serialVersionUID = -1036632603385509897L;
	private List<String> errors;

	public ActionErrorException (String msg)
	{
		super(msg);
		this.errors = new ArrayList<String>();
		this.errors.add(msg);
	}
	
	public void addError(String msg)
	{
		this.errors.add(msg);
	}

	public List<String> getErrors()
	{
		return errors;
	}
}
