/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

import java.util.ArrayList;
import java.util.List;

import kommet.data.KID;

public class RecordDeploymentStatus
{
	private boolean success;
	private List<String> errors;
	private String xml;
	private KID recordId;
	
	public RecordDeploymentStatus (boolean isSuccess, String xml, String error, KID recordId) throws DeploymentException
	{
		this.success = isSuccess;
		addError(error);
		this.recordId = recordId;
	}

	public boolean isSuccess()
	{
		return success;
	}
	
	public void addError(String error) throws DeploymentException
	{
		if (error == null)
		{
			throw new DeploymentException("Cannot add null error message to record status");
		}
		
		if (this.errors == null)
		{
			this.errors = new ArrayList<String>();
		}
		this.errors.add(error);
	}

	public List<String> getErrors()
	{
		return errors;
	}

	public String getXml()
	{
		return xml;
	}

	public KID getRecordId()
	{
		return recordId;
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
	}
}