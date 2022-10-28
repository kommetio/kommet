/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.actions;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import kommet.data.KommetException;
import kommet.web.actions.ParsedURL;

public class ParsedURLTest
{
	@Test
	public void testParsedURL() throws KommetException
	{
		assertTrue(new ParsedURL("john").matches("john"));
		assertTrue(new ParsedURL("john/mike/less").matches("john/mike/less"));
		assertTrue(new ParsedURL("john/mi323ke/less").matches("john/mi323ke/less"));
		assertFalse(new ParsedURL("john").matches("joh"));
		assertFalse(new ParsedURL("john").matches("john1"));
		
		assertTrue(new ParsedURL("john/").matches("john"));
		assertFalse(new ParsedURL("john/").matches("joh/"));
		assertFalse(new ParsedURL("john/").matches("john1/"));
		
		assertTrue(new ParsedURL("john/{lastName}").matches("john/doe"));
		assertTrue(new ParsedURL("john/{lastName}").matches("john/doe/"));
		assertFalse(new ParsedURL("john/{lastName}").matches("joh/doe"));
		assertFalse(new ParsedURL("john/{lastName}").matches("john/doe/mark"));
		
		assertTrue(new ParsedURL("john/{lastName}/suffix").matches("john/doe/suffix"));
		assertTrue(new ParsedURL("john/{lastName}/suffix/").matches("john/doe/suffix"));
		assertTrue(new ParsedURL("john/{lastName}/suffix").matches("john/doe/suffix/"));
		assertTrue(new ParsedURL("john/{lastName}/suffix/{param}").matches("john/doe/suffix/1122"));
		assertTrue(new ParsedURL("john/{lastName}/suffix/{param}").matches("john/doe/suffix/1122/"));	
		assertTrue(new ParsedURL("john/{lastName}/suffix/{param}").matches("john/doe/suffix/1%2022/"));	
		
		assertTrue(new ParsedURL("john/{lastName}/suffix").matchesParameterized("john/{param}/suffix"));
		assertTrue(new ParsedURL("john/lee/suffix").matchesParameterized("john/{param}/suffix"));
		assertTrue(new ParsedURL("john/{lastName}/suffix").matchesParameterized("john/lee/suffix"));
		assertFalse(new ParsedURL("john/{lastName}/suffix").matchesParameterized("john/{param}/suffix/123"));
		assertFalse(new ParsedURL("john/{lastName}/suffix").matchesParameterized("john/{param}/suffix/{anything}"));
		assertFalse(new ParsedURL("john/{lastName}/suffix/{param2}").matchesParameterized("john/{param}/suffix"));
		
		Map<String, String> paramValues = new ParsedURL("john/{lastName}/suffix/{param2}").getParamValues("john/krawiec/suffix/kamila", true);
		assertEquals(2, paramValues.size());
		assertEquals("krawiec", paramValues.get("lastName"));
		assertEquals("kamila", paramValues.get("param2"));
		
		paramValues = new ParsedURL("john/{lastName}/suffix/{param2}/").getParamValues("john/krawiec/suffix/kamila/", true);
		assertEquals(2, paramValues.size());
		assertEquals("krawiec", paramValues.get("lastName"));
		assertEquals("kamila", paramValues.get("param2"));
	}
}
