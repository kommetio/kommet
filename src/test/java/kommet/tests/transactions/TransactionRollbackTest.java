/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.transactions;

import static org.junit.Assert.assertFalse;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.junit.Test;
import org.springframework.transaction.jta.JtaTransactionManager;

import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.services.GlobalSettingsService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.tests.exceptions.MockException;

public class TransactionRollbackTest extends BaseUnitTest
{
	@Inject
	MockTransactionalDbService mockDbService;
	
	@Inject
	GlobalSettingsService settingService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	JtaTransactionManager transactionManager;
	
	@Test
	public void testRollback() throws KommetException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException
	{
		EnvData envData = dataHelper.getTestEnvData(false);
		try
		{
			mockDbService.setSetting("test-1234", "test-1234", envData, true);
		}
		catch (MockException e)
		{
			transactionManager.getUserTransaction().rollback();
		}
		
		assertFalse("DB transaction not rolled back", settingService.settingExists("test-1234", envData));
	}
}
