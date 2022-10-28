/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "."
		+ SystemTypes.BUSINESS_PROCESS_PARAM_ASSIGNMENT_API_NAME)
public class BusinessProcessParamAssignment extends StandardTypeRecordProxy
{
	private BusinessProcess businessProcess;
	private BusinessActionInvocation sourceInvocation;
	private BusinessActionInvocation targetInvocation;
	private BusinessProcessOutput sourceParam;
	private BusinessProcessInput targetParam;
	private BusinessProcessInput processInput;
	private BusinessProcessOutput processOutput;

	public BusinessProcessParamAssignment() throws KommetException
	{
		this(null, null);
	}

	public BusinessProcessParamAssignment(Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
	}

	@Property(field = "businessProcess")
	public BusinessProcess getBusinessProcess()
	{
		return businessProcess;
	}

	public void setBusinessProcess(BusinessProcess businessProcess)
	{
		this.businessProcess = businessProcess;
		setInitialized();
	}

	@Property(field = "sourceInvocation")
	public BusinessActionInvocation getSourceInvocation()
	{
		return sourceInvocation;
	}

	public void setSourceInvocation(BusinessActionInvocation sourceInvocation)
	{
		this.sourceInvocation = sourceInvocation;
		setInitialized();
	}

	@Property(field = "targetInvocation")
	public BusinessActionInvocation getTargetInvocation()
	{
		return targetInvocation;
	}

	public void setTargetInvocation(BusinessActionInvocation targetInvocation)
	{
		this.targetInvocation = targetInvocation;
		setInitialized();
	}

	@Property(field = "sourceParam")
	public BusinessProcessOutput getSourceParam()
	{
		return sourceParam;
	}

	public void setSourceParam(BusinessProcessOutput sourceParam)
	{
		this.sourceParam = sourceParam;
		setInitialized();
	}

	@Property(field = "targetParam")
	public BusinessProcessInput getTargetParam()
	{
		return targetParam;
	}

	public void setTargetParam(BusinessProcessInput targetParam)
	{
		this.targetParam = targetParam;
		setInitialized();
	}

	@Property(field = "processInput")
	public BusinessProcessInput getProcessInput()
	{
		return processInput;
	}

	public void setProcessInput(BusinessProcessInput processInput)
	{
		this.processInput = processInput;
		setInitialized();
	}

	@Property(field = "processOutput")
	public BusinessProcessOutput getProcessOutput()
	{
		return processOutput;
	}

	public void setProcessOutput(BusinessProcessOutput processOutput)
	{
		this.processOutput = processOutput;
		setInitialized();
	}
}