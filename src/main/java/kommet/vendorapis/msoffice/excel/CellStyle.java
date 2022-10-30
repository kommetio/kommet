/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;


public class CellStyle
{
	private Color fillForegroundColor;
	private Color fillBackgroundColor;
	private Font font;
	private BorderFormatting borderFormatting;

	public Color getFillForegroundColor()
	{
		return fillForegroundColor;
	}

	public void setFillForegroundColor(Color fillForegroundColor)
	{
		this.fillForegroundColor = fillForegroundColor;
	}

	public Color getFillBackgroundColor()
	{
		return fillBackgroundColor;
	}

	public void setFillBackgroundColor(Color fillBackgroundColor)
	{
		this.fillBackgroundColor = fillBackgroundColor;
	}

	public Font getFont()
	{
		return font;
	}

	public void setFont(Font font)
	{
		this.font = font;
	}

	public BorderFormatting getBorderFormatting()
	{
		return borderFormatting;
	}

	public void setBorderFormatting(BorderFormatting borderStyle)
	{
		this.borderFormatting = borderStyle;
	}
}