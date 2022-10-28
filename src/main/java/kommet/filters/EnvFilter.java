/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import kommet.data.Env;
import kommet.data.KID;

public class EnvFilter extends BasicFilter<Env>
{
	private String name;
	private KID rid;

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setKID(KID rid)
	{
		this.rid = rid;
	}

	public KID getKID()
	{
		return rid;
	}
}