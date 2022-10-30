/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.objectlist;

public class SerializedListColumn
{
	private String field;
	private String formula;
	private String idField;
	private String nameField;
	private String isLink;
	private String isSortable;

	public String getField()
	{
		return field;
	}

	public void setField(String field)
	{
		this.field = field;
	}

	public String getFormula()
	{
		return formula;
	}

	public void setFormula(String formula)
	{
		this.formula = formula;
	}

	public String getIdField()
	{
		return idField;
	}

	public void setIdField(String idField)
	{
		this.idField = idField;
	}

	public String getNameField()
	{
		return nameField;
	}

	public void setNameField(String nameField)
	{
		this.nameField = nameField;
	}

	public String getIsLink()
	{
		return isLink;
	}

	public void setIsLink(String isLink)
	{
		this.isLink = isLink;
	}

	public String getIsSortable()
	{
		return isSortable;
	}

	public void setIsSortable(String isSortable)
	{
		this.isSortable = isSortable;
	}
}
