/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.File;
import kommet.basic.FileRecordAssignment;
import kommet.basic.FileRevision;
import kommet.basic.Profile;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.User;
import kommet.basic.types.SystemTypes;
import kommet.data.DataService;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.ValidationMessage;
import kommet.env.EnvData;
import kommet.files.FileException;
import kommet.files.FileFilter;
import kommet.files.FileRecordAssignmentFilter;
import kommet.files.FileRevisionDao;
import kommet.files.FileRevisionFilter;
import kommet.files.FileService;
import kommet.services.SystemSettingService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class FileTest extends BaseUnitTest
{
	@Inject
	SystemSettingService settingService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	FileService fileService;
	
	@Inject
	FileRevisionDao revisionDao;
	
	@Inject
	DataService dataService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	UserService userService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ProfileService profileService;
	
	@Test
	public void testFileBasicOperations() throws KommetException, IOException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		Profile testProfile = profileService.save(dataHelper.getTestProfileObject("NewProfile", env), dataHelper.getRootAuthData(env),env);
		User testUser = userService.save(dataHelper.getTestUser("test-user", "test-user@kommet.io", testProfile, env), dataHelper.getRootAuthData(env), env);
		assertNotNull(testUser.getId());
		
		// give the test profile read access to the file type
		permissionService.setTypePermissionForProfile(testProfile.getId(), env.getType(KeyPrefix.get(KID.FILE_PREFIX)).getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		
		// create a file
		File testFile = new File();
		testFile.setAccess(File.PUBLIC_ACCESS);
		testFile.setName("FileName");
		
		// save file
		testFile = fileService.saveFile(testFile, dataHelper.getRootAuthData(env), env);
		assertNotNull(testFile.getId());
		assertEquals(Long.valueOf(1), env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.FILE_API_NAME).count());
		
		// create a revision
		FileRevision testRevision = new FileRevision();
		testRevision.setName("My journal");
		testRevision.setSize(1);
		testRevision.setRevisionNumber(1);
		
		try
		{
			fileService.saveRevision(testRevision, dataHelper.getRootAuthData(env), env);
			fail("Saving file revision should fail because no file is assigned to revision");
		}
		catch (FieldValidationException e)
		{
			// make sure there are validation errors for file and path fields
			assertEquals(2, e.getMessages().size());
			
			boolean pathErrorFound = false;
			boolean fileErrorFound = false;
			
			for (ValidationMessage err : e.getMessages())
			{
				if (err.getText().contains("path"))
				{
					pathErrorFound = true;
				}
				else if (err.getText().contains("file"))
				{
					fileErrorFound = true;
				}
			}
			
			assertTrue(pathErrorFound);
			assertTrue(fileErrorFound);
		}
		
		testRevision.setFile(testFile);
		testRevision.setPath("38293nhj323k3hb323bbj23b232kj3b223k21");
		testRevision = fileService.saveRevision(testRevision, dataHelper.getRootAuthData(env), env);
		assertNotNull(testRevision.getId());
		assertNotNull(fileService.getFileRevision(testRevision.getId(), dataHelper.getRootAuthData(env), env));
		
		// create another revision
		FileRevision testRevision2 = new FileRevision();
		testRevision2.setName("My journal");
		testRevision2.setRevisionNumber(2);
		testRevision2.setFile(testFile);
		testRevision2.setSize(1);
		testRevision2.setPath("38293nhj323k3hb323bjkddff9009");
		testRevision2 = fileService.saveRevision(testRevision2, dataHelper.getRootAuthData(env), env);
		assertNotNull(testRevision2.getId());
		
		FileRevision testRevision4 = new FileRevision();
		testRevision4.setName("My journal");
		testRevision4.setRevisionNumber(4);
		testRevision4.setFile(testFile);
		testRevision4.setSize(1);
		testRevision4.setPath("3829yyyj323k3hb323bjkddff9009");
		testRevision4 = fileService.saveRevision(testRevision4, dataHelper.getRootAuthData(env), env);
		assertNotNull(testRevision4.getId());
		
		// create another file with one revision
		File testFile2 = new File();
		testFile2.setAccess(File.PUBLIC_ACCESS);
		testFile2.setName("FileName2");
		AuthData testUserAuthData = dataHelper.getAuthData(userService.getUser(testUser.getId(), env), env);
		testFile2 = fileService.saveFile(testFile2, testUserAuthData, env);
		FileRevision testRevision3 = new FileRevision();
		testRevision3.setName("Book");
		testRevision3.setRevisionNumber(1);
		testRevision3.setFile(testFile2);
		testRevision3.setSize(1);
		testRevision3.setPath("38293nhj323k3hb323bbj23b232kj3b223k11");
		testRevision3 = fileService.saveRevision(testRevision3, dataHelper.getRootAuthData(env), env);
		
		// get revisions ordered by file ID (ASC) and revision number (DESC)
		List<FileRevision> orderedRevisions = revisionDao.findOrderedRevisions(null, env);
		assertEquals(4, orderedRevisions.size());
		// make sure revisions are properly ordered
		assertEquals(testFile.getId(), orderedRevisions.get(0).getFile().getId());
		assertEquals(testFile.getId(), orderedRevisions.get(1).getFile().getId());
		assertEquals(testFile.getId(), orderedRevisions.get(2).getFile().getId());
		assertEquals(testFile2.getId(), orderedRevisions.get(3).getFile().getId());
		assertEquals(testRevision4.getId(), orderedRevisions.get(0).getId());
		assertEquals(testRevision2.getId(), orderedRevisions.get(1).getId());
		assertEquals(testRevision.getId(), orderedRevisions.get(2).getId());
		assertEquals(testRevision3.getId(), orderedRevisions.get(3).getId());
		
		List<File> filesWithRevisions = fileService.find(null, true, true, null, env);
		assertNotNull(filesWithRevisions);
		assertEquals(2, filesWithRevisions.size());
		File fileWithRevisions = filesWithRevisions.get(0);
		assertNotNull(fileWithRevisions.getRevisions());
		assertEquals(3, fileWithRevisions.getRevisions().size());
		// make sure revisions within a file are ordered by revisionNumber in descending order
		assertEquals(testRevision4.getId(), fileWithRevisions.getRevisions().get(0).getId());
		assertEquals(testRevision2.getId(), fileWithRevisions.getRevisions().get(1).getId());
		assertEquals(testRevision.getId(), fileWithRevisions.getRevisions().get(2).getId());
		
		// make sure that when files are fetched using sharing, only files visible to the
		// given user are returned
		List<File> testUserFiles = fileService.find(null, true, true, testUserAuthData, env);
		assertEquals(1, testUserFiles.size());
		assertEquals(testFile2.getId(), testUserFiles.get(0).getId());
		
		// now delete the file and make sure all its revisions have been deleted as well
		fileService.deleteFile(testFile, true, null, env);
		assertEquals(Long.valueOf(1), env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.FILE_API_NAME).count());
		assertEquals(Long.valueOf(1), env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.FILE_REVISION_API_NAME).count());
		fileService.deleteFile(testFile2, true, null, env);
		assertEquals(Long.valueOf(0), env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.FILE_API_NAME).count());
		assertEquals(Long.valueOf(0), env.getSelectCriteriaFromDAL("select count(id) from " + SystemTypes.FILE_REVISION_API_NAME).count());
		
		testFileRecordAssignments(env);
		testSaveFile(env);
		
		// the method below has to be called at the end of the test because it causes the transaction to
		// fail completely
		testUniqueRevisionPath(env);
	}

	private void testSaveFile(EnvData env) throws IOException, KommetException
	{
		int revisionsBeforeFileInsert = fileService.findRevisions(null, env).size();
		
		// create some file on disk
		java.io.File diskFile = new java.io.File(appConfig.getFileDir() + "/somefile.txt");
		FileWriter fw = new FileWriter(diskFile);
		fw.write("some text");
		fw.close();
		
		File file = fileService.saveFile(null, "myfile.txt", diskFile.getName(), File.PUBLIC_ACCESS, true, dataHelper.getRootAuthData(env), env);
		assertNotNull(file);
		assertNotNull("File not saved", file.getId());
		
		FileRevisionFilter filter = new FileRevisionFilter();
		filter.addFileId(file.getId());
		List<FileRevision> revisions = fileService.findRevisions(filter, env);
		assertNotNull(revisions);
		assertEquals("Exactly one revision should have been created for a new file", 1, revisions.size());
		
		// now try to create a file for an non-existing path
		try
		{
			fileService.saveFile(null, "myfile.txt", diskFile.getAbsolutePath() + "somesuffix", File.PUBLIC_ACCESS, true, dataHelper.getRootAuthData(env), env);
			fail("Saving file for an invalid path should fail");
		}
		catch (FileException e)
		{
			assertTrue(e.getMessage().endsWith("does not exist"));
		}
		
		Integer initialRevisionCount = fileService.findRevisions(null, env).size();
		
		// now save a new file at the old name
		diskFile = new java.io.File(appConfig.getFileDir() + "/somefile2.txt");
		fw = new FileWriter(diskFile);
		fw.write("some text 2");
		fw.close();
		
		File file2 = fileService.saveFile(file.getId(), "myfile.txt", diskFile.getName(), File.PUBLIC_ACCESS, false, dataHelper.getRootAuthData(env), env);
		assertEquals(file.getId(), file2.getId());
		assertEquals(initialRevisionCount, (Integer)fileService.findRevisions(null, env).size());
		
		// now save a new file as a new revision
		diskFile = new java.io.File(appConfig.getFileDir() + "/somefile3.txt");
		fw = new FileWriter(diskFile);
		fw.write("some text 3");
		fw.close();
		
		File file3 = fileService.saveFile(file.getId(), "myfile.txt", diskFile.getName(), File.PUBLIC_ACCESS, true, dataHelper.getRootAuthData(env), env);
		assertEquals(file.getId(), file3.getId());
		assertEquals((Integer)(initialRevisionCount + 1), (Integer)fileService.findRevisions(null, env).size());
		assertTrue((new java.io.File(diskFile.getAbsolutePath())).exists());
		
		FileFilter fileFilter = new FileFilter();
		fileFilter.addId(file3.getId());
		
		// test deleting files
		fileService.deleteFiles(fileService.find(fileFilter, true, false, dataHelper.getRootAuthData(env), env), true, true, dataHelper.getRootAuthData(env), env);
		assertEquals((Integer)revisionsBeforeFileInsert, (Integer)fileService.findRevisions(null, env).size());
		assertFalse("Disk file not removed", (new java.io.File(diskFile.getAbsolutePath())).exists());
	}

	private void testFileRecordAssignments(EnvData env) throws KommetException
	{
		Type pigeonType = dataHelper.getFullPigeonType(env);
		pigeonType = dataService.createType(pigeonType, env);
		
		// create a file
		File testFile = new File();
		testFile.setAccess(File.PUBLIC_ACCESS);
		testFile.setName("FileName");
		
		// save file
		testFile = fileService.saveFile(testFile, dataHelper.getRootAuthData(env), env);
		
		// create some pigeon
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Heniek");
		pigeon1.setField("age", 2);
		pigeon1 = dataService.save(pigeon1, env);
		
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Alek");
		pigeon2.setField("age", 3);
		pigeon2 = dataService.save(pigeon2, env);
		
		// assign file to object
		fileService.assignFileToRecord(testFile.getId(), pigeon1.getKID(), null, dataHelper.getRootAuthData(env), env);
		
		// make sure the assignment can be found
		FileRecordAssignmentFilter filter = new FileRecordAssignmentFilter();
		filter.addRecordId(pigeon1.getKID());
		filter.setInitFiles(true);
		List<FileRecordAssignment> assignments = fileService.findAssignments(filter, dataHelper.getRootAuthData(env), env);
		assertEquals(1, assignments.size());
		FileRecordAssignment assignment = assignments.get(0);
		assertEquals(testFile.getId(), assignment.getFile().getId());
		assertEquals(testFile.getName(), assignment.getFile().getName());
		
		// now delete the assignment
		fileService.unassignFileToRecord(testFile.getId(), pigeon1.getKID(), true, dataHelper.getRootAuthData(env), env);
		assertTrue(fileService.findAssignments(filter, dataHelper.getRootAuthData(env), env).isEmpty());
		
		// add the assignment anew
		// assign file to object
		fileService.assignFileToRecord(testFile.getId(), pigeon1.getKID(), null, dataHelper.getRootAuthData(env), env);
		
		// now delete the file and make sure the assignment has been deleted as well
		fileService.deleteFile(testFile, false, dataHelper.getRootAuthData(env), env);
		assertTrue(fileService.findAssignments(filter, dataHelper.getRootAuthData(env), env).isEmpty());
		
	}

	private void testUniqueRevisionPath(EnvData env) throws KommetException
	{
		// create a file
		File testFile = new File();
		testFile.setAccess(File.PUBLIC_ACCESS);
		testFile.setName("FileName");
		
		// save file
		testFile = fileService.saveFile(testFile, dataHelper.getRootAuthData(env), env);
		
		// create another revision
		FileRevision testRevision = new FileRevision();
		testRevision.setName("My journal");
		testRevision.setRevisionNumber(2);
		testRevision.setFile(testFile);
		testRevision.setSize(1);
		testRevision.setPath("38293nhj323k322323bbj23b232kj3b223k21");
		testRevision = fileService.saveRevision(testRevision, dataHelper.getRootAuthData(env), env);
		assertNotNull(testRevision.getId());
		
		// create a file
		File testFile2 = new File();
		testFile2.setAccess(File.PUBLIC_ACCESS);
		testFile2.setName("FileName2");
		
		// save file
		testFile2 = fileService.saveFile(testFile2, dataHelper.getRootAuthData(env), env);
		
		FileRevision testRevision2 = new FileRevision();
		testRevision2.setName("My journal");
		testRevision2.setRevisionNumber(1);
		testRevision2.setFile(testFile2);
		testRevision2.setSize(1);
		testRevision2.setPath("38293nhj323k322323bbj23b232kj3b223k21");
		
		try
		{
			fileService.saveRevision(testRevision2, dataHelper.getRootAuthData(env), env);
			fail("Insert of file revision should fail because there can't be two revisions with the same path");
		}
		catch (UniqueCheckViolationException e)
		{
			// expected
		}
	}
}
