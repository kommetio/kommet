/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;

public interface RecordContext
{
	public Type getType();
	public KID getRecordId() throws KommetException;
	public String getFieldNamePrefix();
	public void addErrorMsgs(String msg);
}
