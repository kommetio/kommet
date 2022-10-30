/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.vendorapis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.Test;

import kommet.data.KommetException;
import kommet.tests.BaseUnitTest;
import kommet.vendorapis.msoffice.excel.BorderFormatting;
import kommet.vendorapis.msoffice.excel.BorderStyle;
import kommet.vendorapis.msoffice.excel.Cell;
import kommet.vendorapis.msoffice.excel.CellStyle;
import kommet.vendorapis.msoffice.excel.Color;
import kommet.vendorapis.msoffice.excel.ExcelFormat;
import kommet.vendorapis.msoffice.excel.Font;
import kommet.vendorapis.msoffice.excel.HtmlToExcelParser;
import kommet.vendorapis.msoffice.excel.Hyperlink;
import kommet.vendorapis.msoffice.excel.MSExcelApiException;
import kommet.vendorapis.msoffice.excel.MsExcelApi;
import kommet.vendorapis.msoffice.excel.Row;
import kommet.vendorapis.msoffice.excel.Sheet;
import kommet.vendorapis.msoffice.excel.Workbook;

public class MsExcelApiTest extends BaseUnitTest
{
	@Test
	public void testExcelCreator() throws MSExcelApiException
	{
		MsExcelApi api = new MsExcelApi(ExcelFormat.XLSX);
		
		Workbook excelWorkbook = getSampleWorkbook();
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		api.write(excelWorkbook, os);
	}
	
	@Test
	public void testExcelFromHtml() throws KommetException
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<table>");
		sb.append("<tbody>");
		sb.append("<tr><td>11</td><td>Grzesiu</td></tr>");
		sb.append("</tbody>");
		sb.append("</table>");
		
		List<Workbook> workbooks = HtmlToExcelParser.parse(sb.toString());
		assertNotNull(workbooks);
		assertEquals(1, workbooks.size());
	}

	private Workbook getSampleWorkbook()
	{
		Workbook wb = new Workbook();
		Sheet sheet = new Sheet();
		sheet.setName("Sheet One");
		wb.addSheet(sheet);
		
		// add row
		Row row = new Row();
		
		Cell cell1 = new Cell();
		cell1.setStringValue("Raimme");
		row.addCell(cell1);
		
		Cell cell2 = new Cell();
		cell2.setHyperlink(new Hyperlink("http://kommet.io", "the platform"));
		row.addCell(cell2);
		
		sheet.addRow(row);
		
		Font font = new Font();
		font.setBold(true);
		font.setColor(new Color(233,123,11));
		
		CellStyle style = new CellStyle();
		style.setFillBackgroundColor(new Color(11,11,11));
		row.setCellStyle(style);
		
		BorderFormatting borderFormatting = new BorderFormatting(new Color(233,123,11), BorderStyle.MEDIUM);
		style.setBorderFormatting(borderFormatting);
		
		return wb;
	}
}
