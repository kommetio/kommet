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

import javax.inject.Inject;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.dao.ConstraintViolationException;
import kommet.dao.FieldDao;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.services.GlobalSettingsService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;
import kommet.utils.ValidationUtil;

@Repository
public class TypeDao
{
	@Inject
	GlobalSettingsService settingService;
	
	@Inject
	AppConfig appConfig;
	
	private static final String TYPE_QUERY = "SELECT id, apiname, description, label, plurallabel, created, dbtable, package, kid, keyprefix, isbasic, defaultfield, sharingcontrolledbyfield, combinerecordandcascadesharing, isdeclaredincode, uchlabel, uchplurallabel, isautolinkingtype, autonumberfieldid FROM types";
	
	/**
	 * Updates a type.
	 * 
	 * Only some fields can be updated on a type. Others are unmodifiable and if an update is attempted, an exception is thrown.
	 * The only modifiable fields are:
	 * <ul>
	 * <li>API name</li>
	 * <li>label</li>
	 * <li>plural label</li>
	 * </ul>
	 * 
	 * @param updatedType
	 * @param originalType
	 * @param userId
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public Type update (Type updatedType, Type originalType, AuthData authData, EnvData env) throws KommetException
	{
		validateType(updatedType);
		validateTypeModifications(updatedType, originalType);
		
		StringBuilder sql = new StringBuilder("UPDATE types SET ");
		
		List<String> fieldChanges = new ArrayList<String>();
		fieldChanges.add("apiname = '" + updatedType.getApiName() + "'");
		fieldChanges.add("label = '" + updatedType.getLabel() + "'");
		fieldChanges.add("description = " + (StringUtils.hasText(updatedType.getDescription()) ? "'" + updatedType.getDescription() + "'" : "null") + "");
		fieldChanges.add("plurallabel = '" + updatedType.getPluralLabel() + "'");
		fieldChanges.add("package = '" + updatedType.getPackage() + "'");
		fieldChanges.add("created  = '" + MiscUtils.formatPostgresDateTime(updatedType.getCreated()) + "'");
		fieldChanges.add("isbasic = " + updatedType.isBasic());
		fieldChanges.add("defaultfield = " + (updatedType.getDefaultFieldId() != null ? "'" + updatedType.getDefaultFieldId() + "'" : "null"));
		fieldChanges.add("sharingcontrolledbyfield = " + (updatedType.getSharingControlledByFieldId() != null ? ("'" + updatedType.getSharingControlledByFieldId() + "'") : "null"));
		fieldChanges.add("combinerecordandcascadesharing = " + updatedType.isCombineRecordAndCascadeSharing());
		fieldChanges.add("isdeclaredincode = " + updatedType.isDeclaredInCode());
		fieldChanges.add("uchlabel = " + (updatedType.getUchLabel() != null ? "'" + updatedType.getUchLabel() + "'" : "null"));
		fieldChanges.add("uchplurallabel = " + (updatedType.getUchPluralLabel() != null ? "'" + updatedType.getUchPluralLabel() + "'" : "null"));
		fieldChanges.add("isautolinkingtype = " + updatedType.isAutoLinkingType());
		fieldChanges.add("autonumberfieldid = " + (updatedType.getAutoNumberFieldId() != null ? "'" + updatedType.getAutoNumberFieldId() + "'" : "null"));
		
		sql.append(MiscUtils.implode(fieldChanges, ", "));
		sql.append(" WHERE ").append(Field.ID_FIELD_DB_COLUMN).append(" = '").append(originalType.getKID().getId()).append("'");
		
		try
		{
			// execute the update
			env.getJdbcTemplate().execute(sql.toString());
		}
		catch (DuplicateKeyException e)
		{
			throw new ConstraintViolationException("Constraint violation while inserting type. Nested: " + e.getMessage());
		}
		
		return updatedType;
	}
	
	/**
	 * Checks if none of the unmodifiable properties of a KObject has changed.
	 * @param newType
	 * @param originalType
	 * @throws KommetException
	 */
	private static void validateTypeModifications(Type newType, Type originalType) throws KommetException
	{	
		// make sure none of the unmodifiable properties has changed
		if (!newType.getKeyPrefix().equals(originalType.getKeyPrefix()))
		{
			throw new KommetException("Key prefix on type " + originalType.getApiName() + " has been changed, which is not allowed");
		}
		
		if (!originalType.getDbTable().equals(newType.getDbTable()))
		{
			throw new KommetException("Trying to modify DB table name on type " + originalType.getApiName() + " from '" + originalType.getDbTable() + "' to '" + newType.getDbTable() + "'");
		}
		
		if (originalType.getCreated().compareTo(newType.getCreated()) != 0)
		{
			throw new KommetException("Trying to change created date on type " + originalType.getApiName() + " from " + MiscUtils.formatGMTDate(originalType.getCreated(), "dd:MM:yyyy hh:mm:ss.SSS") + " to " + MiscUtils.formatGMTDate(newType.getCreated(), "dd:MM:yyyy hh:mm:ss.SSS"));
		}
		
		if (!originalType.getKID().equals(newType.getKID()))
		{
			throw new KommetException("Trying to change ID on type " + originalType.getApiName());
		}
	}

