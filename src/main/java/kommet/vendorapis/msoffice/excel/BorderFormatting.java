/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

/**
 * Represents information about cell border formatting, such as its width and style
 * @author Radek Krawiec
 * @since 17/04/2015
 */
public class BorderFormatting
{
	private Color topColor;
	private Color bottomColor;
	private Color leftColor;
	private Color rightColor;
	private BorderStyle topStyle;
	private BorderStyle bottomStyle;
	private BorderStyle leftStyle;
	private BorderStyle rightStyle;
	
	public BorderFormatting()
	{
		// empty
	}
	
	public BorderFormatting (Color color, BorderStyle style)
	{
		setColor(color);
		setStyle(style);
	}
	
	public void setColor (Color color)
	{
		this.topColor = color;
		this.bottomColor = color;
		this.leftColor = color;
		this.rightColor = color;
	}
	
	public void setStyle (BorderStyle Style)
	{
		this.topStyle = Style;
		this.bottomStyle = Style;
		this.leftStyle = Style;
		this.rightStyle = Style;
	}

	public Color getTopColor()
	{
		return topColor;
	}

	public void setTopColor(Color topColor)
	{
		this.topColor = topColor;
	}

	public Color getBottomColor()
	{
		return bottomColor;
	}

	public void setBottomColor(Color bottomColor)
	{
		this.bottomColor = bottomColor;
	}

	public Color getLeftColor()
	{
		return leftColor;
	}

	public void setLeftColor(Color leftColor)
	{
		this.leftColor = leftColor;
	}

	public Color getRightColor()
	{
		return rightColor;
	}

	public void setRightColor(Color rightColor)
	{
		this.rightColor = rightColor;
	}

	public BorderStyle getTopStyle()
	{
		return topStyle;
	}

	public void setTopStyle(BorderStyle topStyle)
	{
		this.topStyle = topStyle;
	}

	public BorderStyle getBottomStyle()
	{
		return bottomStyle;
	}

	public void setBottomStyle(BorderStyle bottomStyle)
	{
		this.bottomStyle = bottomStyle;
	}

	public BorderStyle getLeftStyle()
	{
		return leftStyle;
	}

	public void setLeftStyle(BorderStyle leftStyle)
	{
		this.leftStyle = leftStyle;
	}

	public BorderStyle getRightStyle()
	{
		return rightStyle;
	}

	public void setRightStyle(BorderStyle rightStyle)
	{
		this.rightStyle = rightStyle;
	}
}