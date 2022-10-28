/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.js.jsti;

import kommet.auth.AuthData;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;

public class JSTIField
{
	private KID id;
	private String apiName;
	private String label;
	private String typeId;
	private JSTIDataType dataType;
	private FieldPermission permission;
	
	public JSTIField()
	{
		// empty constructor needed for deserialization
	}
	
	public JSTIField(Field field, EnvData env, AuthData authData) throws KommetException
	{
		this.id = field.getKID();
		this.apiName = field.getApiName();
		this.label = field.getInterpretedLabel(authData);
		this.typeId = field.getType().getKID().getId();
		this.dataType = new JSTIDataType(field.getDataType(), authData, env);
	}

	public void setId(KID id)
	{
		this.id = id;
	}

	public KID getId()
	{
		return id;
	}

	public void setApiName(String apiName)
	{
		this.apiName = apiName;
	}

	public String getApiName()
	{
		return apiName;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	public void setDataType(JSTIDataType dataType)
	{
		this.dataType = dataType;
	}

	public JSTIDataType getDataType()
	{
		return dataType;
	}

	public String getTypeId()
	{
		return typeId;
	}

	public void setTypeId(String typeId)
	{
		this.typeId = typeId;
	}

	public FieldPermission getPermission()
	{
		return permission;
	}

	public void setPermission(FieldPermission permission)
	{
		this.permission = permission;
	}

}