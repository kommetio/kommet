/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.basic.LoginHistory;
import kommet.basic.User;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;

@Service
public class LoginHistoryService
{
	@Inject
	LoginHistoryDao loginHistoryDao;
	
	@Transactional(readOnly = true)
	public List<LoginHistory> get (LoginHistoryFilter filter, EnvData env) throws KommetException
	{
		return loginHistoryDao.get(filter, env);
	}
	
	@Transactional
	public LoginHistory save (LoginHistory log, AuthData authData, EnvData env) throws KommetException
	{
		return loginHistoryDao.save(log, authData, env);
	}
	
	@Transactional(readOnly = true)
	public LoginHistory get (KID id, AuthData authData, EnvData env) throws KommetException
	{
		return loginHistoryDao.get(id, authData, env);
	}

	@Transactional
	public LoginHistory recordLogin(KID userId, String method, String result, String ip4Address, AuthData authData, EnvData env) throws KommetException
	{
		LoginHistory lh = new LoginHistory();
		User user = new User();
		user.setId(userId);
		lh.setLoginUser(user);
		lh.setMethod(method);
		lh.setResult(result);
		lh.setIp4Address(ip4Address);
		
		return loginHistoryDao.save(lh, authData, env);
	}
}
