/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Property;

public abstract class Permission extends StandardTypeRecordProxy
{
	private Profile profile;
	private PermissionSet permissionSet;
	
	public Permission() throws RecordProxyException
	{
		this(null, null);
	}
	
	public Permission(Record record, EnvData env) throws RecordProxyException
	{
		super(record, true, env);
	}

	public void setProfile(Profile profile)
	{
		this.profile = profile;
		setInitialized();
	}

	@Property(field = "profile")
	public Profile getProfile()
	{
		return profile;
	}

	public void setPermissionSet(PermissionSet permissionSet)
	{
		this.permissionSet = permissionSet;
		setInitialized();
	}

	@Property(field = "permissionSet")
	public PermissionSet getPermissionSet()
	{
		return permissionSet;
	}
}