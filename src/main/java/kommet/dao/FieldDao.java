/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import javax.inject.Inject;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.basic.Dictionary;
import kommet.data.Field;
import kommet.data.GlobalSettings;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.AutoNumber;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.FormulaReturnType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.exceptions.NotImplementedException;
import kommet.services.GlobalSettingsService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

@Repository
public class FieldDao
{
	@Inject
	GlobalSettingsService settingService;
	
	//private static final Logger log = LoggerFactory.getLogger(FieldDao.class);
	
	public static final String FIELD_SQL_QUERY = "SELECT id, apiname, label, datatype, created, typeid, dbcolumn, kid, required, length, reftypekid, reffieldname, enumvalues, validateenum, description, defaultvalue, cascadedelete, autoset, associatedtypekid, associatedtypefieldname, formulareturntype, formulauserdefinition, formulaparseddefinition, decimalplaces, trackhistory, javatype, islong, isformatted, uchlabel, autonumberformat, dictionary FROM fields";
	public static final String FIELD_WITH_TYPE_SQL_QUERY = "SELECT f.id as id, f.apiname as apiname, f.label as label, f.datatype as datatype, f.created as created, f.typeid as typeid, f.dbcolumn as dbcolumn, f.kid as kid, f.required as required, f.length as length, f.reftypekid as reftypekid, f.reffieldname as reffieldname, f.enumvalues as enumvalues, f.cascadedelete as cascadedelete, f.autoset as autoset, f.associatedtypekid as associatedtypekid, f.associatedtypefieldname as associatedtypefieldname, f.formulareturntype as formulareturntype, f.formulauserdefinition as formulauserdefinition, f.formulaparseddefinition as formulaparseddefinition, type.kid as typekid, f.decimalplaces as decimalplaces, f.trackhistory as trackhistory, f.validateenum as validateenum, f.description as description, f.defaultvalue as defaultvalue, f.javatype as javatype, f.islong as islong, f.isformatted as isformatted, f.uchlabel as uchlabel, f.autonumberformat as autonumberformat, f.dictionary as dictionary FROM fields as f INNER JOIN types as type ON typeid = type.id";
	
	// private static final Logger log = LoggerFactory.getLogger(FieldDao.class);
	
	/**
	 * Transforms a SQL row set into a Field object.
	 * @param rowSet Row set retrieved from the database
	 * @param initTypes
	 * Tells whether types on inverse collection fields (inverse type), type references (type) and associations (linking type and associated type) should be initialized. This should be set to false when the method is called
	 * when all type data is read in from DB on environment initialization, because at that time not all types are yet available and not they cannot be initialized. 
	 * @param env
	 * @return Field object initialized with the SQL row set data
	 * @throws InvalidResultSetAccessException
	 * @throws KommetException
	 */
	public static Field getFieldFromSqlRowSetWithType (SqlRowSet rowSet, boolean initTypes, AppConfig appConfig, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		Field field = getFieldFromSqlRowSet(rowSet, initTypes, appConfig, env);
		field.getType().setKID(KID.get(rowSet.getString("typekid")));
		return field;
	}
	
