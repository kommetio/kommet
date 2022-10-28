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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.VALIDATION_RULE_API_NAME)
public class ValidationRule extends StandardTypeRecordProxy implements Deployable
{
	private KID typeId;
	private String name;
	private String code;
	private Boolean active;
	private Boolean isSystem;
	private String errorMessage;
	private String errorMessageLabel;
	private String referencedFields;
	
	public ValidationRule() throws RecordProxyException
	{
		super(null, true, null);
	}

	public ValidationRule(Record record, EnvData env) throws KommetException
	{
		super(record, true, env);
	}

	public void setTypeId(KID typeId)
	{
		this.typeId = typeId;
		setInitialized();
	}

	@Property(field = "typeId")
	public KID getTypeId()
	{
		return typeId;
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

	public void setActive(Boolean active)
	{
		this.active = active;
		setInitialized();
	}

	@Property(field = "active")
	public Boolean getActive()
	{
		return active;
	}

	public void setCode(String code)
	{
		this.code = code;
		setInitialized();
	}

	@Property(field = "code")
	public String getCode()
	{
		return code;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
		setInitialized();
	}

	@Property(field = "errorMessage")
	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessageLabel(String errorMessageLabel)
	{
		this.errorMessageLabel = errorMessageLabel;
		setInitialized();
	}

	@Property(field = "errorMessageLabel")
	public String getErrorMessageLabel()
	{
		return errorMessageLabel;
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
	public ComponentType getComponentType()
	{
		return ComponentType.VALIDATION_RULE;
	}

	@Property(field = "referencedFields")
	public String getReferencedFields()
	{
		return referencedFields;
	}

	public void setReferencedFields(String referencedFields)
	{
		this.referencedFields = referencedFields;
		setInitialized();
	}
}