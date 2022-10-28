/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.js.jsti;

import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;

public class JSTIType
{
	private KID id;
	private String apiName;
	private String label;
	private String pluralLabel;
	private KID idFieldId;
	private KID defaultFieldId;
	private String qualifiedName;
	private KeyPrefix keyPrefix;
	private TypePermission permission;
	
	public JSTIType()
	{
		// empty constructor needed for deserialization
	}
	
	public JSTIType(Type type, EnvData env) throws KommetException
	{
		this.id = type.getKID();
		this.apiName = type.getApiName();
		this.label = type.getLabel();
		this.qualifiedName = type.getQualifiedName();
		this.pluralLabel = type.getPluralLabel();
		this.idFieldId = type.getField(Field.ID_FIELD_NAME).getKID();
		this.defaultFieldId = type.getDefaultFieldId();
		this.keyPrefix = type.getKeyPrefix();
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

	public void setIdFieldId(KID idFieldId)
	{
		this.idFieldId = idFieldId;
	}

	public KID getIdFieldId()
	{
		return idFieldId;
	}

	public void setDefaultFieldId(KID defaultFieldId)
	{
		this.defaultFieldId = defaultFieldId;
	}

	public KID getDefaultFieldId()
	{
		return defaultFieldId;
	}

	public String getPluralLabel()
	{
		return pluralLabel;
	}

	public void setPluralLabel(String pluralLabel)
	{
		this.pluralLabel = pluralLabel;
	}

	public String getQualifiedName()
	{
		return qualifiedName;
	}

	public void setQualifiedName(String qualifiedName)
	{
		this.qualifiedName = qualifiedName;
	}

	public KeyPrefix getKeyPrefix()
	{
		return keyPrefix;
	}

	public void setKeyPrefix(KeyPrefix keyPrefix)
	{
		this.keyPrefix = keyPrefix;
	}

	public TypePermission getPermission()
	{
		return permission;
	}

	public void setPermission(TypePermission permission)
	{
		this.permission = permission;
	}
	
}