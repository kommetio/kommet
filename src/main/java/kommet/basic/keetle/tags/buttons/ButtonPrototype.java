/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.buttons;

public class ButtonPrototype
{
	private ButtonType type;
	private String id;
	
	public ButtonPrototype (ButtonType type)
	{
		this.type = type;
	}

	public void setType(ButtonType type)
	{
		this.type = type;
	}

	public ButtonType getType()
	{
		return type;
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}
}