	public static Field getFieldFromSqlRowSet (SqlRowSet rowSet, boolean initTypes, AppConfig appConfig, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		Field field = new Field();
		
		field.setApiName(rowSet.getString("apiname"));
		field.setLabel(rowSet.getString("label"));
		field.setUchLabel(rowSet.getString("uchlabel"));
		field.setCreated(rowSet.getDate("created"));
		field.setDataType(DataType.getById(rowSet.getInt("datatype")));
		field.setDbColumn(rowSet.getString("dbcolumn"));
		field.setId(rowSet.getLong("id"));
		field.setKID(KID.get(rowSet.getString("kid")));
		field.setRequired(rowSet.getBoolean("required"));
		field.setAutoSet(rowSet.getBoolean("autoset"));
		field.setTrackHistory(rowSet.getBoolean("trackhistory"));
		field.setDescription(rowSet.getString("description"));
		field.setDefaultValue(rowSet.getString("defaultvalue"));
		field.setDefaultValue(rowSet.getString("defaultvalue"));
		
		Type type = new Type();
		type.setId(rowSet.getLong("typeid"));
		
		field.setType(type);
		
		if (field.getDataType().getId().equals(DataType.ENUMERATION))
		{
			((EnumerationDataType)field.getDataType()).setValues(rowSet.getString("enumvalues"));
			((EnumerationDataType)field.getDataType()).setValidateValues(rowSet.getBoolean("validateenum"));
			
			String sDictionaryId = rowSet.getString("dictionary");
			
			if (StringUtils.hasText(sDictionaryId))
			{
				/*Dictionary dict = env.getDictionaries().get(KID.get(sDictionaryId));
				if (dict == null)
				{
					throw new KommetException("Dictionary " + sDictionaryId + " referenced by field " + field.getApiName() + " not found");
				}*/
				Dictionary dict = new Dictionary();
				dict.setId(KID.get(sDictionaryId));
				((EnumerationDataType)field.getDataType()).setDictionary(dict);
			}
		}
		else if (field.getDataType().getId().equals(DataType.MULTI_ENUMERATION))
		{
			Set<String> valueSet = new HashSet<String>();
			valueSet.addAll(MiscUtils.splitAndTrim(rowSet.getString("enumvalues"), "\n"));
			((MultiEnumerationDataType)field.getDataType()).setValues(valueSet);
		}
		else if (field.getDataType().getId().equals(DataType.NUMBER))
		{
			((NumberDataType)field.getDataType()).setDecimalPlaces(rowSet.getInt("decimalplaces"));
			((NumberDataType)field.getDataType()).setDecimalSeparator(appConfig.getDecimalSeparator());
			((NumberDataType)field.getDataType()).setJavaType(rowSet.getString("javatype"));
		}
		else if (field.getDataType().getId().equals(DataType.TEXT))
		{
			((TextDataType)field.getDataType()).setLength(rowSet.getInt("length"));
			((TextDataType)field.getDataType()).setLong(rowSet.getBoolean("islong"));
			((TextDataType)field.getDataType()).setFormatted(rowSet.getBoolean("isformatted"));
		}
		else if (field.getDataType().getId().equals(DataType.TYPE_REFERENCE))
		{
			TypeReference dt = ((TypeReference)field.getDataType());
			dt.setCascadeDelete(rowSet.getBoolean("cascadedelete"));
			dt.setTypeId(KID.get(rowSet.getString("reftypekid")));
			if (initTypes)
			{
				dt.setType(env.getType(dt.getTypeId()));
			}
		}
		else if (field.getDataType().getId().equals(DataType.INVERSE_COLLECTION))
		{
			InverseCollectionDataType dt = ((InverseCollectionDataType)field.getDataType());
			dt.setInverseTypeId(KID.get(rowSet.getString("reftypekid")));
			dt.setInverseProperty(rowSet.getString("reffieldname"));
			if (initTypes)
			{
				dt.setInverseType(env.getType(dt.getInverseTypeId()));
			}
		}
		else if (field.getDataType().getId().equals(DataType.ASSOCIATION))
		{
			AssociationDataType dt = ((AssociationDataType)field.getDataType());
			dt.setAssociatedTypeId(KID.get(rowSet.getString("associatedtypekid")));
			dt.setForeignLinkingField(rowSet.getString("associatedtypefieldname"));
			dt.setLinkingTypeId((KID.get(rowSet.getString("reftypekid"))));
			dt.setSelfLinkingField((rowSet.getString("reffieldname")));
			
			if (initTypes)
			{
				dt.setAssociatedType(env.getType(dt.getAssociatedTypeId()));
				dt.setLinkingType((env.getType(dt.getLinkingTypeId())));
			}
		}
		else if (field.getDataType().getId().equals(DataType.FORMULA))
		{
			((FormulaDataType)field.getDataType()).setReturnType(FormulaReturnType.values()[rowSet.getInt("formulareturntype")]);
			((FormulaDataType)field.getDataType()).setUserDefinition(rowSet.getString("formulauserdefinition"));
			((FormulaDataType)field.getDataType()).setParsedDefinition(rowSet.getString("formulaparseddefinition"));
		}
		else if (field.getDataType().getId().equals(DataType.AUTO_NUMBER))
		{
			((AutoNumber)field.getDataType()).setFormat(rowSet.getString("autonumberformat"));
		}
		
		return field;
	}
	
	/**
	 * Checks basic conditions that every field must fulfill in order to be saved.
	 * @param field
	 * @throws KommetPersistenceException
	 */
	private static void validateField (Field field) throws KommetException
	{
		if (field.getType() == null)
		{
			throw new KommetPersistenceException("Field cannot be inserted. Its type is null.");
		}
		else if (!field.getType().isPersisted())
		{
			throw new KommetPersistenceException("Field cannot be inserted. Its type is not persisted.");
		}
		else if (field.getType().getId() == null)
		{
			throw new KommetPersistenceException("Error saving field: type ID is null on field " + field.getApiName());
		}
		
		if (!StringUtils.hasText(field.getApiName()))
		{
			throw new ConstraintViolationException("API name of the new field is null");
		}
		
		if (!StringUtils.hasText(field.getLabel()))
		{
			throw new ConstraintViolationException("Label of the new field is null");
		}
		
		if (field.getDataType() == null)
		{
			throw new ConstraintViolationException("Data type of the new field is null");
		}
		
		if (!StringUtils.hasText(field.getDbColumn()))
		{
			throw new ConstraintViolationException("DB column of the new field is null");
		}
		
		if (field.getDataTypeId().equals(DataType.FORMULA) && field.isRequired())
		{
			throw new FieldDefinitionException("Formula field " + field.getApiName() + " cannot be made required");
		}
		
		if (StringUtils.hasText(field.getDescription()) && field.getDescription().length() > 512)
		{
			throw new FieldDefinitionException("Field description is longer than the allowed 512 characters");
		}
		
		validateDefaultValue(field);
		validateDataType(field);
	}
	
	private static void validateDataType(Field field) throws FieldDefinitionException
	{
		Integer dtId = field.getDataTypeId();
		
		if (DataType.ENUMERATION == dtId)
		{
			if (!StringUtils.hasText(((EnumerationDataType)field.getDataType()).getValues()) && ((EnumerationDataType)field.getDataType()).getDictionary() == null)
			{
				throw new FieldDefinitionException("Enumeration field " + field.getApiName() + " has empty value list and no dictionary assigned");
			}
			else if (StringUtils.hasText(((EnumerationDataType)field.getDataType()).getValues()) && ((EnumerationDataType)field.getDataType()).getDictionary() != null)
			{
				throw new FieldDefinitionException("Enumeration field " + field.getApiName() + " has both values and dictionary assigned");
			}
		}
		else if (DataType.MULTI_ENUMERATION == dtId)
		{
			if (((MultiEnumerationDataType)field.getDataType()).getValues().isEmpty())
			{
				throw new FieldDefinitionException("Multi-enumeration field " + field.getApiName() + " has empty value list");
			}
		}
	}

