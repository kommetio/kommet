package kommet.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.UniqueCheck;
import kommet.basic.types.SystemTypes;
import kommet.config.Constants;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.UniqueCheckService;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.AutoNumber;
import kommet.data.datatypes.BlobDataType;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.DateDataType;
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.EmailDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.ValidationUtil;

/** Performs permission-checked custom schema upserts through Kommet persistence services. */
@Service
public class SchemaApiService
{
	@Inject
	private DataService dataService;
	@Inject
	private UniqueCheckService uniqueCheckService;
	@Inject
	private SchemaDataTypeParser dataTypeParser;
	@Inject
	private PlatformTransactionManager transactionManager;
	@Inject
	private EnvService envService;
	@Inject
	private KommetCompiler compiler;

	private final Map<KID, Object> locks = new ConcurrentHashMap<KID, Object>();
	/** Runs the requested upsert atomically and invalidates cached state on failure. */
	public Map<String, Object> save(JsonNode payload, RestAuthenticationService.Authentication authentication, boolean fieldOperation)
	{
		AuthData authData = authentication.getAuthData();
		EnvData env = authentication.getEnv();
		KID requestedEnvId = SchemaDataTypeParser.id(SchemaPayload.text(payload, "envId", true));
		if (!env.getId().equals(requestedEnvId) || !env.getId().equals(authData.getEnvId()))
		{
			throw new ApiException(403, "Authentication does not permit the requested environment");
		}
		if (!AuthUtil.isRoot(authData) && !AuthUtil.isSysAdmin(authData))
		{
			throw new ApiException(403, "Schema operations require a root or system-administrator profile");
		}
		synchronized (locks.computeIfAbsent(env.getId(), key -> new Object()))
		{
			TransactionTemplate transaction = new TransactionTemplate(transactionManager);
			try
			{
				return transaction.execute(status ->
				{
					try
					{
						return fieldOperation ? saveField(payload, authData, env) : saveType(payload, authData, env);
					}
					catch (KommetException exception)
					{
						throw new ApiException(400, "Schema definition failed validation");
					}
				});
			}
			catch (RuntimeException exception)
			{
				envService.clear(env.getId());
				try
				{
					compiler.resetClassLoader(env);
				}
				catch (KommetException cleanupFailure)
				{
					exception.addSuppressed(cleanupFailure);
				}
				throw exception;
			}
		}
	}

	/** Returns one mutable custom type using the documented type payload shape. */
	public Map<String, Object> getType(String qualifiedName, EnvData env) throws KommetException
	{
		return typeDefinition(readableType(qualifiedName, env), env, true);
	}

	/** Returns one field by its fully qualified owner-and-field name. */
	public Map<String, Object> getField(String qualifiedName, EnvData env) throws KommetException
	{
		if (qualifiedName == null || qualifiedName.isBlank())
		{
			throw new ApiException(400, "A fully qualified field name is required");
		}
		int separator = qualifiedName.lastIndexOf('.');
		if (separator <= 0 || separator == qualifiedName.length() - 1)
		{
			throw new ApiException(400, "Field name must include its package, type, and field names");
		}
		Type type = readableType(qualifiedName.substring(0, separator), env);
		Field field = type.getField(qualifiedName.substring(separator + 1));
		if (field == null)
		{
			throw new ApiException(404, "Field not found");
		}
		return fieldDefinition(field, type, env);
	}

	/** Lists mutable custom types, optionally including each type's field definitions. */
	public List<Map<String, Object>> getTypes(EnvData env, boolean includeFields) throws KommetException
	{
		Collection<Type> registeredTypes = env.getCustomTypes();
		List<Type> types = new ArrayList<Type>();
		for (Type type : registeredTypes)
		{
			if (!type.isBasic() && !type.isDeclaredInCode())
			{
				types.add(type);
			}
		}
		types.sort(Comparator.comparing(Type::getQualifiedName));
		List<Map<String, Object>> definitions = new ArrayList<Map<String, Object>>();
		for (Type type : types)
		{
			definitions.add(typeDefinition(type, env, includeFields));
		}
		return definitions;
	}

	private static Type readableType(String qualifiedName, EnvData env) throws KommetException
	{
		if (qualifiedName == null || qualifiedName.isBlank())
		{
			throw new ApiException(400, "A fully qualified type name is required");
		}
		Type type = env.getType(qualifiedName);
		if (type == null || type.isBasic() || type.isDeclaredInCode())
		{
			throw new ApiException(404, "Type not found");
		}
		return type;
	}

