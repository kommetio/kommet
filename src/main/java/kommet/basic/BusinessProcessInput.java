/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.BUSINESS_PROCESS_INPUT_API_NAME)
public class BusinessProcessInput extends StandardTypeRecordProxy
{
	private String name;
	private String description;
	private BusinessProcess businessProcess;
	private BusinessAction businessAction;
	private String dataTypeName;
	private KID dataTypeId;
	
	public BusinessProcessInput() throws KommetException
	{
		this(null, null);
	}
	
	public BusinessProcessInput(Record r, EnvData env) throws KommetException
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

	@Property(field = "description")
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
		setInitialized();
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

	@Property(field = "businessAction")
	public BusinessAction getBusinessAction()
	{
		return businessAction;
	}

	public void setBusinessAction(BusinessAction businessAction)
	{
		this.businessAction = businessAction;
		setInitialized();
	}

	@Property(field = "dataTypeName")
	public String getDataTypeName()
	{
		return dataTypeName;
	}

	public void setDataTypeName(String dataTypeName)
	{
		this.dataTypeName = dataTypeName;
		setInitialized();
	}

	@Property(field = "dataTypeId")
	public KID getDataTypeId()
	{
		return dataTypeId;
	}

	public void setDataTypeId(KID dataTypeId)
	{
		this.dataTypeId = dataTypeId;
		setInitialized();
	}
	
	/**
	 * Tells if this parameter data type is a record proxy or its subclass
	 * @return
	 */
	@Transient
	public boolean isRecordProxy()
	{
		return this.dataTypeId != null || RecordProxy.class.getName().equals(this.dataTypeName);
	}
}