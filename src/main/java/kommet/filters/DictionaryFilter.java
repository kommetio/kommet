/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.Dictionary;
import kommet.data.KID;

public class DictionaryFilter extends BasicFilter<Dictionary>
{
	private Set<KID> dictionaryIds;
	
	public void addDictionaryId(KID id)
	{
		if (this.dictionaryIds == null)
		{
			this.dictionaryIds = new HashSet<KID>();
		}
		this.dictionaryIds.add(id);
	}

	public Set<KID> getDictionaryIds()
	{
		return dictionaryIds;
	}

	public void setDictionaryIds(Set<KID> dictionaryIds)
	{
		this.dictionaryIds = dictionaryIds;
	}
}