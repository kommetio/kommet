/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

/**
 * Represents a colour to be used in Excel styles.
 * @author Radek Krawiec
 * @since 13/04/2015
 */
public class Color
{
	private int red;
	private int green;
	private int blue;

	public Color (int red, int green, int blue)
	{
		this.blue = blue;
		this.red = red;
		this.green = green;
	}
	
	/**
	 * Create colour from its HEX value.
	 * @param hexColor
	 */
	public Color (String hexColor)
	{
		this.red = Integer.parseInt(hexColor.substring(0,2), 16);
		this.green = Integer.parseInt(hexColor.substring(2,4), 16);
		this.blue = Integer.parseInt(hexColor.substring(4,6), 16);
	}
	
	public int getRed()
	{
		return red;
	}

	public void setRed(int red)
	{
		this.red = red;
	}

	public int getGreen()
	{
		return green;
	}

	public void setGreen(int green)
	{
		this.green = green;
	}

	public int getBlue()
	{
		return blue;
	}

	public void setBlue(int blue)
	{
		this.blue = blue;
	}
}