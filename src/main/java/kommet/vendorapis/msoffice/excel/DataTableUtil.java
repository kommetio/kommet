/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DataTableUtil
{
	public static Workbook getWorkbook(Map<String, Object> data, String sheetName)
	{
		Workbook wb = new Workbook();
		Sheet sheet = getSheet(data, sheetName);
		wb.addSheet(sheet);
		
		return wb;
	}

	@SuppressWarnings("unchecked")
	private static Sheet getSheet(Map<String, Object> data, String name)
	{
		Sheet sheet = new Sheet();
		sheet.setName(name);
		
		Object topText = data.get("topText");
		if (topText != null)
		{
			Row topTextRow = new Row();
			Map<String, Object> col = (Map<String, Object>)topText;
			
			Cell cell = new Cell((String)col.get("content"));
			topTextRow.addCell(cell);
			
			System.out.println("[excel] merge count " + col.get("mergeCount"));
			
			if (col.get("mergeCount") != null)
			{
				Integer mergeCount = (Integer)col.get("mergeCount");
				int firstMergeCell = 0;
				System.out.println("[excel] merge count int " + mergeCount);
				topTextRow.mergeCells(firstMergeCell, firstMergeCell + mergeCount);
			}
			
			sheet.addRow(topTextRow);
		}
		
		List<Object> header = (List<Object>)data.get("header");
		
		// add header row
		Row headerRow = new Row();
		for (Object headerCol : header)
		{
			Map<String, Object> col = (Map<String, Object>)headerCol;
			
			Cell cell = new Cell((String)col.get("content"));
			
			if (col.containsKey("style"))
			{
				applyStyle(col, cell);
			}
			
			headerRow.addCell(cell);
		}
		
		sheet.addRow(headerRow);
		
		
		List<Object> rows = (List<Object>)data.get("rows");
				
		// add actual data rows
		for (Object row : rows)
		{
			List<Object> rowData = (List<Object>)row;
			
			Row sheetRow = new Row();
			
			for (Object cell : rowData)
			{
				Map<String, Object> cellData = (Map<String, Object>)cell;
				sheetRow.addCell(new Cell((String)cellData.get("content")));
			}
			
			sheet.addRow(sheetRow);
		}
		
		// add summary row
		List<Object> summary = (List<Object>)data.get("summary");
		Row summaryRow = new Row();
		for (Object summaryCol : summary)
		{
			Map<String, Object> col = (Map<String, Object>)summaryCol; 
			summaryRow.addCell(new Cell((String)col.get("content")));
		}
		
		sheet.addRow(summaryRow);
	
		return sheet;
	}

	@SuppressWarnings("unchecked")
	private static void applyStyle(Map<String, Object> col, Cell cell)
	{
		Map<String, Object> style = (Map<String, Object>)col.get("style");
		if (style == null)
		{
			return;
		}
		
		CellStyle cellStyle = new CellStyle();
		
		String bgColor = (String)style.get("bgcolor");
		
		if (!StringUtils.isEmpty(bgColor))
		{
			cellStyle.setFillForegroundColor(new Color(bgColor));
		}
		cell.setCellStyle(cellStyle);
	}

}