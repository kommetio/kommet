/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;

import kommet.i18n.InternationalizationService;
import kommet.i18n.Locale;

public class I18nTest extends BaseUnitTest
{
	@Inject
	InternationalizationService i18n;
	
	@Test
	public void testI18n()
	{
		assertNotNull(i18n.get(Locale.EN_US, "btn.new"));
	}
}
