/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import kommet.data.ComponentType;
import kommet.data.KID;
import kommet.data.KommetException;

/**
 * Status of a file deployment. Stores information about whether the deployment succeeded
 * plus potential deployment error messages.
 * @author Radek Krawiec
 */
public class FileDeploymentStatus
{
	private boolean success;
	private String fileName;
	private List<String> errors;
	private ComponentType fileType;
	private KID deployedComponentId;
	private int errorType;
	private String definition;
	private List<RecordDeploymentStatus> recordStatuses;
	
	public static final int DEPLOYMENT_ERROR_TYPE_UNKNOWN = 0;
	public static final int DEPLOYMENT_ERROR_TYPE_DUPLICATE = 1;
	
	public FileDeploymentStatus (boolean isSuccess, String fileName, String definition, ComponentType type, String error, KID deployedComponentId) throws KommetException
	{
		this(isSuccess, fileName, definition, type, DEPLOYMENT_ERROR_TYPE_UNKNOWN, error, deployedComponentId);
	}
	
	public FileDeploymentStatus (boolean isSuccess, String fileName, String definition, ComponentType type, int errorType, String error, KID deployedComponentId) throws KommetException
	{
		this.success = isSuccess;
		this.fileName = fileName;
		this.fileType = type;
		this.deployedComponentId = deployedComponentId;
		this.definition = definition;
		
		if (isSuccess)
		{
			if (deployedComponentId == null)
			{
				throw new KommetException("No record ID given for successfully deployed component");
			}
		}
		else
		{
			if (!StringUtils.hasText(error))
			{
				throw new KommetException("No error message define for failed deployment for file " + fileName);
			}
			else
			{
				addError(error);
			}
		}
	}

	private void addError(String error) throws DeploymentException
	{
		if (error == null)
		{
			throw new DeploymentException("Cannot add null error message");
		}
		
		if (this.errors == null)
		{
			this.errors = new ArrayList<String>();
		}
		this.errors.add(error);
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}

	public boolean isSuccess()
	{
		return success;
	}

	public void setErrors(List<String> errors)
	{
		this.errors = errors;
	}

	public List<String> getErrors()
	{
		return errors;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public String getFileName()
	{
		return fileName;
	}

	public ComponentType getFileType()
	{
		return fileType;
	}

	public void setFileType(ComponentType fileType)
	{
		this.fileType = fileType;
	}

	public KID getDeployedComponentId()
	{
		return deployedComponentId;
	}
	
	public void setDeployedComponentId (KID id)
	{
		this.deployedComponentId = id;
	}

	public int getErrorType()
	{
		return errorType;
	}

	public String getDefinition()
	{
		return definition;
	}

	public List<RecordDeploymentStatus> getRecordStatuses()
	{
		return recordStatuses;
	}

	public void setRecordStatuses(List<RecordDeploymentStatus> recordStatuses)
	{
		this.recordStatuses = recordStatuses;
	}
	
	public void addRecordStatus (RecordDeploymentStatus status)
	{
		if (this.recordStatuses == null)
		{
			this.recordStatuses = new ArrayList<RecordDeploymentStatus>();
		}
		
		this.recordStatuses.add(status);
	}
}