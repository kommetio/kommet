/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import kommet.tests.TestDataCreator;
import kommet.utils.UrlUtil;

public class LoginTest extends BaseWebTest
{   
    @Test
    public void testLogin() throws Exception
    {
    	driver = logIn(driver);
        assertTrue(driver.getCurrentUrl().endsWith(UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/me"));
        assertTrue(driver.getTitle().startsWith("Raimme"));
    }
    
    public static WebDriver logIn(WebDriver driver)
    {
    	driver.get(TestDataCreator.WEB_TEST_BASE_URL + "rm/login?env=0010000000005");
    	driver.findElement(By.name("username")).clear();
    	driver.findElement(By.name("username")).sendKeys(TestDataCreator.WEB_TEST_ADMIN_USER);
    	driver.findElement(By.name("password")).clear();
    	driver.findElement(By.name("password")).sendKeys(TestDataCreator.WEB_TEST_ADMIN_PASSWORD);
    	driver.findElement(By.id("loginBtn")).submit();
    	return driver;
    }
}
