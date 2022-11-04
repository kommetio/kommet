/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.kmparams.actions;

import kommet.data.KID;
import kommet.web.kmparams.KmParamException;

public class OverrideLayout extends Action
{
	private KID layoutId;
	
	public OverrideLayout() throws KmParamException
	{
		super("layout");
	}

	public void setLayoutId(KID layoutId)
	{
		this.layoutId = layoutId;
	}

	public KID getLayoutId()
	{
		return layoutId;
	}
}