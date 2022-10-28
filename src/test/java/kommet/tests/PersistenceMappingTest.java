/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.BasicSetupService;
import kommet.dao.ColumnMapping;
import kommet.dao.TypePersistenceConfig;
import kommet.dao.TypePersistenceMapping;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.utils.AppConfig;

public class PersistenceMappingTest extends BaseUnitTest
{
	@Inject
	TypePersistenceConfig persistenceMapping;
	
	@Inject
	DataService typeService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Test
	public void testPersistenceMapping() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		Type obj = dataHelper.getFullPigeonType(env);
		obj = typeService.createType(obj, env);
		
		TypePersistenceMapping objMapping = TypePersistenceMapping.get(obj, env);
		assertNotNull("Object mapping not initialized", objMapping);
		
		assertEquals(obj.getDbTable(), objMapping.getTable());
		ColumnMapping col1 = objMapping.getPropertyMappings().get("name");
		assertNotNull(col1);
		assertEquals("name", col1.getColumn());
		assertTrue(col1.getRequired());
		assertNotNull(objMapping.getPropertyMappings().get("age"));
	}
}
