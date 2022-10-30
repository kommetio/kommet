/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.common.base.Function;
import com.thoughtworks.selenium.SeleneseTestBase;

import kommet.basic.Profile;
import kommet.basic.RecordProxyException;
import kommet.basic.User;
import kommet.basic.View;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.TextDataType;
import kommet.exceptions.NotImplementedException;
import kommet.tests.TestDataCreator;
import kommet.utils.UrlUtil;

public class BaseWebTest extends SeleneseTestBase
{
	protected WebDriver driver;
	private static final int PAGE_LOAD_WAIT_SECS = 10;
	private static final String TYPE_LIST_URL = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/types/list";
	
	// stores data created during the test so that we know what to remove after the test is over
	protected TestData testData;
	protected WebAssertions web;
	
	private static final Logger log = LoggerFactory.getLogger(BaseWebTest.class);
	
	@Before
    public void setUp() throws Exception
    {
		testData = new TestData();
		System.setProperty("webdriver.chrome.driver", "f:/dev/kolmu-workspace/kolmu/chromedriver.exe");
		
		// the flag "--test-type" has been added to get rid of an error:
		// "unsupported option flag: --ignore-certificate-errors" which prevented Chrome web driver from starting
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("--test-type");
		driver = new ChromeDriver(chromeOptions);
		
		web = new WebAssertions(driver);
    }
	
	@After
    public void tearDown()
	{
		cleanUp(this.driver); 
        driver.quit();
    }
	
	@Test
	public void stubTest()
	{
		// empty
	}
	
