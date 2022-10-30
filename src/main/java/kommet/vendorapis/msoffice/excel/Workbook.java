/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

import java.util.ArrayList;
import java.util.List;

public class Workbook
{
	private List<Sheet> sheets;
	
	public Workbook()
	{
		this.sheets = new ArrayList<Sheet>();
	}

	public List<Sheet> getSheets()
	{
		return sheets;
	}

	public void setSheets(List<Sheet> sheets)
	{
		this.sheets = sheets;
	}

	public void addSheet(Sheet sheet)
	{
		this.sheets.add(sheet);
	}
}