/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.FieldValueException;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.utils.MiscUtils;

public class Record
{
	private Type type;
	private Long id;
	private Map<String, Object> fieldValues = new HashMap<String, Object>();
	
	// Tells if we are restricting the depth of queries on inverse collection to one level down.
	// If yes, then query "select children.parent.id from parent" will be allowed, but "select children.parent.name from parent" will not.
	// If no, both queries will be allowed.
	private static final boolean ONE_LEVEL_DOWN_RESTRICTION = false;
	
	private List<ValidationError> errors;
	
	public Record (boolean isUntyped) throws KommetException
	{
		if (!isUntyped)
		{
			throw new KommetException("Cannot instantiate record with null type");
		}
	}
	
	public Record (Type type) throws KommetException
	{
		if (type == null)
		{
			throw new KommetException("Cannot instantiate record with null type");
		}
		this.type = type;
	}
	
	public void uninitializeField(String field)
	{
		this.fieldValues.remove(field);
	}
	
	public void setField (String fieldName, Object value) throws KommetException
	{
		setField(fieldName, value, null);
	}
	
	public void setField (String fieldName, Object value, EnvData env) throws KommetException
	{
		setField(fieldName, value, false, env);
	}
	
	@SuppressWarnings({ "unchecked", "unused" })
	public void setField (String fieldName, Object value, boolean bulk, EnvData env) throws KommetException
	{
		// if field is qualified
		if (fieldName.contains("."))
		{
			if (env == null)
			{
				throw new KommetException("EnvData is required when qualified properties are set");
			}
			
			String firstProperty = fieldName.substring(0, fieldName.indexOf('.'));
			String nestedProperty = fieldName.substring(fieldName.indexOf('.') + 1);
			
			Field field = type.getField(firstProperty, env);
			
			if (field == null)
			{
				throw new KommetException("No field " + firstProperty + " found on object " + type.getApiName());
			}
			
			if (field.getDataType() instanceof TypeReference)
			{
				// the field may already be initialized, so we check this
				Record fieldInstance = (Record)attemptGetField(firstProperty);
				
				// If the assigned value is null, we don't need to go deeper into the nested property - we set the first partial property to null.
				// E.g. if property parent.mother.age is null, and parent.mother is not yet initialized, then we
				// don't initialize it. But if it is, we need to propagate the null value.
				if (value != null)
				{
					// We want to instantiate the nested object on all cases but one, namely:
					// when ID property on the nested object is nullified (e.g. "father.id"), i.e.
					// assigned value SpecialValue.NULL, then we interprete this as a request to nullify the
					// whole type reference.
					if (nestedProperty.equals(Field.ID_FIELD_NAME) && SpecialValue.isNull(value))
					{
						fieldInstance = new NullifiedRecord(env.getType(((TypeReference)field.getDataType()).getType().getKeyPrefix()));
					}
					else if (fieldInstance == null)
					{	
						// if the field is not yet initialized, we create an instance of the referenced type type
						fieldInstance = new Record(env.getType(((TypeReference)field.getDataType()).getType().getKeyPrefix()));
					}
					
					if (!(fieldInstance instanceof NullifiedRecord))
					{
						// set nested properties on the record only if it has not been nullified
						fieldInstance.setField(nestedProperty, value, env);
					}
					
					this.fieldValues.put(firstProperty, fieldInstance);
				}
				// if some partial property has already been set (e.g. parent.mother as above),
				// then we need to propagate the null value to be assigned to property parent.mother.age
				else 
				{
					if (fieldInstance != null)
					{
						fieldInstance.setField(nestedProperty, value, env);
						this.fieldValues.put(firstProperty, fieldInstance);
					}
					else
					{
						// set the first partial property to null
						setField(firstProperty, null);
					}
				}
			}
			else if (field.getDataType() instanceof InverseCollectionDataType)
			{
				// the field may already be initialized, so we check this
				List<Record> fieldInstance = (List<Record>)attemptGetField(firstProperty);
				
				// make sure the inverse collection is not references more than one level down, unless its
				// a reference to the ID
				if (ONE_LEVEL_DOWN_RESTRICTION && nestedProperty.contains(".") && !nestedProperty.endsWith(Field.ID_FIELD_NAME))
				{
					throw new KommetException("Collection property " + fieldName + " cannot be referenced more than one level down. Only its ID can be referenced, e.g. '" + fieldName + ".id'");
				}
				
				// On inverse properties, the value of the field will be a collection of records,
				// and the value that came as a parameter to the setField method needs to be a collection
				// as well.
				// More over, its size needs to be the same as the size of the record collection.
				List<Object> values = null;
				
				if (bulk)
				{
					transformIntoBulkValues(fieldInstance, values, value, fieldName);
				}
				
				if (value instanceof List<?>)
				{
					values = (List<Object>)value;
				}
				else
				{
					throw new KommetException("Value assigned to an inverse collection must be a collection itself, but it is of type " + value.getClass().getName());
				}
				
				InverseCollectionDataType collectionDef = (InverseCollectionDataType)field.getDataType();
				
				if (fieldInstance.isEmpty())
				{
					// this is the first time anything is assigned to the records in this collection,
					// so we initialize its size to the size of the value collection
					// if the field is not yet initialized, we create an instance of list of records
					fieldInstance = new ArrayList<Record>();
					for (int i = 0; i < values.size(); i++)
					{
						Record item = new Record (collectionDef.getInverseType());
						// set the item's type reference to the current object
						item.setField(collectionDef.getInverseProperty(), this);
						fieldInstance.add(item);
					}
				}
				
				if (fieldInstance.size() != values.size())
				{
					throw new KommetException("The number of items in the value collection (" + values.size() + ") is different than the number of records in the collection (" + fieldInstance.size() + ")");
				}
				
				// at this point, thanks to earlier checks, we are sure that nestedProperty
				// is a direct property, so we can get the field type for it
				Field nestedField = collectionDef.getInverseType().getField(nestedProperty, env);
				if (nestedField == null)
				{
					throw new KommetException("Cannot resolve nested property " + nestedProperty + " on type " + collectionDef.getInverseType().getQualifiedName());
				}
				
				// Set the value of the nested property on every collection item.
				// E.g. if collection is "children", and nested property is "age", this will
				// set the property "age" on every item from the "children" collection.
				// It assumes the order of the values is the same as the order of items in the collection.
				for (int i = 0; i < fieldInstance.size(); i++)
				{
					if (values.get(i) == null)
					{
						// The value for this field may simply be null, so we skip further processing.
						// However, we need to explicitly set the value to null, otherwise it would
						// be marked as uninitialized on the record and an error would be thrown when
						// reading it.
						fieldInstance.get(i).setField(nestedProperty, null, env);
						continue;
					}
					
					//log.debug("Set collection item " + i + ", field " + nestedProperty + " to value " + values.get(i));
					Object currValue = values.get(i); 
					if (nestedField.getDataType() instanceof TypeReference)
					{
						currValue = new Record(((TypeReference)nestedField.getDataType()).getType());
						((Record)currValue).setKID(KID.get((String)values.get(i)));
					}
					fieldInstance.get(i).setField(nestedProperty, currValue, env);
				}
				
				this.fieldValues.put(firstProperty, fieldInstance);
			}
			else if (field.getDataType() instanceof AssociationDataType)
			{
				// the field may already be initialized, so we check this
				List<Record> fieldInstance = (List<Record>)attemptGetField(firstProperty);
				
				// make sure the inverse collection is not references more than one level down, unless its
				// a reference to the ID
				if (ONE_LEVEL_DOWN_RESTRICTION && nestedProperty.contains(".") && !nestedProperty.endsWith(Field.ID_FIELD_NAME))
				{
					throw new KommetException("Association property " + fieldName + " cannot be referenced more than one level down. Only its ID can be referenced, e.g. '" + fieldName + ".id'");
				}
				
				// On inverse properties, the value of the field will be a collection of records,
				// and the value that came as a parameter to the setField method needs to be a collection
				// as well.
				// More over, its size needs to be the same as the size of the record collection.
				List<Object> values = null;
				
				if (bulk)
				{
					transformIntoBulkValues(fieldInstance, values, value, fieldName);
				}
				
				if (value instanceof List<?>)
				{
					values = (List<Object>)value;
				}
				else
				{
					throw new KommetException("Value assigned to an association collection must be a collection itself, but it is of type " + value.getClass().getName());
				}
				
				AssociationDataType associationDef = (AssociationDataType)field.getDataType();
				
				if (fieldInstance.isEmpty())
				{
					// this is the first time anything is assigned to the records in this collection,
					// so we initialize its size to the size of the value collection
					// if the field is not yet initialized, we create an instance of list of records
					fieldInstance = new ArrayList<Record>();
					for (int i = 0; i < values.size(); i++)
					{
						Record item = new Record (associationDef.getAssociatedType());
						// set the item's type reference to the current object
						//item.setField(associationDef.getInverseProperty(), this);
						fieldInstance.add(item);
					}
				}
				
				if (fieldInstance.size() != values.size())
				{
					throw new KommetException("The number of items in the value collection (" + values.size() + ") is different than the number of records in the collection (" + fieldInstance.size() + ")");
				}
				
				// at this point, thanks to earlier checks, we are sure that nestedProperty
				// is a direct property, so we can get the field type for it
				Field nestedField = associationDef.getAssociatedType().getField(nestedProperty, env);
				if (nestedField == null)
				{
					throw new KommetException("Cannot resolve nested property type");
				}
				
				// Set the value of the nested property on every collection item.
				// E.g. if collection is "children", and nested property is "age", this will
				// set the property "age" on every item from the "children" collection.
				// It assumes the order of the values is the same as the order of items in the collection.
				for (int i = 0; i < fieldInstance.size(); i++)
				{
					if (values.get(i) == null)
					{
						// The value for this field may simply be null, so we skip further processing.
						// However, we need to explicitly set the value to null, otherwise it would
						// be marked as uninitialized on the record and an error would be thrown when
						// reading it.
						fieldInstance.get(i).setField(nestedProperty, null, env);
						continue;
					}
					
					Object currValue = values.get(i); 
					if (nestedField.getDataType() instanceof TypeReference)
					{
						currValue = new Record(((TypeReference)nestedField.getDataType()).getType());
						((Record)currValue).setKID(KID.get((String)values.get(i)));
					}
					fieldInstance.get(i).setField(nestedProperty, currValue, env);
				}
				
				this.fieldValues.put(firstProperty, fieldInstance);
			}
			else
			{
				throw new KommetException("Qualified property " + firstProperty + " must be an type reference, but is " + field.getDataType().getClass().getName());
			}
		}
		else
		{
			if (this.type.getField(fieldName, env) == null)
			{
				throw new KommetException("Field '" + this.type.getApiName() + "."+ fieldName + "' does not exist. Remember field names are case-sensitive.");
			}
			
			// setting ID fields to null has no effect, so it is forbidden
			if (Field.ID_FIELD_NAME.equals(fieldName) && SpecialValue.isNull(value))
			{
				throw new FieldValueException("Cannot nullify ID field on type " + this.type.getQualifiedName());
			}
			
			this.fieldValues.put(fieldName, type.getField(fieldName, env).getDataType().getJavaValue(value));
		}
	}
	
