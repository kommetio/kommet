/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dataexport;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.File;
import kommet.basic.FileRevision;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.files.FileService;
import kommet.pdf.PdfUtil;
import kommet.utils.AppConfig;
import kommet.vendorapis.msoffice.excel.DataTableUtil;
import kommet.vendorapis.msoffice.excel.ExcelFormat;
import kommet.vendorapis.msoffice.excel.MsExcelApi;
import kommet.vendorapis.msoffice.excel.Workbook;

@Service
public class DataExportService
{
	@Inject
	AppConfig appConfig;
	
	@Inject
	FileService fileService;
	
	@Transactional
	public File exportToPdfFile (Map<String, Object> data, String fileName, AuthData authData, EnvData env) throws KommetException
	{
		ByteArrayOutputStream os;
		try
		{
			os = PdfUtil.getDataTable(data, appConfig.getFontDir());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new KommetException("Error generating PDF: " + e.getMessage());
		}
		
		return store(os, fileName, authData, env);
	}
	
	@Transactional
	public File exportToXlsxFile (Map<String, Object> data, String fileName, String sheetName, AuthData authData, EnvData env) throws KommetException
	{
		Workbook wb = DataTableUtil.getWorkbook(data, sheetName);
		
		MsExcelApi excelApi = new MsExcelApi(ExcelFormat.XLSX);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		excelApi.write(wb, os);
		
		return store(os, fileName, authData, env);
	}
	
	private File store (ByteArrayOutputStream os, String fileName, AuthData authData, EnvData env) throws KommetException
	{
		// create disk file
		try
		{
			FileOutputStream fos = new FileOutputStream(appConfig.getFileDir() + "/" + fileName);
			os.writeTo(fos);
			fos.close();
			os.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new KommetException("Error writing to disk file: " + e.getMessage());
		}
		
		File file = new File();
		file.setName(fileName);
		file.setAccess(File.PUBLIC_ACCESS);
		file = fileService.saveFile(file, authData, env);
		
		// create file revision
		FileRevision revision = new FileRevision();
		revision.setFile(file);
		revision.setName(file.getName());
		revision.setPath(fileName);
		revision.setRevisionNumber(1);
		
		// get file size
		java.io.File storedFile = new java.io.File(appConfig.getFileDir() + "/" + revision.getPath());
		revision.setSize(Long.valueOf(storedFile.length()).intValue());
		revision = fileService.saveRevision(revision, authData, env);
		
		ArrayList<FileRevision> revisions = new ArrayList<FileRevision>();
		revisions.add(revision);
		file.setRevisions(revisions);
		
		return file;
	}
}