	@SuppressWarnings("deprecation")
	public Type insert (Type type, EnvData env) throws KommetException
	{
		validateNewType(type);
		
		KeyPrefix generatedKeyPrefix = null;
		KID id = null;
		Date createdDate = null;
		Long typeSeq = null;
		
		// If it's a basic type, its key prefix and kid will be already assigned
		// And the sequence will not changed after adding a basic object
		if (!type.isBasic())
		{
			typeSeq = settingService.getSettingAsLong(GlobalSettings.TYPE_SEQ, env);
			generatedKeyPrefix = KeyPrefix.get(typeSeq);
			id = KID.get(KID.TYPE_PREFIX, typeSeq);
			createdDate = new Date();
		}
		else
		{
			// get the values from the object
			generatedKeyPrefix = type.getKeyPrefix();
			id = type.getKID();
			createdDate = new Date(1900, 1, 1);
		}
		
		// DB tables will be named from the key prefixes. Naming them the same as API name would cause
		// inconsistencies when the object's API name changes.
		type.setDbTable("obj_" + generatedKeyPrefix);
		
		StringBuilder sql = new StringBuilder("INSERT INTO types (apiname, label, description, plurallabel, package, kid, keyprefix, created, dbtable, isbasic, defaultfield, sharingcontrolledbyfield, combinerecordandcascadesharing, isdeclaredincode, uchlabel, uchplurallabel, isautolinkingtype, autonumberfieldid) VALUES (");
		sql.append("'" + type.getApiName() + "', ");
		sql.append("'" + type.getLabel() + "', ");
		sql.append(StringUtils.hasText(type.getDescription()) ? "'" + type.getDescription() + "'" : "null").append(", ");
		sql.append("'" + type.getPluralLabel() + "', ");
		sql.append("'" + type.getPackage() + "', ");
		sql.append("'" + id + "', ");
		sql.append("'" + generatedKeyPrefix + "', ");
		sql.append("'" + MiscUtils.formatPostgresDateTime(createdDate) + "', ");
		sql.append("'" + type.getDbTable() + "', ");
		sql.append(type.isBasic() + ", ");
		sql.append("" + (type.getDefaultFieldId() != null ? "'" + type.getDefaultFieldId() + "'" : "null") + ", ");
		sql.append("" + (type.getSharingControlledByFieldId() != null ? "'" + type.getSharingControlledByFieldId().getId() + "'" : "null") + ", ");
		sql.append(type.isCombineRecordAndCascadeSharing()).append(", ");
		sql.append(type.isDeclaredInCode()).append(", ");
		sql.append(StringUtils.hasText(type.getUchLabel()) ? "'" + type.getUchLabel() + "'" : "null").append(", ");
		sql.append(StringUtils.hasText(type.getUchPluralLabel()) ? "'" + type.getUchPluralLabel() + "'" : "null").append(", ");
		sql.append(type.isAutoLinkingType()).append(", ");
		sql.append("" + (type.getAutoNumberFieldId() != null ? "'" + type.getAutoNumberFieldId() + "'" : "null"));
		sql.append(") RETURNING id");
		
		try
		{
			Long newId = env.getJdbcTemplate().queryForObject(sql.toString(), Long.class);
			type.setId(newId);
		}
		catch (DuplicateKeyException e)
		{
			throw new ConstraintViolationException("Constraint violation while inserting type. Nested: " + e.getMessage());
		}
		
		if (!BasicSetupService.isBasicType(type))
		{
			// increase the counter of types
			settingService.setSetting(GlobalSettings.TYPE_SEQ, String.valueOf(typeSeq + 1), env);
		}
		
		// init fields on the new type
		type.setCreated(createdDate);
		type.setKeyPrefix(generatedKeyPrefix);
		type.setKID(id);
		
		return type;
	}
	
