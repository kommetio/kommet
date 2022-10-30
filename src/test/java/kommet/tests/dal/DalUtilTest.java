/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.dal;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import kommet.dao.dal.DALCriteriaBuilder;
import kommet.data.KommetException;
import kommet.tests.TestDataCreator;

public class DalUtilTest
{
	@Test
	public void formatDAL()
	{
		assertEquals("SELECT Id, name, Age  FROM entity", DALCriteriaBuilder.format("SELECT Id		, name  , Age  FROM entity"));
	}
	
	
	@Test
	public void testDALUtilTest() throws KommetException
	{
		List<String> tokens = DALCriteriaBuilder.tokenize("select 'maker \\'ble'		tee  'me	'");
		assertEquals("select", tokens.get(0));
		assertEquals("'maker \\'ble'", tokens.get(1));
		assertEquals("tee", tokens.get(2));
		assertEquals("'me	'", tokens.get(3));
		
		String sql = "age > 2 and	(name  = 'ka\\'m' or name = 'ad')";
		tokens = DALCriteriaBuilder.tokenize(sql);
		assertEquals(13, tokens.size());
		assertEquals("age", tokens.get(0));
		assertEquals(">", tokens.get(1));
		assertEquals("2", tokens.get(2));
		assertEquals("and", tokens.get(3));
		assertEquals("(", tokens.get(4));
		assertEquals("name", tokens.get(5));
		assertEquals("=", tokens.get(6));
		assertEquals("'ka\\'m'", tokens.get(7));
		assertEquals(")", tokens.get(12));
	}
	
	@Test
	public void testTokenize() throws KommetException
	{
		List<String> tokens = DALCriteriaBuilder.tokenize("select id, name, age from " + TestDataCreator.PIGEON_TYPE_API_NAME);
		assertEquals(8, tokens.size());
		assertEquals("id", tokens.get(1));
		assertEquals("Commas should be treated as separate tokens by the tokenize() method", ",", tokens.get(2));
	}
}
