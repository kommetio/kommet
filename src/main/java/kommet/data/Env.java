/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.Date;


public class Env
{
	private String name;
	private KID rid;
	private Date created;
	private Record owner;
	private KID adminId;
	private Long id;
	
	public static final String ENV_PACKAGE_PREFIX = "kommet.envs";

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setCreated(Date created)
	{
		this.created = created;
	}

	public Date getCreated()
	{
		return created;
	}

	public void setOwner(Record owner)
	{
		this.owner = owner;
	}

	public Record getOwner()
	{
		return owner;
	}
	
	public String getDBName()
	{
		return "env" + getKID();
	}

	public void setKID(KID rid)
	{
		this.rid = rid;
	}

	public KID getKID()
	{
		return rid;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public Long getId()
	{
		return id;
	}
	
	public String getBasePackage()
	{
		// we need to add "env" to the ID of the env, because each part of a qualified name must start with a letter
		return ENV_PACKAGE_PREFIX + ".env" + this.rid;
	}

	public void setAdminId(KID adminId)
	{
		this.adminId = adminId;
	}

	public KID getAdminId()
	{
		return adminId;
	}
}