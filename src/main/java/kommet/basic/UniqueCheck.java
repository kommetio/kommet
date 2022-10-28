/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.UNIQUE_CHECK_API_NAME)
public class UniqueCheck extends StandardTypeRecordProxy implements Deployable
{
	private KID typeId;
	private String name;
	private String dbName;
	private String fieldIds;
	private List<KID> parsedFieldIds;
	private Boolean isSystem;
	public static final String DB_CHECK_NAME_PREFIX = "uc_";
	
	public UniqueCheck() throws RecordProxyException
	{
		this(null, null);
	}
	
	public UniqueCheck(Record record, EnvData env) throws RecordProxyException
	{
		this(record, false, env);
	}
	
	public UniqueCheck(Record record, boolean ignoreUnfetchedFields, EnvData env) throws RecordProxyException
	{
		super(record, true, env);
		
	}
	
	/**
	 * Generates a DB name for a unique check
	 * @param typeId
	 * @param env
	 * @return
	 */
	public static String generateDbName (KID typeId, EnvData env)
	{
		return DB_CHECK_NAME_PREFIX + typeId + "_" + env.getJdbcTemplate().queryForObject("select nextval('unique_check_seq')", Long.class);
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

	public void setDbName(String dbName)
	{
		this.dbName = dbName;
		setInitialized();
	}

	@Property(field = "dbName")
	public String getDbName()
	{
		return dbName;
	}

	public void setFieldIds(String fieldIds) throws KIDException
	{	
		this.parsedFieldIds = new ArrayList<KID>();
		if (StringUtils.hasText(fieldIds))
		{
			String[] splitIds = fieldIds.split(";");
			for (int i = 0; i < splitIds.length; i++)
			{
				this.parsedFieldIds.add(KID.get(splitIds[i]));
			}
		}
		
		if (!this.parsedFieldIds.isEmpty())
		{
			this.fieldIds = MiscUtils.implode(this.parsedFieldIds, ";");
		}
		else
		{
			this.fieldIds = null;
		}
		setInitialized();
	}
	
	public void addField (Field field) throws KommetException
	{
		if (field == null)
		{
			throw new KommetException("Trying to add null field to type UniqueCheck");
		}
		
		if (this.parsedFieldIds == null)
		{
			this.parsedFieldIds = new ArrayList<KID>();
		}
		else
		{
			for (KID fieldId : this.parsedFieldIds)
			{
				if (field.getKID().equals(fieldId))
				{
					throw new KommetException("Trying to add field " + field.getApiName() + " twice to the unique check");
				}
			}
		}
		
		this.parsedFieldIds.add(field.getKID());
		this.fieldIds = MiscUtils.implode(this.parsedFieldIds, ";");
		setInitialized("fieldIds");
	}

	@Property(field = "fieldIds")
	public String getFieldIds()
	{
		return fieldIds;
	}

	@Transient
	public List<KID> getParsedFieldIds()
	{
		return this.parsedFieldIds;
	}

	public boolean hasField(KID fieldId)
	{
		Set<KID> fieldIds = new HashSet<KID>();
		fieldIds.addAll(this.parsedFieldIds);
		return fieldIds.contains(fieldId);
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.UNIQUE_CHECK;
	}

	@Property(field = "isSystem")
	public Boolean getIsSystem()
	{
		return isSystem;
	}

	public void setIsSystem(Boolean isSystem)
	{
		this.isSystem = isSystem;
		setInitialized();
	}

	public void clearFields()
	{
		this.fieldIds = null;
		this.parsedFieldIds = null;
	}
}