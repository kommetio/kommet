/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.web;

import static org.junit.Assert.assertNotNull;

import java.util.Random;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.springframework.util.StringUtils;

import kommet.basic.keetle.ViewUtil;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.tests.TestDataCreator;
import kommet.utils.MiscUtils;
import kommet.utils.ValidationUtil;

public class ViewTest extends BaseWebTest
{	
	@Test
	public void testCreateKeetleView() throws KommetException
	{
		driver = LoginTest.logIn(driver);
		openNewViewPage();
		
		String viewName = "SomeIllegalViewName__";
		String firstViewPackage = "some.pack";
		
		driver.findElement(By.name("name")).sendKeys(viewName);
		driver.findElement(By.name("package")).sendKeys(firstViewPackage);
		// press save
		driver.findElement(By.id("saveViewBtn")).click();
		// make sure saving view with illegal view name failed
		web.assertErrorDisplayed("Invalid view name. " + ValidationUtil.INVALID_RESOURCE_ERROR_EXPLANATION);
		
		// make sure that view name and package are still filled when page was reloaded with errors
		web.assertTrue("View name not repopulated when page returned an error", viewName.equals(driver.findElement(By.name("name")).getAttribute("value")));
		web.assertTrue("Package name not repopulated when page returned an error, actual value " + driver.findElement(By.name("package")).getAttribute("value"), firstViewPackage.equals(driver.findElement(By.name("package")).getAttribute("value")));
		
		// change view name to a valid one
		viewName = "SomeViewName" + String.valueOf((new Random()).nextInt(10000));
		driver.findElement(By.name("name")).clear();
		driver.findElement(By.name("name")).sendKeys(viewName);
		// press save
		driver.findElement(By.id("saveViewBtn")).click();
		// make sure Kommet IDE was opened
		web.assertTrue(driver.getTitle().startsWith("Kommet IDE"));
	
		String viewCode = getOpenIdeTabContent();
		String firstViewId = getOpenIdeFileId();
		
		testData.addView(KID.get(firstViewId), viewName, firstViewPackage);
		
		// now try to go to view details
		openViewDetailsById(KID.get(firstViewId), true);
		
		// now we have view details opened, so the URL should end with the view ID
		assertTrue("Expected page URL to end with view ID " + firstViewId + ", instead found " + driver.getCurrentUrl(), driver.getCurrentUrl().endsWith("/" + firstViewId));
		
		// make sure the view content is displayed
		assertTrue(viewCode.startsWith("<km:view name=\"" + viewName + "\" package=\"" + firstViewPackage + "\">"));
		
		// make sure the view code displayed in IDE is the same as the one that should be generated by default
		assertEquals(ViewUtil.getEmptyViewCode(viewName, firstViewPackage), viewCode);
		
		// try to add another view with the same name and package
		openNewViewPage();
		driver.findElement(By.name("name")).sendKeys(viewName);
		driver.findElement(By.name("package")).sendKeys(firstViewPackage);
		// press save
		driver.findElement(By.id("saveViewBtn")).click();
		// make sure saving view with illegal view name failed
		web.assertErrorDisplayed("A view with the given name and package already exists.");
		
		// press save
		driver.findElement(By.id("saveViewBtn")).click();
		
		String secondViewPackage = firstViewPackage + ".new";
		
		// now change the package name (but leave the view name) and try to save again
		driver.findElement(By.name("package")).clear();
		driver.findElement(By.name("package")).sendKeys(secondViewPackage);
		// press save
		driver.findElement(By.id("saveViewBtn")).click();
		
		// make sure KIDE was opened
		web.assertTrue(driver.getTitle().startsWith("Raimme IDE"));
		
		String secondViewId = getOpenIdeFileId();
		testData.addView(KID.get(secondViewId), viewName, secondViewPackage);
		
		// delete the first view
		deleteViewThroughList(KID.get(firstViewId), viewName, firstViewPackage, true);
		
		// add a display for some KOLL variable using notation {}
	}
	
