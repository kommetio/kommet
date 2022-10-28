/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.UniqueCheck;
import kommet.basic.types.SystemTypes;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Transient;
import kommet.utils.MiscUtils;

public class Type extends BasicModel<Long> implements Deployable
{
	private static final long serialVersionUID = -2023837848407234237L;
	private String apiName;
	private String packageName;
	private String label;
	private String pluralLabel;
	private Date created;
	private KID rid;
	private KeyPrefix keyPrefix;
	private String dbTable;
	private boolean isBasic;
	private KID defaultFieldId;
	private KID sharingControlledByFieldId;
	private boolean combineRecordAndCascadeSharing;
	private String description;
	private boolean isFieldsInitialized;
	private boolean isDeclaredInCode;
	private String uchLabel;
	private String uchPluralLabel;
	private boolean isAutoLinkingType;
	private KID autoNumberFieldId;
	
	/**
	 * Fields by API name
	 */
	private Map<String, Field> fieldsByApiName = new HashMap<String, Field>();
	
	/**
	 * Field with default values. We keep track of these fields in a separate list so that the method
	 * DataService.setDefaultValues can scan over fields with default values faster, and does not have to
	 * check all fields.
	 */
	private Map<String, Field> fieldsWithDefaultValues = new HashMap<String, Field>();
	
	/**
	 * Fields by their KID
	 */
	private Map<KID, Field> fieldsById = new HashMap<KID, Field>();
	
	/**
	 * Unique checks on fields of this object.
	 */
	private List<UniqueCheck> uniqueChecks;
	
	public Long getId()
	{
		return this.id;
	}

	public void setApiName(String apiName)
	{
		this.apiName = apiName;
	}
	
	public String getApiName()
	{
		return apiName;
	}

	public void setPackage(String packageName) throws KommetException
	{
		if (!MiscUtils.isValidPackageName(packageName))
		{
			throw new KommetException("Package name " + packageName + " contains illegal substring \"package\"");
		}
		this.packageName = packageName;
	}

