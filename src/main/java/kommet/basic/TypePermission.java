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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.TYPE_PERMISSION_API_NAME)
public class TypePermission extends Permission
{
	private KID typeId;
	private Boolean read;
	private Boolean edit;
	private Boolean delete;
	private Boolean create;
	private Boolean readAll;
	private Boolean editAll;
	private Boolean deleteAll;
	
	public TypePermission() throws KommetException
	{
		this(null, null);
	}
	
	public TypePermission (Record permission, EnvData env) throws KommetException
	{
		super(permission, env);
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

	public void setDelete(Boolean delete)
	{
		this.delete = delete;
		setInitialized();
	}

	@Property(field = "delete")
	public Boolean getDelete()
	{
		return delete;
	}

	public void setCreate(Boolean create)
	{
		this.create = create;
		setInitialized();
	}

	@Property(field = "create")
	public Boolean getCreate()
	{
		return create;
	}

	public void setReadAll(Boolean readAll)
	{
		this.readAll = readAll;
		setInitialized();
	}

	@Property(field = "readAll")
	public Boolean getReadAll()
	{
		return readAll;
	}

	public void setEditAll(Boolean editAll)
	{
		this.editAll = editAll;
		setInitialized();
	}

	@Property(field = "editAll")
	public Boolean getEditAll()
	{
		return editAll;
	}

	@Property(field = "deleteAll")
	public Boolean getDeleteAll()
	{
		return deleteAll;
	}

	public void setDeleteAll(Boolean deleteAll)
	{
		this.deleteAll = deleteAll;
		setInitialized();
	}
}