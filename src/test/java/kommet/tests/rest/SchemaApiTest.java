package kommet.tests.rest;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.basic.ApiToken;
import kommet.basic.Dictionary;
import kommet.basic.Profile;
import kommet.basic.User;
import kommet.basic.keetle.RequestFilter;
import kommet.data.Env;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KeyPrefix;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.EnvFilter;
import kommet.rest.ApiException;
import kommet.rest.RestAuthenticationService;
import kommet.rest.RestAuthorizationUtil;
import kommet.rest.SchemaApiService;
import kommet.rest.SchemaDataTypeParser;
import kommet.rest.SchemaPayload;
import kommet.services.ApiTokenService;
import kommet.services.DictionaryService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.web.controllers.DeveloperApiController;
import kommet.web.controllers.DataUploadController;

/** Verifies schema API validation, authentication, and HTTP behavior without a database. */
public class SchemaApiTest
{
	private final ObjectMapper mapper = new ObjectMapper();
	private final String secret = "test-secret";
	private EnvData env;
	private EnvData otherEnv;
	private Type owner;
	private Type target;
	private ApiToken token;
	private User user;
	private RestAuthenticationService authenticator;
	private SchemaDataTypeParser parser;
	private MockMvc mvc;
	private boolean mutated;
	private boolean includeFieldsRequested;

