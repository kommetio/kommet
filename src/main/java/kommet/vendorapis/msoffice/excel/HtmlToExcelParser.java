/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import kommet.data.KommetException;

/**
 * Parses HTML table code to Excel workbook.
 * @author Radek Krawiec
 *
 */
public class HtmlToExcelParser
{
	private static final String TABLE_ELEMENT = "table";
	/**
	 * Parses the HTML in the input and returns a list of workbooks, one for each HTML table
	 * in the input string.
	 * @param html
	 * @return
	 * @throws KommetException
	 */
	public static List<Workbook> parse (String html) throws KommetException
	{
		DOMParser domParser = new DOMParser();
		InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(html));
	    
	    List<Workbook> workbooks = new ArrayList<Workbook>();
		
		try
		{
			domParser.parse(is);
		}
		catch (SAXException e)
		{
			throw new KommetException("Could not parse HTML to DOM. Nested: " + e.getMessage());
		}
		catch (IOException e)
		{
			throw new KommetException("Error parsing HTML do DOM. Nested: " + e.getMessage());
		}
		
		Document doc = domParser.getDocument();
		NodeList nodes = doc.getChildNodes();
		
		for (int i = 0; i < nodes.getLength(); i++)
		{
			Element element = (Element)nodes.item(i);
			
			if (element.getNodeName().toLowerCase().equals(TABLE_ELEMENT))
			{
				workbooks.add(getTableWorkbook(element));
			}
		}
		
		return workbooks;
	}
	
	/**
	 * Returns a workbook from a DOM structure containing an HTML table.
	 * @param element
	 * @return
	 * @throws KommetException 
	 */
	private static Workbook getTableWorkbook(Element element) throws KommetException
	{
		if (!element.getNodeName().toLowerCase().equals(TABLE_ELEMENT))
		{
			throw new KommetException("Cannot parse DOM into Excel because the topmost element in the DOM is not a TABLE element");
		}
		
		// TODO Auto-generated method stub
		return null;
	}
}