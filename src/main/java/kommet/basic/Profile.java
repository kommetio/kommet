/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.PROFILE_API_NAME)
public class Profile extends StandardTypeRecordProxy implements Deployable
{
	public static final String ROOT_NAME = "Root";
	public static final String ROOT_ID = KID.PROFILE_PREFIX + "0000000001";
	public static final String SYSTEM_ADMINISTRATOR_LABEL = "System Administrator";
	public static final String SYSTEM_ADMINISTRATOR_NAME = "SystemAdministrator";
	public static final String SYSTEM_ADMINISTRATOR_ID = KID.PROFILE_PREFIX + "0000000002";
	public static final String UNAUTHENTICATED_NAME = "Unauthenticated";
	public static final String UNAUTHENTICATED_ID = KID.PROFILE_PREFIX + "0000000003";
	
	private String name;
	private String label;
	private Boolean systemProfile;
	
	public Profile() throws RecordProxyException
	{
		super(null, true, null);
	}
	
	public Profile(Record profile, EnvData env) throws KommetException
	{
		this(profile, false, env);
	}
	
	public Profile(Record profileRecord, boolean ignoreUnfetchedFields, EnvData env) throws KommetException
	{
		super(profileRecord, true, env);
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

	public void setSystemProfile(Boolean systemProfile)
	{
		this.systemProfile = systemProfile;
		setInitialized();
	}

	@Property(field = "systemProfile")
	public Boolean getSystemProfile()
	{
		return systemProfile;
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.PROFILE;
	}

	@Property(field = "label")
	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
		setInitialized();
	}
}