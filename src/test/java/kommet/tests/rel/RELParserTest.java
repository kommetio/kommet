/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.rel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.BasicSetupService;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.rel.RELFunctions;
import kommet.rel.RELParser;
import kommet.rel.RELSyntaxException;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class RELParserTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Test
	public void testRELParser() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		
		String utilCls = RELFunctions.class.getName();
		assertEquals(utilCls + ".gt(record.getName().getLabel(), 0)", RELParser.relToJava("name.label > 0", "record", null, false, true, env));
		assertEquals(utilCls + ".gt(record.getName().getLabel(), 0.1)", RELParser.relToJava("name.label > 0.1", "record", null, false, true, env));
		assertEquals(utilCls + ".gt(record.getName().getLabel(), 0.1) || " + utilCls + ".eq(record.getAge(), 11)", RELParser.relToJava("name.label > 0.1 or age == 11", "record", null, false, true, env));
		assertEquals(utilCls + ".gt(record.getName().getLabel(), 0.1) || " + utilCls + ".eq(record.getAge(), 11)", RELParser.relToJava("name.label > 0.1 or age == 11", "record", null, false, true, env));
		assertEquals(utilCls + ".gt(rec.getName().getLabel(), 123)", RELParser.relToJava("name.label>123", "rec", null, false, true, env));
		assertEquals(utilCls + ".gt(rec.getAge123(), 123)", RELParser.relToJava("age123>123", "rec", null, false, true, env));
		assertEquals(utilCls + ".ge(rec.getAge123(), 123.43)", RELParser.relToJava("age123>=123.43", "rec", null, false, true, env));
		assertEquals("!" + utilCls + ".eq(rec.getAge123(), \"sas\")", RELParser.relToJava("age123<>'sas'", "rec", null, false, true, env));
		
		assertEquals("!" + utilCls + ".eq(rec.getAged(), (rec.getName() + rec.getPlace()))", RELParser.relToJava("aged<> (name +place)", "rec", null, false, true, env));
		assertEquals("!" + utilCls + ".eq(rec.getAged(), " + utilCls + ".getLength(rec.getName() + rec.getPlace()))", RELParser.relToJava("aged <>length(name +place)", "rec", null, false, true, env));
		
		String rel = "aged <>length(name +place";
		
		try
		{
			RELParser.relToJava(rel, "rec", null, false, true, env);
			fail("Parsing REL with unclosed brackets should fail");
		}
		catch (RELSyntaxException e)
		{
			assertEquals("REL expression contains unterminated brackets: " + rel, e.getMessage());
		}
		
		basicSetupService.runBasicSetup(env);
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		assertEquals(utilCls + ".eq(rec.getAge(), 11)", RELParser.relToJava("age == 11", "rec", pigeonType, true, true, env));
		
		try
		{
			RELParser.relToJava("label == 11", "rec", pigeonType, true, true, env);
			fail("Parsing REL with non-existing field should fail");
		}
		catch (RELSyntaxException e)
		{
			assertTrue(e.getMessage().startsWith("Field label does not exist"));
		}
		
		testFieldsUsedInREL(env);
	}

	private void testFieldsUsedInREL(EnvData env) throws KommetException
	{
		String code = "project.name <> 'aa' and age < maxAge and isNull(this)";
		Set<String> fields = RELParser.getRecordFields(code, env);
		assertEquals(4, fields.size());
		assertTrue(fields.contains("project.name"));
		assertTrue(fields.contains("age"));
		assertTrue(fields.contains("maxAge"));
		assertTrue(fields.contains("this"));
	}
}
