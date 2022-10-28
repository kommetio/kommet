/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.basic.BusinessActionInvocationAttribute;
import kommet.basic.RecordProxyType;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class BusinessActionInvocationAttributeDao extends GenericDaoImpl<BusinessActionInvocationAttribute>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public BusinessActionInvocationAttributeDao()
	{
		super(RecordProxyType.STANDARD);
	}
}