/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.Action;
import kommet.basic.BasicSetupService;
import kommet.basic.types.SystemTypes;
import kommet.dao.ColumnMapping;
import kommet.dao.RecordProxyMapping;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class PersistenceTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Test
	public void testReadObjectMapping() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		env.addCustomTypeProxyMapping(Action.class);
		
		RecordProxyMapping pageMapping = env.getCustomTypeProxyMapping(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME)).getKID());
		assertNotNull("Object mapping for object Page not found", pageMapping);
		assertEquals(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME), pageMapping.getTypeQualifiedName());
		assertNotNull(pageMapping.getPropertyMappings());
		assertTrue(!pageMapping.getPropertyMappings().isEmpty());
		
		ColumnMapping idColMapping = pageMapping.getPropertyMappings().get(Field.ID_FIELD_NAME);
		assertNotNull("Mapping for ID column not found", idColMapping);
		assertTrue(idColMapping.getRequired());
	}
}
