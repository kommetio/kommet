/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.datatable;

/**
 * Stores the generated code of a data table.
 */
public class DataTableGeneratedCode
{
	// javascript initialization code of the data table
	private String initCode;
	
	// html code of the data table
	private String htmlCode;
	
	/**
	 * Name of the JQuery object, into which the data table code will be injected.
	 * @example $("#mydiv")
	 */
	private String target;
	
	public String getInitCode()
	{
		return initCode;
	}
	public void setInitCode(String initCode)
	{
		this.initCode = initCode;
	}
	public String getHtmlCode()
	{
		return htmlCode;
	}
	public void setHtmlCode(String htmlCode)
	{
		this.htmlCode = htmlCode;
	}
	public String getTarget()
	{
		return target;
	}
	public void setTarget(String target)
	{
		this.target = target;
	}
}
