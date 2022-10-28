/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.transactions;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class Savepoint
{
	private TransactionStatus status;
	private PlatformTransactionManager txManager;
	
	public Savepoint (PlatformTransactionManager txManager)
	{
		// Savepoints are simulated by new transactions, since creating a savepoint resulted in an exception
		// saying "nested transaction object does not support savepoints".
		// The transaction created below must has propagation REQUIRES_NEW to make it independent from the
		// enclosing transaction.
		TransactionDefinition def = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NESTED);
		this.status = txManager.getTransaction(def);
		this.txManager = txManager;
	}
	
	public void release()
	{
		this.txManager.commit(this.status);
	}
	
	public void rollback()
	{
		this.txManager.rollback(this.status);
	}
}