/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.web;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import kommet.basic.BasicSetupService;
import kommet.tests.TestDataCreator;
import kommet.utils.MiscUtils;

public class TabsAndCommentsTest extends BaseWebTest
{
	@Test
	public void testCommentsAndTabsOnUserView()
	{
		driver = LoginTest.logIn(driver);
		
		driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/user/0040000000001");
		assertTrue(driver.getTitle().startsWith(BasicSetupService.ROOT_USERNAME));
		
		WebElement commentsTitle = driver.findElement(By.cssSelector("div.km-cmt-title"));
		assertNotNull(commentsTitle);
		assertFalse(commentsTitle.isDisplayed());
		
		WebElement commentsTabLink = driver.findElement(By.cssSelector("li.km-tabs-head-1"));
		assertEquals("Comments", commentsTabLink.getText());
		commentsTabLink.click();
		
		commentsTitle = driver.findElement(By.cssSelector("div.km-cmt-title"));
		assertNotNull(commentsTitle);
		assertTrue(commentsTitle.isDisplayed());
		
		String commentText = MiscUtils.getHash(100);
		
		// add some comment
		driver.findElement(By.cssSelector(".km-cmt-new > #newcomment")).sendKeys(commentText);
		driver.findElement(By.cssSelector(".km-cmt-new > .sbtn")).click();
		
		// check if the new comment has appeared
		WebDriverWait wait = new WebDriverWait(driver, 15);
		wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//*[contains(.,'" + commentText + "')]")));
	}
}
