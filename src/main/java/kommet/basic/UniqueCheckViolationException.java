/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.data.KommetException;
import kommet.data.Record;

public class UniqueCheckViolationException extends KommetException
{
	private static final long serialVersionUID = 1052297052545344193L;
	private UniqueCheck uniqueCheck;
	private Record record;

	public UniqueCheckViolationException(String msg, UniqueCheck uniqueCheck)
	{
		super(msg);
		this.uniqueCheck = uniqueCheck;
	}
	
	/**
	 * Returns IDs of fields that caused this unique check violation.
	 * @return
	 */
	public UniqueCheck getUniqueCheck()
	{
		return this.uniqueCheck;
	}

	public Record getRecord()
	{
		return record;
	}

	public void setRecord(Record record)
	{
		this.record = record;
	}
}