	private static Map<String, Object> typeDefinition(Type type, EnvData env, boolean includeFields) throws KommetException
	{
		Map<String, Object> definition = new LinkedHashMap<String, Object>();
		definition.put("package", type.getPackage());
		definition.put("apiName", type.getApiName());
		definition.put("label", type.getLabel());
		definition.put("pluralLabel", type.getPluralLabel());
		definition.put("description", type.getDescription());
		definition.put("labelKey", type.getUchLabel());
		definition.put("pluralLabelKey", type.getUchPluralLabel());
		Field defaultField = type.getField(type.getDefaultFieldId());
		definition.put("defaultFieldApiName", defaultField != null ? defaultField.getApiName() : null);
		Field sharingField = type.getSharingControlledByField();
		definition.put("sharingControlledByFieldApiName", sharingField != null ? sharingField.getApiName() : null);
		definition.put("combineRecordAndCascadeSharing", type.isCombineRecordAndCascadeSharing());
		if (includeFields)
		{
			List<Field> fields = new ArrayList<Field>(type.getFields());
			fields.sort(Comparator.comparing(Field::getApiName));
			List<Map<String, Object>> fieldDefinitions = new ArrayList<Map<String, Object>>();
			for (Field field : fields)
			{
				fieldDefinitions.add(fieldDefinition(field, type, env));
			}
			definition.put("fields", fieldDefinitions);
		}
		return definition;
	}

	private static Map<String, Object> fieldDefinition(Field field, Type owner, EnvData env) throws KommetException
	{
		Map<String, Object> definition = new LinkedHashMap<String, Object>();
		definition.put("apiName", field.getApiName());
		definition.put("label", field.getLabel());
		definition.put("description", field.getDescription());
		definition.put("labelKey", field.getUchLabel());
		definition.put("required", field.isRequired());
		definition.put("trackHistory", field.isTrackHistory());
		definition.put("defaultValue", field.getDefaultValue());
		definition.put("unique", isUniqueField(field, owner));
		definition.put("isDefault", Objects.equals(owner.getDefaultFieldId(), field.getKID()));
		definition.put("dataType", dataTypeDefinition(field.getDataType(), env));
		return definition;
	}

	private static boolean isUniqueField(Field field, Type owner)
	{
		if (owner.getUniqueChecks() == null || field.getKID() == null)
		{
			return false;
		}
		for (UniqueCheck check : owner.getUniqueChecks())
		{
			if (check.getParsedFieldIds() != null && check.getParsedFieldIds().size() == 1 && check.getParsedFieldIds().contains(field.getKID()))
			{
				return true;
			}
		}
		return false;
	}

