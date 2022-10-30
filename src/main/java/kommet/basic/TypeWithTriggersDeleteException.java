/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.List;

import kommet.data.KommetException;

public class TypeWithTriggersDeleteException extends KommetException
{
	private static final long serialVersionUID = 7675179949943282851L;
	private List<Class> classes;

	public TypeWithTriggersDeleteException(String msg, List<Class> classes)
	{
		super(msg);
		this.classes = classes;
	}

	public List<Class> getClasses()
	{
		return classes;
	}
}