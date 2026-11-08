package kommet.web.controllers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.EnvFilter;
import kommet.rest.ApiException;
import kommet.rest.RestAuthenticationService;
import kommet.rest.RestAuthorizationUtil;
import kommet.rest.SchemaApiService;
import kommet.rest.SchemaDataTypeParser;
import kommet.rest.SchemaPayload;

/**
 * Exposes authenticated JSON endpoints for custom type and field upserts.
 */
@Controller
public class DeveloperApiController
{
	public static final String TYPE_SAVE_PATH = "/api/dev/type/save";
	public static final String FIELD_SAVE_PATH = "/api/dev/field/save";

	private final ObjectMapper mapper = new ObjectMapper()
			.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
			.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

	@Inject
	private EnvService envService;

	@Inject
	private RestAuthorizationUtil authorizationUtil;

	@Inject
	private SchemaApiService schemaService;

	/** Handles creation or metadata updates of a custom type. */
	@RequestMapping(value = TYPE_SAVE_PATH, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> saveType (@RequestBody(required = false) String body, HttpServletRequest request) throws KommetException
	{
		return save(body, request, false);
	}

	/** Handles creation or compatible updates of a custom field. */
	@RequestMapping(value = FIELD_SAVE_PATH, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> saveField (@RequestBody(required = false) String body, HttpServletRequest request) throws KommetException
	{
		return save(body, request, true);
	}

	/** Validates transport and JSON, authenticates the caller, and dispatches the upsert. */
	private ResponseEntity<Map<String, Object>> save (String body, HttpServletRequest request, boolean fieldOperation) throws KommetException
	{
		authorizationUtil.requireAuthorization(request);

		try
		{
			if (request.getContentType() == null || !MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(request.getContentType())))
			{
				throw new ApiException(400, "Content-Type must be application/json");
			}
		}
		catch (IllegalArgumentException exception)
		{
			throw new ApiException(400, "Invalid Content-Type");
		}

		if (body == null || body.isBlank() || body.length() > 262144)
		{
			throw new ApiException(400, "A JSON body of at most 256 KiB is required");
		}

		JsonNode payload;

		try
		{
			payload = mapper.readTree(body);
		}
		catch (JsonProcessingException exception)
		{
			throw new ApiException(400, "Malformed JSON request");
		}

		SchemaPayload.keys(payload, fieldOperation ? new String[] { "environmentId", "type", "field" } : new String[] { "environmentId", "type" });
		KID envId = SchemaDataTypeParser.id(SchemaPayload.text(payload, "environmentId", true));
		EnvFilter filter = new EnvFilter();
		filter.setKID(envId);

		if (envService.find(filter, envService.getMasterEnv()).isEmpty())
		{
			throw new ApiException(404, "Environment not found");
		}

		EnvData env = envService.get(envId);
		RestAuthenticationService.Authentication authentication = authorizationUtil.authenticate(request, env, RestAuthenticationService.Access.WRITE);
		Map<String, Object> data = schemaService.save(payload, authentication, fieldOperation);
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("success", true);
		response.put("data", data);
		return ResponseEntity.status("created".equals(data.get("operation")) ? 201 : 200).contentType(MediaType.APPLICATION_JSON).body(response);
	}

	/** Converts expected API failures into the JSON error envelope. */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, Object>> error(ApiException exception)
	{
		ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus()).contentType(MediaType.APPLICATION_JSON);
		if (exception.getStatus() == 401)
		{
			response.header("WWW-Authenticate", "Bearer");
		}
		return response.body(errorBody(exception.getMessage()));
	}

	/** Returns a generic failure without leaking persistence or configuration details. */
	@ExceptionHandler({ KommetException.class, RuntimeException.class })
	public ResponseEntity<Map<String, Object>> internalError(Exception exception)
	{
		return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON).body(errorBody("The schema operation could not be completed"));
	}

	/** Builds the standard unsuccessful REST response body. */
	private static Map<String, Object> errorBody(String message)
	{
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("success", false);
		response.put("messages", Collections.singletonList(message));
		return response;
	}
}