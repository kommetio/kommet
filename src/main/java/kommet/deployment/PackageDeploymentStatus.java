/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

import java.util.ArrayList;
import java.util.List;

import kommet.basic.Library;

public class PackageDeploymentStatus
{
	private List<FileDeploymentStatus> fileStatuses;
	private boolean isSuccess;
	private String error;
	
	// if this packages has been deployed as a library, the library data is populated from the library.xml
	// metadata file and from information about each deployed item
	private Library library;
	
	public PackageDeploymentStatus(boolean isSuccess, List<FileDeploymentStatus> fileStatuses, Library deployedLibrary)
	{
		super();
		this.fileStatuses = fileStatuses;
		this.isSuccess = isSuccess;
		this.library = deployedLibrary;
	}

	public PackageDeploymentStatus(boolean isSuccess, String error, Library lib)
	{
		this.isSuccess = isSuccess;
		this.library = lib;
		this.error = error;
	}

	public List<FileDeploymentStatus> getFileStatuses()
	{
		return fileStatuses;
	}

	public void setFileStatuses(List<FileDeploymentStatus> fileStatuses)
	{
		this.fileStatuses = fileStatuses;
	}

	public boolean isSuccess()
	{
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess)
	{
		this.isSuccess = isSuccess;
	}
	
	/**
	 * Get printable summary of the deployment with all its errors.
	 * @return
	 */
	public String printStatus()
	{
		StringBuilder sb = new StringBuilder();
		
		if (isSuccess)
		{
			sb.append("Deployment of " + fileStatuses.size() + " components successful");
		}
		else
		{
			sb.append("Deployment of " + fileStatuses.size() + " components failed\n");
			int i = 1;
			
			for (FileDeploymentStatus fileStatus : getFailedStatuses())
			{	
				sb.append(i).append(". " + fileStatus.getFileName()).append("\n");
				
				for (String err : fileStatus.getErrors())
				{
					sb.append(err).append("\n");
				}
				
				i++;
			}
		}
		
		return sb.toString();
	}

	/**
	 * Returns a list of failed deployment statuses.
	 * @return
	 */
	public List<FileDeploymentStatus> getFailedStatuses()
	{
		List<FileDeploymentStatus> failedStatuses = new ArrayList<FileDeploymentStatus>();
		for (FileDeploymentStatus fileStatus : this.fileStatuses)
		{
			if (!fileStatus.isSuccess())
			{
				failedStatuses.add(fileStatus);
			}
		}
		
		return failedStatuses;
	}

	public Library getLibrary()
	{
		return library;
	}

	public void setLibrary(Library library)
	{
		this.library = library;
	}

	public String getError()
	{
		return error;
	}

	public void setError(String error)
	{
		this.error = error;
	}
}