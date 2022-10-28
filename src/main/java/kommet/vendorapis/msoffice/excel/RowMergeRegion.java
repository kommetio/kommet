/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

public class RowMergeRegion
{
	private int firstCell;
	private int lastCell;
	
	public RowMergeRegion (int firstCell, int lastCell)
	{
		this.firstCell = firstCell;
		this.lastCell = lastCell;
	}
	
	public int getFirstCell()
	{
		return firstCell;
	}
	
	public int getLastCell()
	{
		return lastCell;
	}
}