	private static Map<String, Object> dataTypeDefinition(DataType dataType, EnvData env) throws KommetException
	{
		Map<String, Object> definition = new LinkedHashMap<String, Object>();
		if (dataType instanceof TextDataType)
		{
			TextDataType text = (TextDataType)dataType;
			definition.put("kind", "text");
			definition.put("length", text.getLength());
			definition.put("long", text.isLong());
			definition.put("formatted", text.isFormatted());
		}
		else if (dataType instanceof NumberDataType)
		{
			NumberDataType number = (NumberDataType)dataType;
			definition.put("kind", "number");
			definition.put("decimalPlaces", number.getDecimalPlaces());
			definition.put("javaType", number.getJavaType());
		}
		else if (dataType instanceof BooleanDataType) definition.put("kind", "boolean");
		else if (dataType instanceof DateDataType) definition.put("kind", "date");
		else if (dataType instanceof DateTimeDataType) definition.put("kind", "datetime");
		else if (dataType instanceof EmailDataType) definition.put("kind", "email");
		else if (dataType instanceof KIDDataType) definition.put("kind", "kid");
		else if (dataType instanceof BlobDataType) definition.put("kind", "blob");
		else if (dataType instanceof EnumerationDataType)
		{
			EnumerationDataType enumeration = (EnumerationDataType)dataType;
			definition.put("kind", "enumeration");
			if (enumeration.getDictionary() != null)
			{
				definition.put("dictionaryId", enumeration.getDictionary().getId().getId());
			}
			if (enumeration.getDictionary() == null || !enumeration.getValueList().isEmpty())
			{
				definition.put("values", enumeration.getValueList());
			}
			definition.put("validateValues", enumeration.isValidateValues());
		}
		else if (dataType instanceof MultiEnumerationDataType)
		{
			definition.put("kind", "multiEnumeration");
			definition.put("values", new ArrayList<String>(new TreeSet<String>(((MultiEnumerationDataType)dataType).getValues())));
		}
		else if (dataType instanceof AutoNumber)
		{
			definition.put("kind", "autoNumber");
			definition.put("format", ((AutoNumber)dataType).getFormat());
		}
		else if (dataType instanceof TypeReference)
		{
			TypeReference reference = (TypeReference)dataType;
			definition.put("kind", "typeReference");
			definition.put("targetType", typeIdentity(reference.getType(), reference.getTypeId(), env));
			definition.put("cascadeDelete", reference.isCascadeDelete());
		}
		else if (dataType instanceof FormulaDataType)
		{
			FormulaDataType formula = (FormulaDataType)dataType;
			definition.put("kind", "formula");
			definition.put("formula", formula.getUserDefinition());
			definition.put("returnType", formula.getReturnType() != null ? formula.getReturnType().name() : null);
		}
		else if (dataType instanceof InverseCollectionDataType)
		{
			InverseCollectionDataType inverse = (InverseCollectionDataType)dataType;
			definition.put("kind", "inverseCollection");
			definition.put("inverseType", typeIdentity(inverse.getInverseType(), inverse.getInverseTypeId(), env));
			definition.put("inverseProperty", inverse.getInverseProperty());
		}
		else if (dataType instanceof AssociationDataType)
		{
			AssociationDataType association = (AssociationDataType)dataType;
			definition.put("kind", "association");
			if (association.getLinkingTypeId() == null)
			{
				definition.put("mode", "direct");
				definition.put("associatedType", typeIdentity(association.getAssociatedType(), association.getAssociatedTypeId(), env));
			}
			else
			{
				definition.put("mode", "linkingType");
				definition.put("linkingType", typeIdentity(association.getLinkingType(), association.getLinkingTypeId(), env));
				definition.put("selfLinkingFieldApiName", association.getSelfLinkingField());
				definition.put("foreignLinkingFieldApiName", association.getForeignLinkingField());
			}
		}
		else
		{
			throw new ApiException(500, "Unsupported schema datatype");
		}
		return definition;
	}

	private static Map<String, Object> typeIdentity(Type type, KID typeId, EnvData env) throws KommetException
	{
		Type resolved = type != null ? type : typeId != null ? env.getType(typeId) : null;
		if (resolved == null)
		{
			throw new ApiException(404, "Schema datatype reference not found");
		}
		Map<String, Object> identity = new LinkedHashMap<String, Object>();
		identity.put("package", resolved.getPackage());
		identity.put("apiName", resolved.getApiName());
		return identity;
	}

	/** Creates a custom type or updates its metadata without replacing existing fields. */
	private Map<String, Object> saveType(JsonNode payload, AuthData authData, EnvData env) throws KommetException
	{
		SchemaPayload.keys(payload, "envId", "type");
		JsonNode definition = payload.get("type");
		SchemaPayload.keys(definition, "package", "apiName", "label", "pluralLabel", "description", "labelKey", "pluralLabelKey", "defaultFieldApiName", "sharingControlledByFieldApiName", "combineRecordAndCascadeSharing");
		String qualifiedName = SchemaPayload.qualifiedName(definition);
		validateTypeIdentity(definition);
		Type type = env.getType(qualifiedName);
		boolean created = type == null;
		if (created)
		{
			type = new Type();
			type.setPackage(SchemaPayload.text(definition, "package", true));
			type.setApiName(SchemaPayload.text(definition, "apiName", true));
			type.setCreated(new Date());
		}
		else
		{
			mutable(type);
		}
		type.setLabel(SchemaPayload.text(definition, "label", true));
		type.setPluralLabel(SchemaPayload.text(definition, "pluralLabel", true));
		if (definition.has("description"))
		{
			type.setDescription(SchemaPayload.text(definition, "description", false));
		}
		if (definition.has("labelKey"))
		{
			type.setUchLabel(SchemaPayload.text(definition, "labelKey", false));
		}
		if (definition.has("pluralLabelKey"))
		{
			type.setUchPluralLabel(SchemaPayload.text(definition, "pluralLabelKey", false));
		}
		if (definition.has("combineRecordAndCascadeSharing"))
		{
			type.setCombineRecordAndCascadeSharing(SchemaPayload.bool(definition, "combineRecordAndCascadeSharing", false));
		}
		if (definition.has("defaultFieldApiName"))
		{
			String name = SchemaPayload.text(definition, "defaultFieldApiName", false);
			if (name != null)
			{
				type.setDefaultFieldId(existingField(type, name).getKID());
			}
			else if (!created)
			{
				type.setDefaultFieldId(type.getField(Field.ID_FIELD_NAME).getKID());
			}
		}
		if (definition.has("sharingControlledByFieldApiName"))
		{
			String name = SchemaPayload.text(definition, "sharingControlledByFieldApiName", false);
			type.setSharingControlledByFieldId(name == null ? null : existingField(type, name).getKID());
		}
		type = created ? dataService.createType(type, authData, env) : dataService.updateType(type, authData, env);
		Map<String, Object> result = result(created, type);
		result.put("keyPrefix", type.getKeyPrefix().toString());
		result.put("qualifiedName", type.getQualifiedName());
		return result;
	}

