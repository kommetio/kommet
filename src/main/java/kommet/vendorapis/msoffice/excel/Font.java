/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;


/**
 * Excel font
 * @author Radek Krawiec
 * @since 13/04/2015
 */
public class Font
{
	private Color color;
	private boolean isBold;
	
	public Color getColor()
	{
		return color;
	}
	public void setColor(Color color)
	{
		this.color = color;
	}

	public boolean isBold()
	{
		return isBold;
	}

	public void setBold(boolean isBold)
	{
		this.isBold = isBold;
	}
}