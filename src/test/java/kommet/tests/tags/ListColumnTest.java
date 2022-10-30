/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.keetle.tags.TagException;
import kommet.basic.keetle.tags.objectlist.ListColumn;
import kommet.basic.keetle.tags.objectlist.ListColumnTag;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class ListColumnTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Test
	public void testListColumnForRecord() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		Record pigeon = new Record(pigeonType);
		pigeon.setField("name", "Tippi");
		pigeon.setField("age", 3);
		pigeon = dataService.save(pigeon, env);
		
		ListColumn col = new ListColumn();
		col.setField("age");
		col.setLink(true);
		col.setSortable(true);
		
		String colCode = col.getCode(pigeon, null, null, dataHelper.getRootAuthData(env), "/abc/efg");
		assertEquals("<a href=\"/abc/efg/" + pigeon.getKID() + "\">3</a>", colCode);
		
		col.setLink(false);
		colCode = col.getCode(pigeon, null, null, dataHelper.getRootAuthData(env), "/abc/efg");
		assertEquals("3", colCode);
		
		col.setFormula("name = ${rec.name}...");
		
		try
		{
			col.getCode(pigeon, null, null, dataHelper.getRootAuthData(env), "/abc/efg");
			fail("If field and formula are both set on a list column, an exception should be thrown");
		}
		catch (TagException e)
		{
			assertEquals(ListColumnTag.class.getName(), e.getTagClass());
			assertEquals("Both field and formula attributes cannot be set for a list column", e.getMessage());
		}
		
		col.setField(null);
		
		try
		{
			colCode = col.getCode(pigeon, null, null, dataHelper.getRootAuthData(env), "/abc/efg");
			fail("Generating list column code should fail when item variable is not set");
		}
		catch (KommetException e)
		{
			assertEquals("Record variable name is empty", e.getMessage());
		}
		
		colCode = col.getCode(pigeon, "rec", null, dataHelper.getRootAuthData(env), "/abc/efg");
		assertEquals("name = Tippi...", colCode);
	}
	
	@Test
	public void testListColumnForBean() throws KommetException
	{	
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Pigeon pigeon = new Pigeon();
		pigeon.setAge(4);
		pigeon.setName("Warren");
		
		ListColumn col = new ListColumn();
		col.setField("age");
		col.setLink(true);
		col.setSortable(true);
		col.setIdField("name");
		col.setNameField("age");
		
		String colCode = col.getCode(pigeon, null, null, null, "/abc/efg");
		assertEquals("<a href=\"/abc/efg/" + pigeon.getName() + "\">" + pigeon.getAge() + "</a>", colCode);
		
		col.setLink(false);
		colCode = col.getCode(pigeon, null, null, null, "/abc/efg");
		assertEquals(String.valueOf(pigeon.getAge()), colCode);
		
		col.setFormula("name = ${rec.name}...");
		
		try
		{
			col.getCode(pigeon, null, null, null, "/abc/efg");
			fail("If field and formula are both set on a list column, an exception should be thrown");
		}
		catch (TagException e)
		{
			assertEquals(ListColumnTag.class.getName(), e.getTagClass());
			assertEquals("Both field and formula attributes cannot be set for a list column", e.getMessage());
		}
		
		col.setField(null);
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		try
		{
			colCode = col.getCode(pigeon, null, null, authData, "/abc/efg");
			fail("Generating list column code should fail when item variable is not set");
		}
		catch (KommetException e)
		{
			assertEquals("Record variable name is empty", e.getMessage());
		}
		
		colCode = col.getCode(pigeon, "rec", null, authData, "/abc/efg");
		assertEquals("name = " + pigeon.getName() + "...", colCode);
	}
}
