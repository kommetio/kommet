/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.ViewResource;
import kommet.dao.ViewResourceDao;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.ViewResourceFilter;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

@Service
public class ViewResourceService
{
	@Inject
	ViewResourceDao viewResourceDao;
	
	@Inject
	AppConfig config;
	
	private static final Logger log = LoggerFactory.getLogger(ViewResourceService.class);
	
	@Transactional
	public ViewResource save(ViewResource resource, AuthData authData, EnvData env) throws KommetException
	{
		// convert null resource content to empty string
		if (resource.getContent() == null)
		{
			resource.setContent("");
		}
		
		if (!StringUtils.hasText(resource.getPath()))
		{
			// generate a random path for this resource
			resource.setPath(MiscUtils.getHash(30));
		}
		
		resource = viewResourceDao.save(resource, authData, env);
		env.initViewResources(this);
		
		// store this view on disk
		storeViewResourceOnDisk(resource, env);
		
		return resource;
	}
	
	@Transactional(readOnly = true)
	public List<ViewResource> find (ViewResourceFilter filter, EnvData env) throws KommetException
	{
		return viewResourceDao.find(filter, env);
	}
	
	/**
	 * Delete the view resource from database and from disk.
	 * @param resourceId
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	@Transactional
	public void delete (KID resourceId, AuthData authData, EnvData env) throws KommetException
	{
		ViewResource resource = viewResourceDao.get(resourceId, env);
		viewResourceDao.delete(resourceId, authData, env);
		removeViewResourceFileFromDisk(resource.getPath(), env);
	}

	private void removeViewResourceFileFromDisk(String path, EnvData env) throws KommetException
	{
		File file = new File(env.getViewResourceEnvDir(config.getViewResourceDir()) + "/" + path);
		if (!file.delete())
		{
			throw new KommetException("Failed to delete view resource from disk");
		}
	}

	@Transactional
	public void initViewResourcesOnDisk(EnvData env, boolean forceInit) throws KommetException
	{
		// check if view resource directory exists
		File sharedViewResourceDir = new File(config.getViewResourceDir());
		
		if (!(sharedViewResourceDir.exists() && sharedViewResourceDir.isDirectory()))
		{
			// create directory
			if (!sharedViewResourceDir.mkdir())
			{
				throw new KommetException("Failed to create shared view resource directory");
			}
		}
		
		// now check if the env-specific view resource directory exists
		File envViewResourceDir = new File(env.getViewResourceEnvDir(config.getViewResourceDir()));
		
		if (envViewResourceDir.exists() && envViewResourceDir.isDirectory())
		{
			if (!forceInit)
			{
				return;
			}
			else
			{
				try
				{
					// delete directory with contents - it will be created anew
					FileUtils.deleteDirectory(envViewResourceDir);
				}
				catch (IOException e)
				{
					throw new KommetException("Failed to delete env-specific view resource directory");
				}
			}
		}
		
		// create directory anew
		if (!envViewResourceDir.mkdir())
		{
			throw new KommetException("Failed to create env-specific view resource directory");
		}
		
		log.info("Created view resource directory");
		
		storeViewResourcesOnDisk(env);
	}

	/**
	 * Retrieves all view resources from the database and stores them on disk.
	 * @param env
	 * @throws KommetException
	 */
	private void storeViewResourcesOnDisk (EnvData env) throws KommetException
	{
		ViewResourceFilter filter = new ViewResourceFilter();
		filter.setFetchContent(true);
		
		// find all resources
		List<ViewResource> resources = find(filter, env);
		
		for (ViewResource resource : resources)
		{
			storeViewResourceOnDisk(resource, env);
		}
		
		log.info("Stored view resources on disk");
	}

	private void storeViewResourceOnDisk(ViewResource resource, EnvData env) throws KommetException
	{
		try
		{
			FileOutputStream fos = new FileOutputStream(env.getViewResourceEnvDir(config.getViewResourceDir()) + "/" + resource.getPath());
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
			writer.write(resource.getContent());
			writer.close();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing view resource to disk: " + e.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public ViewResource get(KID rid, EnvData env) throws KommetException
	{
		return viewResourceDao.get(rid, env);
	}

	@Transactional(readOnly = true)
	public ViewResource getByName(String name, AuthData authData, EnvData env) throws KommetException
	{
		ViewResourceFilter filter = new ViewResourceFilter();
		filter.setName(name);
		List<ViewResource> resources = viewResourceDao.find(filter, env);
		return !resources.isEmpty() ? resources.get(0) : null;
		
	}
}