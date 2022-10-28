/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dataimport.csv;

import java.util.List;
import java.util.Map;

import kommet.data.KommetException;

/**
 * Interface representing a processor of maps holding value of a single row in a CSV file.
 * @author Radek Krawiec
 * @since 27/04/2015
 */
public interface CsvLineMapProcessor
{
	public void processLine (Map<String, String> mapByColumnName, Map<Integer, String> mapByColumnIndex, int lineNo) throws KommetException;
	public void processHeaders (List<String> headers) throws KommetException;
	public void finish() throws KommetException;
	public void init() throws KommetException;
	public char getSeparator();
}