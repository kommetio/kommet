/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dataimport.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kommet.data.KommetException;

public class CSVParser
{
	public CSVParser()
	{
		// empty
	}

	public void parse (InputStream in, CsvLineMapProcessor lineProcessor) throws IOException, KommetException
	{	
		// process the file line by line
		processFile(new BufferedReader(new InputStreamReader(in)), lineProcessor);
	}

	/**
	 * Convert the line to map.
	 * @param tokens
	 * @return
	 */
	private LineParseResult lineToMap(List<String> tokens, List<String> columnNames)
	{
		Map<String, String> mapByColumnName = new HashMap<String, String>();
		Map<Integer, String> mapByColumnIndex = new HashMap<Integer, String>();
		
		int colIndex = 0;
		for (String token : tokens)
		{
			mapByColumnIndex.put(colIndex, token);
			mapByColumnName.put(columnNames.get(colIndex), token);
			colIndex++;
		}
		
		LineParseResult result = new LineParseResult();
		result.setMapByColumnIndex(mapByColumnIndex);
		result.setMapByColumnName(mapByColumnName);
		return result;
	}
	
	/**
	 * Process a CSV file, invoking the line processor for each logical line, i.e. each line representing a
	 * record, which can potentially span multiple actual lines in a file (e.g. if there are new line characters
	 * in string literals).
	 * @param fileReader
	 * @param processor
	 * @return
	 * @throws IOException
	 * @throws KommetException 
	 */
	public List<List<String>> processFile(BufferedReader fileReader, CsvLineMapProcessor processor) throws IOException, KommetException
	{
		List<List<String>> lines = new ArrayList<List<String>>();
		List<String> lineTokens = new ArrayList<String>();
		String currentToken = null;
		int intChar;
		boolean isInQuotes = false;
		Character c = null;
		boolean escapeNextChar = false;
		int lineNo = 0;
		
		char separator = processor.getSeparator();
		
		List<String> columnNames = null;
		
		// call init method
		processor.init();
		
		// read file character by character
		while ((intChar = fileReader.read()) != -1)
		{
			c = (char)intChar;
			
			if (c == '\n')
			{
				// check if this new line character is in quotes
				if (!isInQuotes)
				{
					// end existing line, start a new one
					List<String> tokens = new ArrayList<String>();
					tokens.addAll(lineTokens);
					lines.add(tokens);
					lineTokens = new ArrayList<String>();
					
					if (lineNo > 0)
					{
						// check if this line has a correct number of columns
						if (tokens.size() != columnNames.size())
						{
							throw new CSVParserException("Line " + lineNo + " has " + tokens.size() + " columns instead of expected " + columnNames.size());
						}
						
						// parse this line
						LineParseResult parsedLine = lineToMap(tokens, columnNames);
						processor.processLine(parsedLine.getMapByColumnName(), parsedLine.getMapByColumnIndex(), lineNo);
					}
					else
					{	
						// init column name list
						columnNames = new ArrayList<String>();
						columnNames.addAll(tokens);
						
						// process headers
						processor.processHeaders(columnNames);
					}
					
					lineNo++;
				}
				else
				{
					// just append to the existing token
					currentToken += c;
				}
			}
			else if (c == '"')
			{
				// check if escaped
				if (escapeNextChar)
				{
					// quote is escaped, so we just append it
					currentToken += c;
					escapeNextChar = false;
				}
				else if (!isInQuotes)
				{
					// start new empty token
					currentToken = "";
					isInQuotes = true;
				}
				else
				{
					// close token
					lineTokens.add(currentToken);
					currentToken = null;
					isInQuotes = false;
				}
			}
			else if (c == separator)
			{
				if (currentToken != null)
				{
					// start new empty token
					currentToken += c;
				}
			}
			else if (c == '\\')
			{
				if (escapeNextChar)
				{
					currentToken += c;
					escapeNextChar = false;
				}
				else
				{
					escapeNextChar = true;
				}
			}
			else
			{
				currentToken += c;
			}
		}
		
		// append last line if not already appended
		if (!lineTokens.isEmpty())
		{
			List<String> tokens = new ArrayList<String>();
			tokens.addAll(lineTokens);
			lines.add(tokens);
		}
		
		// call finish method
		processor.finish();
		
		return lines;
	}

	class LineParseResult
	{
		private Map<String, String> mapByColumnName;
		private Map<Integer, String> mapByColumnIndex;

		public Map<Integer, String> getMapByColumnIndex()
		{
			return mapByColumnIndex;
		}

		public void setMapByColumnIndex(Map<Integer, String> mapByColumnIndex)
		{
			this.mapByColumnIndex = mapByColumnIndex;
		}

		public Map<String, String> getMapByColumnName()
		{
			return mapByColumnName;
		}

		public void setMapByColumnName(Map<String, String> mapByColumnName)
		{
			this.mapByColumnName = mapByColumnName;
		}
	}
}