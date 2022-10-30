/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

import java.util.ArrayList;
import java.util.List;

/**
 * A row in an Excel sheet
 * @author Radek Krawiec
 * @since 11/04/2015
 */
public class Row
{
	private List<Cell> cells;
	private List<RowMergeRegion> mergeRegions;
	
	public Row()
	{
		this.cells = new ArrayList<Cell>();
	}
	
	public void setCellStyle (CellStyle style)
	{
		for (Cell cell : this.cells)
		{
			cell.setCellStyle(style);
		}
	}
	
	/**
	 * Merge cells within this row.
	 * @param firstCell
	 * @param lastCell
	 */
	public void mergeCells (int firstCell, int lastCell)
	{
		if (this.mergeRegions == null)
		{
			this.mergeRegions = new ArrayList<RowMergeRegion>();
		}
		this.mergeRegions.add(new RowMergeRegion(firstCell, lastCell));
	}

	public List<Cell> getCells()
	{
		return cells;
	}

	public void setCells(List<Cell> cells)
	{
		this.cells = cells;
	}

	public void addCell(Cell cell)
	{
		this.cells.add(cell);
	}

	public List<RowMergeRegion> getMergeRegions()
	{
		return mergeRegions;
	}
}