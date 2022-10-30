/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

public class TabConfig
{
	private String afterRender;
	private String var;
	
	public TabConfig (TabConfigTag tag)
	{
		this.afterRender = tag.getAfterRender();
		this.var = tag.getVar();
	}
	
	public String getAfterRender()
	{
		return afterRender;
	}
	
	public String getVar()
	{
		return var;
	}
	
}