	private static void validateDefaultValue(Field field) throws FieldDefinitionException
	{
		if (field.getDefaultValue() == null)
		{
			// nothing to validate
			return;
		}
		
		if (field.getDataTypeId().equals(DataType.KOMMET_ID) || field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
		{
			try
			{
				// make sure the field value is a correct KID
				KID.get(field.getDefaultValue());
			}
			catch (KIDException e)
			{
				throw new FieldDefinitionException("Invalid KID in field's default value");
			}
		}
		else if (field.getDataType().isCollection())
		{
			throw new FieldDefinitionException("Collection field cannot have default values");
		}
		else if (field.getDataTypeId().equals(DataType.NUMBER))
		{
			// try to parse default value to number
			try
			{
				new BigDecimal(field.getDefaultValue());
			}
			catch (NumberFormatException e)
			{
				throw new FieldDefinitionException("Default field value " + field.getDefaultValue() + " is not a correct number");
			}
		}
		else if (field.getDataTypeId().equals(DataType.TEXT))
		{
			// make sure the value of the default field is not longer than the value of the text field
			if (((TextDataType)field.getDataType()).getLength() < field.getDefaultValue().length())
			{
				throw new FieldDefinitionException("Default value is longer than the maximum allowed length of the text field");
			}
		}
		else if (field.getDataTypeId().equals(DataType.DATE) || field.getDataTypeId().equals(DataType.DATETIME) || field.getDataTypeId().equals(DataType.BOOLEAN))
		{
			// make sure the default field is a correct date
			try
			{
				field.getDataType().getJavaValue(field.getDefaultValue());
			}
			catch (KommetException e)
			{
				throw new FieldDefinitionException("Incorrect default field value");
			}
		}
	}

	public Field update (Field field, EnvData env) throws KommetException
	{
		if (field.getKID() == null)
		{
			throw new KommetException("Attempt to update field with null Kommet ID");
		}
		
		if (field.getId() == null)
		{
			throw new KommetException("Attempt to update field with null ID");
		}
		
		validateField(field);
		
		DataType dt = field.getDataType();
		
		StringBuilder sql = new StringBuilder("UPDATE fields SET ");
		List<String> fieldUpdates = new ArrayList<String>();
		fieldUpdates.add("apiname = '" + field.getApiName() + "'");
		fieldUpdates.add("label = '" + MiscUtils.escapePostgresString(field.getLabel()) + "'");
		fieldUpdates.add("created = '" + MiscUtils.formatPostgresDateTime(field.getCreated()) + "'");
		fieldUpdates.add("datatype = '" + field.getDataType().getId() + "'");
		fieldUpdates.add("typeid = '" + field.getType().getId() + "'");
		fieldUpdates.add("dbcolumn = '" + field.getDbColumn() + "'");
		fieldUpdates.add("required = " + field.isRequired());
		fieldUpdates.add("trackhistory = " + field.isTrackHistory());
		fieldUpdates.add("description = " + (StringUtils.hasText(field.getDescription()) ? "'" + MiscUtils.escapePostgresString(field.getDescription()) + "'" : "null"));
		fieldUpdates.add("defaultvalue = " + (StringUtils.hasText(field.getDefaultValue()) ? "'" + MiscUtils.escapePostgresString(field.getDefaultValue()) + "'" : "null"));
		fieldUpdates.add("length = " + (field.getDataType().getId().equals(DataType.TEXT) ? ((TextDataType)field.getDataType()).getLength() : "null"));
		fieldUpdates.add("islong = " + (field.getDataType().getId().equals(DataType.TEXT) ? ((TextDataType)field.getDataType()).isLong() : "null"));
		fieldUpdates.add("isformatted = " + (field.getDataType().getId().equals(DataType.TEXT) ? ((TextDataType)field.getDataType()).isFormatted() : "null"));
		fieldUpdates.add("uchlabel = " + (StringUtils.hasText(field.getUchLabel()) ? "'" + field.getUchLabel() + "'" : "null"));
		
		String enumValues = null;
		if (dt.getId().equals(DataType.ENUMERATION))
		{
			enumValues = ((EnumerationDataType)dt).getValues();
		}
		else if (dt.getId().equals(DataType.MULTI_ENUMERATION))
		{
			// with multi-enum data type, null value are allowed
			enumValues = ((MultiEnumerationDataType)dt).getValues() != null ? MiscUtils.implode(((MultiEnumerationDataType)dt).getValues(), "\n") : null;
		} 
		
		if (dt.getId().equals(DataType.ENUMERATION) && ((EnumerationDataType)dt).getDictionary() != null)
		{
			fieldUpdates.add("dictionary = '" + ((EnumerationDataType)dt).getDictionary().getId() + "'");
		}
		else
		{
			fieldUpdates.add("dictionary = null");
		}
		
		fieldUpdates.add("enumvalues = " + (enumValues != null ? "'" + MiscUtils.escapePostgresString(enumValues) + "'" : "null"));
		fieldUpdates.add("formulareturntype = " + (field.getDataType().getId().equals(DataType.FORMULA) ? ((FormulaDataType)field.getDataType()).getReturnType().getId() : "null"));
		fieldUpdates.add("formulauserdefinition = " + (field.getDataType().getId().equals(DataType.FORMULA) ? ("'" + MiscUtils.escapePostgresString(((FormulaDataType)field.getDataType()).getUserDefinition()) + "'") : "null"));
		fieldUpdates.add("formulaparseddefinition = " + (field.getDataType().getId().equals(DataType.FORMULA) ? ("'" + MiscUtils.escapePostgresString(((FormulaDataType)field.getDataType()).getParsedDefinition()) + "'") : "null"));
		fieldUpdates.add("decimalplaces = " + (field.getDataType().getId().equals(DataType.NUMBER) ? ((NumberDataType)field.getDataType()).getDecimalPlaces() : "null"));
		fieldUpdates.add("javatype = " + (field.getDataType().getId().equals(DataType.NUMBER) ? "'" + ((NumberDataType)field.getDataType()).getJavaType() + "'" : "null"));
		
		// if data type is type reference, set the proper cascade delete property
		String cascadeDelete = "false";
		if (field.getDataType().getId().equals(DataType.TYPE_REFERENCE))
		{
			if (((TypeReference)field.getDataType()).isCascadeDelete())
			{
				cascadeDelete = "true";
			}
		}
		fieldUpdates.add("cascadedelete = " + cascadeDelete);
		fieldUpdates.add("autoset = " + field.isAutoSet());
		
		if (field.getDataTypeId().equals(DataType.ENUMERATION))
		{
			fieldUpdates.add("validateenum = " + ((EnumerationDataType)field.getDataType()).isValidateValues());
		}
		else
		{
			fieldUpdates.add("validateenum = false");
		}
		
		// if the data type is type reference, add KID of the referenced type type
		if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
		{
			fieldUpdates.add("reftypekid = '" + ((TypeReference)field.getDataType()).getType().getKID().getId() + "'");
			fieldUpdates.add("reffieldname = null");
		}
		else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
		{
			fieldUpdates.add("reftypekid = '" + ((InverseCollectionDataType)field.getDataType()).getInverseTypeId() + "'");
			fieldUpdates.add("reffieldname = '" + ((InverseCollectionDataType)field.getDataType()).getInverseProperty() + "'");
		}
		else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			fieldUpdates.add("reftypekid = '" + ((AssociationDataType)field.getDataType()).getLinkingTypeId() + "'");
			fieldUpdates.add("reffieldname = '" + ((AssociationDataType)field.getDataType()).getSelfLinkingField() + "'");
			fieldUpdates.add("associatedtypekid = '" + ((AssociationDataType)field.getDataType()).getAssociatedTypeId() + "'");
			fieldUpdates.add("associatedtypefieldname = '" + ((AssociationDataType)field.getDataType()).getForeignLinkingField() + "'");
		}
		else
		{
			fieldUpdates.add("reftypekid = null");
			fieldUpdates.add("reffieldname = null");
		}
		
		if (field.getDataTypeId().equals(DataType.AUTO_NUMBER))
		{
			String format = ((AutoNumber)field.getDataType()).getFormat();
			
			if (!StringUtils.hasText(format))
			{
				throw new FieldDefinitionException("AutoNumber format not defined");
			}
			
			// append auto-number format
			fieldUpdates.add("autonumberformat = '" + format + "'");
		}
		else
		{
			fieldUpdates.add("autonumberformat = null");
		}
		
		sql.append(MiscUtils.implode(fieldUpdates, ", "));
		sql.append(" WHERE id = " + field.getId());
		
		try
		{
			env.getJdbcTemplate().execute(sql.toString());
		}
		catch (DuplicateKeyException e)
		{
			throw new ConstraintViolationException("Constraint violation while updating field. Nested: " + e.getMessage());
		}
		
		return field;
	}
	
