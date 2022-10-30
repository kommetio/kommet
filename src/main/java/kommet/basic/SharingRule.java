/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.SHARING_RULE_API_NAME)
public class SharingRule extends StandardTypeRecordProxy
{
	private String name;
	private String description;
	private String type;
	private Class file;
	private String method;
	private KID referencedType;
	private Boolean isEdit;
	private Boolean isDelete;
	private String dependentTypes;
	private String sharedWith;
	
	public SharingRule() throws KommetException
	{
		this(null, null);
	}
	
	public SharingRule(Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
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

	@Property(field = "type")
	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
		setInitialized();
	}

	@Property(field = "file")
	public Class getFile()
	{
		return file;
	}

	public void setFile(Class file)
	{
		this.file = file;
		setInitialized();
	}

	@Property(field = "method")
	public String getMethod()
	{
		return method;
	}

	public void setMethod(String method)
	{
		this.method = method;
		setInitialized();
	}

	@Property(field = "referencedType")
	public KID getReferencedType()
	{
		return referencedType;
	}

	public void setReferencedType(KID referencedType)
	{
		this.referencedType = referencedType;
		setInitialized();
	}

	@Property(field = "isEdit")
	public Boolean getIsEdit()
	{
		return isEdit;
	}

	public void setIsEdit(Boolean isEdit)
	{
		this.isEdit = isEdit;
		setInitialized();
	}

	@Property(field = "isDelete")
	public Boolean getIsDelete()
	{
		return isDelete;
	}

	public void setIsDelete(Boolean isDelete)
	{
		this.isDelete = isDelete;
		setInitialized();
	}

	@Property(field = "dependentTypes")
	public String getDependentTypes()
	{
		return dependentTypes;
	}

	public void setDependentTypes(String dependentTypes)
	{
		this.dependentTypes = dependentTypes;
		setInitialized();
	}

	@Property(field = "sharedWith")
	public String getSharedWith()
	{
		return sharedWith;
	}

	public void setSharedWith(String sharedWith)
	{
		this.sharedWith = sharedWith;
		setInitialized();
	}
}