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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.USER_GROUP_ASSIGNMENT_API_NAME)
public class UserGroupAssignment extends StandardTypeRecordProxy
{
	public UserGroupAssignment() throws KommetException
	{
		this(null, null);
	}
	
	public UserGroupAssignment(Record rt, EnvData env) throws KommetException
	{
		super(rt, true, env);
	}
	
	private UserGroup parentGroup;
	private UserGroup childGroup;
	private User childUser;
	private Boolean isApplyPending;
	private Boolean isRemovePending;
	
	@Property(field = "parentGroup")
	public UserGroup getParentGroup()
	{
		return parentGroup;
	}

	public void setParentGroup(UserGroup parentGroup)
	{
		this.parentGroup = parentGroup;
		setInitialized();
	}

	@Property(field = "childGroup")
	public UserGroup getChildGroup()
	{
		return childGroup;
	}

	public void setChildGroup(UserGroup childGroup)
	{
		this.childGroup = childGroup;
		setInitialized();
	}

	@Property(field = "childUser")
	public User getChildUser()
	{
		return childUser;
	}

	public void setChildUser(User childUser)
	{
		this.childUser = childUser;
		setInitialized();
	}

	@Property(field = "isApplyPending")
	public Boolean getIsApplyPending()
	{
		return isApplyPending;
	}

	public void setIsApplyPending(Boolean isApplyPending)
	{
		this.isApplyPending = isApplyPending;
		setInitialized();
	}
	
	@Property(field = "isRemovePending")
	public Boolean getIsRemovePending()
	{
		return isRemovePending;
	}

	public void setIsRemovePending(Boolean isRemovePending)
	{
		this.isRemovePending = isRemovePending;
		setInitialized();
	}
}