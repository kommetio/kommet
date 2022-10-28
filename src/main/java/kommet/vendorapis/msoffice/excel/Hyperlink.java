/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.msoffice.excel;

/**
 * Represents a hyperlink that can be attached to an MS Excel cell.
 * 
 * Note that this is not a link that is inserted into the cell. Rather, it is a hyperlink that is assigned
 * to a whole cell.
 * 
 * @author Radek Krawiec
 * @since 17/04/2015 
 */
public class Hyperlink
{
	private String label;
	private String url;
	
	public Hyperlink()
	{
		// empty
	}
	
	public Hyperlink (String url, String label)
	{
		this.url = url;
		this.label = label;
	}
	
	public String getLabel()
	{
		return label;
	}
	public void setLabel(String label)
	{
		this.label = label;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
}