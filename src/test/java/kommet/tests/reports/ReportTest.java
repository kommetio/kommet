/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.ReportType;
import kommet.data.DataService;
import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.reports.ReportService;
import kommet.reports.ReportTypeFilter;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class ReportTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ReportService reportService;
	
	@Test
	public void testReportTypeCRUD() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		assertNotNull(pigeonType.getKID());
		
		assertEquals(0, reportService.getReportTypes(null, env).size());
		
		// create some report
		ReportType rt = new ReportType();
		rt.setBaseTypeId(pigeonType.getKID());
		rt.setName("Pigeon Report");
		
		AuthData authData = dataHelper.getRootAuthData(env);
		
		try
		{
			reportService.save(rt, authData, env);
			fail("Saving raport with empty DAL query should fail");
		}
		catch (FieldValidationException e)
		{
			// expected
		}
		
		rt.setSerializedQuery("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME);
		rt = reportService.save(rt, authData, env);
		assertNotNull(rt.getId());
		
		ReportTypeFilter filter = new ReportTypeFilter();
		filter.setName(rt.getName());
		assertEquals(1, reportService.getReportTypes(filter, env).size());
	}
}
