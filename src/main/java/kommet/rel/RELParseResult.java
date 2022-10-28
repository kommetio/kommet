/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rel;

import java.util.HashSet;
import java.util.Set;

public class RELParseResult
{
	private String code;
	private int tokenIndex;
	private Set<String> recordFields = new HashSet<String>();

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getCode()
	{
		return code;
	}

	public void setTokenIndex(int tokenIndex)
	{
		this.tokenIndex = tokenIndex;
	}

	public int getTokenIndex()
	{
		return tokenIndex;
	}

	public Set<String> getRecordFields()
	{
		return recordFields;
	}

	public void setRecordFields(Set<String> recordFields)
	{
		this.recordFields = recordFields;
	}
}