/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.web;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import kommet.basic.Profile;
import kommet.basic.User;
import kommet.basic.keetle.BaseController;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.koll.SystemContext;
import kommet.koll.annotations.Action;
import kommet.koll.annotations.Controller;
import kommet.koll.annotations.ResponseBody;
import kommet.koll.annotations.Rest;
import kommet.tests.TestDataCreator;
import kommet.utils.UrlUtil;

/**
 * Test creation and invoking REST services.
 * @author Radek Krawiec
 * @since 01/12/2014
 */
public class RestServiceTest extends BaseWebTest
{	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreatingWebService() throws KommetException, JsonParseException, JsonMappingException, IOException
	{
		driver = LoginTest.logIn(driver);
		
		String userName = "radek-" + (new Random()).nextInt(1000) + "@kommet.io";
		String pwd = "admin123";
		
		// create user
		User user = createUser(userName, pwd, Profile.ROOT_NAME);
		assertNotNull(user);
		assertNotNull(user.getId());
		
		// obtain access token for this user
		String accessToken = obtainAccessToken(user.getUserName(), user.getPassword(), KID.get(TestDataCreator.WEB_TEST_ENV_ID));
		
		assertNotNull(accessToken);
		
		String privateRestMethodUrl = "users/get/" + (new Random()).nextInt(1000);
		String publicRestMethodUrl = "users/get/" + (new Random()).nextInt(1000);
		
		String privateRestMethod = "@Rest(url = \"" + privateRestMethodUrl +"\")\npublic User getUser() throws KommetException { ";
		privateRestMethod += "User user = new User(); user.setUserName(\"john\"); ";
		privateRestMethod += "return user; }";
		
		String publicRestMethod = "@Rest(url = \"" + publicRestMethodUrl +"\")\n@Public\npublic User getPublicUser() throws KommetException { ";
		publicRestMethod += "User user = new User(); user.setUserName(\"john\"); ";
		publicRestMethod += "return user; }";
		
		List<String> imports = new ArrayList<String>();
		imports.add("kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME + ".kommet.basic.User");
		imports.add(KommetException.class.getName());
		imports.add(List.class.getName());
		
		String controllerName = "UserController" + (new Random()).nextInt(1000);
		
		String controllerCode = TestDataCreator.getRestServiceController(Arrays.asList(privateRestMethod, publicRestMethod), controllerName, "kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME, imports);
		
		KID classId = createController (null, controllerName, controllerCode, accessToken);
		assertNotNull(classId);
		
		// send accept: application/json header so that the service knows a REST resource is requested
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		
		// now call the private rest action declared in the controller
		String restResponseBody = sendGetRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + privateRestMethodUrl, null, headers);
		Map<String, Object> responseJSON = new ObjectMapper().readValue(restResponseBody, HashMap.class);
		assertEquals(Boolean.FALSE, responseJSON.get("success"));
		assertEquals("Permission denied", responseJSON.get("error"));
		
		// now call the public rest action declared in the controller
		restResponseBody = sendGetRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + publicRestMethodUrl, null, headers);
		responseJSON = new ObjectMapper().readValue(restResponseBody, HashMap.class);
		assertEquals(Boolean.TRUE, responseJSON.get("success"));
		assertEquals("john", responseJSON.get("userName"));
		
		// create param list with authentication data
		Map<String, String> oauthParams = new HashMap<String, String>();
		oauthParams.put("access_token", accessToken);
		oauthParams.put("env", TestDataCreator.WEB_TEST_ENV_ID);
		
		// now send a request to the REST service that should succeed
		restResponseBody = sendGetRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + privateRestMethodUrl, oauthParams, headers);
		responseJSON = new ObjectMapper().readValue(restResponseBody, HashMap.class);
		assertEquals("john", responseJSON.get("userName"));
		
		String restMethodUrl2 = "users/getall/" + (new Random()).nextInt(1000);
		
		// now test a method that returns a list of users
		String restMethod2 = "@Rest(url = \"" + restMethodUrl2 + "\")\npublic List<User> getUsers() throws KommetException { ";
		restMethod2 += "return getSys().query(\"select id, userName from User where userName = 'root'\"); }";
		
		controllerCode = TestDataCreator.getRestServiceController(Arrays.asList(privateRestMethod, restMethod2), controllerName, "kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME, imports);
		
		KID newClassId = createController (classId, controllerName, controllerCode, accessToken);
		assertEquals(classId, newClassId);
		restResponseBody = sendGetRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + restMethodUrl2, oauthParams, headers);
		assertTrue("Invalid response: " + restResponseBody, restResponseBody.startsWith("["));
		assertTrue("Invalid response: " + restResponseBody, restResponseBody.contains("\"userName\": \"root\""));
		
