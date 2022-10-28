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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.FIELD_PERMISSION_API_NAME)
public class FieldPermission extends Permission
{
	private KID fieldId;
	private Boolean read;
	private Boolean edit;
	
	public FieldPermission() throws KommetException
	{
		this(null, null);
	}
	
	public FieldPermission(Record permission, EnvData env) throws KommetException
	{
		super(permission, env);
		if (permission != null)
		{
			this.setFieldId((KID) permission.getField("fieldId"));
			this.read = (Boolean) permission.getField("read");
			this.edit = (Boolean) permission.getField("edit");
			this.id = permission.attemptGetKID();
		}
	}

	public void setRead(Boolean read)
	{
		this.read = read;
		setInitialized();
	}

	@Property(field = "read")
	public Boolean getRead()
	{
		return read;
	}

	public void setEdit(Boolean edit)
	{
		this.edit = edit;
		setInitialized();
	}

	@Property(field = "edit")
	public Boolean getEdit()
	{
		return edit;
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
}