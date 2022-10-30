/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.FILE_API_NAME)
public class File extends StandardTypeRecordProxy
{
	private ArrayList<FileRevision> revisions;
	private Boolean sealed;
	
	// public or restricted
	private String access;
	
	private String name;
	
	public static final String PUBLIC_ACCESS = "Public";
	public static final String RESTRICTED_ACCESS = "Restricted";
	
	public File() throws KommetException
	{
		this(null, null);
	}
	
	public File(Record file, EnvData env) throws KommetException
	{
		super(file, true, env);
	}

	public void setRevisions(ArrayList<FileRevision> revisions)
	{
		this.revisions = revisions;
		setInitialized();
	}

	@Property(field = "revisions")
	public ArrayList<FileRevision> getRevisions()
	{
		return revisions;
	}

	public void setSealed(Boolean sealed)
	{
		this.sealed = sealed;
		setInitialized();
	}

	@Property(field = "sealed")
	public Boolean getSealed()
	{
		return sealed;
	}

	public void setAccess(String access)
	{
		this.access = access;
		setInitialized();
	}

	@Property(field = "access")
	public String getAccess()
	{
		return access;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}
	
	@Transient
	public FileRevision getLatestRevision() throws KommetException
	{
		if (this.revisions != null && !this.revisions.isEmpty())
		{
			// TODO this needs to be fixed because it assumes revisions are ordered
			// perhaps we should introduce another collection "orderedRevisions"
			return this.revisions.get(0);
		}
		else
		{
			throw new KommetException("Revisions on file not initialized");
		}
	}
}