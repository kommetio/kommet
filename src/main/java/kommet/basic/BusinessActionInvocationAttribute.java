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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.BUSINESS_ACTION_INVOCATION_ATTRIBUTE_API_NAME)
public class BusinessActionInvocationAttribute extends StandardTypeRecordProxy
{
	private String name;
	private String value;
	private BusinessActionInvocation invocation;
	
	public BusinessActionInvocationAttribute() throws KommetException
	{
		this(null, null);
	}
	
	public BusinessActionInvocationAttribute(Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "value")
	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
		setInitialized();
	}

	@Property(field = "invocation")
	public BusinessActionInvocation getInvocation()
	{
		return invocation;
	}

	public void setInvocation(BusinessActionInvocation invocation)
	{
		this.invocation = invocation;
		setInitialized();
	}
}