/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.FormulaParser;
import kommet.deployment.Deployable;
import kommet.persistence.Transient;
import kommet.utils.ValidationUtil;

@Entity
@Table(name = "fields")
public class Field extends BasicModel<Long> implements Deployable
{
	private static final long serialVersionUID = 847423129239950493L;
	
	public static final String ID_FIELD_NAME = "id";
	public static final String ID_FIELD_LABEL = "Id";
	public static final String ID_FIELD_DB_COLUMN = "kid";
	
	public static final String CREATEDDATE_FIELD_NAME = "createdDate";
	public static final String CREATEDDATE_FIELD_LABEL = "Created Date";
	public static final String CREATEDDATE_FIELD_DB_COLUMN = "createddate";
	
	public static final String CREATEDBY_FIELD_NAME = "createdBy";
	public static final String CREATEDBY_FIELD_LABEL = "Created By";
	public static final String CREATEDBY_FIELD_DB_COLUMN = "createdby";
	
	public static final String LAST_MODIFIED_DATE_FIELD_NAME = "lastModifiedDate";
	public static final String LAST_MODIFIED_DATE_FIELD_LABEL = "Last Modified Date";
	public static final String LAST_MODIFIED_DATE_FIELD_DB_COLUMN = "lastmodifieddate";
	
	public static final String LAST_MODIFIED_BY_FIELD_NAME = "lastModifiedBy";
	public static final String LAST_MODIFIED_BY_FIELD_LABEL = "Last Modified By";
	public static final String LAST_MODIFIED_BY_FIELD_DB_COLUMN = "lastmodifiedby";
	
	public static final String DELETED_DATE_FIELD_NAME = "deletedDate";
	public static final String DELETED_DATE_FIELD_LABEL = "Deleted Date";
	public static final String DELETED_DATE_FIELD_DB_COLUMN = "deleteddate";
	
	public static final String DELETED_BY_FIELD_NAME = "deletedBy";
	public static final String DELETED_BY_FIELD_LABEL = "Deleted By";
	public static final String DELETED_BY_FIELD_DB_COLUMN = "deletedby";
	
	public static final String ACCESS_TYPE_FIELD_NAME = "accessType";
	public static final String ACCESS_TYPE_FIELD_LABEL = "Access Type";
	public static final String ACCESS_TYPE_FIELD_DB_COLUMN = "accesstype";
	
	/*public static final String AUTONUMBER_TYPE_FIELD_NAME = "autoNumber";
	public static final String AUTONUMBER_FIELD_LABEL = "Auto Number";
	public static final String AUTONUMBER_FIELD_DB_COLUMN = "autonumber";*/
	
	public static final String TRIGGER_FLAG_DB_COLUMN = "_triggerflag";
	
	private String apiName;
	private String label;
	private DataType dataType;
	private boolean required = false;
	private Date created;
	private String dbColumn;
	private Type type;
	private KID rid;
	private boolean trackHistory = false;
	
	/**
	 * Default value of the field, stored as string (since all values can be represented as a string).
	 */
	private String defaultValue;
	private String description;
	
	/**
	 * Tells whether the field is created in the database when the object is inserted.
	 * If yes, no separate SQL needs to be run to add a DB column for this field.
	 */
	private boolean createdOnTypeCreation = false;
	
	/**
	 * Tells whether the value for this field is automatically set when a record is created/updated.
	 * E.g. fields createdDate, lastModifiedBy are autoSet = true.
	 */
	private boolean autoSet = false;
	
	private String uchLabel;

	public Long getId()
	{
		return this.id;
	}
	
	public static boolean hasDatabaseRepresenation (DataType dt)
	{
		return !dt.getId().equals(DataType.INVERSE_COLLECTION) && !dt.getId().equals(DataType.ASSOCIATION) && !dt.getId().equals(DataType.FORMULA);
	}

