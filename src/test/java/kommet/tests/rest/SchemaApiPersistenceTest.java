package kommet.tests.rest;

import static org.junit.Assert.*;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.springframework.test.context.transaction.TestTransaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.ApiToken;
import kommet.basic.User;
import kommet.data.DataService;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.rest.ApiException;
import kommet.rest.RestAuthenticationService;
import kommet.rest.SchemaApiService;
import kommet.services.ApiTokenService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

/** Exercises schema upserts and rollback against the transactional PostgreSQL fixtures. */
public class SchemaApiPersistenceTest extends BaseUnitTest
{
	@Inject private TestDataCreator dataHelper;
	@Inject private BasicSetupService setupService;
	@Inject private ApiTokenService tokenService;
	@Inject private RestAuthenticationService authenticationService;
	@Inject private SchemaApiService schemaService;
	@Inject private UserService userService;
	@Inject private DataService dataService;

	private final ObjectMapper mapper = new ObjectMapper();

	/** Verifies token permissions persist, reload, and authorize write access. */
	@Test
	public void apiTokenPermissionsSurvivePersistenceAndSearch() throws Exception
	{
		EnvData env = fixture();
		User root = userService.getUser(env.getRootUser().getKID(), env);
		ApiTokenService.CreatedApiToken created = tokenService.add("Permission round-trip", null, true, true, root, env);
		ApiToken loaded = tokenService.search(root.getId(), env).stream()
				.filter(token -> token.getId().equals(created.getApiToken().getId()))
				.findFirst().orElse(null);

		assertNotNull(loaded);
		assertTrue(loaded.getIsReadPermission());
		assertTrue(loaded.getIsWritePermission());
		authenticationService.authenticate("Bearer " + created.getSecret(), env, RestAuthenticationService.Access.WRITE);
	}

	/** Verifies repeated upserts preserve identities, fields, defaults, and unique checks. */
	@Test
	public void typeAndFieldUpsertsPreserveIdentityAndMetadata() throws Exception
	{
		EnvData env = fixture();
		RestAuthenticationService.Authentication authentication = authentication(env);
		JsonNode typePayload = mapper.readTree("{\"environmentId\":\"" + env.getId() + "\",\"type\":{\"package\":\"acme.billing\",\"apiName\":\"Invoice\",\"label\":\"Invoice\",\"pluralLabel\":\"Invoices\",\"description\":\"Preserved description\"}}");
		Map<String, Object> created = schemaService.save(typePayload, authentication, false);
		assertEquals("created", created.get("operation"));
		Map<String, Object> updated = schemaService.save(typePayload, authentication, false);
		assertEquals("updated", updated.get("operation"));
		assertEquals(created.get("typeId"), updated.get("typeId"));

		JsonNode fieldPayload = mapper.readTree("{\"environmentId\":\"" + env.getId() + "\",\"type\":{\"package\":\"acme.billing\",\"apiName\":\"Invoice\"},\"field\":{\"apiName\":\"invoiceNumber\",\"label\":\"Invoice number\",\"required\":true,\"unique\":true,\"isDefault\":true,\"dataType\":{\"kind\":\"text\",\"length\":80}}}");
		Map<String, Object> fieldCreated = schemaService.save(fieldPayload, authentication, true);
		Map<String, Object> fieldUpdated = schemaService.save(fieldPayload, authentication, true);
		assertEquals("created", fieldCreated.get("operation"));
		assertEquals("updated", fieldUpdated.get("operation"));
		assertEquals(fieldCreated.get("fieldId"), fieldUpdated.get("fieldId"));
		Type type = env.getType("acme.billing.Invoice");
		assertEquals(type.getField("invoiceNumber").getKID(), type.getDefaultFieldId());
		assertNotNull(type.getField("invoiceNumber").getDbColumn());
		kommet.data.KID uniqueFieldId = type.getField("invoiceNumber").getKID();
		assertEquals(1L, type.getUniqueChecks().stream().filter(check -> check.getParsedFieldIds().size() == 1 && check.getParsedFieldIds().contains(uniqueFieldId)).count());
		schemaService.save(typePayload, authentication, false);
		assertNotNull(env.getType("acme.billing.Invoice").getField("invoiceNumber"));
		assertEquals("Preserved description", env.getType("acme.billing.Invoice").getDescription());
	}

