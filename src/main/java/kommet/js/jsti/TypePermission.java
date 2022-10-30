/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.js.jsti;

public class TypePermission
{
	private Boolean read;
	private Boolean edit;
	private Boolean delete;
	private Boolean create;
	private Boolean readAll;
	private Boolean editAll;
	private Boolean deleteAll;

	public Boolean getRead()
	{
		return read;
	}

	public void setRead(Boolean read)
	{
		this.read = read;
	}

	public Boolean getEdit()
	{
		return edit;
	}

	public void setEdit(Boolean edit)
	{
		this.edit = edit;
	}

	public Boolean getDelete()
	{
		return delete;
	}

	public void setDelete(Boolean delete)
	{
		this.delete = delete;
	}

	public Boolean getCreate()
	{
		return create;
	}

	public void setCreate(Boolean create)
	{
		this.create = create;
	}

	public Boolean getReadAll()
	{
		return readAll;
	}

	public void setReadAll(Boolean readAll)
	{
		this.readAll = readAll;
	}

	public Boolean getEditAll()
	{
		return editAll;
	}

	public void setEditAll(Boolean editAll)
	{
		this.editAll = editAll;
	}

	public Boolean getDeleteAll()
	{
		return deleteAll;
	}

	public void setDeleteAll(Boolean deleteAll)
	{
		this.deleteAll = deleteAll;
	}
}