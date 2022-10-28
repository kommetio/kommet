/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.HashMap;
import java.util.Map;

public class ProcessResult
{
	private Map<String, Object> outputValues = new HashMap<String, Object>();
	private boolean isSuccess;
	private boolean isPassedEntryPoint;

	public Map<String, Object> getOutputValues()
	{
		return outputValues;
	}

	public void setOutputValues(Map<String, Object> outputValues)
	{
		this.outputValues = outputValues;
	}

	public boolean isSuccess()
	{
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess)
	{
		this.isSuccess = isSuccess;
	}

	public boolean isPassedEntryPoint()
	{
		return isPassedEntryPoint;
	}

	public void setPassedEntryPoint(boolean isPassedEntryPoint)
	{
		this.isPassedEntryPoint = isPassedEntryPoint;
	}
}