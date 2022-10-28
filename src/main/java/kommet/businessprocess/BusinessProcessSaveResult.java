/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.ArrayList;
import java.util.List;

import kommet.basic.BusinessProcess;

public class BusinessProcessSaveResult
{
	private List<String> errors = new ArrayList<String>();
	private BusinessProcess process;
	private boolean isSuccess;
	
	public boolean isValid()
	{
		return this.errors.isEmpty();
	}
	
	public void addError (String err)
	{
		this.errors.add(err);
	}

	public List<String> getErrors()
	{
		return errors;
	}

	public boolean isSuccess()
	{
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess)
	{
		this.isSuccess = isSuccess;
	}

	public BusinessProcess getProcess()
	{
		return process;
	}

	public void setProcess(BusinessProcess process)
	{
		this.process = process;
	}
}