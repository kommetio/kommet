/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.kmparams.actions;

import kommet.web.kmparams.KmParamException;

public class SetField extends Action
{
	private String field;
	
	public SetField() throws KmParamException
	{
		super("set");
	}

	public void setField(String field)
	{
		this.field = field;
	}

	public String getField()
	{
		return field;
	}
	
}