	protected void openTypeDetails(KeyPrefix keyPrefix)
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/type/" + keyPrefix);
	}
	
	protected boolean openViewDetailsByName(String viewName, String packageName, boolean verify) throws KommetException
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "km/" + TestDataCreator.WEB_VIEW_LIST_URL);
		
		assertTrue("View list not opened, instead the URL is " + driver.getCurrentUrl(), driver.getCurrentUrl().endsWith(TestDataCreator.WEB_VIEW_LIST_URL));
		
		// open view details
		List<WebElement> linksToViews = driver.findElements(By.xpath("//table[@id='viewList']/tbody/tr/td/a[.='" + viewName + "']"));
		boolean viewFound = false;
		
		for (WebElement linkToView : linksToViews)
		{
			try
			{
				WebElement packageCell = linkToView.findElement(By.xpath("../../td/following-sibling::td[1]"));
				
				// if the package we're looking for is not empty
				if (StringUtils.hasText(packageName))
				{
					if (packageCell.getText() != null && packageCell.getText().equals(packageName))
					{
						// if view package matches, we can just click the view link
						linkToView.click();
						viewFound = true;
						break;
					}
				}
				// if the package we are looking for is empty
				else
				{
					if (!StringUtils.hasText(packageCell.getText()))
					{
						// just click the view link
						linkToView.click();
						viewFound = true;
						break;
					}
				}
			}
			catch (NoSuchElementException e)
			{
				// there was no cell with the package name found in the same row, so this view has a different package
				// and we just continue to the next view
				continue;
			}
		}
		
		if (!viewFound && verify)
		{
			fail("View with name " + viewName + " and package " + (packageName != null ? packageName : "<empty>") + " not found on view list");
			return false;
		}
		
		// check if view details have been opened
		if (!driver.getTitle().startsWith(viewName))
		{
			if (verify)
			{
				fail("View details apparently not opened since page title is " + driver.getTitle() + ", URL " + driver.getCurrentUrl());
			}
			return false;
		}
		
		return true;
		
		/*
		// go to IDE
		driver.findElement(By.id("openIdeBtn")).click();
		
		// get current tab's file ID
		String fileId = getJavascriptResult("return $(\"#openFiles div.ui-tabs-panel[aria-hidden='false']\").attr('id').split('_')[1];");
		assertTrue(fileId.startsWith(KID.KEETLE_VIEW_PREFIX) && fileId.length() == KID.LENGTH);*/
	}
	
	protected String getOpenIdeTabContent() throws KommetException
	{
		// get current tab's file ID
		String fileId = getOpenIdeFileId();
		
		// read view code from editors var using JS
		return getJavascriptResult("return editors['" + fileId + "'].getValue()");
	}
	
	protected String getOpenIdeFileId() throws KommetException
	{
		String fileId = getJavascriptResult("return $(\"#ide ul.tabbar > li.active\").attr('id').split('-')[1];");
		assertTrue((fileId.startsWith(KID.VIEW_PREFIX) || fileId.startsWith(KID.CLASS_PREFIX))&& fileId.length() == KID.LENGTH);
		return fileId;
	}
	
	protected Profile createProfile (String profileName) throws RecordProxyException, KIDException
	{
		// go to create profile page
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/profiles/new");
		
		driver.findElement(By.name("name")).sendKeys(profileName);
		driver.findElement(By.name("name")).submit();
		
		// read URL to which we have been redirected
		String url = driver.getCurrentUrl();
		assertTrue(url.contains(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/profile/" + KID.PROFILE_PREFIX));
		
		Profile profile = new Profile();
		profile.setName(profileName);
		
		// get ID of the new profile from URL
		profile.setId(KID.get(url.substring(url.indexOf(KID.PROFILE_PREFIX))));
		
		return profile;
	}
	
	@SuppressWarnings("unchecked")
	protected String obtainAccessToken (String userName, String password, KID envId) throws KommetException
	{
		// remove slash from the beginning of the URL
		String oauthURL = ("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.OAUTH_GET_TOKEN_URL).substring(1);
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("grant_type", "password");
		params.put("client_id", "any");
		params.put("client_secret", "any");
		params.put("env", envId.getId());
		params.put("username", userName);
		params.put("password", password);
		
		String responseBody = sendPostRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + oauthURL, params);
		
		Map<String, Object> tokenJSON;
		try
		{
			tokenJSON = new ObjectMapper().readValue(responseBody, HashMap.class);
		}
		catch (Exception e)
		{
			throw new KommetException("Could not parse response body: " + responseBody + ":\n" + e.getMessage());
		}
		
        return (String)tokenJSON.get("access_token");
	}
	
	@SuppressWarnings("unchecked")
	protected KID createController(KID id, String controllerName, String controllerCode, String accessToken) throws KommetException
	{
		Map<String, String> params = new HashMap<String, String>();
		params.put("env", TestDataCreator.WEB_TEST_ENV_ID);
		params.put("access_token", accessToken);
		params.put("name", controllerName);
		params.put("code", controllerCode);
		
		if (id != null)
		{
			params.put("id", id.getId());
		}
		
		String responseBody = sendPostRequestGetResponseBody(TestDataCreator.WEB_TEST_BASE_URL + "km" + UrlUtil.REST_API_SAVE_CLASS_URL, params);
		Map<String, Object> tokenJSON;
		
		try
		{
			tokenJSON = new ObjectMapper().readValue(responseBody, HashMap.class);
		}
		catch (Exception e)
		{
			throw new KommetException("Could not parse response body: " + responseBody + ":\n" + e.getMessage());
		}
		
		try
		{
			return KID.get((String)tokenJSON.get("id"));
		}
		catch (KIDException e)
		{
			fail("Could not get controller ID from response, probably saving file saved. Response:\n" + responseBody);
			return null;
		}
	}
	
	protected String sendPostRequestGetResponseBody(String url, Map<String, String> params) throws KommetException
	{
		HttpResponse resp = sendPostRequest(url, params);
		HttpEntity entity = resp.getEntity();
		try
		{
			return IOUtils.toString(entity.getContent(), entity.getContentEncoding() != null ? entity.getContentEncoding().getValue() : "UTF-8");
		}
		catch (IllegalStateException e)
		{
			throw new KommetException("Error translating HTTP response body into string");
		}
		catch (IOException e)
		{
			throw new KommetException("Error translating HTTP response body into string");
		}
	}
	
	protected String sendGetRequestGetResponseBody(String url, Map<String, String> params, Map<String, String> headers) throws KommetException
	{
		HttpResponse resp = sendGetRequest(url, params, headers);
		HttpEntity entity = resp.getEntity();
		try
		{
			return IOUtils.toString(entity.getContent(), entity.getContentEncoding() != null ? entity.getContentEncoding().getValue() : "UTF-8");
		}
		catch (IllegalStateException e)
		{
			throw new KommetException("Error translating HTTP response body into string");
		}
		catch (IOException e)
		{
			throw new KommetException("Error translating HTTP response body into string");
		}
	}
	
	protected HttpResponse sendGetRequest(String url, Map<String, String> params, Map<String, String> headers) throws KommetException
	{	 
		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		
		URIBuilder urlBuilder = new URIBuilder(httpGet.getURI());
		
		if (params != null && !params.isEmpty())
		{
			for (String paramName : params.keySet())
			{
				urlBuilder.setParameter(paramName, params.get(paramName));
			}
		}
		
		try
		{
			httpGet.setURI(urlBuilder.build());
		}
		catch (URISyntaxException e)
		{
			throw new KommetException("Invalid URI: " + e.getMessage());
		}
		
		if (headers != null)
		{
			for (String headerName : headers.keySet())
			{
				httpGet.addHeader(headerName, headers.get(headerName));
			}
		}

		try
		{
			return httpclient.execute(httpGet);
		}
		catch (ClientProtocolException e)
		{
			throw new KommetException("Error sending HTTP POST request: " + e.getMessage());
		}
		catch (IOException e)
		{
			throw new KommetException("Error sending HTTP POST request: " + e.getMessage());
		}
	}

	protected HttpResponse sendPostRequest(String url, Map<String, String> params) throws KommetException
	{	 
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(url);

		// Request parameters and other properties.
		List<NameValuePair> paramList = new ArrayList<NameValuePair>();
		
		if (params != null)
		{
			for (String paramName : params.keySet())
			{
				paramList.add(new BasicNameValuePair(paramName, params.get(paramName)));
			}
		}
		
		try
		{
			httppost.setEntity(new UrlEncodedFormEntity(paramList, "UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			throw new KommetException("Unsupported HTTP encoding");
		}

		//Execute and get the response.
		try
		{
			return httpclient.execute(httppost);
		}
		catch (ClientProtocolException e)
		{
			throw new KommetException("Error sending HTTP POST request: " + e.getMessage());
		}
		catch (IOException e)
		{
			throw new KommetException("Error sending HTTP POST request: " + e.getMessage());
		}
	}

	protected User createUser (String userName, String password, String profileName) throws KommetException
	{
		// go to create profile page
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/users/new");
		
		driver.findElement(By.name("userName")).sendKeys(userName);
		driver.findElement(By.name("email")).sendKeys(userName);
		driver.findElement(By.name("password")).sendKeys(password);
		driver.findElement(By.name("repeatedPassword")).sendKeys(password);
		selectDropdownOptionByText(driver.findElement(By.name("profileId")), profileName);
		driver.findElement(By.name("timezone")).sendKeys("GMT");
		selectDropdownOptionByText(driver.findElement(By.name("locale")), "English");
		
		// submit form
		driver.findElement(By.name("userName")).submit();
		
		// read URL to which we have been redirected
		String url = driver.getCurrentUrl();
		
		if (!url.contains(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/user/" + KID.USER_PREFIX))
		{
			this.web.fail("Invalid URL after saving user: " + driver.getCurrentUrl());
		}
		
		User user = new User();
		user.setUserName(userName);
		user.setPassword(password);
		
		// get ID of the new profile from URL
		user.setId(KID.get(url.substring(url.indexOf(KID.USER_PREFIX))));
		
		return user;
	}
	
	public void assertIDETabActive(String tabName) throws KommetException
	{
		
		
		throw new NotImplementedException("Method not implemented");
	}
	
	protected String getJavascriptResult(String jsCode) throws KommetException
	{
		if (driver instanceof JavascriptExecutor)
		{
		    return (String)((JavascriptExecutor)driver).executeScript(jsCode);
		}
		else
		{
			throw new KommetException("WebDriver is not a JavascriptExecutor");
		}
	}
	
	protected void openNewViewPage() throws KommetException
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "km/" + TestDataCreator.WEB_NEW_VIEW_URL);
		web.assertTrue(driver.getTitle().startsWith("New view"));
	}
	
	protected void openRecordList(KeyPrefix keyPrefix)
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + keyPrefix);
	}
	
	protected void testUIFieldValue(String label, String expectedValue) throws KommetException
	{
		assertEquals(expectedValue, getUIFieldValue(label));
	}
	
	protected String getUIFieldValue (String label) throws KommetException
	{
		try
		{
			driver.findElement(By.xpath("//td[.='" + label + "']"));
		}
		catch (NoSuchElementException e)
		{
			try
			{
				driver.findElement(By.cssSelector("div.km-rd-table"));
				fail("Label '" + label + "' not found on page");
			}
			catch (NoSuchElementException e1)
			{
				fail("Label '" + label + "' not found because the page did not contain a property table at all");
			}
		}
		
		WebElement valueCell = null;
		
		try
		{
			// select the first TD element after the TD with specified content 
			valueCell = driver.findElement(By.xpath("//td[.='" + label + "']/following-sibling::td[1]"));
			
			// make sure the retrieved element has class = "value"
			if (!valueCell.getAttribute("class").startsWith("value"))
			{
				throw new KommetException("The next sibling of a label cell is not a value cell");
			}
		}
		catch (NoSuchElementException e)
		{
			fail("Value cell for label '" + label + "' not found");
		}
		
		return valueCell.getText();
	}
	
	protected void openSetup()
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/setup");
	}
	
	/**
	 * This method clicks the link identified by a given locator and then waits for some time for
	 * the page to load. It should only be called with links that invoke new pages.
	 * @param locator
	 */
	protected void clickLink(final By locator)
	{
		driver.findElement(locator).click();
		waitForPage();
	}
	
	protected void createType (Type type) throws KommetException
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + TYPE_LIST_URL);
		
		assertTrue("Incorrect page title: " + driver.getTitle(), driver.getTitle().startsWith("Types"));
		driver.findElement(By.id("newTypeBtn")).click();
		assertTrue(driver.getTitle().startsWith("New type"));
		driver.findElement(By.name("apiName")).sendKeys(type.getApiName());
		driver.findElement(By.name("label")).sendKeys(type.getLabel());
		driver.findElement(By.name("pluralLabel")).sendKeys(type.getPluralLabel());
		
		if (StringUtils.hasText(type.getPackage()))
		{
			driver.findElement(By.name("packageName")).sendKeys(type.getPackage());
		}
		
		clickLink(By.id("saveTypeBtn"));
		assertTrue(driver.getTitle().startsWith(type.getLabel()));
		
		String keyPrefix = driver.getCurrentUrl().substring(driver.getCurrentUrl().length() - KeyPrefix.LENGTH);
		// key prefix is not set on type so we need to get it from URL for further tests
		type.setKeyPrefix(KeyPrefix.get(keyPrefix));
		
		for (Field field : type.getFields())
		{
			if (Field.isSystemField(field.getApiName()))
			{
				continue;
			}
			
			createField(type, field);
		}
	}
	
	/**
	 * Gets the web element representing the col-th cell in the row-th row of a table identified by the given tableId.
	 * If element not found, returns null;
	 * @param string
	 * @param i
	 * @param j
	 * @return
	 */
	protected WebElement getTableCell(String tableId, int row, int col)
	{
		try
		{
			return driver.findElement(By.xpath("//table[@id='" + tableId + "']//tbody/tr[" + (row + 1) + "]/td[" + (col + 1 + "]")));
		}
		catch (NoSuchElementException e)
		{
			return null;
		}
	}
	
	protected void deleteViewThroughLink(KID viewId, boolean verify) throws KommetException
	{
		openViewDetailsById(viewId, verify);
		deleteViewOnceOnViewDetails(viewId, verify);
	}
	
	protected void openViewDetailsById(KID viewId, boolean verify)
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "views/" + viewId);
		if (verify)
		{
			assertTrue(driver.getCurrentUrl().endsWith("/" + viewId));
		}
	}

	private void deleteViewOnceOnViewDetails(KID viewId, boolean verify) throws KommetException
	{
		// check if view details have been opened
		assertTrue("When method deleteViewOnceOnViewDetails, you should already be on view details screen (for view " + viewId + "), instead the URL is " + driver.getCurrentUrl(), driver.getCurrentUrl().endsWith("/" + viewId));
		
		// click the delete button
		driver.findElement(By.id("deleteViewBtn")).click();
		
		// a prompt will appear, click "Yes"
		clickYesOnPrompt();
		
		if (verify)
		{
			// make sure you are taken to the view list
			web.assertTitleStartsWith("View list not opened, expected page title 'Keetle Views', but found '" + driver.getTitle() + "'", "Keetle Views");
			
			// make sure a link to the view does not work
			driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/views/" + viewId);
			web.assertTrue(driver.getTitle().startsWith("Message"));
		}
	}
	
	protected void deleteViewThroughList(KID viewId, String viewName, String packageName, boolean verify) throws KommetException
	{
		boolean detailsOpened = openViewDetailsByName(viewName, packageName, verify);
		
		// when this method is called from the cleanUp method, it is possible that the details of the view
		// will not be opened because the view no longer exists - we allow this
		if (detailsOpened)
		{
			deleteViewOnceOnViewDetails(viewId, verify);
		}
		else if (verify)
		{
			fail("Opening details for view " + viewId + "failed");
		}
	}

	protected void clickYesOnPrompt() throws KommetException
	{
		try
		{
			driver.findElement(By.xpath("//div[contains(@class,'ask')]//input[@value='Yes']")).click();
		}
		catch (NoSuchElementException e)
		{
			web.fail("'Yes' button or whole prompt message not found");
		}
	}
	
	protected void setFieldValueByName(String name, String value)
	{
		driver.findElement(By.name(name)).clear();
		driver.findElement(By.name(name)).sendKeys(value);
	}
	
	/**
	 * Gets the value of the selected option of the select element identified by the given ID
	 * @param selectId
	 * @return
	 * @throws KommetException 
	 */
	protected String getSelectedOption(String selectId) throws KommetException
	{
		return getJavascriptResult("return $('#" + selectId + "').val()");
	}
	
	protected void setOpenIdeTabContent(String content, boolean verify) throws KommetException
	{
		String fileId = getOpenIdeFileId();
		String normalizedContent = content.replaceAll("\\r?\\n", "");
		getJavascriptResult("return editors['" + fileId + "'].setValue('" + normalizedContent + "');");
		
		if (verify)
		{
			assertTrue(normalizedContent.equals(getOpenIdeTabContent()));
		}
	}
	
	protected void openTypesList()
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + TYPE_LIST_URL.substring(1));
	}

	protected void deleteType(Type type, boolean verify) throws KommetException
	{
		openTypeDetails(type.getKeyPrefix());
		
		deleteRelatedPages(type);
		
		openTab(type.getLabel());
		driver.findElement(By.id("deleteTypeBtn")).click();
		clickYesOnPrompt();
		
		if (verify)
		{	
			// type does not exist so there should be no link to it
			try
			{
				web.assertPageHeader("Types");
				// refresh the page because it sometimes used buffered content with the deleted type still being displayed
				driver.navigate().refresh();
				driver.findElement(By.linkText(type.getLabel())).click();
				fail("Link to deleted type " + type.getLabel() + " should not be on the list of types");
			}
			catch (NoSuchElementException e)
			{
				// expected
			}
		}
	}
	
	private void deleteRelatedPages(Type type) throws KIDException, KommetException
	{
		openTypeDetails(type.getKeyPrefix());
		openTab("Related actions");
		
		List<WebElement> relatedPageLinks = driver.findElements(By.xpath("//table[@id='relatedPageList']/tbody/tr/td[1]/a"));
		
		log.debug("Found " + relatedPageLinks.size() + " related page links");
		for (WebElement relatedPageLink : relatedPageLinks)
		{
			String pageId = relatedPageLink.getAttribute("href").substring(relatedPageLink.getAttribute("href").lastIndexOf("/") + 1);
			log.debug("Deleting page with ID " + pageId);
			deletePage(KID.get(pageId));
		}
	}

	private void deletePage(KID pageId) throws KommetException
	{
		openPageDetails(pageId);
		
		driver.findElement(By.id("deletePageBtn")).click();
		clickYesOnPrompt();
		
		// make sure you have been taken back to the page list
		web.assertTitleStartsWith("Pages");
	}

	private void openPageDetails(KID pageId)
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "pages/" + pageId);
	}

	protected void createField(Type type, Field field) throws KommetException
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/type/" + type.getKeyPrefix());
		assertTrue(driver.getTitle().startsWith(type.getLabel()));
		
		// go to fields tab
		openTab("Fields");
		
		driver.findElement(By.id("newFieldBtn")).click();
		assertTrue(driver.getTitle().startsWith("New field"));
		driver.findElement(By.name("apiName")).sendKeys(field.getApiName());
		driver.findElement(By.name("label")).sendKeys(field.getLabel());
		driver.findElement(By.name("dataType")).sendKeys(type.getApiName());
		driver.findElement(By.cssSelector(("select#dataType > option[value=\"" + field.getDataTypeId() + "\"]"))).click();
		
		// click somewhere outside the select for the changes in select to have effect
		driver.findElement(By.name("apiName")).click();
		
		// fill additional data type fields depending on data type
		switch (field.getDataTypeId())
		{
			case DataType.TEXT: driver.findElement(By.name("textDataTypeLength")).sendKeys(((TextDataType)field.getDataType()).getLength().toString());
			case DataType.NUMBER: break;
			default: throw new KommetException("Data type " + field.getDataType().getName() + " not supported in web tests");
		}
		
		// if required, mark it
		if (field.isRequired())
		{
			driver.findElement(By.name("required")).click();
		}
		
		// click "Save"
		driver.findElement(By.id("saveFieldBtn")).click();
		
		// make sure type page is opened
		web.assertTrue(driver.getTitle().startsWith(type.getLabel()));
		
		// open fields tab
		openTab("Fields");
		
		// make sure there is a link to the field
		driver.findElement(By.linkText(field.getLabel())).isDisplayed();
	}
	
	/**
	 * Opens a tab of an km.js.tab component.
	 * @param tabLabel
	 */
	protected void openTab(String tabLabel)
	{
		driver.findElement(By.xpath("//ul[contains(@class,'km-tabs-head')]/li[(.='" + tabLabel + "')]")).click();
	}

	protected void selectDropdownOptionByText(WebElement selectElem, final String value)
	{
	    List<WebElement> options = selectElem.findElements(By.tagName("option"));

	    for (WebElement option : options)
	    {
	        if (value.equals(option.getText()))
	        {
	            option.click();
	            break;
	        }
	    }
	}

	protected void waitForPage()
	{
	    Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
	            .withTimeout(PAGE_LOAD_WAIT_SECS, TimeUnit.SECONDS)
	            .pollingEvery(1, TimeUnit.SECONDS)
	            .ignoring(NoSuchElementException.class);

	    wait.until(new Function<WebDriver, WebElement>() {
	        public WebElement apply(WebDriver driver) {
	            return driver.findElement(By.tagName("body"));
	        }
	    });
	}
	
	protected void setDefaultField(Type type, String fieldApiName) throws KommetException
	{
		openTypeEdit(type.getKeyPrefix());
		Field field = type.getField(fieldApiName);
		if (field == null)
		{
			throw new KommetException("Field " + fieldApiName + " not found on type " + type.getQualifiedName());
		}
		selectDropdownOptionByText(driver.findElement(By.id("defaultField")), field.getLabel());
		driver.findElement(By.id("saveTypeBtn")).click();
	}
	
	protected void deleteAndConfirm() throws KommetException
	{
		driver.findElement(By.linkText("Delete")).click();
		clickYesOnPrompt();
	}
	
	protected void deleteField(Type type, String fieldApiName) throws KommetException
	{
		Field field = type.getField(fieldApiName);
		
		if (field == null)
		{
			throw new KommetException("Field " + fieldApiName + " not found on type " + type.getQualifiedName());
		}
		
		if (field.getKID() != null)
		{
			// simple open field URL
			openFieldDetails(field.getKID());
		}
		else
		{
			// open type details and then field
			openFieldDetails(type, field.getLabel());
		}
		
		// assert we're in field details screen
		web.assertTitleStartsWith(field.getLabel());
		// click delete on field details
		deleteAndConfirm();
		// make sure type details page is opened
		web.assertTitleStartsWith(type.getLabel());
	}
	
	protected void openFieldDetails(Type type, String fieldLabel)
	{
		openTypeDetails(type.getKeyPrefix());
		openTab("Fields");
		// go into field details
		driver.findElement(By.xpath("//table[@id='fieldList']/tbody/tr/td/a[.='" + fieldLabel + "']")).click();
	}
	
	protected void openFieldDetails(KID fieldId)
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/field/" + fieldId);
	}

	protected void openTypeEdit(KeyPrefix keyPrefix)
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/types/edit/" + keyPrefix);
	}

	private void cleanUp(WebDriver driver)
	{
		log.debug("Cleanup method called");
		if (this.testData == null)
		{
			return;
		}
		
		// clean up types
		if (this.testData.getTypes() != null)
		{
			log.debug("Types to remove: " + testData.getTypes().size());
			for (Type type : this.testData.getTypes())
			{
				log.debug("Cleaning up type " + type.getQualifiedName());
				try
				{
					deleteType(type, false);
					log.debug("success");
				}
				catch (Exception e)
				{
					// ignore any errors
				}
			}
		}
		
		// clean up KTL views
		if (this.testData.getViews() != null)
		{
			for (View view : this.testData.getViews())
			{
				try
				{
					deleteViewThroughList(view.getId(), view.getInterpretedName(), view.getPackageName(), false);
				}
				catch (Exception e)
				{
					// ignore any errors
				}
			}
		}
	}
	
	class TestData
	{
		private List<Type> types;
		private List<View> views;
		
		@Test
		public void stubTest()
		{
			// empty
		}
		
		public void addType (Type type)
		{
			log.debug("Adding type to delete: " + type.getQualifiedName());
			if (this.types == null)
			{
				this.types = new ArrayList<Type>();
			}
			this.types.add(type);
		}

		public void setTypes(List<Type> types)
		{
			this.types = types;
		}

		public List<Type> getTypes()
		{
			return types;
		}

		public List<View> getViews()
		{
			return views;
		}

		public void addView(KID viewId, String viewName, String viewPackage) throws KommetException
		{
			View view = new View();
			view.setId(viewId);
			view.setName(viewName);
			view.setPackageName(viewPackage);
			
			if (this.views == null)
			{
				this.views = new ArrayList<View>();
			}
			this.views.add(view);
		}
	}
}
