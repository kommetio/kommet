/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.buttons;

import java.util.ArrayList;
import java.util.List;

public class ButtonPanel
{
	private List<ButtonPrototype> buttons = new ArrayList<ButtonPrototype>();
	private boolean customButtons;
	
	public ButtonPanel (boolean customButtons)
	{
		this.customButtons = customButtons;
	}

	public List<ButtonPrototype> getButtons()
	{
		return buttons;
	}
	
	public boolean hasButtons()
	{
		return this.buttons != null && !this.buttons.isEmpty();
	}
	
	public void addButton (ButtonPrototype button)
	{
		if (this.buttons == null)
		{
			this.buttons = new ArrayList<ButtonPrototype>();
		}
		this.buttons.add(button);
	}
	
	public boolean isCustomButtons()
	{
		return customButtons;
	}
}