	public Field insert (Field field, EnvData env) throws KommetException
	{
		validateField(field);
		
		Long fieldSeq = settingService.getSettingAsLong(GlobalSettings.FIELD_SEQ, env);
		
		Date createdDate = new Date();
		KID id = KID.get(KID.FIELD_PREFIX, fieldSeq);
		
		StringBuilder sql = new StringBuilder("INSERT INTO fields (apiname, label, datatype, created, typeid, kid, dbcolumn, required, trackhistory, description, defaultvalue, validateenum, length, enumvalues, formulareturntype, formulauserdefinition, formulaparseddefinition, cascadedelete, autoset, reftypekid, reffieldname, associatedtypekid, associatedtypefieldname, decimalplaces, javatype, islong, isformatted, uchlabel, autonumberformat, dictionary) VALUES (");
		sql.append("'" + field.getApiName() + "', ");
		sql.append("'" + MiscUtils.escapePostgresString(field.getLabel()) + "', ");
		sql.append("'" + field.getDataType().getId() + "', ");
		sql.append("'" + MiscUtils.formatPostgresDateTime(createdDate) + "', ");
		sql.append("'" + field.getType().getId() + "', ");
		sql.append("'" + id + "', ");
		sql.append("'" + field.getDbColumn() + "', ");
		sql.append(field.isRequired() + ", ");
		sql.append(field.isTrackHistory() + ", ");
		sql.append((StringUtils.hasText(field.getDescription()) ? "'" + MiscUtils.escapePostgresString(field.getDescription()) + "'" : "null") + ", ");
		sql.append((StringUtils.hasText(field.getDefaultValue()) ? "'" + MiscUtils.escapePostgresString(field.getDefaultValue()) + "'" : "null") + ", ");
		sql.append((field.getDataTypeId().equals(DataType.ENUMERATION) ? ((EnumerationDataType)field.getDataType()).isValidateValues() : "null") + ", ");
		sql.append((field.getDataType().getId().equals(DataType.TEXT) ? ((TextDataType)field.getDataType()).getLength() : "null") + ", ");
		
		String enumValues = null;
		if (field.getDataType().getId().equals(DataType.ENUMERATION))
		{
			enumValues = ((EnumerationDataType)field.getDataType()).getValues();
		}
		else if (field.getDataType().getId().equals(DataType.MULTI_ENUMERATION))
		{
			enumValues = ((MultiEnumerationDataType)field.getDataType()).getValues() != null ? MiscUtils.implode(((MultiEnumerationDataType)field.getDataType()).getValues(), "\n") : null;
		} 
			
		sql.append((enumValues != null ? "'" + MiscUtils.escapePostgresString(enumValues) + "'" : "null") + ", ");
		sql.append(field.getDataType().getId().equals(DataType.FORMULA) ? ((FormulaDataType)field.getDataType()).getReturnType().getId() : "null");
		sql.append(", ");
		sql.append(field.getDataType().getId().equals(DataType.FORMULA) ? ("'" + MiscUtils.escapePostgresString(((FormulaDataType)field.getDataType()).getUserDefinition()) + "'") : "null");
		sql.append(", ");
		sql.append(field.getDataType().getId().equals(DataType.FORMULA) ? ("'" + MiscUtils.escapePostgresString(((FormulaDataType)field.getDataType()).getParsedDefinition()) + "'") : "null");
		sql.append(", ");
		
		// if data type is type reference, set the proper cascade delete property
		String cascadeDelete = "false";
		if (field.getDataType().getId().equals(DataType.TYPE_REFERENCE))
		{
			if (((TypeReference)field.getDataType()).isCascadeDelete())
			{
				cascadeDelete = "true";
			}
		}
		sql.append(cascadeDelete + ", ");
		sql.append(field.isAutoSet() + ", ");
		
		if (field.getDataTypeId().equals(DataType.NUMBER))
		{
			sql.append("null, null, null, null, ").append(((NumberDataType)field.getDataType()).getDecimalPlaces());
			sql.append(", '").append(((NumberDataType)field.getDataType()).getJavaType()).append("'");
		}
		// if the data type is type reference, add KID of the referenced type type
		else if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
		{
			KID refTypeId = ((TypeReference)field.getDataType()).getType().getKID();
			if (refTypeId == null)
			{
				throw new KommetException("Field " + field.getApiName() + " has data type type reference, but the ID of the referenced type is null");
			}
			
			sql.append("'").append(refTypeId.getId()).append("', null, null, null, null, null");
		}
		// if the data type is an inverse collection, add KID of the referenced type type and the inverse binding property
		else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
		{
			KID refTypeId = ((InverseCollectionDataType)field.getDataType()).getInverseTypeId();
			if (refTypeId == null)
			{
				throw new KommetException("Field " + field.getApiName() + " has data type inverse collection, but the ID of the referenced type is null");
			}
			sql.append("'").append(refTypeId.getId()).append("', ");
			
			String inverseProperty = ((InverseCollectionDataType)field.getDataType()).getInverseProperty();
			if (!StringUtils.hasText(inverseProperty))
			{
				throw new KommetException("Field " + field.getApiName() + " has data type inverse collection, but the inverse property is not set");
			}
			sql.append("'").append(inverseProperty).append("', null, null, null, null");
		}
		// if the data type is an inverse collection, add KID of the referenced type type and the inverse binding property
		else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			KID linkingTypeId = ((AssociationDataType)field.getDataType()).getLinkingTypeId();
			if (linkingTypeId == null)
			{
				throw new KommetException("Field " + field.getApiName() + " has data type association, but the ID of the linking type type is null");
			}
			
			// get linking type
			Type linkingType = env.getType(linkingTypeId);
			if (linkingType == null)
			{
				throw new KommetException("Linking type with ID " + linkingTypeId + " for association " + field.getApiName() + " on type " + field.getType().getQualifiedName() + " not found ");
			}
			
			KID associatedTypeId = ((AssociationDataType)field.getDataType()).getAssociatedTypeId();
			if (associatedTypeId == null)
			{
				throw new KommetException("Field " + field.getApiName() + " has data type association, but the ID of the associated type type is null");
			}
			
			sql.append("'").append(linkingTypeId.getId()).append("', ");
			
			String selfLinkingFieldName = ((AssociationDataType)field.getDataType()).getSelfLinkingField();
			if (!StringUtils.hasText(selfLinkingFieldName))
			{
				throw new KommetException("Field " + field.getApiName() + " has data type association, but the self linking field is not set");
			}
			else
			{
				Field selfLinkingField = linkingType.getField(selfLinkingFieldName);
				if (selfLinkingField == null)
				{
					throw new FieldDefinitionException("Field " + selfLinkingFieldName + " does not exist on type " + linkingType.getQualifiedName());
				}
				
				if (selfLinkingField.getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					TypeReference selfTypeRef = ((TypeReference)selfLinkingField.getDataType());
					// make sure that the self linking field on the linking table links back to the current type
					if (!selfTypeRef.getTypeId().equals(field.getType().getKID()))
					{
						throw new KommetException("Self linking field " + selfLinkingFieldName + " on linking type " + linkingType.getQualifiedName() + " should link back to type " + field.getType().getQualifiedName() + ", but links to type " + selfTypeRef.getType().getQualifiedName());
					}
				}
				else
				{
					throw new KommetException("Self linking field on linking type " + linkingType.getQualifiedName() + " must be of data type type reference, but is " + linkingType.getField(selfLinkingFieldName).getDataType().getName());
				}
			}
			sql.append("'").append(selfLinkingFieldName).append("', ");
			
			// add values for the associated type and linking field
			sql.append("'").append(associatedTypeId.getId()).append("', ");
			
			String foreignLinkingFieldName = ((AssociationDataType)field.getDataType()).getForeignLinkingField();
			if (!StringUtils.hasText(foreignLinkingFieldName))
			{
				throw new KommetException("Field " + field.getApiName() + " has data type association, but the foreign linking field is not set");
			}
			else
			{
				Field foreignLinkingField = linkingType.getField(foreignLinkingFieldName);
				
				if (foreignLinkingField == null)
				{
					throw new FieldDefinitionException("Field " + foreignLinkingFieldName + " does not exist on type " + linkingType.getQualifiedName());
				}
				
				if (foreignLinkingField.getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					TypeReference foreignTypeRef = ((TypeReference)foreignLinkingField.getDataType());
					// make sure that the foreign linking field on the linking table links back to the current type
					if (!foreignTypeRef.getTypeId().equals(associatedTypeId))
					{
						throw new KommetException("Foreign linking field " + foreignLinkingFieldName + " on linking type " + linkingType.getQualifiedName() + " should link to the associated type " + env.getType(associatedTypeId).getQualifiedName() + ", but links to type " + foreignTypeRef.getType().getQualifiedName());
					}
				}
				else
				{
					throw new KommetException("Foreign linking field on linking type " + linkingType.getQualifiedName() + " must be of data type type reference, but is " + linkingType.getField(foreignLinkingFieldName).getDataType().getName());
				}
			}
			sql.append("'").append(foreignLinkingFieldName).append("', null, null");
		}
		else
		{
			sql.append("null, null, null, null, null, null");
		}
		
