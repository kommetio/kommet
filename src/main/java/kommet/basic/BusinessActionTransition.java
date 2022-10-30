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

/**
 * Describes a transitions from one action invocation to another in a business process. Each action invocation can
 * have multiple transitions (both as next action and as previous action), but there can be only one transition for a
 * given next action and previous action pair.
 * 
 * @author Radek Krawiec
 * @since 19/07/2016
 */
@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.BUSINESS_ACTION_TRANSITION_API_NAME)
public class BusinessActionTransition extends StandardTypeRecordProxy
{
	private BusinessProcess businessProcess;
	private BusinessActionInvocation previousAction;
	private BusinessActionInvocation nextAction;
	
	public BusinessActionTransition() throws KommetException
	{
		this(null, null);
	}
	
	public BusinessActionTransition(Record r, EnvData env) throws KommetException
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

	@Property(field = "previousAction")
	public BusinessActionInvocation getPreviousAction()
	{
		return previousAction;
	}

	public void setPreviousAction(BusinessActionInvocation previousAction)
	{
		this.previousAction = previousAction;
		setInitialized();
	}

	@Property(field = "nextAction")
	public BusinessActionInvocation getNextAction()
	{
		return nextAction;
	}

	public void setNextAction(BusinessActionInvocation nextAction)
	{
		this.nextAction = nextAction;
		setInitialized();
	}
}