/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import javax.inject.Inject;
import org.junit.Test;

import kommet.data.KommetException;


public class TmpEnvSetupTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Test
	public void mockTest() throws KommetException
	{
		dataHelper.configureFullTestEnv();
	}
}
