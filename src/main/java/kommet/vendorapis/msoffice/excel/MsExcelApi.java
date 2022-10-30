/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;

public class MsExcelApi
{
	// document format - either .xls or .xlsx
	private ExcelFormat format;
	
	public MsExcelApi (ExcelFormat format)
	{
		this.format = format;
	}
	
	/**
	 * Writes an MS Excel workbook to an output stream.
	 * @param workBook
	 * @param os
	 * @throws MSExcelApiException 
	 */
	public void write (Workbook workbook, OutputStream os) throws MSExcelApiException
	{
		if (ExcelFormat.XLSX.equals(format))
		{
			writeXlsx (workbook, os);
		}
		else
		{
			throw new MSExcelApiException("Excel format " + format + " is not supported");
		}
	}

	/**
	 * Writes an MS Excel workbook in XLSX format to an output stream, and closes the stream.
	 * @param workBook
	 * @param os
	 * @throws MSExcelApiException 
	 */
	private void writeXlsx(Workbook workbook, OutputStream os) throws MSExcelApiException
	{
		//Get the workbook instance for XLS file 
		XSSFWorkbook nativeWorkbook = new XSSFWorkbook();
		
		// create sheets
		for (Sheet sheet : workbook.getSheets())
		{
			if (sheet.getName() == null)
			{
				throw new MSExcelApiException("Sheet name is empty");
			}
			
			XSSFSheet nativeSheet = nativeWorkbook.createSheet(sheet.getName());
			
			// rewrite column widths
			for (Integer colIndex : sheet.getColumnWidths().keySet())
			{
				nativeSheet.setColumnWidth(colIndex, sheet.getColumnWidths().get(colIndex));
			}
			
			int rowIndex = 0;
			
			// iterate over rows in the sheet
			for (Row row : sheet.getRows())
			{
				XSSFRow nativeRow = nativeSheet.createRow(rowIndex);
				
				int columnIndex = 0;
				
				// iterate over cells in the row
				for (Cell cell : row.getCells())
				{
					XSSFCell nativeCell = nativeRow.createCell(columnIndex++);
					nativeCell = cell.clone(nativeCell, nativeWorkbook);
				}
				
				// check if row merge regions are defined
				if (row.getMergeRegions() != null && !row.getMergeRegions().isEmpty())
				{
					for (RowMergeRegion mergeRegion : row.getMergeRegions())
					{
						// create a merge region on the sheet spanning only the current row
						sheet.mergeCells(rowIndex, rowIndex, mergeRegion.getFirstCell(), mergeRegion.getLastCell());
					}
				}
				
				rowIndex++;
			}
			
			// rewrite merge regions if they exist
			if (sheet.getMergeRegions() != null)
			{
				for (MergeRegion region : sheet.getMergeRegions())
				{
					CellRangeAddress nativeRegion = new CellRangeAddress(region.getStartRow(), region.getEndRow(), region.getStartCell(), region.getEndCell());
					nativeSheet.addMergedRegion(nativeRegion);
				}
			}
		}
		
		try
		{
			nativeWorkbook.write(os);
			os.close();
		}
		catch (IOException e)
		{
			throw new MSExcelApiException("Error writing workbook to output stream. Nested: " + e.getMessage());
		}
	}
	
	private static XSSFColor getNativeXlsxColor (Color color)
	{
		return new XSSFColor(new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue()));
	}
	
	public static XSSFCellStyle getNativeXlsxCellStyle (XSSFCellStyle originalStyle, CellStyle style, XSSFWorkbook wb) throws MSExcelApiException
	{
		XSSFCellStyle nativeStyle = wb.createCellStyle();
		
		if (originalStyle != null)
		{
			// if original style is available, clone it and apply user-defined
			// styles over this original style.
			// this will allow us to define only some styles and use defaults for the other ones.
			nativeStyle.cloneStyleFrom(originalStyle);
		}
		else
		{
			nativeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		}
		
		if (style.getFillBackgroundColor() != null)
		{
			nativeStyle.setFillBackgroundColor(getNativeXlsxColor(style.getFillBackgroundColor()));
			if (nativeStyle.getFillPatternEnum() == FillPatternType.NO_FILL)
			{
				nativeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			}
		}
		/*else
		{
			nativeStyle.setFillBackgroundColor(null);
		}*/
		
		if (style.getFillForegroundColor() != null)
		{
			nativeStyle.setFillForegroundColor(getNativeXlsxColor(style.getFillForegroundColor()));
			if (nativeStyle.getFillPatternEnum() == FillPatternType.NO_FILL)
			{
				nativeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			}
		}
		/*else
		{
			nativeStyle.setFillForegroundColor(null);
		}*/
		
		if (style.getFont() != null)
		{
			nativeStyle.setFont(getNativeXSSFFont(style.getFont(), wb));
		}
		
		if (style.getBorderFormatting() != null)
		{
			BorderFormatting bf = style.getBorderFormatting();
			
			if (bf.getBottomStyle() != null)
			{
				nativeStyle.setBorderBottom(convertBorderStyle(bf.getBottomStyle()));
			}
			if (bf.getTopStyle() != null)
			{
				nativeStyle.setBorderTop(convertBorderStyle(bf.getTopStyle()));
			}
			if (bf.getLeftStyle() != null)
			{
				nativeStyle.setBorderLeft(convertBorderStyle(bf.getLeftStyle()));
			}
			if (bf.getRightStyle() != null)
			{
				nativeStyle.setBorderRight(convertBorderStyle(bf.getRightStyle()));
			}
			
			if (bf.getBottomColor() != null)
			{
				nativeStyle.setBorderColor(BorderSide.BOTTOM, getNativeXlsxColor(bf.getBottomColor()));
			}
			if (bf.getTopColor() != null)
			{
				nativeStyle.setBorderColor(BorderSide.TOP, getNativeXlsxColor(bf.getTopColor()));
			}
			if (bf.getLeftColor() != null)
			{
				nativeStyle.setBorderColor(BorderSide.LEFT, getNativeXlsxColor(bf.getLeftColor()));
			}
			if (bf.getRightColor() != null)
			{
				nativeStyle.setBorderColor(BorderSide.RIGHT, getNativeXlsxColor(bf.getRightColor()));
			}
		}
		
		return nativeStyle;
	}

	private static BorderStyle convertBorderStyle(kommet.vendorapis.msoffice.excel.BorderStyle style) throws MSExcelApiException
	{
		if (style == null)
		{
			return null;
		}
		else if (kommet.vendorapis.msoffice.excel.BorderStyle.NONE.equals(style))
		{
			return BorderStyle.NONE;
		}
		else if (kommet.vendorapis.msoffice.excel.BorderStyle.THIN.equals(style))
		{
			return BorderStyle.THIN;
		}
		else if (kommet.vendorapis.msoffice.excel.BorderStyle.MEDIUM.equals(style))
		{
			return BorderStyle.MEDIUM;
		}
		else if (BorderStyle.THICK.equals(style))
		{
			return BorderStyle.THICK;
		}
		else
		{
			throw new MSExcelApiException("Unsupported border style " + style);
		}
	}

	private static Font getNativeXSSFFont(kommet.vendorapis.msoffice.excel.Font font, XSSFWorkbook wb)
	{
		XSSFFont nativeFont = wb.createFont();
		
		nativeFont.setBoldweight(font.isBold() ? XSSFFont.BOLDWEIGHT_BOLD : XSSFFont.BOLDWEIGHT_NORMAL);
		
		if (font.getColor() != null)
		{
			nativeFont.setColor(getNativeXlsxColor(font.getColor()));
		}
		
		return nativeFont;
	}
}