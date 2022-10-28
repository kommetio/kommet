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
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.FIELD_HISTORY_API_NAME)
public class FieldHistory extends StandardTypeRecordProxy
{
	private KID recordId;
	private KID fieldId;
	private String oldValue;
	private String newValue;
	private String operation;
	
	public FieldHistory() throws KommetException
	{
		this(null, null);
	}
	
	public FieldHistory (Record fh, EnvData env) throws KommetException
	{
		super(fh, true, env);
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
		setInitialized();
	}

	@Property(field = "recordId")
	public KID getRecordId()
	{
		return recordId;
	}

	public void setFieldId(KID fieldId)
	{
		this.fieldId = fieldId;
		setInitialized();
	}

	@Property(field = "fieldId")
	public KID getFieldId()
	{
		return fieldId;
	}

	public void setOldValue(String oldValue)
	{
		this.oldValue = oldValue;
		setInitialized();
	}

	@Property(field = "oldValue")
	public String getOldValue()
	{
		return oldValue;
	}

	public void setNewValue(String newValue)
	{
		this.newValue = newValue;
		setInitialized();
	}

	@Property(field = "newValue")
	public String getNewValue()
	{
		return newValue;
	}

	public void setOperation(String operation)
	{
		this.operation = operation;
		setInitialized();
	}

	@Property(field = "operation")
	public String getOperation()
	{
		return operation;
	}
}