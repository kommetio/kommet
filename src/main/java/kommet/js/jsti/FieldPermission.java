/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.js.jsti;

public class FieldPermission
{
	private Boolean read;
	private Boolean edit;
	
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
}