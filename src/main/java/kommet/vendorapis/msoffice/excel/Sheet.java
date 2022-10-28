/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sheet
{
	private String name;
	private List<Row> rows;
	private List<MergeRegion> mergeRegions;
	
	// map column numbers to their widths (measured in character count)
	private Map<Integer, Integer> colWidths;

	public Sheet()
	{
		this.rows = new ArrayList<Row>();
		this.colWidths = new HashMap<Integer, Integer>();
	}
	
	public void mergeCells (int startRow, int endRow, int startCell, int endCell)
	{
		if (this.mergeRegions == null)
		{
			this.mergeRegions = new ArrayList<MergeRegion>();
		}
		this.mergeRegions.add(new MergeRegion(startRow, endRow, startCell, endCell));
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public List<Row> getRows()
	{
		return rows;
	}

	public void setRows(List<Row> rows)
	{
		this.rows = rows;
	}

	public void addRow(Row row)
	{
		this.rows.add(row);
	}
	
	public List<MergeRegion> getMergeRegions()
	{
		return mergeRegions;
	}

	public Map<Integer, Integer> getColumnWidths()
	{
		return colWidths;
	}

	public void setColumnWidth(int colIndex, int width)
	{
		this.colWidths.put(colIndex, width);
	}
}