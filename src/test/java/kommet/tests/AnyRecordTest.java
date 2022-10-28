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

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.AnyRecord;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.services.AnyRecordService;

public class AnyRecordTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Inject
	AnyRecordService anyRecordService;
	
	@SuppressWarnings("deprecation")
	@Test
	public void testAnyRecord() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		List<AnyRecord> anyRecords = anyRecordService.get(null, authData, env);
		assertTrue(anyRecords.isEmpty());
		
		Record youngPigeon = dataService.instantiate(pigeonType.getKID(), env);
		youngPigeon.setField("name", "Zenek");
		youngPigeon.setField("age", 2);
		youngPigeon.setField("birthdate", new Date (112, 3, 4));
		youngPigeon = dataService.save(youngPigeon, env);
		assertNotNull(youngPigeon.getKID());
		
		anyRecords = anyRecordService.get(null, authData, env);
		assertEquals(1, anyRecords.size());
		assertEquals(youngPigeon.getKID(), anyRecords.get(0).getRecordId());
		
		Record oldPigeon = dataService.instantiate(pigeonType.getKID(), env);
		oldPigeon.setField("name", "Zenek2");
		oldPigeon.setField("age", 2);
		oldPigeon.setField("birthdate", new Date (112, 3, 4));
		oldPigeon = dataService.save(oldPigeon, env);
		assertNotNull(oldPigeon.getKID());
		
		// update pigeon
		oldPigeon.setField("name", "Zenek3");
		oldPigeon = dataService.save(oldPigeon, env);
		
		anyRecords = anyRecordService.get(null, authData, env);
		assertEquals(2, anyRecords.size());
		
		// delete pigeon
		dataService.deleteRecord(youngPigeon, env);
		
		anyRecords = anyRecordService.get(null, authData, env);
		assertEquals(1, anyRecords.size());
		assertEquals(oldPigeon.getKID(), anyRecords.get(0).getRecordId());
	}

}