	/** Persists each supported datatype shape through the schema API service. */
	@Test
	public void supportedDatatypeShapesCanBeCreated() throws Exception
	{
		EnvData env = fixture();
		RestAuthenticationService.Authentication authentication = authentication(env);
		String identity = "{\"package\":\"acme.billing\",\"apiName\":\"DatatypeInvoice\"}";
		schemaService.save(mapper.readTree("{\"environmentId\":\"" + env.getId() + "\",\"type\":{\"package\":\"acme.billing\",\"apiName\":\"DatatypeInvoice\",\"label\":\"Invoice\",\"pluralLabel\":\"Invoices\"}}"), authentication, false);
		String[] definitions = {
			"{\"kind\":\"text\",\"length\":80}",
			"{\"kind\":\"number\",\"decimalPlaces\":2,\"javaType\":\"java.math.BigDecimal\"}",
			"{\"kind\":\"boolean\"}", "{\"kind\":\"date\"}", "{\"kind\":\"datetime\"}",
			"{\"kind\":\"email\"}", "{\"kind\":\"kid\"}", "{\"kind\":\"blob\"}",
			"{\"kind\":\"enumeration\",\"values\":[\"Open\",\"Closed\"]}",
			"{\"kind\":\"multiEnumeration\",\"values\":[\"First\",\"Second\"]}",
			"{\"kind\":\"autoNumber\",\"format\":\"INV-{000000}\"}",
			"{\"kind\":\"typeReference\",\"targetType\":" + identity + "}",
			"{\"kind\":\"formula\",\"formula\":\"\\\"Literal\\\"\",\"returnType\":\"TEXT\"}",
			"{\"kind\":\"inverseCollection\",\"inverseType\":" + identity + ",\"inverseProperty\":\"field11\"}",
			"{\"kind\":\"association\",\"mode\":\"direct\",\"associatedType\":" + identity + "}"
		};
		for (int index = 0; index < definitions.length; index++)
		{
			JsonNode payload = mapper.readTree("{\"environmentId\":\"" + env.getId() + "\",\"type\":" + identity + ",\"field\":{\"apiName\":\"field" + index + "\",\"label\":\"Field\",\"required\":" + (index == 10) + ",\"dataType\":" + definitions[index] + "}}");
			assertEquals("created", schemaService.save(payload, authentication, true).get("operation"));
			assertNotNull(env.getType("acme.billing.DatatypeInvoice").getField("field" + index));
		}
	}

	/** Confirms schema automation cannot add fields to a built-in type. */
	@Test
	public void builtInTypesCannotBeModified() throws Exception
	{
		EnvData env = fixture();
		JsonNode payload = mapper.readTree("{\"environmentId\":\"" + env.getId() + "\",\"type\":{\"package\":\"kommet.basic\",\"apiName\":\"User\"},\"field\":{\"apiName\":\"unsafeField\",\"label\":\"Unsafe\",\"dataType\":{\"kind\":\"boolean\"}}}");
		try { schemaService.save(payload, authentication(env), true); fail("Expected forbidden built-in mutation"); }
		catch (ApiException exception) { assertEquals(403, exception.getStatus()); }
		assertNull(env.getType("kommet.basic.User").getField("unsafeField"));
	}

	/** Checks failed field updates are rolled back at the enclosing transaction boundary. */
	@Test
	public void failedFieldSaveRollsBackAtTheTransactionBoundary() throws Exception
	{
		EnvData env = fixture();
		RestAuthenticationService.Authentication authentication = authentication(env);
		JsonNode typePayload = mapper.readTree("{\"environmentId\":\"" + env.getId() + "\",\"type\":{\"package\":\"acme.billing\",\"apiName\":\"RollbackInvoice\",\"label\":\"Invoice\",\"pluralLabel\":\"Invoices\"}}");
		schemaService.save(typePayload, authentication, false);
		JsonNode fieldPayload = mapper.readTree("{\"environmentId\":\"" + env.getId() + "\",\"type\":{\"package\":\"acme.billing\",\"apiName\":\"RollbackInvoice\"},\"field\":{\"apiName\":\"optionalNumber\",\"label\":\"Number\",\"required\":false,\"isDefault\":true,\"dataType\":{\"kind\":\"text\",\"length\":80}}}");
		try { schemaService.save(fieldPayload, authentication, true); fail("An optional field cannot be the default"); }
		catch (ApiException exception) { assertEquals(400, exception.getStatus()); }
		TestTransaction.flagForRollback();
		TestTransaction.end();
		assertNull(dataService.getTypeByName("acme.billing.RollbackInvoice", true, env));
	}

	/** Initializes the existing rollback-based test environment with built-in types. */
	private EnvData fixture() throws Exception
	{
		EnvData env = dataHelper.getTestEnvData(false);
		setupService.runBasicSetup(env);
		return env;
	}

	/** Issues and authenticates a write-capable token for the fixture's root user. */
	private RestAuthenticationService.Authentication authentication(EnvData env) throws Exception
	{
		User root = userService.getUser(env.getRootUser().getKID(), env);
		String secret = tokenService.add("Schema integration test", null, true, true, root, env).getSecret();
		return authenticationService.authenticate("Bearer " + secret, env, RestAuthenticationService.Access.WRITE);
	}
}