/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kommet.basic.MimeTypes;
import kommet.data.KommetException;
import kommet.json.JSON;
import kommet.pdf.PdfUtil;
import kommet.utils.AppConfig;
import kommet.utils.UrlUtil;
import kommet.vendorapis.msoffice.excel.DataTableUtil;
import kommet.vendorapis.msoffice.excel.ExcelFormat;
import kommet.vendorapis.msoffice.excel.MsExcelApi;
import kommet.vendorapis.msoffice.excel.Workbook;

@Controller
public class ExportController extends BasicRestController
{
	@Inject
	AppConfig appConfig;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/table/export", method = RequestMethod.POST)
	@ResponseBody
	public void export(@RequestParam(value = "format", required = false) String format,
						@RequestParam(value = "data", required = false) String serializedData,
						@RequestParam(value = "fileName", required = false) String fileName,
						@RequestParam(value = "workbookName", required = false) String workbookName,
						HttpServletResponse resp) throws IOException, KommetException
	{
		Map<String, Object> data = JSON.parseToMap(serializedData);
		
		if (StringUtils.isBlank(workbookName))
		{
			workbookName = "Table";
		}
		
		if ("xlsx".equals(format.toLowerCase()))
		{
			if (StringUtils.isBlank(fileName))
			{
				fileName = "download.xlsx";
			}
			
			returnXLSX(data, workbookName, fileName, resp);
			return;
		}
		else if ("pdf".equals(format.toLowerCase()))
		{
			if (StringUtils.isBlank(fileName))
			{
				fileName = "download.pdf";
			}
			
			returnPDF(data, workbookName, fileName, resp);
			return;
		}
		else
		{
			PrintWriter out = resp.getWriter();
			returnRestError("Unsupported format", out);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
	
	private void returnPDF (Map<String, Object> data, String sheetName, String exportFileName, HttpServletResponse resp) throws KommetException
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
		
		// return the file
		resp.setHeader("Content-Disposition", "attachment; filename=\"" + exportFileName + "\""); 
		
		InputStream is = new ByteArrayInputStream(os.toByteArray());
		
		// copy it to response's output stream
		try
		{
			IOUtils.copy(is, resp.getOutputStream());
			resp.setContentType(MimeTypes.APPLICATION_EXCEL);
			resp.flushBuffer();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing file to output. Nested: " + e.getMessage());
		}
	}

	private void returnXLSX (Map<String, Object> data, String sheetName, String exportFileName, HttpServletResponse resp) throws KommetException
	{
		Workbook wb = DataTableUtil.getWorkbook(data, sheetName);
		
		// return the file
		resp.setHeader("Content-Disposition", "attachment; filename=\"" + exportFileName + "\""); 
		
		MsExcelApi excelApi = new MsExcelApi(ExcelFormat.XLSX);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		excelApi.write(wb, os);
		
		InputStream is = new ByteArrayInputStream(os.toByteArray());
		
		// copy it to response's output stream
		try
		{
			IOUtils.copy(is, resp.getOutputStream());
			resp.setContentType(MimeTypes.APPLICATION_EXCEL);
			resp.flushBuffer();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing file to output. Nested: " + e.getMessage());
		}
	}
}