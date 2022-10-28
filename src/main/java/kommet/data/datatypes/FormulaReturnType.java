/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import java.math.BigDecimal;
import java.sql.Date;

import kommet.data.KommetException;

public enum FormulaReturnType
{
	TEXT(0),
	NUMBER(1),
	DATETIME(2);
	
	private int id;
	
	private FormulaReturnType (int id)
	{
		this.id = id;
	}

	public String getJavaType()
	{
		switch (this)
		{
			case TEXT: return String.class.getName();
			case NUMBER: return BigDecimal.class.getName();
			case DATETIME: return Date.class.getName();
			default: return null;
		}
	}

	public int getId()
	{
		return this.id;
	}

	public String getPostgresType() throws KommetException
	{
		switch (this)
		{
			case TEXT: return DataType.getPostgresType(new TextDataType(255));
			case NUMBER: return DataType.getPostgresType(new NumberDataType(20, BigDecimal.class));
			case DATETIME: return DataType.getPostgresType(new DateTimeDataType());
			default: return null;
		}
	}
}