/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.data;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import kommet.dao.FieldDefinitionException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class TypeInsertTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppConfig config;
	
	@Inject
	TransactionManager txManager;
	
	@Transactional
	public void insertTypeWithError() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		// add multi-enum field
		Field labelsField = new Field();
		labelsField.setApiName("labels");
		labelsField.setLabel("Labels");
		labelsField.setDataType(new MultiEnumerationDataType());
		labelsField.setRequired(false);
		pigeonType.addField(labelsField);
		
		try
		{
			pigeonType = dataService.createType(pigeonType, env);
			fail("It should not be possible to save a multi-enum field with null value list");
		}
		catch (FieldDefinitionException e)
		{
			// expected
			assertTrue(e.getMessage().startsWith("Multi-enumeration field"));
		}
	}
	
	@Test
	public void testRollbackOnError() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
	}
}
