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
import kommet.basic.SettingValue;
import kommet.dao.SettingValueDao;
import kommet.dao.SettingValueFilter;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;

@Service
public class SettingValueService
{
	@Inject
	SettingValueDao dao;
	
	@Transactional(readOnly = true)
	public List<SettingValue> get (SettingValueFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return dao.get(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public SettingValue get (KID id, boolean fetchAllUchFields, AuthData authData, EnvData env) throws KommetException
	{	
		SettingValueFilter filter = new SettingValueFilter();
		filter.addKID(id);
		filter.setFetchAllUchFields(fetchAllUchFields);
		List<SettingValue> values = dao.get(filter, authData, env);
		return values.isEmpty() ? null : values.get(0);
	}
	
	@Transactional(readOnly = true)
	public SettingValue get (String key, boolean fetchAllUchFields, AuthData authData, EnvData env) throws KommetException
	{	
		SettingValueFilter filter = new SettingValueFilter();
		filter.addKey(key);
		filter.setFetchAllUchFields(fetchAllUchFields);
		List<SettingValue> values = dao.get(filter, authData, env);
		return values.isEmpty() ? null : values.get(0);
	}
	
	@Transactional
	public SettingValue save(SettingValue obj, AuthData authData, EnvData env) throws KommetException
	{
		return dao.save(obj, authData, env);
	}
	
	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		dao.delete(id, authData, env);
	}
}