package kommet.rest;

import java.math.BigDecimal;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import kommet.auth.AuthData;
import kommet.basic.Dictionary;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
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
import kommet.data.datatypes.FormulaReturnType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.services.DictionaryService;
import kommet.utils.AppConfig;

/** Converts validated schema API datatype descriptions into Kommet model objects. */
@Component
public class SchemaDataTypeParser
{
	@Inject
	private DictionaryService dictionaryService;

	@Inject
	private AppConfig appConfig;

	/** Validates kind-specific options and resolves references in the selected environment. */
	public DataType parse(JsonNode definition, Type owner, AuthData authData, EnvData env) throws KommetException
	{
		SchemaPayload.keys(definition, "kind", "length", "long", "formatted", "decimalPlaces", "javaType", "values", "dictionaryId", "validateValues", "format", "targetType", "cascadeDelete", "formula", "returnType", "inverseType", "inverseProperty", "mode", "associatedType", "linkingType", "selfLinkingFieldApiName", "foreignLinkingFieldApiName");
		String kind = SchemaPayload.text(definition, "kind", true);
		switch (kind)
		{
			case "text":
				SchemaPayload.keys(definition, "kind", "length", "long", "formatted");
				int length = SchemaPayload.integer(definition, "length", 1);
				if (length > appConfig.getMaxTextFieldLength())
				{
					throw new ApiException(400, "Text length exceeds the configured maximum");
				}
				return new TextDataType(length, SchemaPayload.bool(definition, "long", false), SchemaPayload.bool(definition, "formatted", false));
			case "number":
				SchemaPayload.keys(definition, "kind", "decimalPlaces", "javaType");
				int decimalPlaces = SchemaPayload.integer(definition, "decimalPlaces", 0);
				String javaType = SchemaPayload.text(definition, "javaType", false);
				java.lang.Class<?> numericClass;
				if (javaType == null || javaType.equals(BigDecimal.class.getName()))
				{
					numericClass = BigDecimal.class;
				}
				else if (javaType.equals(Double.class.getName()))
				{
					numericClass = Double.class;
				}
				else if (javaType.equals(Integer.class.getName()))
				{
					numericClass = Integer.class;
				}
				else if (javaType.equals(Long.class.getName()))
				{
					numericClass = Long.class;
				}
				else
				{
					throw new ApiException(400, "Unsupported numeric Java type");
				}
				if (decimalPlaces > 0 && (numericClass == Integer.class || numericClass == Long.class))
				{
					throw new ApiException(400, "Integral Java types cannot have decimal places");
				}
				return new NumberDataType(decimalPlaces, numericClass);
			case "enumeration":
				SchemaPayload.keys(definition, "kind", "values", "dictionaryId", "validateValues");
				EnumerationDataType enumeration = new EnumerationDataType();
				if (definition.has("values"))
				{
					enumeration.setValues(String.join("\n", SchemaPayload.values(definition, "values")));
				}
				String dictionaryId = SchemaPayload.text(definition, "dictionaryId", false);
				if (dictionaryId != null)
				{
					Dictionary dictionary = dictionaryService.get(id(dictionaryId), authData, env);
					if (dictionary == null)
					{
						throw new ApiException(404, "Dictionary not found");
					}
					enumeration.setDictionary(dictionary);
				}
				if (!definition.has("values") && dictionaryId == null)
				{
					throw new ApiException(400, "Enumeration values or dictionaryId are required");
				}
				enumeration.setValidateValues(SchemaPayload.bool(definition, "validateValues", false));
				return enumeration;
			case "multiEnumeration":
				SchemaPayload.keys(definition, "kind", "values");
				return new MultiEnumerationDataType(SchemaPayload.values(definition, "values"));
			case "autoNumber":
				SchemaPayload.keys(definition, "kind", "format");
				return new AutoNumber(SchemaPayload.text(definition, "format", true));
			case "typeReference":
				SchemaPayload.keys(definition, "kind", "targetType", "cascadeDelete");
				TypeReference reference = new TypeReference(resolve(definition.get("targetType"), env));
				reference.setCascadeDelete(SchemaPayload.bool(definition, "cascadeDelete", false));
				return reference;
			case "formula":
				SchemaPayload.keys(definition, "kind", "formula", "returnType");
				FormulaReturnType returnType;
				try { returnType = FormulaReturnType.valueOf(SchemaPayload.text(definition, "returnType", true)); }
				catch (IllegalArgumentException exception) { throw new ApiException(400, "Unsupported formula return type"); }
				if (returnType != FormulaReturnType.TEXT)
				{
					throw new ApiException(400, "The existing formula parser supports only TEXT return types");
				}
				String formula = SchemaPayload.text(definition, "formula", true);
				if (formula.indexOf('\'') >= 0)
				{
					throw new ApiException(400, "Apostrophes are not supported by the existing formula parser");
				}
				return new FormulaDataType(returnType, formula, owner, env);
			case "inverseCollection":
				SchemaPayload.keys(definition, "kind", "inverseType", "inverseProperty");
				Type inverseType = resolve(definition.get("inverseType"), env);
				String inverseProperty = SchemaPayload.text(definition, "inverseProperty", true);
				checkReference(inverseType, inverseProperty, owner);
				return new InverseCollectionDataType(inverseType, inverseProperty);
			case "association":
				String mode = SchemaPayload.text(definition, "mode", true);
				if ("direct".equals(mode))
				{
					SchemaPayload.keys(definition, "kind", "mode", "associatedType");
					AssociationDataType association = new AssociationDataType();
					association.setAssociatedType(resolve(definition.get("associatedType"), env));
					return association;
				}
				if (!"linkingType".equals(mode))
				{
					throw new ApiException(400, "Unsupported association mode");
				}
				SchemaPayload.keys(definition, "kind", "mode", "linkingType", "selfLinkingFieldApiName", "foreignLinkingFieldApiName");
				Type linkingType = resolve(definition.get("linkingType"), env);
				String self = SchemaPayload.text(definition, "selfLinkingFieldApiName", true);
				String foreign = SchemaPayload.text(definition, "foreignLinkingFieldApiName", true);
				checkReference(linkingType, self, owner);
				Field foreignField = linkingType.getField(foreign);
				if (self.equals(foreign) || foreignField == null || !(foreignField.getDataType() instanceof TypeReference))
				{
					throw new ApiException(400, "Invalid foreign linking field");
				}
				return new AssociationDataType(linkingType, ((TypeReference)foreignField.getDataType()).getType(), self, foreign);
			default:
				SchemaPayload.keys(definition, "kind");
				switch (kind)
				{
					case "boolean": return new BooleanDataType();
					case "date": return new DateDataType();
					case "datetime": return new DateTimeDataType();
					case "email": return new EmailDataType();
					case "kid": return new KIDDataType();
					case "blob": return new BlobDataType();
					default: throw new ApiException(400, "Unsupported dataType.kind");
				}
		}
	}

	/** Parses an external Kommet identifier or reports a malformed request. */
	public static KID id(String value)
	{
		try { return KID.get(value); }
		catch (KommetException exception) { throw new ApiException(400, "Invalid Kommet ID"); }
	}

	/** Looks up a referenced type by its stable qualified name. */
	public static Type resolve(JsonNode identity, EnvData env) throws KommetException
	{
		SchemaPayload.keys(identity, "package", "apiName");
		Type type = env.getType(SchemaPayload.qualifiedName(identity));
		if (type == null)
		{
			throw new ApiException(404, "Referenced type not found");
		}
		return type;
	}

	/** Ensures a relationship property references the expected owning type. */
	private static void checkReference(Type type, String property, Type target) throws KommetException
	{
		Field field = type.getField(property);
		if (field == null || !(field.getDataType() instanceof TypeReference) || !target.getKID().equals(((TypeReference)field.getDataType()).getTypeId()))
		{
			throw new ApiException(400, "Relationship field must reference the owning type");
		}
	}
}