	/** Upserts a field and its unique/default settings after validating existing data. */
	private Map<String, Object> saveField(JsonNode payload, AuthData authData, EnvData env) throws KommetException
	{
		SchemaPayload.keys(payload, "envId", "type", "field");
		Type type = SchemaDataTypeParser.resolve(payload.get("type"), env);
		mutable(type);
		JsonNode definition = payload.get("field");
		SchemaPayload.keys(definition, "apiName", "label", "description", "labelKey", "required", "trackHistory", "defaultValue", "unique", "isDefault", "dataType");
		String name = SchemaPayload.text(definition, "apiName", true);
		if (!ValidationUtil.isValidFieldApiName(name) || Field.isReservedFieldApiName(name) || Field.isSystemField(name))
		{
			throw new ApiException(400, "Invalid or reserved field API name");
		}
		Field oldField = type.getField(name);
		boolean created = oldField == null;
		Field field = created ? new Field() : env.getFieldForUpdate(type.getKeyPrefix(), name);
		field.setApiName(name);
		field.setType(type);
		field.setLabel(SchemaPayload.text(definition, "label", true));
		if (definition.has("description"))
		{
			field.setDescription(SchemaPayload.text(definition, "description", false));
		}
		if (definition.has("labelKey"))
		{
			field.setUchLabel(SchemaPayload.text(definition, "labelKey", false));
		}
		field.setRequired(SchemaPayload.bool(definition, "required", !created && field.isRequired()));
		field.setTrackHistory(SchemaPayload.bool(definition, "trackHistory", !created && field.isTrackHistory()));
		if (definition.has("defaultValue"))
		{
			field.setDefaultValue(SchemaPayload.text(definition, "defaultValue", false));
		}
		DataType dataType = dataTypeParser.parse(definition.get("dataType"), type, authData, env);
		if (!created)
		{
			compatible(oldField.getDataType(), dataType);
			if (dataType instanceof AssociationDataType && ((AssociationDataType)dataType).getLinkingTypeId() == null)
			{
				dataType = oldField.getDataType();
			}
		}
		field.setDataType(dataType);
		if (dataType instanceof AutoNumber && !field.isRequired())
		{
			throw new ApiException(400, "AutoNumber fields must be required");
		}
		boolean unique = SchemaPayload.bool(definition, "unique", false);
		if (unique && !UniqueCheckService.isValidUniqueCheckFieldDataType(dataType.getId()))
		{
			throw new ApiException(400, "This datatype cannot be unique");
		}
		validateRecords(field, oldField, unique, authData, env);
		if (created && dataType instanceof AssociationDataType && ((AssociationDataType)dataType).getLinkingTypeId() == null)
		{
			AssociationDataType association = (AssociationDataType)dataType;
			DataService.LinkingTypeResult linking = dataService.createLinkingType(type, association.getAssociatedType(), authData, env);
			association.setLinkingType(linking.getType());
			association.setSelfLinkingField(linking.getSelfLinkingField().getApiName());
			association.setForeignLinkingField(linking.getForeignLinkingField().getApiName());
		}
		field = created ? dataService.createField(field, authData, env) : dataService.updateField(field, authData, env);
		if (definition.has("unique"))
		{
			updateUnique(field, unique, authData, env);
		}
		if (definition.has("isDefault"))
		{
			boolean isDefault = SchemaPayload.bool(definition, "isDefault", false);
			Type updatedType = env.getType(type.getKID());
			if (isDefault)
			{
				dataService.setDefaultField(type.getKID(), field.getKID(), authData, env);
			}
			else if (field.getKID().equals(updatedType.getDefaultFieldId()))
			{
				dataService.setDefaultField(type.getKID(), updatedType.getField(Field.ID_FIELD_NAME).getKID(), authData, env);
			}
		}
		Map<String, Object> result = result(created, type);
		result.put("fieldId", field.getKID().getId());
		result.put("typeKeyPrefix", type.getKeyPrefix().toString());
		result.put("apiName", name);
		Map<String, Object> datatypeResult = new LinkedHashMap<String, Object>();
		datatypeResult.put("kind", SchemaPayload.text(definition.get("dataType"), "kind", true));
		result.put("dataType", datatypeResult);
		return result;
	}