		// append text field properties - isLong and isFormatted
		if (field.getDataTypeId().equals(DataType.TEXT))
		{
			sql.append(", ").append(((TextDataType)field.getDataType()).isLong());
			sql.append(", ").append(((TextDataType)field.getDataType()).isFormatted());
		}
		else
		{
			sql.append(", null, null");
		}
		
		// append uchlabel
		sql.append(", ").append(StringUtils.hasText(field.getUchLabel()) ? "'" + field.getUchLabel() + "'" : "null");
		
		if (field.getDataTypeId().equals(DataType.AUTO_NUMBER))
		{
			String format = ((AutoNumber)field.getDataType()).getFormat();
			
			if (!StringUtils.hasText(format))
			{
				throw new FieldDefinitionException("AutoNumber format not defined");
			}
			
			// append auto-number format
			sql.append(", '").append(format).append("'");
		}
		else
		{
			sql.append(", null");
		}
		
		if (field.getDataTypeId().equals(DataType.ENUMERATION) && ((EnumerationDataType)field.getDataType()).getDictionary() != null)
		{
			sql.append(", '").append(((EnumerationDataType)field.getDataType()).getDictionary().getId()).append("'");
		}
		else
		{
			sql.append(", null");
		}
		
		sql.append(") RETURNING id");
		
