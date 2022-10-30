/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.rmparams.actions;

import kommet.web.rmparams.KmParamException;

public class ExecuteCode extends Action
{
	private String code;
	
	public ExecuteCode() throws KmParamException
	{
		super("js");
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getCode()
	{
		return code;
	}
}