	/** Adds or removes single-field unique checks without changing composite checks. */
	private void updateUnique(Field field, boolean unique, AuthData authData, EnvData env) throws KommetException
	{
		List<UniqueCheck> singleFieldChecks = new ArrayList<UniqueCheck>();
		for (UniqueCheck check : uniqueCheckService.findForField(field, env, dataService))
		{
			if (check.getParsedFieldIds().size() == 1)
			{
				singleFieldChecks.add(check);
			}
		}
		if (unique && singleFieldChecks.isEmpty())
		{
			UniqueCheck check = new UniqueCheck();
			check.setTypeId(field.getType().getKID());
			check.setFieldIds(field.getKID().getId());
			check.setName(Constants.UNIQUE_CHECK_FIELD_PREFIX + field.getKID());
			uniqueCheckService.save(check, authData, env);
		}
		else if (!unique)
		{
			for (UniqueCheck check : singleFieldChecks)
			{
				if (Boolean.TRUE.equals(check.getIsSystem()))
				{
					throw new ApiException(409, "Cannot remove a system unique check");
				}
				uniqueCheckService.delete(check.getId(), authData, env);
			}
		}
	}

	/** Ensures defaults, requiredness, uniqueness, and choices are valid for existing records. */
	private void validateRecords(Field field, Field oldField, boolean unique, AuthData authData, EnvData env) throws KommetException
	{
		DataType dataType = field.getDataType();
		Object defaultValue = null;
		if (field.getDefaultValue() != null)
		{
			if (!dataType.isPrimitive())
			{
				throw new ApiException(400, "Defaults are not supported for this datatype");
			}
			defaultValue = dataType.getJavaValue(field.getDefaultValue());
		}
		Set<String> choices = null;
		if (dataType instanceof EnumerationDataType)
		{
			EnumerationDataType enumeration = (EnumerationDataType)dataType;
			choices = new HashSet<String>(enumeration.getValueList());
			if (enumeration.getDictionary() != null)
			{
				Criteria dictionaryItems = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.DICTIONARY_ITEM_API_NAME)).getKID(), authData);
				dictionaryItems.addProperty("name");
				dictionaryItems.add(Restriction.eq("dictionary.id", enumeration.getDictionary().getId()));
				for (Record item : dictionaryItems.list())
				{
					choices.add((String)item.getField("name"));
				}
			}
		}
		else if (dataType instanceof MultiEnumerationDataType)
		{
			choices = ((MultiEnumerationDataType)dataType).getValues();
		}
		if (choices != null && defaultValue != null && !SpecialValue.isNull(defaultValue) && !choices.contains(defaultValue.toString()))
		{
			throw new ApiException(400, "Default is not an allowed enumeration value");
		}
		if (!unique && !field.isRequired() && choices == null)
		{
			return;
		}
		Criteria records = env.getSelectCriteria(field.getType().getKID(), authData);
		String property = oldField == null ? Field.ID_FIELD_NAME : field.getApiName();
		if (oldField != null && dataType instanceof TypeReference)
		{
			property += ".id";
		}
		records.addProperty(property);
		Set<Object> seen = new HashSet<Object>();
		for (Record record : records.list())
		{
			Object value = oldField == null ? defaultValue : record.getField(property);
			if (value == null || SpecialValue.isNull(value))
			{
				if (field.isRequired() && Field.hasDatabaseRepresenation(dataType))
				{
					throw new ApiException(400, "Existing records contain an empty required field");
				}
				continue;
			}
			if (unique && !seen.add(value))
			{
				throw new ApiException(400, "Existing records contain duplicate field values");
			}
			if (choices != null)
			{
				if (value instanceof Set<?>)
				{
					if (!choices.containsAll((Set<?>)value))
					{
						throw new ApiException(400, "Existing records contain removed enumeration values");
					}
				}
				else if (!choices.contains(value.toString()))
				{
					throw new ApiException(400, "Existing records contain removed enumeration values");
				}
			}
		}
	}

	/** Rejects datatype or storage changes that require an explicit migration. */
	public static void compatible(DataType oldType, DataType newType)
	{
		boolean equal = oldType.getId().equals(newType.getId());
		if (equal && oldType instanceof TextDataType)
		{
			TextDataType oldText = (TextDataType)oldType;
			TextDataType newText = (TextDataType)newType;
			equal = Objects.equals(oldText.getLength(), newText.getLength()) && oldText.isLong() == newText.isLong() && oldText.isFormatted() == newText.isFormatted();
		}
		else if (equal && oldType instanceof NumberDataType)
		{
			equal = Objects.equals(((NumberDataType)oldType).getDecimalPlaces(), ((NumberDataType)newType).getDecimalPlaces()) && Objects.equals(oldType.getJavaType(), newType.getJavaType());
		}
		else if (equal && oldType instanceof TypeReference)
		{
			equal = Objects.equals(((TypeReference)oldType).getTypeId(), ((TypeReference)newType).getTypeId());
		}
		else if (equal && oldType instanceof FormulaDataType)
		{
			equal = Objects.equals(((FormulaDataType)oldType).getUserDefinition(), ((FormulaDataType)newType).getUserDefinition()) && ((FormulaDataType)oldType).getReturnType() == ((FormulaDataType)newType).getReturnType();
		}
		else if (equal && oldType instanceof AutoNumber)
		{
			equal = Objects.equals(((AutoNumber)oldType).getFormat(), ((AutoNumber)newType).getFormat());
		}
		else if (equal && oldType instanceof InverseCollectionDataType)
		{
			equal = Objects.equals(((InverseCollectionDataType)oldType).getInverseTypeId(), ((InverseCollectionDataType)newType).getInverseTypeId()) && Objects.equals(((InverseCollectionDataType)oldType).getInverseProperty(), ((InverseCollectionDataType)newType).getInverseProperty());
		}
		else if (equal && oldType instanceof AssociationDataType)
		{
			AssociationDataType oldAssociation = (AssociationDataType)oldType;
			AssociationDataType newAssociation = (AssociationDataType)newType;
			equal = Objects.equals(oldAssociation.getAssociatedTypeId(), newAssociation.getAssociatedTypeId()) && (newAssociation.getLinkingTypeId() == null || (Objects.equals(oldAssociation.getLinkingTypeId(), newAssociation.getLinkingTypeId()) && Objects.equals(oldAssociation.getSelfLinkingField(), newAssociation.getSelfLinkingField()) && Objects.equals(oldAssociation.getForeignLinkingField(), newAssociation.getForeignLinkingField())));
		}
		if (!equal)
		{
			throw new ApiException(409, "Datatype or storage changes require an explicit migration");
		}
	}

	/** Prevents schema requests from using reserved or environment-generated identities. */
	private static void validateTypeIdentity(JsonNode definition)
	{
		String packageName = SchemaPayload.text(definition, "package", true);
		String name = SchemaPayload.text(definition, "apiName", true);
		if (packageName.equals("kommet.basic") || packageName.startsWith("kommet.envs.") || ValidationUtil.getReservedTypeNames().contains(name.toLowerCase(java.util.Locale.ROOT)))
		{
			throw new ApiException(400, "Reserved type identity");
		}
	}

	/** Allows changes only to mutable, custom, non-code-declared types. */
	private static void mutable(Type type)
	{
		if (type.isBasic() || type.isDeclaredInCode())
		{
			throw new ApiException(403, "Only mutable custom types can be changed");
		}
	}

	/** Resolves a named field used by default-field or sharing metadata. */
	private static Field existingField(Type type, String name) throws KommetException
	{
		Field field = type.getField(name);
		if (field == null)
		{
			throw new ApiException(404, "Named field does not exist on this type");
		}
		return field;
	}

	/** Builds the common identity and operation fields of an upsert response. */
	private static Map<String, Object> result(boolean created, Type type)
	{
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("operation", created ? "created" : "updated");
		result.put("typeId", type.getKID().getId());
		return result;
	}
}