/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;

import kommet.tests.koll.GenericActionTest;
import kommet.tests.webmock.JCRTest;
import kommet.tests.webmock.LoginTest;
import kommet.tests.webmock.OAuthTest;
import kommet.tests.webmock.RestApiTest;
import kommet.tests.webmock.StandardActionTest;
import kommet.tests.webmock.UserWebMockTest;
import kommet.tests.webmock.ValidationRuleWebMockTest;

@RunWith(Suite.class)
@ContextConfiguration("/test-app-context.xml")
@Rollback
@SuiteClasses({ LoginTest.class,
	ValidationRuleWebMockTest.class,
	UserWebMockTest.class,
	OAuthTest.class,
	RestApiTest.class,
	JCRTest.class,
	GenericActionTest.class,
	StandardActionTest.class
})
public class WebMockSuite
{

}
