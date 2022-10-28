/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class TestUtil
{
	/**
	 * Tests if two queries are equal. The order of fields in the SELECT clause is irrelevant
	 * to the result of the comparison.
	 * @param q1
	 * @param q2
	 */
	public static void assertQueriesEquals (String q1, String q2)
	{
		if ((q1 == null && q2 != null) || (q1 != null && q2 == null))
		{
			fail("Queries not equal, one of them is null and the other is not. Query 1:" + q1 + ", query 2: " + q2);
		}
		
		// get property names from the first query
		List<String> q1Props = MiscUtils.splitAndTrim(q1.substring("SELECT ".length(), q1.indexOf("FROM")).trim(), ",");
		List<String> q2Props = MiscUtils.splitAndTrim(q2.substring("SELECT ".length(), q2.indexOf("FROM")).trim(), ",");
		
		assertEquals(q1.toLowerCase().replaceAll("select[a-z\\s,\\.]+from", "select * from"), q2.toLowerCase().replaceAll("select[a-z\\s,\\.]+from", "select * from"));
		assertTrue("Queries have different properties. Query 1:" + q1 + ", query 2: " + q2, CollectionUtils.intersection(q1Props, q2Props).size() == q1Props.size());
	}
}