	private static void validateNewType (Type type) throws KommetException
	{
		if (type.getDbTable() != null)
		{
			throw new KommetException("DB table must not be set manually. It will be set to the value of the object's key prefix.");
		}
		
		validateType(type);
	}
	
	private static void validateType (Type type) throws TypeDefinitionException
	{	
		if (!StringUtils.hasText(type.getApiName()))
		{
			throw new TypeDefinitionException("API name of the inserted type is empty");
		}
		
		if (!ValidationUtil.isValidTypeApiName(type.getApiName()))
		{
			throw new TypeDefinitionException("API name " + type.getApiName() + " is not valid. API names must start with an uppercase letter and can contain only letters, digits and an underscore, and must not end with an underscore");
		}
		
		if (StringUtils.hasText(type.getPackage()) && !ValidationUtil.isValidPackageName(type.getPackage()))
		{
			throw new TypeDefinitionException("Package name " + type.getApiName() + " is not valid.");
		}
		
		if (!StringUtils.hasText(type.getLabel()))
		{
			throw new TypeDefinitionException("Label of the inserted type is empty");
		}
		
		if (!StringUtils.hasText(type.getPluralLabel()))
		{
			throw new TypeDefinitionException("Plural label of the inserted type is empty");
		}
		
		if (!StringUtils.hasText(type.getPackage()))
		{
			throw new TypeDefinitionException("Package name of the inserted type is empty");
		}
	}

	public void createDbTableForType(Type type, EnvData env) throws KommetException
	{
		String ridColumnDef = "kid character varying(13) DEFAULT next_kolmu_id('" + type.getKeyPrefix() + "', nextval('" + type.getKIDSeqName() + "'::regclass))"; 
		
		StringBuilder sql = new StringBuilder(getKIDSeqCreateSQL(type.getKIDSeqName()) + "; ");
		
		// create sequence for autonumber
		sql.append(getKIDSeqCreateSQL(type.getAutonumberSeqName())).append("; ");
		
		// column _triggerflag has to have at least 13 characters, because the longest value we want to store in it is "EDITDELETEALL"
		sql.append("CREATE TABLE " + type.getDbTable() + "(id bigserial not null, " + ridColumnDef + ", " + Field.TRIGGER_FLAG_DB_COLUMN + " character varying(13), CONSTRAINT " + type.getDbTable() + "_pkey PRIMARY KEY (id))");
		sql.append("WITH (OIDS=FALSE);");
		
		// add a unique constraint on the id and kid fields
		sql.append("ALTER TABLE " + type.getDbTable() + " ");
		sql.append(" ADD CONSTRAINT " + type.getDbTable() + "_" + Field.ID_FIELD_DB_COLUMN + "_unique_key UNIQUE(" + Field.ID_FIELD_DB_COLUMN + "); ");
		
		sql.append("ALTER TABLE " + type.getDbTable() + " OWNER TO " + appConfig.getEnvDBUser() + ";");
		
		// add index on the kid column
		sql.append("CREATE INDEX " + type.getDbTable() + "_" + Field.ID_FIELD_DB_COLUMN + " ON " + type.getDbTable() + " USING btree (" + Field.ID_FIELD_DB_COLUMN + ");");
		
		try
		{
			env.getJdbcTemplate().update(sql.toString());
		}
		catch (Exception e)
		{
			throw new KommetException("Error creating DB table '" + type.getDbTable() + "' for new type. Nested: " + e.getMessage());
		}
	}

	/**
	 * Return an SQL statement that creates a new sequence based on which new KIDs will be generated for this KObject.
	 * Since type's API name may change, the sequence name will be based on its key prefix.
	 * @param keyPrefix
	 * @return
	 * @throws PropertyUtilException 
	 */
	private String getKIDSeqCreateSQL(String seqName) throws PropertyUtilException
	{
		String sql = "CREATE SEQUENCE " + seqName + " INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1; ALTER TABLE " + seqName + " OWNER TO " + appConfig.getEnvDBUser();
		return sql;
	}

