/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.reports;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.ReportType;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;

@Service
public class ReportService
{
	@Inject
	ReportTypeDao rtDao;
	
	@Transactional (readOnly = true)
	public List<ReportType> getReportTypes (ReportTypeFilter filter, EnvData env) throws KommetException
	{
		return rtDao.get(filter, null, env);
	}
	
	@Transactional (readOnly = true)
	public List<ReportType> getReportTypes (ReportTypeFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return rtDao.get(filter, authData, env);
	}
	
	@Transactional
	public ReportType save (ReportType rt, AuthData authData, EnvData env) throws KommetException
	{
		return rtDao.save(rt, authData, env);
	}

	@Transactional (readOnly = true)
	public ReportType getReportType(KID reportTypeId, EnvData env) throws KommetException
	{
		return rtDao.get(reportTypeId, env);
	}

	@Transactional
	public void deleteReportType(KID rid, AuthData authData, EnvData env) throws KommetException
	{
		rtDao.delete(rid, authData, env);
	}
}