	/** Builds environment metadata and isolated service doubles for each test. */
	@Before
	public void prepare() throws Exception
	{
		env = environment("0010000000002");
		otherEnv = environment("0010000000003");
		owner = type("Invoice", 200);
		target = type("Customer", 201);
		env.registerType(owner);
		env.registerType(target);
		Type linking = type("InvoiceCustomer", 202);
		Field self = reference("invoice", owner);
		Field foreign = reference("customer", target);
		linking.addField(self);
		linking.addField(foreign);
		env.registerType(linking);
		Type inverse = type("InvoiceLine", 203);
		inverse.addField(reference("invoice", owner));
		env.registerType(inverse);
		user = new User();
		user.setId(KID.get(KID.USER_PREFIX, 2L));
		user.setIsActive(true);
		token = new ApiToken();
		token.setId(KID.get(KID.API_TOKEN_PREFIX, 1L));
		token.setSystemUser(user);
		token.setIsReadPermission(true);
		token.setIsWritePermission(true);
		authenticator = new RestAuthenticationService();
        
		// Supplies token storage restricted to the primary test environment.
		ReflectionTestUtils.setField(authenticator, "apiTokenService", new ApiTokenService() {
			/** Resolves only the test secret in its owning environment. */
			@Override public ApiToken findValidToken(String supplied, EnvData selected) {
				return secret.equals(supplied) && selected == env ? token : null;
			}
		});
        
		// Supplies a user identity without database-backed permission queries.
		ReflectionTestUtils.setField(authenticator, "userService", new UserService() {
			/** Returns the test user selected by token ownership. */
			@Override public User getUser(KID id, EnvData selected)
			{
				return user;
			}
			/** Builds request-local authentication for the test user. */
			@Override public AuthData getAuthData(User selected, EnvData selectedEnv) throws KommetException {
				// Replaces persistence-backed permission initialization for this fixture.
				AuthData authData = new AuthData() {
					/** Avoids profile permission queries in a database-free test. */
					@Override public void initUserPermissions(EnvData selectedEnv) { }
					/** Avoids cascade-setting queries in a database-free test. */
					@Override public void initUserCascadeSettings(UserCascadeHierarchyService service, EnvData selectedEnv) { }
				};
				Profile profile = new Profile();
				profile.setId(KID.get(Profile.ROOT_ID));
				profile.setName(Profile.ROOT_NAME);
				authData.setProfile(profile);
				authData.setUser(selected);
				authData.setEnvId(selectedEnv.getId());
				return authData;
			}
		});
		parser = new SchemaDataTypeParser();
		// Supplies a deterministic text-length limit for parser validation.
		ReflectionTestUtils.setField(parser, "appConfig", new AppConfig() {
			/** Returns the fixture's maximum text-field length. */
			@Override public int getMaxTextFieldLength() { return 1000; }
		});
		// Supplies one resolvable dictionary without opening database connections.
		ReflectionTestUtils.setField(parser, "dictionaryService", new DictionaryService() {
			/** Resolves the dictionary identifier supported by this fixture. */
			@Override public Dictionary get(KID id, AuthData authData, EnvData selectedEnv) throws KommetException {
				if (!id.equals(KID.get(KID.DICTIONARY_PREFIX, 1L)))
				{
					return null;
				}
				Dictionary dictionary = new Dictionary();
				dictionary.setId(id);
				return dictionary;
			}
		});
		DeveloperApiController controller = new DeveloperApiController();
		RestAuthorizationUtil authorizationUtil = new RestAuthorizationUtil();
		ReflectionTestUtils.setField(authorizationUtil, "allowLoopbackHttp", true);
		ReflectionTestUtils.setField(authorizationUtil, "authenticationService", authenticator);
		ReflectionTestUtils.setField(controller, "authorizationUtil", authorizationUtil);
		// Resolves the two simulated environments used by HTTP authentication tests.
		ReflectionTestUtils.setField(controller, "envService", new EnvService() {
			/** Supplies the master-environment lookup used by the controller. */
			@Override public EnvData getMasterEnv() { return env; }
			/** Reports that the fixture environment exists. */
			@Override public List<Env> find(EnvFilter filter, EnvData selected) { return Collections.singletonList(env.getEnv()); }
			/** Selects the simulated environment by its identifier. */
			@Override public EnvData get(KID id) { return id.equals(env.getId()) ? env : otherEnv; }
		});
		// Records dispatch without performing schema mutations in HTTP-only tests.
		ReflectionTestUtils.setField(controller, "schemaService", new SchemaApiService() {
			/** Returns a representative type shape for controller response tests. */
			@Override public Map<String, Object> getType(String qualifiedName, EnvData selected) {
				return Map.of("package", "acme.billing", "apiName", "Invoice", "fields", List.of());
			}
			/** Returns a representative field shape for controller response tests. */
			@Override public Map<String, Object> getField(String qualifiedName, EnvData selected) {
				return Map.of("apiName", "invoiceNumber", "dataType", Map.of("kind", "text", "length", 80));
			}
			/** Records whether the type-list request asks for fields. */
			@Override public List<Map<String, Object>> getTypes(EnvData selected, boolean includeFields) {
				includeFieldsRequested = includeFields;
				return List.of(Map.of("package", "acme.billing", "apiName", "Invoice"));
			}
			/** Records a successful upsert invocation and returns a creation result. */
			@Override public Map<String, Object> save(JsonNode body, RestAuthenticationService.Authentication authentication, boolean fieldOperation) {
				mutated = true;
				return Map.of("operation", "created", "typeId", owner.getKID().getId());
			}
		});
		mvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	/** Checks every supported datatype payload, including relationship references. */
	@Test
	public void parsesEveryDatatypeShape() throws Exception
	{
		String[] definitions = {
			"{\"kind\":\"text\",\"length\":80,\"long\":false,\"formatted\":true}",
			"{\"kind\":\"number\",\"decimalPlaces\":2,\"javaType\":\"java.math.BigDecimal\"}",
			"{\"kind\":\"boolean\"}", "{\"kind\":\"date\"}", "{\"kind\":\"datetime\"}",
			"{\"kind\":\"email\"}", "{\"kind\":\"kid\"}", "{\"kind\":\"blob\"}",
			"{\"kind\":\"enumeration\",\"values\":[\"Paid\",\"Open\"],\"validateValues\":true}",
			"{\"kind\":\"enumeration\",\"dictionaryId\":\"" + KID.get(KID.DICTIONARY_PREFIX, 1L) + "\"}",
			"{\"kind\":\"multiEnumeration\",\"values\":[\"First\",\"Second\"]}",
			"{\"kind\":\"autoNumber\",\"format\":\"INV-{000000}\"}",
			"{\"kind\":\"typeReference\",\"targetType\":{\"package\":\"acme.billing\",\"apiName\":\"Customer\"}}",
			"{\"kind\":\"formula\",\"formula\":\"\\\"Literal\\\"\",\"returnType\":\"TEXT\"}",
			"{\"kind\":\"inverseCollection\",\"inverseType\":{\"package\":\"acme.billing\",\"apiName\":\"InvoiceLine\"},\"inverseProperty\":\"invoice\"}",
			"{\"kind\":\"association\",\"mode\":\"direct\",\"associatedType\":{\"package\":\"acme.billing\",\"apiName\":\"Customer\"}}",
			"{\"kind\":\"association\",\"mode\":\"linkingType\",\"linkingType\":{\"package\":\"acme.billing\",\"apiName\":\"InvoiceCustomer\"},\"selfLinkingFieldApiName\":\"invoice\",\"foreignLinkingFieldApiName\":\"customer\"}"
		};
		for (String definition : definitions)
		{
			assertNotNull(parser.parse(mapper.readTree(definition), owner, new AuthData(), env));
		}
	}

	/** Ensures the API rejects formulas unsupported by the unchanged shared engine. */
	@Test
	public void formulaApiRespectsExistingParserLimitations() throws Exception
	{
		for (String definition : new String[] {
			"{\"kind\":\"formula\",\"formula\":\"1\",\"returnType\":\"NUMBER\"}",
			"{\"kind\":\"formula\",\"formula\":\"issuedAt\",\"returnType\":\"DATETIME\"}",
			"{\"kind\":\"formula\",\"formula\":\"\\\"O'Reilly\\\"\",\"returnType\":\"TEXT\"}"
		})
		{
			expect(400, () -> parser.parse(mapper.readTree(definition), owner, new AuthData(), env));
		}
	}

	/** Rejects unknown datatype options and incorrectly typed or constrained values. */
	@Test
	public void rejectsUnknownAndWrongKindOptions() throws Exception
	{
		for (String definition : new String[] {
			"{\"kind\":\"Text\",\"length\":80}", "{\"kind\":\"boolean\",\"length\":80}",
			"{\"kind\":\"text\",\"length\":0}", "{\"kind\":\"text\",\"length\":1001}",
			"{\"kind\":\"text\",\"length\":80.5}", "{\"kind\":\"text\",\"length\":80,\"long\":\"true\"}",
			"{\"kind\":\"enumeration\",\"values\":[\"duplicate\",\"duplicate\"]}",
			"{\"kind\":\"number\",\"decimalPlaces\":2,\"javaType\":\"java.lang.Long\"}"
		})
		{
			expect(400, () -> parser.parse(mapper.readTree(definition), owner, new AuthData(), env));
		}
	}

	/** Rejects missing relationship targets and client-supplied storage names. */
	@Test
	public void rejectsMissingAndInvalidReferences() throws Exception
	{
		expect(404, () -> parser.parse(mapper.readTree("{\"kind\":\"typeReference\",\"targetType\":{\"package\":\"acme.billing\",\"apiName\":\"Missing\"}}"), owner, new AuthData(), env));
		expect(400, () -> parser.parse(mapper.readTree("{\"kind\":\"inverseCollection\",\"inverseType\":{\"package\":\"acme.billing\",\"apiName\":\"Customer\"},\"inverseProperty\":\"missing\"}"), owner, new AuthData(), env));
		expect(400, () -> SchemaPayload.keys(mapper.readTree("{\"dbColumn\":\"unsafe\"}"), "apiName"));
	}

	/** Verifies incompatible field storage or reference changes require a migration. */
	@Test
	public void blocksUnsafeStorageChanges() throws Exception
	{
		SchemaApiService.compatible(new TextDataType(80), new TextDataType(80));
		expect(409, () -> SchemaApiService.compatible(new TextDataType(80), new TextDataType(40)));
		expect(409, () -> SchemaApiService.compatible(new TypeReference(owner), new TypeReference(target)));
	}

	/** Checks token ownership, environment isolation, invalid credentials, and revocation. */
	@Test
	public void authenticatesOwnerAndScopesTokenToEnvironment() throws Exception
	{
		RestAuthenticationService.Authentication authentication = authenticator.authenticate("Bearer " + secret, env, RestAuthenticationService.Access.WRITE);
		assertEquals(user.getId(), authentication.getAuthData().getUserId());
		assertEquals(token.getId(), authentication.getTokenId());
		expect(401, () -> authenticator.authenticate("Bearer " + secret, otherEnv, RestAuthenticationService.Access.WRITE));
		expect(401, () -> authenticator.authenticate("Bearer invalid", env, RestAuthenticationService.Access.WRITE));
		token = null;
		expect(401, () -> authenticator.authenticate("Bearer " + secret, env, RestAuthenticationService.Access.WRITE));
	}

	/** Prevents an authenticated identity from mutating a different environment. */
	@Test
	public void schemaServiceRejectsMismatchedEnvironment() throws Exception
	{
		RestAuthenticationService.Authentication authentication = authenticator.authenticate("Bearer " + secret, env, RestAuthenticationService.Access.WRITE);
		JsonNode payload = mapper.readTree("{\"envId\":\"0010000000003\",\"type\":{}}");
		expect(403, () -> new SchemaApiService().save(payload, authentication, false));
	}

	/** Rejects disabled users and tokens without the requested read or write capability. */
	@Test
	public void rejectsInactiveUsersAndInsufficientCapabilities() throws Exception
	{
		token.setIsWritePermission(false);
		expect(403, () -> authenticator.authenticate("Bearer " + secret, env, RestAuthenticationService.Access.WRITE));
		assertNotNull(authenticator.authenticate("Bearer " + secret, env, RestAuthenticationService.Access.READ));
		token.setIsReadPermission(false);
		expect(403, () -> authenticator.authenticate("Bearer " + secret, env, RestAuthenticationService.Access.READ));
		user.setIsActive(false);
		expect(401, () -> authenticator.authenticate("Bearer " + secret, env, RestAuthenticationService.Access.WRITE));
	}

	/** Confirms browser sessions cannot replace a missing API bearer credential. */
	@Test
	public void rejectsMissingBearerRegardlessOfSession() throws Exception
	{
		mvc.perform(post(DeveloperApiController.TYPE_SAVE_PATH).sessionAttr("authData", new AuthData()).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isUnauthorized());
		assertFalse(mutated);
	}

	/** Rejects ambiguous requests carrying more than one Authorization header. */
	@Test
	public void rejectsMultipleBearerCredentials() throws Exception
	{
		mvc.perform(post(DeveloperApiController.TYPE_SAVE_PATH).header("Authorization", "Bearer " + secret, "Bearer other").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isUnauthorized());
		assertFalse(mutated);
	}

	/** Verifies successful responses use JSON and do not establish sessions. */
	@Test
	public void jsonEndpointsReturnCreatedAndStayStateless() throws Exception
	{
		for (String path : new String[] { DeveloperApiController.TYPE_SAVE_PATH, DeveloperApiController.FIELD_SAVE_PATH })
		{
			var response = mvc.perform(post(path).header("Authorization", "Bearer " + secret).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isCreated()).andReturn();
			assertTrue(mapper.readTree(response.getResponse().getContentAsString()).get("success").booleanValue());
			assertNull(response.getRequest().getSession(false));
		}
	}

	/** Verifies read routes authenticate with a bearer and preserve the documented response shapes. */
	@Test
	public void schemaReadEndpointsReturnDocumentedJson() throws Exception
	{
		String envId = env.getId().getId();
		token.setIsWritePermission(false);
		mvc.perform(get(DeveloperApiController.TYPE_PATH)
				.param("envId", envId)
				.param("qualifiedName", "acme.billing.Invoice"))
				.andExpect(status().isUnauthorized());
		var typeResponse = mvc.perform(get(DeveloperApiController.TYPE_PATH)
				.param("envId", envId)
				.param("qualifiedName", "acme.billing.Invoice")
				.header("Authorization", "Bearer " + secret))
				.andExpect(status().isOk()).andReturn();
		JsonNode type = mapper.readTree(typeResponse.getResponse().getContentAsString()).get("data");
		assertEquals("acme.billing", type.get("package").asText());
		assertTrue(type.get("fields").isArray());

		var fieldResponse = mvc.perform(get(DeveloperApiController.FIELD_PATH)
				.param("envId", envId)
				.param("qualifiedName", "acme.billing.Invoice.invoiceNumber")
				.header("Authorization", "Bearer " + secret))
				.andExpect(status().isOk()).andReturn();
		JsonNode field = mapper.readTree(fieldResponse.getResponse().getContentAsString()).get("data");
		assertEquals("invoiceNumber", field.get("apiName").asText());
		assertEquals("text", field.get("dataType").get("kind").asText());

		mvc.perform(get(DeveloperApiController.TYPES_PATH)
				.param("envId", envId)
				.header("Authorization", "Bearer " + secret))
				.andExpect(status().isOk());
		assertFalse(includeFieldsRequested);
		mvc.perform(get(DeveloperApiController.TYPES_PATH)
				.param("envId", envId)
				.param("includeField", "1")
				.header("Authorization", "Bearer " + secret))
				.andExpect(status().isOk());
		assertTrue(includeFieldsRequested);
	}

	/** Verifies schema serializers emit documented definitions from environment metadata. */
	@Test
	public void schemaReadServiceSerializesTypeAndFieldDefinitions() throws Exception
	{
		Field invoiceNumber = new Field();
		invoiceNumber.setApiName("invoiceNumber");
		invoiceNumber.setLabel("Invoice number");
		invoiceNumber.setDataType(new TextDataType(80));
		invoiceNumber.setRequired(true);
		invoiceNumber.setKID(KID.get(KID.FIELD_PREFIX, 300L));
		owner.addField(invoiceNumber);
		owner.setDefaultFieldId(invoiceNumber.getKID());

		SchemaApiService reader = new SchemaApiService();
		Map<String, Object> type = reader.getType("acme.billing.Invoice", env);
		assertEquals("acme.billing", type.get("package"));
		assertTrue(type.containsKey("fields"));
		Map<?, ?> field = (Map<?, ?>)((List<?>)type.get("fields")).get(0);
		assertEquals("invoiceNumber", field.get("apiName"));
		assertEquals(Boolean.TRUE, field.get("isDefault"));
		assertEquals("text", ((Map<?, ?>)field.get("dataType")).get("kind"));
		assertEquals("invoiceNumber", reader.getField("acme.billing.Invoice.invoiceNumber", env).get("apiName"));

		List<Map<String, Object>> typesWithoutFields = reader.getTypes(env, false);
		assertFalse(typesWithoutFields.stream().filter(item -> "Invoice".equals(item.get("apiName"))).findFirst().get().containsKey("fields"));
		List<Map<String, Object>> typesWithFields = reader.getTypes(env, true);
		assertTrue(typesWithFields.stream().filter(item -> "Invoice".equals(item.get("apiName"))).findFirst().get().containsKey("fields"));
	}

	/** Rejects malformed JSON, duplicate keys, and trailing payload content. */
	@Test
	public void rejectsMalformedDuplicateAndTrailingJson() throws Exception
	{
		for (String body : new String[] { "{", body() + " {}", "{\"envId\":\"0010000000002\",\"envId\":\"0010000000003\",\"type\":{}}" })
		{
			mvc.perform(post(DeveloperApiController.TYPE_SAVE_PATH).header("Authorization", "Bearer " + secret).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
		}
		assertFalse(mutated);
	}

	/** Ensures the development HTTP exception cannot authorize a non-loopback peer. */
	@Test
	public void requiresHttpsOutsideLoopback() throws Exception
	{
		mvc.perform(post(DeveloperApiController.TYPE_SAVE_PATH).with(request -> { request.setRemoteAddr("192.0.2.1"); return request; }).header("Authorization", "Bearer " + secret).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
		assertFalse(mutated);
	}

	/** Confirms only the new schema routes bypass existing custom REST authentication. */
	@Test
	public void onlyDeveloperRoutesBypassLegacyFilter()
	{
		RequestFilter filter = new RequestFilter();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app/api/dev/type/save");
		request.setContextPath("/app");
		assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(filter, "shouldNotFilter", request));
		for (String path : new String[] { DeveloperApiController.TYPE_PATH, DeveloperApiController.FIELD_PATH, DeveloperApiController.TYPES_PATH, DataUploadController.UPLOAD_PATH })
		{
			request.setRequestURI("/app" + path);
			assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(filter, "shouldNotFilter", request));
		}
		request.setRequestURI("/app/custom-rest");
		assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(filter, "shouldNotFilter", request));
	}

