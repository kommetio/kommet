/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.Row;
import be.quodlibet.boxable.VerticalAlignment;
import be.quodlibet.boxable.line.LineStyle;
import kommet.utils.PropertyUtilException;

public class PdfUtil
{
	private static final int ROW_HEIGHT = 30;
	private static final int FONT_SIZE = 10;
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	public static ByteArrayOutputStream getDataTable (Map<String, Object> data, String fontDir) throws IOException, PropertyUtilException
	{
		// Create a document and add a page to it
		PDDocument doc = new PDDocument();
		
		PDType0Font plainFont = PDType0Font.load(doc, new File(fontDir + "/arial.ttf"));
		PDType0Font boldFont = PDType0Font.load(doc, new File(fontDir + "/arial-bold.ttf"));
		
		PDPage page = new PDPage(PDRectangle.A4);
		
		// PDRectangle.LETTER and others are also possible
		//PDRectangle rect = page.getMediaBox();
		//float pageWidth = page.getMediaBox().getWidth();
		
		// rect can be used to get the page width and height
		doc.addPage(page);
        
		// Start a new content stream which will "hold" the to be created content
		PDPageContentStream cos = new PDPageContentStream(doc, page);
		
		int marginTop = 0;
		int sideMargin = 40;
		
		Object topText = data.get("topText");
		if (topText != null)
		{
			Map<String, Object> topTextMap = (Map<String, Object>)topText;
			Float fontSize = (Float)topTextMap.get("fontSize");
			if (fontSize == null)
			{
				fontSize = 12f;
			}
			
			cos.setFont(plainFont, fontSize);
			cos.beginText();
			cos.moveTextPositionByAmount (sideMargin, page.getMediaBox().getHeight() - sideMargin);
			cos.drawString ((String)topTextMap.get("content"));
			cos.endText();
			
			marginTop = 20;
		}
		
		BaseTable table = getBaseTable(doc, page, marginTop, sideMargin);
		
		// render table rows
		List<Object> header = (List<Object>)data.get("header");
		
		int columnCount = header.size();
		float columnWidthPercent = 100 / columnCount; 
		
		// add header row
		Row<PDPage> headerRow = table.createRow(ROW_HEIGHT);
		for (Object headerCol : header)
		{
			Map<String, Object> col = (Map<String, Object>)headerCol;
		
			// the first parameter is the cell width
	        Cell<PDPage> cell = getCell(headerRow, (String)col.get("content"), columnWidthPercent, boldFont);
	        cell.setFont(boldFont);
	        cell.setTopBorderStyle(new LineStyle(Color.BLACK, 10));
	        
	        String bgColor = (String)col.get("bgColor");
	        
	        if (!StringUtils.isEmpty(bgColor))
	        {
	        	cell.setFillColor(hex2Rgb(bgColor));
	        }
		}
		
		table.addHeaderRow(headerRow);
		
		
		List<Object> rows = (List<Object>)data.get("rows");
				
		// add actual data rows
		for (Object row : rows)
		{
			List<Object> rowData = (List<Object>)row;
			
			Row<PDPage> dataRow = table.createRow(ROW_HEIGHT);
			
			for (Object cell : rowData)
			{
				Map<String, Object> cellData = (Map<String, Object>)cell;
				String fontWeight = (String)cellData.get("fontWeight");
				Cell<PDPage> cellObj = getCell(dataRow, (String)cellData.get("content"), columnWidthPercent, "bold".equals(fontWeight) ? boldFont : plainFont);
				
				String bgColor = (String)cellData.get("bgColor");
		        if (!StringUtils.isEmpty(bgColor))
		        {
		        	cellObj.setFillColor(hex2Rgb(bgColor));
		        }
			}
		}
		
		// add summary row
		List<Object> summary = (List<Object>)data.get("summary");
		
		if (summary != null)
		{
			Row<PDPage> summaryRow = table.createRow(ROW_HEIGHT);
			for (Object summaryCol : summary)
			{
				Map<String, Object> col = (Map<String, Object>)summaryCol;
				getCell(summaryRow, (String)col.get("content"), columnWidthPercent, plainFont);
			}
		}
		
		table.draw();
		
		// close the content stream 
        cos.close();
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        doc.save(os);
        doc.close();
        
        return os;
	}
	
	private static Color hex2Rgb (String colorStr)
	{
	    return new Color(
	            Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
	            Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
	            Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
	}
	
	private static Cell<PDPage> getCell (Row<PDPage> row, String text, float widthPercent, PDType0Font font)
	{
		Cell<PDPage> cell = row.createCell(widthPercent, text);
        cell.setFont(font);
        cell.setFontSize(FONT_SIZE);
        
        // vertical alignment
        cell.setValign(VerticalAlignment.MIDDLE);
        
        return cell;
	}

	private static BaseTable getBaseTable(PDDocument doc, PDPage page, int marginTop, float margin) throws IOException
	{
        // starting y position is whole page height subtracted by top and bottom margin
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
        // we want table across whole page width (subtracted by left and right margin ofcourse)
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);

        boolean drawContent = true;
        float bottomMargin = 50;
        // y position is your coordinate of top left corner of the table
        float yPosition = page.getMediaBox().getHeight() - margin - marginTop;

        return new BaseTable(yPosition, yStartNewPage, bottomMargin, tableWidth, margin, doc, page, true, drawContent);
	}
}