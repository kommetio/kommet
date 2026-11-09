package kommet.tests.rest;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.basic.ApiToken;
import kommet.basic.Profile;
import kommet.basic.User;
import kommet.data.DataService;
import kommet.data.Env;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KeyPrefix;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.TextDataType;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.EnvFilter;
import kommet.rest.RestAuthenticationService;
import kommet.rest.RestAuthorizationUtil;
import kommet.services.ApiTokenService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.web.controllers.DataUploadController;

/** Verifies synchronous CSV upload semantics without a database. */
public class DataUploadControllerTest
{
	private static final String SECRET = "upload-test-secret";
	private final ObjectMapper mapper = new ObjectMapper();
	private EnvData env;
	private Type invoiceType;
	private RecordingDataService dataService;
	private RecordingTransactionManager transactionManager;
	private MockMvc mvc;

	@Before
	public void prepare() throws Exception
	{
		Env definition = new Env();
		definition.setKID(KID.get("0010000000002"));
		definition.setName("UploadTest");
		env = new EnvData(definition, new DriverManagerDataSource());

		invoiceType = new Type();
		invoiceType.setApiName("Invoice");
		invoiceType.setPackage("acme.billing");
		invoiceType.setKID(KID.get(KID.TYPE_PREFIX, 200L));
		invoiceType.setId(200L);
		invoiceType.setKeyPrefix(KeyPrefix.get("a01"));
		Field invoiceNumber = new Field();
		invoiceNumber.setApiName("invoiceNumber");
		invoiceNumber.setLabel("Invoice number");
		invoiceNumber.setDataType(new TextDataType(80));
		invoiceNumber.setKID(KID.get(KID.FIELD_PREFIX, 300L));
		invoiceNumber.setType(invoiceType);
		invoiceType.addField(invoiceNumber);
		env.registerType(invoiceType);

		User user = new User();
		user.setId(KID.get(KID.USER_PREFIX, 2L));
		user.setIsActive(true);
		ApiToken token = new ApiToken();
		token.setId(KID.get(KID.API_TOKEN_PREFIX, 1L));
		token.setSystemUser(user);
		token.setIsReadPermission(true);
		token.setIsWritePermission(true);
		RestAuthenticationService authenticationService = new RestAuthenticationService();
		ReflectionTestUtils.setField(authenticationService, "apiTokenService", new ApiTokenService() {
			@Override public ApiToken findValidToken(String supplied, EnvData selected) {
				return SECRET.equals(supplied) && selected == env ? token : null;
			}
		});
		ReflectionTestUtils.setField(authenticationService, "userService", new UserService() {
			@Override public User getUser(KID id, EnvData selected) { return user; }
			@Override public AuthData getAuthData(User selected, EnvData selectedEnv) throws KommetException {
				AuthData authData = new AuthData() {
					@Override public void initUserPermissions(EnvData selectedEnv) { }
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
		RestAuthorizationUtil authorizationUtil = new RestAuthorizationUtil();
		ReflectionTestUtils.setField(authorizationUtil, "allowLoopbackHttp", true);
		ReflectionTestUtils.setField(authorizationUtil, "authenticationService", authenticationService);
		EnvService envService = new EnvService() {
			@Override public EnvData getMasterEnv() { return env; }
			@Override public List<Env> find(EnvFilter filter, EnvData selected) { return Collections.singletonList(env.getEnv()); }
			@Override public EnvData get(KID id) { return env; }
		};

		dataService = new RecordingDataService();
		transactionManager = new RecordingTransactionManager(dataService.insertedValues);
		DataUploadController controller = new DataUploadController();
		ReflectionTestUtils.setField(controller, "envService", envService);
		ReflectionTestUtils.setField(controller, "authorizationUtil", authorizationUtil);
		ReflectionTestUtils.setField(controller, "dataService", dataService);
		ReflectionTestUtils.setField(controller, "transactionManager", transactionManager);
		mvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	public void defaultsToAtomicInsertAndRollsBackAllRowsOnFailure() throws Exception
	{
		var result = mvc.perform(multipart(DataUploadController.UPLOAD_PATH)
				.file(csv("invoiceNumber\nINV-1\ninvalid\n"))
				.param("envId", env.getId().getId())
				.param("typeName", "acme.billing.Invoice")
				.header("Authorization", "Bearer " + SECRET))
				.andExpect(status().isBadRequest()).andReturn();

		JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
		assertEquals(0, body.get("data").get("recordsCreated").asInt());
		assertEquals(2, body.get("data").get("rowsFailed").asInt());
		assertTrue(dataService.insertedValues.isEmpty());
		assertEquals(1, transactionManager.rollbackCount);
	}

	@Test
	public void defaultsToAtomicInsertAndCommitsAllValidRows() throws Exception
	{
		var result = mvc.perform(multipart(DataUploadController.UPLOAD_PATH)
				.file(csv("invoiceNumber\nINV-1\nINV-2\n"))
				.param("envId", env.getId().getId())
				.param("typeName", "acme.billing.Invoice")
				.header("Authorization", "Bearer " + SECRET))
				.andExpect(status().isOk()).andReturn();

		JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
		assertEquals(2, body.get("data").get("recordsCreated").asInt());
		assertEquals(0, body.get("data").get("rowsFailed").asInt());
		assertEquals(List.of("INV-1", "INV-2"), dataService.insertedValues);
		assertEquals(1, transactionManager.commitCount);
	}

	@Test
	public void partialInsertKeepsValidRowsAndReportsFailedRow() throws Exception
	{
		var result = mvc.perform(multipart(DataUploadController.UPLOAD_PATH)
				.file(csv("invoiceNumber\nINV-1\ninvalid\nINV-2\n"))
				.param("envId", env.getId().getId())
				.param("typeName", "acme.billing.Invoice")
				.param("partialInsert", "1")
				.header("Authorization", "Bearer " + SECRET))
				.andExpect(status().isOk()).andReturn();

		JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
		assertEquals(2, body.get("data").get("recordsCreated").asInt());
		assertEquals(1, body.get("data").get("rowsFailed").asInt());
		assertEquals(3, body.get("data").get("rowErrors").get(0).get("rowId").asInt());
		assertEquals("invalid field", body.get("data").get("rowErrors").get(0).get("error").asText());
		assertEquals(List.of("INV-1", "INV-2"), dataService.insertedValues);
		assertEquals(1, transactionManager.rollbackCount);
		assertEquals(2, transactionManager.commitCount);
	}

	@Test
	public void rejectsInvalidPartialInsertFlag() throws Exception
	{
		mvc.perform(multipart(DataUploadController.UPLOAD_PATH)
				.file(csv("invoiceNumber\nINV-1\n"))
				.param("envId", env.getId().getId())
				.param("typeName", "acme.billing.Invoice")
				.param("partialInsert", "true")
				.header("Authorization", "Bearer " + SECRET))
				.andExpect(status().isBadRequest());
	}

	private static MockMultipartFile csv(String content)
	{
		return new MockMultipartFile("file", "invoices.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
	}

	private static final class RecordingDataService extends DataService
	{
		private final List<String> insertedValues = new ArrayList<String>();

		@Override
		public Record save(Record record, AuthData authData, EnvData env) throws KommetException
		{
			String value = (String)record.getField("invoiceNumber");
			if ("invalid".equals(value))
			{
				throw new KommetException("invalid field");
			}
			insertedValues.add(value);
			return record;
		}
	}

	private static final class RecordingTransactionManager implements PlatformTransactionManager
	{
		private final List<String> insertedValues;
		private int rollbackCount;
		private int commitCount;

		private RecordingTransactionManager(List<String> insertedValues)
		{
			this.insertedValues = insertedValues;
		}

		@Override
		public TransactionStatus getTransaction(TransactionDefinition definition)
		{
			return new ImportTransactionStatus(insertedValues.size());
		}

		@Override
		public void commit(TransactionStatus status) { commitCount++; }

		@Override
		public void rollback(TransactionStatus status)
		{
			rollbackCount++;
			int originalSize = ((ImportTransactionStatus)status).originalSize;
			while (insertedValues.size() > originalSize)
			{
				insertedValues.remove(insertedValues.size() - 1);
			}
		}
	}

	private static final class ImportTransactionStatus extends SimpleTransactionStatus
	{
		private final int originalSize;

		private ImportTransactionStatus(int originalSize)
		{
			this.originalSize = originalSize;
		}
	}
}
