/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.permissions;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.TypePermission;
import kommet.data.KID;

public class FieldPermissionFilter extends PermissionFilter<TypePermission>
{
	private Set<KID> fieldIds;

	public void setFieldIds(Set<KID> fieldIds)
	{
		this.fieldIds = fieldIds;
	}

	public Set<KID> getFieldIds()
	{
		return fieldIds;
	}
	
	public void addFieldId (KID fieldId)
	{
		if (this.fieldIds == null)
		{
			this.fieldIds = new HashSet<KID>();
		}
		this.fieldIds.add(fieldId);
	}
}