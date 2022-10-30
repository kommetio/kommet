/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.persistence;

import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyType;
import kommet.integration.EnvPersistenceInterface;

public class CustomTypeRecordProxyDao<T extends RecordProxy> extends GenericDaoImpl<T>
{
	private Class<? extends RecordProxy> proxyClass;
	private EnvPersistenceInterface envPersistence;
	
	public CustomTypeRecordProxyDao (Class<? extends RecordProxy> proxyClass, EnvPersistenceInterface envPersistence)
	{
		super(RecordProxyType.CUSTOM);
		this.proxyClass = proxyClass;
		this.envPersistence = envPersistence;
	}

	public Class<? extends RecordProxy> getProxyClass()
	{
		return proxyClass;
	}

	@Override
	protected EnvPersistenceInterface getEnvCommunication()
	{
		return envPersistence;
	}
}