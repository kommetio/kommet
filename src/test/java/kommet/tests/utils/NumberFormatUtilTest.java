/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import org.junit.Test;

import kommet.data.KommetException;
import kommet.i18n.Locale;
import kommet.tests.BaseUnitTest;
import kommet.utils.NumberFormatUtil;

public class NumberFormatUtilTest extends BaseUnitTest
{
	@Test
	public void testNumberConversions() throws KommetException
	{
		assertEquals("11.232", NumberFormatUtil.parseLocaleSpecificNumber("11,232", Locale.PL_PL));
		assertEquals("11.232", NumberFormatUtil.parseLocaleSpecificNumber("11.232", Locale.EN_US));
		assertEquals("11022.232", NumberFormatUtil.parseLocaleSpecificNumber("11,022.232", Locale.EN_US));
		assertNull(NumberFormatUtil.parseLocaleSpecificNumber(null, Locale.EN_US));
		
		try
		{
			NumberFormatUtil.parseLocaleSpecificNumber("", Locale.EN_US);
			fail("Parsing empty string as number should fail");
		}
		catch (KommetException e)
		{
			// expected
			assertEquals("Invalid number string: empty string", e.getMessage());
		}
		
		// test formatting numbers
		assertEquals("32.11", NumberFormatUtil.format(new BigDecimal("32.11"), 2, Locale.EN_US));
		assertEquals("32,11", NumberFormatUtil.format(new BigDecimal("32.11"), 2, Locale.PL_PL));
		assertEquals("32,1", NumberFormatUtil.format(new BigDecimal("32.11"), 1, Locale.PL_PL));
		assertEquals("32", NumberFormatUtil.format(new BigDecimal("32.11"), 0, Locale.PL_PL));
		assertEquals(null, NumberFormatUtil.format(null, 0, Locale.PL_PL));
		assertEquals("32.10", NumberFormatUtil.format(new BigDecimal("32.1"), 2, Locale.EN_US));
		assertEquals("32,10", NumberFormatUtil.format(new BigDecimal("32.1"), 2, Locale.PL_PL));
		
		Pattern numberFormat = Pattern.compile(NumberFormatUtil.getLocaleSpecificNumberFormat(Locale.PL_PL));
		assertTrue(numberFormat.matcher("123,11").find());
		assertTrue(numberFormat.matcher("123").find());
		assertTrue(numberFormat.matcher("0").find());
		assertTrue(numberFormat.matcher("3").find());
		assertFalse(numberFormat.matcher("012").find());
		assertFalse(numberFormat.matcher("123,").find());
	}
}