		String restMethodUrl3 = "users/getasrest/" + (new Random()).nextInt(1000);
		
		// now test REST method that returns response body
		String restMethod3 = "@Rest(url = \"" + restMethodUrl3 + "\")\n@ResponseBody\n";
		restMethod3 += "public String getUsersAsRest() throws KommetException { ";
		restMethod3 += "return \"{ \\\"data\\\": \\\"hello\\\" }\"; }";
		
		imports.add(ResponseBody.class.getName());
		
		controllerCode = TestDataCreator.getRestServiceController(Arrays.asList(privateRestMethod, restMethod2, restMethod3), controllerName, "kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME, imports);
		createController (classId, controllerName, controllerCode, accessToken);
		restResponseBody = sendGetRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + restMethodUrl3, oauthParams, headers);
		assertEquals("{ \"data\": \"hello\" }", restResponseBody);
		
		testRollbackOnDbError(oauthParams, accessToken, headers);
		
		// TODO this method has never been finished and does not run successfully
		testTypeCreateErrorRollback(oauthParams, accessToken, headers);
	}
	
	private void testTypeCreateErrorRollback(Map<String, String> oauthParams, String accessToken, Map<String, String> headers) throws KommetException
	{
		Integer randomId = (new Random()).nextInt(100000);
		
		Profile restrictedProfile = createProfile("Restricted Profile " + randomId);
		
		String controllerName = "ExceptionController" + randomId;
		
		// create rest service that will perform so illegal action
		String imports = "import " + Controller.class.getName() + ";\n";
		imports += "import " + Action.class.getName() + ";\n";
		imports += "import " + Rest.class.getName() + ";\n";
		imports += "import " + BaseController.class.getName() + ";\n";
		imports += "import " + KommetException.class.getName() + ";\n";
		imports += "import " + Type.class.getName() + ";\n";
		imports += "import " + Field.class.getName() + ";\n";
		imports += "import " + MultiEnumerationDataType.class.getName() + ";\n";
		imports += "import " + SystemContext.class.getName() + ";\n";
		imports += "import " + KID.class.getName() + ";\n";
		imports += "import kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME + ".kommet.basic.User;\n";
		imports += "import kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME + ".kommet.basic.Profile;\n";
		
		String classCode = "package kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME + ";\n";
		classCode += imports;
		classCode += "@Controller\npublic class " + controllerName + " extends " + BaseController.class.getSimpleName() + " { ";
		classCode += "@Rest(url = \"createtype" + randomId + "\")\n";
		classCode += "public Type createType() throws KommetException {\n";
		classCode += "Type type = new Type(); type.setApiName(\"TestType\");";
		classCode += "type.setPackage(MiscUtils.userToEnvPackage(\"kommet.test\", env));";
		classCode += "type.setLabel(\"Test Type\");";
		classCode += "type.setPluralLabel(\"Test Types\");";
		
		// add enum field to type
		classCode += "Field labelsField = new Field();";
		classCode += "labelsField.setApiName(\"labels\");";
		classCode += "labelsField.setLabel(\"Labels\");";
		classCode += "labelsField.setDataType(new MultiEnumerationDataType());";
		classCode += "labelsField.setRequired(false);";
		classCode += "type.addField(labelsField);";
		
		classCode += "return getSys().getDataService().createType(type, getSys().getEnv()); }";
		classCode += "}";
		
		KID controllerId = createController (null, controllerName, classCode, accessToken);
		assertNotNull(controllerId);
		
		String restResponseBody = sendGetRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + "createtype" + randomId, oauthParams, headers);
		assertTrue("Incorrect response: " + restResponseBody, restResponseBody.contains("\"success\": false"));
	}

	/**
	 * This method makes sure that if an error occurs in a transaction (here: a REST action call), all DB operations from this method such as
	 * inserts or updates are rolled back.
	 * 
	 * It also makes sure that profiles with no access to records can't update these records in REST actions.
	 * 
	 * TODO add tests that make sure record deletion is also rolled back on error. All needs to be done is to 
	 * add a record deletion to REST method updateRecord created in this test method.
	 * 
	 * @param oauthParams
	 * @param adminAccessToken
	 * @param headers
	 * @throws KommetException
	 */
	private void testRollbackOnDbError(Map<String, String> oauthParams, String adminAccessToken, Map<String, String> headers) throws KommetException
	{	
		Integer randomId = (new Random()).nextInt(100000);
		
		Profile restrictedProfile = createProfile("Restricted Profile " + randomId);
		
		// create two users
		User user1 = createUser("restr" + randomId + "@kommet.io", "abc", restrictedProfile.getName());
		assertNotNull(user1); 
		assertNotNull(user1.getId());
		String user1AccessToken = obtainAccessToken(user1.getUserName(), user1.getPassword(), KID.get(TestDataCreator.WEB_TEST_ENV_ID));
		
		User user2 = createUser("restr" + randomId + "@kommet.io", "abc", restrictedProfile.getName());
		assertNotNull(user2); 
		assertNotNull(user2.getId());
		String user2AccessToken = obtainAccessToken(user2.getUserName(), user2.getPassword(), KID.get(TestDataCreator.WEB_TEST_ENV_ID));
		
		String controllerName = "ExceptionController" + randomId;
		String userName = "john" + randomId;
		String newUserName = "test-user-" + (new Random()).nextInt(100000) + "@kommet.io";
		
		// create rest service that will perform so illegal action
		String imports = "import " + Controller.class.getName() + ";\n";
		imports += "import " + Action.class.getName() + ";\n";
		imports += "import " + Rest.class.getName() + ";\n";
		imports += "import " + BaseController.class.getName() + ";\n";
		imports += "import " + KommetException.class.getName() + ";\n";
		imports += "import " + SystemContext.class.getName() + ";\n";
		imports += "import " + KID.class.getName() + ";\n";
		imports += "import kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME + ".kommet.basic.User;\n";
		imports += "import kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME + ".kommet.basic.Profile;\n";
		
		String classCode = "package kommet.envs." + TestDataCreator.WEB_TEST_ENV_NAME + ";\n";
		classCode += imports;
		classCode += "@Controller\npublic class " + controllerName + " extends " + BaseController.class.getSimpleName() + " { ";
		classCode += "@Rest(url = \"createrecord" + randomId + "\")\n";
		classCode += "public User createRecord() throws KommetException {\n";
		classCode += "User user = createUser(\"rollback-" + randomId + "@kommet.io\");\n";
		classCode += "return getSys().save(user); }";
		
		// add method for updating record
		classCode += "@Rest(url = \"updaterecord" + randomId + "\")\n";
		classCode += "public User updateRecord() throws KommetException {\n";
		classCode += "User newUser = createUser(\"" + newUserName + "\");\n";
		classCode += "User user = getSys().queryUniqueResult(\"select id from User where userName = '" + userName + "'\", true);";
		classCode += "return getSys().save(user);}";
		
		classCode += "private User createUser (String userName) throws KommetException\n{";
		classCode += "User user = new User(); user.setUserName(userName);\n";
		classCode += "user.setEmail(userName); user.setIsActive(true);\n";
		classCode += "Profile profile = new Profile(); profile.setId(KID.get(\"0060000000001\"));\n";
		classCode += "user.setProfile(profile); user.setTimezone(\"GMT\"); user.setLocale(\"PL_PL\");\n";
		classCode += "return user; }\n";
		
		classCode += "}";
		
		KID controllerId = createController (null, controllerName, classCode, adminAccessToken);
		assertNotNull(controllerId);
		
		// create record as user one
		oauthParams.put("access_token", user1AccessToken);
		String restResponseBody = sendGetRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + "createrecord" + randomId, oauthParams, headers);
		assertTrue("Incorrect response: " + restResponseBody, restResponseBody.contains("\"userName\": \"rollback-" + randomId + "@kommet.io\""));
		
		// try to modify record as user two - this should fail
		oauthParams.put("access_token", user2AccessToken);
		restResponseBody = sendGetRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + "updaterecord" + randomId, oauthParams, headers);
		assertTrue("Incorrect response: " + restResponseBody, restResponseBody.contains("\"success\": false"));
		
		// prepare a query that will count users with the given name
		oauthParams.put("access_token", adminAccessToken);
		oauthParams.put("q", "select id from User where userName = '" + newUserName + "'");
		
		// REST method updaterecord created a user with user name newUserName, however, it failed,
		// so the user creation should be rolled back, so here we make sure that transaction has been rolled back and no user with name newUserName has been created
		restResponseBody = sendGetRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DAL_URL, oauthParams, headers);
		assertTrue("Incorrect response. Zero results should be returned, instead got:\n" + restResponseBody, restResponseBody.replaceAll("\\s+", "").equals("[]"));
	}
}
