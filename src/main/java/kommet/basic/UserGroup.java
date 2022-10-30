/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;

import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.USER_GROUP_API_NAME)
public class UserGroup extends StandardTypeRecordProxy implements Deployable
{
	private String name;
	private ArrayList<User> users;
	private String description;
	
	public UserGroup() throws KommetException
	{
		this(null, null);
	}
	
	public UserGroup(Record rt, EnvData env) throws KommetException
	{
		super(rt, true, env);
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "users")
	public ArrayList<User> getUsers()
	{
		return users;
	}

	public void setUsers(ArrayList<User> users)
	{
		this.users = users;
		setInitialized();
	}

	@Property(field = "description")
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
		setInitialized();
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.USER_GROUP;
	}
}