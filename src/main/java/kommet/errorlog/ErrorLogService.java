/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.errorlog;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.ErrorLog;
import kommet.basic.User;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

@Service
public class ErrorLogService
{
	@Inject
	ErrorLogDao errorLogDao;

	@Transactional(readOnly = true)
	public List<ErrorLog> get (ErrorLogFilter filter, EnvData env) throws KommetException
	{
		return errorLogDao.get(filter, env);
	}

	@Transactional(readOnly = true)
	public ErrorLog get (KID id, EnvData env) throws KommetException
	{
		return errorLogDao.get(id, env);
	}

	@Transactional
	public ErrorLog save (ErrorLog log, AuthData authData, EnvData env) throws KommetException
	{
		return errorLogDao.save(log, authData, env);
	}

	@Transactional(readOnly = true)
	public ErrorLog get (KID id, AuthData authData, EnvData env) throws KommetException
	{
		return errorLogDao.get(id, authData, env);
	}

	@Transactional
	public ErrorLog logException(Exception ex, ErrorLogSeverity severity, String codeClass, int codeLine, KID userId, AuthData authData, EnvData env) throws KommetException
	{
		ErrorLog log = new ErrorLog();
		User affectedUser = new User();
		affectedUser.setId(userId);
		log.setAffectedUser(affectedUser);
		log.setMessage(ex.getMessage() != null ? ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 500)) : "<no-message>");
		log.setSeverity(severity.name());
		log.setCodeClass(codeClass);
		log.setCodeLine(codeLine);

		String desc = MiscUtils.getExceptionDesc(ex);
		log.setDetails(desc.substring(0, Math.min(desc.length(), 10000)));

		return errorLogDao.save(log, authData, env);
	}

	@Transactional
	public ErrorLog log(String msg, ErrorLogSeverity severity, String codeClass, int codeLine, KID userId, AuthData authData, EnvData env) throws KommetException
	{
		ErrorLog log = new ErrorLog();
		User affectedUser = new User();
		affectedUser.setId(userId);
		log.setAffectedUser(affectedUser);
		log.setMessage(msg != null ? msg.substring(0, Math.min(msg.length(), 500)) : "<no-message>");
		log.setSeverity(severity.name());
		log.setCodeClass(codeClass);
		log.setCodeLine(codeLine);

		String desc = msg;
		log.setDetails(desc.substring(0, Math.min(desc.length(), 10000)));

		return errorLogDao.save(log, authData, env);
	}

	@Transactional
	public void logException(Exception e, ErrorLogSeverity fatal, String name, int codeLine, KID userId, EnvData env) throws KommetException
	{
		logException(e, fatal, name, codeLine, userId, AuthData.getRootAuthData(env), env);
	}
}