	public Type getByKID (KID rid, AppConfig appConfig, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		TypeFilter filter = new TypeFilter();
		filter.addKID(rid);
		List<Type> types = getTypes(filter, appConfig, env, true);
		
		if (types.isEmpty())
		{
			return null;
		}
		else if (types.size() > 1)
		{
			throw new KommetException("More than one type found with ID " + rid);
		}
		else
		{
			return types.get(0);
		}
	}
	
	public Type getTypeByApiName (String apiName, AppConfig appConfig, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		TypeFilter filter = new TypeFilter();
		filter.setApiName(apiName);
		List<Type> types = getTypes(filter, appConfig, env, true);
		
		if (types.isEmpty())
		{
			return null;
		}
		else if (types.size() > 1)
		{
			throw new KommetException("More than one type found with API name " + apiName);
		}
		else
		{
			return types.get(0);
		}
	}

	public List<Type> getTypes (TypeFilter filter, AppConfig appConfig, EnvData env, boolean initFields) throws KommetException
	{
		String sql = TYPE_QUERY;
		
		List<String> conditions = new ArrayList<String>();
		
		if (filter == null)
		{
			filter = new TypeFilter();
		}
		
		if (filter.getKIDs() != null && !filter.getKIDs().isEmpty())
		{
			conditions.add("kid IN (" + MiscUtils.implode(filter.getKIDs(), ", ", "'", null) + ")");
		}
		else if (filter.getIds() != null && !filter.getIds().isEmpty())
		{
			conditions.add("id IN (" + MiscUtils.implode(filter.getIds(), ", ") + ")");
		}
		
		if (StringUtils.hasText(filter.getApiName()) && StringUtils.hasText(filter.getQualifiedName()))
		{
			throw new KommetException("Cannot search types both by API name and qualified name");
		}
		
		if (StringUtils.hasText(filter.getApiName()))
		{
			conditions.add("apiName = '" + filter.getApiName() + "'");
		}
		
		if (StringUtils.hasText(filter.getQualifiedName()))
		{
			String apiName = filter.getQualifiedName().substring(filter.getQualifiedName().lastIndexOf('.') + 1);
			String packageName = filter.getQualifiedName().substring(0, filter.getQualifiedName().lastIndexOf('.'));
			conditions.add("apiName = '" + apiName + "'");
			conditions.add("package = '" + packageName + "'");
		}
		
		if (filter.getIsBasic() != null)
		{
			conditions.add("isbasic = " + filter.getIsBasic());
		}
		
		if (!conditions.isEmpty())
		{
			sql += " WHERE " + MiscUtils.implode(conditions, " AND ");
		}
		
		// add order by clause if defined in filter
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			sql += " ORDER BY " + filter.getOrderBy();
			sql += filter.getOrder().equals(QueryResultOrder.ASC) ?  " ASC": " DESC";
		}
		
		if (filter.getLimit() != null)
		{
			sql += " LIMIT " + filter.getLimit();
		}
		
		SqlRowSet rowSet = env.getJdbcTemplate().queryForRowSet(sql);
		Map<KID, Type> types = new HashMap<KID, Type>();
		
		// list of type reference fields for which types have to be completed after all data is read in
		List<Field> objectRefs = new ArrayList<Field>();
		List<Field> inverseRefs = new ArrayList<Field>();
		List<Field> associationRefs = new ArrayList<Field>();
		List<String> typeRefIds = new ArrayList<String>();
		
		// types may have been sorted in the query, and we want to preserve any ordering in the result list
		// so we must keep track of the order in which types where retrieved, using list of KIDs
		List<KID> originalTypeOrder = new ArrayList<KID>();
		