	public String getPackage()
	{
		return packageName;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
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
	
	public String getInterpretedPluralLabel(AuthData authData)
	{
		String actualLabel = null;
		if (this.uchLabel != null)
		{
			actualLabel = authData.getUserCascadeSettings().get(this.uchPluralLabel);
		}
		return StringUtils.hasText(actualLabel) ? actualLabel : this.pluralLabel;
	}

	public void setPluralLabel(String pluralLabel)
	{
		this.pluralLabel = pluralLabel;
	}

	public String getPluralLabel()
	{
		return pluralLabel;
	}

	public void setCreated(Date created)
	{
		this.created = created;
	}

	public Date getCreated()
	{
		return created;
	}

	public void setKID(KID rid)
	{
		this.rid = rid;
	}

	public KID getKID()
	{
		return rid;
	}

	public void setKeyPrefix(KeyPrefix keyPrefix)
	{
		this.keyPrefix = keyPrefix;
	}

	public KeyPrefix getKeyPrefix()
	{
		return keyPrefix;
	}

	public void setDbTable(String dbTable)
	{
		this.dbTable = dbTable;
	}

	public String getDbTable()
	{
		return dbTable;
	}
	
	public boolean isPersisted()
	{
		return this.keyPrefix != null && this.rid != null;
	}

	public void addField (Field field) throws KommetException
	{
		this.fieldsByApiName.put(field.getApiName(), field);
		if (field.getKID() != null)
		{
			this.fieldsById.put(field.getKID(), field);
		}
		
		if (field.getDefaultValue() != null)
		{
			this.fieldsWithDefaultValues.put(field.getApiName(), field);
		}
		
		field.setType(this);
		this.isFieldsInitialized = true;
	}
	
	public String getKIDSeqName()
	{
		return "obj_" + this.keyPrefix + "_kolmu_id_seq";
	}
	
	public String getAutonumberSeqName()
	{
		return "obj_" + this.keyPrefix + "_autonumber_seq";
	}
	
	public Field getField (KID id)
	{
		return this.fieldsById.get(id);
	}
	
	public Field getField (String qualifiedProperty) throws KommetException
	{
		return getField(qualifiedProperty, null);
	}

	/**
	 * Gets field definitions from type.
	 * @param qualifiedProperty
	 * @param env If specified, type definitions will be fetched from the environment, not from the type object, which will guarantee that the definitions are up-to-date.
	 * @return
	 * @throws KommetException
	 */
	public Field getField (String qualifiedProperty, EnvData env) throws KommetException
	{
		if (!qualifiedProperty.contains("."))
		{
			return this.fieldsByApiName.get(qualifiedProperty);
		}
		else
		{
			if (env == null)
			{
				throw new KommetException("When nested field (" + qualifiedProperty + ") is read, env data must be passed");
			}
			
			String firstProperty = qualifiedProperty.substring(0, qualifiedProperty.indexOf('.'));
			String furtherProperties = qualifiedProperty.substring(qualifiedProperty.indexOf('.') + 1);
			Field field = this.fieldsByApiName.get(firstProperty);
			if (field == null)
			{
				throw new NoSuchFieldException("No property '" + firstProperty + "' found on type " + this.getQualifiedName());
			}
			
			if (field.getDataType() instanceof TypeReference)
			{
				// recursively get deeper properties
				return env.getType(((TypeReference)field.getDataType()).getTypeId()).getField(furtherProperties, env);
			}
			else if (field.getDataType() instanceof InverseCollectionDataType)
			{
				// recursively get deeper properties
				return env.getType(((InverseCollectionDataType)field.getDataType()).getInverseTypeId()).getField(furtherProperties, env);
			}
			else if (field.getDataType() instanceof AssociationDataType)
			{
				// recursively get deeper properties
				return env.getType(((AssociationDataType)field.getDataType()).getAssociatedTypeId()).getField(furtherProperties, env);
			}
			else
			{
				throw new KommetException("Qualified property '" + firstProperty + "' must be an type reference, but is of type " + field.getDataType().getClass().getName());
			}
		}
	}
	
	public synchronized Collection<Field> getFields()
	{
		return this.fieldsByApiName.values();
	}

	public void setUniqueChecks(List<UniqueCheck> uniqueChecks)
	{
		this.uniqueChecks = uniqueChecks;
	}
	
	/**
	 * Adds a unique check to the type, or replaces an existing check.
	 * @param check
	 */
	public void addUniqueCheck (UniqueCheck check)
	{
		if (this.uniqueChecks == null)
		{
			this.uniqueChecks = new ArrayList<UniqueCheck>();
		}
		else
		{
			UniqueCheck uniqueCheckToRemove = null;
			// make sure a unique check with this ID does not already exist, if it does, remove it
			for (UniqueCheck existingCheck : this.uniqueChecks)
			{
				if (existingCheck.getId().equals(check.getId()))
				{
					// remove the existing check - it will be added anew below
					uniqueCheckToRemove = existingCheck;
					break;
				}
			}
			
			if (uniqueCheckToRemove != null)
			{
				this.uniqueChecks.remove(uniqueCheckToRemove);
			}
		}
		
		this.uniqueChecks.add(check);
	}

	/**
	 * Returns unique checks.
	 * NOTE: this field is transient.
	 * @return
	 */
	public List<UniqueCheck> getUniqueChecks()
	{
		return uniqueChecks;
	}
	
	public Map<String, Field> getFieldsWithDefaultValues()
	{
		return this.fieldsWithDefaultValues;
	}

	public Map<KID, Field> getFieldsByKID() throws KommetException
	{
		Map<KID, Field> fieldsById = new HashMap<KID, Field>();
		if (this.fieldsByApiName == null || this.fieldsByApiName.isEmpty())
		{
			return fieldsById;
		}
		
		for (Field field : this.fieldsByApiName.values())
		{
			if (field.getId() == null)
			{
				throw new KommetException("Cannot call method getFieldsByKID because object has some unsaved fields ('" + field.getApiName() + "')");
			}
			fieldsById.put(field.getKID(), field);
		}
		
		return fieldsById;
	}
	
	/**
	 * Returns the full qualified name of the class, i.e. package and class name.
	 * @return qualified name of the class
	 */
	public String getQualifiedName()
	{
		return (StringUtils.hasText(this.packageName) ? this.packageName + "." : "") + this.apiName;
	}

	public void removeField (String apiName) throws KommetException
	{
		Field field = this.fieldsByApiName.get(apiName);
		if (field == null)
		{
			throw new KommetException("Field with API name " + apiName + " does not exist on type " + this.apiName);
		}
		
		this.fieldsByApiName.remove(apiName);
		this.fieldsWithDefaultValues.remove(apiName);
		this.fieldsById.remove(field.getKID());
	}

	public String describe()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("Type [").append(this.apiName).append("]\n");
		
		for (Field field : this.getFields())
		{
			sb.append("field [").append(field.getApiName()).append("] [").append(field.getKID()).append("]\n|");
		}
		
		return sb.toString();
	}

