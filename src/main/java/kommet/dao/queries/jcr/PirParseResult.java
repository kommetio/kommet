/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import kommet.data.Field;

/**
 * This class represents information retrieved from parsing a PIR. It contains the API-name-representation
 * of the PIR (i.e. a nested property) and field information of the most nested field. The latter is useful
 * if we want to find out about e.g. the field's data type.
 * @author Radek Krawiec
 * @created 19/11/2014
 */
public class PirParseResult
{
	private String qualifiedName;
	private Field mostNestedField;

	public String getQualifiedName()
	{
		return qualifiedName;
	}

	public void setQualifiedName(String qualifiedName)
	{
		this.qualifiedName = qualifiedName;
	}

	public Field getMostNestedField()
	{
		return mostNestedField;
	}

	public void setMostNestedField(Field mostNestedField)
	{
		this.mostNestedField = mostNestedField;
	}
}