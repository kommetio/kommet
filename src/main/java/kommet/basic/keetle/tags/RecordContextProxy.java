/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.ArrayList;
import java.util.List;

import kommet.data.KID;
import kommet.data.Type;

/**
 * Object that proxies a record context when it is not available as <tt>ObjectDetailsTag</tt>.
 * @author Radek Krawiec
 * @since 26/01/2015
 */
public class RecordContextProxy implements RecordContext
{
	private List<String> errorMsgs;
	private Type type;
	private KID recordId;
	private String fieldNamePrefix;

	public List<String> getErrorMsgs()
	{
		return errorMsgs;
	}

	public void setErrorMsgs(List<String> errorMsgs)
	{
		this.errorMsgs = errorMsgs;
	}

	public KID getRecordId()
	{
		return recordId;
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
	}

	public Type getType()
	{
		return type;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public void addErrorMsgs(String msg)
	{
		if (this.errorMsgs == null)
		{
			this.errorMsgs = new ArrayList<String>();
		}
		this.errorMsgs.add(msg);
	}

	public String getFieldNamePrefix()
	{
		return fieldNamePrefix;
	}

	public void setFieldNamePrefix(String fieldNamePrefix)
	{
		this.fieldNamePrefix = fieldNamePrefix;
	}
}
