/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.AnyRecord;
import kommet.dao.AnyRecordDao;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.AnyRecordFilter;

@Service
public class AnyRecordService
{
	@Inject
	AnyRecordDao dao;
	
	@Transactional(readOnly = true)
	public List<AnyRecord> get(AnyRecordFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return dao.get(filter, authData, env);
	}
}