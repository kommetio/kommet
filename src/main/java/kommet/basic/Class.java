/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.koll.InvalidClassCodeException;
import kommet.koll.KollUtil;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.CLASS_API_NAME)
public class Class extends StandardTypeRecordProxy implements Deployable
{
	private String javaCode;
	private String kollCode;
	
	/**
	 * Package is the full name of the controller package. It is different from the package name
	 * defined by the user and visible to them.
	 * 
	 * E.g. if a user defines the package as "com.pigeons.test", then the actual package name
	 * set to this property will be "kommet.envs.<envId>.com.pigeons.test". 
	 */
	private String packageName;
	
	private String name;
	private Boolean isSystem;
	private Boolean isDraft;
	private String accessLevel;
	
	public Class() throws KommetException
	{
		this(null, null);
	}
	
	public Class (Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
	}

	public void setJavaCode(String javaCode)
	{
		this.javaCode = javaCode;
		setInitialized();
	}

	@Property(field = "javaCode")
	public String getJavaCode()
	{
		return javaCode;
	}

	public void setPackageName(String packageName) throws KommetException
	{
		// make sure the package name does not contain the word "package" itself, as this causes
		// java compilation error
		if (packageName != null && !MiscUtils.isValidPackageName(packageName))
		{
			throw new KommetException("Package name " + packageName + " contains illegal substring \"package\"");
		}
		this.packageName = packageName;
		setInitialized();
	}

	@Property(field = "packageName")
	public String getPackageName()
	{
		return packageName;
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

	public void setKollCode(String kollCode) throws InvalidClassCodeException
	{
		KollUtil.validateKollCode(kollCode);
		this.kollCode = kollCode;
		setInitialized();
	}

	@Property(field = "kollCode")
	public String getKollCode()
	{
		return kollCode;
	}

	public void setIsSystem(Boolean isSystem)
	{
		this.isSystem = isSystem;
		setInitialized();
	}

	@Property(field = "isSystem")
	public Boolean getIsSystem()
	{
		return isSystem;
	}
	
	@Transient
	public String getQualifiedName()
	{
		return (this.packageName != null ? this.packageName + "." : "") + this.name;
	}

	@Property(field = "accessLevel")
	public String getAccessLevel()
	{
		return accessLevel;
	}

	public void setAccessLevel(String accessLevel)
	{
		this.accessLevel = accessLevel;
		setInitialized();
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.CLASS;
	}

	@Property(field = "isDraft")
	public Boolean getIsDraft()
	{
		return isDraft;
	}

	public void setIsDraft(Boolean isDraft)
	{
		this.isDraft = isDraft;
		setInitialized();
	}
}