/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.springframework.util.StringUtils;

import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.tests.TestDataCreator;

public class TypeManipulationTest extends BaseWebTest
{
	@Test
	public void testCreateAndDeleteType() throws KommetException
	{
		driver = LoginTest.logIn(driver);
		
		Type companyType = TestDataCreator.getCompanyType();
		
		openSetup();
		driver.findElement(By.linkText("Data")).click();
		driver.findElement(By.linkText("Types")).click();
		assertTrue(driver.getTitle().startsWith("Types"));
		driver.findElement(By.id("newTypeBtn")).click();
		assertTrue(driver.getTitle().startsWith("New type"));
		driver.findElement(By.name("apiName")).sendKeys(companyType.getApiName());
		driver.findElement(By.name("label")).sendKeys(companyType.getLabel());
		driver.findElement(By.name("pluralLabel")).sendKeys(companyType.getPluralLabel());
		clickLink(By.id("saveTypeBtn"));
		assertTrue(driver.getTitle().startsWith(companyType.getLabel()));
		
		testStandardPages(companyType);
		
		// open types list
		driver.findElement(By.linkText("Data")).click();
		driver.findElement(By.linkText("Types")).click();
		// go to type details
		driver.findElement(By.linkText(companyType.getLabel())).click();
		assertTrue(driver.getTitle().startsWith(companyType.getLabel()));
		
		// now delete the type
		driver.findElement(By.id("deleteTypeBtn")).click();
		clickYesOnPrompt();
		
		// open types list
		clickLink(By.linkText("Data"));
		clickLink(By.linkText("Types"));
		
		// type does not exist so there should be no link to it
		try
		{
			web.assertPageHeader("Types");
			// refresh the page because it sometimes used buffered content with the deleted type still being displayed
			driver.navigate().refresh();
			driver.findElement(By.linkText(companyType.getLabel())).click();
			fail("Link to deleted type should not be on the list of types");
		}
		catch (NoSuchElementException e)
		{
			// expected
		}
	}
	
	@Test
	public void testCreateTypeWithFields() throws KommetException
	{
		driver = LoginTest.logIn(driver);
		Type companyType = TestDataCreator.getCompanyType();
		testData.addType(companyType);
		createType(companyType);
		
		// add ID field to type - we did not add it earlier because it's not supposed to be created in the UI
		Field idField = new Field();
		idField.setApiName("id");
		idField.setLabel("Id");
		idField.setRequired(true);
		companyType.addField(idField);
		
		openTypeDetails(companyType.getKeyPrefix());
		testUIFieldValue("Default field", "Id");
		
		// change default field to "name"
		driver.findElement(By.id("editTypeBtn")).click();
		selectDropdownOptionByText(driver.findElement(By.id("defaultField")), "Name");
		assertEquals(companyType.getApiName(), driver.findElement(By.name("apiName")).getAttribute("value"));
		assertEquals(companyType.getLabel(), driver.findElement(By.name("label")).getAttribute("value"));
		assertEquals(companyType.getPluralLabel(), driver.findElement(By.name("pluralLabel")).getAttribute("value"));
		
		if (StringUtils.hasText(companyType.getPackage()))
		{
			assertEquals(companyType.getPackage(), driver.findElement(By.name("packageName")).getAttribute("value"));
		}
		else
		{
			assertEquals("", driver.findElement(By.name("apiName")).getText());
		}
		
		driver.findElement(By.id("saveTypeBtn")).click();
		assertTrue(driver.getTitle().startsWith(companyType.getLabel()));
		// make sure the default field has changed
		testUIFieldValue("Default field", "Name");
		
		// open record list and make sure this field is the only one displayed
		openRecordList(companyType.getKeyPrefix());
		try
		{
			driver.findElement(By.xpath("//div[contains(@class,'object-list-container')]//table/thead/tr/th/a[.='Name']"));
		}
		catch (NoSuchElementException e)
		{
			web.fail("Default column not found on record list");
		}
		
		// make sure there are no records on the list
		web.assertListContainsText("No results to display");
		// add new record
		driver.findElement(By.linkText("New")).click();
		driver.findElement(By.id("name")).sendKeys("Kamila");
		
		// make sure all the required fields are on the edit page, but none of the non-required ones is there
		for (Field field : companyType.getFields())
		{
			if (field.isRequired() && !Field.isSystemField(field.getApiName()))
			{
				web.assertExists(By.id(field.getApiName()));
			}
			else
			{
				web.assertNotExists(By.id(field.getApiName()));
			}
		}
		
		driver.findElement(By.id("saveBtn")).click();
		openRecordList(companyType.getKeyPrefix());
		web.assertListContainsText("Kamila");
		
		testUnsuccessfulDeleteDefaultField(companyType, "name");
		setDefaultField(companyType, "id");
		deleteField(companyType, "name");
		
		deleteType(companyType, true);
	}

	/**
	 * Makes sure that default field for type cannot be deleted and error message is displayed.
	 * @param type
	 * @param defaultFieldApiName
	 * @throws KommetException
	 */
	private void testUnsuccessfulDeleteDefaultField(Type type, String defaultFieldApiName) throws KommetException
	{
		openTypeDetails(type.getKeyPrefix());
		openTab("Fields");
		Field defaultField = type.getField(defaultFieldApiName);
		// go into field details
		driver.findElement(By.xpath("//table[@id='fieldList']/tbody/tr/td/a[.='" + defaultField.getLabel() + "']")).click();
		web.assertTitleStartsWith(defaultField.getLabel());
		// click delete on field details
		deleteAndConfirm();
		web.assertErrorDisplayed("Field cannot be deleted because it is the default field for its type");
	}

	private void testStandardPages(Type type)
	{
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/views/list");
		// make sure all four standard views for this type are on the list of views
		assertTrue(driver.findElement(By.linkText("StandardList" + type.getLabel())).isDisplayed());
		assertTrue(driver.findElement(By.linkText("StandardEdit" + type.getLabel())).isDisplayed());
		assertTrue(driver.findElement(By.linkText("StandardCreate" + type.getLabel())).isDisplayed());
		assertTrue(driver.findElement(By.linkText("StandardView" + type.getLabel())).isDisplayed());
	}
}
