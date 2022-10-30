/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.web;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.thoughtworks.selenium.SeleneseTestBase;

import kommet.data.KommetException;

public class WebAssertions
{
	WebDriver driver;
	private static final String ERROR_SCREENSHOT_DIR = "f:/dev/kolmu-workspace/kolmu/target/screenshots/";
	
	public WebAssertions()
	{
		// empty constructor required so that unit test engine does not produce a warning
	}
	
	public WebAssertions (WebDriver driver)
	{
		this.driver = driver;
	}
	
	@Test
	public void stubTest()
	{
		
	}
	
	public Integer getRandomSuffix()
	{
		return (new Random()).nextInt(10000);
	}
	
	public void assertErrorDisplayed(String errorMsg) throws KommetException
	{
		assertErrorDisplayed(errorMsg, true);
	}
	
	public void assertTrue(String msg, boolean cond) throws KommetException
	{
		if (!cond)
		{
			fail(msg);
		}
	}
	
	public void assertTrue(boolean cond) throws KommetException
	{
		if (!cond)
		{
			fail("Assertion failed");
		}
	}

	public void fail(String msg) throws KommetException
	{
		// generate screenshot name
		String fileName = "ui-error-" + (new Random()).nextInt(10000);
		// take screenshot
		File scrShot = takeScreenshot(fileName);
		SeleneseTestBase.fail(msg + " [screenshot: " + scrShot.getName() + "]");
	}

	public void assertErrorDisplayed(String errorMsg, boolean exactMatch) throws KommetException
	{
		try
		{
			String matchExpr = exactMatch ? ".='" + errorMsg + "'" : "contains(., '" + errorMsg + "')";
			driver.findElement(By.xpath("//table[contains(@class,'action-errors')]/tbody/tr/td/ul/li[" + matchExpr + "]"));
		}
		catch (NoSuchElementException e)
		{
			fail("Error message '" + errorMsg + "' not displayed although expected");
		}
	}
	
	public void assertTitleStartsWith(String title) throws KommetException
	{
		assertTitleStartsWith(null, title);
	}
	
	public void assertTitleStartsWith(String msg, String title) throws KommetException
	{
		SeleneseTestBase.assertTrue((msg != null ? msg : "Page title expected to start with '" + title + "', but found '" + driver.getTitle() + "' on URL " + driver.getCurrentUrl()) + ". Screenshot " + takeScreenshot("ui-error-" + (new Random()).nextInt(10000)).getName(), driver.getTitle().replaceAll("\\s+", " ").startsWith(title));
	}
	
	public void assertPageHeader(String text)
	{
		driver.findElement(By.className("page-header")).getText().startsWith(text);
	}
	
	public File takeScreenshot(String fileName) throws KommetException
	{
		File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		// Now you can do whatever you need to do with it, for example copy somewhere
		try
		{
			File newFile = new File(ERROR_SCREENSHOT_DIR + fileName + ".png");
			FileUtils.copyFile(scrFile, newFile);
			return newFile;
		}
		catch (IOException e)
		{
			throw new KommetException("Cannot save screenshot to disk: " + e.getMessage());
		}
	}
	
	public void assertNotExists(By selector) throws KommetException
	{
		try
		{
			driver.findElement(selector);
			SeleneseTestBase.fail("Element with selector " + selector.toString() + " does exists although it should not");
		}
		catch (NoSuchElementException e)
		{
			// expected
		}
	}
	
	public void assertExists(By selector) throws KommetException
	{
		try
		{
			driver.findElement(selector);
		}
		catch (NoSuchElementException e)
		{
			SeleneseTestBase.fail("Element with selector " + selector.toString() + " does not exist: " + e.getMessage());
		}
	}
	
	/**
	 * Checks that a standard object list on the current page contains a TD element with inner text exactly
	 * matching that passed in the argument.
	 * @param text
	 * @throws KommetException 
	 */
	public void assertListContainsText(String text) throws KommetException
	{
		try
		{
			driver.findElement(By.xpath("//div[contains(@class,'object-list-container')]//table/tbody/tr/td[.='" + text + "']"));
		}
		catch (NoSuchElementException e)
		{
			// check if a list is at all there
			try
			{
				driver.findElement(By.xpath("//div[contains(@class,'object-list-container')]//table"));
			}
			catch (java.util.NoSuchElementException e1)
			{
				throw new KommetException("Text not found on list, but the list itself has not been found either");
			}
			
			fail("Text '" + text + "' not found on list");
		}
	}
}
