/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import kommet.basic.Button;
import kommet.data.KID;

public class ButtonFilter extends BasicFilter<Button>
{
	private KID typeId;
	private KID buttonId;

	public KID getTypeId()
	{
		return typeId;
	}

	public void setTypeId(KID typeId)
	{
		this.typeId = typeId;
	}

	public KID getButtonId()
	{
		return buttonId;
	}

	public void setButtonId(KID buttonId)
	{
		this.buttonId = buttonId;
	}
	
}