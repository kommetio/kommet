/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * A cell in an Excel row.
 * @author Radek Krawiec
 * @since 11/04/2015
 */
public class Cell
{
	private Double numericValue;
	private String stringValue;
	private Boolean booleanValue;
	private Hyperlink hyperlink;
	private CellStyle cellStyle;
	
	public Cell()
	{
		// empty
	}
	
	public Cell (Double val)
	{
		this.numericValue = val;
	}
	
	public Cell (String val)
	{
		this.stringValue = val;
	}
	
	public Cell (boolean val)
	{
		this.booleanValue = val;
	}
	
	public XSSFCell clone(XSSFCell nativeCell, XSSFWorkbook wb) throws MSExcelApiException
	{	
		if (numericValue != null)
		{
			nativeCell.setCellValue(numericValue);
		}
		else if (stringValue != null)
		{
			nativeCell.setCellValue(stringValue);
		}
		else if (booleanValue != null)
		{
			nativeCell.setCellValue(booleanValue);
		}
		else
		{
			// nullify cell value
			nativeCell.setCellValue((String)null);
		}
		
		if (hyperlink != null)
		{
			org.apache.poi.ss.usermodel.Hyperlink nativeLink = wb.getCreationHelper().createHyperlink(org.apache.poi.common.usermodel.Hyperlink.LINK_URL);
			nativeLink.setAddress(hyperlink.getUrl());
			nativeLink.setLabel(hyperlink.getLabel());
			nativeCell.setHyperlink(nativeLink);
		}
		
		if (this.cellStyle != null)
		{
			// get the original style of the cell and add user-defined formatting on top of it
			nativeCell.setCellStyle(MsExcelApi.getNativeXlsxCellStyle(nativeCell.getCellStyle(), cellStyle, wb));
		}
		
		return nativeCell;
	}

	public Double getNumericValue()
	{
		return numericValue;
	}

	public void setNumericValue(Double numericValue)
	{
		this.numericValue = numericValue;
	}

	public String getStringValue()
	{
		return stringValue;
	}

	public void setStringValue(String stringValue)
	{
		this.stringValue = stringValue;
	}

	public Boolean getBooleanValue()
	{
		return booleanValue;
	}

	public void setBooleanValue(Boolean booleanValue)
	{
		this.booleanValue = booleanValue;
	}

	public CellStyle getCellStyle()
	{
		return cellStyle;
	}

	public void setCellStyle(CellStyle cellStyle)
	{
		this.cellStyle = cellStyle;
	}

	public Hyperlink getHyperlink()
	{
		return hyperlink;
	}

	public void setHyperlink(Hyperlink hyperlink)
	{
		this.hyperlink = hyperlink;
	}
}