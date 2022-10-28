/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import kommet.basic.keetle.tags.objectlist.ListColumn;

/**
 * An interface representing all tags that display lists of items.
 * @author Radek Krawiec
 * @created 23-03-2014
 */
public interface ListDisplay
{
	public void addColumn(ListColumn col);
	public String getItemVar();
}