		while (rowSet.next())
		{
			Type type = new Type();
			
			type.setApiName(rowSet.getString("apiname"));
			type.setCreated(rowSet.getDate("created"));
			type.setPackage(rowSet.getString("package"));
			type.setDbTable(rowSet.getString("dbtable"));
			type.setId(rowSet.getLong("id"));
			type.setDescription(rowSet.getString("description"));
			type.setKeyPrefix(KeyPrefix.get(rowSet.getString("keyprefix")));
			type.setKID(KID.get(rowSet.getString("kid")));
			type.setLabel(rowSet.getString("label"));
			type.setPluralLabel(rowSet.getString("plurallabel"));
			type.setBasic(rowSet.getBoolean("isbasic"));
			type.setCombineRecordAndCascadeSharing(rowSet.getBoolean("combinerecordandcascadesharing"));
			type.setDeclaredInCode(rowSet.getBoolean("isdeclaredincode"));
			type.setAutoLinkingType(rowSet.getBoolean("isautolinkingtype"));
			type.setUchLabel(rowSet.getString("uchlabel"));
			type.setUchPluralLabel(rowSet.getString("uchplurallabel"));
			
			String defaultField = rowSet.getString("defaultfield");
			type.setDefaultFieldId(StringUtils.hasText(defaultField) ? KID.get(defaultField) : null);
			
			String autoNumberField = rowSet.getString("autonumberfieldid");
			type.setAutoNumberFieldId(StringUtils.hasText(autoNumberField) ? KID.get(autoNumberField) : null);
			
			String sharingControlledByField = rowSet.getString("sharingcontrolledbyfield");
			if (StringUtils.hasText(sharingControlledByField))
			{
				type.setSharingControlledByFieldId(KID.get(sharingControlledByField));
			}
			
			if (initFields)
			{
				// fetch object fields
				String fieldQuery = FieldDao.FIELD_SQL_QUERY + " WHERE typeid = " + type.getId();
				SqlRowSet fields = env.getJdbcTemplate().queryForRowSet(fieldQuery);
				
				while (fields.next())
				{	
					try
					{
						// get field, but do not initialize types on it because this method is the one to load
						// the types so at this moment not all types exist and they could not be initialized
						Field newField = FieldDao.getFieldFromSqlRowSet(fields, false, appConfig, env);
						type.addField(newField);
						
						if (newField.getDataTypeId().equals(DataType.TYPE_REFERENCE))
						{
							objectRefs.add(newField);
							typeRefIds.add(((TypeReference)newField.getDataType()).getTypeId().getId());
						}
						else if (newField.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
						{
							inverseRefs.add(newField);
							typeRefIds.add(((InverseCollectionDataType)newField.getDataType()).getInverseTypeId().getId());
						}
						else if (newField.getDataTypeId().equals(DataType.ASSOCIATION))
						{
							associationRefs.add(newField);
							typeRefIds.add(((AssociationDataType)newField.getDataType()).getAssociatedTypeId().getId());
							typeRefIds.add(((AssociationDataType)newField.getDataType()).getLinkingTypeId().getId());
						}
					}
					catch (InvalidResultSetAccessException e)
					{
						throw new KommetException("Error reading field " + e.getMessage());
					}
					catch (KommetException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			types.put(type.getKID(), type);
			originalTypeOrder.add(type.getKID());
		}
		
		// if there are any type references or inverse types to initialize, do it
		if (!typeRefIds.isEmpty())
		{
			Map<KID, Type> refTypes = new HashMap<KID, Type>();
			
			// fetch objects for type reference fields
			SqlRowSet refObjRowSet = env.getJdbcTemplate().queryForRowSet(TYPE_QUERY + " WHERE kid in (" + MiscUtils.implode(typeRefIds, ",", "'", null)+ ")");
			while (refObjRowSet.next())
			{
				Type type = new Type();
				
				type.setApiName(refObjRowSet.getString("apiname"));
				type.setCreated(refObjRowSet.getDate("created"));
				type.setPackage(refObjRowSet.getString("package"));
				type.setDbTable(refObjRowSet.getString("dbtable"));
				type.setId(refObjRowSet.getLong("id"));
				type.setKeyPrefix(KeyPrefix.get(refObjRowSet.getString("keyprefix")));
				type.setKID(KID.get(refObjRowSet.getString("kid")));
				type.setLabel(refObjRowSet.getString("label"));
				type.setPluralLabel(refObjRowSet.getString("plurallabel"));
				type.setDeclaredInCode(refObjRowSet.getBoolean("isdeclaredincode"));
				type.setUchPluralLabel(refObjRowSet.getString("uchplurallabel"));
				type.setUchLabel(refObjRowSet.getString("uchlabel"));
				
				String defaultField = refObjRowSet.getString("defaultfield");
				type.setDefaultFieldId(StringUtils.hasText(defaultField) ? KID.get(defaultField) : null);
				
				if (!refTypes.containsKey(type.getKID()))
				{
					refTypes.put(type.getKID(), type);
				}
			}
			
			// read in types for type references
			for (Field objRefField : objectRefs)
			{
				KID kid = ((TypeReference)objRefField.getDataType()).getTypeId();
				
				if (kid == null)
				{
					throw new KommetException("type referenced by reference field " + objRefField.getApiName() + " on object " + objRefField.getType().getApiName() + " has null KID");
				}
				
				// first try to obtain type from type list, because it contains full object definitions,
				// and only then from refKObject, which do not contain field definitions
				Type refType = types.get(kid);
				if (refType == null)
				{
					refType = refTypes.get(kid);
				}
				
				if (refType == null)
				{
					throw new KommetException("Incomplete object metadata read from database: no reference found to type with ID " + kid);
				}
				((TypeReference)objRefField.getDataType()).setType(refType);
			}
			
			// read in types for inverse fields
			for (Field inverseCollectionField : inverseRefs)
			{
				KID kid = ((InverseCollectionDataType)inverseCollectionField.getDataType()).getInverseTypeId();
				
				if (kid == null)
				{
					throw new KommetException("type referenced by inverse collection field " + inverseCollectionField.getApiName() + " on object " + inverseCollectionField.getType().getApiName() + " has null KID");
				}
				
				// first try to obtain kobject from kObject list, because it contains full object definitions,
				// and only then from refKObject, which do not contain field definitions
				Type refType = types.get(kid);
				if (refType == null)
				{
					refType = refTypes.get(kid);
				}
				
				if (refType == null)
				{
					throw new KommetException("Incomplete object metadata read from database: no reference found to type with ID " + kid);
				}
				((InverseCollectionDataType)inverseCollectionField.getDataType()).setInverseType(refType);
			}
			
			// read in types for associations
			for (Field associationField : associationRefs)
			{
				KID associatedTypeId = ((AssociationDataType)associationField.getDataType()).getAssociatedTypeId();
				KID linkingTypeId = ((AssociationDataType)associationField.getDataType()).getLinkingTypeId();
				
				if (associatedTypeId == null)
				{
					throw new KommetException("type referenced by association field " + associationField.getApiName() + " on object " + associationField.getType().getApiName() + " has null KID");
				}
				if (linkingTypeId == null)
				{
					throw new KommetException("type referenced by linking type for field " + associationField.getApiName() + " on object " + associationField.getType().getApiName() + " has null KID");
				}
				
				// first try to obtain type from type list, because it contains full object definitions,
				// and only then from refKObject, which do not contain field definitions
				Type associatedType = types.get(associatedTypeId);
				if (associatedType == null)
				{
					associatedType = refTypes.get(associatedTypeId);
				}
				
				if (associatedType == null)
				{
					throw new KommetException("Incomplete object metadata read from database: no reference found to type with ID " + associatedTypeId);
				}
				((AssociationDataType)associationField.getDataType()).setAssociatedType(associatedType);
				
				// first try to obtain type from type list, because it contains full object definitions,
				// and only then from refKObject, which do not contain field definitions
				Type linkingType = types.get(linkingTypeId);
				if (linkingType == null)
				{
					linkingType = refTypes.get(linkingTypeId);
				}
				
				if (linkingType == null)
				{
					throw new KommetException("Incomplete object metadata read from database: no reference found to type with ID " + linkingTypeId);
				}
				((AssociationDataType)associationField.getDataType()).setLinkingType(linkingType);
			}
		}
		
		List<Type> typeList = new ArrayList<Type>();
		// restore original type order because it may have been messed up due to using hash maps
		for (KID typeId : originalTypeOrder)
		{
			typeList.add(types.get(typeId));
		}
		return typeList;
	}

	public Type getById(Long id, AppConfig appConfig, EnvData env) throws KommetException
	{
		TypeFilter filter = new TypeFilter();
		filter.addId(id);
		List<Type> types = getTypes(filter, appConfig, env, true);
		
		if (types.isEmpty())
		{
			return null;
		}
		else if (types.size() > 1)
		{
			throw new KommetException("More than one type found with ID " + id);
		}
		else
		{
			return types.get(0);
		}
	}

	public Type setDefaultField (Type type, EnvData env) throws KommetException
	{
		if (type.getKID() == null)
		{
			throw new KommetException("Type ID is null");
		}
		
		StringBuilder sql = new StringBuilder("UPDATE types SET defaultfield = ");
		sql.append("'" + type.getDefaultFieldId() + "'");
		sql.append(" WHERE ").append(Field.ID_FIELD_DB_COLUMN).append(" = '").append(type.getKID().getId()).append("'");
		
		try
		{
			// execute the update
			env.getJdbcTemplate().execute(sql.toString());
		}
		catch (DuplicateKeyException e)
		{
			throw new ConstraintViolationException("Constraint violation while inserting KObject. Nested: " + e.getMessage());
		}
		
		return type;
	}

	public void deleteDbTableForType(Type type, EnvData env) throws KommetException
	{
		try
		{
			env.getJdbcTemplate().update("DROP TABLE " + type.getDbTable());
		}
		catch (Exception e)
		{
			throw new KommetException("Error deleting DB table '" + type.getDbTable() + "'. Nested: " + e.getMessage());
		}
	}

	public void delete(KID typeId, EnvData env) throws KommetException
	{
		try
		{
			env.getJdbcTemplate().update("DELETE FROM types WHERE kid = '" + typeId + "'");
		}
		catch (Exception e)
		{
			throw new KommetException("Error deleting type with ID '" + typeId + "'. Nested: " + e.getMessage());
		}
	}
	
	/**
	 * Create triggers on the type's db table that will check whether the current lastmodifiedby user
	 * has rights to edit and delete the given record.
	 * @param type
	 * @param env
	 * @throws KommetException
	 */
	public void createCheckEditDeletePermissionsTriggers (Type type, EnvData env) throws KommetException
	{
		createCheckEditPermissionsTrigger(type, env);
		createCheckDeletePermissionsTrigger(type, env);
	}

	/**
	 * Create a trigger on the type's db table that will check whether the current lastmodifiedby user
	 * has rights to edit the given record.
	 * @param type
	 * @param env
	 * @throws KommetException
	 */
	private void createCheckEditPermissionsTrigger(Type type, EnvData env) throws KommetException
	{
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TRIGGER check_edit_permissions_" + type.getKeyPrefix().getPrefix());
		sql.append(" BEFORE UPDATE ON ").append(type.getDbTable()).append(" FOR EACH ROW EXECUTE PROCEDURE ");
		try
		{
			sql.append(appConfig.getCheckEditPermissionsFunction()).append("();");
		}
		catch (PropertyUtilException e1)
		{
			throw new KommetException("The name of the check edit permissions database trigger cannot be retrieved from config");
		}
		
		try
		{
			env.getJdbcTemplate().update(sql.toString());
		}
		catch (Exception e)
		{
			throw new KommetException("Error creating edit permissions trigger for type '" + type.getApiName() + "' for new type. Nested: " + e.getMessage());
		}
	}
	
	/**
	 * Create a trigger on the type's db table that will check whether the current lastmodifiedby user
	 * has rights to delete the given record.
	 * @param type
	 * @param env
	 * @throws KommetException
	 */
	private void createCheckDeletePermissionsTrigger(Type type, EnvData env) throws KommetException
	{
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TRIGGER check_delete_permissions_" + type.getKeyPrefix().getPrefix());
		sql.append(" BEFORE DELETE ON ").append(type.getDbTable()).append(" FOR EACH ROW EXECUTE PROCEDURE ");
		try
		{
			sql.append(appConfig.getCheckDeletePermissionsFunction()).append("();");
		}
		catch (PropertyUtilException e1)
		{
			throw new KommetException("The name of the check delete permissions database trigger cannot be retrieved from config");
		}
		
		try
		{
			env.getJdbcTemplate().update(sql.toString());
		}
		catch (Exception e)
		{
			throw new KommetException("Error creating edit permissions trigger for type '" + type.getApiName() + "' for new type. Nested: " + e.getMessage());
		}
	}
}