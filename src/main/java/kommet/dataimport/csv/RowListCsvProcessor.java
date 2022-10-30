/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dataimport.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A standard row processor for a CSV file. It simply collects all rows from a CSV file and its headers
 * in one place, without performing any additional operation on them.
 * @author Radek Krawiec
 * @since 27/04/2015
 */
public class RowListCsvProcessor implements CsvLineMapProcessor
{
	private List<Map<Integer, String>> rows;
	private List<String> headers;
	
	public RowListCsvProcessor()
	{
		this.rows = new ArrayList<Map<Integer,String>>();
	}
	
	@Override
	public void processLine(Map<String, String> mapByColumnName, Map<Integer, String> mapByColumnIndex, int lineNo)
	{
		this.rows.add(mapByColumnIndex);
	}

	@Override
	public void processHeaders(List<String> headers)
	{
		List<String> columnNames = new ArrayList<String>();
		columnNames.addAll(headers);
		this.headers = columnNames;
	}

	public List<String> getHeaders()
	{
		return headers;
	}
	
	public List<Map<Integer, String>> getRows()
	{
		return rows;
	}

	@Override
	public void finish()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public char getSeparator()
	{
		return ';';
	}
}