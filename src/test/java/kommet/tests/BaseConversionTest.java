/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import kommet.utils.BaseConverter;

public class BaseConversionTest
{
	@Test
	public void testBase26Conversion()
	{
		assertEquals("0", BaseConverter.convertToKommetBase(0));
		assertEquals("1", BaseConverter.convertToKommetBase(1));
		assertEquals("b", BaseConverter.convertToKommetBase(11));
		assertEquals("11", BaseConverter.convertToKommetBase(27));
		assertEquals("10", BaseConverter.convertToKommetBase(26));
		assertEquals("1002", BaseConverter.convertToKommetBase(17578));
	}
}