	public void setBasic(boolean isBasic)
	{
		this.isBasic = isBasic;
	}

	public boolean isBasic()
	{
		return isBasic;
	}

	public void renameField (Field field, String oldFieldName) throws KommetException
	{
		Field existingField = this.fieldsByApiName.get(oldFieldName);
		if (existingField == null)
		{
			throw new KommetException("Field " + oldFieldName + " cannot be renamed - it does not exist");
		}
		
		this.fieldsByApiName.remove(oldFieldName);
		this.fieldsWithDefaultValues.remove(oldFieldName);
		
		addField(field);
	}

	public void setDefaultFieldId(KID defaultField) throws KommetException
	{	
		this.defaultFieldId = defaultField;
	}

	public KID getDefaultFieldId()
	{
		return defaultFieldId;
	}

	public String getDefaultFieldLabel(AuthData authData)
	{
		return this.defaultFieldId != null ? getField(this.defaultFieldId).getInterpretedLabel(authData) : Field.ID_FIELD_LABEL;
	}

	public String getDefaultFieldApiName() throws KommetException
	{
		if (!this.isFieldsInitialized)
		{
			throw new KommetException("Cannot read default field name because fields on type have not been initialized");
		}
		
		return this.defaultFieldId != null ? getField(this.defaultFieldId).getApiName() : Field.ID_FIELD_NAME;
	}

	public Field getDefaultField() throws KommetException
	{
		return getField(getDefaultFieldApiName());
	}

	public KID getSharingControlledByFieldId()
	{
		return sharingControlledByFieldId;
	}

	public void setSharingControlledByFieldId(KID sharingControlledByField)
	{
		this.sharingControlledByFieldId = sharingControlledByField;
	}

	public Field getSharingControlledByField()
	{
		return this.sharingControlledByFieldId != null ? this.fieldsById.get(this.sharingControlledByFieldId) : null;
	}

	public boolean isCombineRecordAndCascadeSharing()
	{
		return combineRecordAndCascadeSharing;
	}

	public void setCombineRecordAndCascadeSharing(boolean combineRecordAndCascadeSharings)
	{
		this.combineRecordAndCascadeSharing = combineRecordAndCascadeSharings;
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.TYPE;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public boolean isFieldsInitialized()
	{
		return isFieldsInitialized;
	}

	public void removeUniqueCheckById(KID id) throws KommetException
	{
		if (this.uniqueChecks == null)
		{
			throw new KommetException("Unique check with ID " + id + " cannot be removed from the type because it is not registered with it");
		}
		
		UniqueCheck ucToRemove = null;
		
		for (UniqueCheck uc : this.uniqueChecks)
		{
			if (uc.getId().equals(id))
			{
				ucToRemove = uc;
				break;
			}
		}
		
		if (ucToRemove != null)
		{
			this.uniqueChecks.remove(ucToRemove);
		}
		else
		{
			throw new KommetException("Unique check with ID " + id + " cannot be removed from the type because it is not registered with it");
		}
	}

	public boolean isDeclaredInCode()
	{
		return isDeclaredInCode;
	}

	public void setDeclaredInCode(boolean isDeclaredInCode)
	{
		this.isDeclaredInCode = isDeclaredInCode;
	}

	public String getUchLabel()
	{
		return uchLabel;
	}

	public void setUchLabel(String uchLabel)
	{
		this.uchLabel = uchLabel;
	}

	public String getUchPluralLabel()
	{
		return uchPluralLabel;
	}

	public void setUchPluralLabel(String uchPluralLabel)
	{
		this.uchPluralLabel = uchPluralLabel;
	}

	public boolean isAutoLinkingType()
	{
		return isAutoLinkingType;
	}

	public void setAutoLinkingType(boolean isAutoLinkingType)
	{
		this.isAutoLinkingType = isAutoLinkingType;
	}

	/**
	 * Tells if a type is accessible for use by users.
	 * @return
	 */
	public boolean isAccessible()
	{
		return !isAutoLinkingType && (!isBasic() || !SystemTypes.isInaccessibleSystemType(this));
	}

	public KID getAutoNumberFieldId()
	{
		return autoNumberFieldId;
	}

	public void setAutoNumberFieldId(KID autoNumberFieldId)
	{
		this.autoNumberFieldId = autoNumberFieldId;
	}
}