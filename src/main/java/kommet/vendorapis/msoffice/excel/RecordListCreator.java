/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

import java.math.BigDecimal;
import java.util.List;

import kommet.auth.AuthData;
import kommet.data.KommetException;
import kommet.data.Record;

/**
 * Creates a sheet containing a record list.
 * @author Radek Krawiec
 * @since 29/04/2015
 */
public class RecordListCreator
{
	public Sheet createRecordListSheet (List<Record> records, List<String> columnNames, List<String> properties, AuthData authData) throws KommetException
	{
		Sheet sheet = new Sheet();
		sheet.setName("Records");
		
		// add header row
		Row header = new Row();
		for (String col : columnNames)
		{
			header.addCell(new Cell(col));
		}
		
		sheet.addRow(header);
		
		// add a row for each record
		for (Record record : records)
		{
			sheet.addRow(getRecordRow(record, properties, authData));
		}
		
		return sheet;
	}

	private Row getRecordRow(Record record, List<String> properties, AuthData authData) throws KommetException
	{
		Row row = new Row();
		
		for (String prop : properties)
		{
			Object val = record.getField(prop);
			Cell cell = new Cell();
			
			if (val == null)
			{
				cell.setStringValue("");
			}
			else if (val instanceof String)
			{
				cell.setStringValue((String)val);
			}
			else if (val instanceof BigDecimal)
			{
				cell.setNumericValue(((BigDecimal)val).doubleValue());
			}
			else if (val instanceof Boolean)
			{
				cell.setBooleanValue((Boolean)val);
			}
			else
			{
				cell.setStringValue(val.toString());
			}
			
			row.addCell(cell);
		}
		
		return row;
	}
}