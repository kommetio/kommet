/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.File;
import kommet.basic.FileRecordAssignment;
import kommet.basic.FileRevision;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.AppConfig;

@Service
public class FileService
{
	@Inject
	FileDao fileDao;
	
	@Inject
	FileRevisionDao revisionDao;
	
	@Inject
	FileRecordAssignmentDao fileObjAssignmentDao;
	
	@Inject
	AppConfig config;
	
	/**
	 * Save or update file.
	 * @param fileId ID of an existing file, or null, if new file is created.
	 * @param fileName
	 * @param relativePath
	 * @param access
	 * @param createNewRevision If set to true, the file indicated by the path will be associated with a new revision. Otherwise, it will be associated with the latest existing revision for the file.
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public File saveFile (KID fileId, String fileName, String relativePath, String access, boolean createNewRevision, AuthData authData, EnvData env) throws KommetException
	{
		// first make sure the path to the disk file exists
		java.io.File diskFile = new java.io.File(config.getFileDir() + "/" + relativePath);
		
		if (!diskFile.exists())
		{
			throw new FileException("Disk file with path " + relativePath + " does not exist");
		}
		
		if (!StringUtils.hasText(relativePath))
		{
			throw new FileException("File path is empty");
		}
		
		File file = null;
		
		if (fileId != null)
		{
			// get existing file
			file = fileDao.get(fileId, authData, env);
			if (file == null)
			{
				throw new FileException("File with ID " + fileId + " not found");
			}
		}
		else
		{
			if (!createNewRevision)
			{
				throw new FileException("When new file is created, a new revision also has to be created");
			}
			file = new File();
		}
		
		file.setAccess(access);
		file.setName(fileName);
		
		// save the file
		file = fileDao.save(file, authData, env);
		
		FileRevision revision = null;
		Integer nextRevisionNumber = null;
		List<FileRevision> orderedRevisionList = null;
		
		if (fileId != null)
		{
			// get the latest revision for this file
			FileRevisionFilter revisionFilter = new FileRevisionFilter();
			revisionFilter.addFileId(fileId);
			// get list of revisions ordered by file ID and revision number
			orderedRevisionList = revisionDao.findOrderedRevisions(revisionFilter, env);
			
			if (orderedRevisionList.isEmpty())
			{
				throw new FileException("Illegal state: file with ID " + fileId + " has no revisions");
			}
			
			nextRevisionNumber = orderedRevisionList.get(0).getRevisionNumber() + 1;
		}
		else
		{
			nextRevisionNumber = 1;
		}
		
		if (createNewRevision)
		{
			// create and save a revision
			revision = new FileRevision();
			revision.setFile(file);
			revision.setRevisionNumber(nextRevisionNumber);
		}
		else
		{	
			// get the latest revision
			revision = orderedRevisionList.get(0);
			
			// delete the previous path for this revision
			java.io.File previousDiskFile = new java.io.File(config.getFileDir() + "/" + revision.getPath());
			if (!previousDiskFile.exists())
			{
				throw new FileException("Illegal state: disk file for the previous revision does not exist");
			}
			
			if (!previousDiskFile.delete())
			{
				throw new FileException("Previous disk file for revision " + revision.getId() + " could not be removed");
			}
		}
		
		revision.setName(fileName);
		revision.setPath(relativePath);
		
		// get file size
		revision.setSize(Long.valueOf(diskFile.length()).intValue());
		revisionDao.save(revision, authData, env);
		
		return file;
	}
	
	@Transactional(readOnly = true)
	public File getFileById (KID id, EnvData env) throws KommetException
	{
		return fileDao.get(id, env);
	}

	@Transactional
	public File saveFile(File file, AuthData authData, EnvData env) throws KommetException
	{
		return fileDao.save(file, authData, env);
	}

	@Transactional
	public FileRevision saveRevision(FileRevision revision, AuthData authData, EnvData env) throws KommetException
	{
		return revisionDao.save(revision, authData, env);
	}
	
	@Transactional
	public FileRecordAssignment assignFileToRecord (KID fileId, KID recordId, String comment, AuthData authData, EnvData env) throws KommetException
	{
		// TODO should this method check if such assignment already exists? or should user do it?
		FileRecordAssignment assignment = new FileRecordAssignment();
		assignment.setComment(comment);
		
		File file = new File();
		file.setId(fileId);
		assignment.setFile(file);
		
		assignment.setRecordId(recordId);
		
		return fileObjAssignmentDao.save(assignment, authData, env);
	}
	
	@Transactional
	public void unassignFileToRecord (KID fileId, KID recordId, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{
		FileRecordAssignmentFilter filter = new FileRecordAssignmentFilter();
		filter.addFileId(fileId);
		filter.addRecordId(recordId);
		List<FileRecordAssignment> assignments = fileObjAssignmentDao.find(filter, authData, env);
		if (assignments.isEmpty())
		{
			throw new KommetException("File " + fileId + " is not assigned to record " + recordId);
		}
		else if (assignments.size() > 1)
		{
			throw new KommetException("More than one assignment found for file " + fileId + " and record " + recordId);
		}
		else
		{
			fileObjAssignmentDao.delete(assignments, skipTriggers, authData, env);
		}
	}
	
	@Transactional(readOnly = true)
	public List<FileRecordAssignment> findAssignments (FileRecordAssignmentFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return fileObjAssignmentDao.find(filter, authData, env);
	}

	@Transactional
	public void deleteFile(File file, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{
		// first delete all revisions
		FileRevisionFilter filter = new FileRevisionFilter();
		filter.addFileId(file.getId());
		List<FileRevision> revisions = revisionDao.find(filter, env);
		
		if (!revisions.isEmpty())
		{
			revisionDao.delete(revisions, authData, env);
		}
		
		// then delete all file object associations
		FileRecordAssignmentFilter fileTypeFilter = new FileRecordAssignmentFilter();
		fileTypeFilter.addFileId(file.getId());
		List<FileRecordAssignment> assignments = findAssignments(fileTypeFilter, authData, env);
		fileObjAssignmentDao.delete(assignments, authData, env);
		
		List<File> files = new ArrayList<File>();
		files.add(file);
		// now delete the file itself
		fileDao.delete(files, skipTriggers, authData, env);
	}

	// TODO this method is not optimal
	@Transactional(readOnly = true)
	public List<File> find (FileFilter filter, boolean initRevisions, boolean orderedRevisions, AuthData authData, EnvData env) throws KommetException
	{
		// Search for files, but initialize revisions only if they are not supposed to be sorted.
		// If revisions are to be sorted, they will be fetched by another query
		List<File> files = fileDao.find(filter, initRevisions && !orderedRevisions, authData, env);
		
		if (orderedRevisions)
		{
			if (!initRevisions)
			{
				throw new KommetException("When orderedRevisions option is selected, initRevisions should also be set - you cannot order revisions that have not been initialized");
			}
			
			Set<KID> fileIds = new HashSet<KID>();
			// sort revisions for each file
			for (File file : files)
			{
				fileIds.add(file.getId());
			}
			
			FileRevisionFilter revisionFilter = new FileRevisionFilter();
			revisionFilter.setFileIds(fileIds);
			
			// get list of revisions ordered by file ID and revision number
			List<FileRevision> orderedRevisionList = revisionDao.findOrderedRevisions(revisionFilter, env);
			
			Map<KID, ArrayList<FileRevision>> revisionsByFileId = new HashMap<KID, ArrayList<FileRevision>>();
			
			// map revisions by file ID
			for (FileRevision revision : orderedRevisionList)
			{
				ArrayList<FileRevision> revisionsForFile = revisionsByFileId.get(revision.getFile().getId());
				if (revisionsForFile == null)
				{
					revisionsForFile = new ArrayList<FileRevision>(); 
					revisionsByFileId.put(revision.getFile().getId(), revisionsForFile);
				}
				
				revisionsForFile.add(revision);
			}
			
			// Only now do we assign ordered revisions to files - this way if there is any file ordering
			// it will be preserved
			for (File file : files)
			{
				file.setRevisions(revisionsByFileId.get(file.getId()));
			}
		}
		
		return files;
	}

	@Transactional(readOnly = true)
	public FileRevision findRevision(KID id, EnvData env) throws KommetException
	{
		FileRevisionFilter filter = new FileRevisionFilter();
		filter.setId(id);
		List<FileRevision> revisions = revisionDao.find(filter, env);
		return revisions.isEmpty() ? null : revisions.get(0);
	}

	@Transactional(readOnly = true)
	public List<FileRevision> findRevisions(FileRevisionFilter filter, EnvData env) throws KommetException
	{
		return revisionDao.find(filter, env);
	}

	@Transactional
	public void deleteFiles(List<File> files, boolean deleteDiskFiles, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{
		for (File file : files)
		{
			if (file.getRevisions() == null)
			{
				throw new FileException("Cannot delete file " + file.getId() + ". File revisions not initialized");
			}
			
			for (FileRevision rev : file.getRevisions())
			{
				java.io.File diskFile = new java.io.File(config.getFileDir() + "/" + rev.getPath());
				if (!diskFile.exists())
				{
					throw new FileException("Disk file for file revision " + rev.getId() + " does not exist");
				}
				
				if (!diskFile.delete())
				{
					throw new FileException("Disk file for file revision " + rev.getId() + " could not be deleted");
				}
			}
			
			deleteFile(file, skipTriggers, authData, env);
		}
	}

	@Transactional(readOnly = true)
	public FileRevision getFileRevision(KID id, AuthData authData, EnvData env) throws KommetException
	{
		return revisionDao.get(id, authData, env);
	}
}