	public void setApiName (String apiName) throws KommetException
	{
		String oldApiName = this.apiName;
		this.apiName = apiName;
		
		// fields are mapped by their names with the type they belong to,
		// so when the name changes, the mapping has to be updated as well
		if (this.type != null && oldApiName != null && this.type.getField(oldApiName) != null)
		{
			this.type.removeField(oldApiName);
			this.type.addField(this);
		}
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

	public void setDataType(DataType dataType)
	{
		this.dataType = dataType;
	}

	public DataType getDataType()
	{
		return dataType;
	}

	public void setRequired(boolean required)
	{
		this.required = required;
	}

	public boolean isRequired()
	{
		return required;
	}

	public void setCreated(Date created)
	{
		this.created = created;
	}

	public Date getCreated()
	{
		return created;
	}

	public void setDbColumn(String dbColumn)
	{
		//validateDbColumnName(dbColumn);
		
		// Column names are all lowercase.
		// Postgres probably converts their names anyway to lower case if we don't use quotes
		// around their names while creating them. We could do that, but it's better to keep it clear.
		this.dbColumn = dbColumn != null ? dbColumn.toLowerCase() : null;
	}
	
	/*private void validateDbColumnName(String dbColumn) throws KommetException
	{
		if (dbColumn.toLowerCase().equals("view"))
		{
			throw new KommetException("Cannot name column '" + dbColumn + "'. It is a reserved keyword in PostgresSQL.");
		}
	}*/

	public String getDbColumn()
	{
		return dbColumn;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}

	public void setKID (KID rid)
	{
		this.rid = rid;
	}

	public KID getKID()
	{
		return rid;
	}
	
	public static boolean isReservedFieldApiName (String name)
	{
		return ValidationUtil.getReservedFieldNames().contains(name.toLowerCase()) || ID_FIELD_NAME.equals(name) || CREATEDDATE_FIELD_NAME.equals(name) || CREATEDBY_FIELD_NAME.equals(name) || LAST_MODIFIED_BY_FIELD_NAME.equals(name) || LAST_MODIFIED_DATE_FIELD_NAME.equals(name) || ACCESS_TYPE_FIELD_NAME.equals(name);
	}

	public void setCreatedOnTypeCreation(boolean createdOnTypeCreation)
	{
		this.createdOnTypeCreation = createdOnTypeCreation;
	}

	/**
	 * Tells if the field's column definition is created when the object table is created.
	 * This applies only to the ID field. Other system fields, e.g. lastModifiedBy, have this property
	 * set to false because their columns are added when the object is saved, not created (in a separate
	 * SQL statement).
	 * @return
	 */
	public boolean isCreatedOnTypeCreation()
	{
		return createdOnTypeCreation;
	}

	public void setAutoSet(boolean autoSet)
	{
		this.autoSet = autoSet;
	}

	public boolean isAutoSet()
	{
		return autoSet;
	}

	public static boolean isSystemField(String apiName)
	{
		return CREATEDBY_FIELD_NAME.equals(apiName) || CREATEDDATE_FIELD_NAME.equals(apiName) || LAST_MODIFIED_BY_FIELD_NAME.equals(apiName) || LAST_MODIFIED_DATE_FIELD_NAME.equals(apiName) || ID_FIELD_NAME.equals(apiName) || ACCESS_TYPE_FIELD_NAME.equals(apiName);
 	}

	public boolean isDataType (int dataTypeId)
	{
		return this.dataType.getId().equals(dataTypeId);
	}

	public Integer getDataTypeId()
	{
		return this.dataType.getId();
	}

	public String getSQL(String alias, String quote) throws KIDException
	{
		if (!getDataTypeId().equals(DataType.FORMULA))
		{
			return (alias != null ? (quote + alias + quote + ".") : "") + quote + this.dbColumn + quote;
		}
		else
		{
			// TODO add quotes?
			return FormulaParser.getSQLFromParsedDefinition(((FormulaDataType)getDataType()).getParsedDefinition(), type, alias);
		}
	}

	public void setTrackHistory(boolean trackHistory)
	{
		this.trackHistory = trackHistory;
	}

	public boolean isTrackHistory()
	{
		return trackHistory;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.FIELD;
	}

	public String getUchLabel()
	{
		return uchLabel;
	}

	public void setUchLabel(String uchLabel)
	{
		this.uchLabel = uchLabel;
	}

	public String getInterpretedLabel(AuthData authData)
	{
		String actualLabel = null;
		if (this.uchLabel != null)
		{
			actualLabel = authData.getUserCascadeSettings().get(this.uchLabel);
		}
		return StringUtils.hasText(actualLabel) ? actualLabel : this.label;
	}
}