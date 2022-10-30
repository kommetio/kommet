/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.dataimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import kommet.data.KommetException;
import kommet.dataimport.csv.CSVParser;
import kommet.dataimport.csv.CSVParserException;
import kommet.dataimport.csv.RowListCsvProcessor;
import kommet.tests.BaseUnitTest;

public class CsvImportTest extends BaseUnitTest
{
	//private static final Logger log = LoggerFactory.getLogger(CsvImportTest.class);
	
	@Test
	public void testCSV() throws IOException, KommetException
	{
		String line = "\"john\";\"lea mea\";\"kareem\nree\t\";\"tee-aaa;ss\";\"q q\"";
		
		// create some file
		StringBuilder csvFile = new StringBuilder();
		csvFile.append("\"one\";\"two\";\"three\";\"four\";\"five\"\n");
		csvFile.append("\"one1\";\"two1\";\"three1\";\"four1\";\"five1\"\n");
		csvFile.append(line).append("\n");
		csvFile.append("\"one1\\\"\";\"two1\";\"three1\";\"four1\";\"five1\"\n");
		
		CSVParser parser = new CSVParser();
		RowListCsvProcessor processor = new RowListCsvProcessor();
		parser.parse(new ByteArrayInputStream(csvFile.toString().getBytes()), processor);
		
		List<Map<Integer,String>> rows = processor.getRows();
		assertEquals(3, rows.size());
		
		assertEquals("one1", rows.get(0).get(0));
		assertEquals("one1\"", rows.get(2).get(0));
		assertEquals("john", rows.get(1).get(0));
		assertEquals("lea mea", rows.get(1).get(1));
		assertEquals("kareem\nree\t", rows.get(1).get(2));
		
		assertNotNull(processor.getHeaders());
		assertEquals(5,  processor.getHeaders().size());
		assertEquals("one", processor.getHeaders().get(0));
		
		// try parsing file with incorrect number of columns in one line
		csvFile.append("\"one\";\"two\";\"three\";\"four\"\n");
		parser = new CSVParser();
		processor = new RowListCsvProcessor();
		
		try
		{
			parser.parse(new ByteArrayInputStream(csvFile.toString().getBytes()), processor);
			fail("Parsing file with incorrect number of columns should fail");
		}
		catch (CSVParserException e)
		{
			// expected
			assertEquals("Line 4 has 4 columns instead of expected 5", e.getMessage());
		}
	}
}
