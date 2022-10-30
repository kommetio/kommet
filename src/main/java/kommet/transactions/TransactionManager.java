/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.transactions;

import org.springframework.transaction.PlatformTransactionManager;

/**
 * A wrapper that hides the actual implementation of the transaction manager from the user,
 * providing transaction manipulation possibilities to users.
 * 
 * @author Radek Krawiec
 * @created 23-02-2014
 */
public class TransactionManager
{
	private PlatformTransactionManager transactionManager;
	
	public TransactionManager (PlatformTransactionManager txManager)
	{
		this.transactionManager = txManager;
	}
	
	public Savepoint getSavepoint()
	{
		return new Savepoint(transactionManager);
	}
}