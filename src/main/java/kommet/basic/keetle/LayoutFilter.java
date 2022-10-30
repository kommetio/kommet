/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

public class LayoutFilter
{
	private String name;
	private Boolean isSystem;

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public Boolean getIsSystem()
	{
		return isSystem;
	}

	public void setIsSystem(Boolean isSystem)
	{
		this.isSystem = isSystem;
	}
}
