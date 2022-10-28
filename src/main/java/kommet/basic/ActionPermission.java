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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.ACTION_PERMISSION_API_NAME)
public class ActionPermission extends Permission
{
	private Boolean read;
	private Profile profile;
	private Action action;
	
	public ActionPermission() throws KommetException
	{
		this(null, null);
	}
	
	public ActionPermission(Record permission, EnvData env) throws KommetException
	{
		super(permission, env);
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
	
	@Property(field = "profile")
	public Profile getProfile()
	{
		return profile;
	}

	public void setProfile(Profile profile)
	{
		this.profile = profile;
		setInitialized();
	}

	@Property(field = "action")
	public Action getAction()
	{
		return action;
	}

	public void setAction(Action action)
	{
		this.action = action;
		setInitialized();
	}
}
