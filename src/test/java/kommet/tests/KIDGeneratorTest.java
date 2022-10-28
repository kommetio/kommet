/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import kommet.data.KommetException;
import kommet.env.EnvData;

public class KIDGeneratorTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Test
	public void testDatabaseIdGeneration() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		JdbcTemplate jdbcTemplate = env.getJdbcTemplate();
		
		assertEquals("abc0000000000", jdbcTemplate.queryForObject("select next_kolmu_id('abc', 0)", String.class));
		assertEquals("abc0000000001", jdbcTemplate.queryForObject("select next_kolmu_id('abc', 1)", String.class));
		assertEquals("abc000000000b", jdbcTemplate.queryForObject("select next_kolmu_id('abc', 11)", String.class));
		assertEquals("abc0000000011", jdbcTemplate.queryForObject("select next_kolmu_id('abc', 27)", String.class));
		assertEquals("abc0000000012", jdbcTemplate.queryForObject("select next_kolmu_id('abc', 28)", String.class));
		assertEquals("abc0000000010", jdbcTemplate.queryForObject("select next_kolmu_id('abc', 26)", String.class));
		assertEquals("abc0000001002", jdbcTemplate.queryForObject("select next_kolmu_id('abc', 17578)", String.class));
	}
}
