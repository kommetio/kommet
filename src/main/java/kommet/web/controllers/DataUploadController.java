package kommet.web.controllers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kommet.auth.AuthData;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.EnvFilter;
import kommet.rest.ApiException;
import kommet.rest.RestAuthenticationService;
import kommet.rest.RestAuthorizationUtil;
import kommet.rest.SchemaDataTypeParser;

/** Imports CSV records through the normal permission and validation paths. */
@Controller
public class DataUploadController
{
	public static final String UPLOAD_PATH = "/api/dev/records/imports";

	private final ObjectMapper mapper = new ObjectMapper();

	@Inject
	private EnvService envService;

	@Inject
	private RestAuthorizationUtil authorizationUtil;

	@Inject
	private DataService dataService;

	@Inject
	private org.springframework.transaction.PlatformTransactionManager transactionManager;

	/** Imports CSV records synchronously; partialInsert controls row transaction boundaries. */
	@RequestMapping(value = UPLOAD_PATH, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> upload(
			@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "typeName", required = false) String typeName,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "mapping", required = false) String mapping,
			@RequestParam(value = "partialInsert", required = false, defaultValue = "0") String partialInsert,
			HttpServletRequest request) throws KommetException
	{
		authorizationUtil.requireAuthorization(request);
		if (!"0".equals(partialInsert) && !"1".equals(partialInsert))
		{
			throw new ApiException(400, "partialInsert must be 0 or 1");
		}
		if (envId == null || envId.isBlank())
		{
			throw new ApiException(400, "envId is required");
		}
		if (typeName == null || typeName.isBlank())
		{
			throw new ApiException(400, "typeName is required");
		}
		if (file == null || file.isEmpty())
		{
			throw new ApiException(400, "A non-empty CSV file is required");
		}

		EnvData env = resolveEnvironment(envId);
		RestAuthenticationService.Authentication authentication = authorizationUtil.authenticate(request, env, RestAuthenticationService.Access.WRITE);
		Type type = env.getType(typeName);
		if (type == null)
		{
			throw new ApiException(404, "Type not found");
		}

		CsvUpload upload = parseCsv(file, mapping, type);
		AuthData authData = authentication.getAuthData();
		env.addAuthData(authData);
		try
		{
			return "1".equals(partialInsert)
					? importPartial(upload, type, authData, env)
					: importAtomic(upload, type, authData, env);
		}
		finally
		{
			env.clearAuthData();
		}
	}

	private ResponseEntity<Map<String, Object>> importAtomic(CsvUpload upload, Type type, AuthData authData, EnvData env)
	{
		org.springframework.transaction.support.TransactionTemplate transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
		try
		{
			transaction.execute(status ->
			{
				for (CsvRow row : upload.rows)
				{
					insertRow(row, upload.fields, type, authData, env);
				}
				return null;
			});
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(successBody(upload.rows.size(), upload.rows.size(), 0, List.of()));
		}
		catch (RowImportException exception)
		{
			Map<String, Object> response = errorBody("Import failed; no records were inserted.");
			response.put("data", resultData(upload.rows.size(), 0, upload.rows.size(), List.of(rowError(exception))));
			return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(response);
		}
	}

	private ResponseEntity<Map<String, Object>> importPartial(CsvUpload upload, Type type, AuthData authData, EnvData env)
	{
		org.springframework.transaction.support.TransactionTemplate transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
		int inserted = 0;
		List<Map<String, Object>> errors = new ArrayList<Map<String, Object>>();
		for (CsvRow row : upload.rows)
		{
			try
			{
				transaction.execute(status ->
				{
					insertRow(row, upload.fields, type, authData, env);
					return null;
				});
				inserted++;
			}
			catch (RowImportException exception)
			{
				errors.add(rowError(exception));
			}
		}
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
				.body(successBody(upload.rows.size(), inserted, errors.size(), errors));
	}

	private void insertRow(CsvRow row, List<Field> fields, Type type, AuthData authData, EnvData env)
	{
		try
		{
			Record record = new Record(type);
			for (int columnIndex = 0; columnIndex < fields.size(); columnIndex++)
			{
				Field field = fields.get(columnIndex);
				String value = row.values.get(columnIndex);
				Object javaValue = value == null || value.isEmpty()
						? SpecialValue.NULL
						: fieldValue(field, value, env);
				record.setField(field.getApiName(), javaValue, env);
			}
			dataService.save(record, authData, env);
		}
		catch (Exception exception)
		{
			throw new RowImportException(row.rowId, message(exception), exception);
		}
	}

	private static Object fieldValue(Field field, String value, EnvData env) throws KommetException
	{
		if (field.getDataType() instanceof TypeReference)
		{
			KID referenceId = SchemaDataTypeParser.id(value);
			Type referencedType = env.getTypeByRecordId(referenceId);
			TypeReference referenceType = (TypeReference)field.getDataType();
			KID expectedTypeId = referenceType.getTypeId();
			if (referencedType == null || expectedTypeId == null || !expectedTypeId.equals(referencedType.getKID()))
			{
				throw new KommetException("Reference value does not identify a record of the expected type");
			}
			Record referencedRecord = new Record(referencedType);
			referencedRecord.setKID(referenceId);
			return referencedRecord;
		}
		return field.getDataType().getJavaValue(value);
	}

	private CsvUpload parseCsv(MultipartFile file, String mappingJson, Type type) throws KommetException
	{
		List<CsvRow> rows = new ArrayList<CsvRow>();
		List<String> headers;
		try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
				CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).build().parse(reader))
		{
			headers = new ArrayList<String>(parser.getHeaderNames());
			if (headers.isEmpty())
			{
				throw new ApiException(400, "CSV header row is required");
			}
			if (!headers.isEmpty() && headers.get(0).startsWith("\uFEFF"))
			{
				headers.set(0, headers.get(0).substring(1));
			}
			Set<String> uniqueHeaders = new HashSet<String>();
			for (String header : headers)
			{
				if (header == null || header.isBlank() || !uniqueHeaders.add(header))
				{
					throw new ApiException(400, "CSV headers must be non-empty and unique");
				}
			}
			long rowId = 2;
			for (CSVRecord csvRecord : parser)
			{
				if (!csvRecord.isConsistent())
				{
					throw new ApiException(400, "CSV row " + rowId + " has an unexpected number of columns");
				}
				List<String> values = new ArrayList<String>(headers.size());
				for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++)
				{
					values.add(csvRecord.get(columnIndex));
				}
				rows.add(new CsvRow(rowId++, values));
			}
		}
		catch (IOException | IllegalArgumentException exception)
		{
			throw new ApiException(400, "CSV file could not be parsed");
		}
		return new CsvUpload(rows, mapFields(headers, mappingJson, type));
	}

	private List<Field> mapFields(List<String> headers, String mappingJson, Type type) throws KommetException
	{
		Map<String, String> mapping = parseMapping(mappingJson);
		Set<String> headerSet = new HashSet<String>(headers);
		if (!headerSet.containsAll(mapping.keySet()))
		{
			throw new ApiException(400, "Mapping contains a CSV header that does not exist");
		}
		List<Field> fields = new ArrayList<Field>(headers.size());
		Set<String> mappedFields = new HashSet<String>();
		for (String header : headers)
		{
			String fieldName = mapping.getOrDefault(header, header);
			Field field = type.getField(fieldName);
			if (field == null || Field.isSystemField(fieldName) || !Field.hasDatabaseRepresenation(field.getDataType()))
			{
				throw new ApiException(400, "CSV header does not map to a writable field: " + header);
			}
			if (!mappedFields.add(fieldName))
			{
				throw new ApiException(400, "Multiple CSV headers map to field " + fieldName);
			}
			fields.add(field);
		}
		return fields;
	}

	private Map<String, String> parseMapping(String mappingJson)
	{
		Map<String, String> mapping = new HashMap<String, String>();
		if (mappingJson == null || mappingJson.isBlank())
		{
			return mapping;
		}
		JsonNode json;
		try
		{
			json = mapper.readTree(mappingJson);
		}
		catch (JsonProcessingException exception)
		{
			throw new ApiException(400, "mapping must be a JSON object");
		}
		if (json == null || !json.isObject())
		{
			throw new ApiException(400, "mapping must be a JSON object");
		}
		json.fields().forEachRemaining(entry ->
		{
			if (!entry.getValue().isTextual() || entry.getValue().asText().isBlank())
			{
				throw new ApiException(400, "mapping values must be non-empty field API names");
			}
			mapping.put(entry.getKey(), entry.getValue().asText());
		});
		return mapping;
	}

	private EnvData resolveEnvironment(String environmentId) throws KommetException
	{
		KID envId = SchemaDataTypeParser.id(environmentId);
		EnvFilter filter = new EnvFilter();
		filter.setKID(envId);
		if (envService.find(filter, envService.getMasterEnv()).isEmpty())
		{
			throw new ApiException(404, "Environment not found");
		}
		return envService.get(envId);
	}

	private static Map<String, Object> successBody(int rowsReceived, int recordsCreated, int rowsFailed, List<Map<String, Object>> errors)
	{
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("success", true);
		response.put("data", resultData(rowsReceived, recordsCreated, rowsFailed, errors));
		return response;
	}

	private static Map<String, Object> resultData(int rowsReceived, int recordsCreated, int rowsFailed, List<Map<String, Object>> errors)
	{
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("rowsReceived", rowsReceived);
		data.put("recordsCreated", recordsCreated);
		data.put("rowsFailed", rowsFailed);
		data.put("rowErrors", errors);
		return data;
	}

	private static Map<String, Object> rowError(RowImportException exception)
	{
		Map<String, Object> error = new LinkedHashMap<String, Object>();
		error.put("rowId", exception.rowId);
		error.put("error", exception.getMessage());
		return error;
	}

	private static Map<String, Object> errorBody(String message)
	{
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("success", false);
		response.put("messages", List.of(message));
		return response;
	}

	private static String message(Exception exception)
	{
		return exception.getMessage() != null ? exception.getMessage() : "Record could not be created";
	}

	/** Converts expected upload errors to the common REST envelope. */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, Object>> apiError(ApiException exception)
	{
		ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus()).contentType(MediaType.APPLICATION_JSON);
		if (exception.getStatus() == 401)
		{
			response.header("WWW-Authenticate", "Bearer");
		}
		return response.body(errorBody(exception.getMessage()));
	}

	/** Reports multipart uploads rejected by the configured request-size limit. */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, Object>> uploadTooLarge(MaxUploadSizeExceededException exception)
	{
		return ResponseEntity.status(413).contentType(MediaType.APPLICATION_JSON)
				.body(errorBody("Upload exceeds the configured request limit"));
	}

	private static final class CsvUpload
	{
		private final List<CsvRow> rows;
		private final List<Field> fields;

		private CsvUpload(List<CsvRow> rows, List<Field> fields)
		{
			this.rows = rows;
			this.fields = fields;
		}
	}

	private static final class CsvRow
	{
		private final long rowId;
		private final List<String> values;

		private CsvRow(long rowId, List<String> values)
		{
			this.rowId = rowId;
			this.values = values;
		}
	}

	private static final class RowImportException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;
		private final long rowId;

		private RowImportException(long rowId, String message, Throwable cause)
		{
			super(message, cause);
			this.rowId = rowId;
		}
	}
}