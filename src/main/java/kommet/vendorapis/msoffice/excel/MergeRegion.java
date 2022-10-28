/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

public class MergeRegion
{
	private int startRow;
	private int endRow;
	private int startCell;
	private int endCell;
	
	public MergeRegion (int startRow, int endRow, int startCell, int endCell)
	{
		this.startRow = startRow;
		this.endRow = endRow;
		this.startCell = startCell;
		this.endCell = endCell;
	}

	public int getStartRow()
	{
		return startRow;
	}

	public void setStartRow(int startRow)
	{
		this.startRow = startRow;
	}

	public int getEndRow()
	{
		return endRow;
	}

	public void setEndRow(int endRow)
	{
		this.endRow = endRow;
	}

	public int getStartCell()
	{
		return startCell;
	}

	public void setStartCell(int startCell)
	{
		this.startCell = startCell;
	}

	public int getEndCell()
	{
		return endCell;
	}

	public void setEndCell(int endCell)
	{
		this.endCell = endCell;
	}
}