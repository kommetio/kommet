/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.File;
import kommet.basic.WebResource;
import kommet.dao.WebResourceDao;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.files.FileDao;
import kommet.files.FileFilter;
import kommet.files.FileService;
import kommet.filters.WebResourceFilter;
import kommet.integration.PropertySelection;
import kommet.utils.ValidationUtil;

@Service
public class WebResourceService
{
	@Inject
	WebResourceDao dao;
	
	@Inject
	FileDao fileDao;
	
	@Inject
	FileService fileService;
	
	@Inject
	SharingService sharingService;
	
	@Transactional(readOnly = true)
	public List<WebResource> find (WebResourceFilter filter, EnvData env) throws KommetException
	{
		return dao.find(filter, env);
	}
	
	/**
	 * 
	 * @param resource
	 * @param reinitCachedResources Tells if cached web resources should be reinitialized on the env. This options should basically always be set to true.
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public WebResource save(WebResource resource, boolean reinitCachedResources, AuthData authData, EnvData env) throws KommetException
	{
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(resource.getName()))
		{
			throw new KommetException("Invalid web resource name");
		}
		
		resource = dao.save(resource, authData, env);
		
		if (reinitCachedResources)
		{
			env.initWebResources(this);
		}
		
		if (Boolean.TRUE.equals(resource.getIsPublic()))
		{
			// share with guest user
			sharingService.shareRecord(resource.getId(), env.getGuestUser().getId(), false, false, authData, "Public Web Resource", true, env);
			
			// share file
			sharingService.shareRecord(resource.getFile().getId(), env.getGuestUser().getId(), false, false, authData, "Public Web Resource File", true, env);
		}
		else
		{
			// unshare with guest user
			sharingService.unshareRecord(resource.getId(), env.getGuestUser().getId(), authData, env);
			
			// unshare file
			sharingService.unshareRecord(resource.getFile().getId(), env.getGuestUser().getId(), authData, env);
		}
		
		return resource;
	}
	
	@Transactional
	public void delete(KID id, boolean deleteFile, AuthData authData, EnvData env) throws KommetException
	{
		if (deleteFile)
		{
			// first find the resource to have the file ID
			WebResource resource = dao.get(id, env);
			
			if (resource.getFile() == null)
			{
				throw new KommetException("Cannot delete web resource because the associated file has not been retrieved");
			}
			
			// delete the resource
			dao.delete(id, authData, env);
			
			// delete the associated file with revisions
			fileService.deleteFile(resource.getFile(), true, authData, env);
		}
		else
		{
			dao.delete(id, authData, env);
		}
	}

	@Transactional(readOnly = true)
	public WebResource get(KID id, EnvData env) throws KommetException
	{
		WebResource res = dao.get(id, PropertySelection.SPECIFIED, "id, name, createdDate, createdBy.id, lastModifiedDate, lastModifiedBy.id, file.id, file.name, mimeType", env);
		
		if (res != null)
		{
			res.setPublic(sharingService.canViewRecord(res.getId(), env.getGuestUser().getId(), env));
		}
		
		return res;
	}

	@Transactional(readOnly = true)
	public List<WebResource> initFilePath(List<WebResource> resources, AuthData authData, EnvData env) throws KommetException
	{
		if (resources.isEmpty())
		{
			return resources;
		}
		
		FileFilter filter = new FileFilter();
		
		// collect file IDs
		Map<KID, WebResource> resourcesByFileId = new HashMap<KID, WebResource>();
		Map<KID, WebResource> resourcesById = new HashMap<KID, WebResource>();
		for (WebResource r : resources)
		{
			filter.addId(r.getFile().getId());
			resourcesByFileId.put(r.getFile().getId(), r);
			resourcesById.put(r.getId(), r);
		}
		
		// find files
		List<File> files = fileService.find(filter, true, true, authData, env);
		
		for (File file : files)
		{
			if (file.getRevisions() == null || file.getRevisions().isEmpty())
			{
				throw new KommetException("No revisions found for file " + file.getId());
			}
			resourcesByFileId.get(file.getId()).setDiskFilePath(file.getLatestRevision().getPath());
		}
		
		return resources;
	}

	@Transactional(readOnly = true)
	public WebResource getByName(String name, AuthData authData, EnvData env) throws KommetException
	{
		WebResourceFilter filter = new WebResourceFilter();
		filter.setName(name);
		List<WebResource> resources = dao.find(filter, env);
		WebResource res = resources.isEmpty() ? null : resources.get(0);
		
		if (res != null)
		{
			res.setPublic(sharingService.canViewRecord(res.getId(), env.getGuestUser().getId(), env));
		}
		
		return res;
	}
}