	/** Builds the minimal body used by controller dispatch tests. */
	private String body()
	{
		return "{\"envId\":\"0010000000002\",\"type\":{}}";
	}

	/** Creates detached environment metadata without a database connection. */
	private static EnvData environment(String id) throws KommetException
	{
		Env definition = new Env();
		definition.setKID(KID.get(id));
		definition.setName("SchemaTest");
		return new EnvData(definition, new DriverManagerDataSource());
	}

	/** Creates a simulated persisted type with stable identifiers and a key prefix. */
	private static Type type(String name, int sequence) throws KommetException
	{
		Type type = new Type();
		type.setApiName(name);
		type.setPackage("acme.billing");
		type.setKID(KID.get(KID.TYPE_PREFIX, (long)sequence));
		type.setId((long)sequence);
		type.setKeyPrefix(KeyPrefix.get(String.format("a%02d", sequence - 199)));
		return type;
	}

	/** Creates a reference-field definition for relationship parser tests. */
	private static Field reference(String name, Type target) throws KommetException
	{
		Field field = new Field();
		field.setApiName(name);
		field.setDataType(new TypeReference(target));
		return field;
	}

	/** Asserts that an operation fails with the expected API status. */
	private static void expect(int status, CheckedOperation operation) throws Exception
	{
		try
		{
			operation.run();
			fail("Expected API failure " + status);
		}
		catch (ApiException exception)
		{
			assertEquals(status, exception.getStatus());
		}
	}

	/** Represents a test operation that may throw a checked exception. */
	private interface CheckedOperation
	{
		/** Executes the operation being checked for an API failure. */
		void run() throws Exception;
	}
}