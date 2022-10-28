/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

public class FieldNotSetException extends KommetException
{
	private static final long serialVersionUID = -1313399487807829418L;
	private KID recordId;
	private String fieldApiName;
	
	public FieldNotSetException(String msg, KID recordId, String fieldApiName)
	{
		super(msg);
		this.fieldApiName = fieldApiName;
		this.recordId = recordId;
	}

	public KID getRecordId()
	{
		return recordId;
	}

	public String getFieldApiName()
	{
		return fieldApiName;
	}
}