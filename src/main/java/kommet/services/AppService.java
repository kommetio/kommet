/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.App;
import kommet.basic.AppUrl;
import kommet.dao.AppDao;
import kommet.dao.AppUrlDao;
import kommet.dao.DomainMappingDao;
import kommet.data.DomainMapping;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.AppFilter;
import kommet.filters.AppUrlFilter;
import kommet.utils.MiscUtils;
import kommet.utils.ValidationUtil;

@Service
public class AppService
{
	@Inject
	AppDao appDao;
	
	@Inject
	AppUrlDao appUrlDao;
	
	@Inject
	DomainMappingDao domainMappingDao;
	
	private static final Logger log = LoggerFactory.getLogger(AppService.class);
	
	/**
	 * Deletes an app, all its related app URLs and their domain mappings.
	 * 
	 * Note: since this method combines operations on two data sources in one transaction, it requires the
	 * max_prepared_transactions on Postgres (postgresql.conf) to be set to larger than zero.
	 * 
	 * @param id
	 * @param authData
	 * @param env
	 * @param sharedEnv
	 * @throws KommetException
	 */
	@Transactional
	public void delete (KID id, AuthData authData, EnvData env, EnvData sharedEnv) throws KommetException
	{
		// find delete app URLs and domain mappings
		AppUrlFilter filter = new AppUrlFilter();
		filter.addAppId(id);
		List<AppUrl> appUrls = appUrlDao.find(filter, env);
		
		Set<String> urls = new HashSet<String>();
		for (AppUrl appUrl : appUrls)
		{
			urls.add(appUrl.getUrl());
		}
		
		// delete domain mappings
		domainMappingDao.deleteForUrls(urls, sharedEnv);
		
		appUrlDao.delete(appUrls, authData, env);
		
		appDao.delete(id, authData, env);
	}
	
	@Transactional
	public App save (App app, AuthData authData, EnvData env) throws KommetException
	{
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(app.getName()))
		{
			throw new KommetException("Invalid app name " + app.getName());
		}
		
		return appDao.save(app, authData, env);
	}
	
	@Transactional(readOnly = true)
	public List<AppUrl> find (AppUrlFilter filter, EnvData env) throws KommetException
	{
		return appUrlDao.find(filter, env);
	}
	
	@Transactional(readOnly = true)
	public List<App> find (AppFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return appDao.get(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public App get (KID appId, AuthData authData, EnvData env) throws KommetException
	{
		AppFilter filter = new AppFilter();
		filter.addAppId(appId);
		List<App> apps = appDao.get(filter, authData, env);
		return apps.isEmpty() ? null : apps.get(0);
	}
	
	/**
	 * Save an app URL and its domain mapping.
	 * 
	 * Note: since this method combines operations on two data sources in one transaction, it requires the
	 * max_prepared_transactions on Postgres (postgresql.conf) to be set to larger than zero.
	 * @param appUrl
	 * @param authData
	 * @param env
	 * @param sharedEnv
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public AppUrl save (AppUrl appUrl, AuthData authData, EnvData env, EnvData sharedEnv) throws KommetException
	{
		try
		{
			appUrl = appUrlDao.save(appUrl, authData, env);
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			log.debug("Error saving app url " + appUrl.getUrl());
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.debug("Error saving app url " + appUrl.getUrl());
			throw new KommetException("Error saving app URL: " + e.getMessage());
		}
		
		// save the domain mapping for this app URL in the shared database
		DomainMapping mapping = new DomainMapping();
		mapping.setEnv(env.getEnv());
		mapping.setUrl(appUrl.getUrl());
		
		try
		{
			mapping = domainMappingDao.save(mapping, sharedEnv);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.debug("Error saving domain mapping " + appUrl.getUrl());
			throw new KommetException("Error saving domain mapping: " + e.getMessage());
		}
		
		return appUrl;
	}

	/**
	 * Deletes an app URL together with its domain mapping in the master database.
	 * 
	 * Note: since this method combines operations on two data sources in one transaction, it requires the
	 * max_prepared_transactions on Postgres (postgresql.conf) to be set to larger than zero.
	 * 
	 * @param id
	 * @param authData
	 * @param env
	 * @param sharedEnv
	 * @throws KommetException
	 */
	@Transactional
	public void deleteAppUrl(KID id, AuthData authData, EnvData env, EnvData sharedEnv) throws KommetException
	{
		// find app URL
		AppUrl appUrl = appUrlDao.get(id, authData, env);
		if (appUrl == null)
		{
			throw new KommetException("App URL with ID " + id + " not found");
		}
		
		appUrlDao.delete(id, authData, env);
		
		// delete domain mappings
		domainMappingDao.deleteForUrls(MiscUtils.toSet(appUrl.getUrl()), sharedEnv);
	}

	@Transactional(readOnly = true)
	public DomainMapping getDomainMapping(String url, EnvData env) throws KommetException
	{
		return domainMappingDao.getForURL(url, env);
	}

	@Transactional(readOnly = true)
	public App getAppByName(String name, AuthData authData, EnvData env) throws KommetException
	{
		AppFilter filter = new AppFilter();
		filter.setName(name);
		List<App> apps = appDao.get(filter, authData, env);
		return apps.isEmpty() ? null : apps.get(0);
	}
}