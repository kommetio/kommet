/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.BasicSetupService;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.tests.tags.Pigeon;
import kommet.utils.AppConfig;
import kommet.utils.VarInterpreter;

public class VarInterpreterTest extends BaseUnitTest
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
	public void testInterpreteRecordVars() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		Record pigeon = new Record(pigeonType);
		pigeon.setField("name", "Tippi");
		pigeon.setField("age", 3);
		pigeon = dataService.save(pigeon, env);
		
		String formula = "ID = ${record.id}, name = ${record.name}";
		assertEquals("ID = " + pigeon.getKID() + ", name = Tippi", VarInterpreter.interprete(pigeon, formula, "record", Locale.EN_US));
	}
	
	@Test
	public void testInterpreteBeanVars() throws KommetException
	{
		String formula = "Age = ${record.age}, name = ${record.name}";
		
		Pigeon pigeon = new Pigeon();
		pigeon.setAge(4);
		pigeon.setName("Warren");
		
		assertEquals("Age = " + pigeon.getAge() + ", name = " + pigeon.getName(), VarInterpreter.interprete(pigeon, formula, "record", Locale.EN_US));
		
		Collection<String> usedProperties = VarInterpreter.extractProperties(formula, "record");
		assertNotNull(usedProperties);
		assertEquals(2, usedProperties.size());
		
		for (String prop : usedProperties)
		{
			assertTrue(prop.equals("age") || prop.equals("name"));
		}
	}
}
