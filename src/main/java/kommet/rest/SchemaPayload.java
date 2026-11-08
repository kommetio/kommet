package kommet.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

/** Validates JSON properties and scalar values used by schema API requests. */
public final class SchemaPayload
{
	/** Prevents instantiation of this validation utility. */
	private SchemaPayload()
	{
	}

	/** Rejects non-object payloads and properties outside the supplied allowlist. */
	public static void keys(JsonNode object, String... allowed)
	{
		if (object == null || !object.isObject())
		{
			throw new ApiException(400, "Expected a JSON object");
		}
		Set<String> names = new HashSet<String>(Arrays.asList(allowed));
		Iterator<String> fields = object.fieldNames();
		while (fields.hasNext())
		{
			if (!names.contains(fields.next()))
			{
				throw new ApiException(400, "Unknown payload property");
			}
		}
	}

	/** Reads a string property and enforces its requiredness. */
	public static String text(JsonNode object, String key, boolean required)
	{
		JsonNode value = object.get(key);
		if (value == null || value.isNull())
		{
			if (required)
			{
				throw new ApiException(400, key + " is required");
			}
			return null;
		}
		if (!value.isTextual() || (required && value.textValue().trim().isEmpty()))
		{
			throw new ApiException(400, key + " must be a string" );
		}
		return value.textValue();
	}

	/** Reads a boolean property or uses the default when it is omitted. */
	public static boolean bool(JsonNode object, String key, boolean fallback)
	{
		JsonNode value = object.get(key);
		if (value == null)
		{
			return fallback;
		}
		if (!value.isBoolean())
		{
			throw new ApiException(400, key + " must be boolean");
		}
		return value.booleanValue();
	}

	/** Reads an integer constrained by the supplied lower bound. */
	public static int integer(JsonNode object, String key, int minimum)
	{
		JsonNode value = object.get(key);
		if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < minimum)
		{
			throw new ApiException(400, key + " must be an integer of at least " + minimum);
		}
		return value.intValue();
	}

	/** Reads distinct, nonempty, single-line enumeration choices. */
	public static Set<String> values(JsonNode object, String key)
	{
		JsonNode values = object.get(key);
		if (values == null || !values.isArray() || values.isEmpty())
		{
			throw new ApiException(400, key + " must be a nonempty array of strings");
		}
		Set<String> result = new LinkedHashSet<String>();
		for (JsonNode value : values)
		{
			if (!value.isTextual() || value.textValue().isBlank() || value.textValue().contains("\n") || value.textValue().contains("\r") || !result.add(value.textValue()))
			{
				throw new ApiException(400, "Enumeration values must be distinct nonempty single-line strings");
			}
		}
		return result;
	}

	/** Validates and combines a type's package and stable API name. */
	public static String qualifiedName(JsonNode object)
	{
		String packageName = text(object, "package", true);
		String apiName = text(object, "apiName", true);
		if (!packageName.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*") || !apiName.matches("[A-Z][a-zA-Z0-9_]*"))
		{
			throw new ApiException(400, "Invalid type package or API name");
		}
		return packageName + "." + apiName;
	}
}