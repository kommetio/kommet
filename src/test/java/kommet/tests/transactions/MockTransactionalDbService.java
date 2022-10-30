/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.transactions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.env.EnvData;
import kommet.tests.exceptions.MockException;

@Service
public class MockTransactionalDbService
{	
	@Transactional
	public void setSetting (String name, String value, EnvData envData, boolean throwException) throws MockException
	{
		String sql = "INSERT INTO settings (name, value) VALUES ('" + name + "', '" + value + "')";
		envData.getJdbcTemplate().update(sql);
		if (throwException)
		{
			throw new MockException("Mock DB exception");
		}
	}
}
