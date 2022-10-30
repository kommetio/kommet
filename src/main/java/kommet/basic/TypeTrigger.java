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

/**
 * Represents a binding between a trigger and a type on which this trigger is executed.
 * @author Radek Krawiec
 */
@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.TYPE_TRIGGER_API_NAME)
public class TypeTrigger extends StandardTypeRecordProxy
{
	private KID typeId;
	private Class triggerFile;
	private Boolean isSystem;
	private Boolean isActive;
	private Boolean isBeforeInsert;
	private Boolean isBeforeUpdate;
	private Boolean isBeforeDelete;
	private Boolean isAfterInsert;
	private Boolean isAfterUpdate;
	private Boolean isAfterDelete;
	
	public TypeTrigger() throws KommetException
	{
		this(null, null);
	}
	
	public TypeTrigger (Record tt, EnvData env) throws KommetException
	{
		super(tt, true, env);
	}

	public void setIsSystem(Boolean isSystem)
	{
		this.isSystem = isSystem;
		setInitialized();
	}

	@Property(field = "isSystem")
	public Boolean getIsSystem()
	{
		return isSystem;
	}

	public void setIsActive(Boolean isActive)
	{
		this.isActive = isActive;
		setInitialized();
	}

	@Property(field = "isActive")
	public Boolean getIsActive()
	{
		return isActive;
	}

	public void setIsBeforeInsert(Boolean isBeforeInsert)
	{
		this.isBeforeInsert = isBeforeInsert;
		setInitialized();
	}

	@Property(field = "isBeforeInsert")
	public Boolean getIsBeforeInsert()
	{
		return isBeforeInsert;
	}

	public void setIsBeforeUpdate(Boolean isBeforeUpdate)
	{
		this.isBeforeUpdate = isBeforeUpdate;
		setInitialized();
	}

	@Property(field = "isBeforeUpdate")
	public Boolean getIsBeforeUpdate()
	{
		return isBeforeUpdate;
	}

	public void setIsBeforeDelete(Boolean isBeforeDelete)
	{
		this.isBeforeDelete = isBeforeDelete;
		setInitialized();
	}

	@Property(field = "isBeforeDelete")
	public Boolean getIsBeforeDelete()
	{
		return isBeforeDelete;
	}
	
	public void setIsAfterDelete(Boolean isAfterDelete)
	{
		this.isAfterDelete = isAfterDelete;
		setInitialized();
	}

	@Property(field = "isAfterDelete")
	public Boolean getIsAfterDelete()
	{
		return isAfterDelete;
	}

	public void setIsAfterInsert(Boolean isAfterInsert)
	{
		this.isAfterInsert = isAfterInsert;
		setInitialized();
	}

	@Property(field = "isAfterInsert")
	public Boolean getIsAfterInsert()
	{
		return isAfterInsert;
	}

	public void setIsAfterUpdate(Boolean isAfterUpdate)
	{
		this.isAfterUpdate = isAfterUpdate;
		setInitialized();
	}

	@Property(field = "isAfterUpdate")
	public Boolean getIsAfterUpdate()
	{
		return isAfterUpdate;
	}

	public void setTypeId(KID typeId)
	{
		this.typeId = typeId;
		setInitialized();
	}

	@Property(field = "typeId")
	public KID getTypeId()
	{
		return typeId;
	}

	public void setTriggerFile(Class triggerFile)
	{
		this.triggerFile = triggerFile;
		setInitialized();
	}

	@Property(field = "triggerFile")
	public Class getTriggerFile()
	{
		return triggerFile;
	}
}