	private static void transformIntoBulkValues(List<Record> fieldInstance, List<Object> values, Object value, String fieldName) throws KommetException
	{
		if (fieldInstance.isEmpty())
		{
			throw new KommetException("Cannot perform a bulk set on an uninitialized collection " + fieldName);
		}
		
		if (value instanceof List<?>)
		{
			throw new KommetException("Value assigned in a bulk set operation cannot be a list");
		}
		
		values = new ArrayList<Object>();
		for (int i = 0; i < fieldInstance.size(); i++)
		{
			values.add(value);
		}
	}
	
	public Type getType()
	{
		return this.type;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public Long getId()
	{
		return id;
	}

	public void setKID(KID rid) throws KommetException
	{
		setField(Field.ID_FIELD_NAME, rid, null);
	}
	
	/**
	 * Return a value of a field that is not an type reference.
	 * @param fieldName
	 * @return
	 * @throws KommetException 
	 */
	public Object getScalarField (String fieldName) throws KommetException
	{
		return getField(fieldName, true);
	}
	
	public Object attemptGetScalarField (String fieldName) throws KommetException
	{
		return getField(fieldName, false);
	}
	
	public Object attemptGetField (String fieldName) throws KommetException
	{
		return getField(fieldName, false);
	}
	
	public Object getField (String fieldName) throws KommetException
	{
		return getField(fieldName, true);
	}

	/**
	 * Returns the value of a field, direct or nested.
	 * @param fieldName The name of the field. It can be either a direct field, or a nested one.
	 * @param errorIfNotInitialized
	 * @return
	 * @throws KommetException
	 */
	public Object getField (String fieldName, boolean errorIfNotInitialized) throws KommetException
	{
		if (fieldName.contains("."))
		{
			String firstProperty = fieldName.substring(0, fieldName.indexOf('.'));
			Object propertyVal = this.fieldValues.get(firstProperty);
			if (propertyVal == null)
			{
				// check if field exists on type
				if (this.type.getField(firstProperty) != null)
				{
					return null;
				}
				else
				{
					throw new NoSuchFieldException("Field " + firstProperty + " does not exist on type " + this.type.getQualifiedName());
				}
			}
			else
			{
				if (!(propertyVal instanceof Record))
				{
					throw new KommetException("Property " + firstProperty + " is not an type reference");
				}
				
				return ((Record)propertyVal).getField(fieldName.substring(fieldName.indexOf('.') + 1), errorIfNotInitialized);
			}
		}
		else
		{
			if (this.fieldValues.containsKey(fieldName))
			{
				// return a value, even if it's null
				Object value = this.fieldValues.get(fieldName);
				if (value != null)
				{
					return value;
				}
				else
				{
					// check if field exists on type
					if (this.type.getField(fieldName) != null)
					{
						return value;
					}
					else
					{
						throw new NoSuchFieldException("Field " + fieldName + " does not exist on type " + this.type.getQualifiedName());
					}
				}
			}
			else if (errorIfNotInitialized)
			{
				// the value has not been initialized at all, even with a null value, so it cannot be retrieved
				throw new UninitializedFieldException("Attempt to retrieve uninitilized field '" + fieldName + "' on record of type " + this.type.getQualifiedName() + ". If the record was retrieved through a DAL query, probably the field was not included in the SELECT clause. If the record was created manually, the field was not set using method setField(). If you are not sure whether the field can be retrieved, use method attemptGetField()");
			}
			else
			{
				return null;
			}
		}
	}

	public String getFieldValueForPostgres (String fieldName) throws KommetException
	{ 
		// get raw field value
		Object fieldValue = getField(fieldName, true);
		
		// format the fields value according to its data type for Postgres queries
		Field field = type.getField(fieldName);
		return field.getDataType().getPostgresValue(fieldValue);
	}
	
	public Date getCreatedDate() throws KommetException
	{
		return (Date)getScalarField(Field.CREATEDDATE_FIELD_NAME);
	}
	
	public Date getLastModifiedDate() throws KommetException
	{
		return (Date)getScalarField(Field.LAST_MODIFIED_DATE_FIELD_NAME);
	}
	
	public Integer getAccessType() throws KommetException
	{
		return (Integer)getScalarField(Field.ACCESS_TYPE_FIELD_NAME);
	}
	
	public Record getCreatedBy() throws KommetException
	{
		return (Record)getScalarField(Field.CREATEDBY_FIELD_NAME);
	}
	
	public void setCreatedBy(KID rid, EnvData env) throws KommetException
	{
		setField(Field.CREATEDBY_FIELD_NAME + "." + Field.ID_FIELD_NAME, rid, env);
	}
	
	public Record getLastModifiedBy() throws KommetException
	{
		return (Record)getScalarField(Field.LAST_MODIFIED_BY_FIELD_NAME);
	}
	
	public void setLastModifiedBy(KID rid, EnvData env) throws KommetException
	{
		setField(Field.LAST_MODIFIED_BY_FIELD_NAME + "." + Field.ID_FIELD_NAME, rid, env);
	}
	
	public KID attemptGetKID() throws KommetException
	{
		return (KID)attemptGetScalarField(Field.ID_FIELD_NAME);
	}
	
	/**
	 * Returns the ID of the record.
	 * @return The ID of the record
	 * @throws KommetException
	 */
	public KID getKID() throws KommetException
	{
		return (KID)getScalarField(Field.ID_FIELD_NAME);
	}

	public void setCreatedDate(Date date) throws KommetException
	{
		setField(Field.CREATEDDATE_FIELD_NAME, date, null);
	}
	
	public void setAccessType(Integer accessType) throws KommetException
	{
		setField(Field.ACCESS_TYPE_FIELD_NAME, accessType, null);
	}
	
	public void setLastModifiedDate(Date date) throws KommetException
	{
		setField(Field.LAST_MODIFIED_DATE_FIELD_NAME, date, null);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("{ ");
		
		List<String> fieldDescriptions = new ArrayList<String>();
		
		for (String field : this.fieldValues.keySet())
		{	
			Object fieldValue = this.fieldValues.get(field);
			
			// printing recursively contents of an inverse collection could cause an
			// infinite loop because it would take us back to this object
			if (fieldValue instanceof List<?>)
			{
				List<Object> list = (List<Object>)fieldValue;
				if (list.isEmpty())
				{
					fieldDescriptions.add("[ List (empty) ]");
				}
				else
				{
					fieldDescriptions.add("[ List of " + list.get(0).getClass().getName() + ", size = " + list.size() + "]");
				}
				
				continue;
			}
			
			fieldDescriptions.add("\"" + field + "\" = " + (fieldValue != null ? (((fieldValue instanceof Record) || (fieldValue instanceof List)) ? fieldValue.toString() : "\"" + fieldValue.toString() + "\"") : "null"));
		}
		
		return sb.append(MiscUtils.implode(fieldDescriptions, ", ")).append(" }").toString();
	}
	
	public Map<String, Object> getFieldValues()
	{
		return this.fieldValues;
	}
	
	public String getFieldStringValue (String fieldName, Locale locale) throws KommetException
	{
		return getFieldStringValue(fieldName, true, locale);
	}

	public String getFieldStringValue (String fieldName, boolean failOnUnitialized, Locale locale) throws KommetException
	{
		// get raw field value
		Object fieldValue = getField(fieldName, failOnUnitialized);
		
		// format the fields value according to its data type for Postgres queries
		Field field = type.getField(fieldName);
		
		if (field == null)
		{
			throw new KommetException("Field " + fieldName + " not found on type " + type.getQualifiedName());
		}
		
		return field.getDataType().getStringValue(fieldValue, locale);
	}

	/**
	 * Returns the value of the default field for this record.
	 * @return
	 * @throws KommetException
	 */
	public String getDefaultFieldValue(Locale locale) throws KommetException
	{
		if (this.type == null)
		{
			throw new KommetException("Cannot get default record value because type of record is not set");
		}
		
		return getFieldStringValue(this.type.getDefaultFieldApiName(), locale);
	}
	
	/**
	 * Checks if a property with the given name is set on the record. This method works only for
	 * simple properties. For nested properties, it always returns false.
	 * @param fieldApiName
	 * @return
	 * @throws KommetException 
	 */
	public boolean isSet (String fieldApiName) throws KommetException
	{
		if (fieldApiName == null)
		{
			throw new KommetException("Field name to get is null");
		}
		
		if (fieldApiName.contains("."))
		{
			throw new KommetException("Cannot call isSet for nested property " + fieldApiName);
		}
		
		return this.fieldValues.containsKey(fieldApiName);
	}
	
	/**
	 * Returns true is the value of the given field is set and is not empty. Value is treated as empty if its:
	 * <ul>
	 * <li>{@link SpecialValue.NULL}</li>
	 * <li>instance of {@link NullifiedRecord}</li>
	 * <li>null</li>
	 * <li>empty string</li>
	 * </ul>
	 * @param fieldApiName
	 * @return
	 * @throws FieldNotSetException
	 * @throws KommetException
	 */
	public boolean isEmpty (String fieldApiName) throws FieldNotSetException, KommetException
	{
		if (!isSet(fieldApiName))
		{
			throw new FieldNotSetException("Field " + fieldApiName + " is not set on record with ID " + attemptGetKID(), attemptGetKID(), fieldApiName);
		}
		
		Object value = attemptGetField(fieldApiName);
		return value == null || "".equals(value) || SpecialValue.isNull(value);
	}

	public void setErrors(List<ValidationError> errors)
	{
		this.errors = errors;
	}

	public List<ValidationError> getErrors()
	{
		return errors;
	}
	
	/**
	 * Tells if any errors have been attached to this record.
	 * @return
	 */
	public boolean hasErrors()
	{
		return this.errors != null && !this.errors.isEmpty();
	}
}