	@Test
	public void modifyStandardViewTest() throws KommetException
	{
		driver = LoginTest.logIn(driver);
		
		Type companyType = TestDataCreator.getCompanyType();
		createType(companyType);
		testData.addType(companyType);
		assertNotNull("Type prefix should be set by method createType(), but it is empty", companyType.getKeyPrefix());
		
		// go to editing standard views
		openTypeDetails(companyType.getKeyPrefix());
		openTab("Standard actions");
		WebElement pageCell = getTableCell("stdPageList", 0, 1);
		
		if (pageCell == null)
		{
			fail("Table cell #1 in row #0 not found");
		}
		
		assertTrue("Standard page list for a newly created type should contain phrase 'default' and a link to edit the page in the default page assignment list, instead found " + pageCell.getText(), pageCell.getText() != null && pageCell.getText().startsWith("default"));
		
		// click the link to edit standard list page
		pageCell.findElement(By.xpath("a[contains(.,'edit')]")).click();
		web.assertTitleStartsWith("Edit standard page");
		
		String pageName = "NewListPage" + String.valueOf((new Random()).nextInt(10000));
		String url = "newlisturl/" + String.valueOf((new Random()).nextInt(10000));
		String viewName = "NewListView" + String.valueOf((new Random()).nextInt(10000));
		
		// edit new page for this action
		driver.findElement(By.xpath("//input[@name='usedPage' and @value='new']")).click();
		setFieldValueByName("pageName", pageName);
		setFieldValueByName("url", url);
		String selectedControllerOption = getSelectedOption("controllerOption");
		assertTrue("When modifying a default view, the default controller option should be 'default', instead it's '" + selectedControllerOption + "'" , "default".equals(selectedControllerOption));
		String selectedViewOption = getSelectedOption("viewOption");
		assertTrue("When modifying a default view, the default view option should be 'new', instead it's '" + selectedViewOption + "'" , "new".equals(selectedViewOption));
		setFieldValueByName("newViewName", viewName);
		driver.findElement(By.id("savePageBtn")).click();
		
		// make sure we are taken to page details
		web.assertTitleStartsWith(pageName);
		
		// find link pointing to view details
		driver.findElement(By.linkText(viewName)).click();
		
		// make sure we are taken to view details
		web.assertTitleStartsWith(viewName);
		
		// extract view Id from url
		String viewId = driver.getCurrentUrl().substring(driver.getCurrentUrl().lastIndexOf("/") + 1);
		assertTrue(viewId.startsWith(KID.VIEW_PREFIX));
		
		// open view in IDE
		driver.findElement(By.id("openIdeBtn")).click();

		// make sure we have the right view open
		String openFileId = getOpenIdeFileId();
		assertEquals(viewId, openFileId);
		
		// get view contents
		String viewCode = getOpenIdeTabContent();
		
		String expectedViewDeclaration = "<km:view name=\"" + viewName + "\"";
		if (StringUtils.hasText(companyType.getPackage()))
		{
			expectedViewDeclaration += " package=\"" + companyType.getPackage() + "\"";
		}
		expectedViewDeclaration += ">";
		
		web.assertTrue("Incorrect start of view code " + viewCode.substring(0, Math.min(100, viewCode.length() - 1)), viewCode.startsWith(expectedViewDeclaration));
		
		// add some words to the view content
		String randomString = MiscUtils.getHash(50);
		setOpenIdeTabContent(viewCode.replaceFirst("<km:messages />", "<km:messages /><h3>" + randomString + "</h3>"), true);
		
		// save view by clicking CTRL + S
		new Actions(driver).sendKeys(Keys.chord(Keys.CONTROL, "s")).perform();
		
		// open record list for the new type and make sure the random string is there
		openRecordList(companyType.getKeyPrefix());
		
		driver.navigate().refresh();
		
		try
		{
			WebElement randomStringElem = driver.findElement(By.xpath("//h3[.='" + randomString + "']"));
			web.assertTrue(randomString.equals(randomStringElem.getText()));
		}
		catch (NoSuchElementException e)
		{
			web.fail("Element h3 with text '" + randomString + "' not found on record list at URL " + driver.getCurrentUrl());
		}
	}
}