		try
		{
			Long newId = env.getJdbcTemplate().queryForObject(sql.toString(), Long.class);
			field.setId(newId);
		}
		catch (DuplicateKeyException e)
		{
			throw new ConstraintViolationException("Constraint violation while inserting field. Nested: " + e.getMessage());
		}
		
		// increase the counter of fields
		settingService.setSetting(GlobalSettings.FIELD_SEQ, String.valueOf(fieldSeq + 1), env);
		
		// initialize fields on the new object
		field.setCreated(createdDate);
		field.setKID(id);
		
		return field;
	}

	/**
	 * Creates the actual DB column representing field data on the table representing the type this field
	 * belongs to.
	 * @param field field object for which column will be created
	 * @param env
	 * @throws KommetException
	 */
	public void createDbColumnForField (Field field, EnvData env) throws KommetException
	{
		StringBuilder sql = new StringBuilder();
		sql.append("ALTER TABLE " + field.getType().getDbTable() + " ADD COLUMN " + field.getDbColumn() + " " + DataType.getPostgresType(field.getDataType()));
		
		if (field.getDataTypeId().equals(DataType.AUTO_NUMBER))
		{
			String format = ((AutoNumber)field.getDataType()).getFormat();
			String prefix = null;
			Integer padChars = 0;
			
			Matcher m = AutoNumber.AUTONUMBER_PATTERN.matcher(format);
			
			if (m.find())
			{
				prefix = m.group(1);
				
				if (m.groupCount() < 2)
				{
					throw new KommetException("Autonumber format does not contain numeric part definition");
				}
				else
				{
					padChars = m.group(2).length() - 2;
				}
			}
			else
			{
				throw new KommetException("Incorrect auto-number format " + format);
			}
			
			// add computing default auto-number value for this field
			Type type = env.getType(field.getType().getKID());
			sql.append(" DEFAULT autonumber('" + prefix + "', nextval('" + type.getAutonumberSeqName() + "'::regclass), " + padChars + ")");
		}
		
		if (field.isRequired())
		{
			sql.append(" NOT NULL");
		}
		
		// if it's an type reference, then we need to create a foreign key for it
		if (field.getDataType().getId().equals(DataType.TYPE_REFERENCE))
		{
			sql.append(", ").append(getObjectReferenceConstraintSql(field));
		}
		
		try
		{
			env.getJdbcTemplate().update(sql.toString());
		}
		catch (Exception e)
		{
			throw new KommetException("Error creating DB column '" + field.getDbColumn() + "' on table + '" + field.getType().getDbTable() + "'. Nested: " + e.getMessage());
		}
	}
	
	private static String getObjectReferenceConstraintSql (Field field)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("ADD CONSTRAINT " + field.getDbColumn() + "_" + field.getType().getDbTable() + "_fkey FOREIGN KEY (" + field.getDbColumn() + ")");
		sql.append(" REFERENCES " + ((TypeReference)field.getDataType()).getType().getDbTable() + " (" + Field.ID_FIELD_DB_COLUMN + ") MATCH SIMPLE");
			
		// if the referencing object can exist without the referenced one, then nullify the column
		// on the referencing object when the referenced one is deleted
		String onDeleteAction = ((TypeReference)field.getDataType()).isCascadeDelete() ? "CASCADE" : "SET NULL";
			
		sql.append(" ON UPDATE NO ACTION ON DELETE " + onDeleteAction + ";");
		return sql.toString();
	}

	public void makeFieldRequired(Field field, boolean isRequired, EnvData env)
	{
		String sql = "ALTER TABLE " + field.getType().getDbTable() + " ALTER COLUMN " + field.getDbColumn() + (isRequired ? " SET " : " DROP ") + "NOT NULL";
		
		// update field metadata
		env.getJdbcTemplate().update(sql + "; UPDATE fields SET required = " + isRequired + " WHERE kid = '" + field.getKID() + "'");
	}

	public Field getByKID(KID rid, AppConfig appConfig, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		if (rid == null)
		{
			throw new KommetException("Attempt to retrieve field by null ID");
		}
		
		String fieldQuery = FieldDao.FIELD_SQL_QUERY + " WHERE kid = '" + rid.getId() + "' LIMIT 1";
		SqlRowSet fields = env.getJdbcTemplate().queryForRowSet(fieldQuery);
		
		while (fields.next())
		{
			return getFieldFromSqlRowSet(fields, true, appConfig, env);
		}
		
		return null;
	}

	/**
	 * Updates the column definition in the database.
	 * @param field
	 * @param oldColumnName
	 * @param env
	 * @throws KommetException 
	 */
	public void updateDbColumnForField(Field field, Field oldField, EnvData env) throws KommetException
	{
		StringBuilder sql = new StringBuilder();
		
		if (!field.getDbColumn().equals(oldField.getDbColumn()))
		{
			// if column name has changed, update it
			sql.append("ALTER TABLE " + field.getType().getDbTable() + " RENAME COLUMN " + oldField.getDbColumn() + " TO " + field.getDbColumn()).append("; ");
		}
		
		String alterColumnSql = "ALTER TABLE " + field.getType().getDbTable() + " ALTER COLUMN " + field.getDbColumn();
		
		boolean updateType = false;
		
		// check if text field length has changed
		if (oldField.getDataType().getId().equals(DataType.TEXT) && field.getDataType().getId().equals(DataType.TEXT) && !( ((TextDataType)oldField.getDataType()).getLength().equals(((TextDataType)field.getDataType()).getLength())))
		{
			updateType = true;
		}
		
		// if type changed, update it too
		if (!oldField.getDataType().getId().equals(field.getDataType().getId()) || updateType)
		{
			sql.append(alterColumnSql + " TYPE " + DataType.getPostgresType(field.getDataType()));
			sql.append("; ");
		}
		
		String operationOnRequired = null;
		if (field.isRequired() && !oldField.isRequired())
		{
			operationOnRequired = "SET";
		}
		else if (!field.isRequired() && oldField.isRequired())
		{
			operationOnRequired = "DROP";
		}
		
		if (operationOnRequired != null)
		{
			sql.append(alterColumnSql).append(" ").append(operationOnRequired).append(" NOT NULL");
			sql.append("; ");
		}
		
		// update autonumber definition - do not check if it changed, just update it always
		if (field.getDataTypeId().equals(DataType.AUTO_NUMBER))
		{
			String format = ((AutoNumber)field.getDataType()).getFormat();
			String prefix = null;
			Integer padChars = 0;
			
			Matcher m = AutoNumber.AUTONUMBER_PATTERN.matcher(format);
			
			if (m.find())
			{
				prefix = m.group(1);
				
				if (m.groupCount() < 2)
				{
					throw new KommetException("Autonumber format does not contain numeric part definition");
				}
				else
				{
					padChars = m.group(2).length() - 2;
				}
			}
			else
			{
				throw new KommetException("Incorrect auto-number format " + format);
			}
			
			// add computing default auto-number value for this field
			Type type = env.getType(field.getType().getKID());
			sql.append("ALTER TABLE ").append(field.getType().getDbTable()).append(" ALTER COLUMN ").append(field.getDbColumn()).append(" SET DEFAULT autonumber('" + prefix + "', nextval('" + type.getAutonumberSeqName() + "'::regclass), " + padChars + ")");
		}
		
		// if it's an type reference, then we need to create a foreign key for it
		if (field.getDataType().getId().equals(DataType.TYPE_REFERENCE) && !oldField.getDataType().getId().equals(DataType.TYPE_REFERENCE))
		{
			sql.append("ALTER TABLE ").append(field.getType().getDbTable()).append(" ");
			sql.append(getObjectReferenceConstraintSql(field));
		}
		else if (!field.getDataType().getId().equals(DataType.TYPE_REFERENCE) && oldField.getDataType().getId().equals(DataType.TYPE_REFERENCE))
		{
			throw new NotImplementedException("Changing field type from object ref. to another type is not implemented");
		}
		
		//log.debug("Updating DB field definition: " + sql.toString());
		
		env.getJdbcTemplate().execute(sql.toString());
	}

	public void delete(Field field, EnvData env)
	{
		env.getJdbcTemplate().execute("DELETE FROM fields WHERE kid = '" + field.getKID() + "'");
	}

	public void deleteDbColumnForField(Field field, EnvData env)
	{
		env.getJdbcTemplate().execute("ALTER TABLE " + field.getType().getDbTable() + " DROP COLUMN " + field.getDbColumn());
	}

	/**
	 * Retrieves fields from database (not from environment data) basing on filter criteria.
	 * @param filter Filter holding criteria according to which fields will be retrieved.
	 * @param env
	 * @return
	 * @throws InvalidResultSetAccessException
	 * @throws KommetException
	 */
	public List<Field> getFields (FieldFilter filter, AppConfig appConfig, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		if (filter == null)
		{
			filter = new FieldFilter();
		}
		
		List<String> conditions = new ArrayList<String>();
		
		if (filter.getDataType() != null)
		{
			conditions.add("datatype = " + filter.getDataType().getId());
			
			if (filter.getDataType().getId().equals(DataType.TYPE_REFERENCE))
			{
				if (filter.getObjectRefTypeId() != null)
				{
					conditions.add("reftypekid = '" + filter.getObjectRefTypeId().getId() + "'");
				}
			}
			else if (filter.getDataType().getId().equals(DataType.ASSOCIATION))
			{
				if (filter.getAssociatedTypeId() != null)
				{
					conditions.add("associatedtypekid = '" + filter.getAssociatedTypeId().getId() + "'");
				}
			}
		}
		
		if (StringUtils.hasText(filter.getApiName()))
		{
			conditions.add("f.apiname = '" + filter.getApiName() + "'");
		}
		
		if (StringUtils.hasText(filter.getTypeQualifiedName()))
		{
			String typeApiName = filter.getTypeQualifiedName().substring(filter.getTypeQualifiedName().lastIndexOf('.') + 1);
			String typePackageName = filter.getTypeQualifiedName().substring(0, filter.getTypeQualifiedName().lastIndexOf('.'));
			
			//conditions.add("f.typeid IN (SELECT id FROM types t1 WHERE t1.apiname = ' " + typeApiName + "' and t1.package = '" + typePackageName + "')");
			conditions.add("type.apiname = '" + typeApiName + "' and type.package = '" + typePackageName + "'");
		}
		
		if (filter.getFormulaFieldId() != null)
		{
			// find formula fields using this field
			conditions.add("f.formulaparseddefinition like '%field{" + filter.getFormulaFieldId() + "}%'");
		}
		
		if (filter.getDictionaryId() != null)
		{
			// find enum fields using a specific dictionary
			conditions.add("f.dictionary = '" + filter.getDictionaryId() + "'");
		}
		
		if (filter.getTypeId() != null)
		{
			conditions.add("f.typeid = " + filter.getTypeId());
		}
		
		if (filter.getTypeKID() != null)
		{
			//conditions.add("f.typeid IN (SELECT id FROM types t2 WHERE t2.kid = ' " + filter.getTypeKID().getId() + "')");
			conditions.add("type.kid = '" + filter.getTypeKID() + "'");
		}
		
		String fieldQuery = FieldDao.FIELD_WITH_TYPE_SQL_QUERY;
		
		if (!conditions.isEmpty())
		{
			fieldQuery += " WHERE " + MiscUtils.implode(conditions, " AND ");
		}
		
		//System.out.println("\n\n" + fieldQuery);
		
		SqlRowSet fieldRowSet = env.getJdbcTemplate().queryForRowSet(fieldQuery);
		List<Field> results = new ArrayList<Field>();
		
		while (fieldRowSet.next())
		{
			Field field = getFieldFromSqlRowSetWithType(fieldRowSet, true, appConfig, env);
			results.add(field);
		}
		
		return results;
	}
}