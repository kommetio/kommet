/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@Rollback(true)
public class BaseUnitTest
{
	private static final Logger log = LoggerFactory.getLogger(BaseUnitTest.class);
	
	public BaseUnitTest()
	{
		log.info("Starting test " + this.getClass().getName());
	}
	
	@Ignore
	public void emptyStubTest()
	{
		// empty
	}
}
